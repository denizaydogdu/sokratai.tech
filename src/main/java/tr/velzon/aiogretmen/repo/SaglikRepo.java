package tr.velzon.aiogretmen.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * Sağlık denetimi (Faz 11, ADR-0018): veritabanına {@code SELECT 1}. Paylaşılan {@code JdbcTemplate}'in ayarlarını
 * bozmamak için KENDİ şablonunu kurar; sorgu zaman aşımı {@value #SORGU_ZAMAN_ASIMI_SN} sn.
 *
 * <p>Not: havuzdan bağlantı alma süresi Hikari'nin {@code connection-timeout}'una bağlıdır (sorgu zaman aşımı onu
 * kısaltmaz) — veritabanı tamamen erişilemezse yanıt o süre kadar gecikebilir. Site profili onu 5 sn'ye indirir
 * ({@code APP_SITE_DB_BAGLANTI_ZAMAN_ASIMI_MS}, ADR-0018).
 */
@Repository
public class SaglikRepo {

    static final int SORGU_ZAMAN_ASIMI_SN = 2;

    private final JdbcTemplate jdbc;

    public SaglikRepo(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(SORGU_ZAMAN_ASIMI_SN);
    }

    /** {@code SELECT 1} çalıştırır; veritabanı yanıt vermezse {@link org.springframework.dao.DataAccessException}. */
    public void selectBir() {
        jdbc.queryForObject("SELECT 1", Integer.class);
    }
}
