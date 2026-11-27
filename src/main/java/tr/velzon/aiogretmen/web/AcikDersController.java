package tr.velzon.aiogretmen.web;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tr.velzon.aiogretmen.config.SiteAyarlari;
import tr.velzon.aiogretmen.config.TaramaYolu;
import tr.velzon.aiogretmen.service.AcikDersService;
import tr.velzon.aiogretmen.types.Gun;
import tr.velzon.aiogretmen.types.Hafta;
import tr.velzon.aiogretmen.types.Kaynak;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Herkese açık, JS'siz, salt okunur ders sayfaları + {@code llms.txt} (Faz 12, ADR-0020) — yalnız {@code site}
 * profili. Arama motoru ve AI tarayıcılarının çoğu (GPTBot, ClaudeBot, PerplexityBot…) JavaScript çalıştırmaz; SPA
 * içeriği onlara görünmez. Bu uçlar aynı içeriği sunucuda HTML olarak verir:
 * <ul>
 *   <li>{@code /dersler} (program dizini), {@code /hafta/{n}}, {@code /hafta/{n}/gun/{g}}, {@code /kaynak/{slug}}</li>
 *   <li>{@code /llms.txt} (llmstxt.org biçimi) ve {@code /llms-full.txt} (tüm ders + kaynak metinleri, markdown)</li>
 * </ul>
 * İçerik çalışırken değişmediğinden TÜM sayfalar açılışta bir kez üretilir (istekte yalnız sözlükten sunulur; bozuk
 * içerik açılışı düşürür). {@code APP_SITE_GA_OLCUM_KIMLIGI} ayarlıysa sayfalar harici {@code /s.js} ile sayfa
 * görüntülemesi gönderir (satır içi betik yok); yoksa betik etiketi hiç yok, CSP {@code script-src 'none'}.
 * Haftalık kilit/ilerleme YOK (ziyaretçiye bakılmaz); çerez yazılmaz, ziyaretçi satırı açılmaz, hız kovasına girilmez
 * ({@link TaramaYolu}). Bilinmeyen hafta/gün/slug → Türkçe HTML 404 (JSON değil). Mülakat içeriği hiç bağlanmaz.
 */
@RestController
@Profile("site")
public class AcikDersController {

    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
    private static final MediaType METIN = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
    private static final CacheControl ONBELLEK = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
    private static final Pattern SAYI = Pattern.compile("^[0-9]{1,3}$");
    private static final int ACIKLAMA_SINIRI = 155;

    static final String PROGRAM_ADI = "AI Çağı Yazılım Mimarlığı — 12 Haftalık Yapay Zekâ Mühendisliği Programı";
    static final String PROGRAM_OZETI = "Ücretsiz, Türkçe, 12 haftalık yapay zekâ mühendisliği ve yazılım mimarlığı "
            + "programı: günlük dersler, görevler ve Türkçe kaynak metinleri; Sokratik AI öğretmenle.";

    private final AcikDersService icerik;
    private final String temelUrl;
    private final String gaKimligi;
    private final String sayfaYok;
    private final Map<String, String> sayfalar;
    private final String llms;
    private final String llmsFull;

    public AcikDersController(AcikDersService icerik, SiteAyarlari ayarlar) {
        this.icerik = icerik;
        this.temelUrl = ayarlar.temelUrl();
        this.gaKimligi = ayarlar.gaOlcumKimligi(); // SiteAyarlari'nda doğrulanmış ya da boş
        try {
            this.sayfaYok = new ClassPathResource("static/error/404.html").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("static/error/404.html okunamadı", e);
        }
        // İçerik açılışta yüklenir ve çalışırken değişmez → metin dosyaları bir kez üretilir.
        this.llms = llmsUret();
        this.llmsFull = llmsFullUret();
        this.sayfalar = sayfalariUret();
    }

    // ---------- HTML sayfaları ----------

    @GetMapping(TaramaYolu.DERSLER)
    public ResponseEntity<String> dersler() {
        return sun(TaramaYolu.DERSLER);
    }

    @GetMapping("/hafta/{n}")
    public ResponseEntity<String> hafta(@PathVariable String n) {
        return sayi(n).map(no -> sun(AcikDersService.haftaYolu(no))).orElseGet(this::yok);
    }

