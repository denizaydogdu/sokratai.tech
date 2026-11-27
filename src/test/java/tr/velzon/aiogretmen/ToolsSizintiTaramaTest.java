package tr.velzon.aiogretmen;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Faz 11 inceleme düzeltmeleri: depo herkese açık; sızıntıya karşı tek koruma {@code tools/sizinti-tara.sh} +
 * {@code tools/git-kancalari/pre-push}. Bunların senaryo testi {@code tools/sizinti-tara-test.sh} (geçici depoda
 * push/tarama: kapalı-güvenli git grep, env dosyaları, commit mesajı, eski geçmiş, silme push'u, yol adları,
 * sembolik bağlar, sha'ya bağlı ikili onayı …) —
 * {@code ./mvnw verify} onu da koşsun diye buradan çağrılır. bash ya da git yoksa atlanır.
 */
class ToolsSizintiTaramaTest {

    private static boolean komutVar(String... komut) {
        try {
            Process p = new ProcessBuilder(komut).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Test
    void sizintiTarayiciVeKancaSenaryolari() throws Exception {
        assumeTrue(komutVar("bash", "-c", "true"), "bash yok — sızıntı tarayıcı testi atlandı");
        assumeTrue(komutVar("git", "--version"), "git yok — sızıntı tarayıcı testi atlandı");

        Path ciktiDosyasi = Files.createTempFile("sizinti-tara-test", ".log");
        try {
            Process p = new ProcessBuilder("bash", "tools/sizinti-tara-test.sh")
                    .directory(new File("."))
                    .redirectErrorStream(true)
                    .redirectOutput(ciktiDosyasi.toFile())
                    .start();
            boolean bitti = p.waitFor(300, TimeUnit.SECONDS);  // ~85 senaryo, yerelde ~50 sn
            if (!bitti) {
                p.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
            String cikti = Files.readString(ciktiDosyasi, StandardCharsets.UTF_8);

            assertThat(bitti).as("sizinti-tara-test.sh zaman aşımı; çıktı:%n%s", cikti).isTrue();
            assertThat(p.exitValue()).as("sizinti-tara-test.sh çıktısı:%n%s", cikti).isZero();
            assertThat(cikti).contains("SONUÇ: tüm sızıntı tarayıcı senaryoları geçti");
        } finally {
            Files.deleteIfExists(ciktiDosyasi);
        }
    }
}
