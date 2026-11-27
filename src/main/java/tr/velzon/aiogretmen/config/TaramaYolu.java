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
 */
public final class TaramaYolu {

    public static final String ROBOTS = "/robots.txt";
    public static final String SITEMAP = "/sitemap.xml";
    public static final String FAVICON = "/favicon.svg";

    private static final Set<String> YOLLAR = Set.of(ROBOTS, SITEMAP, FAVICON);

    private TaramaYolu() {
    }

    /** @return istek {@code GET|HEAD} ve ÇÖZÜLMÜŞ yolu bot yollarından biri mi? */
    public static boolean mi(HttpServletRequest istek) {
        String yontem = istek.getMethod();
        if (!"GET".equals(yontem) && !"HEAD".equals(yontem)) {
            return false;
        }
        return YOLLAR.contains(UrlPathHelper.defaultInstance.getLookupPathForRequest(istek));
    }
}
