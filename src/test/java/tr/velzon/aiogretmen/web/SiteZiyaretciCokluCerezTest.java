package tr.velzon.aiogretmen.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tr.velzon.aiogretmen.config.ZiyaretciCerezi;
import tr.velzon.aiogretmen.config.ZiyaretciKullaniciBaglami;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Faz 11 güvenlik notu (ADR-0018 §Güvenlik notu): aynı adla FARKLI değerli birden çok {@code aio_ziyaretci} çerezi,
 * kardeş alt alan adından ana alan adına yerleştirilmiş bir çerez olabilir (kimlik sabitleme). Filtre hiçbirine
 * güvenmez: yeni kimlik üretip yazar, veri o yeni kimliğe gider. Aynı değerin tekrarı tek çerez sayılır.
 *
 * <p>Ayrı sınıf ve GERÇEK XSRF çerez akışı (spring-security-test {@code csrf()} DEĞİL): {@code csrf()} bağlamdaki
 * CSRF deposunu kalıcı olarak oturum tabanlısıyla değiştirir → aynı bağlamdaki sonraki istekler oturum açar.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:siteziyaretcicoklu;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.hafiza-dir=target/test-hafiza-siteziyaretcicoklu",
        "app.rapor-dir=target/test-raporlar-siteziyaretcicoklu"
})
@ActiveProfiles("site")
@AutoConfigureMockMvc
class SiteZiyaretciCokluCerezTest {

    private static final String CEREZ_A = "MMMMMMMMMMMMMMMMMMMMMa";
    private static final String CEREZ_B = "MMMMMMMMMMMMMMMMMMMMMb";
    private static final String CEREZ_TEKRAR = "TTTTTTTTTTTTTTTTTTTTTt";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private static String setCookieDegeri(MvcResult r, String ad) {
        List<String> cerezler = r.getResponse().getHeaders("Set-Cookie").stream()
                .filter(c -> c.startsWith(ad + "=")).toList();
        assertThat(cerezler).as(ad + " tek kez yazılmalı").hasSize(1);
        return cerezler.get(0).substring(ad.length() + 1).split(";")[0];
    }

    @Test
    void farkliDegerliCokluCerezdeHicbirineGuvenilmez() throws Exception {
        String xsrf = setCookieDegeri(mvc.perform(get("/")).andReturn(), "XSRF-TOKEN");

        MvcResult r = mvc.perform(post("/api/gorev/durum")
                        .cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ_A), new Cookie(ZiyaretciCerezi.AD, CEREZ_B),
                                new Cookie("XSRF-TOKEN", xsrf))
                        .header("X-XSRF-TOKEN", xsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hafta\":1,\"gun\":1,\"gorevIndex\":0,\"tamamlandi\":true}"))
                .andExpect(status().isOk()).andReturn();

        String yeni = setCookieDegeri(r, ZiyaretciCerezi.AD);
        assertThat(ZiyaretciCerezi.gecerliMi(yeni)).isTrue();
        assertThat(yeni).isNotIn(CEREZ_A, CEREZ_B);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gorev_ilerleme WHERE kullanici_id = ?", Integer.class,
                ZiyaretciKullaniciBaglami.ONEK + yeni)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gorev_ilerleme WHERE kullanici_id IN (?, ?)",
                Integer.class, ZiyaretciKullaniciBaglami.ONEK + CEREZ_A, ZiyaretciKullaniciBaglami.ONEK + CEREZ_B))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ziyaretci WHERE id IN (?, ?)", Integer.class,
                ZiyaretciKullaniciBaglami.ONEK + CEREZ_A, ZiyaretciKullaniciBaglami.ONEK + CEREZ_B)).isZero();
    }

    @Test
    void ayniDegerliTekrarliCerezKimligiKorur() throws Exception {
        MvcResult r = mvc.perform(get("/api/program")
                        .cookie(new Cookie(ZiyaretciCerezi.AD, CEREZ_TEKRAR), new Cookie(ZiyaretciCerezi.AD, CEREZ_TEKRAR)))
                .andExpect(status().isOk()).andReturn();

        assertThat(setCookieDegeri(r, ZiyaretciCerezi.AD)).isEqualTo(CEREZ_TEKRAR);
    }
}
