package tr.velzon.aiogretmen.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code /api/**} yanıtlarına {@code X-Robots-Tag: noindex} (Faz 12, ADR-0019; yalnız {@code site} profili).
 * robots.txt {@code /api/}'yi kapatmaz — Googlebot tek sayfalık uygulamayı çizerken bu GET'leri çekebilmeli; JSON
 * yanıtlarının arama sonuçlarına girmemesini bu başlık sağlar. Başlık işleyiciden ÖNCE yazılır (yanıt erken
 * commit edilse de bulunur). Ziyaretçi/hız interceptor'larından bağımsız: çerez, satır, kova yok.
 */
@Component
@Profile("site")
public class ApiNoindexInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    static final String BASLIK = "X-Robots-Tag";

    @Override
    public void addInterceptors(InterceptorRegistry kayit) {
        kayit.addInterceptor(this).addPathPatterns("/api/**").order(-2);
    }

    @Override
    public boolean preHandle(HttpServletRequest istek, HttpServletResponse yanit, Object isleyici) {
        yanit.setHeader(BASLIK, "noindex");
        return true;
    }
}
