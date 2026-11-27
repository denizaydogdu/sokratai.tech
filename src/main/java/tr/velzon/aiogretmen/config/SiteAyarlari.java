package tr.velzon.aiogretmen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.regex.Pattern;

/**
 * {@code site} profili ayarları (Faz 10, ADR-0017) — {@code app.site.*}. Girişsiz site: kimlik/e-posta ayarı yok.
 *
 * @param izinliOriginler nginx arkasındaki genel adres(ler) (ör. {@code https://egitim.ornek.com});
 *                        {@code web/CsrfKoruma} yerel origin'e EK olarak bunları kabul eder. Boş = yalnız yerel.
 * @param guvenliCerez    ziyaretçi + CSRF çerezlerinde {@code Secure}; yalnız yerel http denemesinde {@code false}.
 * @param hiz             hız sınırları ({@code app.site.hiz.*}; ortamda {@code APP_SITE_HIZ_*}) — pencereler sabit.
 * @param gaOlcumKimligi  Google Analytics 4 ölçüm kimliği ({@code APP_SITE_GA_OLCUM_KIMLIGI}, Faz 12, ADR-0019);
 *                        biçim {@code ^G-[A-Z0-9]{4,20}$}, boş/geçersiz = analitik TAMAMEN kapalı (geçersizde WARN).
 *                        Yalnız {@code site} profilinde ön yüze verilir ({@code web/OzellikController}).
 * @param temelUrl        sitenin genel kök adresi ({@code APP_SITE_TEMEL_URL}, ör. {@code https://egitim.ornek.com});
 *                        canonical, {@code og:url}, {@code robots.txt} Sitemap satırı ve {@code sitemap.xml} için.
 *                        Sondaki {@code /} atılır; boş/geçersiz = bu etiketler/uçlar yok (alan adı koda gömülmez).
 */
@ConfigurationProperties(prefix = "app.site")
public record SiteAyarlari(@DefaultValue List<String> izinliOriginler,
                           @DefaultValue("true") boolean guvenliCerez,
                           @DefaultValue Hiz hiz,
                           String gaOlcumKimligi,
                           String temelUrl) {

    private static final Logger log = LoggerFactory.getLogger(SiteAyarlari.class);

    /** GA4 web akışı ölçüm kimliği (eski UA-/AW- kimlikleri ve keyfi metin dışarıda kalır). */
    private static final Pattern GA_KIMLIGI = Pattern.compile("^G-[A-Z0-9]{4,20}$");

    /** Yalnız şema + ASCII ana makine (+ isteğe bağlı port, sondaki {@code /}); yol/sorgu/tırnak yok → HTML/XML-güvenli. */
    private static final Pattern TEMEL_URL = Pattern.compile("^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?/?$");

    public SiteAyarlari {
        gaOlcumKimligi = gaKimligiNormallestir(gaOlcumKimligi);
        temelUrl = temelUrlNormallestir(temelUrl);
    }

    /** @return kırpılmış geçerli kimlik; boş ya da biçim dışıysa {@code ""} (biçim dışında WARN — değer loglanmaz). */
    public static String gaKimligiNormallestir(String ham) {
        String deger = ham == null ? "" : ham.strip();
        if (deger.isEmpty()) {
            return "";
        }
        if (!GA_KIMLIGI.matcher(deger).matches()) {
            log.warn("APP_SITE_GA_OLCUM_KIMLIGI geçersiz (beklenen biçim G-XXXXXXXX) — analitik KAPALI");
            return "";
        }
        return deger;
    }

    /** @return sondaki {@code /} atılmış kök adres; boş ya da biçim dışıysa {@code ""} (biçim dışında WARN). */
    public static String temelUrlNormallestir(String ham) {
        String deger = ham == null ? "" : ham.strip();
        if (deger.isEmpty()) {
            return "";
        }
        if (!TEMEL_URL.matcher(deger).matches()) {
            log.warn("APP_SITE_TEMEL_URL geçersiz (beklenen biçim https://alan-adi, yol yok) — canonical/sitemap KAPALI");
            return "";
        }
        return deger.endsWith("/") ? deger.substring(0, deger.length() - 1) : deger;
    }

    /**
     * Ayarlanabilir hız sınırları (son inceleme #4) — tek NAT arkasındaki sınıf/ofis için yükseltilebilir.
     * Varsayılanların TEK kaynağı buradaki {@code @DefaultValue}'lardır: {@code application-site.yml}
     * placeholder'ları ({@code ${APP_SITE_HIZ_…:}}) ortam değişkeni yoksa boş verir, boş değer buraya düşer
     * ({@code Integer}: boş → null → varsayılan; {@code int} boşu bağlayamazdı).
     *
     * @param yazmaDakika        yazan istek ({@code POST/PUT/DELETE /api/**}) / dakika / IP
     * @param sohbetDakika       sohbet mesajı / dakika / ziyaretçi
     * @param geriYukleSaat      ilerleme kodu geri yükleme / saat / IP
     * @param yeniZiyaretciSaat  yeni ziyaretçi satırı / saat / IP
     */
    public record Hiz(@DefaultValue("60") Integer yazmaDakika,
                      @DefaultValue("20") Integer sohbetDakika,
                      @DefaultValue("10") Integer geriYukleSaat,
                      @DefaultValue("20") Integer yeniZiyaretciSaat) {

        public Hiz {
            for (Integer deger : new Integer[]{yazmaDakika, sohbetDakika, geriYukleSaat, yeniZiyaretciSaat}) {
                if (deger == null || deger < 1) {
                    throw new IllegalArgumentException("app.site.hiz.* sınırları en az 1 olmalı");
                }
            }
        }
    }
}
