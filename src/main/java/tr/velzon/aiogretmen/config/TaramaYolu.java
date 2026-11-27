package tr.velzon.aiogretmen.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

import java.util.Set;

/**
 * Arama motoru / tarayıcı botu yolları (Faz 12, ADR-0019) — tek kaynak. {@code robots.txt}, {@code sitemap.xml} ve
 * {@code favicon.svg} botlar tarafından sık ve çerezsiz çekilir: {@link SaglikYolu} gibi {@link ZiyaretciCerezi} ve
 * {@link SiteGuvenlikYapilandirma} CSRF çerez filtresi bu istekleri ATLAR → çerez yazılmaz, ziyaretçi kimliği
 * üretilmez. ({@code web/} katmanındaki ziyaretçi dokunma / hız sınırı interceptor'ları yalnız {@code /api/**}'ye
 * bağlı; bu yollar oraya hiç girmez.) Yalnız okuyan yöntemler ({@code GET}, {@code HEAD}).
 *
 * <p>Herkese açık, JS'siz ders sayfaları (ADR-0020) da aynı muafiyeti alır: {@value #DERSLER},
 * {@code /hafta/**}, {@code /kaynak/**}, {@value #LLMS}, {@value #LLMS_FULL} ve sayfaların kullandığı
 * {@value #STIL}, {@value #BETIK} — salt okunur içerik, kişiye özel durum taşımaz.
 */
public final class TaramaYolu {

    public static final String ROBOTS = "/robots.txt";
    public static final String SITEMAP = "/sitemap.xml";
    public static final String FAVICON = "/favicon.svg";
    public static final String STIL = "/style.css";
    /** Açık sayfaların sayfa görüntüleme betiği (yalnız GA kimliği ayarlıysa sayfaya eklenir). */
    public static final String BETIK = "/s.js";
    public static final String DERSLER = "/dersler";
    public static final String LLMS = "/llms.txt";
    public static final String LLMS_FULL = "/llms-full.txt";
    /** Önek yollar: {@code /hafta/{n}}, {@code /hafta/{n}/gun/{g}}, {@code /kaynak/{slug}}. */
    public static final String HAFTA_ONEKI = "/hafta/";
    public static final String KAYNAK_ONEKI = "/kaynak/";

    private static final Set<String> YOLLAR = Set.of(ROBOTS, SITEMAP, FAVICON, STIL, BETIK, DERSLER, LLMS, LLMS_FULL);

    private TaramaYolu() {
    }

    /** @return istek {@code GET|HEAD} ve ÇÖZÜLMÜŞ yolu bot yollarından biri mi? */
    public static boolean mi(HttpServletRequest istek) {
        String yontem = istek.getMethod();
        if (!"GET".equals(yontem) && !"HEAD".equals(yontem)) {
            return false;
        }
        String yol = UrlPathHelper.defaultInstance.getLookupPathForRequest(istek);
        return YOLLAR.contains(yol) || yol.startsWith(HAFTA_ONEKI) || yol.startsWith(KAYNAK_ONEKI);
    }
}
