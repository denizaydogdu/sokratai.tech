# ADR-0021 — Ön yüz davranış testleri: Playwright for Java (Faz 13)

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-27
- **Bağlam sahibi:** kullanıcı kararı (yeni bağımlılık onayı) + uygulayıcı kararları (aşağıda)
- **İlişkili:** ADR-0017 (girişsiz site, ziyaretçi anahtarı), ADR-0019 (GA4 + CSP), tech-debt #11

## Bağlam
Ön yüz framework'süz tek sayfalık uygulamadır (`static/app.js`, ~2000 satır). Şimdiye kadar onu yalnız
**kaynak-metin bekçileri** (tripwire: `SiteOnYuzIcerikTest`, `SiteAnalitikOnYuzTest`) koruyordu: "app.js şu dizeyi
içeriyor mu?" Bu testler ucuzdur ama DAVRANIŞ görmez (tech-debt #11): düğme tıklanınca görev kaydediliyor mu, sayfa
konsol/CSP hatasız açılıyor mu, hata metni HTML olarak mı basılıyor — hiçbiri yakalanmaz. Faz 13'ün "OpenRouter ile
bağlan" (OAuth PKCE) akışı yönlendirme + `crypto.subtle` + `sessionStorage` + CSP içerdiğinden dize denetimiyle
anlamlı biçimde sınanamaz.

## Karar
**Kullanıcı onayı (2026-11-27):** `com.microsoft.playwright:playwright` (sürüm `pom.xml` `playwright.version`'da
sabit, **yalnız `test` kapsamı**). Bağımlılık varsayılan açık `onyuz-testleri` Maven profilindedir (etkinleşme:
`!onyuz.atla`); yerelde `./mvnw verify` onu her zaman içerir, Docker derlemesi `-Donyuz.atla` ile dışarıda bırakır.

Uygulayıcı kararları:
1. Testler `./mvnw verify` içinde koşar: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `site` profili + bellek
   içi H2 → gerçek Chromium (headless) gerçek uygulamaya gider (`web/OnYuzDavranisTest`).
2. **Gerçek ağ yok:** sunucumuz dışındaki her istek kesilir ve kaydedilir; OpenRouter (`/auth` yönlendirmesi ve
   `/api/v1/auth/keys`) `page.route`/`context.route` ile taklit edilir. Test "dışarı istek gitmedi" diye de doğrular.
3. **Tarayıcı yoksa ATLA, kırma:** tarayıcı kendiliğinden indirilmez (`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` — aksi
   hâlde üç tarayıcı birden iner). Chromium kurulu değilse ya da başlatılamazsa (çevrimdışı CI, desteklenmeyen
   işletim sistemi) `@BeforeAll` yalnız nedeni saklar; her testin `@BeforeEach`'indeki JUnit varsayımı (assumption)
   testi atlatır — surefire raporunda 11+ test tek tek "skipped" ve kurulum komutunu söyleyen mesajla görünür.
   **İstisna:** ortamda `CI` değişkeni varsa (boş, `false`, `0` değilse) tarayıcı yokluğu atlama değil **hata**dır —
   CI'da davranış testleri sessizce kaybolmasın. Tarayıcı yokken atlanan davranışın ucuz yedeği olarak olumlu
   metin iddiaları (`SiteOnYuzIcerikTest.eskiSiteMetinleriYok`) kaynak-metin bekçisinde de kalır.
   Bir kez kurulum:
   ```
   ./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI \
     -Dexec.args="install chromium" -Dexec.classpathScope=test
   ```
   (Linux'ta sistem kütüphaneleri eksikse `install --with-deps chromium`.)
4. Kapsam (en az): konsol/CSP hatasız açılış, sol menünün genişliğe göre varsayılanı ve hatırlanması, "Okudum ✓"
   ile işaretlenen görevin yeniden yüklemede kalması, ilerleme kodu diyaloğu, anahtar kartı kaydet/kaldır, GA
   kimliği yokken analitiğin hiç yüklenmemesi, OpenRouter PKCE akışı (S256 doğrulaması, state, anahtarın sunucumuza
   gitmemesi, hata/uyuşmazlık yolları), sohbet hata metninde XSS dumanı.
5. Kaynak-metin bekçileri KALIR (tarayıcı yokken yedek ağ); davranış testinin
   göremeyeceği kurallar (ör. anahtar başlığının TEK çağrı noktası, analitik bölümünün kişisel veriye dokunmaması,
   CSP dizesinin tam hâli) dize bekçisi olarak kalır.

## Değerlendirilen seçenekler
- **jsdom (+ Node test koşucusu):** gerçek tarayıcı değil — CSP, `<dialog>`, `matchMedia`, gerçek yönlendirme ve
  `crypto.subtle` davranışı taklittir ya da yoktur; üstelik derlemeye Node/npm zinciri ekler (repo Maven-only).
- **HtmlUnit:** saf Java ama modern JS (ES2020+, `crypto.subtle`, `<dialog>`) desteği zayıf; sahte yeşil riski.
- **Selenium + WebDriver:** gerçek tarayıcı ama sürücü/tarayıcı sürüm eşleştirmesi ve ağ taklidi (route) için ek
  araç gerekir; Playwright bunları tek pakette verir.
- **Hiç yapmamak (dize bekçileri):** tech-debt #11'in kendisi.

## Sonuçlar / maliyet
- **Bir kerelik indirme:** Chromium + headless shell ≈ 150–250 MB, `~/.cache/ms-playwright` (macOS:
  `~/Library/Caches/ms-playwright`). Repoya ikili dosya GİRMEZ (sızıntı taraması ikili izin listesi değişmez).
- **Maven yerel deposu:** `driver-bundle` jar'ı (~225 MB, tüm platformların Node sürücüsü) geliştirici makinesinde
  `~/.m2`'ye bir kez iner.
- **Docker derlemesi indirmez:** `-DskipTests` ya da `-Dmaven.test.skip=true` tek başına yetmez — Maven test
  kapsamındaki bağımlılıkları yine çözer (go-offline da). Bu yüzden Dockerfile iki adımda da `-Donyuz.atla` verir
  (profil kapalı → Playwright hiç çözülmez) ve paketlemeyi `-Dmaven.test.skip=true` ile yapar (test kaynakları
  derlenmez; `-Donyuz.atla` bu bayrak OLMADAN kullanılmaz, yoksa `OnYuzDavranisTest` derlenemez). Doğrulandı:
  derleme aşaması imajında `~/.m2/repository/com/microsoft` yok. Çalışma imajı zaten yalnız jar'ı alır; üretim
  etkilenmez.
- Test süresi: Spring bağlamı + tarayıcı açılışı birkaç saniye, test başına ~1–3 sn.
- Atlanan testler surefire raporunda "skipped" görünür; yeşil derleme davranışın sınandığı anlamına gelmez —
  rapor ya da `Tests run/Skipped` satırı kontrol edilir.
