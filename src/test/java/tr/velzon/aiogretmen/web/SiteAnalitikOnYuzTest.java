package tr.velzon.aiogretmen.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Faz 12 (ADR-0019) ön yüz sözleşmesi — SEO etiketleri ve GA4 betiği (statik dosya içerik iddiaları; tarayıcı
 * davranışı elle doğrulanır). Bekçiler: CSP yalnız listelenen Google Analytics alanlarıyla genişler, satır içi betik
 * yok, {@code analitik.js} anahtar deposuna / ziyaretçi kimliğine / çerezlere dokunmaz ve Google'a kullanıcı
 * kimliği ya da metin göndermez.
 */
class SiteAnalitikOnYuzTest {

    private static final Path STATIK = Path.of("src/main/resources/static");

    private static String oku(String ad) throws IOException {
        return Files.readString(STATIK.resolve(ad));
    }

    private static String csp(String index) {
        Matcher m = Pattern.compile("http-equiv=\"Content-Security-Policy\" content=\"([^\"]*)\"").matcher(index);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void cspYalnizGaAlanlariylaGenisler() throws IOException {
        assertThat(csp(oku("index.html"))).isEqualTo("default-src 'self'; "
                + "script-src 'self' https://www.googletagmanager.com; "
                + "connect-src 'self' https://*.google-analytics.com https://*.analytics.google.com "
                + "https://*.googletagmanager.com; "
                + "img-src 'self' data: https://*.google-analytics.com https://*.googletagmanager.com");
    }

    /** Çalıştırılabilir satır içi betik yok: her {@code <script>} ya {@code src}'li ya da JSON-LD veri bloğu. */
    @Test
    void satirIciBetikYok() throws IOException {
        Matcher m = Pattern.compile("<script([^>]*)>").matcher(oku("index.html"));
        int n = 0;
        while (m.find()) {
            n++;
            String oz = m.group(1);
            assertThat(oz.contains(" src=\"") || oz.equals(" type=\"application/ld+json\"")).as(oz).isTrue();
        }
        assertThat(n).isEqualTo(4); // analitik.js, app.js, mulakat.js + JSON-LD
        String index = oku("index.html");
        assertThat(index.indexOf("<script src=\"analitik.js\">")).isPositive()
                .isLessThan(index.indexOf("<script src=\"app.js\">"));
    }

    @Test
    void seoEtiketleriTurkce() throws IOException {
        String index = oku("index.html");
        assertThat(index).contains("<html lang=\"tr\">")
                .containsPattern("<title>SokratAI[^<]+</title>")
                .containsPattern("<meta name=\"description\" content=\"[^\"]{70,170}\">")
                .contains("<meta property=\"og:title\"").contains("<meta property=\"og:description\"")
                .contains("<meta property=\"og:type\" content=\"website\">")
                .contains("<meta property=\"og:locale\" content=\"tr_TR\">")
                .contains("<meta property=\"og:site_name\" content=\"SokratAI\">")
                .contains("<meta name=\"twitter:card\" content=\"summary\">")
                .contains("<meta name=\"theme-color\"")
                .contains("<link rel=\"icon\" href=\"favicon.svg\" type=\"image/svg+xml\">")
                .contains("<!-- seo:alan -->");
        assertThat(Files.exists(STATIK.resolve("favicon.svg"))).isTrue();
    }

    @Test
    void jsonLdKursSemasi() throws IOException {
        String index = oku("index.html");
        int bas = index.indexOf("<script type=\"application/ld+json\">");
        assertThat(bas).isPositive();
        String json = index.substring(index.indexOf('>', bas) + 1, index.indexOf("</script>", bas));
        JsonNode kurs = new ObjectMapper().readTree(json);
        assertThat(kurs.path("@context").asText()).isEqualTo("https://schema.org");
        assertThat(kurs.path("@type").asText()).isEqualTo("Course");
        assertThat(kurs.path("inLanguage").asText()).isEqualTo("tr");
        assertThat(kurs.path("isAccessibleForFree").asBoolean()).isTrue();
        assertThat(kurs.path("provider").path("name").asText()).isEqualTo("SokratAI");
        assertThat(kurs.path("name").asText()).isNotBlank();
        assertThat(kurs.path("description").asText()).isNotBlank();
    }

    /** analitik.js ziyaretçi anahtarını, kimliğini, ilerleme kodunu ve çerezleri hiç bilmez. */
    @Test
    void analitikKisiselVeriyeDokunmaz() throws IOException {
        String js = oku("analitik.js");
        for (String yasak : new String[]{"aio.openrouter", "anahtarOku", "ANAHTAR", "localStorage", "sessionStorage",
                "document.cookie", "aio_ziyaretci", "XSRF", "user_id", "userId", "X-OpenRouter", "ilerleme",
                "/api/", "location.search", "location.href", "eval(", "innerHTML"}) {
            assertThat(js).as(yasak).doesNotContain(yasak);
        }
        assertThat(js).contains("https://www.googletagmanager.com/gtag/js?id=")
                .contains("send_page_view: false")
                .contains("allow_google_signals: false")
                .contains("allow_ad_personalization_signals: false")
                .contains("/^G-[A-Z0-9]{4,20}$/");
    }

    /** Sanal sayfa görüntülemeleri ve olaylar app.js'te; olaylara metin/anahtar parametresi girmez. */
    @Test
    void sanalSayfaVeOlaylarKisiselVeriTasimaz() throws IOException {
        String app = oku("app.js");
        assertThat(app).contains("analitikBaslat(oz.gaOlcumKimligi)")
                .contains("analitikSayfa(\"Ana sayfa\", \"/\")")
                .contains("\"/hafta/\" + ")
                .contains("\"/gun/\" + ")
                .contains("\"/kaynak/\" + ")
                .contains("analitikSayfa(\"Prompt Atölyesi\", \"/atolye\")");
        // olay adı sabit bir dize; adın ARDINDAKİ parametrelerde metin/anahtar/kimlik değişkeni olmamalı
        Matcher m = Pattern.compile("analitikOlay\\(\"[a-z_]+\"([^;]*)\\);").matcher(app);
        int n = 0;
        while (m.find()) {
            n++;
            String arg = m.group(1);
            assertThat(arg).as(arg).doesNotContain("metin").doesNotContain("anahtar").doesNotContain("icerik")
                    .doesNotContain("kod").doesNotContain("kimlik");
        }
        assertThat(n).isGreaterThanOrEqualTo(3);
        assertThat(app).contains("analitikOlay(\"sohbet_mesaji\")").contains("analitikOlay(\"anahtar_eklendi\")")
                .contains("analitikOlay(\"gorev_tamamlandi\"");
    }

    @Test
    void robotsVeSitemapStatikDegil() {
        // dinamik (site profili, temel URL'den) — statik kopya, ayarlanmamış alan adını sızdırmasın
        assertThat(Files.exists(STATIK.resolve("robots.txt"))).isFalse();
        assertThat(Files.exists(STATIK.resolve("sitemap.xml"))).isFalse();
    }

    @Test
    void belgelerOrtamDegiskenleriniAnlatir() throws IOException {
        String kurulum = Files.readString(Path.of("docs/kurulum-site.md"));
        assertThat(kurulum).contains("APP_SITE_GA_OLCUM_KIMLIGI").contains("APP_SITE_TEMEL_URL")
                .contains("SEO ve analitik");
        assertThat(Files.readString(Path.of("deploy/site.env.ornek")))
                .contains("APP_SITE_GA_OLCUM_KIMLIGI").contains("APP_SITE_TEMEL_URL");
        assertThat(Files.exists(Path.of("docs/adr/0019-google-analytics-4-ve-seo.md"))).isTrue();
    }
}
