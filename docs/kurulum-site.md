# Kurulum ve işletme kılavuzu — girişsiz eğitim sitesi

> Faz 10 (ADR-0017 girişsiz site + kendi anahtarın, ADR-0014 PostgreSQL). Bu belge sunucuyu kuracak ve
> işletecek kişi içindir. Gerçek sunucu adresi, şifre, anahtar BU BELGEYE YAZILMAZ — yer tutucular `<...>`
> biçimindedir. `egitim.ornek.com` örnek alan adıdır; kendi alan adınla değiştir.
>
> **Güncel canlı ortam (2026-11-27):** herkese açık site **sokratai.tech** ("SokratAI") **YAYINDA** — bir Hostinger
> VPS üzerinde **Dokploy** (Docker + Traefik, Let's Encrypt TLS) ile çalışır (§10, ADR-0018). İlk kurulumda duman testi
> geçti (sayfa, `/api/saglik`, görev işaretleme, sahte anahtarla sohbet → 401); Swarm `dnsrr` ayarı §10.5'te. Sunucu adresi ve erişim bilgisi bu
> repoda tutulmaz. Aşağıdaki adımlar yer tutucularla (`<alan-adi>`, `egitim.ornek.com`) yazılmıştır; kendi
> sunucunda gerçek değerleri koy.
>
> **İki kurulum yolu:** §1–§9 klasik sunucu (systemd + nginx) — başka sunucular için durur. Dokploy (Docker +
> Traefik) kullanan çok projeli sunucu için **§10** (ADR-0018); yedekleme, temizlik, KVKK silme gibi işletme
> bilgileri (§6–§8) orada da geçerlidir, yalnız komutlar konteynere göre uyarlanır.

## 1. Genel resim

```
tarayıcı ──https──▶ nginx (443, TLS, certbot) ──http──▶ uygulama 127.0.0.1:8090 (systemd, Java 17)
   │                                                        │
   │ (ziyaretçinin kendi OpenRouter anahtarı,               ├──▶ PostgreSQL 127.0.0.1:5432 (ai_ogretmen)
   │  yalnız sohbet isteğinde başlıkta)                     └──▶ OpenRouter (YALNIZ sohbet; ziyaretçinin anahtarıyla)
```

- **Giriş/kayıt yok** (ADR-0017). Her ziyaretçi tarayıcısındaki rastgele `aio_ziyaretci` çereziyle tanınır;
  ilerlemesi sunucuda o kimlikle saklanır. Başka tarayıcıda devam etmek için "İlerleme kodum" → "Geri yükle".
  Hesap, e-posta, şifre, ekip kodu, SMTP **yoktur**.
- **Sunucuda OpenRouter anahtarı yoktur.** AI yalnız isteğe bağlı sohbet botudur; kullanmak isteyen ziyaretçi
  **kendi** anahtarını sohbet paneline yapıştırır (tarayıcıda durur, her sohbet isteğiyle gönderilir, sunucu
  o istekte kullanıp unutur — hiçbir yere yazılmaz). Rapor, AI değerlendirme, AI quiz, kitap notu sitede yok.
- Uygulama **yalnız 127.0.0.1**'e bağlanır; dışarıya yalnız nginx açılır (AGENTS.md NEVER kuralı; tek istisna
  Docker konteyneri — §10, ADR-0018).
- Bu yolda Docker yok: uygulama **tek örnekli** bir **systemd servisi**dir (`deploy/ai-ogretmen-site.service`). Tek
  örnek şarttır: hız sınırları JVM belleğinde tutulur, gece temizliği kilitsiz çalışır (ADR-0017 §7–§8).
- Sunucudaki dizin düzeni (uygulama dosyaları bu dizine GÖRELİ okunur):

```
/opt/ai-ogretmen/
  ai-ogretmen.jar          # uygulama
  content/                 # müfredat (repodan kopya) — açılışta belleğe yüklenir
  prompts/                 # AI prompt şablonları (repodan kopya)
  data/                    # YAZILABİLİR: ziyaretçinin AI hafızası (yedeklenir)
    hafiza/kullanicilar/<sha256("z-<ilerleme-kodu>")>/...
/etc/ai-ogretmen/site.env  # ortam değişkenleri (deploy/site.env.ornek şablonundan)
```

## 2. Önkoşullar

Sunucu (Ubuntu 22.04/24.04 veya Debian 12 varsayıldı; komutlar `apt` ile):

| Gereken | Komut / not |
|---|---|
| Java 17 çalışma ortamı (JRE) | `sudo apt install openjdk-17-jre-headless` → `java -version` 17 göstermeli |
| PostgreSQL 14+ | `sudo apt install postgresql` (yalnız `localhost` dinlesin — varsayılan) |
| nginx | `sudo apt install nginx` |
| certbot | `sudo apt install certbot` |
| DNS | `egitim.ornek.com` A/AAAA kaydı sunucuyu göstermeli |
| Güvenlik duvarı | Dışarıya yalnız 80 + 443 (+ yönetim için SSH). 8090 ve 5432 KAPALI kalır. |

Elinde olması gereken tek sır: veritabanı şifresi (§3.2'de sen belirlersin). OpenRouter anahtarı ve SMTP
hesabı **gerekmez**.

**Dağıtımdan önce bir kez:** `SitePostgresEntegrasyonTest`'i atılabilir bir PostgreSQL veritabanına karşı
koş (§9). Faz 10 dalında yerel PostgreSQL 14.15'e karşı 5/5 geçti (ADR-0017); canlı sunucunun sürümü
farklıysa orada da bir kez koş.

## 3. İlk kurulum (adım adım)

Komutlar sunucuda **sudo yetkili kendi yönetici hesabınla** çalıştırılır (doğrudan root oturumu
açılmaz). `<yonetici>@<sunucu>` senin SSH hesabındır.

### 3.1 Sistem kullanıcısı ve dizinler
```bash
sudo useradd --system --home-dir /opt/ai-ogretmen --shell /usr/sbin/nologin ai-ogretmen
sudo install -d -m 0755 -o root -g root /opt/ai-ogretmen
sudo install -d -m 0750 -o ai-ogretmen -g ai-ogretmen /opt/ai-ogretmen/data
sudo install -d -m 0750 -o root -g ai-ogretmen /etc/ai-ogretmen
```
Uygulama kullanıcısı jar/content/prompts'u yalnız OKUR; yazabildiği tek yer `data/` (systemd
`ProtectSystem=strict` + `ReadWritePaths` bunu ayrıca zorlar).

