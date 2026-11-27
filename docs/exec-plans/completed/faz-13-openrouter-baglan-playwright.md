# ExecPlan: Faz 13 — "OpenRouter ile bağlan", sol menü ve Playwright ön yüz testleri

> Tamamlandı (2026-11-27). Karar kaydı: [ADR-0021](../../adr/0021-playwright-on-yuz-davranis-testleri.md)

## Neden önemli (kullanıcı bakışı)
Ziyaretçinin sohbeti kullanması için OpenRouter anahtarını elle kopyalayıp yapıştırması zordu. Tek tıkla bağlanma
istendi; anahtar yine yalnız ziyaretçinin tarayıcısında kalmalıydı. Ayrıca ön yüz davranışları yalnız kaynak metin
denetimiyle test ediliyordu (tech-debt #11); gerçek tarayıcıyla test gerekti.

## Kullanıcı kararları
1. Playwright for Java bağımlılığı onaylandı (ADR-0021).
2. Sol menü masaüstünde (genişlik >= 900px) açık gelsin.
3. Sol üst başlık ("AI ÖĞRETMEN · 12 HAFTALIK PROGRAM / Mimar Defteri") şimdilik kalsın; SokratAI'ye çevirmek ileride.
4. Sol menü oku şimdilik bu haliyle kalsın.

## Yapılanlar
- "OpenRouter ile bağlan": OAuth PKCE, tamamen tarayıcıda; `state` kontrolü; CSP `connect-src`'e `openrouter.ai`;
  başka sekme ve geri tuşu kenar durumları ele alındı; elle yapıştırma çalışmaya devam eder.
- Sol menü masaüstünde açık; ders bağlantılarında `#parça` korunur.
- Arayüz: "Toplam İlerleme" tek satır; anahtar kartında elle yapıştırma kutusu üstte; yeşil, belirgin "Anahtarın
  güvende" kutusu; sol menü oku ilk satırla hizalı.
- Playwright ile 14 gerçek tarayıcı testi (`web/OnYuzDavranisTest`): konsol/CSP hatası, sol menü, "Okudum"
  kalıcılığı, ilerleme kodu, anahtar kartı, GA yokluğu, OpenRouter PKCE (taklitli), XSS dumanı. Profil
  `onyuz-testleri`; Docker derlemesi `-Donyuz.atla -Dmaven.test.skip=true` ile Playwright indirmez; CI'da tarayıcı
  yoksa test hata verir (sessizce atlanmaz).
- Tech-debt #11 kapandı; davranışın kapsadığı dize iddiaları `SiteOnYuzIcerikTest`'ten kaldırıldı.

## Doğrulama
- `./mvnw clean verify`: 527 test, 0 hata, 5 atlanan (PostgreSQL isteğe bağlı); 14 Playwright testi koşuyor.
- `SitePostgresEntegrasyonTest` PostgreSQL 14.15'e karşı 5/5.

## İncelemelerin yakaladıkları
- Docker derlemesinde Playwright sürücüsünün (~225 MB) indirilmesi; profil ile dışarıda bırakıldı.
- OAuth kenar durumları: yanlış/eksik `state`, ikinci sekmede dönüş, geri tuşu; testlerle kapatıldı.
- Sızıntı tarayıcısı boşlukları (noktalı IP, yol adları, sembolik bağlar, ref adları, ikili onayın uç sha'ya
  bağlanması, noktalama ile başlayan şifreler) kapandı; 84+ senaryo testi `verify` içinde koşar.

## Sıradakiler
- Kullanıcı: gerçek OpenRouter hesabıyla "OpenRouter ile bağlan"ı bir kez uçtan uca denesin (testler taklitle).
- İsteğe bağlı: sol üst başlığı SokratAI yapmak.
- Bilinen küçük sınır: uzaktaki sızıntılı bir etiketi silmek kancada `--no-verify` gerektirir (şu an etiket yok).
