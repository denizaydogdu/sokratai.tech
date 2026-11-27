// JS'siz herkese açık ders sayfalarının sayfa görüntüleme betiği (ADR-0019/0020).
// Sunucu bu dosyayı YALNIZ geçerli bir GA4 kimliği ayarlıysa data-k ile ekler; yoksa etiket hiç yoktur.
// Bizim gönderdiğimiz: sayfa başlığı + yol (sorgu dizesi yok); ziyaretçi kimliği/ilerleme kodu/sohbet/anahtar YOK.
// gtag.js ayrıca kendi standart verisini ekler: `_ga` çerezi ve istemci kimliği, yönlendiren (referrer), tarayıcı/
// cihaz/ekran bilgisi (ADR-0019). Google sinyalleri ve reklam kişiselleştirmesi kapalı. Hata sayfayı asla etkilemez.
(function () {
  try {
    var el = document.currentScript || document.querySelector("script[data-k]");
    var k = el ? el.getAttribute("data-k") : null;
    if (typeof k !== "string" || !/^G-[A-Z0-9]{4,20}$/.test(k)) return;
    var dl = (window.dataLayer = window.dataLayer || []);
    var g = function () { dl.push(arguments); }; // Google'ın betiği arguments nesnesi bekler
    var yer = window.location.origin + window.location.pathname;
    g("js", new Date());
    g("config", k, {
      send_page_view: false,
      page_location: yer,
      allow_google_signals: false,
      allow_ad_personalization_signals: false,
    });
    g("event", "page_view", { page_title: String(document.title).slice(0, 100), page_location: yer });
    var b = document.createElement("script");
    b.async = true;
    b.src = "https://www.googletagmanager.com/gtag/js?id=" + encodeURIComponent(k);
    document.head.appendChild(b);
  } catch (e) { /* sessiz */ }
})();