### 3.2 PostgreSQL rolü ve veritabanı
Şablonu sunucuya kopyala, şifreyi değiştir, postgres yönetici hesabıyla çalıştır:
```bash
cp deploy/postgres-kurulum.sql.ornek /tmp/pg-kurulum.sql
nano /tmp/pg-kurulum.sql                       # <veritabani-sifresi> → güçlü bir şifre
sudo -u postgres psql -v ON_ERROR_STOP=1 -f /tmp/pg-kurulum.sql
shred -u /tmp/pg-kurulum.sql                   # şifre diskte kalmasın
```
Rol süper kullanıcı değildir; yalnız `ai_ogretmen` veritabanının sahibidir. Tablolar **uygulama ilk
açıldığında** `schema.sql` ile kurulur (her açılışta tekrar çalışır, idempotent — göç aracı yok).

### 3.3 Uygulamayı derle ve kopyala (geliştirici makinesinde)
```bash
./mvnw -DskipTests package                     # → target/ai-ogretmen-0.1.0-SNAPSHOT.jar
scp target/ai-ogretmen-0.1.0-SNAPSHOT.jar <yonetici>@<sunucu>:/tmp/ai-ogretmen.jar
rsync -a --delete content prompts <yonetici>@<sunucu>:/tmp/ai-ogretmen-yukle/
```
Sunucuda yerine koy (sahibi root, uygulama yalnız okur):
```bash
sudo install -m 0644 -o root -g root /tmp/ai-ogretmen.jar /opt/ai-ogretmen/ai-ogretmen.jar
sudo rsync -a --delete --chown=root:root /tmp/ai-ogretmen-yukle/content/ /opt/ai-ogretmen/content/
sudo rsync -a --delete --chown=root:root /tmp/ai-ogretmen-yukle/prompts/ /opt/ai-ogretmen/prompts/
sudo chmod -R a+rX /opt/ai-ogretmen/content /opt/ai-ogretmen/prompts
```
> İsteğe bağlı: bu belgeyi de `/opt/ai-ogretmen/docs/` altına koyarsan systemd `Documentation=` bağlantısı çalışır.

### 3.4 Ortam değişkenleri
```bash
sudo install -m 0640 -o root -g ai-ogretmen deploy/site.env.ornek /etc/ai-ogretmen/site.env
sudo nano /etc/ai-ogretmen/site.env
```
Doldurulacaklar: `SPRING_DATASOURCE_PASSWORD` ve alan adın (`APP_SITE_IZINLI_ORIGINLER`).
`OPENROUTER_API_KEY` **yazma** — sitede kullanılmaz (yazılsa bile site yolu onu okumaz; şablonda yok).
Zorunlu olanlar eksikse uygulama **açılmaz** ve nedenini loga yazar (bkz. §8).

### 3.5 systemd servisi
```bash
sudo cp deploy/ai-ogretmen-site.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now ai-ogretmen-site
sudo systemctl status ai-ogretmen-site
```
Sunucu küçükse/büyükse `ExecStart` içindeki `-Xmx768m`'i ayarla (512m–1g yeter). **Tek örnek** çalıştır.

### 3.6 nginx + TLS sertifikası
```bash
sudo install -d /var/www/certbot
# 1) Önce yalnız 80 portlu bloğu etkinleştirip sertifika al:
sudo cp deploy/nginx-site.conf.ornek /etc/nginx/sites-available/ai-ogretmen-site.conf
#    (alan adını kendi adınla değiştir; sertifika henüz yoksa 443 bloğunu geçici olarak yorum satırı yap)
sudo ln -s /etc/nginx/sites-available/ai-ogretmen-site.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot certonly --webroot -w /var/www/certbot -d egitim.ornek.com
# 2) 443 bloğunu aç, tekrar:
sudo nginx -t && sudo systemctl reload nginx
```
Yenileme: certbot'un zamanlayıcısı (`systemctl list-timers | grep certbot`) otomatik yeniler; yenileme
sonrası nginx'in yeniden yüklenmesi için:
`echo 'systemctl reload nginx' | sudo tee /etc/letsencrypt/renewal-hooks/deploy/nginx-reload.sh && sudo chmod +x $_`

nginx'in `X-Forwarded-For` başlığı şarttır: hız sınırları istemci IP'sine göre sayılır ve uygulama gerçek IP'yi
yalnız 127.0.0.1'deki vekilden gelen bu başlıktan öğrenir. Başlık yoksa bütün ziyaretçiler tek IP sayılır.

## 4. İlk açılış ve sağlık kontrolleri

```bash
sudo journalctl -u ai-ogretmen-site -f            # "Started AiOgretmenApplication" görünmeli
curl -sI http://127.0.0.1:8090/ | head -1         # HTTP/1.1 200
curl -s http://127.0.0.1:8090/api/ozellikler      # {"mulakat":false,"site":true,"sunucuAi":false} (alan sırası değişebilir)
curl -sI https://egitim.ornek.com/ | grep -i -E '^HTTP|strict-transport|set-cookie'
#   → 200; HSTS bir kez; aio_ziyaretci ve XSRF-TOKEN çerezleri "Secure", aio_ziyaretci "HttpOnly"
sudo -u postgres psql -d ai_ogretmen -c '\dt'     # gorev_ilerleme, gonderim, quiz_sonuc, sohbet_mesaj, ziyaretci, ...
```
Logda şu satır beklenir: `site profili — AI ziyaretçinin kendi OpenRouter anahtarıyla (istek başına); sunucu
anahtarı kullanılmaz.` Sonra tarayıcıdan siteyi aç → bir görevi "Okudum ✓" ile işaretle → "İlerleme kodum"dan
kodu kopyala → başka bir tarayıcıda (ya da gizli pencerede) "Geri yükle" ile aynı ilerlemeyi gör. Sohbeti
denemek için kendi OpenRouter anahtarını sohbet paneline yapıştır (harcama limitli bir deneme anahtarı).

