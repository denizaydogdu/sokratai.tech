package tr.velzon.aiogretmen;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tr.velzon.aiogretmen.config.ZiyaretciCerezi;
import tr.velzon.aiogretmen.config.ZiyaretciKullaniciBaglami;
import tr.velzon.aiogretmen.repo.SaglikRepo;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Faz 11 (ADR-0018): Dokploy/Docker sağlık ucu {@code GET /api/saglik} — yeni bağımlılık yok (actuator yerine).
 * Veritabanına {@code SELECT 1}: başarı 200 {@code {"durum":"ayakta"}}, hata 503 {@code {"durum":"veritabani-yok"}}
 * (istisna metni gövdeye girmez). Sağlık kontrolü dakikada bir gelir → ziyaretçi/XSRF çerezi YAZILMAZ,
 * {@code ziyaretci} satırı AÇILMAZ, hız kovası tüketilmez.
 *
 * <p>Kök pakette: repo katmanını ({@link SaglikRepo}) taklit eder; ArchUnit'in web→repo yasağı test sınıflarına da
 * uygulanır.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesaglik;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.hafiza-dir=target/test-hafiza-sitesaglik",
        "app.rapor-dir=target/test-raporlar-sitesaglik"
})
@ActiveProfiles("site")
@AutoConfigureMockMvc
class SiteSaglikTest {

    private static final String CEREZ = "HHHHHHHHHHHHHHHHHHHHHh";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    @MockitoSpyBean
    SaglikRepo saglikRepo;

    private int ziyaretciSayisi() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci", Integer.class);
    }

    @Test
    void veritabaniAyaktaysa200() throws Exception {
        MvcResult r = mvc.perform(get("/api/saglik"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"durum\":\"ayakta\"}", JsonCompareMode.STRICT))
                .andReturn();

        assertThat(r.getResponse().getHeaders("Set-Cookie")).as("sağlık ucu çerez yazmaz").isEmpty();
        assertThat(r.getResponse().getHeader("Cache-Control")).contains("no-store");
    }

    @Test
    void cerezliSaglikIstegiZiyaretciSatiriAcmazCerezYenilemez() throws Exception {
        int once = ziyaretciSayisi();

        MvcResult r = mvc.perform(get("/api/saglik").cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(r.getResponse().getHeaders("Set-Cookie")).isEmpty();
        assertThat(r.getRequest().getAttribute(ZiyaretciCerezi.ISTEK_OZNITELIGI)).isNull();
        assertThat(ziyaretciSayisi()).isEqualTo(once);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci WHERE id = ?", Integer.class,
                ZiyaretciKullaniciBaglami.ONEK + CEREZ)).isZero();
    }

    @Test
    void veritabaniYoksa503AyrintiSizdirmaz() throws Exception {
        doThrow(new CannotGetJdbcConnectionException("gizli-baglanti-ayrintisi")).when(saglikRepo).selectBir();

        MvcResult r = mvc.perform(get("/api/saglik"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"durum\":\"veritabani-yok\"}", JsonCompareMode.STRICT))
                .andReturn();

        assertThat(r.getResponse().getContentAsString()).doesNotContain("gizli").doesNotContain("Exception");
        assertThat(r.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    /**
     * Canlılık (liveness) ucu: süreç ayakta mı — veritabanına DOKUNMAZ, DB düşükken de 200 (Docker HEALTHCHECK
     * bunu yoklar; DB kesintisinde konteyner gereksiz yeniden başlatılmasın). Hazırlık/izleme {@code /api/saglik}'ta.
     */
    @Test
    void canlilikUcuVeritabaninaDokunmadan200() throws Exception {
        clearInvocations(saglikRepo);
        doThrow(new CannotGetJdbcConnectionException("db-yok")).when(saglikRepo).selectBir();
        int once = ziyaretciSayisi();

        MvcResult r = mvc.perform(get("/api/saglik/canli").cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"durum\":\"canli\"}", JsonCompareMode.STRICT))
                .andReturn();

        verify(saglikRepo, never()).selectBir();
        assertThat(r.getResponse().getHeaders("Set-Cookie")).as("canlılık ucu çerez yazmaz").isEmpty();
        assertThat(r.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(r.getRequest().getAttribute(ZiyaretciCerezi.ISTEK_OZNITELIGI)).isNull();
        assertThat(ziyaretciSayisi()).isEqualTo(once);
    }

    @Test
    void dockerHealthcheckCanlilikUcunuYoklar() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        assertThat(dockerfile).contains("http://127.0.0.1:8090/api/saglik/canli");
        String kurulum = Files.readString(Path.of("docs/kurulum-site.md"));
        assertThat(kurulum).contains("/api/saglik/canli");
    }

    @Test
    void digerApiUclariCerezYazmayaDevamEder() throws Exception {
        // istisna yalnız GET /api/saglik — başka uçlar ziyaretçi çerezini her zamanki gibi yazar
        MvcResult r = mvc.perform(get("/api/ozellikler")).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getHeaders("Set-Cookie")).anyMatch(c -> c.startsWith(ZiyaretciCerezi.AD + "="));
    }

    /**
     * Veritabanı tamamen düşükken sağlık ucu Hikari'nin bağlantı bekleme süresi kadar asılı kalır; Boot varsayılanı
     * 30 sn, health check zaman aşımını (10 sn) aşar. Site profili bunu 5 sn'ye indirir
     * ({@code APP_SITE_DB_BAGLANTI_ZAMAN_ASIMI_MS}).
     */
    @Test
    void sitedeHavuzBaglantiZamanAsimiBesSaniye() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(((HikariDataSource) dataSource).getConnectionTimeout()).isEqualTo(5_000L);
    }

    @Test
    void baglantiZamanAsimiBelgelerdeYazili() throws Exception {
        String kurulum = Files.readString(Path.of("docs/kurulum-site.md"));
        assertThat(kurulum).contains("APP_SITE_DB_BAGLANTI_ZAMAN_ASIMI_MS");
        String ornek = Files.readString(Path.of("deploy/site.env.ornek"));
        assertThat(ornek).contains("APP_SITE_DB_BAGLANTI_ZAMAN_ASIMI_MS");
    }
}
