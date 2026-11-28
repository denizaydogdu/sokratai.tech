# ADR-0019 — Google Analytics 4 (onay bandı olmadan) ve SEO etiketleri (Faz 12)

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-27
- **Bağlam sahibi:** kullanıcı kararı (GA4, çerez onay bandı YOK) + uygulayıcı kararları (aşağıda işaretli)
- **İlişkili:** ADR-0017 (girişsiz site, ziyaretçi çerezi), ADR-0018 (Dokploy dağıtımı)

## Bağlam
Site (SokratAI) yayında; kullanıcı ziyaretçi sayısını ("şu an kaç kişi var") görmek ve sitenin aramada
bulunmasını istiyor. Ön yüz satır içi betiksiz, sıkı CSP'li (`default-src 'self'`) tek sayfalık bir uygulama;
ziyaretçi kimliği HttpOnly çerezde, OpenRouter anahtarı yalnız tarayıcıda.

## Karar
**Kullanıcının kararı (aynen, 2026-11-27):** Google Analytics 4 kullanılır; **çerez onay bandı eklenmez**.

Uygulayıcı kararları:
1. **Ortam değişkeniyle aç/kapa:** `APP_SITE_GA_OLCUM_KIMLIGI` (yalnız `site` profilinde ön yüze verilir,
   `/api/ozellikler` → `gaOlcumKimligi`). Boş ya da `^G-[A-Z0-9]{4,20}$` dışı → analitik TAMAMEN kapalı (betik
   yüklenmez, istek gitmez; biçim dışında WARN). Yerel mod asla izlenmez.
2. **CSP yalnız GA alanları için genişler:** `script-src https://www.googletagmanager.com`; `connect-src` ve
   `img-src` `*.google-analytics.com`, `*.analytics.google.com`, `*.googletagmanager.com`. Satır içi betik yok:
   `app.js`'in analitik bölümü gtag.js'i dinamik yükler (ayrı dosya değil: engelleyici dosya adına göre
   engellerse çağrılar siteyi düşürmesin; işlevler hata yutar, gtag yoksa no-op). JSON-LD çalıştırılmayan veri bloğudur, CSP'ye takılmaz.
3. **Kişisel veri gönderilmez:** `user_id` yok; ziyaretçi kimliği, ilerleme kodu, sohbet metni, anahtar Google'a
   gitmez. Yalnız sanal sayfa yolu/başlığı (`/hafta/N/gun/M`, `/kaynak/<slug>`) ve adı sabit olaylar
   (`gorev_tamamlandi` + hafta/gün numarası, `sohbet_mesaji`, `anahtar_eklendi`). Google sinyalleri ve reklam
   kişiselleştirmesi kapalı; `page_location` kökten kurulur (sorgu dizesi taşınmaz). Ön yüz kaynak testleri bekçi.
4. **SEO:** Türkçe başlık/açıklama, Open Graph/Twitter, schema.org `Course`, `favicon.svg`; `robots.txt` ve
   `sitemap.xml` site profilinde dinamik. `robots.txt` `/api/`'yi kapatmaz (Googlebot SPA'yı çizerken bu GET'lere
   ihtiyaç duyar); `/api/**` yanıtları `X-Robots-Tag: noindex` taşır. Alan adı koda gömülmez: `APP_SITE_TEMEL_URL` (yalnız `https://`; `http://` yalnız `APP_SITE_GUVENLI_CEREZ=false` yerel denemede) varsa canonical/`og:url`
   (ana sayfa sunucuda yer tutucu yorumdan doldurulur), Sitemap satırı ve site haritası; yoksa bunlar yok.
5. **Bot uçları çerezsiz:** `robots.txt`, `sitemap.xml`, `favicon.svg` ziyaretçi/XSRF çerezi almaz, ziyaretçi
   satırı açmaz (`config/TaramaYolu`, sağlık uçlarıyla aynı desen).

## Sonuçlar
- Ziyaretçi verisi (sayfa görüntüleme, cihaz/konum özeti, GA çerezleri `_ga*`) **Google'a gider**; onay bandı
  olmadığından KVKK/GDPR açısından risk kullanıcı tarafından kabul edildi. Geri alma: değişkeni silip Redeploy.
- GA verisi Google'ın ABD altyapısında işlenir (yurt dışına aktarım); kullanıcı kararıyla aydınlatma metni/onay
  penceresi yok.
- **Kabul edilen risk (2026-11-28, kullanıcı kararı; Faz 15 tehdit modelinde bulundu):** ölçüm kimliği verilince Google'ın
  etiketi (gtag) sayfaya yüklenir. Bu betik sayfanın kendi betiğiyle aynı origin'de çalışır; ziyaretçinin
  tarayıcıda tuttuğu OpenRouter anahtarını (`localStorage` "Bu cihazda hatırla" ya da sekme ömürlü `sessionStorage`, ikisi
  de aynı ölçüde okunabilir) okuyabilir. Google'a anahtar **gönderilmez**, ama anahtarın güvenliği yüklenen üçüncü taraf
  betiğe duyulan güvene bağlıdır. Kabul gerekçesi: tek üçüncü taraf betik Google'ın kendi etiketi; anahtar kartı harcama
  limiti öneriyor. Reddedilen seçenekler: sohbet açıkken GA'yı yüklememek (kod değişikliği), GA'yı kapatmak. Yeniden
  açma koşulu: sayfaya başka bir üçüncü taraf betik (etiket yöneticisi, reklam, sohbet aracı) eklenmesi.
  Ek not: `index.html`'deki CSP sabittir; `script-src` içindeki `www.googletagmanager.com` ve GA alanları ölçüm
  kimliği verilmese de listededir. Bir HTML enjeksiyonu olursa bu izin CSP'yi aşmaya yarayabilir; bugün güvenilmeyen
  metin `textContent` ile basıldığı için açık delik değil. Kimliğe bağlı CSP yalnız JS'siz sayfalarda (`AcikSayfa`).
- `og:image` yok: ikili PNG sızıntı taramasının ikili izin listesine girmeden işlenemez (ayrı iş).
- SPA sınırı: site haritasında yalnız ana sayfa; ders sayfalarının dizine girmesi sunucuda üretilen sayfalar ister.
  → Çözüldü: ADR-0020 (herkese açık JS'siz ders sayfaları, llms.txt, genişletilmiş site haritası).
- Googlebot sayfayı JS ile çizerken `/api/**` GET'leri ziyaretçi satırı açmaz (ADR-0017 kuralı), çerez alır.
