package tr.velzon.aiogretmen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Türkçe içeriğin yazım kuralı mekanik olarak burada zorlanır (docs/yazim-kilavuzu.md §1, Faz 14):
 * ders, kaynak ve atölye metinlerinde ve AI öğretmenin prompt'larında uzun çizgi (—) ve boşluklu kısa çizgi
 * (" – ") yoktur. Prompt'lar da kapsamdadır, çünkü model prompt'un üslubunu taklit eder. Arayüzün HTML dosyaları
 * (index.html, hata sayfaları) da taranır; app.js ve Java dizeleri kod yorumu içerdiğinden incelemeyle denetlenir.
 * Mülakat içeriği (content/mulakat) bu kuralın dışındadır.
 */
class IcerikYazimKurallariTest {

    private static final String YOL_GOSTERICI = """
            [Düzeltme: uzun çizgiyi cümlenin işlevine göre değiştir: açıklama → iki nokta, ek düşünce → yeni \
            cümle, ara söz → virgül ya da parantez, karşıtlık → "ama", başlıkta ayraç → iki nokta. Mekanik \
            "— → ," değişimi yapma; tablo ve örnekler: docs/yazim-kilavuzu.md §1.]""";

    @Test
    void icerikte_ve_promptlarda_uzun_cizgi_yok() throws IOException {
        List<String> ihlaller = new ArrayList<>();
        for (Path dosya : taranacakDosyalar()) {
            List<String> satirlar = Files.readAllLines(dosya, StandardCharsets.UTF_8);
            for (int i = 0; i < satirlar.size(); i++) {
                String satir = satirlar.get(i);
                if (satir.contains("—") || satir.contains(" – ")) {
                    ihlaller.add(dosya + ":" + (i + 1) + ": " + kisalt(satir.strip()));
                }
            }
        }
        assertThat(ihlaller)
                .as("Uzun çizgi (—) ya da boşluklu kısa çizgi (' – ') bulundu (%d satır):%n%s%n%s",
                        ihlaller.size(), String.join("\n", ihlaller.stream().limit(30).toList()), YOL_GOSTERICI)
                .isEmpty();
    }

    @Test
    void tarama_bos_degil() throws IOException {
        // Klasör adı değişirse test sessizce hiçbir şeyi taramadan geçmesin.
        List<Path> dosyalar = taranacakDosyalar();
        assertThat(dosyalar).anyMatch(p -> p.startsWith(Path.of("content", "kaynaklar")));
        assertThat(dosyalar).anyMatch(p -> p.startsWith(Path.of("content", "hafta-01")));
        assertThat(dosyalar).anyMatch(p -> p.startsWith(Path.of("content", "prompt-atolyesi")));
        assertThat(dosyalar).anyMatch(p -> p.startsWith(Path.of("prompts")));
        assertThat(dosyalar).contains(Path.of("src", "main", "resources", "static", "index.html"));
        assertThat(dosyalar).anyMatch(p -> p.startsWith(Path.of("src", "main", "resources", "static", "error")));
        assertThat(dosyalar).noneMatch(p -> p.startsWith(Path.of("content", "mulakat")));
    }

    private static List<Path> taranacakDosyalar() throws IOException {
        Path mulakat = Path.of("content", "mulakat");
        Path statik = Path.of("src", "main", "resources", "static");
        try (Stream<Path> icerik = Files.walk(Path.of("content"));
             Stream<Path> promptlar = Files.list(Path.of("prompts"));
             Stream<Path> hataSayfalari = Files.walk(statik.resolve("error"))) {
            return Stream.of(
                            icerik.filter(p -> !p.startsWith(mulakat)).filter(p -> p.toString().endsWith(".md")),
                            promptlar.filter(p -> p.toString().endsWith(".st")),
                            Stream.of(statik.resolve("index.html")),
                            hataSayfalari.filter(p -> p.toString().endsWith(".html")))
                    .flatMap(s -> s)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }
    }

    private static String kisalt(String s) {
        return s.length() <= 120 ? s : s.substring(0, 117) + "...";
    }
}
