package tr.velzon.aiogretmen.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Faz 11 (ADR-0018): Docker'da aynı ağdaki komşu konteynerler Tomcat'in varsayılan "iç vekil" aralığındadır
 * (10/8, 172.16/12, 192.168/16, 127/8 …) → {@code X-Forwarded-*} taklit edip IP başına hız sınırını atlatabilirler.
 * {@code APP_SITE_GUVENILIR_VEKIL} ({@code server.tomcat.remoteip.internal-proxies}) güvenilen vekili daraltır:
 * burada yalnız 10.0.1.5 (örnek Traefik IP'si) güvenilir → 127.0.0.1'den gelen başlıklar YOK SAYILIR, istek https
 * sayılmaz, çerez {@code Secure} almaz. GERÇEK Tomcat (MockMvc valve çalıştırmaz).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:siteguvenilirvekil;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.hafiza-dir=target/test-hafiza-siteguvenilirvekil",
        "app.rapor-dir=target/test-raporlar-siteguvenilirvekil",
        "app.site.guvenli-cerez=false",
        "APP_SITE_GUVENILIR_VEKIL=10\\.0\\.1\\.5"
})
@ActiveProfiles("site")
class SiteGuvenilirVekilTest {

    @LocalServerPort
    int port;

    @Test
    void guvenilmeyenVekilinBasliklariYokSayilir() throws Exception {
        HttpRequest istek = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "10.20.30.40")
                .build();
        HttpResponse<String> yanit = HttpClient.newHttpClient().send(istek, HttpResponse.BodyHandlers.ofString());

        assertThat(yanit.statusCode()).isEqualTo(200);
        String cerez = yanit.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("aio_ziyaretci=")).findFirst().orElseThrow();
        assertThat(cerez).as("127.0.0.1 artık güvenilir vekil değil → X-Forwarded-Proto yok sayılmalı")
                .doesNotContain("Secure");
    }
}
