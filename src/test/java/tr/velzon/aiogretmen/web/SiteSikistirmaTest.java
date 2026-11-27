package tr.velzon.aiogretmen.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0020: site profilinde Tomcat yanıt sıkıştırması ({@code server.compression}) — GERÇEK sunucuda (MockMvc
 * sıkıştırmayı görmez). {@code Accept-Encoding: gzip} ile llms-full.txt sıkıştırılmış döner ve açılınca aynı metindir.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesikistirma;DB_CLOSE_DELAY=-1",
        "spring.ai.openai.api-key=",
        "app.hafiza-dir=target/test-hafiza-sitesikistirma",
        "app.rapor-dir=target/test-raporlar-sitesikistirma",
        "app.site.guvenli-cerez=false"
})
@ActiveProfiles("site")
class SiteSikistirmaTest {

    @LocalServerPort
    int port;

    private final HttpClient istemci = HttpClient.newHttpClient(); // kendiliğinden açmaz

    private HttpResponse<byte[]> al(String yol, boolean gzip) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + yol));
        if (gzip) {
            b.header("Accept-Encoding", "gzip");
        }
        return istemci.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    void llmsFullGzipIleSikistirilmisDoner() throws Exception {
        HttpResponse<byte[]> duz = al("/llms-full.txt", false);
        HttpResponse<byte[]> sikistirilmis = al("/llms-full.txt", true);
        assertThat(duz.statusCode()).isEqualTo(200);
        assertThat(duz.headers().firstValue("Content-Encoding")).isEmpty();
        assertThat(sikistirilmis.statusCode()).isEqualTo(200);
        assertThat(sikistirilmis.headers().firstValue("Content-Encoding")).contains("gzip");
        assertThat(sikistirilmis.body().length).isLessThan(duz.body().length / 2);
        try (GZIPInputStream ac = new GZIPInputStream(new ByteArrayInputStream(sikistirilmis.body()))) {
            assertThat(new String(ac.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(new String(duz.body(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void htmlSayfaDaSikistirilir() throws Exception {
        HttpResponse<byte[]> r = al("/hafta/1/gun/1", true);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Content-Encoding")).contains("gzip");
    }
}
