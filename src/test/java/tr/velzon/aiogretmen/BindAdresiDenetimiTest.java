package tr.velzon.aiogretmen;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import tr.velzon.aiogretmen.config.BindAdresiDenetimi;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Faz 11 inceleme düzeltmesi (ADR-0018): konteyner istisnası ({@code SERVER_ADDRESS=0.0.0.0}) YALNIZ {@code site}
 * profiliyle birlikte geçerli. İmaj {@code SPRING_PROFILES_ACTIVE}'i ezen bir ortamda (ör. profil boş bırakıldı)
 * yerel mod — giriş yok, sunucu anahtarı, mülakat/MCP — dış arayüzde açılmasın: loopback dışı adres + site profili
 * yok → bağlam AÇILMAZ.
 *
 * <p>0.0.0.0 + site durumu birim testle denetlenir (gerçek sunucu tüm arayüzlere bağlanmasın); ret durumu bağlamda
 * da denenir — denetim web sunucusu kurulmadan çalıştığı için hiçbir porta bağlanılmaz.
 */
class BindAdresiDenetimiTest {

    private static void denetle(String adres, String... profiller) {
        MockEnvironment ortam = new MockEnvironment();
        if (adres != null) {
            ortam.setProperty("server.address", adres);
        }
        ortam.setActiveProfiles(profiller);
        BindAdresiDenetimi denetim = new BindAdresiDenetimi();
        denetim.setEnvironment(ortam);
        denetim.postProcessBeanFactory(new DefaultListableBeanFactory());
    }

    @Test
    void loopbackAdresleriProfilsizAcilir() {
        for (String adres : List.of("127.0.0.1", "::1", "[::1]", "localhost", "0:0:0:0:0:0:0:1")) {
            assertThatCode(() -> denetle(adres)).as(adres).doesNotThrowAnyException();
        }
    }

    @Test
    void tumArayuzlerSiteProfiliyleAcilir() {
        assertThatCode(() -> denetle("0.0.0.0", "site")).doesNotThrowAnyException();
        assertThatCode(() -> denetle("0.0.0.0", "mcp", "site")).doesNotThrowAnyException();
    }

    @Test
    void loopbackDisiAdresSiteProfilsizAcilmaz() {
        for (String adres : List.of("0.0.0.0", "::", "10.0.1.5", "192.168.1.20")) {
            assertThatThrownBy(() -> denetle(adres))
                    .as(adres)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ADR-0018")
                    .hasMessageContaining("site");
        }
        assertThatThrownBy(() -> denetle("0.0.0.0", "mcp-http")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adresVerilmezseTumArayuzSayilirAcilmaz() {
        // server.address boş/yok → Tomcat tüm arayüzlere bağlanır
        assertThatThrownBy(() -> denetle(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> denetle("  ")).isInstanceOf(IllegalStateException.class);
    }

    private static ConfigurableApplicationContext ac(String... ek) {
        List<String> arg = new ArrayList<>(List.of("--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:bindadresi;DB_CLOSE_DELAY=-1",
                "--spring.ai.openai.api-key=",
                "--app.hafiza-dir=target/test-hafiza-bind",
                "--app.rapor-dir=target/test-raporlar-bind"));
        arg.addAll(List.of(ek));
        return new SpringApplicationBuilder(AiOgretmenApplication.class).run(arg.toArray(String[]::new));
    }

    @Test
    void baglamTumArayuzlerdeProfilsizAcilmaz() {
        assertThatThrownBy(() -> ac("--server.address=0.0.0.0").close())
                .hasStackTraceContaining("ADR-0018");
    }

    @Test
    void baglamLoopbackProfilsizAcilir() {
        try (ConfigurableApplicationContext c = ac("--server.address=127.0.0.1")) {
            assertThat(c.isActive()).isTrue();
        }
    }
}
