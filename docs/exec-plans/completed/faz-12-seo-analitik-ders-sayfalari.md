# ExecPlan: Faz 12 — SEO, Google Analytics 4 ve herkese açık JS'siz ders sayfaları

> Tamamlandı (2026-11-27). Karar kayıtları: [ADR-0019](../../adr/0019-google-analytics-4-ve-seo.md) ·
> [ADR-0020](../../adr/0020-herkese-acik-ders-sayfalari-ve-llms-txt.md)

## Neden önemli (kullanıcı bakışı)
Site canlıya çıktı ama kimse bulamazsa işe yaramaz. Kullanıcı iki şey istedi: sitenin arama motorlarında ve AI
arama araçlarında (ChatGPT, Claude, Perplexity) bulunması; ve kaç kişinin geldiğini görmesi. Uygulama tek sayfalık
bir JavaScript uygulaması olduğundan çoğu AI tarayıcısı içeriği göremiyordu; dersleri sunucuda düz HTML olarak da
yayımlamak gerekti.

## Kullanıcı kararları
1. Çerez onay penceresi YOK (ADR-0019); Google'a ziyaretçi kimliği, ilerleme kodu, sohbet metni ya da anahtar gitmez.
2. GA4 mülkü "SokratAI"; ölçüm kimliği yalnız ortam değişkeniyle verilir (`APP_SITE_GA_OLCUM_KIMLIGI`); boşsa analitik
   tamamen kapalı. Yerel mod izlenmez.
3. Mülakat içeriği hiçbir herkese açık sayfada yayımlanmaz.
4. AI tarayıcılarına açık izin: GPTBot, OAI-SearchBot, ClaudeBot, Claude-SearchBot, PerplexityBot, Google-Extended,
   Bingbot, Applebot.

## Yapılanlar
- SEO: Türkçe başlık/açıklama, Open Graph + Twitter kartı, schema.org `Course` (JSON-LD), `favicon.svg`,
  `robots.txt`, `sitemap.xml` (`APP_SITE_TEMEL_URL=https://sokratai.tech` ile canonical); `/api/**` yanıtlarında
  `X-Robots-Tag: noindex`.
- GA4: sanal sayfa görüntülemeleri ve sabit adlı olaylar (`gorev_tamamlandi`, `sohbet_mesaji`, `anahtar_eklendi`);
  CSP yalnız gerekli Google adreslerine genişletildi; GA4 Gelişmiş ölçümde tarayıcı geçmişi, site içi arama ve form
  etkileşimleri KAPALI.
- Herkese açık, JS'siz, çerezsiz ders sayfaları: `/dersler`, `/hafta/N`, `/hafta/N/gun/G`, `/kaynak/<slug>`
  (12 hafta, 60 gün, 38 kaynak çevirisi); `/llms.txt` ve `/llms-full.txt`; sitemap 112 adres. Sayfalar açılışta bir
  kez üretilir, gzip ile sunulur; SVG diyagramlar izin listesi süzgecinden geçer (mülakat diyagramları dahil).
- Google Search Console: alan adı mülkü DNS TXT kaydıyla doğrulandı (TXT KALICI kalmalı), sitemap gönderildi.
  Bing Webmaster: Search Console'dan içe aktarıldı.

## Doğrulama
- `./mvnw verify` yeşil; yeni sayfalar, robots/sitemap, `llms.txt`, SVG süzgeci ve GA yokluğu/varlığı için testler.
- Canlı denetim: `/llms.txt`, `/hafta/1/gun/1` (200, çerez yok), gzip, sitemap adres sayısı, robots grupları
  (`docs/kurulum-site.md` §10.7).

## İncelemelerin yakaladıkları
- SEO/GA ve ders sayfaları için ayrı inceleme düzeltme turları yapıldı (inceleme düzeltmeleri, GA, önceden üretim).
- Gizlilik açıklamasında gtag.js'in kendi eklediği verilerin belirtilmemesi; metin düzeltildi.
- Ders sayfası bağlantılarında sorgudan sonra gelen `#parça`nın kaybolması (Faz 13'te kapandı).
- Bir mülakat diyagramındaki `font-family` yazım hatası SVG süzgeci incelenirken bulundu ve düzeltildi.

## Sıradakiler
- Google/Bing dizinlemesi ve GA4 verisi zamanla dolacak: izle Search Console Sayfalar/Performans, Bing AI Performance,
  GA4 Gerçek zamanlı.
- `og:image` (1200x630 PNG) eklenmedi; ikili dosya sızıntı taramasının izin listesine girmeden işlenemez.
- Siteyi paylaşmak (LinkedIn, GitHub README, topluluklar) bulunabilirliği en çok artıran adımdır.
