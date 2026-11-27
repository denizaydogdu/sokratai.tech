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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Arama motoru uçları ve ana sayfanın alan adına bağlı etiketleri (Faz 12, ADR-0019) — yalnız {@code site} profili.
 *
 * <ul>
 *   <li>{@code GET /} ve {@code /index.html}: statik {@code index.html} açılışta BİR KEZ okunur; içindeki
 *       {@value #ISARET} yorumu, {@code APP_SITE_TEMEL_URL} ayarlıysa canonical + {@code og:url} etiketleriyle,
 *       değilse boşlukla değiştirilir (alan adı koda gömülmez). Sayfanın geri kalanı statik dosyanın aynısıdır.</li>
 *   <li>{@code GET /robots.txt}: her şeye izin — {@code /api/} KAPATILMAZ (Googlebot SPA'yı çizerken {@code /api/**}
 *       GET'lerine ihtiyaç duyar; dizine girmemesini {@link ApiNoindexInterceptor} sağlar); temel URL ayarlıysa
 *       {@code Sitemap:} satırı.</li>
 *   <li>{@code GET /sitemap.xml}: temel URL ayarlıysa yalnız ana sayfa; değilse 404. Ön yüz tek sayfalık uygulamadır
 *       (hafta/gün/kaynak görünümleri sunucuda ayrı URL DEĞİL, JS ile çizilir) → sunucuda çözülen tek içerik URL'si
 *       köktür. Ders sayfalarının dizine girmesi sunucu tarafında üretilen sayfalar gerektirir (sonraki adım).</li>
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

    private final String temelUrl;
    private final String anaSayfa;

    public SeoController(SiteAyarlari ayarlar) {
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
    }

    @GetMapping({"/", "/index.html"})
    public ResponseEntity<String> anaSayfa() {
        return ResponseEntity.ok().contentType(HTML).cacheControl(CacheControl.noCache()).body(anaSayfa);
    }

    @GetMapping(TaramaYolu.ROBOTS)
    public ResponseEntity<String> robots() {
        StringBuilder metin = new StringBuilder("User-agent: *\nAllow: /\n");
        if (!temelUrl.isEmpty()) {
            metin.append("\nSitemap: ").append(temelUrl).append(TaramaYolu.SITEMAP).append('\n');
        }
        return ResponseEntity.ok().contentType(METIN).cacheControl(BOT_ONBELLEK).body(metin.toString());
    }

    @GetMapping(TaramaYolu.SITEMAP)
    public ResponseEntity<String> sitemap() {
        if (temelUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
                + "  <url><loc>" + temelUrl + "/</loc></url>\n"
                + "</urlset>\n";
        return ResponseEntity.ok().contentType(XML).cacheControl(BOT_ONBELLEK).body(xml);
    }
}
