package tr.velzon.aiogretmen.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Faz 12 (ADR-0019): ayar VERİLMEMİŞ ya da GEÇERSİZ site — analitik tamamen kapalı, alan adına bağlı etiketler yok.
 * Geçersiz ölçüm kimliği boş sayılır (açılış düşmez); temel URL yoksa sitemap 404, robots'ta Sitemap satırı yok,
 * ana sayfada canonical/og:url yok (alan adı koda gömülmez).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:siteseoayarsiz;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.hafiza-dir=target/test-hafiza-siteseoayarsiz",
        "app.rapor-dir=target/test-raporlar-siteseoayarsiz",
        "app.site.ga-olcum-kimligi=UA-12345-1",
        "app.site.temel-url="
})
@ActiveProfiles("site")
@AutoConfigureMockMvc
class SiteSeoAyarsizTest {

    @Autowired
    MockMvc mvc;

    @Test
    void gecersizGaKimligiOzelliklereGirmez() throws Exception {
        mvc.perform(get("/api/ozellikler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.site").value(true))
                .andExpect(jsonPath("$.gaOlcumKimligi").doesNotExist());
    }

    @Test
    void temelUrlYoksaSitemapYok() throws Exception {
        MvcResult r = mvc.perform(get("/sitemap.xml")).andExpect(status().isNotFound()).andReturn();
        assertThat(r.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    void temelUrlYoksaRobotsSitemapSatirsiz() throws Exception {
        String govde = mvc.perform(get("/robots.txt")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(govde).contains("Allow: /").doesNotContain("Disallow").doesNotContain("Sitemap:");
    }

    @Test
    void temelUrlYoksaCanonicalYok() throws Exception {
        String html = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("rel=\"canonical\"").doesNotContain("og:url").doesNotContain("seo:alan")
                .contains("<meta name=\"description\"");
    }

    /** ADR-0020: temel URL yoksa açık ders sayfaları yine çizilir; canonical/og:url ve JSON-LD url'leri yok. */
    @Test
    void temelUrlYoksaAcikSayfalarCanonicalsiz() throws Exception {
        String html = mvc.perform(get("/hafta/1/gun/1")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(html).contains("<title>").doesNotContain("rel=\"canonical\"").doesNotContain("og:url")
                .doesNotContain("\"url\":");
        // geçersiz/boş ölçüm kimliği → betik etiketi yok, CSP betiksiz
        assertThat(html).doesNotContain("/s.js").doesNotContain("data-k=").contains("script-src 'none'")
                .doesNotContain("googletagmanager");
        String llms = mvc.perform(get("/llms.txt")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(llms).contains("](/hafta/1)").contains("](/llms-full.txt)");
    }
}
