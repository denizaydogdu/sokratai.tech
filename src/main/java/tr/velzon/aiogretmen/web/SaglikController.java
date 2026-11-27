package tr.velzon.aiogretmen.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.velzon.aiogretmen.config.SaglikYolu;
import tr.velzon.aiogretmen.service.SaglikService;

import java.util.Map;

/**
 * Sağlık ucu (Faz 11, ADR-0018) — Dokploy/Docker health check'i için; actuator YOK (yeni bağımlılık eklenmedi).
 *  - {@code GET /api/saglik} → 200 {@code {"durum":"ayakta"}} (veritabanı {@code SELECT 1}'e yanıt verdi)
 *  - veritabanı yoksa 503 {@code {"durum":"veritabani-yok"}} — hata ayrıntısı gövdeye GİRMEZ. Hazırlık/izleme ucu.
 *  - {@code GET /api/saglik/canli} → sabit 200 {@code {"durum":"canli"}}, veritabanına DOKUNMAZ (canlılık; Docker
 *    HEALTHCHECK). DB kesintisinde konteyner yeniden başlatılmaz — yeniden başlatma DB'yi düzeltmez.
 * Hem yerel modda hem {@code site} profilinde çalışır; sitede çerez yazmaz, ziyaretçi satırı açmaz ({@link SaglikYolu}).
 */
@RestController
public class SaglikController {

    private final SaglikService saglikService;

    public SaglikController(SaglikService saglikService) {
        this.saglikService = saglikService;
    }

    @GetMapping(SaglikYolu.YOL)
    public ResponseEntity<Map<String, String>> saglik() {
        boolean ayakta = saglikService.veritabaniAyaktaMi();
        return ResponseEntity.status(ayakta ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("durum", ayakta ? "ayakta" : "veritabani-yok"));
    }

    @GetMapping(SaglikYolu.CANLI)
    public ResponseEntity<Map<String, String>> canli() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("durum", "canli"));
    }
}
