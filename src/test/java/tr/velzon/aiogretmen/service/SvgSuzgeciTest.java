package tr.velzon.aiogretmen.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Faz 12 (ADR-0020): canlı açılacak SVG için izin listesi — olay özniteliği, betik, bağlantı geçemez. */
class SvgSuzgeciTest {

    @Test
    void duzPresentationSvgGecer() {
        assertThat(SvgSuzgeci.guvenliMi("<svg viewBox=\"0 0 10 10\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\""
                + " aria-label=\"Ports &amp; Adapters\"><defs><marker id=\"m-ok\" markerWidth=\"6\" markerHeight=\"6\""
                + " refX=\"3\" refY=\"3\" orient=\"auto\"><path d=\"M0,0 L6,3 L0,6 z\" fill=\"#333\"/></marker></defs>"
                + "<line x1=\"0\" y1=\"0\" x2=\"5\" y2=\"5\" stroke=\"#000\" marker-end=\"url(#m-ok)\"/>"
                + "<text x=\"1\" y=\"2\" font-size=\"10\">a &lt; b &amp; \"c\"</text></svg>")).isTrue();
    }

    @Test
    void tehlikeliIcerikGecmez() {
        List<String> kotu = List.of(
                "<svg onload=\"alert(1)\"></svg>",
                "<svg ONLOAD=\"alert(1)\"></svg>",
                "<svg><rect onclick=\"x()\"/></svg>",
                "<svg><script>alert(1)</script></svg>",
                "<svg><foreignObject><div>x</div></foreignObject></svg>",
                "<svg><a href=\"javascript:alert(1)\"><text>x</text></a></svg>",
                "<svg><use xlink:href=\"#a\"/></svg>",
                "<svg><rect style=\"fill:red\"/></svg>",
                "<svg><rect class=\"k\"/></svg>",
                "<svg><!-- yorum --></svg>",
                "<svg><![CDATA[x]]></svg>",
                "<svg><rect fill='red'/></svg>",
                "<svg><rect fill=red/></svg>",
                "<svg><rect fill=\"url(https://kotu.example/x)\"/></svg>",
                "<svg><rect fill=\"url(javascript:alert(1))\"/></svg>",
                "<svg><rect fill=\"&#106;avascript:x\"/></svg>",
                "<svg><img src=x onerror=alert(1)></svg>",
                "<svg><rect x=\"1\"</svg>",
                "<div><svg></svg></div>",
                "<svg></svg><script>alert(1)</script>",
                "<svg></svg x=\"1\">");
        for (String svg : kotu) {
            assertThat(SvgSuzgeci.guvenliMi(svg)).as(svg).isFalse();
        }
        assertThat(SvgSuzgeci.guvenliMi(null)).isFalse();
    }

    @Test
    void kaynakSvgsiAcilmazsaKacisliKodBlogundaKalir() {
        String html = "<pre><code class=\"language-svg\">&lt;svg onload=&quot;alert(1)&quot;&gt;&lt;/svg&gt;</code></pre>";
        assertThat(KaynakService.semalariAc(html)).isEqualTo(html);
    }

    @Test
    void clipPathMarkerUnitsVeTirnakIcindeBuyuktirGecer() {
        assertThat(SvgSuzgeci.guvenliMi("<svg><defs><clipPath id=\"k\"><rect width=\"1\" height=\"1\"/></clipPath>"
                + "<marker id=\"m\" markerUnits=\"strokeWidth\"><path d=\"M0 0\"/></marker></defs>"
                + "<g clip-path=\"url(#k)\"><text aria-label=\"a > b\">x</text></g></svg>")).isTrue();
    }

    @Test
    void yorumlarDogrulamadanVeCiktidanOnceSilinir() {
        assertThat(SvgSuzgeci.yorumsuz("<svg><!-- not --><rect/><!--\nçok satır\n--></svg>"))
                .isEqualTo("<svg><rect/></svg>");
        assertThat(SvgSuzgeci.guvenliMi(SvgSuzgeci.yorumsuz("<svg><!-- x --><rect/></svg>"))).isTrue();
        String html = "<pre><code class=\"language-svg\">&lt;svg&gt;&lt;!-- not --&gt;&lt;rect/&gt;&lt;/svg&gt;</code></pre>";
        assertThat(KaynakService.semalariAc(html)).isEqualTo("<figure class=\"oku-sema\"><svg><rect/></svg></figure>");
        // yorum içinde gizlenmiş betik de silinir; yorum dışı betik yine reddedilir
        assertThat(SvgSuzgeci.guvenliMi(SvgSuzgeci.yorumsuz("<svg><!-- <script>x</script> --></svg>"))).isTrue();
        assertThat(SvgSuzgeci.guvenliMi(SvgSuzgeci.yorumsuz("<svg><!-- a --><script>x</script></svg>"))).isFalse();
    }

    /** I2: uzun öznitelik değeri yığın taşırmamalı (doğrusal tarama). */
    @Test
    void cokUzunDegerYiginTasirmaz() {
        String d = "M0 0 " + "L1 1 ".repeat(10_000);
        assertThat(d.length()).isGreaterThan(50_000);
        assertThat(SvgSuzgeci.guvenliMi("<svg><path d=\"" + d + "\"/></svg>")).isTrue();
        assertThat(SvgSuzgeci.guvenliMi("<svg><path d=\"" + d + "&#106;\"/></svg>")).isFalse();
    }

    /** Gerçek içerikteki HER diyagram (kaynaklar + mülakat) izin listesine uyar (aksi hâlde kod bloğu görünür). */
    @Test
    void gercekDiyagramlarinHepsiGecer() throws IOException {
        int sayac = 0;
        try (Stream<Path> dosyalar = Stream.concat(Files.list(Path.of("content/kaynaklar")),
                Files.walk(Path.of("content/mulakat")))) {
            for (Path dosya : dosyalar.filter(f -> f.toString().endsWith(".md")).toList()) {
                Matcher m = KaynakService.SVG_MD_FENCE.matcher(Files.readString(dosya));
                while (m.find()) {
                    String blok = m.group();
                    String svg = SvgSuzgeci.yorumsuz(blok.substring("```svg".length(), blok.length() - 3));
                    assertThat(SvgSuzgeci.guvenliMi(svg)).as(dosya + " #" + sayac).isTrue();
                    sayac++;
                }
            }
        }
        assertThat(sayac).isGreaterThan(900);
    }
}
