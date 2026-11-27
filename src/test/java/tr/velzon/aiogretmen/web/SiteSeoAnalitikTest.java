package tr.velzon.aiogretmen.web;

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
import tr.velzon.aiogretmen.config.ZiyaretciKullaniciBaglami;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Faz 12 (ADR-0019): SEO uçları ve GA4 ölçüm kimliği — ayarlar VERİLMİŞ site.
 * <ul>
 *   <li>{@code /api/ozellikler} geçerli ölçüm kimliğini {@code gaOlcumKimligi} olarak verir (ziyaretçi kimliği yine yok).</li>
 *   <li>{@code /robots.txt} ve {@code /sitemap.xml} temel URL'den mutlak adres üretir.</li>
 *   <li>Tarayıcı botu uçları ({@code robots.txt}, {@code sitemap.xml}, {@code favicon.svg}) ziyaretçi/XSRF çerezi
 *       YAZMAZ, ziyaretçi satırı AÇMAZ — {@code /api/saglik} gibi.</li>
 *   <li>Ana sayfa canonical + {@code og:url} taşır; ziyaretçi çerezi her zamanki gibi yazılır.</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:siteseo;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.hafiza-dir=target/test-hafiza-siteseo",
        "app.rapor-dir=target/test-raporlar-siteseo",
        "app.site.ga-olcum-kimligi=G-TEST12345",
        "app.site.temel-url=https://ornek.example/"
})
@ActiveProfiles("site")
@AutoConfigureMockMvc
class SiteSeoAnalitikTest {

    private static final String CEREZ = "RRRRRRRRRRRRRRRRRRRRRr";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void ozelliklerGaKimliginiVerir() throws Exception {
        mvc.perform(get("/api/ozellikler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.site").value(true))
                .andExpect(jsonPath("$.gaOlcumKimligi").value("G-TEST12345"))
                .andExpect(jsonPath("$.kullanici").doesNotExist());
    }

    /**
     * I2: {@code /api/} robots.txt'te KAPATILMAZ — Googlebot SPA'yı çizerken {@code /api/**} GET'lerine ihtiyaç duyar.
     * Dizine girmemesini {@code X-Robots-Tag: noindex} yanıt başlığı sağlar.
     */
    @Test
    void robotsTxtHerSeyeIzinVerirSitemapGosterir() throws Exception {
        MvcResult r = mvc.perform(get("/robots.txt")).andExpect(status().isOk()).andReturn();
        String govde = r.getResponse().getContentAsString();
        assertThat(r.getResponse().getContentType()).startsWith("text/plain");
        assertThat(govde).contains("User-agent: *").contains("Allow: /").doesNotContain("Disallow")
                .contains("Sitemap: https://ornek.example/sitemap.xml");
    }

    @Test
    void apiYanitlariNoindexSayfalarDegil() throws Exception {
        for (String yol : List.of("/api/ozellikler", "/api/program", "/api/saglik/canli")) {
            assertThat(mvc.perform(get(yol)).andReturn().getResponse().getHeader("X-Robots-Tag")).as(yol)
                    .isEqualTo("noindex");
        }
        for (String yol : List.of("/", "/robots.txt", "/sitemap.xml")) {
            assertThat(mvc.perform(get(yol)).andReturn().getResponse().getHeader("X-Robots-Tag")).as(yol).isNull();
        }
    }

