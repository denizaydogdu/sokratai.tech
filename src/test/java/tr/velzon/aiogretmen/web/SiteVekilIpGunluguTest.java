package tr.velzon.aiogretmen.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import tr.velzon.aiogretmen.AiOgretmenApplication;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Faz 11 inceleme düzeltmesi (docs/kurulum-site.md §10.5): vekil güveni ({@code APP_SITE_GUVENILIR_VEKIL}) doğru mu?
 * Belirti: yanlış ayarda tüm ziyaretçiler Traefik'in IP'siyle görünür → sitenin tamamında 429. Doğrulama yolu:
 * {@code LOGGING_LEVEL_TR_VELZON_AIOGRETMEN_VEKIL=DEBUG} geçici açılır, {@link HizSiniriInterceptor} çözülen istemci
 * IP'sini MASKELİ (yalnız ilk iki oktet) yazar. Sağlık ucuna bilgi eklenmez (sızdırırdı).
 *
 * <p>Ortam değişkeni biçimi bilerek denenir: Spring ortam değişkenini küçük harfe çevirir — sınıf adlı bir logger
 * ({@code ...HizSiniriInterceptor}) bu yolla AÇILAMAZ; logger adı bu yüzden küçük harfli {@code tr.velzon.aiogretmen.vekil}.
 */
@ExtendWith(OutputCaptureExtension.class)
class SiteVekilIpGunluguTest {

    @Test
    void ipv4IlkIkiOktetKalir() {
        assertThat(HizSiniriInterceptor.maskeliIp("10.20.30.40")).isEqualTo("10.20.x.x");
        assertThat(HizSiniriInterceptor.maskeliIp("127.0.0.1")).isEqualTo("127.0.x.x");
    }

    @Test
    void ipv6IlkIkiGrupKalir() {
        assertThat(HizSiniriInterceptor.maskeliIp("fd00:ab:1:2::5")).isEqualTo("fd00:ab:…");
        assertThat(HizSiniriInterceptor.maskeliIp("0:0:0:0:0:0:0:1")).isEqualTo("0:0:…");
    }

    @Test
    void tanimsizGirdiMaskelenir() {
        assertThat(HizSiniriInterceptor.maskeliIp(null)).isEqualTo("?");
        assertThat(HizSiniriInterceptor.maskeliIp("")).isEqualTo("?");
        assertThat(HizSiniriInterceptor.maskeliIp("bilinmeyen")).isEqualTo("?");
    }

    @Test
    void debugAcikkenCozulenIstemciIpsiMaskeliYazilir(CapturedOutput cikti) throws Exception {
        StandardEnvironment ortam = new StandardEnvironment();
        Map<String, Object> degiskenler = new HashMap<>(System.getenv());
        degiskenler.put("LOGGING_LEVEL_TR_VELZON_AIOGRETMEN_VEKIL", "DEBUG");
        ortam.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        degiskenler));

        try (ConfigurableApplicationContext c = new SpringApplicationBuilder(AiOgretmenApplication.class)
                .environment(ortam)
                .profiles("site")
                .run("--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:sitevekilip;DB_CLOSE_DELAY=-1",
                        "--spring.ai.openai.api-key=",
                        "--app.hafiza-dir=target/test-hafiza-sitevekilip",
                        "--app.rapor-dir=target/test-raporlar-sitevekilip",
                        "--app.site.guvenli-cerez=false")) {
            int port = ((WebServerApplicationContext) c).getWebServer().getPort();
            // 127.0.0.1 varsayılan güvenilir vekil → X-Forwarded-For çözülür (Traefik arkasındaki gerçek istemci)
            HttpRequest istek = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ozellikler"))
                    .header("X-Forwarded-For", "10.20.30.40")
                    .build();
            HttpResponse<String> yanit = HttpClient.newHttpClient().send(istek, HttpResponse.BodyHandlers.ofString());
            assertThat(yanit.statusCode()).isEqualTo(200);
        }

        assertThat(cikti.getOut()).contains("istemci IP (maskeli): 10.20.x.x");
        assertThat(cikti.getOut()).doesNotContain("10.20.30.40");
    }

    @Test
    void debugKapaliykenIpYazilmaz(CapturedOutput cikti) throws Exception {
        try (ConfigurableApplicationContext c = new SpringApplicationBuilder(AiOgretmenApplication.class)
                .profiles("site")
                .run("--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:sitevekilipkapali;DB_CLOSE_DELAY=-1",
                        "--spring.ai.openai.api-key=",
                        "--app.hafiza-dir=target/test-hafiza-sitevekilip",
                        "--app.rapor-dir=target/test-raporlar-sitevekilip",
                        "--app.site.guvenli-cerez=false")) {
            int port = ((WebServerApplicationContext) c).getWebServer().getPort();
            HttpRequest istek = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ozellikler"))
                    .header("X-Forwarded-For", "10.20.30.41")
                    .build();
            HttpClient.newHttpClient().send(istek, HttpResponse.BodyHandlers.ofString());
        }
        assertThat(cikti.getOut()).doesNotContain("istemci IP (maskeli)");
    }
}
