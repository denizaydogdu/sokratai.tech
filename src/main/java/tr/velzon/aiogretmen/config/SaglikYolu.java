package tr.velzon.aiogretmen.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

/**
 * Sağlık uçlarının yolları (Faz 11, ADR-0018) — tek kaynak: hazırlık/izleme {@link #YOL} (veritabanı) ve canlılık
 * {@link #CANLI} (süreç; Docker HEALTHCHECK). Sağlık kontrolü (Dokploy/Docker) dakikada bir gelir:
 * {@link ZiyaretciCerezi}, {@link SiteGuvenlikYapilandirma} CSRF çerez filtresi ve {@code web/} katmanındaki
 * ziyaretçi dokunma / hız sınırı interceptor'ları bu isteği ATLAR → çerez yazılmaz, {@code ziyaretci} satırı açılmaz,
 * hız kovası tüketilmez. Yalnız {@code GET} (başka yöntem bu yolda zaten 405 olur, istisna genişlemesin).
 */
public final class SaglikYolu {

    /** Hazırlık/izleme ucu: veritabanına {@code SELECT 1}. */
    public static final String YOL = "/api/saglik";

    /** Canlılık ucu: veritabanına DOKUNMAZ, süreç yanıt veriyorsa 200. */
    public static final String CANLI = "/api/saglik/canli";

    private SaglikYolu() {
    }

    /**
     * @return istek {@code GET /api/saglik} ya da {@code GET /api/saglik/canli} mi? Yönlendirmenin kullandığı
     * ÇÖZÜLMÜŞ yol karşılaştırılır.
     */
    public static boolean mi(HttpServletRequest istek) {
        if (!"GET".equals(istek.getMethod())) {
            return false;
        }
        String yol = UrlPathHelper.defaultInstance.getLookupPathForRequest(istek);
        return YOL.equals(yol) || CANLI.equals(yol);
    }
}
