package tr.velzon.aiogretmen.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tr.velzon.aiogretmen.repo.ContentRepo;

import java.nio.file.Path;

/**
 * Dilim testleri ({@code @DataJdbcTest}) için sabit test müfredatı: {@link IlerlemeService} görevin müfredatta
 * var olduğunu denetler (Faz 15). Gerçek {@code content/} elle düzenlendiği için ona bağlanmayız; yoksa bir
 * gün dersinden görev silmek ilgisiz kalıcılık testlerini kırardı. {@code src/test/resources/test-content}:
 * hafta 1 → gün 1'de 2 görev (0, 1), gün 2'de 1 görev (0).
 */
@TestConfiguration
class TestIcerikYapilandirma {

    @Bean
    ContentRepo contentRepo() {
        return new ContentRepo(Path.of("src/test/resources/test-content"));
    }
}
