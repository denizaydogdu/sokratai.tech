package tr.velzon.aiogretmen.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tr.velzon.aiogretmen.config.AppProperties;
import tr.velzon.aiogretmen.config.SiteAyarlari;
import tr.velzon.aiogretmen.config.VarsayilanGuvenlikYapilandirma;
import tr.velzon.aiogretmen.service.KullanimService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tech-debt #14: yerel modda (profil yok) {@code sunucuAi} sunucu AI'sının açık/kapalı oluşunu izler.
 * Faz 12 (ADR-0019): yerel mod ASLA izlenmez — GA kimliği ortamda olsa bile {@code gaOlcumKimligi} dönmez.
 */
@TestPropertySource(properties = "app.site.ga-olcum-kimligi=G-YEREL12345")
@Import({VarsayilanGuvenlikYapilandirma.class, OzellikControllerTest.Ayarlar.class})
@WebMvcTest(OzellikController.class)
class OzellikControllerTest {

    /** Dilim testinde {@code @ConfigurationPropertiesScan} çalışmaz — SiteAyarlari gerçek bağlamayla eklenir. */
    @TestConfiguration
    @EnableConfigurationProperties(SiteAyarlari.class)
    static class Ayarlar {
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    KullanimService kullanimService;

    @MockitoBean
    AppProperties props;

    @Test
    void yereldeAiAktifkenSunucuAiTrue() throws Exception {
        when(kullanimService.aiAktif()).thenReturn(true);
        mvc.perform(get("/api/ozellikler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sunucuAi").value(true))
                .andExpect(jsonPath("$.site").value(false))
                .andExpect(jsonPath("$.kullanici").doesNotExist())
                .andExpect(jsonPath("$.gaOlcumKimligi").doesNotExist());
    }

    @Test
    void yereldeAiKapaliykenSunucuAiFalse() throws Exception {
        when(kullanimService.aiAktif()).thenReturn(false);
        mvc.perform(get("/api/ozellikler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sunucuAi").value(false))
                .andExpect(jsonPath("$.site").value(false))
                .andExpect(jsonPath("$.kullanici").doesNotExist());
    }
}
