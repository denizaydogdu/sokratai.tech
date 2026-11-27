package tr.velzon.aiogretmen.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tr.velzon.aiogretmen.repo.SaglikRepo;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Faz 11 (ADR-0018): sağlık denetimi gerçek {@code SELECT 1} ile; bağlantı alınamazsa {@code false}, istisna atmaz. */
class SaglikServiceTest {

    @Test
    void calisanVeritabaniAyakta() {
        DataSource h2 = new DriverManagerDataSource("jdbc:h2:mem:saglikbirim;DB_CLOSE_DELAY=-1", "sa", "");
        assertThat(new SaglikService(new SaglikRepo(h2)).veritabaniAyaktaMi()).isTrue();
    }

    @Test
    void baglantiAlinamazsaAyaktaDegil() throws Exception {
        DataSource bozuk = mock(DataSource.class);
        when(bozuk.getConnection()).thenThrow(new SQLException("baglanti reddedildi"));

        assertThat(new SaglikService(new SaglikRepo(bozuk)).veritabaniAyaktaMi()).isFalse();
    }
}