## 5. Yeni sürüme güncelleme

1. Geliştirici makinesinde: `./mvnw verify` YEŞİL → `./mvnw -DskipTests package`.
2. Önce yedek al (§6).
3. Jar + content/prompts'u §3.3'teki gibi `/tmp`'ye kopyala; sunucuda:
```bash
sudo cp /opt/ai-ogretmen/ai-ogretmen.jar /opt/ai-ogretmen/ai-ogretmen.jar.onceki   # geri dönüş için
sudo install -m 0644 -o root -g root /tmp/ai-ogretmen.jar /opt/ai-ogretmen/ai-ogretmen.jar
sudo rsync -a --delete --chown=root:root /tmp/ai-ogretmen-yukle/content/ /opt/ai-ogretmen/content/
sudo rsync -a --delete --chown=root:root /tmp/ai-ogretmen-yukle/prompts/ /opt/ai-ogretmen/prompts/
sudo systemctl restart ai-ogretmen-site && sudo journalctl -u ai-ogretmen-site -n 50
```
- Şema değişikliği varsa açılışta `schema.sql` kendiliğinden uygular (yalnız ekleyen, idempotent ifadeler).
- **content/ yalnız açılışta okunur** → müfredat güncellemesinden sonra da `restart` gerekir.
- Yeniden başlatma ziyaretçileri düşürmez (kimlik tarayıcıdaki çerezde, oturum yok); yalnız bellekteki hız
  sınırı sayaçları sıfırlanır. O anda süren bir sohbet isteği yarıda kalabilir.
- Geri dönüş: `ai-ogretmen.jar.onceki`'yi geri kopyala + restart (şema yalnız eklediği için eski sürüm çalışır).

### 5.1 Eski (girişli) siteden geçiş
Faz 9 veritabanında `kullanici` ve `kullanici_token` tabloları **kalır** — yeni `schema.sql` onları
oluşturmaz ama silmez de (ADR-0017). Eski e-posta kimlikli ilerleme satırları sahipsiz kalır ve 180 gün
temizliğine girmez (`ziyaretci` tablosunda yoklar). Silmek ayrı, bilinçli bir karardır; karar verilirse önce
yedek al (§6), sonra:
```bash
sudo -u postgres psql -d ai_ogretmen -c "SELECT COUNT(*) FROM kullanici;"   # önce ne silineceğini gör
```
Eski `site.env`'yi yeni şablonla (`deploy/site.env.ornek`) karşılaştır; şablonda olmayan satırları (OpenRouter
anahtarı, kayıt yöntemi, ekip kodu, SMTP ve gönderici, temel URL) sil — artık okunmuyorlar. Eski şirket
anahtarını OpenRouter panelinden de iptal et.

## 6. Yedekleme ve geri yükleme

Yedeklenecek iki şey var: **PostgreSQL** (ziyaretçiler, ilerleme, quiz, sohbet, kullanım) ve **`data/`**
(ziyaretçinin AI hafızası). content/prompts repodan yeniden kurulur, yedek gerekmez.

```bash
sudo install -d -m 0700 -o postgres -g postgres /var/backups/ai-ogretmen
# Veritabanı (sıkıştırılmış özel biçim):
sudo -u postgres pg_dump -Fc -d ai_ogretmen -f /var/backups/ai-ogretmen/db-$(date +%F).dump
# Kişisel dosyalar:
sudo tar -C /opt/ai-ogretmen -czf /var/backups/ai-ogretmen/data-$(date +%F).tar.gz data
```
Günlük otomatik yedek (ör. `/etc/cron.d/ai-ogretmen-yedek`; 14 gün saklar). 03:30'daki ziyaretçi
temizliğiyle çakışmasın diye 04:30'da:
```
30 4 * * * root /usr/bin/sudo -u postgres pg_dump -Fc -d ai_ogretmen -f /var/backups/ai-ogretmen/db-$(date +\%F).dump && tar -C /opt/ai-ogretmen -czf /var/backups/ai-ogretmen/data-$(date +\%F).tar.gz data && find /var/backups/ai-ogretmen -mtime +14 -delete
```
Yedekleri sunucu DIŞINA da kopyala. Yedekler ziyaretçi verisi içerir (sohbet metinleri) — buna göre sakla.

**Geri yükleme:**
```bash
sudo systemctl stop ai-ogretmen-site
sudo -u postgres pg_restore --clean --if-exists --no-owner --role=ai_ogretmen -d ai_ogretmen \
  /var/backups/ai-ogretmen/db-<TARIH>.dump
sudo rm -rf /opt/ai-ogretmen/data && sudo tar -C /opt/ai-ogretmen -xzf /var/backups/ai-ogretmen/data-<TARIH>.tar.gz
sudo chown -R ai-ogretmen:ai-ogretmen /opt/ai-ogretmen/data
sudo systemctl start ai-ogretmen-site
```

## 7. Rutin işletme işleri

### 7.1 Loglar

| Ne | Nerede |
|---|---|
| Uygulama | `sudo journalctl -u ai-ogretmen-site` (`-f` canlı, `--since "1 hour ago"`) |
| nginx | `/var/log/nginx/ai-ogretmen-site.access.log`, `.error.log` |
| PostgreSQL | `/var/log/postgresql/` |

