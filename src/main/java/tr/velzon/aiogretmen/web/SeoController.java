package tr.velzon.aiogretmen.web;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.velzon.aiogretmen.config.SiteAyarlari;
import tr.velzon.aiogretmen.config.TaramaYolu;
import tr.velzon.aiogretmen.service.AcikDersService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Arama motoru uçları ve ana sayfanın alan adına bağlı etiketleri (Faz 12, ADR-0019) — yalnız {@code site} profili.
 *
 * <ul>
 *   <li>{@code GET /} ve {@code /index.html}: statik {@code index.html} açılışta BİR KEZ okunur; içindeki
 *       {@value #ISARET} yorumu, {@code APP_SITE_TEMEL_URL} ayarlıysa canonical + {@code og:url} etiketleriyle,
 *       değilse boşlukla değiştirilir (alan adı koda gömülmez). Sayfanın geri kalanı statik dosyanın aynısıdır.</li>
 *   <li>{@code GET /robots.txt}: her şeye izin; AI/arama botları (GPTBot, ClaudeBot, PerplexityBot…) için açık
 *       {@code Allow: /} grupları (ADR-0020) — {@code /api/} KAPATILMAZ (Googlebot SPA'yı çizerken {@code /api/**}
 *       GET'lerine ihtiyaç duyar; dizine girmemesini {@link ApiNoindexInterceptor} sağlar); temel URL ayarlıysa
 *       {@code Sitemap:} satırı.</li>
 *   <li>{@code GET /sitemap.xml}: temel URL ayarlıysa ana sayfa + herkese açık JS'siz ders sayfaları
 *       ({@code /dersler}, her hafta, her gün, her kaynak — ADR-0020; {@code lastmod} kaynak tarihinden ya da dosya
 *       zamanından); değilse 404. Mülakat/atölye hiçbir zaman listelenmez.</li>
 * </ul>
 * Bot uçları çerez yazmaz ({@link TaramaYolu}). Temel URL {@link SiteAyarlari}'nda doğrulanmıştır (yalnız şema +
 * ASCII ana makine + port) → HTML/XML'e kaçışsız girebilir.
 */
@RestController
@Profile("site")
public class SeoController {

    /** index.html'deki yer tutucu yorum. */
    static final String ISARET = "<!-- seo:alan -->";

    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
    private static final MediaType METIN = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
    private static final MediaType XML = new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8);
    private static final CacheControl BOT_ONBELLEK = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    /**
     * Açıkça izin verilen tarayıcı botları (ADR-0020). {@code User-agent: *} zaten her şeye izin verir; adlı gruplar
     * kararı açık yazar (bir bot kendi adlı grubunu bulunca yalnız onu okur — her grupta {@code Allow: /}).
     */
    static final List<String> IZINLI_BOTLAR = List.of("GPTBot", "OAI-SearchBot", "ClaudeBot", "Claude-SearchBot",
            "PerplexityBot", "Google-Extended", "Bingbot", "Applebot");

    private final String temelUrl;
    private final String anaSayfa;
    private final String siteHaritasi;

    public SeoController(SiteAyarlari ayarlar, AcikDersService acikDersler) {
        this.temelUrl = ayarlar.temelUrl();
        String sablon;
        try {
            sablon = new ClassPathResource("static/index.html").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("static/index.html okunamadı", e);
        }
        if (!sablon.contains(ISARET)) {
            throw new IllegalStateException("static/index.html içinde " + ISARET + " yer tutucusu yok");
        }
        String etiketler = temelUrl.isEmpty() ? ""
                : "<link rel=\"canonical\" href=\"" + temelUrl + "/\">\n"
                + "<meta property=\"og:url\" content=\"" + temelUrl + "/\">";
        this.anaSayfa = sablon.replace(ISARET, etiketler);
        this.siteHaritasi = temelUrl.isEmpty() ? null : siteHaritasiUret(acikDersler.sayfaYollari());
    }

    @GetMapping({"/", "/index.html"})
    public ResponseEntity<String> anaSayfa() {
        return ResponseEntity.ok().contentType(HTML).cacheControl(CacheControl.noCache()).body(anaSayfa);
    }

    @GetMapping(TaramaYolu.ROBOTS)
    public ResponseEntity<String> robots() {
        StringBuilder metin = new StringBuilder("User-agent: *\nAllow: /\n");
        for (String bot : IZINLI_BOTLAR) {
            metin.append("\nUser-agent: ").append(bot).append("\nAllow: /\n");
        }
        if (!temelUrl.isEmpty()) {
            metin.append("\nSitemap: ").append(temelUrl).append(TaramaYolu.SITEMAP).append('\n');
        }
        return ResponseEntity.ok().contentType(METIN).cacheControl(BOT_ONBELLEK).body(metin.toString());
    }

    @GetMapping(TaramaYolu.SITEMAP)
    public ResponseEntity<String> sitemap() {
        if (siteHaritasi == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(XML).cacheControl(BOT_ONBELLEK).body(siteHaritasi);
    }

    /** İçerik açılışta yüklenir, çalışırken değişmez → harita bir kez üretilir. */
    private String siteHaritasiUret(List<AcikDersService.SayfaYolu> yollar) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
                + "  <url><loc>" + temelUrl + "/</loc></url>\n");
        for (AcikDersService.SayfaYolu y : yollar) {
            xml.append("  <url><loc>").append(AcikSayfa.kac(temelUrl + y.yol())).append("</loc>");
            if (y.sonDegisiklik() != null) {
                xml.append("<lastmod>").append(y.sonDegisiklik()).append("</lastmod>");
            }
            xml.append("</url>\n");
        }
        return xml.append("</urlset>\n").toString();
    }
}
