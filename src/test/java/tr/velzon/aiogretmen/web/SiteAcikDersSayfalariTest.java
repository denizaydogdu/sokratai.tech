package tr.velzon.aiogretmen.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tr.velzon.aiogretmen.config.ZiyaretciCerezi;
import tr.velzon.aiogretmen.service.AcikDersService;
import tr.velzon.aiogretmen.types.Gun;
import tr.velzon.aiogretmen.types.Hafta;
import tr.velzon.aiogretmen.types.Kaynak;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Faz 12 (ADR-0020): herkese açık, JS'siz ders sayfaları, genişletilmiş site haritası, llms.txt — temel URL'li site.
 * Gerçek {@code content/} ile çalışır (12 hafta, tüm kaynaklar).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:siteacikders;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.hafiza-dir=target/test-hafiza-siteacikders",
        "app.rapor-dir=target/test-raporlar-siteacikders",
        "app.site.temel-url=https://ornek.example"
})
@ActiveProfiles("site")
@AutoConfigureMockMvc
class SiteAcikDersSayfalariTest {

    private static final String KOK = "https://ornek.example";
    private static final String CEREZ = "AAAAAAAAAAAAAAAAAAAAAa";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AcikDersService icerik;

    private MvcResult sayfa(String yol) throws Exception {
        MvcResult r = mvc.perform(get(yol).accept("text/html")).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentType()).as(yol).startsWith("text/html");
        assertThat(r.getResponse().getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        return r;
    }

    private static JsonNode jsonLd(String html) throws Exception {
        Matcher m = Pattern.compile("<script type=\"application/ld\\+json\">(.*?)</script>", Pattern.DOTALL)
                .matcher(html);
        assertThat(m.find()).as("JSON-LD bloğu").isTrue();
        return JSON.readTree(m.group(1));
    }

    @Test
    void gunSayfasiDersMetniGorevlerBaslikCanonicalVeJsonLdTasir() throws Exception {
        Hafta h = icerik.hafta(1).orElseThrow();
        Gun g = h.gunler().get(0);
        String html = sayfa("/hafta/1/gun/1").getResponse().getContentAsString();

        assertThat(html).contains("<html lang=\"tr\">")
                .containsPattern("<title>[^<]*Hafta 1, Gün 1 \\| SokratAI</title>")
                .containsPattern("<meta name=\"description\" content=\"[^\"]{40,260}\">")
                .contains("<link rel=\"canonical\" href=\"" + KOK + "/hafta/1/gun/1\">")
                .contains("<meta property=\"og:url\" content=\"" + KOK + "/hafta/1/gun/1\">")
                .contains("<meta property=\"og:type\" content=\"article\">")
                .contains("<link rel=\"stylesheet\" href=\"/style.css\">")
                .contains("href=\"/?hafta=1&amp;gun=1\"").contains("Bu dersi etkileşimli çalış →")
                .contains("<nav class=\"acik-yol\"").contains("href=\"/dersler\"").contains("href=\"/hafta/1\"")
                .contains("<h2>Görevler</h2>")
                .contains(AcikSayfa.kac(g.gorevler().get(0)))
                .contains("href=\"/hafta/1/gun/2\"");
        // ders metni sunucuda çizilmiş (JS yok): gün dosyasının ilk alt başlığı HTML'de
        assertThat(html).contains("<h3>Bugün ne öğreneceksin?</h3>");
        // satır içi çalıştırılabilir betik yok
        Matcher m = Pattern.compile("<script([^>]*)>").matcher(html);
        while (m.find()) {
            assertThat(m.group(1)).isEqualTo(" type=\"application/ld+json\"");
        }
        assertThat(html).contains("script-src 'none'");

        JsonNode ld = jsonLd(html);
        assertThat(ld.path("@context").asText()).isEqualTo("https://schema.org");
        assertThat(ld.path("@type").asText()).isEqualTo("LearningResource");
        assertThat(ld.path("inLanguage").asText()).isEqualTo("tr");
        assertThat(ld.path("isAccessibleForFree").asBoolean()).isTrue();
        assertThat(ld.path("url").asText()).isEqualTo(KOK + "/hafta/1/gun/1");
        assertThat(ld.path("isPartOf").path("@type").asText()).isEqualTo("Course");
        assertThat(ld.path("isPartOf").path("url").asText()).isEqualTo(KOK + "/dersler");
    }