    /** M3: muafiyet yalnız okuyan yöntemlerde — {@code POST /robots.txt} ziyaretçi çerezi filtresinden geçer. */
    @Test
    void postRobotsCerezFiltresindenMuafDegil() throws Exception {
        MvcResult r = mvc.perform(post("/robots.txt").cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ))).andReturn();
        assertThat(r.getRequest().getAttribute(ZiyaretciCerezi.ISTEK_OZNITELIGI)).isEqualTo(CEREZ);
        assertThat(r.getResponse().getHeaders("Set-Cookie")).anyMatch(c -> c.startsWith(ZiyaretciCerezi.AD + "="));
    }

    @Test
    void sitemapAnaSayfaVeAcikDersSayfalariniListeler() throws Exception {
        MvcResult r = mvc.perform(get("/sitemap.xml")).andExpect(status().isOk()).andReturn();
        String govde = r.getResponse().getContentAsString();
        assertThat(r.getResponse().getContentType()).startsWith("application/xml");
        assertThat(govde).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
                .contains("<loc>https://ornek.example/</loc>");
        // ADR-0020: JS'siz ders sayfaları da listelenir (sayım: SiteAcikDersSayfalariTest)
        assertThat(govde).contains("<loc>https://ornek.example/dersler</loc>");
    }

    @Test
    void botUclariCerezYazmazZiyaretciSatiriAcmaz() throws Exception {
        int once = jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci", Integer.class);
        for (String yol : List.of("/robots.txt", "/sitemap.xml", "/favicon.svg")) {
            MvcResult r = mvc.perform(get(yol).cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ)))
                    .andExpect(status().isOk()).andReturn();
            assertThat(r.getResponse().getHeaders("Set-Cookie")).as(yol + " çerez yazmaz").isEmpty();
            assertThat(r.getRequest().getAttribute(ZiyaretciCerezi.ISTEK_OZNITELIGI)).as(yol).isNull();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci", Integer.class)).isEqualTo(once);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci WHERE id = ?", Integer.class,
                ZiyaretciKullaniciBaglami.ONEK + CEREZ)).isZero();
    }

    @Test
    void anaSayfaCanonicalVeOgUrlTasirCerezYazmayaDevamEder() throws Exception {
        for (String yol : List.of("/", "/index.html")) {
            MvcResult r = mvc.perform(get(yol)).andExpect(status().isOk()).andReturn();
            String html = r.getResponse().getContentAsString();
            assertThat(r.getResponse().getContentType()).startsWith("text/html");
            assertThat(html).as(yol).contains("<link rel=\"canonical\" href=\"https://ornek.example/\">")
                    .contains("<meta property=\"og:url\" content=\"https://ornek.example/\">")
                    .doesNotContain("seo:alan");
            assertThat(html).contains("<title>").contains("<script src=\"app.js\"></script>");
            assertThat(r.getResponse().getHeaders("Set-Cookie"))
                    .anyMatch(c -> c.startsWith(ZiyaretciCerezi.AD + "="));
        }
    }

    /** ADR-0020: ölçüm kimliği ayarlıyken JS'siz ders sayfaları harici /s.js ile sayfa görüntülemesi gönderir. */
    @Test
    void acikSayfalarKimlikliyseHariciBetikVeGaCspTasir() throws Exception {
        for (String yol : List.of("/dersler", "/hafta/1", "/hafta/1/gun/1", "/kaynak/00-baslangic-kilavuzu")) {
            String html = mvc.perform(get(yol)).andExpect(status().isOk()).andReturn().getResponse()
                    .getContentAsString();
            assertThat(html).as(yol).contains("<script src=\"/s.js\" data-k=\"G-TEST12345\" defer></script>")
                    .contains("script-src 'self' https://www.googletagmanager.com")
                    .contains("connect-src 'self' https://*.google-analytics.com")
                    .doesNotContain("script-src 'none'");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<script([^>]*)>").matcher(html);
            while (m.find()) { // satır içi çalıştırılabilir betik yok
                assertThat(m.group(1).contains(" src=\"/s.js\"") || m.group(1).equals(" type=\"application/ld+json\""))
                        .as(m.group(1)).isTrue();
            }
        }
        MvcResult r = mvc.perform(get("/s.js").cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ)))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    /** /s.js: adında ve içinde takipçi listelerine takılan sözcük yok, kişisel veriye dokunmaz, kimliği doğrular. */
    @Test
    void sayfaBetigiTemizVeKimligiDogrular() throws Exception {
        String js = new org.springframework.core.io.ClassPathResource("static/s.js")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String kucuk = js.toLowerCase(java.util.Locale.ROOT);
        for (String yasak : List.of("analitik", "analytics", "olcum", "ölçüm", "track", "document.cookie",
                "localstorage", "sessionstorage", "innerhtml", "location.search", "location.href", "eval(", "user_id")) {
            assertThat(kucuk).as(yasak).doesNotContain(yasak);
        }
        assertThat(js).contains("/^G-[A-Z0-9]{4,20}$/").contains("send_page_view: false")
                .contains("allow_google_signals: false").contains("allow_ad_personalization_signals: false")
                .contains("window.location.origin + window.location.pathname");
    }
}
