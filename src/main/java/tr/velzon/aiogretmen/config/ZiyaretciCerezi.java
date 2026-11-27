package tr.velzon.aiogretmen.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Girişsiz {@code site} profilinde ziyaretçi kimliği (Faz 10, ADR-0017): {@value #AD} çerezi.
 *
 * <p>Her istekte çerezi okur; yoksa, biçimi geçersizse ya da aynı adla farklı değerli birden çok çerez geldiyse ({@code ^[A-Za-z0-9_-]{22}$} dışı) yeni rastgele
 * kimlik üretir. Kimliği (öneksiz) {@value #ISTEK_OZNITELIGI} istek özniteliğine koyar →
 * {@link ZiyaretciKullaniciBaglami} okur. Çerez her yanıtta yeniden yazılır (kaydırmalı 1 yıl ömür).
 *
 * <p><strong>Biçim doğrulaması şart:</strong> çerezle gelen keyfi metin {@code kullanici_id}'ye ya da dosya yoluna
 * ({@code data/hafiza/<id>}) giremez — yalnız URL-güvenli Base64 alfabesi, sabit uzunluk.
 *
 * <p>Çerez: {@code HttpOnly} (JS okuyamaz), {@code SameSite=Lax}, {@code Path=/}; {@code Secure} kuralı CSRF
 * çereziyle aynı: {@code guvenliCerez=true} → her zaman, değilse isteğe göre (nginx {@code X-Forwarded-Proto=https}).
 *
 * <p>{@code GET /api/saglik} ve {@code /api/saglik/canli} ({@link SaglikYolu}, Faz 11 / ADR-0018) atlanır: dakikada bir gelen sağlık kontrolü
 * kimlik üretmez, çerez yazmaz. Bot yolları ({@code robots.txt}, {@code sitemap.xml}, {@code favicon.svg};
 * {@link TaramaYolu}, Faz 12 / ADR-0019) da atlanır: tarayıcı botu çerez almaz.
 */
public final class ZiyaretciCerezi extends OncePerRequestFilter {

    /** Çerez adı. */
    public static final String AD = "aio_ziyaretci";

    /** İstekteki ziyaretçi kimliğinin (öneksiz) öznitelik adı. */
    public static final String ISTEK_OZNITELIGI = "aio.ziyaretci";

    /** Çerez ömrü (her ziyarette yenilenir). */
    static final Duration OMUR = Duration.ofDays(365);

    /** 16 bayt → URL-güvenli Base64, dolgusuz = 22 karakter. */
    private static final int BAYT = 16;
    private static final Pattern BICIM = Pattern.compile("^[A-Za-z0-9_-]{22}$");
    private static final SecureRandom RASTGELE = new SecureRandom();

    private final boolean guvenliCerez;

    public ZiyaretciCerezi(boolean guvenliCerez) {
        this.guvenliCerez = guvenliCerez;
    }

    /** @return kimlik beklenen biçimde mi (22 karakter, yalnız {@code A-Z a-z 0-9 _ -})? */
    public static boolean gecerliMi(String kimlik) {
        return kimlik != null && BICIM.matcher(kimlik).matches();
    }

    /** @return 128 bit rastgele, tahmin edilemez yeni kimlik. */
    public static String yeniKimlik() {
        byte[] bayt = new byte[BAYT];
        RASTGELE.nextBytes(bayt);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bayt);
    }

    /** Çerezi bu kimlikle (yeniden) yazar (filtre: her yanıtta, kaydırmalı ömür). */
    public static void yaz(HttpServletRequest istek, HttpServletResponse yanit, String kimlik, boolean guvenliCerez) {
        yanit.addHeader(HttpHeaders.SET_COOKIE, cerezMetni(istek, kimlik, guvenliCerez));
    }

    /**
     * Çerezi BAŞKA bir kimlikle değiştirir (ilerleme kodu geri yükleme). Filtre bu yanıta eski kimliği zaten yazmıştı;
     * iki {@code Set-Cookie: aio_ziyaretci} göndermemek için eski başlık çıkarılır, diğer çerezler (ör.
     * {@code XSRF-TOKEN}) korunur. Yanıt gövdesi yazılmadan (commit öncesi) çağrılmalı.
     */
    public static void degistir(HttpServletRequest istek, HttpServletResponse yanit, String kimlik,
                                boolean guvenliCerez) {
        String yeni = cerezMetni(istek, kimlik, guvenliCerez);
        List<String> korunan = yanit.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> !c.startsWith(AD + "="))
                .toList();
        yanit.setHeader(HttpHeaders.SET_COOKIE, yeni); // aynı adlı TÜM başlıkları tek değerle değiştirir
        korunan.forEach(c -> yanit.addHeader(HttpHeaders.SET_COOKIE, c));
    }

    private static String cerezMetni(HttpServletRequest istek, String kimlik, boolean guvenliCerez) {
        if (!gecerliMi(kimlik)) {
            throw new IllegalArgumentException("ziyaretçi kimliği geçersiz biçimde");
        }
        return ResponseCookie.from(AD, kimlik)
                .httpOnly(true)
                .secure(guvenliCerez || istek.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(OMUR)
                .build()
                .toString();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest istek) {
        return SaglikYolu.mi(istek) || TaramaYolu.mi(istek);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest istek, HttpServletResponse yanit, FilterChain zincir)
            throws ServletException, IOException {
        String kimlik = cereztenOku(istek);
        if (!gecerliMi(kimlik)) {
            kimlik = yeniKimlik();
        }
        istek.setAttribute(ISTEK_OZNITELIGI, kimlik);
        yaz(istek, yanit, kimlik, guvenliCerez);
        zincir.doFilter(istek, yanit);
    }

    /**
     * @return istekteki {@value #AD} değeri; yoksa ya da aynı adla FARKLI değerli birden çok çerez geldiyse
     *         {@code null} (→ yeni kimlik). Birden çok farklı değer, kardeş alt alan adından ana alan adına
     *         yerleştirilmiş bir çerez olabilir (kimlik sabitleme): hiçbirine güvenilmez. Aynı değerin tekrarı tek
     *         sayılır. Kalan risk ve {@code __Host-} öneki: ADR-0018 §Güvenlik notu.
     */
    private static String cereztenOku(HttpServletRequest istek) {
        Cookie[] cerezler = istek.getCookies();
        if (cerezler == null) {
            return null;
        }
        String bulunan = null;
        for (Cookie c : cerezler) {
            if (AD.equals(c.getName())) {
                if (bulunan != null && !bulunan.equals(c.getValue())) {
                    return null;
                }
                bulunan = c.getValue();
            }
        }
        return bulunan;
    }
}
