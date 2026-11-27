package tr.velzon.aiogretmen.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hız sınırı ayarlarının bağlanması (son inceleme kalıntıları #4–#5). Varsayılanların TEK kaynağı
 * {@link SiteAyarlari.Hiz}'daki {@code @DefaultValue}'lardır; {@code application-site.yml} placeholder'ları
 * ortam değişkeni yoksa BOŞ değer verir ve boş değer varsayılana düşer.
 */
@ExtendWith(OutputCaptureExtension.class)
class SiteAyarlariTest {

    @Configuration
    @EnableConfigurationProperties(SiteAyarlari.class)
    static class Yapi {
    }

    private final ApplicationContextRunner calistirici = new ApplicationContextRunner().withUserConfiguration(Yapi.class);

    @Test
    void ayarYoksaVarsayilanlar() {
        calistirici.run(b -> assertThat(b.getBean(SiteAyarlari.class).hiz())
                .isEqualTo(new SiteAyarlari.Hiz(60, 20, 10, 20)));
    }

    /** yml'deki {@code ${APP_SITE_HIZ_…:}} ortam değişkeni yoksa boş dize üretir → @DefaultValue. */
    @Test
    void bosDegerVarsayilanaDuser() {
        calistirici.withPropertyValues("app.site.hiz.yazma-dakika=", "app.site.hiz.sohbet-dakika=",
                        "app.site.hiz.geri-yukle-saat=", "app.site.hiz.yeni-ziyaretci-saat=7")
                .run(b -> assertThat(b.getBean(SiteAyarlari.class).hiz())
                        .isEqualTo(new SiteAyarlari.Hiz(60, 20, 10, 7)));
    }

    @Test
    void sifirSinirBaglamiAcmaz() {
        calistirici.withPropertyValues("app.site.hiz.yazma-dakika=0").run(b -> {
            assertThat(b).hasFailed();
            Throwable t = b.getStartupFailure();
            StringBuilder mesajlar = new StringBuilder();
            while (t != null) {
                mesajlar.append(t.getMessage()).append('\n');
                t = t.getCause();
            }
            assertThat(mesajlar.toString()).contains("en az 1");
        });
    }

    // ---------- Faz 12: GA4 ölçüm kimliği + temel URL (ADR-0019) ----------

    @Test
    void gaVeTemelUrlYoksaBos() {
        calistirici.run(b -> {
            SiteAyarlari a = b.getBean(SiteAyarlari.class);
            assertThat(a.gaOlcumKimligi()).isEmpty();
            assertThat(a.temelUrl()).isEmpty();
        });
    }

    /** yml'deki {@code ${APP_SITE_GA_OLCUM_KIMLIGI:}} değişken yoksa boş verir → analitik tamamen kapalı. */
    @Test
    void bosPlaceholderBosKalir() {
        calistirici.withPropertyValues("app.site.ga-olcum-kimligi=", "app.site.temel-url=")
                .run(b -> {
                    assertThat(b.getBean(SiteAyarlari.class).gaOlcumKimligi()).isEmpty();
                    assertThat(b.getBean(SiteAyarlari.class).temelUrl()).isEmpty();
                });
    }

    @Test
    void gecerliGaKimligiKorunur() {
        calistirici.withPropertyValues("app.site.ga-olcum-kimligi= G-AB12CD34EF ")
                .run(b -> assertThat(b.getBean(SiteAyarlari.class).gaOlcumKimligi()).isEqualTo("G-AB12CD34EF"));
    }

    /** Biçim dışı kimlik (UA-, küçük harf, betik enjeksiyonu…) BOŞ sayılır + WARN — açılış düşmez, analitik kapalı. */
    @Test
    void gecersizGaKimligiBosSayilirVeUyarir(CapturedOutput cikti) {
        for (String kotu : new String[]{"UA-12345-1", "g-abcd1234", "G-ABC", "G-ABCD1234\"><script>",
                "G-" + "A".repeat(21), "AW-12345678"}) {
            assertThat(SiteAyarlari.gaKimligiNormallestir(kotu)).as(kotu).isEmpty();
        }
        assertThat(cikti.getOut()).contains("APP_SITE_GA_OLCUM_KIMLIGI");
        assertThat(cikti.getOut()).doesNotContain("<script>"); // geçersiz değer loga ham yazılmaz
    }

    @Test
    void temelUrlSondakiEgikCizgisizSaklanir() {
        assertThat(SiteAyarlari.temelUrlNormallestir("https://ornek.example/", false)).isEqualTo("https://ornek.example");
        assertThat(SiteAyarlari.temelUrlNormallestir(" https://egitim.ornek.example ", false))
                .isEqualTo("https://egitim.ornek.example");
        assertThat(SiteAyarlari.temelUrlNormallestir(null, false)).isEmpty();
    }

    /** M1: canlıda yalnız https; http yalnız yerel http denemesinde ({@code guvenli-cerez=false}). */
    @Test
    void httpYalnizGuvensizCerezDenemesinde(CapturedOutput cikti) {
        assertThat(SiteAyarlari.temelUrlNormallestir("http://ornek.example", false)).isEmpty();
        assertThat(cikti.getOut()).contains("APP_SITE_TEMEL_URL");
        assertThat(SiteAyarlari.temelUrlNormallestir("http://127.0.0.1:8090", true)).isEqualTo("http://127.0.0.1:8090");
        assertThat(SiteAyarlari.temelUrlNormallestir("https://ornek.example", true)).isEqualTo("https://ornek.example");
        calistirici.withPropertyValues("app.site.temel-url=http://ornek.example")
                .run(b -> assertThat(b.getBean(SiteAyarlari.class).temelUrl()).isEmpty());
        calistirici.withPropertyValues("app.site.temel-url=http://127.0.0.1:8090", "app.site.guvenli-cerez=false")
                .run(b -> assertThat(b.getBean(SiteAyarlari.class).temelUrl()).isEqualTo("http://127.0.0.1:8090"));
    }

    /** Yol, sorgu, tırnak, şemasız adres → boş + WARN (canonical/sitemap'e keyfi metin girmesin). */
    @Test
    void gecersizTemelUrlBosSayilir(CapturedOutput cikti) {
        for (String kotu : new String[]{"ornek.example", "https://ornek.example/yol", "https://ornek.example?x=1",
                "https://ornek\".example", "javascript:alert(1)", "ftp://ornek.example"}) {
            assertThat(SiteAyarlari.temelUrlNormallestir(kotu, true)).as(kotu).isEmpty();
        }
        assertThat(cikti.getOut()).contains("APP_SITE_TEMEL_URL");
    }
}