Loglarda ziyaretçinin OpenRouter anahtarı ve ilerleme kodu YAZILMAZ; sağlayıcı hatasında yalnız HTTP kodu
ve kategori yazılır (`AI sağlayıcı hatası: islem=SOHBET kod=401 neden=ANAHTAR_GECERSIZ`).

### 7.2 180 gün temizliği
Her gece 03:30'da (sunucu saati) 180 gündür gelmeyen ziyaretçilerin bütün satırları ve hafıza dizini silinir.
Kontrol:
```bash
sudo journalctl -u ai-ogretmen-site --since today | grep 'Ziyaretçi temizliği'
#   → "Ziyaretçi temizliği tamam: N ziyaretçi silindi, M geri döndüğü için atlandı, K başarısız"
```
`… başarısız (sonraki turda yeniden denenecek)` görürsen o ziyaretçinin silinmesi geri alınmıştır; sonraki
gece yeniden denenir. `kişisel dizin silinemedi` nedeninde hafıza/rapor dizini (izin, disk) silinemediği için
veritabanı satırları da geri alınmıştır — izinleri düzelt, sonraki gece hepsi birlikte silinir (ADR-0017).
`Ziyaretçi geri döndü, atlandı` (INFO) hata değildir: ziyaretçi listelemeyle silme arasında geldi, verisi
korundu; özetteki "atlandı" sayısına girer, "başarısız"a girmez.

### 7.3 Bir ziyaretçinin verisini silme (KVKK talebi)
Ziyaretçi kendi tarayıcısında "🔄 Sıfırla" düğmesiyle ilerlemesini silebilir. Tamamen silinmesini
isterse **ilerleme kodunu** göndermesi gerekir (başka tanımlayıcı yok):
```bash
K='z-<ilerleme-kodu>'
sudo -u postgres psql -d ai_ogretmen -v k="$K" <<'SQL'
BEGIN;
DELETE FROM gorev_ilerleme WHERE kullanici_id = :'k';
DELETE FROM gonderim       WHERE kullanici_id = :'k';
DELETE FROM quiz_sonuc     WHERE kullanici_id = :'k';
DELETE FROM sohbet_mesaj   WHERE kullanici_id = :'k';
DELETE FROM mulakat_cevap  WHERE kullanici_id = :'k';
DELETE FROM ai_kullanim    WHERE kullanici_id = :'k';
DELETE FROM ziyaretci      WHERE id = :'k';
COMMIT;
SQL
D=$(printf '%s' "$K" | sha256sum | cut -d' ' -f1)
sudo rm -rf /opt/ai-ogretmen/data/hafiza/kullanicilar/$D /opt/ai-ogretmen/data/raporlar/kullanicilar/$D
```
Yedeklerdeki kopyalar saklama süresi (14 gün) dolunca kendiliğinden silinir.

### 7.4 Veritabanı şifresini değiştirme
`sudo -u postgres psql -c "ALTER ROLE ai_ogretmen PASSWORD '<yeni>';"` → `/etc/ai-ogretmen/site.env`'de
`SPRING_DATASOURCE_PASSWORD`'ü değiştir → `sudo systemctl restart ai-ogretmen-site`.

### 7.5 Kullanım
Sitede kota yok; AI maliyeti ziyaretçinin kendi anahtarına yazılır. Ziyaretçi başı token/süre (anahtarsız):
`SELECT kullanici_id, islem, COUNT(*), SUM(input_tokens), SUM(output_tokens) FROM ai_kullanim GROUP BY 1, 2 ORDER BY 1;`

### 7.6 Hız sınırları (bilgi)
Aşımda 429 Türkçe mesaj döner: yazan istekler IP başına 60/dk, sohbet ziyaretçi başına 20/dk, ilerleme kodu
geri yükleme IP başına 10/saat, yeni ziyaretçi IP başına 20/saat (varsayılanlar). Sayaçlar bellekte; restart
sıfırlar. Pencereler sabit, sayılar `/etc/ai-ogretmen/site.env`'den ayarlanır (sonra `systemctl restart`):

| Değişken | Varsayılan | Anlamı |
|---|---|---|
| `APP_SITE_HIZ_YAZMA_DAKIKA` | 60 | yazan istek / dakika / IP |
| `APP_SITE_HIZ_SOHBET_DAKIKA` | 20 | sohbet mesajı / dakika / ziyaretçi |
| `APP_SITE_HIZ_GERI_YUKLE_SAAT` | 10 | ilerleme kodu geri yükleme / saat / IP |
| `APP_SITE_HIZ_YENI_ZIYARETCI_SAAT` | 20 | yeni ziyaretçi / saat / IP |

**Tek NAT arkasında sınıf/ofis:** herkes aynı genel IP'den gelir, IP başına sınırlar bütün sınıfa paylaşılır
(ör. 30 kişilik sınıf ilk derste 20 yeni ziyaretçiden sonra 429 alır). Böyle bir kullanım bekleniyorsa
`APP_SITE_HIZ_YENI_ZIYARETCI_SAAT` (ör. 60) ve `APP_SITE_HIZ_YAZMA_DAKIKA` (ör. 300) yükseltilir; sohbet sınırı
ziyaretçi başına olduğundan etkilenmez. Değerler en az 1 olmalı (yoksa uygulama açılmaz).

## 8. Sorun giderme

