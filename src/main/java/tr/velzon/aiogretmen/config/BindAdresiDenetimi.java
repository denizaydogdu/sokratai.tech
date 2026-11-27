package tr.velzon.aiogretmen.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Faz 11 inceleme düzeltmesi (ADR-0018): bind kuralının konteyner istisnası ({@code SERVER_ADDRESS=0.0.0.0}) YALNIZ
 * {@code site} profiliyle geçerlidir. {@code server.address} loopback değilse (127.0.0.1, ::1, localhost dışı — boş
 * da "tüm arayüzler" sayılır) ve {@code site} profili aktif değilse uygulama — hiçbir bean, web sunucusu kurulmadan —
 * AÇILMAZ. Böylece imajdaki {@code SPRING_PROFILES_ACTIVE=site} ezilirse yerel mod (giriş yok, sunucu anahtarı,
 * mülakat/MCP) dış arayüzde açılamaz. {@link SiteVeritabaniDenetimi} ile aynı desen.
 */
@Component
public class BindAdresiDenetimi implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "::1", "[::1]", "0:0:0:0:0:0:0:1", "localhost");

    private Environment ortam;

    @Override
    public void setEnvironment(Environment environment) {
        this.ortam = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String adres = ortam.getProperty("server.address");
        String temiz = adres == null ? "" : adres.trim().toLowerCase(Locale.ROOT);
        if (LOOPBACK.contains(temiz) || ortam.acceptsProfiles(Profiles.of("site"))) {
            return;
        }
        throw new IllegalStateException("server.address='" + (temiz.isEmpty() ? "(boş: tüm arayüzler)" : temiz)
                + "' loopback değil ve site profili aktif değil — uygulama açılmıyor. 127.0.0.1 dışına bind YALNIZ "
                + "Docker konteynerinde site profiliyle yapılır (SPRING_PROFILES_ACTIVE=site, ADR-0018); yerelde "
                + "SERVER_ADDRESS'i kaldır ya da 127.0.0.1 ver.");
    }
}
