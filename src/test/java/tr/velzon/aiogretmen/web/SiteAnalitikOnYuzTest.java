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
 * yok, app.js'teki analitik bölümü anahtar deposuna / ziyaretçi kimliğine / çerezlere dokunmaz ve Google'a kullanıcı
 * kimliği ya da metin göndermez. Analitik AYRI dosya değildir: takip engelleyiciler dosya adına göre betiği
 * engelleyince çağrılar ReferenceError atıp siteyi düşürmesin (inceleme I1) — yalnız Google'ın gtag.js'i
 * engellenebilir, o zaman işlevler sessizce no-op kalır.
 */
class SiteAnalitikOnYuzTest {

    private static final Path STATIK = Path.of("src/main/resources/static");

    private static String oku(String ad) throws IOException {
        return Files.readString(STATIK.resolve(ad));
    }

    private static final String BOLUM_BAS = "// ---------- analitik (ADR-0019) ----------";
    private static final String BOLUM_SON = "// ---------- analitik sonu ----------";

    /** app.js'teki analitik bölümü (başlangıç/bitiş işaretleri arası). */
    private static String analitikBolumu() throws IOException {
        String app = oku("app.js");
        int bas = app.indexOf(BOLUM_BAS);
        int son = app.indexOf(BOLUM_SON);
        assertThat(bas).as("analitik bölümü app.js'te").isPositive();
        assertThat(son).isGreaterThan(bas);
        return app.substring(bas, son);
    }

    private static String csp(String index) {
        Matcher m = Pattern.compile("http-equiv=\"Content-Security-Policy\" content=\"([^\"]*)\"").matcher(index);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    /** CSP yalnız GA alanlarıyla ve (yalnız connect-src'de) "OpenRouter ile bağlan" anahtar ucuyla genişler. */
    @Test
    void cspYalnizGaAlanlariylaGenisler() throws IOException {
        assertThat(csp(oku("index.html"))).isEqualTo("default-src 'self'; "
                + "script-src 'self' https://www.googletagmanager.com; "
                + "connect-src 'self' https://openrouter.ai https://*.google-analytics.com https://*.analytics.google.com "
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
        assertThat(n).isEqualTo(3); // app.js, mulakat.js + JSON-LD
    }

    /**
     * I1: analitik ayrı (engellenebilir) dosyada DEĞİL; işlevler app.js'te bildirilir (hoisting → çağrı hiçbir zaman
     * ReferenceError atmaz) ve gövdeleri try/catch içinde — gtag.js engellense de gezinti/görev kaydı bozulmaz.
     */
    @Test
    void analitikEngellenemezDosyadaVeHataYutulur() throws IOException {
        assertThat(Files.exists(STATIK.resolve("analitik.js"))).isFalse();
        try (var dosyalar = Files.list(STATIK)) {
            assertThat(dosyalar.map(f -> f.getFileName().toString().toLowerCase()))
                    .noneMatch(a -> a.contains("analitik") || a.contains("analytics") || a.contains("gtag")
                            || a.contains("olcum") || a.contains("track"));
        }
        String index = oku("index.html");
        assertThat(index).doesNotContain("analitik.js");
        String bolum = analitikBolumu();
        for (String islev : new String[]{"analitikBaslat", "analitikSayfa", "analitikOlay"}) {
            int bas = bolum.indexOf("function " + islev + "(");
            assertThat(bas).as(islev + " app.js'te bildirilmiş").isPositive();
            String govde = bolum.substring(bas, bolum.indexOf("\n}", bas));
            assertThat(govde).as(islev + " hatayı yutar").contains("try {").contains("} catch (e) {");
        }
        // gtag yoksa (betik engellendi/henüz yok) sessiz no-op
        assertThat(bolum).contains("typeof window.gtag !== \"function\"");
        // app.js dışında hiçbir dosya analitik işlevlerini çağırmaz (mulakat.js app.js'ten bağımsız yüklenir)
        assertThat(oku("mulakat.js")).doesNotContain("analitik");
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

    /** Analitik bölümü ziyaretçi anahtarını, kimliğini, ilerleme kodunu ve çerezleri hiç bilmez. */
    @Test
    void analitikKisiselVeriyeDokunmaz() throws IOException {
        String js = analitikBolumu();
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
