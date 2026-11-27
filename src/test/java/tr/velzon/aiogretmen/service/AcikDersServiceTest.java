package tr.velzon.aiogretmen.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tr.velzon.aiogretmen.repo.ContentRepo;
import tr.velzon.aiogretmen.repo.KaynakRepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Faz 12 (ADR-0020): herkese açık ders içeriğinin verisi ve markdown güvenliği. */
class AcikDersServiceTest {

    @TempDir
    Path kok;

    private AcikDersService servis() throws IOException {
        Path hafta = Files.createDirectories(kok.resolve("hafta-01"));
        Files.writeString(hafta.resolve("hafta.md"), "---\nhafta: 1\nbaslik: \"H1\"\nhedef: \"Hedef\"\n---\n");
        Files.writeString(hafta.resolve("gun-1.md"), "---\nhafta: 1\ngun: 1\nbaslik: \"G1\"\n---\n\n## Ders\n\n"
                + "### Başlık atlanır\nİlk paragraf metni.\n\n## Görevler\n- [ ] Görev bir\n");
        Path kaynaklar = Files.createDirectories(kok.resolve("kaynaklar"));
        Files.writeString(kaynaklar.resolve("iyi-kaynak.md"),
                "---\nbaslik: \"İyi\"\ntarih: \"2026-07-10\"\netiketler: 1\n---\nGövde.");
        Files.writeString(kaynaklar.resolve("tarihsiz.md"), "---\nbaslik: \"Tarihsiz\"\netiketler: 2\n---\nGövde.");
        Files.writeString(kaynaklar.resolve("Kotu Ad.md"), "---\nbaslik: \"URL'siz\"\n---\nGövde.");
        return new AcikDersService(new ContentRepo(kok), new KaynakRepo(kaynaklar));
    }

    @Test
    void hamHtmlVeOlayOznitelikleriCiktiyaUlasmaz() throws IOException {
        String html = servis().html("Metin <script>alert(1)</script> ve <img src=x onerror=alert(1)>\n\n"
                + "[tıkla](javascript:alert(1))\n\n<svg onload=\"alert(1)\"></svg>\n\n"
                + "```svg\n<svg><rect onclick=\"x()\"/></svg>\n```\n", "content/kaynaklar");
        assertThat(html).doesNotContain("<script").doesNotContain("<img").doesNotContain("<svg")
                .doesNotContain("javascript:").contains("&lt;script&gt;");
        assertThat(html.replace("&lt;", "")).doesNotContainPattern("<[^>]*onerror=").doesNotContainPattern("<[^>]*onclick=");
    }

    @Test
    void izinliSvgCanliAcilir() throws IOException {
        String html = servis().html("```svg\n<svg viewBox=\"0 0 10 10\"><rect width=\"1\" height=\"1\"/></svg>\n```\n",
                "content/kaynaklar");
        assertThat(html).contains("<figure class=\"oku-sema\"><svg viewBox=\"0 0 10 10\">");
    }

    @Test
    void ozetBasliklariVeSvgyiAtlarSinirdaKeser() throws IOException {
        AcikDersService s = servis();
        assertThat(s.ozet("### Başlık\nİlk paragraf.\n\n```svg\n<svg></svg>\n```\n\nİkinci.", 155))
                .isEqualTo("İlk paragraf. İkinci.");
        String uzun = s.ozet("kelime ".repeat(100), 155);
        assertThat(uzun).hasSizeLessThanOrEqualTo(155).endsWith("…");
    }

    @Test
    void sayfaYollariDerslerHaftaGunVeUrlUygunKaynaklar() throws IOException {
        List<AcikDersService.SayfaYolu> yollar = servis().sayfaYollari();
        assertThat(yollar).extracting(AcikDersService.SayfaYolu::yol)
                .containsExactly("/dersler", "/hafta/1", "/hafta/1/gun/1", "/kaynak/iyi-kaynak", "/kaynak/tarihsiz");
        // lastmod yalnız frontmatter tarihli kaynakta (ders dosya zamanı dağıtımda ilerler → yazılmaz)
        assertThat(yollar).extracting(AcikDersService.SayfaYolu::sonDegisiklik)
                .containsExactly(null, null, null, LocalDate.of(2026, 7, 10), null);
    }

    @Test
    void slugBicimDenetimi() throws IOException {
        AcikDersService s = servis();
        assertThat(s.kaynak("iyi-kaynak")).isPresent();
        assertThat(s.kaynak("../hafta-01/hafta")).isEmpty();
        assertThat(s.kaynak("Kotu Ad")).isEmpty();
        assertThat(s.haftaninKaynaklari(1)).extracting("slug").containsExactly("iyi-kaynak");
        assertThat(s.gun(1, 1)).isPresent();
        assertThat(s.gun(1, 9)).isEmpty();
        assertThat(s.gun(7, 1)).isEmpty();
    }

    @Test
    void goreliBaglantilarSitedeCalisanAdreseCevrilir() throws IOException {
        AcikDersService s = servis();
        String k = "content/kaynaklar";
        assertThat(s.baglantiCoz("iyi-kaynak.md", k)).isEqualTo("/kaynak/iyi-kaynak");
        assertThat(s.baglantiCoz("./iyi-kaynak.md#bolum-2", k)).isEqualTo("/kaynak/iyi-kaynak#bolum-2");
        assertThat(s.baglantiCoz("yok-boyle.md", k)).isEqualTo(AcikDersService.DEPO_URL + "content/kaynaklar/yok-boyle.md");
        assertThat(s.baglantiCoz("../../docs/adr/0001-x.md", k)).isEqualTo(AcikDersService.DEPO_URL + "docs/adr/0001-x.md");
        assertThat(s.baglantiCoz("../hafta-01/gun-1.md?a=1", k))
                .isEqualTo(AcikDersService.DEPO_URL + "content/hafta-01/gun-1.md?a=1");
        assertThat(s.baglantiCoz("../../../etc/passwd", k)).isEqualTo("../../../etc/passwd");
        for (String dokunma : List.of("https://ornek.example/a", "http://x.y", "#bolum", "/dersler",
                "mailto:a@b.c", "")) {
            assertThat(s.baglantiCoz(dokunma, k)).as(dokunma).isEqualTo(dokunma);
        }
        assertThat(s.html("[a](iyi-kaynak.md) [b](../../src/A.java) [c](https://x.example) [d](javascript:alert(1))", k))
                .contains("href=\"/kaynak/iyi-kaynak\"")
                .contains("href=\"" + AcikDersService.DEPO_URL + "src/A.java\"")
                .contains("href=\"https://x.example\"")
                .doesNotContain("javascript:");
    }
}
