# ADR-0020 — Herkese açık, JS'siz ders sayfaları ve llms.txt (Faz 12)

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-27
- **Bağlam sahibi:** kullanıcı kararı (içerik kapsamı) + uygulayıcı kararları (aşağıda işaretli)
- **İlişkili:** ADR-0011 (mülakat modülü), ADR-0017 (girişsiz site), ADR-0019 (SEO + GA4)

## Bağlam
Ön yüz tek sayfalık uygulamadır: dersler `/api/**`'den JS ile çizilir, sunucu yalnız kabuğu (`/`) verir.
Googlebot JS çalıştırır ama geç ve seçici; GPTBot, OAI-SearchBot, ClaudeBot, PerplexityBot gibi AI tarayıcılarının
çoğu hiç çalıştırmaz → site içeriği onlara boş görünür. ADR-0019 bu sınırı "sunucuda üretilen ders sayfaları ister"
diye açık bırakmıştı.

## Karar
**Kullanıcının kararı (2026-11-27):** 12 haftanın TÜM dersleri ve `content/kaynaklar` altındaki TÜM Türkçe kaynak
metinleri (kullanıcıya göre hepsi kamuya açık kaynakların çevirisi) tarayıcılara açılır. Mülakat içeriği
(`content/mulakat`, ADR-0011) DIŞARIDA kalır.

Uygulayıcı kararları:
1. **Sunucuda çizilen salt okunur sayfalar** (yalnız `site` profili, `web/AcikDersController`): `/dersler`,
   `/hafta/{n}`, `/hafta/{n}/gun/{g}`, `/kaynak/{slug}`. Şablon motoru yok (yeni bağımlılık yok): küçük dize
   kurucusu (`web/AcikSayfa`), commonmark (mevcut). Her sayfada benzersiz Türkçe başlık, meta açıklama, canonical
   (`APP_SITE_TEMEL_URL` varsa), Open Graph, JSON-LD (`LearningResource`/`Course`, `isPartOf` program, `inLanguage
   tr`, `isAccessibleForFree`), sayfa yolu (breadcrumb) ve "etkileşimli çalış" bağlantısı (`/?hafta=N&gun=G`; `app.js`
   yalnız 1-2 haneli tam sayı ve var olan haftayı kabul eder, kilit mantığı aynen uygulanır).
2. **Kilit/ilerleme yok, quiz cevabı yok:** sayfalar ziyaretçiye bakmaz; ders metni + görev metni yayımlanır, quiz
   soruları/cevapları yayımlanmaz.
3. **Güvenlik:** markdown `escapeHtml(true)` + `sanitizeUrls(true)`; ```svg diyagramları yalnız izin listesine
   (`service/SvgSuzgeci`: öğe/öznitelik adları, çift tırnaklı değer, `on*`/`href`/`style`/`script`/`foreignObject`/
   yorum/`javascript:`/dış `url()` yok) uyarsa canlı açılır — aynı denetim SPA'nın `/api/kaynak` çıktısına da
   uygulanır. Sayfa CSP'si `script-src 'none'`; JSON-LD `< > &` JSON kaçışlı.
4. **Çerezsiz, sınırsız:** bu yollar `config/TaramaYolu`'na eklendi (GET/HEAD) → ziyaretçi/XSRF çerezi yok,
   ziyaretçi satırı yok; ziyaretçi/hız interceptor'ları zaten yalnız `/api/**`. `Cache-Control: public,
   max-age=3600`. Bilinmeyen hafta/gün/slug → Türkçe HTML 404 (JSON değil).
5. **`/llms.txt`** (llmstxt.org biçimi: `# SokratAI`, `> özet`, açıklama, `## Program`, `## Kaynaklar`,
   `## Hakkında`) ve **`/llms-full.txt`** (tüm ders + kaynak metinleri, SVG'siz markdown, kaynak URL'leriyle;
   `text/plain; charset=utf-8`). İçerik açılışta yüklendiğinden metinler bir kez üretilir.
6. **`sitemap.xml`** ana sayfa + yukarıdaki tüm sayfalar; `lastmod` YALNIZ kaynak metinlerinde (frontmatter
   `tarih`). Ders dosyalarının zamanı dağıtımda (git checkout / Docker COPY) ilerlediğinden ders sayfalarında yok. **`robots.txt`** `User-agent: *` + GPTBot, OAI-SearchBot, ClaudeBot, Claude-SearchBot, PerplexityBot,
   Google-Extended, Bingbot, Applebot için açık `Allow: /` grupları.
7. Ana sayfaya görünür "Ders içeriği" bağlantısı ve `<noscript>` açıklaması (botlar bağlantıyı izler).
8. **Önceden üretim:** içerik çalışırken değişmez → tüm sayfalar açılışta bir kez üretilir, istekte sözlükten
   sunulur; bozuk içerik açılışı düşürür. Site profilinde Tomcat sıkıştırması (`server.compression`, ≥1 KB;
   html/plain/xml/json/css/js) — yeni bağımlılık yok; nginx örneğinde de `gzip on`.
9. **Ölçüm (kullanıcı ziyaretçi sayısını istiyor):** `APP_SITE_GA_OLCUM_KIMLIGI` ayarlıysa sayfalar harici
   `/s.js`'i (`data-k` = doğrulanmış kimlik, `defer`) yükler; betik yalnız başlık + yol ile `page_view` gönderir
   (sorgu dizesi, kimlik, çerez değeri yok; Google sinyalleri/reklam kişiselleştirmesi kapalı). O zaman CSP
   `index.html` ile aynı GA listesi; kimlik yoksa etiket hiç yok ve CSP `script-src 'none'`. Dosya adı nötr
   (takipçi listeleri dosya adına göre engeller; engellenirse sayfa etkilenmez).
10. **Repo-göreli bağlantılar:** içerikteki `x.md` gibi bağlantılar dosyanın dizinine göre çözülür; başka bir
    kaynak metnine gidiyorsa `/kaynak/x`, repodaki başka dosyaysa
    `https://github.com/denizaydogdu/sokratai.tech/blob/main/<yol>`. `http(s)`, `#…`, `/…` ve diğer şemalar
    dokunulmaz; kökün dışına çıkan yol değişmez. (llms-full.txt ham markdown'dır, çevrilmez.)

## Sonuçlar
- Ders ve kaynak metinleri AI modellerinin eğitim/arama dizinlerine girebilir (Google-Extended/GPTBot izinli) —
  kullanıcı kararı. Geri alma: robots.txt'te ilgili gruba `Disallow: /`.
- Ders sayfalarında `lastmod` yok; arama motorları değişikliği taramayla bulur.
- GA4 açıkken JS'siz sayfaları okuyan insanlar da sayılır; JS çalıştırmayan botlar sayılmaz (istenen davranış).
  Bizim gönderdiğimiz yalnız başlık + yol; gtag.js kendi standart verisini (`_ga` çerezi/istemci kimliği, referrer,
  tarayıcı/cihaz bilgisi) ayrıca ekler — ADR-0019.
- SVG izin listesi mülakat diyagramlarına da uygulanır (MulakatService aynı `semalariAc`'ı kullanır); `<!-- -->`
  yorumları doğrulamadan ve çıktıdan önce silinir. Tüm kaynak + mülakat diyagramları testte taranır.