    @Test
    void haftaDerslerVeKaynakSayfalari() throws Exception {
        String hafta = sayfa("/hafta/3").getResponse().getContentAsString();
        Hafta h3 = icerik.hafta(3).orElseThrow();
        assertThat(hafta).contains("<title>Hafta 3: " + AcikSayfa.kac(h3.baslik()) + " | SokratAI</title>")
                .contains("<link rel=\"canonical\" href=\"" + KOK + "/hafta/3\">")
                .contains("href=\"/?hafta=3\"");
        for (Gun g : h3.gunler()) {
            assertThat(hafta).contains("href=\"/hafta/3/gun/" + g.gun() + "\"");
        }
        assertThat(jsonLd(hafta).path("hasPart").size()).isEqualTo(h3.gunler().size());

        String dersler = sayfa("/dersler").getResponse().getContentAsString();
        assertThat(dersler).contains("<link rel=\"canonical\" href=\"" + KOK + "/dersler\">");
        for (Hafta h : icerik.haftalar()) {
            assertThat(dersler).contains("href=\"/hafta/" + h.no() + "\"");
        }
        for (Kaynak k : icerik.kaynaklar()) {
            assertThat(dersler).contains("href=\"/kaynak/" + k.slug() + "\"");
        }
        JsonNode kurs = jsonLd(dersler);
        assertThat(kurs.path("@type").asText()).isEqualTo("Course");
        assertThat(kurs.path("hasPart").size()).isEqualTo(icerik.haftalar().size());

        String kaynak = sayfa("/kaynak/00-baslangic-kilavuzu").getResponse().getContentAsString();
        assertThat(kaynak).contains("<link rel=\"canonical\" href=\"" + KOK + "/kaynak/00-baslangic-kilavuzu\">")
                .contains("<figure class=\"oku-sema\"><svg");
        assertThat(jsonLd(kaynak).path("@type").asText()).isEqualTo("LearningResource");
    }

    /** Tüm başlıklar benzersiz (her sayfa ayrı dizinlenir). */
    @Test
    void basliklarBenzersiz() throws Exception {
        Set<String> basliklar = new HashSet<>();
        List<String> yollar = new java.util.ArrayList<>(List.of("/dersler"));
        for (Hafta h : icerik.haftalar()) {
            yollar.add("/hafta/" + h.no());
            h.gunler().forEach(g -> yollar.add("/hafta/" + h.no() + "/gun/" + g.gun()));
        }
        icerik.kaynaklar().forEach(k -> yollar.add("/kaynak/" + k.slug()));
        Pattern baslik = Pattern.compile("<title>([^<]+)</title>");
        for (String yol : yollar) {
            Matcher m = baslik.matcher(sayfa(yol).getResponse().getContentAsString());
            assertThat(m.find()).as(yol).isTrue();
            assertThat(basliklar.add(m.group(1))).as("yinelenen başlık: " + m.group(1)).isTrue();
        }
    }