| Belirti | Neden / çözüm |
|---|---|
| Servis açılmıyor, logda `site profili veritabanı adresini ister: SPRING_DATASOURCE_URL` | `site.env`'de `SPRING_DATASOURCE_URL` yok/boş. Site H2'ye sessizce düşmez (ADR-0014). |
| Logda `password authentication failed for user "ai_ogretmen"` | `SPRING_DATASOURCE_PASSWORD` yanlış ya da `pg_hba.conf` 127.0.0.1 için `scram-sha-256` izin vermiyor. |
| Görev işaretleme, quiz, sohbet 403 veriyor | `APP_SITE_IZINLI_ORIGINLER` tarayıcının gördüğü adresle **birebir** olmalı: `https://egitim.ornek.com` (sonda `/` yok, şema dahil). Değiştirdiysen `restart`. |
| Her sayfa yenilemede ilerleme "kayboluyor" | Site **http** üzerinden açılıyor: `Secure` çerezleri tarayıcı http'de geri göndermez → her istekte yeni ziyaretçi. https adresini kullan; nginx `X-Forwarded-Proto` gönderiyor mu kontrol et. Yalnız yerel denemede `APP_SITE_GUVENLI_CEREZ=false`. |
| Çok sayıda ziyaretçi aynı anda 429 alıyor | nginx `X-Forwarded-For` göndermiyor ya da uygulamaya 127.0.0.1 dışından bağlanıyor → herkes tek IP sayılıyor (§3.6). |
| Yönlendirmeler `http://127.0.0.1:8090/...` adresine gidiyor | nginx `X-Forwarded-*` başlıklarını göndermiyor (şablondaki `proxy_set_header` satırları) ya da nginx uygulamaya 127.0.0.1 dışından bağlanıyor (uygulama yalnız 127.0.0.1 vekile güvenir). |
| "Geri yükle" → "Bu kodla kayıtlı ilerleme bulunamadı" | Kod yanlış kopyalandı ya da kodun sahibi henüz hiçbir şey işaretlememiş (satır ilk yazmada açılır) ya da 180 gündür gelmediği için silinmiş. |
| Sohbet "OpenRouter anahtarın geçersiz" / "kredin bitti" diyor | Ziyaretçinin kendi anahtarıyla ilgili (401 / 402); sunucu tarafında yapılacak bir şey yok. |
| nginx 502 Bad Gateway | Uygulama kapalı/açılıyor: `systemctl status ai-ogretmen-site`. |
| nginx 504 / AI cevabı yarıda kesiliyor | Sohbet çağrısı 150 sn'yi aştı (OpenRouter yavaş) ya da `proxy_read_timeout` < 180s. |

## 9. Geliştirici notu: PostgreSQL uyumluluk testi

`schema.sql` hem H2 (yerel mod) hem PostgreSQL'de çalışmak zorunda. Normal `./mvnw verify` PostgreSQL'e
bağlanmaz; yerel bir PostgreSQL varsa **atılabilir** bir veritabanıyla elle koş (**dağıtımdan önce şart**):
```bash
createdb ai_ogretmen_test_x
PG_TEST_URL=jdbc:postgresql://localhost:5432/ai_ogretmen_test_x PG_TEST_USERNAME=<kullanici> \
  PG_TEST_PASSWORD=<sifre> ./mvnw -Dtest=SitePostgresEntegrasyonTest test
dropdb ai_ogretmen_test_x
```
Test şemayı iki kez uygular (idempotent), ziyaretçi kaydını, iki ziyaretçinin izolasyonunu, ilerleme kodu geri
yüklemesini ve 180 gün temizliğini (tek okuma-yazma işleminde) gerçek PostgreSQL'de doğrular. Temizlik testi
veritabanındaki **bütün** eski ziyaretçileri siler — yalnız atılabilir veritabanıyla koş.

**Yerel deneme:** `site` profilini veritabanı kurmadan denemek için H2 adresini açıkça ver (OpenRouter
anahtarı gerekmez; sohbeti denemek için kendi anahtarını tarayıcıda gir):
```bash
SPRING_DATASOURCE_URL='jdbc:h2:file:./data/site-deneme' APP_SITE_GUVENLI_CEREZ=false \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=site
```

## 10. Dokploy ile kurulum (Docker + Traefik)

> ADR-0018. Çok projeli sunucuda Dokploy her projeyi bir Docker konteyneri olarak çalıştırır; önde Dokploy'un
> yönettiği **Traefik** alan adını ve Let's Encrypt sertifikasını üstlenir. nginx/systemd (§3.5–§3.6) bu yolda
> KULLANILMAZ. Gerçek alan adı/IP/şifre bu belgeye yazılmaz: `<alan-adi>` yer tutucusunu kendi alan adınla değiştir.

```
tarayıcı ──https──▶ Traefik (Dokploy, TLS, Let's Encrypt) ──http (Docker ağı)──▶ ai-ogretmen konteyneri 0.0.0.0:8090
                                                                                  ├──▶ Dokploy Postgres servisi (iç ad, 5432)
                                                                                  └──▶ OpenRouter (YALNIZ sohbet; ziyaretçinin anahtarıyla)
```

### 10.1 Uygulama türü ve derleme
- Dokploy'da **Application** oluştur; derleme türü **Dockerfile** (repo kökündeki `Dockerfile`). Kaynak: Git
  deposu (dal seçilir) ya da önceden derlenmiş bir imaj (`docker build -t ai-ogretmen .` ile kendi kayıt
  defterine itilmiş).
- İmaj: çok aşamalı (Maven derleme → `eclipse-temurin:17-jre`), root olmayan kullanıcı `uid 10001`, `WORKDIR /app`
  (`uygulama.jar`, `content/`, `prompts/`). Testler imajda koşmaz — dağıtımdan önce yerelde `./mvnw verify`.
  `thoughts/`, `data/`, `.git`, `.env*` imaja girmez (`.dockerignore`).
- İmaj kendisi verir: `SPRING_PROFILES_ACTIVE=site`, `SERVER_ADDRESS=0.0.0.0` (yalnız konteyner içinde — ADR-0018),
  `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError`.
- **Replika sayısı 1** (hız sınırları JVM belleğinde, gece temizliği kilitsiz — §1, ADR-0017).

### 10.2 Veritabanı
Dokploy'da bir **PostgreSQL** servisi oluştur (veritabanı `ai_ogretmen`, kullanıcı `ai_ogretmen`, güçlü şifre);
rol ve veritabanını Dokploy oluşturur — `deploy/postgres-kurulum.sql.ornek` yalnız systemd yolu içindir. Postgres
servisinin **dış portunu yayınlama**. Tablolar uygulamanın ilk açılışında `schema.sql` ile **kendiliğinden** kurulur
(idempotent, her açılışta; göç aracı yok).

