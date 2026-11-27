# ADR-0018 — Dokploy (Docker + Traefik) ile konteyner dağıtımı (Faz 11)

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-27
- **Bağlam sahibi:** kullanıcı kararı (konteyner + bind istisnası, aynen) + uygulayıcı kararları (aşağıda
  *uygulayıcı kararı* diye işaretli)
- **Değiştirdiği kararlar:** AGENTS.md "NEVER: 127.0.0.1 dışına bind" kuralına **konteyner istisnası** eklenir.
  ADR-0007 (127.0.0.1 + önde TLS vekili ilkesi), ADR-0014 (PostgreSQL) ve ADR-0017 (girişsiz site) geçerli kalır;
  systemd + nginx yolu (`docs/kurulum-site.md` §1–§9, `deploy/`) başka sunucular için durur.

## Bağlam

Site (ADR-0017) bugüne kadar tek sunucuda **systemd servisi + nginx/TLS** olarak kurgulanmıştı; uygulama
`127.0.0.1:8090`'a bağlanır, dışarıya yalnız nginx açılır. Kullanıcı sunucuyu (bir VPS) **çok projeli**
kullanacak ve bunun için **Dokploy** kuracak: her proje bir Docker konteyneri, önde Dokploy'un yönettiği
**Traefik** (alan adı + Let's Encrypt). nginx ve systemd o sunucuda bu proje için kullanılmayacak.

Konteynerde `127.0.0.1`'e bağlanan bir uygulamaya Traefik (başka bir konteyner) ulaşamaz: konteyner içindeki
loopback yalnız o konteynerindir. Kural olduğu gibi kalırsa Dokploy yolu çalışmaz.

Başarısızlık ölçütleri: uygulamanın host'un dış arayüzünde doğrudan erişilebilir olması (TLS'siz, Traefik
atlanarak); `thoughts/` (sunucu/SSH ayrıntısı) ya da `data/` (kişisel veri) içeriğinin imaja girmesi;
konteyner yeniden kurulunca ziyaretçi hafızasının kaybolması; aynı Docker ağındaki başka bir projenin
konteynerinin `X-Forwarded-For` taklidiyle hız sınırlarını atlatması; yerel modun değişmesi.

## Karar

### Kullanıcının kararı (aynen, 2026-11-27)
Sunucu Dokploy + Traefik ile çok projeli kullanılacak; ai_engineering Docker konteyneri olarak kurulacak.
Bind kuralına **konteyner istisnası**: konteyner içinde `0.0.0.0:8090`, **host portu YAYINLANMAZ**, dış erişim
**yalnız Traefik + TLS**. Yerel varsayılan `127.0.0.1` kalır.

### 1. İmaj (`Dockerfile`, `.dockerignore`)
- Çok aşamalı: `maven:3.9.16-eclipse-temurin-17` ile `mvn -DskipTests package` (testler yerelde `./mvnw verify`
  ile koşar; imaj kurulumu onları tekrarlamaz) → çalışma imajı `eclipse-temurin:17.0.21_9-jre`. Temel imajlar
  **kesin etiketle** sabittir (tekrarlanabilir derleme; inceleme düzeltmesi, 2026-11-27).
- **Root olmayan kullanıcı**, sabit uid/gid `10001` (volume sahipliği imaj yeniden kurulsa da değişmez).
- `WORKDIR /app`: `uygulama.jar` + `content/` + `prompts/` (uygulama bunları göreli `./content`, `./prompts`
  yolundan okur) — root'a ait, salt okunur. Yazılabilir tek dizin `/app/data` (hafıza, raporlar, vektör deposu)
  → `VOLUME ["/app/data"]`; Dokploy'da **kalıcı volume şart**.