    @Test
    void acikSayfalarCerezYazmazZiyaretciSatiriAcmazOnbelleklenir() throws Exception {
        int once = jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci", Integer.class);
        for (String yol : List.of("/dersler", "/hafta/1", "/hafta/1/gun/1", "/kaynak/00-baslangic-kilavuzu",
                "/llms.txt", "/llms-full.txt", "/style.css")) {
            MvcResult r = mvc.perform(get(yol).cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ)))
                    .andExpect(status().isOk()).andReturn();
            assertThat(r.getResponse().getHeaders("Set-Cookie")).as(yol + " çerez yazmaz").isEmpty();
            assertThat(r.getRequest().getAttribute(ZiyaretciCerezi.ISTEK_OZNITELIGI)).as(yol).isNull();
            assertThat(r.getResponse().getHeader("X-Robots-Tag")).as(yol).isNull();
            if (!yol.equals("/style.css")) {
                assertThat(r.getResponse().getHeader("Cache-Control")).as(yol)
                        .isEqualTo("max-age=3600, public");
            }
        }
        MvcResult bas = mvc.perform(head("/hafta/2/gun/3")).andExpect(status().isOk()).andReturn();
        assertThat(bas.getResponse().getHeaders("Set-Cookie")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci", Integer.class)).isEqualTo(once);
    }

    @Test
    void bilinmeyenSayfaHtml404() throws Exception {
        for (String yol : List.of("/hafta/99", "/hafta/0", "/hafta/abc", "/hafta/1/gun/9", "/hafta/99/gun/1",
                "/hafta/1/gun/x", "/kaynak/yok-boyle-kaynak", "/kaynak/ABC", "/hafta/-1")) {
            MvcResult r = mvc.perform(get(yol)).andExpect(status().isNotFound()).andReturn();
            assertThat(r.getResponse().getContentType()).as(yol).startsWith("text/html");
            assertThat(r.getResponse().getContentAsString()).as(yol).contains("Sayfa bulunamadı")
                    .doesNotContain("{\"hata\"");
            assertThat(r.getResponse().getHeaders("Set-Cookie")).as(yol).isEmpty();
        }
    }

    @Test
    void mulakatIcerigiHicbirYerdeYok() throws Exception {
        for (String yol : List.of("/sitemap.xml", "/llms.txt", "/llms-full.txt", "/dersler")) {
            String govde = mvc.perform(get(yol)).andReturn().getResponse().getContentAsString();
            // mülakat sayfası/URL'si yok (kaynak metni düz yazıda repo yolunu anabilir — bağlantı değil)
            assertThat(govde).as(yol).doesNotContain(KOK + "/mulakat").doesNotContain("href=\"/mulakat")
                    .doesNotContain("](/mulakat").doesNotContain(KOK + "/atolye")
                    .doesNotContain("Mülakat Hazırlığı")
                    .doesNotContain("Java Platformu: JDK / JRE / JVM"); // bir mülakat konusunun başlığı
        }
        mvc.perform(get("/kaynak/mulakat")).andExpect(status().isNotFound());
        mvc.perform(get("/kaynak/01-temel-java")).andExpect(status().isNotFound());
    }

    @Test
    void sitemapTumAcikSayfalariListeler() throws Exception {
        String xml = mvc.perform(get("/sitemap.xml")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        int gunSayisi = icerik.haftalar().stream().mapToInt(h -> h.gunler().size()).sum();
        int beklenen = 1 + 1 + icerik.haftalar().size() + gunSayisi + icerik.kaynaklar().size();
        assertThat(xml.split("<loc>", -1)).hasSize(beklenen + 1);
        assertThat(xml).contains("<loc>" + KOK + "/</loc>").contains("<loc>" + KOK + "/dersler</loc>")
                .contains("<loc>" + KOK + "/hafta/12/gun/5</loc>")
                .contains("<loc>" + KOK + "/kaynak/00-baslangic-kilavuzu</loc><lastmod>2026-07-15</lastmod>")
                // ders dosyası zamanı dağıtımda ilerler → ders sayfalarında lastmod YOK
                .contains("<loc>" + KOK + "/hafta/1/gun/1</loc></url>")
                .contains("<loc>" + KOK + "/hafta/1</loc></url>")
                .contains("<loc>" + KOK + "/dersler</loc></url>");
    }

    @Test
    void llmsTxtBicimi() throws Exception {
        MvcResult r = mvc.perform(get("/llms.txt")).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentType()).startsWith("text/plain");
        String t = r.getResponse().getContentAsString();
        assertThat(t).startsWith("# SokratAI\n\n> ");
        assertThat(t).contains("\n## Program\n").contains("\n## Kaynaklar\n").contains("\n## Hakkında\n")
                .contains("OpenRouter").contains("(" + KOK + "/llms-full.txt)");
        assertThat(t.indexOf("## Program")).isLessThan(t.indexOf("## Kaynaklar"));
        for (Hafta h : icerik.haftalar()) {
            assertThat(t).containsPattern("\\n- \\[Hafta " + h.no() + ": [^\\]]+\\]\\(" + Pattern.quote(KOK + "/hafta/" + h.no()) + "\\): .+");
        }
        for (Kaynak k : icerik.kaynaklar()) {
            assertThat(t).contains("](" + KOK + "/kaynak/" + k.slug() + ")");
        }
    }

    @Test
    void llmsFullTumMetinleriTasirSvgsiz() throws Exception {
        MvcResult r = mvc.perform(get("/llms-full.txt")).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentType()).startsWith("text/plain");
        assertThat(r.getResponse().getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        String t = r.getResponse().getContentAsString();
        assertThat(t).startsWith("# SokratAI").doesNotContain("```svg").doesNotContain("<svg");
        assertThat(t).contains("URL: " + KOK + "/hafta/12/gun/5").contains("Bugün ne öğreneceksin?");
        for (Kaynak k : icerik.kaynaklar()) {
            assertThat(t).contains("URL: " + KOK + "/kaynak/" + k.slug());
        }
    }

    @Test
    void robotsAiBotlarinaAcikIzin() throws Exception {
        String t = mvc.perform(get("/robots.txt")).andReturn().getResponse().getContentAsString();
        assertThat(t).startsWith("User-agent: *\nAllow: /\n").doesNotContain("Disallow");
        for (String bot : List.of("GPTBot", "OAI-SearchBot", "ClaudeBot", "Claude-SearchBot", "PerplexityBot",
                "Google-Extended", "Bingbot", "Applebot")) {
            assertThat(t).contains("User-agent: " + bot + "\nAllow: /\n");
        }
        assertThat(t).contains("Sitemap: " + KOK + "/sitemap.xml");
    }

    @Test
    void anaSayfaDersIcerigineBaglanirNoscriptTasir() throws Exception {
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<a class=\"btn btn-ikincil ders-icerik-bag\" href=\"/dersler\">")
                .contains("<noscript>").containsPattern("(?s)<noscript>.*href=\"/dersler\".*</noscript>");
    }

    /** Derin bağlantı (?hafta=N&gun=G) yalnız tam sayı kabul eder ve değeri DOM'a metin olarak yazmaz. */
    @Test
    void appJsDerinBagYalnizTamSayi() throws Exception {
        String app = new org.springframework.core.io.ClassPathResource("static/app.js")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        int bas = app.indexOf("function derinBag()");
        assertThat(bas).isPositive();
        String islev = app.substring(bas, app.indexOf("\n}\n", bas));
        assertThat(islev).contains("/^[0-9]{1,2}$/.test(h)").contains("/^[0-9]{1,2}$/.test(g)")
                .contains("durum.ozetler.some((o) => o.no === hafta)").doesNotContain("innerHTML");
        assertThat(app).contains("const bag = derinBag();");
    }

    /** Repo-göreli bağlantılar sitede kırık kalmaz: kaynak→kaynak {@code /kaynak/x}, diğerleri GitHub; ölçüm yoksa betik yok. */
    @Test
    void sayfalardaGoreliBaglantiVeBetikYok() throws Exception {
        List<String> yollar = new java.util.ArrayList<>(List.of("/dersler"));
        for (Hafta h : icerik.haftalar()) {
            yollar.add("/hafta/" + h.no());
            h.gunler().forEach(g -> yollar.add("/hafta/" + h.no() + "/gun/" + g.gun()));
        }
        icerik.kaynaklar().forEach(k -> yollar.add("/kaynak/" + k.slug()));
        Pattern goreli = Pattern.compile("href=\"(?!https?://|/|#)[^\"]*\"");
        for (String yol : yollar) {
            String html = sayfa(yol).getResponse().getContentAsString();
            Matcher m = goreli.matcher(html);
            boolean var = m.find();
            assertThat(var).as(yol + (var ? " → " + m.group() : "")).isFalse();
            assertThat(html).as(yol).doesNotContain("/s.js").contains("script-src 'none'");
        }
        String html = sayfa("/kaynak/model-ve-caba").getResponse().getContentAsString();
        assertThat(html).containsPattern("href=\"/kaynak/[a-z0-9-]+(#[^\"]*)?\"");
    }
}
