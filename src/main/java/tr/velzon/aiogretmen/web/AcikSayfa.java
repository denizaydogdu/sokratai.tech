package tr.velzon.aiogretmen.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Herkese açık, JS'siz ders sayfalarının HTML iskeleti (Faz 12, ADR-0020) — küçük dize kurucusu, şablon motoru yok.
 *
 * <p>CSP-temiz: satır içi betik/stil YOK; tek {@code <script>} çalıştırılmayan JSON-LD veri bloğudur ve içindeki
 * {@code < > &} JSON kaçışlarıyla ({@code <}…) yazılır → {@code </script>} ile bloktan çıkılamaz. Tüm metin
 * alanları {@link #kac} ile HTML-kaçışlıdır; yalnız {@code govdeHtml} hazır (güvenli) HTML'dir.
 */
final class AcikSayfa {

    static final String SITE_ADI = "SokratAI";

    /** Sayfanın sabit CSP'si: betik yok (JSON-LD veri bloğu CSP'ye takılmaz), yalnız aynı kökten stil/görsel. */
    static final String CSP = "default-src 'self'; script-src 'none'; img-src 'self' data:; form-action 'self'";

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * @param baslik    {@code <title>} (benzersiz, site adıyla biter)
     * @param ogBaslik  Open Graph başlığı (site adı eksiz)
     * @param aciklama  meta description (≤ ~155 karakter)
     * @param yol       kökten yol ({@code /hafta/1}); canonical = temel URL + yol
     * @param ogTuru    {@code website} | {@code article}
     * @param jsonLd    schema.org nesnesi (JSON'a çevrilir)
     * @param yolHaritasi breadcrumb: (ad, yol) çiftleri; sonuncusu geçerli sayfa (bağlantısız)
     * @param govdeHtml hazır, güvenli HTML gövde
     */
    record Bilgi(String baslik, String ogBaslik, String aciklama, String yol, String ogTuru,
                 Map<String, Object> jsonLd, List<String[]> yolHaritasi, String govdeHtml) {
    }

    private AcikSayfa() {
    }

    static String ciz(Bilgi b, String temelUrl) {
        StringBuilder h = new StringBuilder(8192 + b.govdeHtml().length());
        h.append("<!DOCTYPE html>\n<html lang=\"tr\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("<title>").append(kac(b.baslik())).append("</title>\n")
                .append("<meta name=\"description\" content=\"").append(kac(b.aciklama())).append("\">\n")
                .append("<meta http-equiv=\"Content-Security-Policy\" content=\"").append(CSP).append("\">\n")
                .append("<meta name=\"theme-color\" content=\"#4F46E5\">\n")
                .append("<meta name=\"robots\" content=\"index, follow\">\n");
        if (!temelUrl.isEmpty()) {
            h.append("<link rel=\"canonical\" href=\"").append(kac(temelUrl + b.yol())).append("\">\n")
                    .append("<meta property=\"og:url\" content=\"").append(kac(temelUrl + b.yol())).append("\">\n");
        }
        h.append("<meta property=\"og:type\" content=\"").append(kac(b.ogTuru())).append("\">\n")
                .append("<meta property=\"og:locale\" content=\"tr_TR\">\n")
                .append("<meta property=\"og:site_name\" content=\"").append(SITE_ADI).append("\">\n")
                .append("<meta property=\"og:title\" content=\"").append(kac(b.ogBaslik())).append("\">\n")
                .append("<meta property=\"og:description\" content=\"").append(kac(b.aciklama())).append("\">\n")
                .append("<meta name=\"twitter:card\" content=\"summary\">\n")
                .append("<meta name=\"twitter:title\" content=\"").append(kac(b.ogBaslik())).append("\">\n")
                .append("<meta name=\"twitter:description\" content=\"").append(kac(b.aciklama())).append("\">\n")
                .append("<link rel=\"icon\" href=\"/favicon.svg\" type=\"image/svg+xml\">\n")
                .append("<link rel=\"stylesheet\" href=\"/style.css\">\n")
                .append("<script type=\"application/ld+json\">").append(jsonLd(b.jsonLd())).append("</script>\n")
                .append("</head>\n<body data-sayfa=\"acik\">\n")
                .append("<header>\n  <div class=\"hwrap\">\n    <div class=\"brand\">\n")
                .append("      <span class=\"eyebrow\">AI Öğretmen · 12 Haftalık Program</span>\n")
                .append("      <a class=\"acik-marka\" href=\"/\">SokratAI — Mimar Defteri</a>\n")
                .append("    </div>\n    <a class=\"btn btn-ikincil acik-ust-bag\" href=\"/dersler\">Ders içeriği</a>\n")
                .append("  </div>\n</header>\n<main class=\"acik-sayfa\">\n");
        yolHaritasi(h, b.yolHaritasi());
        h.append(b.govdeHtml())
                .append("</main>\n<footer class=\"acik-alt\">\n  <p><a href=\"/\">Etkileşimli uygulama</a> · ")
                .append("<a href=\"/dersler\">Ders içeriği</a> · <a href=\"/llms.txt\">llms.txt</a></p>\n")
                .append("  <p>SokratAI: ücretsiz, Türkçe, 12 haftalık yapay zekâ mühendisliği programı. Giriş/kayıt yok; ")
                .append("AI öğretmenle sohbet için kendi OpenRouter anahtarını kullanırsın.</p>\n</footer>\n")
                .append("</body>\n</html>\n");
        return h.toString();
    }

    private static void yolHaritasi(StringBuilder h, List<String[]> adimlar) {
        h.append("<nav class=\"acik-yol\" aria-label=\"Sayfa yolu\">\n<ol>\n");
        for (int i = 0; i < adimlar.size(); i++) {
            String[] a = adimlar.get(i);
            if (i == adimlar.size() - 1) {
                h.append("<li aria-current=\"page\">").append(kac(a[0])).append("</li>\n");
            } else {
                h.append("<li><a href=\"").append(kac(a[1])).append("\">").append(kac(a[0])).append("</a></li>\n");
            }
        }
        h.append("</ol>\n</nav>\n");
    }

    /** schema.org nesnesi → {@code <script>} içine güvenle gömülebilen JSON ({@code < > &} kaçışlı). */
    static String jsonLd(Map<String, Object> nesne) {
        try {
            return JSON.writeValueAsString(nesne)
                    .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON-LD üretilemedi", e);
        }
    }

    /** HTML metin + çift tırnaklı öznitelik kaçışı. */
    static String kac(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
