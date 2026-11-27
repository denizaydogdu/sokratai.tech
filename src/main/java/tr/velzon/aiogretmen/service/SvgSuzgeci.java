package tr.velzon.aiogretmen.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ```svg diyagram bloklarının canlı açılmadan önceki izin listesi denetimi (Faz 12, ADR-0020).
 *
 * <p>Kaynak metinlerdeki diyagramlar yalnız presentation attribute'lu düz SVG'dir (AGENTS.md konvansiyonu:
 * {@code <style>}/{@code class} YOK). Bu sınıf o konvansiyonu MEKANİK olarak zorlar: yalnız izinli öğe ve öznitelik
 * adları, çift tırnaklı değerler; {@code on*} olay öznitelikleri, {@code href}, {@code style}, {@code <script>},
 * {@code <foreignObject>}, CDATA/işlem talimatı (yorumlar {@link #yorumsuz} ile önceden silinir), {@code javascript:} ve {@code #} dışı {@code url(...)}
 * geçemez. Uymayan blok AÇILMAZ — kaçışlı kod bloğu olarak kalır (görünür, çalışmaz).
 *
 * <p>Herkese açık, JS'siz ders sayfaları içeriği her ziyaretçiye ve tarayıcı botuna sunar; CSP ikinci kilittir,
 * bu denetim birincisidir.
 */
public final class SvgSuzgeci {

    private static final Set<String> OGELER = Set.of(
            "svg", "g", "defs", "marker", "clippath", "path", "rect", "line", "text", "tspan", "circle", "ellipse",
            "polygon", "polyline", "title", "desc");

    private static final Set<String> OZNITELIKLER = Set.of(
            "xmlns", "viewbox", "width", "height", "x", "y", "x1", "x2", "y1", "y2", "cx", "cy", "r", "rx", "ry",
            "d", "points", "transform", "fill", "fill-opacity", "stroke", "stroke-width", "stroke-dasharray",
            "stroke-opacity", "stroke-linecap", "stroke-linejoin", "opacity", "font-family", "font-size",
            "font-style", "font-weight", "text-anchor", "dominant-baseline", "id", "marker-end", "marker-start",
            "markerwidth", "markerheight", "markerunits", "clip-path", "refx", "refy", "orient", "role", "aria-label", "dx", "dy",
            "preserveaspectratio", "letter-spacing");

    /** Etiket: {@code <ad (öznitelik="değer")* />} ya da {@code </ad>}; değerde {@code < > "} yok. */
    private static final Pattern ETIKET = Pattern.compile(
            "\\G<(/?)([a-zA-Z][a-zA-Z0-9-]*)((?:\\s+[a-zA-Z][a-zA-Z0-9:-]*\\s*=\\s*\"[^\"<]*\")*)\\s*(/?)>");
    private static final Pattern OZNITELIK = Pattern.compile("([a-zA-Z][a-zA-Z0-9:-]*)\\s*=\\s*\"([^\"<]*)\"");
    /**
     * Değerde {@code &} yalnız adlandırılmış beş temel varlık olarak ({@code &#106;} gibi sayısal başvuru yok).
     * Doğrusal arama ({@code find}): değerin tamamını tekrarlı grupla eşlemek uzun {@code d} değerinde yığın taşırırdı.
     */
    private static final Pattern YASAK_VARLIK = Pattern.compile("&(?!(?:amp|lt|gt|quot|#39);)");
    /** XML yorumu (çok satırlı); doğrulamadan ve çıktıdan önce silinir. */
    private static final Pattern YORUM = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern URL_FN = Pattern.compile("url\\(\\s*([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    private SvgSuzgeci() {
    }

    /**
     * XML yorumlarını siler — {@link #guvenliMi}'den ÖNCE ve çıktıya yazmadan önce uygulanır (yorum içeriği hiç
     * yayımlanmaz). Kapanmamış {@code <!--} kalır ve doğrulamada reddedilir.
     */
    public static String yorumsuz(String svg) {
        return svg == null ? null : YORUM.matcher(svg).replaceAll("");
    }

    /** @return çözülmüş (kaçışsız) SVG metni izin listesine uyuyor ve {@code <svg>…</svg>} ile sarılıysa {@code true}. */
    public static boolean guvenliMi(String svg) {
        if (svg == null) {
            return false;
        }
        String s = svg.strip();
        if (!s.startsWith("<svg") || !s.endsWith("</svg>")) {
            return false;
        }
        int i = 0;
        while (i < s.length()) {
            int lt = s.indexOf('<', i);
            if (lt < 0) {
                break;
            }
            Matcher m = ETIKET.matcher(s);
            m.region(lt, s.length());
            if (!m.lookingAt()) {
                return false; // yorum, CDATA, <!DOCTYPE, <?…?>, tırnaksız/tek tırnaklı öznitelik, yarım etiket
            }
            if (!OGELER.contains(m.group(2).toLowerCase(Locale.ROOT))) {
                return false;
            }
            if (!m.group(1).isEmpty() && (!m.group(3).isEmpty() || !m.group(4).isEmpty())) {
                return false; // kapanış etiketi öznitelik taşımaz
            }
            if (!oznitelikleriGuvenli(m.group(3))) {
                return false;
            }
            i = m.end();
        }
        return true;
    }

    private static boolean oznitelikleriGuvenli(String grup) {
        Matcher a = OZNITELIK.matcher(grup);
        while (a.find()) {
            String ad = a.group(1).toLowerCase(Locale.ROOT);
            String deger = a.group(2).toLowerCase(Locale.ROOT);
            if (!OZNITELIKLER.contains(ad)) {
                return false;
            }
            if (deger.contains("javascript:") || YASAK_VARLIK.matcher(deger).find()) {
                return false; // sayısal karakter başvurusuyla gizlenmiş şema da geçmesin
            }
            Matcher u = URL_FN.matcher(deger);
            while (u.find()) {
                if (!u.group(1).strip().matches("#[a-z0-9_-]+")) {
                    return false; // yalnız aynı belgedeki marker'a başvuru
                }
            }
        }
        return true;
    }
}
