# AI Öğretmen — girişsiz site konteyner imajı (Faz 11, ADR-0018: Dokploy + Traefik).
#   docker build -t ai-ogretmen .
# Çalıştırma/ortam değişkenleri: docs/kurulum-site.md "Dokploy ile kurulum (Docker + Traefik)".
# İmaja GİRMEZ: data/, thoughts/, .git, sırlar (.dockerignore). Testler imajda koşmaz — yerelde `./mvnw verify`.

# Temel imajlar KESİN etiketle sabit (tekrarlanabilir derleme; 2026-11-27'de var olduğu doğrulandı). Güncellerken
# ikisini birlikte yükselt ve `docker build` + HEALTHCHECK'i yeniden doğrula.
# --- 1) Derleme ---
FROM maven:3.9.16-eclipse-temurin-17 AS derleme
WORKDIR /derleme
# Bağımlılık katmanı önce: pom.xml değişmedikçe çoğu yeniden indirilmez (go-offline her eklentiyi çekmez →
# paketlemede -o KULLANILMAZ, eksikler o adımda iner).
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && cp target/ai-ogretmen-*.jar /derleme/uygulama.jar

# --- 2) Çalışma imajı ---
FROM eclipse-temurin:17.0.21_9-jre
# Sabit uid/gid: volume (/app/data) sahipliği imaj yeniden kurulsa da değişmez.
RUN groupadd --system --gid 10001 aiogretmen \
    && useradd --system --uid 10001 --gid 10001 --home-dir /app --shell /usr/sbin/nologin aiogretmen
WORKDIR /app
# Uygulama içeriği ve prompt'ları ./content, ./prompts göreli yolundan okur (application.yml app.content-dir,
# app.prompt-dir) — salt okunur, root'a ait.
COPY --from=derleme /derleme/uygulama.jar /app/uygulama.jar
COPY content /app/content
COPY prompts /app/prompts
# Kişisel veri (öğrenci hafızası, raporlar, vektör deposu: app.hafiza-dir, app.rapor-dir, app.vektor-dosyasi)
# ./data altında → KALICI VOLUME şart (Dokploy: Volume /app/data); yazılabilir tek dizin.
RUN mkdir -p /app/data && chown 10001:10001 /app/data
VOLUME ["/app/data"]

# Konteyner istisnası (ADR-0018): konteyner içinde 0.0.0.0 — host portu YAYINLANMAZ, dış erişim yalnız
# Traefik/TLS. application.yml'deki 127.0.0.1 yerel varsayılan olarak kalır (relaxed binding ezer).
ENV SERVER_ADDRESS=0.0.0.0 \
    SPRING_PROFILES_ACTIVE=site \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Yalnız belge amaçlı; Dokploy'da host'a port yayını YAPILMAZ (Traefik iç ağdan 8090'a gider).
EXPOSE 8090

USER 10001:10001
# curl temel imajda var (eclipse-temurin, Ubuntu). CANLILIK ucu: veritabanına dokunmaz → DB kesintisinde konteyner
# gereksiz yeniden başlatılmaz (yeniden başlatma DB'yi düzeltmez). Hazırlık/izleme (DB dahil): GET /api/saglik.
HEALTHCHECK --interval=60s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -fsS -o /dev/null http://127.0.0.1:8090/api/saglik/canli || exit 1

ENTRYPOINT ["java", "-jar", "/app/uygulama.jar"]