    @GetMapping("/hafta/{n}/gun/{g}")
    public ResponseEntity<String> gun(@PathVariable String n, @PathVariable("g") String gunNo) {
        Optional<Integer> hNo = sayi(n);
        Optional<Integer> gNo = sayi(gunNo);
        if (hNo.isEmpty() || gNo.isEmpty()) {
            return yok();
        }
        return sun(AcikDersService.gunYolu(hNo.get(), gNo.get()));
    }

    @GetMapping("/kaynak/{slug}")
    public ResponseEntity<String> kaynak(@PathVariable String slug) {
        return sun(AcikDersService.kaynakYolu(slug));
    }

    /** Açılışta üretilmiş sayfayı sunar; yoksa HTML 404. Yol yalnız sözlük anahtarıdır (dosya/şablon yolu değil). */
    private ResponseEntity<String> sun(String yol) {
        String html = sayfalar.get(yol);
        return html == null ? yok()
                : ResponseEntity.ok().contentType(HTML).cacheControl(ONBELLEK).body(html);
    }

    /** Tüm açık sayfalar (yol → HTML), açılışta bir kez; içerik hatası açılışı düşürür. */
    private Map<String, String> sayfalariUret() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(TaramaYolu.DERSLER, dersleriCiz());
        for (Hafta h : icerik.haftalar()) {
            m.put(AcikDersService.haftaYolu(h.no()), haftaCiz(h));
            for (Gun g : h.gunler()) {
                m.put(AcikDersService.gunYolu(h.no(), g.gun()), gunCiz(h, g));
            }
        }
        for (Kaynak k : icerik.kaynaklar()) {
            m.put(AcikDersService.kaynakYolu(k.slug()), kaynakCiz(k));
        }
        return Map.copyOf(m);
    }

    private String dersleriCiz() {
        StringBuilder g = new StringBuilder();
        g.append("<article class=\"acik-icerik\">\n<h1>").append(AcikSayfa.kac(PROGRAM_ADI)).append("</h1>\n")
                .append("<p class=\"acik-ozet\">").append(AcikSayfa.kac(PROGRAM_OZETI)).append("</p>\n");
        etkilesimliBag(g, "/", "Programı etkileşimli çalış →");
        g.append("<h2>Haftalar</h2>\n<ol class=\"acik-liste\">\n");
        List<Map<String, Object>> parcalar = new ArrayList<>();
        for (Hafta h : icerik.haftalar()) {
            g.append("<li><h3><a href=\"").append(AcikDersService.haftaYolu(h.no())).append("\">Hafta ").append(h.no())
                    .append(": ").append(AcikSayfa.kac(h.baslik())).append("</a></h3>\n<p>")
                    .append(AcikSayfa.kac(h.hedef())).append("</p>\n<ul>\n");
            for (Gun gun : h.gunler()) {
                g.append("<li><a href=\"").append(AcikDersService.gunYolu(h.no(), gun.gun())).append("\">Gün ")
                        .append(gun.gun()).append(": ").append(AcikSayfa.kac(gun.baslik())).append("</a></li>\n");
            }
            g.append("</ul></li>\n");
            parcalar.add(parca("LearningResource", "Hafta " + h.no() + ": " + h.baslik(),
                    AcikDersService.haftaYolu(h.no())));
        }
        g.append("</ol>\n<h2>Kaynak metinleri (Türkçe)</h2>\n<ul class=\"acik-liste\">\n");
        for (Kaynak k : icerik.kaynaklar()) {
            g.append("<li><a href=\"").append(AcikDersService.kaynakYolu(k.slug())).append("\">")
                    .append(AcikSayfa.kac(k.baslik())).append("</a>");
            if (!k.yazar().isBlank()) {
                g.append(" <span class=\"acik-soluk\">— ").append(AcikSayfa.kac(k.yazar())).append("</span>");
            }
            g.append("</li>\n");
        }
        g.append("</ul>\n</article>\n");

        Map<String, Object> ld = program();
        ld.put("@context", "https://schema.org");
        ld.put("hasPart", parcalar);
        return sayfa(new AcikSayfa.Bilgi("Ders İçeriği — 12 Haftalık Yapay Zekâ Mühendisliği Programı | SokratAI",
                "Ders İçeriği — 12 Haftalık Yapay Zekâ Mühendisliği Programı", PROGRAM_OZETI, TaramaYolu.DERSLER,
                "website", ld, List.of(adim("SokratAI", "/"), adim("Ders içeriği", TaramaYolu.DERSLER)),
                g.toString()));
    }

    private String haftaCiz(Hafta h) {
        String ad = "Hafta " + h.no() + ": " + h.baslik();
        StringBuilder g = new StringBuilder();
        g.append("<article class=\"acik-icerik\">\n<h1>").append(AcikSayfa.kac(ad)).append("</h1>\n")
                .append("<p class=\"acik-ozet\"><strong>Hedef:</strong> ").append(AcikSayfa.kac(h.hedef())).append("</p>\n");
        if (!h.kaynak().isBlank()) {
            g.append("<p class=\"acik-soluk\">").append(AcikSayfa.kac(h.kaynak())).append("</p>\n");
        }
        etkilesimliBag(g, "/?hafta=" + h.no(), "Bu haftayı etkileşimli çalış →");
        g.append("<h2>Günler</h2>\n<ol class=\"acik-liste\">\n");
        List<Map<String, Object>> parcalar = new ArrayList<>();
        for (Gun gun : h.gunler()) {
            String yol = AcikDersService.gunYolu(h.no(), gun.gun());
            g.append("<li><a href=\"").append(yol).append("\">Gün ").append(gun.gun()).append(": ")
                    .append(AcikSayfa.kac(gun.baslik())).append("</a> <span class=\"acik-soluk\">— ")
                    .append(AcikSayfa.kac(icerik.ozet(gun.ders(), 140))).append("</span></li>\n");
            parcalar.add(parca("LearningResource", gun.baslik(), yol));
        }
        g.append("</ol>\n");
        kaynakListesi(g, h.no());
        haftaGezinme(g, h.no());
        g.append("</article>\n");

        Map<String, Object> ld = kaynakNesnesi(ad, h.hedef(), AcikDersService.haftaYolu(h.no()));
        ld.put("hasPart", parcalar);
        return sayfa(new AcikSayfa.Bilgi(ad + " | SokratAI", ad, AcikDersService.kirp(h.hedef(), ACIKLAMA_SINIRI),
                AcikDersService.haftaYolu(h.no()), "website", ld,
                List.of(adim("SokratAI", "/"), adim("Ders içeriği", TaramaYolu.DERSLER),
                        adim("Hafta " + h.no(), AcikDersService.haftaYolu(h.no()))),
                g.toString()));
    }

    private String gunCiz(Hafta h, Gun gun) {
        String yol = AcikDersService.gunYolu(h.no(), gun.gun());
        String aciklama = icerik.ozet(gun.ders(), ACIKLAMA_SINIRI);
        StringBuilder g = new StringBuilder();
        g.append("<article class=\"acik-icerik\">\n<p class=\"eyebrow\">Hafta ").append(h.no()).append(" · Gün ")
                .append(gun.gun()).append(" — ").append(AcikSayfa.kac(h.baslik())).append("</p>\n<h1>")
                .append(AcikSayfa.kac(gun.baslik())).append("</h1>\n");
        etkilesimliBag(g, "/?hafta=" + h.no() + "&amp;gun=" + gun.gun(), "Bu dersi etkileşimli çalış →");
        g.append("<div class=\"ders oku-metin\">\n").append(icerik.html(gun.ders(), String.format("content/hafta-%02d", h.no()))).append("</div>\n");
        if (!gun.gorevler().isEmpty()) {
            g.append("<h2>Görevler</h2>\n<ol class=\"acik-gorevler\">\n");
            for (String gorev : gun.gorevler()) {
                g.append("<li>").append(AcikSayfa.kac(gorev)).append("</li>\n");
            }
            g.append("</ol>\n<p class=\"acik-soluk\">Görevleri işaretlemek, quiz ve AI öğretmenle sohbet için ")
                    .append("etkileşimli uygulamayı kullan.</p>\n");
        }
        kaynakListesi(g, h.no());
        gunGezinme(g, h, gun.gun());
        g.append("</article>\n");

        Map<String, Object> ld = kaynakNesnesi(gun.baslik(), aciklama, yol);
        ld.put("position", gun.gun());
        return sayfa(new AcikSayfa.Bilgi(gun.baslik() + " — Hafta " + h.no() + ", Gün " + gun.gun() + " | SokratAI",
                gun.baslik() + " — Hafta " + h.no() + ", Gün " + gun.gun(), aciklama, yol, "article", ld,
                List.of(adim("SokratAI", "/"), adim("Ders içeriği", TaramaYolu.DERSLER),
                        adim("Hafta " + h.no(), AcikDersService.haftaYolu(h.no())), adim("Gün " + gun.gun(), yol)),
                g.toString()));
    }

    private String kaynakCiz(Kaynak k) {
        String yol = AcikDersService.kaynakYolu(k.slug());
        String aciklama = icerik.ozet(k.govde(), ACIKLAMA_SINIRI);
        StringBuilder g = new StringBuilder();
        g.append("<article class=\"acik-icerik\">\n<p class=\"eyebrow\">Kaynak metni (Türkçe)</p>\n<h1>")
                .append(AcikSayfa.kac(k.baslik())).append("</h1>\n<p class=\"acik-soluk\">");
        if (!k.yazar().isBlank()) {
            g.append(AcikSayfa.kac(k.yazar()));
        }
        if (!k.tarih().isBlank()) {
            g.append(k.yazar().isBlank() ? "" : " · ").append(AcikSayfa.kac(k.tarih()));
        }
        g.append("</p>\n");
        if (ozgunUrl(k).isPresent()) {
            g.append("<p class=\"acik-soluk\">Özgün kaynak: <a href=\"").append(AcikSayfa.kac(ozgunUrl(k).get()))
                    .append("\" rel=\"nofollow noopener\">").append(AcikSayfa.kac(ozgunUrl(k).get())).append("</a></p>\n");
        }
        if (!k.etiketler().isEmpty()) {
            g.append("<p class=\"acik-soluk\">İlgili haftalar: ");
            String ayrac = "";
            for (int no : k.etiketler()) {
                if (icerik.hafta(no).isEmpty()) {
                    continue;
                }
                g.append(ayrac).append("<a href=\"").append(AcikDersService.haftaYolu(no))
                        .append("\">Hafta ").append(no).append("</a>");
                ayrac = " · ";
            }
            g.append("</p>\n");
        }
        etkilesimliBag(g, "/", "Programı etkileşimli çalış →");
        g.append("<div class=\"oku-metin\">\n").append(icerik.html(k.govde(), "content/kaynaklar")).append("</div>\n</article>\n");

        Map<String, Object> ld = kaynakNesnesi(k.baslik(), aciklama, yol);
        ozgunUrl(k).ifPresent(u -> ld.put("isBasedOn", u));
        return sayfa(new AcikSayfa.Bilgi(k.baslik() + " | SokratAI", k.baslik(), aciklama, yol, "article", ld,
                List.of(adim("SokratAI", "/"), adim("Ders içeriği", TaramaYolu.DERSLER), adim(k.baslik(), yol)),
                g.toString()));
    }

    // ---------- llms.txt ----------

    @GetMapping(TaramaYolu.LLMS)
    public ResponseEntity<String> llmsTxt() {
        return ResponseEntity.ok().contentType(METIN).cacheControl(ONBELLEK).body(llms);
    }

    @GetMapping(TaramaYolu.LLMS_FULL)
    public ResponseEntity<String> llmsFullTxt() {
        return ResponseEntity.ok().contentType(METIN).cacheControl(ONBELLEK).body(llmsFull);
    }

    private String llmsUret() {
        StringBuilder t = new StringBuilder();
        t.append("# SokratAI\n\n> ").append(PROGRAM_OZETI).append("\n\n")
                .append("SokratAI, Türkçe konuşan yazılımcılar için 12 haftalık bir yapay zekâ mühendisliği ve ")
                .append("yazılım mimarlığı programıdır. Her hafta beş günlük kısa derslere ve uygulamalı görevlere ")
                .append("bölünür; İngilizce kaynaklar Türkçe çalışma metnine çevrilmiştir (teknik terimlerin ")
                .append("İngilizcesi parantez içinde). Tüm dersler ve kaynak metinleri JavaScript gerektirmeyen ")
                .append("HTML sayfaları olarak okunabilir; hepsi tek dosyada: [llms-full.txt](")
                .append(mutlak(TaramaYolu.LLMS_FULL)).append(").\n\n## Program\n\n");
        t.append("- [Ders içeriği (program dizini)](").append(mutlak(TaramaYolu.DERSLER))
                .append("): 12 hafta, günler ve kaynak metinlerinin listesi\n");
        for (Hafta h : icerik.haftalar()) {
            t.append("- [Hafta ").append(h.no()).append(": ").append(mdBaglantiMetni(h.baslik())).append("](")
                    .append(mutlak(AcikDersService.haftaYolu(h.no()))).append("): ").append(tekSatir(h.hedef()))
                    .append('\n');
        }
        t.append("\n## Kaynaklar\n\n");
        for (Kaynak k : icerik.kaynaklar()) {
            t.append("- [").append(mdBaglantiMetni(k.baslik())).append("](")
                    .append(mutlak(AcikDersService.kaynakYolu(k.slug()))).append(')');
            if (!k.yazar().isBlank()) {
                t.append(": ").append(tekSatir(k.yazar()));
            }
            t.append('\n');
        }
        t.append("\n## Hakkında\n\n")
                .append("- [Etkileşimli uygulama](").append(mutlak("/")).append("): ücretsiz ve Türkçe; giriş/kayıt ")
                .append("yok, ilerleme tarayıcıda tutulur. Sokratik AI öğretmen hazır cevap vermez, sorularla ")
                .append("öğrenciyi cevaba yürütür. Sohbet için öğrenci kendi OpenRouter anahtarını getirir ")
                .append("(bring-your-own key); anahtar sunucuda saklanmaz.\n");
        return t.toString();
    }

    private String llmsFullUret() {
        StringBuilder t = new StringBuilder();
        t.append("# SokratAI — tüm ders ve kaynak metinleri\n\n> ").append(PROGRAM_OZETI).append("\n\n")
                .append("Kaynak: ").append(mutlak(TaramaYolu.DERSLER)).append("\n\n# Program\n");
        for (Hafta h : icerik.haftalar()) {
            t.append("\n## Hafta ").append(h.no()).append(": ").append(tekSatir(h.baslik())).append("\n\n")
                    .append("URL: ").append(mutlak(AcikDersService.haftaYolu(h.no()))).append('\n')
                    .append("Hedef: ").append(tekSatir(h.hedef())).append('\n');
            if (!h.kaynak().isBlank()) {
                t.append(tekSatir(h.kaynak())).append('\n');
            }
            for (Gun g : h.gunler()) {
                t.append("\n### Hafta ").append(h.no()).append(" · Gün ").append(g.gun()).append(": ")
                        .append(tekSatir(g.baslik())).append("\n\nURL: ")
                        .append(mutlak(AcikDersService.gunYolu(h.no(), g.gun()))).append("\n\n")
                        .append(AcikDersService.semasiz(g.ders())).append('\n');
                if (!g.gorevler().isEmpty()) {
                    t.append("\n#### Görevler\n\n");
                    g.gorevler().forEach(gr -> t.append("- ").append(tekSatir(gr)).append('\n'));
                }
            }
        }
        t.append("\n# Kaynak metinleri (Türkçe)\n");
        for (Kaynak k : icerik.kaynaklar()) {
            t.append("\n## ").append(tekSatir(k.baslik())).append("\n\nURL: ")
                    .append(mutlak(AcikDersService.kaynakYolu(k.slug()))).append('\n');
            if (!k.yazar().isBlank()) {
                t.append("Yazar: ").append(tekSatir(k.yazar())).append('\n');
            }
            ozgunUrl(k).ifPresent(u -> t.append("Özgün kaynak: ").append(u).append('\n'));
            t.append('\n').append(AcikDersService.semasiz(k.govde())).append('\n');
        }
        return t.toString();
    }

    // ---------- yardımcılar ----------

    private String sayfa(AcikSayfa.Bilgi bilgi) {
        return AcikSayfa.ciz(bilgi, temelUrl, gaKimligi);
    }

    /** Bilinmeyen hafta/gün/slug: Türkçe HTML 404 (JSON hata sözleşmesi yalnız {@code /api/**} içindir). */
    private ResponseEntity<String> yok() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(HTML).cacheControl(CacheControl.noCache())
                .body(sayfaYok);
    }

    private static Optional<Integer> sayi(String s) {
        return s != null && SAYI.matcher(s).matches() ? Optional.of(Integer.parseInt(s)) : Optional.empty();
    }

    private static void etkilesimliBag(StringBuilder g, String hedefHtml, String metin) {
        g.append("<p class=\"acik-cta\"><a class=\"btn\" href=\"").append(hedefHtml).append("\">")
                .append(AcikSayfa.kac(metin)).append("</a></p>\n");
    }

    private void kaynakListesi(StringBuilder g, int hafta) {
        List<Kaynak> kaynaklar = icerik.haftaninKaynaklari(hafta);
        if (kaynaklar.isEmpty()) {
            return;
        }
        g.append("<h2>Bu haftanın kaynak metinleri</h2>\n<ul class=\"acik-liste\">\n");
        for (Kaynak k : kaynaklar) {
            g.append("<li><a href=\"").append(AcikDersService.kaynakYolu(k.slug())).append("\">")
                    .append(AcikSayfa.kac(k.baslik())).append("</a></li>\n");
        }
        g.append("</ul>\n");
    }

    private void haftaGezinme(StringBuilder g, int no) {
        g.append("<nav class=\"acik-gezinme\" aria-label=\"Haftalar arası\">\n");
        icerik.hafta(no - 1).ifPresent(o -> g.append("<a rel=\"prev\" href=\"").append(AcikDersService.haftaYolu(o.no()))
                .append("\">← Hafta ").append(o.no()).append("</a>\n"));
        icerik.hafta(no + 1).ifPresent(s -> g.append("<a rel=\"next\" href=\"").append(AcikDersService.haftaYolu(s.no()))
                .append("\">Hafta ").append(s.no()).append(" →</a>\n"));
        g.append("</nav>\n");
    }

    private void gunGezinme(StringBuilder g, Hafta h, int gunNo) {
        g.append("<nav class=\"acik-gezinme\" aria-label=\"Günler arası\">\n");
        List<Gun> gunler = h.gunler();
        for (int i = 0; i < gunler.size(); i++) {
            if (gunler.get(i).gun() != gunNo) {
                continue;
            }
            if (i > 0) {
                g.append("<a rel=\"prev\" href=\"").append(AcikDersService.gunYolu(h.no(), gunler.get(i - 1).gun()))
                        .append("\">← Gün ").append(gunler.get(i - 1).gun()).append("</a>\n");
            }
            g.append("<a href=\"").append(AcikDersService.haftaYolu(h.no())).append("\">Hafta ").append(h.no())
                    .append(" özeti</a>\n");
            if (i < gunler.size() - 1) {
                g.append("<a rel=\"next\" href=\"").append(AcikDersService.gunYolu(h.no(), gunler.get(i + 1).gun()))
                        .append("\">Gün ").append(gunler.get(i + 1).gun()).append(" →</a>\n");
            } else {
                icerik.hafta(h.no() + 1).ifPresent(s -> g.append("<a rel=\"next\" href=\"")
                        .append(AcikDersService.haftaYolu(s.no())).append("\">Hafta ").append(s.no()).append(" →</a>\n"));
            }
        }
        g.append("</nav>\n");
    }

    /** Programın kendisi (schema.org {@code Course}). */
    private Map<String, Object> program() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("@type", "Course");
        m.put("name", PROGRAM_ADI);
        m.put("description", PROGRAM_OZETI);
        m.put("inLanguage", "tr");
        m.put("isAccessibleForFree", true);
        if (!temelUrl.isEmpty()) {
            m.put("url", temelUrl + TaramaYolu.DERSLER);
        }
        m.put("provider", Map.of("@type", "Organization", "name", AcikSayfa.SITE_ADI));
        return m;
    }

    /** Ders/hafta/kaynak sayfası (schema.org {@code LearningResource}, programın parçası). */
    private Map<String, Object> kaynakNesnesi(String ad, String aciklama, String yol) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("@context", "https://schema.org");
        m.put("@type", "LearningResource");
        m.put("name", ad);
        m.put("description", AcikDersService.kirp(aciklama, ACIKLAMA_SINIRI));
        m.put("inLanguage", "tr");
        m.put("isAccessibleForFree", true);
        m.put("learningResourceType", "ders");
        if (!temelUrl.isEmpty()) {
            m.put("url", temelUrl + yol);
        }
        m.put("isPartOf", program());
        return m;
    }

    private Map<String, Object> parca(String tur, String ad, String yol) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("@type", tur);
        m.put("name", ad);
        m.put("url", mutlak(yol));
        return m;
    }

    private String mutlak(String yol) {
        return temelUrl + yol;
    }

    private static String[] adim(String ad, String yol) {
        return new String[]{ad, yol};
    }

    /** Özgün kaynak adresi yalnız {@code http(s)://} ise gösterilir (frontmatter'daki keyfi metin bağlantı olmaz). */
    private static Optional<String> ozgunUrl(Kaynak k) {
        String u = k.url() == null ? "" : k.url().strip();
        return u.matches("^https?://[^\\s\"<>]+$") ? Optional.of(u) : Optional.empty();
    }

    private static String tekSatir(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").strip();
    }

    /** Markdown bağlantı metninde köşeli parantez kaçışı. */
    private static String mdBaglantiMetni(String s) {
        return tekSatir(s).replace("[", "\\[").replace("]", "\\]");
    }
}
