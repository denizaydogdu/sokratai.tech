package tr.velzon.aiogretmen.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.microsoft.playwright.options.WaitForSelectorState.ATTACHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ön yüz DAVRANIŞ testleri (ADR-0021): gerçek Chromium (Playwright) girişsiz site profiline (H2) karşı koşar.
 * Kaynak-metin bekçilerinin yakalayamadığı davranışı doğrular: konsol hatası, sol menü, görev kalıcılığı, ilerleme
 * kodu, anahtar kartı, GA yokluğu, "OpenRouter ile bağlan" (PKCE; openrouter.ai {@code page.route} ile taklit —
 * gerçek ağ YOK) ve XSS dumanı.
 *
 * <p>Tarayıcı kurulu değilse / başlatılamazsa testler JUnit varsayımıyla ATLANIR, derleme kırılmaz — ortamda
 * {@code CI} değişkeni varsa ATLAMA yerine HATA verir. Bir kez kur: {@code ./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI
 * -Dexec.args="install chromium" -Dexec.classpathScope=test}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:onyuzdavranis;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.site.guvenli-cerez=false",
        "app.site.hiz.yeni-ziyaretci-saat=1000",
        "app.site.hiz.yazma-dakika=1000",
        "app.hafiza-dir=target/test-hafiza-onyuzdavranis",
        "app.rapor-dir=target/test-raporlar-onyuzdavranis"
})
@ActiveProfiles("site")
class OnYuzDavranisTest {

    static final String KURULUM = "./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI "
            + "-Dexec.args=\"install chromium\" -Dexec.classpathScope=test";
    static final int MASAUSTU_GENISLIK = 1280;
    static final int DAR_GENISLIK = 600;
    static final String TEST_ANAHTARI = "sk-or-v1-" + "0123456789abcdef".repeat(4);

    private static Playwright playwright;
    private static Browser tarayici;
    private static String atlamaNedeni;

    @LocalServerPort
    int port;

