# ExecPlan: Faz 11 — Dokploy dağıtımı, sokratai.tech ve yeni herkese açık repo

> Tamamlandı (kod ve belge: 2026-11-27). Canlıya alma adımları (DNS, Dokploy uygulaması, TLS, ilk dağıtım)
> henüz yapılmadı — aşağıda "Sıradakiler". Karar kaydı: [ADR-0018](../../adr/0018-dokploy-konteyner-dagitimi.md).

## Neden önemli (kullanıcı bakışı)
Girişsiz site (Faz 10) hazırdı ama bir sunucusu yoktu. Kullanıcı Hostinger'dan `sokratai.tech` alan adını ve bir
VPS aldı; VPS'te Dokploy (Docker Swarm + Traefik) hazır geliyor ve aynı sunucuda 6-7 başka proje de çalışacak.
Ayrıca proje Pera'dan (pera-soft) tamamen ayrıldı: kod artık kullanıcının kendi herkese açık deposunda durur.
Halka açık site adı **SokratAI (sokratai.tech)**; proje ve kod adları (AI Öğretmen, paketler) değişmedi.

## Kararlar
1. Dağıtım: Docker imajı (kökte `Dockerfile`) → Dokploy uygulaması; konteyner portu 8090; veritabanı Dokploy
   PostgreSQL servisi; kalıcı volume `/app/data`; sağlık kontrolü `GET /api/saglik`.
2. Bind kuralına **tek istisna** (kullanıcı onayladı): konteyner içinde `SERVER_ADDRESS=0.0.0.0`, host portu
   YAYINLANMAZ, dış erişim yalnız Traefik + TLS. Yerel varsayılan `127.0.0.1` kalır.
3. Repo: Pera bağı tamamen kalktı. Yeni herkese açık depo `github.com/denizaydogdu/sokratai.tech` (`main`),
   **tek commit'lik temiz geçmişle** başladı. Eski 259 commit'lik geçmiş (gerçek mülakat dökümleri + eski sunucu
   adresleri) yalnız yerelde `eski-gecmis` dalında kalır, ASLA push edilmez.
4. Yayın aracı `tools/yayin-hazirla.sh` kalktı; yerine `tools/sizinti-tara.sh` + her push edilen commit'i tarayan
   `pre-push` kancası geldi.
5. Belgelere ve `deploy/` şablonlarına gerçek alan adı/IP/şifre yazılmaz (yer tutucular kalır).

## Yapılanlar (commit'ler)
- `c170000` `/api/saglik` sağlık ucu, `APP_SITE_GUVENILIR_VEKIL`, çoklu ziyaretçi çerezinde yeni kimlik
  (kimlik sabitleme açığı kapandı).
- `54a640e` Dockerfile + `.dockerignore`, ADR-0018, `docs/kurulum-site.md` §10.
- `076e2c5` Pera bağı kaldırıldı; yeni repo, sızıntı taramalı pre-push.
- `af60542` Sağlık ucu hızlı hata (DB bağlantı zaman aşımı); `SiteZiyaretciKimlikTest` sıra bağımsız.
- `5ccd142` pre-push: push edilen her commit taranır.
- Belgeler: sokratai.tech ve Dokploy durumu README, AGENTS.md, kurulum kılavuzu, SECURITY, tech-debt'e işlendi.

## Doğrulama
- `./mvnw verify` YEŞİL: 440 test, 5 atlanan (PostgreSQL opt-in), 0 hata.
- `bash tools/sizinti-tara.sh HEAD` çıkış kodu 0 (belge commit'inden sonra çalıştırıldı).

## Bilinen açıklar (tech-debt'e yazıldı)
- #19: `aio_ziyaretci` çerezi `__Host-` önekli değil — karar bekliyor (alt alan adı açılırsa yeniden değerlendir).
- #20: site testlerinde `csrf()` yardımcısı dağınık.

## Sıradakiler
1. DNS: Hostinger'da `sokratai.tech` için `@` ve `www` A kayıtları → VPS (ekleniyor).
2. Dokploy'da uygulamayı kur (Dockerfile, port 8090, env değişkenleri `docs/kurulum-site.md` §10.3, PostgreSQL
   servisi, kalıcı `/app/data` volume'u, sağlık kontrolü `/api/saglik`). Sunucu tarafını ayrı "hostinger" oturumu yönetir.
3. TLS: Traefik + Let's Encrypt ile `sokratai.tech` sertifikası; `APP_SITE_IZINLI_ORIGINLER` alan adına ayarlanır.
4. İlk gerçek dağıtım + duman testi: kök adres 200, `/api/saglik` 200, çerezler `Secure`, bir görevi işaretle,
   ilerleme kodunu başka tarayıcıda geri yükle; `SitePostgresEntegrasyonTest` gerçek PostgreSQL'e karşı bir kez.
5. Karar bekleyenler: `__Host-` çerez öneki (#19), JS davranış testi (#11).
