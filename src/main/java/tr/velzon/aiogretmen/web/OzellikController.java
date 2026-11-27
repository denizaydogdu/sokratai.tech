package tr.velzon.aiogretmen.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.velzon.aiogretmen.config.AppProperties;
import tr.velzon.aiogretmen.config.SiteAyarlari;
import tr.velzon.aiogretmen.service.KullanimService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Arayüzün açılışta okuduğu özellik bayrakları (ADR-0011):
 *  - GET /api/ozellikler → {@code {"mulakat": false, "site": false, "sunucuAi": true}}
 *  - Ziyaretçi kimliği (HttpOnly çerez) ASLA dönmez (Faz 10); {@code sunucuAi} = !site && sunucu anahtarı var.
 *  - {@code gaOlcumKimligi} (Faz 12, ADR-0019): YALNIZ {@code site} profilinde ve geçerli kimlik ayarlıysa
 *    ({@code APP_SITE_GA_OLCUM_KIMLIGI}); yoksa alan hiç yoktur → app.js analitik bölümü hiçbir şey yüklemez/göndermez.
 *    Yerel mod asla izlenmez.
 * index.html'e sunucu yalnız alan adına bağlı SEO etiketlerini koyar ({@link SeoController}, sitede); hangi bölümün gösterileceğini
 * frontend bu uçtan öğrenir. Mülakat kapalıyken mulakat.js hiçbir /api/mulakat isteği atmaz.
 */
@RestController
@RequestMapping("/api")
public class OzellikController {

    private final AppProperties props;
    private final KullanimService kullanimService;
    private final boolean site;
    private final String gaOlcumKimligi;

    public OzellikController(AppProperties props, KullanimService kullanimService, Environment ortam,
                             SiteAyarlari siteAyarlari) {
        this.props = props;
        this.kullanimService = kullanimService;
        this.site = ortam.matchesProfiles("site");
        // SiteAyarlari kimliği zaten doğruladı (biçim dışı → ""); yerel modda hiç verilmez
        this.gaOlcumKimligi = site ? siteAyarlari.gaOlcumKimligi() : "";
    }

    @GetMapping("/ozellikler")
    public Map<String, Object> ozellikler() {
        Map<String, Object> ozellik = new LinkedHashMap<>();
        ozellik.put("mulakat", props.mulakatEtkin() && !site); // sitede zorla kapalı (MulakatEtkin, Faz 9b)
        ozellik.put("site", site);
        // sunucu anahtarıyla çalışan AI düğmeleri (rapor/değerlendirme/AI quiz): yalnız yerelde ve anahtar varsa
        ozellik.put("sunucuAi", !site && kullanimService.aiAktif());
        if (!gaOlcumKimligi.isEmpty()) {
            ozellik.put("gaOlcumKimligi", gaOlcumKimligi);
        }
        return ozellik;
    }
}