### 10.3 Ortam değişkenleri (Dokploy → Environment)
`deploy/site.env.ornek` aynı değişkenleri belgeler; Dokploy'da tırnaksız `ANAHTAR=değer` olarak gir.

| Değişken | Değer |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<postgres-servisinin-ic-adi>:5432/ai_ogretmen` — Dokploy Postgres servisinin **iç (Docker ağı) adı**; zorunlu |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Postgres servisindeki kullanıcı/şifre |
| `APP_SITE_IZINLI_ORIGINLER` | `https://<alan-adi>,https://www.<alan-adi>` (şema dahil, sonda `/` yok; kullandığın her adres) |
| `APP_SITE_GUVENLI_CEREZ` | `true` |
| `APP_SITE_GUVENILIR_VEKIL` | Traefik konteynerinin IP'si/alt ağı (Java regex) — §10.5 |
| `APP_SITE_HIZ_*`, `APP_SITE_DB_HAVUZ` | İsteğe bağlı (§7.6) |
| `APP_SITE_DB_BAGLANTI_ZAMAN_ASIMI_MS` | İsteğe bağlı; varsayılan `5000` (Hikari bağlantı bekleme, ms) — §10.4 sağlık kontrolü |
| `APP_SITE_TEMEL_URL` | İsteğe bağlı: `https://<alan-adi>` (şema dahil, yol yok) — canonical, `og:url`, `robots.txt` Sitemap satırı, `sitemap.xml`; boşsa bunlar yok (§10.7) |
| `APP_SITE_GA_OLCUM_KIMLIGI` | İsteğe bağlı: GA4 ölçüm kimliği `G-XXXXXXXXXX`; boş = analitik tamamen kapalı, biçim dışı = boş + WARN (§10.7, ADR-0019) |

`OPENROUTER_API_KEY` **verilmez** (sitede kullanılmaz, ADR-0017).

### 10.4 Ağ, alan adı, volume, sağlık, bellek
- **Konteyner portu 8090.** Dokploy'da host'a port **YAYINLAMA** (Ports bölümü boş kalır) — konteynere yalnız
  Docker ağındaki Traefik ulaşır.
- **Domains:** `<alan-adi>` (ve istersen `www.<alan-adi>`), container port `8090`, HTTPS açık, sertifika
  sağlayıcı Let's Encrypt. DNS A/AAAA kaydı sunucuyu göstermeli.
- **Volume (ŞART):** kalıcı volume → konteyner yolu `/app/data` (ziyaretçilerin AI hafızası, raporlar, vektör
  deposu). Volume yoksa her yeniden dağıtımda bu veri kaybolur. Dizin `uid 10001`'e aittir; bind mount
  kullanırsan host dizininin sahibini `10001:10001` yap.
- **Sağlık kontrolü — iki uç (canlılık/hazırlık ayrımı):**
  - **Canlılık (liveness):** `GET /api/saglik/canli` → sabit 200 `{"durum":"canli"}`; veritabanına **dokunmaz**.
    İmajdaki Docker `HEALTHCHECK` bunu yoklar (`curl`, 60 sn aralık, 90 sn başlangıç payı, 10 sn zaman aşımı,
    3 deneme). Dokploy'da sağlık kontrolü yolu da **bu** olmalı: veritabanı kesintisinde konteyner gereksiz yere
    yeniden başlatılmaz (yeniden başlatma veritabanını düzeltmez, yalnız siteyi tamamen düşürür).
  - **Hazırlık/izleme (readiness):** `GET /api/saglik` → 200 `{"durum":"ayakta"}`; veritabanı yoksa 503
    `{"durum":"veritabani-yok"}`. Dış izleme (uptime) aracı bunu yoklasın. Veritabanı tamamen düşükken uç, havuzun
    bağlantı bekleme süresi (`APP_SITE_DB_BAGLANTI_ZAMAN_ASIMI_MS`, varsayılan 5 sn) + sorgu zaman aşımı (2 sn)
    ≈ 7 sn içinde 503 döner; izleme aracının zaman aşımını ≥ 10 sn tut (bekleme süresini yükseltirsen en az
    bekleme + 2 sn).
  - İki uç da çerez yazmaz, ziyaretçi kaydı açmaz, hız sınırı kovası tüketmez.
- **Bellek sınırı:** 768 MB–1 GB (Advanced → Resources). JVM yığını sınırın %75'i; bellek biterse süreç düşer ve
  Dokploy yeniden başlatır.

### 10.5 Vekil güveni (`APP_SITE_GUVENILIR_VEKIL`)
Uygulama `X-Forwarded-For/Proto` başlıklarına yalnız güvenilir vekilden gelince inanır. Varsayılan liste Tomcat'inki
(10/8, 172.16/12, 192.168/16, 127/8 …) — Docker'da **aynı ağdaki başka projelerin konteynerleri de** bu
aralıktadır ve başlığı taklit edip IP başına hız sınırını atlatabilir. Traefik konteynerinin IP'sine/alt ağına
**daralt**:
```bash
docker network inspect dokploy-network --format '{{range .Containers}}{{.Name}} {{.IPv4Address}}{{"\n"}}{{end}}' | grep -i traefik
# ör. çıktı: dokploy-traefik 10.0.1.5/24  →  APP_SITE_GUVENILIR_VEKIL=10\.0\.1\.5
```
Değer bir Java düzenli ifadesidir (noktalar `\.` ile). Traefik IP'si yeniden oluşturmada değişebiliyorsa alt ağı
ver (ör. `10\.0\.1\.\d{1,3}`) — bu durumda aynı alt ağdaki komşular yine güvenilir kalır; mümkünse ayrı ağ.
**Belirti:** sitenin tamamında `429` (çok fazla istek) = vekil güveni yanlış ayarlı — Traefik güvenilmediği için
tüm ziyaretçiler Traefik'in IP'siyle görünür ve tek bir IP kovasını paylaşır.

