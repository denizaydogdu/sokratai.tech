package tr.velzon.aiogretmen.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Service;
import tr.velzon.aiogretmen.repo.ContentRepo;
import tr.velzon.aiogretmen.repo.KaynakRepo;
import tr.velzon.aiogretmen.types.Gun;
import tr.velzon.aiogretmen.types.Hafta;
import tr.velzon.aiogretmen.types.Kaynak;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Herkese açık, salt okunur ders içeriği (Faz 12, ADR-0020): JS çalıştırmayan arama motoru ve AI tarayıcıları için
 * sunucuda çizilen sayfaların verisi. Yalnız müfredat haftaları ({@code content/hafta-NN}) ve Türkçe kaynak metinleri
 * ({@code content/kaynaklar}); mülakat içeriği (ADR-0011) ve atölye bu servise HİÇ bağlanmaz.
 *
 * <p>Ziyaretçi/ilerleme bilgisi YOK: haftalık kilit, görev durumu, quiz puanı uygulanmaz — her şey okunur. Quiz
 * cevapları yayımlanmaz (yalnız ders metni + görev metni).
 *
 * <p>Markdown güvenliği: {@code escapeHtml(true)} (içerikteki ham HTML kaçışlı metin olur) + {@code sanitizeUrls(true)}
 * ({@code javascript:} gibi bağlantı şemaları atılır); yalnız {@link SvgSuzgeci} izin listesine uyan ```svg
 * diyagramları canlı açılır ({@link KaynakService#semalariAc}).
 */
@Service
public class AcikDersService {

    /** Herkese açık URL'de kullanılabilen slug (dosya adı bu biçimde değilse sayfa/site haritası dışında kalır). */
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{0,99}$");
    private static final Pattern ISO_TARIH = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private static final List<Extension> MD_EKLENTILER = List.of(TablesExtension.create());
    private final Parser mdParser = Parser.builder().extensions(MD_EKLENTILER).build();
    /** İçerikteki repo-göreli bağlantıların gittiği yer (kaynak kodun herkese açık deposu). */
    public static final String DEPO_URL = "https://github.com/denizaydogdu/sokratai.tech/blob/main/";
    private static final Pattern SEMA = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");
    private static final Pattern KAYNAK_DOSYASI = Pattern.compile("^content/kaynaklar/([a-z0-9][a-z0-9-]{0,99})\\.md$");
    private final TextContentRenderer metinRenderer = TextContentRenderer.builder().build();

    private final ContentRepo contentRepo;
    private final KaynakRepo kaynakRepo;

    public AcikDersService(ContentRepo contentRepo, KaynakRepo kaynakRepo) {
        this.contentRepo = contentRepo;
        this.kaynakRepo = kaynakRepo;
    }

    public List<Hafta> haftalar() {
        return contentRepo.haftalar();
    }

    public Optional<Hafta> hafta(int no) {
        return contentRepo.hafta(no);
    }

    public Optional<Gun> gun(int hafta, int gun) {
        return hafta(hafta).flatMap(h -> h.gunler().stream().filter(g -> g.gun() == gun).findFirst());
    }

    /** @return URL'ye uygun slug'lı tüm kaynak metinleri (gövdeli), dosya adı sırasıyla. */
    public List<Kaynak> kaynaklar() {
        return kaynakRepo.hepsi().stream().filter(k -> SLUG.matcher(k.slug()).matches()).toList();
    }

    /** @return slug biçimsizse ya da yoksa boş (dışarıdan gelen metin yalnız biçim denetiminden sonra aranır). */
    public Optional<Kaynak> kaynak(String slug) {
        if (slug == null || !SLUG.matcher(slug).matches()) {
            return Optional.empty();
        }
        return kaynakRepo.bul(slug);
    }

    public List<Kaynak> haftaninKaynaklari(int hafta) {
        return kaynaklar().stream().filter(k -> k.etiketler().contains(hafta)).toList();
    }

    /**
     * Markdown → güvenli HTML parçası (ham HTML kaçışlı, güvensiz bağlantı şeması atılır, izinli SVG açılır).
     * Repo-göreli bağlantılar {@code tabanDizin}'e (ör. {@code content/kaynaklar}) göre çözülür: başka bir kaynak
     * metnine giden ({@code x.md}) → {@code /kaynak/x}; repodaki diğer dosyalar → {@link #DEPO_URL}. {@code http(s):},
     * {@code #…}, {@code /…} ve başka şemalar dokunulmaz.
     */
    public String html(String md, String tabanDizin) {
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(MD_EKLENTILER).escapeHtml(true).sanitizeUrls(true)
                .attributeProviderFactory(c -> (node, etiket, oznitelikler) -> {
                    if (node instanceof org.commonmark.node.Link && oznitelikler.containsKey("href")) {
                        oznitelikler.put("href", baglantiCoz(oznitelikler.get("href"), tabanDizin));
                    }
                })
                .build();
        return KaynakService.semalariAc(renderer.render(mdParser.parse(md == null ? "" : md)));
    }

    /** Repo-göreli bağlantıyı sitede çalışan mutlak adrese çevirir (bkz. {@link #html}). */
    String baglantiCoz(String href, String tabanDizin) {
        if (href == null || href.isEmpty() || href.startsWith("#") || href.startsWith("/")
                || SEMA.matcher(href).find()) {
            return href;
        }
        int kes = href.length();
        for (char c : new char[]{'#', '?'}) {
            int i = href.indexOf(c);
            if (i >= 0 && i < kes) {
                kes = i;
            }
        }
        String yol = href.substring(0, kes);
        String ek = href.substring(kes);
        java.util.Deque<String> parcalar = new java.util.ArrayDeque<>();
        for (String p : (tabanDizin + "/" + yol).split("/")) {
            if (p.isEmpty() || p.equals(".")) {
                continue;
            }
            if (p.equals("..")) {
                if (parcalar.isEmpty()) {
                    return href; // repo kökünün dışına çıkıyor — dokunma
                }
                parcalar.removeLast();
            } else {
                parcalar.addLast(p);
            }
        }
        String cozulen = String.join("/", parcalar);
        java.util.regex.Matcher m = KAYNAK_DOSYASI.matcher(cozulen);
        if (m.matches() && kaynak(m.group(1)).isPresent()) {
            return kaynakYolu(m.group(1)) + (ek.startsWith("#") ? ek : "");
        }
        return DEPO_URL + cozulen.replace(" ", "%20") + ek;
    }

    /** @return ```svg blokları tamamen çıkarılmış markdown (llms-full.txt için; diyagram markup'ı metin değil). */
    public static String semasiz(String md) {
        return KaynakService.SVG_MD_FENCE.matcher(md == null ? "" : md).replaceAll("").replaceAll("\n{3,}", "\n\n")
                .strip();
    }

    /**
     * Sayfa açıklaması (meta description): başlıklar ve diyagramlar atlanır, ilk paragrafların düz metni; boşluklar
     * tek boşluğa iner, {@code sinir} karakteri aşarsa sözcük sınırında kesilip {@code …} eklenir.
     */
    public String ozet(String md, int sinir) {
        Node belge = mdParser.parse(semasiz(md));
        StringBuilder sb = new StringBuilder();
        for (Node n = belge.getFirstChild(); n != null && sb.length() <= sinir; n = n.getNext()) {
            if (n instanceof Paragraph) {
                sb.append(metinRenderer.render(n)).append(' ');
            }
        }
        return kirp(sb.toString(), sinir);
    }

    /** Boşlukları sadeleştirir; uzunsa sözcük sınırında keser ve {@code …} ekler. */
    public static String kirp(String metin, int sinir) {
        String s = metin == null ? "" : metin.replaceAll("\\s+", " ").strip();
        if (s.length() <= sinir) {
            return s;
        }
        String kes = s.substring(0, sinir - 1);
        int bosluk = kes.lastIndexOf(' ');
        if (bosluk > sinir / 2) {
            kes = kes.substring(0, bosluk);
        }
        return kes.replaceAll("[\\s,;:.\\-—]+$", "") + "…";
    }

    /** Herkese açık sayfanın kökten yolu ve (biliniyorsa) son değişiklik tarihi — site haritası satırı. */
    public record SayfaYolu(String yol, LocalDate sonDegisiklik) {
    }

    public static String haftaYolu(int hafta) {
        return "/hafta/" + hafta;
    }

    public static String gunYolu(int hafta, int gun) {
        return "/hafta/" + hafta + "/gun/" + gun;
    }

    public static String kaynakYolu(String slug) {
        return "/kaynak/" + slug;
    }

    /**
     * @return {@code /dersler} + her hafta + her gün + her kaynak sayfası (ana sayfa HARİÇ — o SPA kabuğudur);
     *         mülakat/atölye hiçbir zaman. {@code lastmod} yalnız kaynak metinlerinde (frontmatter {@code tarih});
     *         ders dosyalarının zamanı dağıtımda (git checkout / Docker COPY) ilerlediğinden yanıltıcı olurdu.
     */
    public List<SayfaYolu> sayfaYollari() {
        List<SayfaYolu> yollar = new java.util.ArrayList<>();
        yollar.add(new SayfaYolu("/dersler", null));
        for (Hafta h : haftalar()) {
            yollar.add(new SayfaYolu(haftaYolu(h.no()), null));
            for (Gun g : h.gunler()) {
                yollar.add(new SayfaYolu(gunYolu(h.no(), g.gun()), null));
            }
        }
        for (Kaynak k : kaynaklar()) {
            yollar.add(new SayfaYolu(kaynakYolu(k.slug()), kaynakTarihi(k).orElse(null)));
        }
        return List.copyOf(yollar);
    }

    /** Kaynak tarihi: frontmatter {@code tarih} geçerli bir YYYY-AA-GG ise o; değilse yok. */
    public Optional<LocalDate> kaynakTarihi(Kaynak k) {
        if (k.tarih() != null && ISO_TARIH.matcher(k.tarih()).matches()) {
            try {
                return Optional.of(LocalDate.parse(k.tarih()));
            } catch (DateTimeParseException e) {
                // geçersiz takvim tarihi (ör. 2026-02-31) → lastmod yazılmaz
            }
        }
        return Optional.empty();
    }
}
