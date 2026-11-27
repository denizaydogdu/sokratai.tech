# AGENTS.md — ai_engineering (AI Öğretmen)
> İçindekiler tablosudur, ansiklopedi değil. Detay: docs/ ve ARCHITECTURE.md.
> Pera (pera-soft) ile ilişik kalmadı (2026-11-27); repo denizaydogdu/sokratai.tech; deploy: Hostinger VPS üzerinde Dokploy.

## Komutlar
- Derle+test: `./mvnw verify`
- Çalıştır: `OPENROUTER_API_KEY=... ./mvnw spring-boot:run`  (http://127.0.0.1:8090; sağlayıcı: OpenRouter, ADR-0004)
  - Mülakat modülü varsayılan KAPALI; açmak için başına `APP_MULAKAT_ETKIN=true` ekle (ADR-0011)
  - Girişsiz çok ziyaretçili site (ADR-0017): `--spring.profiles.active=site` + `SPRING_DATASOURCE_URL`
    (yerelde: `SPRING_DATASOURCE_URL=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=site`). Giriş/kayıt/
    e-posta YOK; ziyaretçi `aio_ziyaretci` çereziyle tanınır. Sitede `OPENROUTER_API_KEY` GEREKMEZ (verilse de
    kullanılmaz) — sohbeti ziyaretçi kendi anahtarıyla yapar; sohbet dışı AI uçları sitede kapalı.
    Yerel http denemesi: `APP_SITE_GUVENLI_CEREZ=false`
  - Site veritabanı (ADR-0014): `SPRING_DATASOURCE_URL` (+ `_USERNAME`, `_PASSWORD`) ŞART — canlıda
    PostgreSQL; yoksa açılmaz. Yerel deneme: `SPRING_DATASOURCE_URL='jdbc:h2:file:./data/site-deneme'`
  - Site hız sınırları (isteğe bağlı, tek NAT arkasındaki sınıf/ofis için): `APP_SITE_HIZ_*` (yazma/dk 60, sohbet/dk 20, geri-yükle/saat 10, yeni ziyaretçi/saat 20; `docs/kurulum-site.md` §7.6)
  - Site dağıtımı/işletmesi: `docs/kurulum-site.md` (runbook) + `deploy/` (systemd, nginx, env, PostgreSQL
    şablonları — gerçek host/IP/sır YOK); nginx arkasında `APP_SITE_IZINLI_ORIGINLER`
  - Docker imajı (Dokploy + Traefik, ADR-0018): `docker build -t ai-ogretmen .` — kurulum/ortam/volume/sağlık
    (canlılık `/api/saglik/canli`, hazırlık `/api/saglik`): `docs/kurulum-site.md` §10
- PostgreSQL uyumluluk testi (opt-in, atılabilir DB ile): `PG_TEST_URL=jdbc:postgresql://localhost:5432/<db>
  PG_TEST_USERNAME=... ./mvnw -Dtest=SitePostgresEntegrasyonTest test` (değişken yoksa atlanır)
- Sadece mimari testleri: `./mvnw -Dtest=MimariTestleri test`
- Müfredat üret (HTML → content/): `node tools/html-to-content.mjs`

## Sınırlar
- ALWAYS: KULLANICI İNGİLİZCE BİLMİYOR — verdiği İngilizce kaynaklar Türkçe çalışma
  metnine çevrilir (content/kaynaklar/), tüm eğitim içeriği ve AI çıktıları Türkçe olur;
  teknik terimlerin İngilizcesi parantezle verilir.
- ALWAYS: types → config → repo → service → ai → web katman yönüne uy (ArchUnit zorlar).
- ALWAYS: Claude çağrısı yalnız ai/ paketinden; prompt'lar prompts/ dizininden.
- ALWAYS: yeni davranışa test; API değişikliğinde bu dosyadaki komutları güncelle.
- ALWAYS: ANLAMLI iş bitince Obsidian Vault'a yaz (eşik: "3 ay sonraki okuyucu bunu bilmek
  ister mi?"). İLK vault işleminden önce docs/obsidian-vault-rehberi.md'yi OKU — sıra,
  terfi sorusu ve link disiplini orada, bağlayıcıdır.
- ALWAYS: yeni/büyüyen kaynak metni (content/kaynaklar/) konvansiyona uyar: **3 inline SVG
  diyagram** (```svg çit; CSP-temiz — `<style>`/`class` YOK, yalnız presentation attribute;
  dosyaya özgü marker id öneki) + öz-sınav + satıcı iddiasına şerh. Render doğrula:
  `/api/kaynak/{slug}` → 3 svg. İstisna: rapor/soru bankası türü metinler diyagramsız kalabilir.
- ASK FIRST: yeni bağımlılık (önce docs/adr/'ye ADR).
- NEVER: API anahtarını koda/git'e koymak; **Obsidian Vault'a şifre/token/secret yazmak**
  (vault kuralı); 127.0.0.1 dışına bind — uygulama daima 127.0.0.1'e bağlanır; **istisna: Docker
  konteynerinde `SERVER_ADDRESS=0.0.0.0`, host portu yayınlanmaz, dış erişim yalnız Traefik/TLS (ADR-0018)**;
  public erişim yalnız nginx/TLS ya da Traefik/TLS önünden (ADR-0007 Faz 2b); content/ altını kod değişikliğiyle aynı PR'da
  yeniden üretmek.

## Nerede ne var
- Mimari: ARCHITECTURE.md · Kararlar: docs/adr/ · Planlar: docs/exec-plans/
- Müfredat: content/ (frontmatter'lı md) · Prompt şablonları: prompts/
- Öğrenci hafızası (Faz 2, AI yazar): data/hafiza/ (git dışı)
- Borç listesi: docs/tech-debt-tracker.md
- Oturum defterleri: thoughts/ledgers/ (git DIŞI — sunucu/SSH ayrıntısı içerir; /clear öncesi güncellenir)
- İkinci beyin (git dışı): `~/Documents/Obsidian Vault/AI ÖĞRETMEN/` — kurallar:
  docs/obsidian-vault-rehberi.md (İLK vault işleminden önce OKU)
- Canlı site: **sokratai.tech** ("SokratAI"; proje adı AI Öğretmen kalır) — Hostinger VPS üzerinde Dokploy
  (Docker + Traefik, konteyner portu 8090, Dokploy PostgreSQL, kalıcı `/app/data`; ADR-0018,
  `docs/kurulum-site.md` §10). Sunucu adresi/SSH bilgisi repoda YOK.
- MCP sunucusu: kod duruyor (`mcp-http` profili, ADR-0007); eski canlı sunucular (ai.velzon.tr,
  mulakat.velzon.tr) 2026-10-21'de KAPATILDI — o MCP dağıtımı yok, MCP yalnız yerelde.

## Repo ve yayın
- Repo: `denizaydogdu/sokratai.tech` (PUBLIC, dal `main`). Pera (pera-soft) ile ilişik yok (2026-11-27).
  Deploy: Hostinger VPS üzerinde Dokploy (ADR-0018, `docs/kurulum-site.md` §10).
- Geçmiş: `main` geçmişsiz (orphan) tek commit'le başladı ve `origin`'e (yukarıdaki PUBLIC repo) push edildi.
  Eski geçmiş YALNIZ yerel `eski-gecmis` dalında; hiçbir uzak depoda yok.
- NEVER: `eski-gecmis` dalını ya da ondan türeyen bir commit'i push etmek — geçmişinde mülakat dökümü/sunucu
  ayrıntısı var; yalnız bu bilgisayarda kalır. Kanca eski kök commit'i SABİT SHA ile de tanır (dal silinse bile).
- Yeni bir uzak depo (ayna/yedek) eklenecekse YENİ bir remote adıyla ekle (`git remote add <yeni-ad> ...`);
  var olan bir remote'un URL'sini başka depoya ÇEVİRME — eski izleme ref'leri (`refs/remotes/<ad>/*`) yerinde
  kalır, kanca o commit'leri "zaten uzakta" sayıp taramadan geçer.
- Push öncesi sızıntı taraması kancada otomatik (`tools/git-kancalari/pre-push` → `tools/sizinti-tara.sh`;
  eski geçmişten türeyen commit'i de engeller; ağaç + commit mesajı/yazarı/commit edeni + etiket metni; commit
  dışı nesneye işaret eden etiket, izin listesi (`tools/sizinti-ikili-izinli.txt`) dışındaki ikili dosya ve
  içeriği ikili saydıran `.gitattributes` satırı ENGEL; tarama yapılamazsa da engeller).
  Ref adları da (yerel + uzak dal/etiket adı) aynı desenlerle taranır. İkili izin listesi yayınlanmış sürümden
  (uzak ref / `<uzak>/main`) okunur; listeye yeni girdi ekleyen push yalnız `SIZINTI_IKILI_ONAY=1 git push ...` ile geçer.
  Elle: `bash tools/sizinti-tara.sh [<commit>]`; kancanın senaryo testi `bash tools/sizinti-tara-test.sh`
  (`./mvnw verify` içinde `ToolsSizintiTaramaTest` koşar).
- Kilit: `git config core.hooksPath tools/git-kancalari`

## PR / commit kuralları
- Küçük, tek amaçlı commit'ler; mesaj: `[faz-N] özet`
- Merge öncesi `./mvnw verify` (birim + ArchUnit) YEŞİL olmalı.
- OVERRIDE (tek kişilik repo, kullanıcı kararı 2026-11-27): genel standardın "Dockerfile/dağıtım/güvenlik/
  migration değişikliğinde ikinci insan gözden geçirici adı" kuralı yerine bu değişikliklerde `qa-code-reviewer`
  + Codex incelemesi ZORUNLU ve bulguları push'tan önce kapatılır. Gerekçe: repoda tek geliştirici var, ikinci
  insan gözden geçirici yok.
- OVERRIDE (tek kişilik repo, kullanıcı kararı 2026-11-27): genel standart tek kişilik repoda proje `CLAUDE.md`'yi
  git dışı tutar; bu `AGENTS.md` (ve ona bağlı `CLAUDE.md`) ise BİLEREK commit'lenir ve herkese açıktır. Gerekçe:
  repo bir öğretim/örnek reposu, ajan talimatları da öğretilen içeriğin parçası.