**Swarm VIP tuzağı (Dokploy varsayılanı — ilk canlı kurulumda yaşandı, 2026-11-27):** Dokploy uygulamaları Swarm
servisi olarak çalışır; varsayılan uç noktası modu **VIP**'dir. Bu modda Traefik → uygulama trafiği IPVS yük
dengeleyicisinden geçer ve uygulama **tüm istemcileri o dengeleyicinin iç adresi** (ör. `10.0.1.4`) olarak görür.
O adrese güvenmek YANLIŞ olur — ağdaki her konteyner aynı adresten geçer. Doğrusu:
1. Uygulama servisinin uç noktası modunu **`dnsrr`** yap (Dokploy → uygulama → Advanced/Swarm ayarları →
   Endpoint Spec `{"Mode":"dnsrr"}`; API: `application.update` → `endpointSpecSwarm`).
2. VIP'ten `dnsrr`'ye geçiş var olan serviste eski DNS kaydını bırakır → servisi **silip yeniden oluştur**
   (volume ve Postgres ayrı olduğu için veri kaybolmaz; yine de önce §10.6 yedeği al).
3. `APP_SITE_GUVENILIR_VEKIL`'i yalnız `dokploy-traefik` konteynerinin IP'sine daralt (yukarıdaki `docker network
   inspect` komutu) ve aşağıdaki doğrulamayı yap.

**Traefik IP'si değişirse:** `dokploy-traefik` konteyneri yeniden oluşturulduğunda (Dokploy güncellemesi, sunucu
yeniden başlatma) IP'si değişebilir → belirti yine sitenin tamamında `429`. Çözüm: yeni IP'yi bul,
`APP_SITE_GUVENILIR_VEKIL`'i güncelle, Redeploy, doğrula. Dokploy/Traefik güncellemesinden sonra bunu kontrol et.

