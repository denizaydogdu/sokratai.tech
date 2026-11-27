package tr.velzon.aiogretmen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import tr.velzon.aiogretmen.repo.SaglikRepo;

/**
 * Sağlık denetimi (Faz 11, ADR-0018): Dokploy/Docker health check'i {@code GET /api/saglik}'i çağırır; burada
 * veritabanı {@code SELECT 1}'e yanıt veriyor mu bakılır. Hata istisna olarak YUKARI ÇIKMAZ (ayrıntı yanıta
 * sızmasın); yalnız istisna sınıf adı WARN loglanır (bağlantı metni URL/kullanıcı adı taşıyabilir).
 */
@Service
public class SaglikService {

    private static final Logger log = LoggerFactory.getLogger(SaglikService.class);

    private final SaglikRepo repo;

    public SaglikService(SaglikRepo repo) {
        this.repo = repo;
    }

    /** @return veritabanı {@code SELECT 1}'e yanıt verdiyse {@code true}. */
    public boolean veritabaniAyaktaMi() {
        try {
            repo.selectBir();
            return true;
        } catch (DataAccessException e) {
            log.warn("Sağlık denetimi: veritabanı yanıt vermedi ({})", e.getClass().getSimpleName());
            return false;
        }
    }
}