- `ENV SERVER_ADDRESS=0.0.0.0` (Spring relaxed binding `server.address`'i ezer; `application.yml`'deki
  `127.0.0.1` yerel varsayılan olarak kalır), `SPRING_PROFILES_ACTIVE=site`,
  `JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"` (konteyner sınırı ~768 MB–1 GB;
  OOM'da süreç düşer, Docker/Dokploy yeniden başlatır — yarı ölü JVM kalmaz).
- `EXPOSE 8090` yalnız belge amaçlı. `HEALTHCHECK`: `curl` temel imajda var (doğrulandı) → `GET /api/saglik/canli`
  (canlılık; §2).
- **Profil koruması** (*uygulayıcı kararı*, inceleme düzeltmesi): `server.address` loopback değilse (127.0.0.1, ::1,
  localhost dışı; boş = tüm arayüzler) ve `site` profili aktif değilse uygulama açılmaz (`config/BindAdresiDenetimi`,
  `IllegalStateException`) — imajdaki `SPRING_PROFILES_ACTIVE=site` ezilse bile yerel mod dış arayüzde açılamaz.
- `.dockerignore`: `.git`, `target/`, `data/`, `thoughts/`, `.superpowers/`, `.claude/`, `deploy/`, `tools/`,
  `.env*`/`*.env`, IDE dosyaları. İmaja yalnız `pom.xml`, `src/`, `content/`, `prompts/` kopyalanır (açık
  `COPY` listesi — `.dockerignore` ikinci kilit).

### 2. Sağlık uçları (actuator YOK) — *uygulayıcı kararı*
**Canlılık/hazırlık ayrımı** (inceleme düzeltmesi): `GET /api/saglik/canli` sabit 200 `{"durum":"canli"}` döner,
veritabanına dokunmaz — Docker `HEALTHCHECK` ve Dokploy sağlık kontrolü bunu yoklar; veritabanı kesintisinde
konteyner yeniden başlatılmaz (yeniden başlatma DB'yi düzeltmez, siteyi tamamen düşürürdü). `GET /api/saglik`
(aşağıda) hazırlık/izleme ucudur. İkisi aynı dışlamaları paylaşır (`config/SaglikYolu`, yalnız `GET`).

`GET /api/saglik`: veritabanına `SELECT 1` (kendi `JdbcTemplate`'i, sorgu zaman aşımı 2 sn): başarı 200 `{"durum":"ayakta"}`,
hata 503 `{"durum":"veritabani-yok"}` — istisna metni gövdeye girmez, logda yalnız sınıf adı. `Cache-Control:
no-store`. Hem yerel modda hem sitede çalışır. Sağlık kontrolü dakikada bir gelir: `ZiyaretciCerezi`, CSRF çerez
filtresi, ziyaretçi dokunma ve hız sınırı interceptor'ları **yalnız `GET /api/saglik`**'i atlar (`config/SaglikYolu`)
→ çerez yazılmaz, `ziyaretci` satırı açılmaz, hız kovası tüketilmez. Katman: `web/SaglikController` →
`service/SaglikService` → `repo/SaglikRepo`.

### 3. Vekil güveni daraltılabilir: `APP_SITE_GUVENILIR_VEKIL`
`server.forward-headers-strategy: native` → Tomcat `RemoteIpValve` varsayılan olarak 10/8, 172.16/12,
192.168/16, 127/8 (ve 169.254/16, 100.64/10, IPv6 yerel) adreslerini güvenilir vekil sayar. Docker'da **aynı
ağdaki komşu konteynerler** de bu aralıktadır → başka bir projenin konteyneri `X-Forwarded-For` taklit edip
IP başına hız sınırını atlatabilir ya da https'miş gibi görünebilir. `application-site.yml`:
`server.tomcat.remoteip.internal-proxies: ${APP_SITE_GUVENILIR_VEKIL:<Tomcat varsayılanı>}` — değişken yoksa
davranış **birebir aynı** (test bunu Boot'un `ServerProperties` varsayılanıyla karşılaştırır). Dokploy'da
Traefik konteynerinin IP'sine/alt ağına daraltılır (`docs/kurulum-site.md` §10). Doğrulama: geçici
`LOGGING_LEVEL_TR_VELZON_AIOGRETMEN_VEKIL=DEBUG` → `HizSiniriInterceptor` çözülen istemci IP'sini maskeli (ilk iki
oktet) yazar; sağlık ucuna IP eklenmez (bilgi sızdırırdı). Yanlış ayarın belirtisi: sitenin tamamında 429.

### 4. Şema ve veritabanı
Flyway/Liquibase **gereksiz**: şema `schema.sql` ile her açılışta idempotent kurulur (`CREATE TABLE IF NOT
EXISTS`, ADR-0014). Rol ve veritabanı Dokploy'un Postgres servisinde oluşturulur; `deploy/postgres-kurulum.sql.ornek`
yalnız systemd yolu içindir.

## Güvenlik notu — çoklu ziyaretçi çerezi (kimlik sabitleme)
Otomatik inceleme bulgusu (Faz 11): istekte aynı adla birden çok `aio_ziyaretci` çerezi gelirse filtre ilkine
güveniyordu. Alan adı altındaki bir **kardeş alt alan adını** kontrol eden biri, ana alan adına (`Domain=`) bilinen
kimlikli bir çerez yerleştirip ziyaretçinin kimliğini sabitleyebilir (ziyaretçinin ilerlemesi saldırganın bildiği
koda yazılır). Düzeltme: aynı adla **farklı değerli** birden çok çerez gelirse **hiçbirine güvenilmez** → yeni kimlik
üretilip yazılır (aynı değerin tekrarı tek sayılır). Test: `SiteZiyaretciCokluCerezTest`.

**Kalan risk:** ilk gelişte (ziyaretçinin henüz kendi çerezi yokken) yerleştirilmiş **tek** bir çerez hâlâ kabul
edilir. Tam çözüm `__Host-` önekli çerez adıdır (`Secure` + `Path=/` + `Domain` yok — tarayıcı alt alan adından
yazılmasına izin vermez); ancak `Secure` zorunlu olduğundan yerel http denemesini (`APP_SITE_GUVENLI_CEREZ=false`)
kırar ve mevcut ziyaretçilerin çerezini geçersiz kılar — şimdilik yapılmadı. Çoklu çerez düzeltmesinin yan etkisi: kardeş alt alan adından
yerleştirilen ikinci bir çerez artık sabitleme yerine her istekte **kimlik sıfırlamasına** (ziyaretçi ilerlemesine
erişemez, kilitlenme) yol açar — sabitlemeden iyidir, ama yine bir saldırı yüzeyidir ve aynı kuralla kapanır. İşletme kuralı: sitenin alan adı
altında **güvenilmeyen bir alt alan adı açılmaz** (Dokploy'da aynı ana alan adına başka projeler bağlanacaksa
bu risk yeniden değerlendirilir).

## Sonuçlar
- Kalıcı volume `/app/data` şart; yedekleme = Postgres dökümü + bu volume.
- Uygulama hâlâ **tek örnek** çalışmalı (hız sınırları JVM belleğinde, gece temizliği kilitsiz — ADR-0017);
  Dokploy'da replika sayısı 1.
- Host'a port yayını yapılmaz; konteynere yalnız Docker ağındaki Traefik ulaşır.
- Sağlık ucu veritabanı tamamen erişilemezken Hikari'nin bağlantı bekleme süresi kadar gecikebilir.
  **Değişiklik notu (2026-11-27):** site profilinde bu süre Boot varsayılanı 30 sn'den 5 sn'ye indirildi
  (`spring.datasource.hikari.connection-timeout`, `APP_SITE_DB_BAGLANTI_ZAMAN_ASIMI_MS`); uç en geç ~7 sn'de
  (5 + 2 sn sorgu) 503 döner, izleme aracının 10 sn zaman aşımı içinde kalır (Docker HEALTHCHECK artık
  DB'siz canlılık ucunu yoklar). Yan etki: dolu havuzda bir istek de
  en çok 5 sn bekleyip hata alır (60 kişi / 10 bağlantıda beklenmez).
- Yerel mod ve systemd/nginx yolu değişmez.

## Reddedilen seçenekler
- **Konteynerde de 127.0.0.1 + yan konteyner (sidecar) vekil:** Traefik zaten TLS vekili; konteyner içine ikinci
  bir vekil koymak karmaşıklık ekler, güvenlik kazancı yok (host portu zaten yayınlanmıyor).
- **`network_mode: host` ile 127.0.0.1:** konteyner yalıtımını kaldırır, Dokploy/Traefik ağ modeliyle uyumsuz.
- **Spring Boot Actuator health:** yeni bağımlılık; tek bir `SELECT 1` için gereksiz yüzey (`/actuator/**`).
- **Flyway:** şema zaten idempotent `schema.sql`; yeni bağımlılık ve göç tablosu getirirdi.
- **Host portu yayınlayıp (`-p 8090:8090`) nginx'i host'ta tutmak:** Dokploy'un Traefik'iyle çakışır; port
  yanlışlıkla dışarı açılabilir.