**Doğrulama** (kurulumdan sonra ve `APP_SITE_GUVENILIR_VEKIL`'i her değiştirdiğinde):
1. Dokploy → Environment'a **geçici olarak** `LOGGING_LEVEL_TR_VELZON_AIOGRETMEN_VEKIL=DEBUG` ekle, Redeploy.
2. Siteyi kendi bilgisayarından aç (sayfa açılışı `/api/...` isteği yapar).
3. Dokploy → Logs: `istemci IP (maskeli): a.b.x.x` satırı. Gördüğün ilk iki oktet **kendi** genel IP'nin ilk iki
   okteti olmalı (kendi IP'ni herhangi bir "IP'm ne" sayfasından öğren). Docker iç ağı (`10.x.x`, `172.x.x`)
   görünüyorsa Traefik güvenilmiyor → `APP_SITE_GUVENILIR_VEKIL`'i düzelt.
4. Değişkeni **kaldır**, Redeploy (DEBUG kalıcı açık kalmaz; IP zaten maskeli yazılır, tam adres günlüğe girmez).

Not: ortam değişkeni adı tam bu olmalı; Spring ortam değişkenlerini küçük harfe çevirdiğinden sınıf adına göre
(`..._HIZSINIRIINTERCEPTOR`) logger açılamaz — kod bu yüzden küçük harfli `tr.velzon.aiogretmen.vekil` logger'ını
kullanır. Ayrıca https ile girince `aio_ziyaretci` çerezi `Secure` olmalı (değilse Traefik güvenilmiyor demektir;
`X-Forwarded-Proto` da yok sayılıyordur).

### 10.6 İlk açılış, güncelleme, yedekleme
- İlk dağıtım: Deploy → loglarda `Started AiOgretmenApplication`; şema kendiliğinden kurulur. Sonra §4'teki
  tarayıcı kontrolleri (`https://<alan-adi>/`, çerezler `Secure`, `/api/ozellikler`).
- Güncelleme: yerelde `./mvnw verify` yeşil → depoya gönder → Dokploy'da Redeploy (volume ve Postgres korunur).
  Otomatik dağıtım (autoDeploy) **kapalı**: push siteyi kendiliğinden güncellemez; yeni sürümü panelden yayına al.
- **Yedekleme:** (1) Postgres: Dokploy'un Postgres yedekleme özelliği ya da `pg_dump -Fc` (§6); (2) `/app/data`
  volume'ü (ör. `docker run --rm -v <volume-adi>:/v -v "$PWD":/yedek alpine tar czf /yedek/data.tgz -C /v .`).
  İkisi birlikte geri yüklenir; yalnız biri yetmez (hafıza dosyaları `z-<kimlik>` ile veritabanına bağlı).
- Güvenlik notu: sitenin alan adı altında **güvenilmeyen bir alt alan adı açma** (çerez sabitleme riski — ADR-0018
  §Güvenlik notu).

### 10.7 SEO ve analitik
> ADR-0019 (+ ADR-0020: JS'siz ders sayfaları, llms.txt). İki değişken de isteğe bağlıdır; verilmezse site eskisi gibi çalışır.

- **Ne var:** Türkçe `<title>` + `description`, Open Graph / Twitter kartı, schema.org `Course` (JSON-LD),
  `favicon.svg`, `GET /robots.txt` (her şeye izin — `/api/` kapatılmaz, Googlebot SPA'yı çizerken ona ihtiyaç duyar; `/api/**`
  yanıtları bunun yerine `X-Robots-Tag: noindex` taşır) ve `GET /sitemap.xml`. Bot uçları çerez yazmaz,
  ziyaretçi kaydı açmaz (sağlık uçları gibi).
- **`APP_SITE_TEMEL_URL=https://<alan-adi>`** (yalnız `https://`; `http://` yalnız `APP_SITE_GUVENLI_CEREZ=false`
  ile yerel denemede kabul edilir, yoksa boş + WARN): ana sayfaya canonical + `og:url`, `robots.txt`'ye `Sitemap:` satırı
  eklenir, `sitemap.xml` açılır (yoksa 404). `www` kullanıyorsan tercih ettiğin TEK adresi yaz.
- **JS'siz ders sayfaları (ADR-0020):** GPTBot, ClaudeBot, PerplexityBot gibi AI tarayıcılarının çoğu JavaScript
  çalıştırmaz; SPA'yı göremezler. Aynı içerik sunucuda HTML olarak da sunulur: `/dersler` (program dizini),
  `/hafta/N`, `/hafta/N/gun/G`, `/kaynak/<slug>` (+ `/llms.txt`, `/llms-full.txt`). Bu sayfalar salt okunurdur
  (kilit/ilerleme yok, quiz cevabı yok), çerez yazmaz, ziyaretçi kaydı açmaz, `Cache-Control: public, max-age=3600`.
  Mülakat içeriği hiç yayımlanmaz. `sitemap.xml` ana sayfa + bu sayfaların hepsini listeler. "Bu dersi etkileşimli
  çalış" bağlantısı `/?hafta=N&gun=G` ile uygulamada o haftayı/günü açar.
  Sayfalar açılışta bir kez üretilir; yanıtlar gzip'lenir (`server.compression`; systemd/nginx yolunda
  `deploy/nginx-site.conf.ornek` `gzip on`). `APP_SITE_GA_OLCUM_KIMLIGI` ayarlıysa bu sayfalar da harici `/s.js` ile
  sayfa görüntülemesi gönderir (GA4 *Gerçek zamanlı*'da `/hafta/N/gun/G` yolları); ayarlı değilse betik yok.
- **Bing Webmaster Tools** (Bing + ChatGPT arama/Copilot Bing dizinini kullanır): <https://www.bing.com/webmasters>
  → *Google Search Console'dan içe aktar* (Import from GSC) → Google hesabıyla izin ver → mülk ve site haritası
  kendiliğinden gelir (ayrı DNS doğrulaması gerekmez). Gelmezse *Site haritaları* → `https://<alan-adi>/sitemap.xml`.
- **Canlı denetim (dağıtımdan sonra):** `curl -s https://<alan-adi>/llms.txt | head` → `# SokratAI` ile başlamalı;
  `curl -sI https://<alan-adi>/hafta/1/gun/1` → `200`, `Cache-Control: max-age=3600, public`, `Set-Cookie` YOK;
  `curl -s -H 'Accept-Encoding: gzip' -D - -o /dev/null https://<alan-adi>/llms-full.txt | grep -i content-encoding` → `gzip`;
  `curl -s https://<alan-adi>/sitemap.xml | grep -c '<loc>'` → ana sayfa + `/dersler` + hafta + gün + kaynak sayısı.
  `curl -s https://<alan-adi>/robots.txt` → `GPTBot`, `ClaudeBot`… grupları `Allow: /`.
- **Google Search Console:** <https://search.google.com/search-console> → *Mülk ekle* → **Alan adı** →
  verilen `google-site-verification=…` TXT kaydını alan adının DNS'ine (Hostinger → DNS) ekle → *Doğrula*. Sonra
  *Site haritaları* → `https://<alan-adi>/sitemap.xml` gönder.
- **Google Analytics 4:** <https://analytics.google.com> → mülk oluştur → *Veri akışı* → **Web** → adres
  `https://<alan-adi>` → ölçüm kimliğini (`G-…`) `APP_SITE_GA_OLCUM_KIMLIGI`'ye yaz → Redeploy. Sanal sayfa
  görüntülemelerini uygulama kendisi gönderir (adres çubuğu değişmez).
- **GA4 veri akışı → Gelişmiş ölçüm (enhanced measurement) → ayarlar:** **tarayıcı geçmişi olaylarına dayalı sayfa
  değişiklikleri**, **form etkileşimleri** ve **site içi arama**yı KAPAT (ilk ikisi sanal sayfa/olaylarla çift sayar
  ya da form alanı adları gönderir; site içi arama yok). Veri akışındaki **kullanıcı tarafından sağlanan veri
  toplama** (user-provided data collection) KAPALI kalsın.
  Doğrulama: siteyi aç, bir haftaya tıkla → GA4 *Gerçek zamanlı*'da `/hafta/N` görünmeli.
- **"Şu an kaç kişi var?":** GA4 → *Raporlar* → **Gerçek zamanlı** (son 30 dakikadaki etkin kullanıcılar).
- **Ne gönderilir:** sanal sayfa görüntülemesi (`/`, `/hafta/3`, `/hafta/3/gun/2`, `/kaynak/<slug>`, `/atolye`) ve
  adı sabit olaylar `gorev_tamamlandi` (hafta/gün numarası), `sohbet_mesaji`, `anahtar_eklendi`. **Gönderilmez:**
  ziyaretçi kimliği, ilerleme kodu, sohbet metni, OpenRouter anahtarı (`user_id` yok). Google sinyalleri ve reklam
  kişiselleştirmesi kapalı; GA4 IP adresini saklamaz.
- **Çerez onay bandı yok** (kullanıcı kararı, ADR-0019). GA4 `_ga` çerezlerini Google yazar.
- **CSP:** `index.html` yalnız `www.googletagmanager.com` (betik) ve `*.google-analytics.com`,
  `*.analytics.google.com`, `*.googletagmanager.com` (bağlantı/görsel) için genişletildi; satır içi betik yok. Analitik kodu ayrı dosyada değil, `app.js` içinde (engelleyici
  gtag.js'i engellerse site çalışmaya devam eder).
- **Kapatmak:** `APP_SITE_GA_OLCUM_KIMLIGI`'yi sil → Redeploy (betik yüklenmez, hiçbir istek gitmez).
- **Paylaşım görseli yok:** `og:image` (1200×630 PNG) eklenmedi — ikili dosya sızıntı taramasının izin listesine
  (`tools/sizinti-ikili-izinli.txt`) girmeden işlenemez; eklenince `twitter:card` `summary_large_image` yapılır.
