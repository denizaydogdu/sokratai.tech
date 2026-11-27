/* SokratAI — Google Analytics 4 (Faz 12, ADR-0019). Framework yok, satır içi betik yok (CSP).
 *
 * Yalnız sunucu geçerli bir ölçüm kimliği verirse (site profili + APP_SITE_GA_OLCUM_KIMLIGI) çalışır; kimlik
 * yoksa gtag.js YÜKLENMEZ, hiçbir istek Google'a GİTMEZ, aşağıdaki işlevler sessizce hiçbir şey yapmaz.
 *
 * Gizlilik sınırı (bilerek dar): Google'a yalnız sanal sayfa yolu/başlığı ve adı sabit olaylar gider. Ziyaretçi
 * kimliği, kullanıcı kimliği, sohbet metni, OpenRouter bilgisi ya da çerez değeri GÖNDERİLMEZ — bu dosya onlara
 * erişmez. Google sinyalleri ve reklam kişiselleştirmesi kapalı. Sayfa görüntülemelerini app.js bildirir
 * (send_page_view: false), adres çubuğundaki sorgu dizesi Google'a taşınmaz (page_location kökten kurulur).
 */
"use strict";

const GA_KIMLIK_BICIMI = /^G-[A-Z0-9]{4,20}$/; // sunucudaki doğrulamanın aynısı (ikinci kilit)
let analitikEtkin = false;

/** Ölçüm kimliği geçerliyse gtag.js'i yükler ve yapılandırır; her sayfa açılışında en çok bir kez. */
function analitikBaslat(kimlik) {
  if (analitikEtkin || typeof kimlik !== "string" || !GA_KIMLIK_BICIMI.test(kimlik)) return;
  analitikEtkin = true;
  window.dataLayer = window.dataLayer || [];
  // gtag.js arguments nesnesi bekler (dizi değil)
  window.gtag = function gtag() { window.dataLayer.push(arguments); };
  window.gtag("js", new Date());
  window.gtag("config", kimlik, {
    send_page_view: false,
    page_location: window.location.origin + "/",
    allow_google_signals: false,
    allow_ad_personalization_signals: false,
  });
  const betik = document.createElement("script");
  betik.async = true;
  betik.src = "https://www.googletagmanager.com/gtag/js?id=" + encodeURIComponent(kimlik);
  document.head.appendChild(betik);
}

/** Sanal sayfa görüntülemesi (tek sayfalık uygulama: hafta/gün/kaynak görünümleri). yol örn. "/hafta/3/gun/2". */
function analitikSayfa(baslik, yol) {
  if (!analitikEtkin) return;
  window.gtag("event", "page_view", {
    page_title: String(baslik).slice(0, 100),
    page_location: window.location.origin + yol,
  });
}

/** Adı sabit olay; parametreler yalnız sayı (hafta/gün numarası gibi) — metin taşımaz. */
function analitikOlay(ad, sayilar) {
  if (!analitikEtkin) return;
  const temiz = {};
  for (const [k, v] of Object.entries(sayilar || {})) {
    if (Number.isInteger(v)) temiz[k] = v;
  }
  window.gtag("event", ad, temiz);
}