    private BrowserContext baglam;
    private Page sayfa;
    private final List<String> konsolHatalari = Collections.synchronizedList(new ArrayList<>());
    private final List<String> konsolMesajlari = Collections.synchronizedList(new ArrayList<>());
    private final List<String> disIstekler = Collections.synchronizedList(new ArrayList<>());
    private final List<Request> yerelIstekler = Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    static void tarayiciyiBaslat() {
        String neden = null;
        try {
            // Tarayıcıları kendiliğinden İNDİRME (3 tarayıcı, yüzlerce MB): yalnız kurulu Chromium kullanılır.
            playwright = Playwright.create(new Playwright.CreateOptions()
                    .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
            tarayici = playwright.chromium().launch();
        } catch (RuntimeException e) {
            String m = String.valueOf(e.getMessage()).replaceAll("\\s+", " ").strip();
            neden = m.length() > 300 ? m.substring(0, 300) + "…" : m;
            tarayiciyiKapat();
        }
        atlamaNedeni = neden;
    }

    @AfterAll
    static void tarayiciyiKapat() {
        if (tarayici != null) {
            tarayici.close();
            tarayici = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    @BeforeEach
    void baglamHazirla() {
        // Varsayım @BeforeAll'da değil burada: surefire raporunda her test tek tek "skipped" (mesajıyla) görünsün.
        // CI'da tarayıcı yokluğu sessiz atlama değil HATA (inceleme M4): orada davranış testleri koşmak zorunda.
        String ci = System.getenv("CI");
        if (tarayici == null && ci != null && !ci.isBlank() && !"false".equalsIgnoreCase(ci) && !"0".equals(ci)) {
            fail("CI ortamında Playwright Chromium zorunlu (ön yüz davranış testleri atlanamaz). Kur: " + KURULUM
                    + " | Neden: " + atlamaNedeni);
        }
        assumeTrue(tarayici != null, () -> "Playwright Chromium kurulu değil ya da başlatılamadı — ön yüz davranış "
                + "testleri ATLANDI (derleme kırılmaz). Bir kez kur: " + KURULUM + " | Neden: " + atlamaNedeni);
        sayfaAc(MASAUSTU_GENISLIK);
    }

    @AfterEach
    void baglamKapat() {
        if (baglam != null) {
            baglam.close();
        }
    }

    /** Yeni ziyaretçi (yeni çerezler/depo) + verilen genişlikte sayfa. Kılavuz ilk ziyarette açılmasın. */
    private void sayfaAc(int genislik) {
        if (baglam != null) {
            baglam.close();
        }
        baglam = tarayici.newContext(new Browser.NewContextOptions().setViewportSize(genislik, 900));
        baglam.addInitScript("try { localStorage.setItem('kilavuzGoruldu', '1'); } catch (e) {}");
        // Gerçek ağ YOK: kendi sunucumuz dışındaki her istek kaydedilir ve kesilir (özel route'lar sonra eklenir,
        // Playwright son ekleneni önce dener).
        baglam.route(u -> !u.startsWith(taban()), r -> {
            disIstekler.add(r.request().method() + " " + r.request().url());
            r.abort();
        });
        baglam.setDefaultTimeout(10_000); // asılı bir bekleme derlemeyi uzun süre tutmasın
        sayfa = baglam.newPage();
        sayfa.onConsoleMessage(m -> {
            konsolMesajlari.add(m.text());
            if ("error".equals(m.type())) {
                konsolHatalari.add(m.text());
            }
        });
        sayfa.onPageError(h -> konsolHatalari.add("pageerror: " + h));
        sayfa.onRequest(r -> {
            if (r.url().startsWith(taban())) {
                yerelIstekler.add(r);
            }
        });
    }

    private String taban() {
        return "http://127.0.0.1:" + port;
    }

    /** Ana sayfayı açar ve uygulamanın açılışını (sol harita çizildi, sohbet paneli kuruldu) bekler. */
    private void anaSayfa(String yol) {
        sayfa.navigate(taban() + yol);
        haritaCizildi();
        sayfa.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /** Harita çizildi (menü kapalıysa görünmez — yalnız DOM'da olması beklenir). */
    private void haritaCizildi() {
        sayfa.waitForSelector("#katman-haritasi .layer", new Page.WaitForSelectorOptions().setState(ATTACHED));
    }

    private Object depo(String tur, String ad) {
        return sayfa.evaluate("([t, a]) => window[t].getItem(a)", List.of(tur, ad));
    }

    private boolean menuAcik() {
        return (Boolean) sayfa.evaluate("() => document.getElementById('egitim-menu').open");
    }

    // ---------- açılış ----------

    @Test
    void sayfaKonsolHatasizYuklenirVeDisariIstekYok() {
        anaSayfa("/");
        assertThat(sayfa.locator("#katman-haritasi .layer").count()).isGreaterThan(2);
        assertThat(sayfa.locator(".anahtar-kart").isVisible()).isTrue();
        assertThat(konsolHatalari).as("konsol hataları (CSP ihlali dahil)").isEmpty();
        assertThat(disIstekler).as("dışarı giden istek").isEmpty();
    }

    @Test
    void gaKimligiYoksaAnalitikYuklenmez() {
        anaSayfa("/");
        assertThat(sayfa.evaluate("() => document.querySelector('script[src*=\"googletagmanager\"]') === null"))
                .isEqualTo(true);
        assertThat(sayfa.evaluate("() => typeof window.gtag")).isEqualTo("undefined");
        assertThat(sayfa.evaluate("() => typeof window.dataLayer")).isEqualTo("undefined");
        assertThat(disIstekler).isEmpty();
    }

    // ---------- sol menü (Faz 13) ----------

    @Test
    void masaustundeMenuVarsayilanAcikVeKapatmaHatirlanir() {
        anaSayfa("/");
        assertThat(menuAcik()).as("≥ 900 px: varsayılan açık").isTrue();
        sayfa.click("#egitim-menu > summary");
        assertThat(menuAcik()).isFalse();
        sayfa.reload();
        haritaCizildi();
        assertThat(menuAcik()).as("kullanıcının kapatması hatırlanır").isFalse();
        assertThat(konsolHatalari).isEmpty();
    }

    @Test
    void darEkrandaMenuVarsayilanKapaliVeAcmaHatirlanir() {
        sayfaAc(DAR_GENISLIK);
        anaSayfa("/");
        assertThat(menuAcik()).as("< 900 px: varsayılan kapalı").isFalse();
        sayfa.click("#egitim-menu > summary");
        assertThat(menuAcik()).isTrue();
        sayfa.reload();
        haritaCizildi();
        assertThat(menuAcik()).as("kullanıcının açması hatırlanır").isTrue();
        assertThat(konsolHatalari).isEmpty();
    }

    // ---------- görevler ve ilerleme kodu ----------

    @Test
    void okudumIleIsaretlenenGorevYenidenYuklemedeKalir() {
        anaSayfa("/?hafta=1&gun=1");
        var gun = sayfa.locator(".day[data-gun=\"1\"]");
        assertThat(gun.locator(".task.checked").count()).isZero();
        gun.locator("button.task-isaretle", new com.microsoft.playwright.Locator.LocatorOptions()
                .setHasText("Okudum ✓")).click();
        gun.locator(".task.checked").first().waitFor();
        sayfa.reload();
        sayfa.waitForSelector(".day[data-gun=\"1\"] .task");
        gun = sayfa.locator(".day[data-gun=\"1\"]");
        assertThat(gun.locator(".task.checked").count()).isEqualTo(1);
        assertThat(gun.locator(".task input[type=checkbox]").first().isChecked()).isTrue();
        assertThat(konsolHatalari).isEmpty();
    }

    @Test
    void ilerlemeKoduDiyaloguKodGosterir() {
        anaSayfa("/");
        sayfa.click("#kod-ac");
        sayfa.waitForFunction("() => { const v = document.getElementById('kod-deger').value;"
                + " return v && v !== 'yükleniyor…'; }");
        assertThat(sayfa.locator("#kod-dialog").isVisible()).isTrue();
        assertThat(sayfa.inputValue("#kod-deger")).matches("[A-Za-z0-9_-]{16,}");
        assertThat(sayfa.locator("#kod-mesaj").isHidden()).isTrue();
    }

    // ---------- anahtar kartı ----------

    @Test
    void anahtarKaydedilirVeKaldirilir() {
        anaSayfa("/");
        assertThat(sayfa.locator("#chat-girdi").isHidden()).isTrue();
        sayfa.fill("#anahtar-girdi", "yanlis-anahtar");
        sayfa.click(".anahtar-kaydet");
        assertThat(sayfa.locator(".anahtar-hata").textContent()).contains("sk-or-");

        sayfa.fill("#anahtar-girdi", TEST_ANAHTARI);
        sayfa.click(".anahtar-kaydet");
        sayfa.locator("#chat-girdi").waitFor();
        assertThat(sayfa.locator("#chat-girdi").isEnabled()).isTrue();
        assertThat(sayfa.locator(".anahtar-kart").count()).isZero();
        assertThat(sayfa.locator("#anahtar-kaldir").isVisible()).isTrue();
        assertThat(depo("sessionStorage", "aio.openrouter")).isEqualTo(TEST_ANAHTARI);
        assertThat(depo("localStorage", "aio.openrouter")).as("hatırla işaretsiz → kalıcı depoya yazılmaz").isNull();

        sayfa.click("#anahtar-kaldir");
        sayfa.locator(".anahtar-kart").waitFor();
        assertThat(depo("sessionStorage", "aio.openrouter")).isNull();
        assertThat(sayfa.locator("#chat-girdi").isHidden()).isTrue();
        assertThat(konsolHatalari).isEmpty();
    }

    @Test
    void sohbetHataMetniHtmlOlarakDegilMetinOlarakBasilir() {
        baglam.addInitScript("try { sessionStorage.setItem('aio.openrouter', '" + TEST_ANAHTARI + "'); } catch (e) {}");
        String saldiri = "<img src=x onerror=\"window.__xss=1\"><b>kalin</b>";
        sayfa.route(Pattern.compile(".*/api/sohbet$"), r -> {
            if (!"POST".equals(r.request().method())) {
                r.fallback();
                return;
            }
            r.fulfill(new Route.FulfillOptions().setStatus(500).setContentType("application/json")
                    .setBody("{\"detay\":" + jsonDizesi(saldiri) + "}"));
        });
        anaSayfa("/");
        sayfa.fill("#chat-girdi", "merhaba öğretmen");
        sayfa.click("#chat-gonder");
        var hata = sayfa.locator(".msg.asistan", new Page.LocatorOptions().setHasText("Hata:"));
        hata.waitFor();
        assertThat(hata.textContent()).contains(saldiri);
        assertThat(sayfa.locator("#chat-mesajlar img").count()).isZero();
        assertThat(sayfa.locator("#chat-mesajlar b").count()).isZero();
        assertThat(sayfa.evaluate("() => typeof window.__xss")).isEqualTo("undefined");
    }

    // ---------- "OpenRouter ile bağlan" (OAuth PKCE, Faz 13) ----------

    /** openrouter.ai taklidi: yetkilendirme sayfası callback'e kod + state ile döner; anahtar ucu {@code key} verir. */
    private Map<String, String> openRouterTaklidi(int anahtarDurumu, String anahtarGovdesi,
                                                  AtomicReference<String> anahtarIstegi) {
        Map<String, String> yetki = new HashMap<>();
        baglam.route(Pattern.compile("^https://openrouter\\.ai/auth\\?.*"), r -> {
            yetki.putAll(sorgu(r.request().url()));
            String donus = yetki.get("callback_url") + "?code=TEST-KOD-123&state="
                    + java.net.URLEncoder.encode(yetki.getOrDefault("state", ""), StandardCharsets.UTF_8);
            r.fulfill(new Route.FulfillOptions().setStatus(302).setHeaders(Map.of("Location", donus)));
        });
        baglam.route("https://openrouter.ai/api/v1/auth/keys", r -> {
            Map<String, String> cors = Map.of("Access-Control-Allow-Origin", taban(),
                    "Access-Control-Allow-Methods", "POST", "Access-Control-Allow-Headers", "content-type");
            if ("OPTIONS".equals(r.request().method())) {
                r.fulfill(new Route.FulfillOptions().setStatus(204).setHeaders(cors));
                return;
            }
            anahtarIstegi.set(r.request().postData());
            r.fulfill(new Route.FulfillOptions().setStatus(anahtarDurumu).setHeaders(cors)
                    .setContentType("application/json").setBody(anahtarGovdesi));
        });
        return yetki;
    }

    @Test
    void openRouterIleBaglanPkceAkisiAnahtariYalnizTarayicidaSaklar() throws Exception {
        AtomicReference<String> anahtarIstegi = new AtomicReference<>();
        Map<String, String> yetki = openRouterTaklidi(200, "{\"key\":\"" + TEST_ANAHTARI + "\"}", anahtarIstegi);
        anaSayfa("/");
        sayfa.check("#anahtar-hatirla-kutu"); // "Bu cihazda hatırla" yönlendirmeden ÖNCE seçilir
        sayfa.click("#openrouter-baglan");
        sayfa.waitForSelector("#chat-girdi:not([hidden])");
        sayfa.waitForLoadState(LoadState.NETWORKIDLE);

        // yetkilendirme isteği: S256 meydan okuması + state + kök callback
        assertThat(yetki.get("callback_url")).isEqualTo(taban() + "/");
        assertThat(yetki.get("code_challenge_method")).isEqualTo("S256");
        assertThat(yetki.get("state")).matches("[A-Za-z0-9_-]{16,}");
        // anahtar değişimi: code + code_verifier + S256; meydan = base64url(SHA-256(doğrulayıcı))
        assertThat(anahtarIstegi.get()).as("anahtar ucuna POST gitti").isNotNull();
        var govde = new com.fasterxml.jackson.databind.ObjectMapper().readTree(anahtarIstegi.get());
        assertThat(govde.get("code").asText()).isEqualTo("TEST-KOD-123");
        assertThat(govde.get("code_challenge_method").asText()).isEqualTo("S256");
        String dogrulayici = govde.get("code_verifier").asText();
        assertThat(dogrulayici).matches("[A-Za-z0-9_-]{43,128}");
        byte[] ozet = MessageDigest.getInstance("SHA-256").digest(dogrulayici.getBytes(StandardCharsets.US_ASCII));
        assertThat(yetki.get("code_challenge"))
                .isEqualTo(Base64.getUrlEncoder().withoutPadding().encodeToString(ozet));

        // anahtar "hatırla" seçimine göre kalıcı depoda; doğrulayıcı/state temizlendi; URL'den kod silindi
        assertThat(depo("localStorage", "aio.openrouter")).isEqualTo(TEST_ANAHTARI);
        assertThat(depo("sessionStorage", "aio.openrouter")).isNull();
        assertThat(sayfa.evaluate("() => Object.keys(sessionStorage).filter(k => k.startsWith('aio.oauth')).length"))
                .isEqualTo(0);
        assertThat(sayfa.url()).isEqualTo(taban() + "/");
        assertThat(sayfa.locator("#chat-girdi").isEnabled()).isTrue();

        // anahtar sunucumuza hiçbir istekte gitmedi; konsola yazılmadı; gerçek ağa çıkılmadı
        synchronized (yerelIstekler) {
            for (Request r : yerelIstekler) {
                assertThat(r.url()).doesNotContain(TEST_ANAHTARI);
                assertThat(r.headers().toString()).as(r.url()).doesNotContain(TEST_ANAHTARI);
                assertThat(String.valueOf(r.postData())).as(r.url()).doesNotContain(TEST_ANAHTARI);
            }
        }
        assertThat(konsolMesajlari).noneMatch(m -> m.contains(TEST_ANAHTARI));
        assertThat(konsolHatalari).as("konsol hataları (connect-src CSP dahil)").isEmpty();
        assertThat(disIstekler).isEmpty();
    }

    @Test
    void openRouterAnahtarVermezseKartTurkceHataGosterir() {
        AtomicReference<String> anahtarIstegi = new AtomicReference<>();
        openRouterTaklidi(403, "{\"error\":\"invalid code\"}", anahtarIstegi);
        anaSayfa("/");
        sayfa.click("#openrouter-baglan");
        sayfa.waitForSelector(".anahtar-hata");
        assertThat(sayfa.locator(".anahtar-hata").textContent()).contains("OpenRouter");
        assertThat(anahtarIstegi.get()).isNotNull();
        assertThat(depo("sessionStorage", "aio.openrouter")).isNull();
        assertThat(depo("localStorage", "aio.openrouter")).isNull();
        assertThat(sayfa.url()).isEqualTo(taban() + "/");
        assertThat(disIstekler).isEmpty();
    }

    @Test
    void stateUyusmazsaAnahtarIstenmez() {
        AtomicReference<String> anahtarIstegi = new AtomicReference<>();
        openRouterTaklidi(200, "{\"key\":\"" + TEST_ANAHTARI + "\"}", anahtarIstegi);
        anaSayfa("/");
        sayfa.evaluate("() => { sessionStorage.setItem('aio.oauth.dogrulayici', 'x'.repeat(64));"
                + " sessionStorage.setItem('aio.oauth.durum', 'beklenen-durum-degeri'); }");
        anaSayfa("/?code=SALDIRGAN-KODU&state=baska-durum");
        assertThat(sayfa.locator(".anahtar-hata").textContent()).contains("doğrulanamadı");
        assertThat(anahtarIstegi.get()).as("state uyuşmazken anahtar ucu çağrılmaz").isNull();
        assertThat(depo("sessionStorage", "aio.openrouter")).isNull();
        assertThat(sayfa.url()).isEqualTo(taban() + "/");
    }

    /** İnceleme M1: doğrulayıcı yokken (başka sekmede başlatılmış) gelen kod URL'de kalmaz, kart uyarır. */
    @Test
    void dogrulayiciYokkenGelenKodUrldenSilinirVeUyarilir() {
        AtomicReference<String> anahtarIstegi = new AtomicReference<>();
        openRouterTaklidi(200, "{\"key\":\"" + TEST_ANAHTARI + "\"}", anahtarIstegi);
        anaSayfa("/?code=BASKA-SEKME-KODU&state=bir-durum&hafta=1");
        assertThat(sayfa.locator(".anahtar-hata").textContent()).contains("başka sekmede");
        assertThat(sayfa.url()).as("code/state silinir, diğer parametreler kalır").isEqualTo(taban() + "/?hafta=1");
        assertThat(anahtarIstegi.get()).isNull();
        assertThat(depo("sessionStorage", "aio.openrouter")).isNull();
    }

    /** İnceleme M3: /api/ozellikler alınamazsa (site bilinemez) OAuth dönüşü sessiz kalmaz, kod da URL'de kalmaz. */
    @Test
    void ozelliklerAlinamazsaOauthDonusuUyarir() {
        AtomicReference<String> anahtarIstegi = new AtomicReference<>();
        openRouterTaklidi(200, "{\"key\":\"" + TEST_ANAHTARI + "\"}", anahtarIstegi);
        anaSayfa("/");
        sayfa.evaluate("() => { sessionStorage.setItem('aio.oauth.dogrulayici', 'x'.repeat(64));"
                + " sessionStorage.setItem('aio.oauth.durum', 'durum-1'); }");
        sayfa.route(Pattern.compile(".*/api/ozellikler$"), r -> r.fulfill(new Route.FulfillOptions().setStatus(500)
                .setContentType("application/json").setBody("{}")));
        anaSayfa("/?code=GECERLI-KOD&state=durum-1");
        sayfa.locator("#chat-mesajlar .msg.sistem", new Page.LocatorOptions().setHasText("başka sekmede")).waitFor();
        assertThat(sayfa.url()).isEqualTo(taban() + "/");
        assertThat(anahtarIstegi.get()).isNull();
        assertThat(sayfa.evaluate("() => Object.keys(sessionStorage).filter(k => k.startsWith('aio.oauth')).length"))
                .isEqualTo(0);
    }

    /**
     * İnceleme M2: OpenRouter'dan "Geri" ile dönülünce sayfa bfcache'ten gelir, tıklanmış bağlan düğmesi pasif kalırdı.
     * Playwright bfcache'i kapattığından olay doğrudan tetiklenir ({@code pageshow}, {@code persisted: true}).
     */
    @Test
    void geriIleDonulunceBaglanDugmesiYenidenAcilir() {
        anaSayfa("/");
        sayfa.evaluate("() => { document.getElementById('openrouter-baglan').disabled = true; }");
        sayfa.evaluate("() => window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }))");
        assertThat(sayfa.locator("#openrouter-baglan").isEnabled()).isTrue();
        assertThat(konsolHatalari).isEmpty();
    }

    // ---------- Prompt Atölyesi: ai:true/false bayrağı ----------

    /** Sol menüden atölyeyi açar ve seçili bölümün kartlarını bekler. */
    private void atolyeAc() {
        sayfa.click("#katman-haritasi .layer.atolye");
        sayfa.locator(".atolye-kart").first().waitFor();
    }

    /** Sitede (sunucuAi=false) davranış değişmez: hiçbir kartta değerlendir düğmesi de not da yok. */
    @Test
    void sitedeAtolyedeAiDegerlendirDugmesiVeNotuYok() {
        anaSayfa("/");
        atolyeAc();
        assertThat(sayfa.locator(".week-goal").textContent()).contains("son \"birleştir\" alıştırmasını");
        assertThat(sayfa.locator(".atolye-ai-btn").count()).isZero();
        assertThat(sayfa.locator(".atolye-ai-not").count()).isZero();
        assertThat(konsolHatalari).isEmpty();
    }

    /**
     * Yerel mod (sunucuAi=true; {@code /api/ozellikler} taklit edilir): düğme yalnız ai:true alıştırmada (bölümün son
     * "birleştir"i), ai:false alıştırmalarda düğme yerine kısa not.
     */
    @Test
    void yerelModdaDegerlendirDugmesiYalnizAiAcikAlistirmada() {
        sayfa.route(Pattern.compile(".*/api/ozellikler$"), r -> r.fulfill(new Route.FulfillOptions().setStatus(200)
                .setContentType("application/json").setBody("{\"site\":false,\"sunucuAi\":true,\"mulakat\":false}")));
        anaSayfa("/");
        atolyeAc();
        var kartlar = sayfa.locator(".atolye-kart");
        int adet = kartlar.count();
        assertThat(adet).isGreaterThan(1);
        assertThat(sayfa.locator(".atolye-ai-btn").count()).isEqualTo(1);
        assertThat(kartlar.nth(adet - 1).locator(".atolye-ai-btn").count()).as("son kartta düğme").isEqualTo(1);
        assertThat(kartlar.nth(0).locator(".atolye-ai-btn").count()).as("ilk kartta düğme yok").isZero();
        assertThat(sayfa.locator(".atolye-ai-not").count()).isEqualTo(adet - 1);
        assertThat(kartlar.nth(0).locator(".atolye-ai-not").textContent()).contains("bölümün son alıştırmasını");
    }

    // ---------- yardımcılar ----------

    private static Map<String, String> sorgu(String url) {
        Map<String, String> m = new HashMap<>();
        String q = URI.create(url).getRawQuery();
        if (q == null) {
            return m;
        }
        for (String parca : q.split("&")) {
            int i = parca.indexOf('=');
            String ad = URLDecoder.decode(i < 0 ? parca : parca.substring(0, i), StandardCharsets.UTF_8);
            m.put(ad, i < 0 ? "" : URLDecoder.decode(parca.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    private static String jsonDizesi(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
