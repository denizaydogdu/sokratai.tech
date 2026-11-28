# ARCHITECTURE.md — AI Öğretmen

## Katman haritası (tek yönlü, ArchUnit ile zorlanır)

```
types → config → repo → service → ai → web
```

| Katman | Paket | İçerik | Erişebilir |
|---|---|---|---|
| Types | `tr.velzon.aiogretmen.types` | record'lar, enum'lar, DTO'lar | (hiçbir şey) |
| Config | `..config` | AppProperties, model adları, content yolu | types |
| Repo | `..repo` | H2/PostgreSQL erişimi (Spring Data JDBC) + ContentRepo (markdown okuyucu) | types, config |
| Service | `..service` | MufredatService, IlerlemeService, (Faz 2: Degerlendirme/Quiz/Rapor) | types, config, repo, ai |
| AI (runtime) | `..ai` | Claude entegrasyonu — Spring AI ChatClient BURADA yaşar, tek çıkış noktası | types, config |
| Web (UI) | `..web` | REST controller'lar + static/ frontend — SADECE service'i çağırır | types, service |

## Kurallar
- `types` hiçbir katmana bağımlı olamaz.
- `web` doğrudan `repo` veya `ai`'a erişemez — her şey `service` üzerinden.
- Claude API çağrısı SADECE `ai` paketinde (Spring AI sınıfları başka pakette import edilemez).
- Kesişen kaygılar (ileride: telemetri, feature flag) arayüz üzerinden girer.

## Veri
- H2 file DB: `./data/ogrenme.mv.db` (yerel/mcp); `site` profilinde PostgreSQL (ADR-0014). Şema tek ve
  taşınabilir: `src/main/resources/schema.sql` (H2 + PostgreSQL, idempotent).
- Müfredat: `content/hafta-NN/{hafta.md, gun-N.md}` — frontmatter'lı markdown, açılışta belleğe yüklenir.
- Öğrenci hafızası (Faz 2): `data/hafiza/` markdown ağacı — AI öğretmen yazar, prompt'lara enjekte edilir.

## Çalışma modları (profiller)
- **Yerel** (profil yok): tek kullanıcı (`yerel`), kimlik yok; tüm AI özellikleri sunucu anahtarıyla
  (`OPENROUTER_API_KEY`).
- **`site`** (ADR-0017): girişsiz, çok ziyaretçili. Kimlik = `aio_ziyaretci` çerezi → `kullanici_id` `z-<kimlik>`
  (`config.ZiyaretciKullaniciBaglami`, fail-closed); hesap/e-posta yok. AI yalnız sohbet ve **ziyaretçinin kendi
  anahtarıyla** (`X-OpenRouter-Anahtar` → `config.BaslikAiAnahtarBaglami` → `ai.ZiyaretciChatModelKaynagi`,
  istek başına; sunucu anahtarı okunmaz, anahtar hiçbir yerde saklanmaz). Sohbet dışı AI uçları
  `@Profile("!site")`. IP/ziyaretçi hız sınırları (`web.HizSiniriInterceptor`), 180 gün temizliği
  (`service.ZiyaretciTemizlikService`).
- **Dağıtım (Faz 11, ADR-0018):** `site` profili canlıda **sokratai.tech** olarak Hostinger VPS üzerinde Dokploy
  (Docker konteyneri + Traefik/TLS + Dokploy PostgreSQL, kalıcı `/app/data`) ile çalışır; konteyner içinde
  `0.0.0.0:8090`, host portu yayınlanmaz. Canlılık ucu `GET /api/saglik/canli` (Docker `HEALTHCHECK`, DB'siz), hazırlık/izleme
  `GET /api/saglik` (DB). Loopback dışı bind yalnız `site` profiliyle (`config/BindAdresiDenetimi`). Adımlar: `docs/kurulum-site.md` §10.
- **Herkese açık sayfalar ve SEO (Faz 12, ADR-0019/0020, yalnız `site`):** JS'siz, çerezsiz, salt okunur `/dersler`,
  `/hafta/N[/gun/G]`, `/kaynak/<slug>` (mülakat HARİÇ), `/llms.txt`, `/llms-full.txt`, `robots.txt`, `sitemap.xml`
  (112 adres); sayfalar açılışta üretilir, gzip'lenir. GA4 yalnız `APP_SITE_GA_OLCUM_KIMLIGI` verilirse yüklenir
  (onay bandı yok); canonical/sitemap `APP_SITE_TEMEL_URL` ile; `/api/**` `X-Robots-Tag: noindex`.
- **"OpenRouter ile bağlan" (Faz 13):** OAuth PKCE tamamen tarayıcıda (CSP `connect-src` + `openrouter.ai`); sunucu
  anahtarı görmez, yalnız sohbet isteğinin `X-OpenRouter-Anahtar` başlığında gelir.
- **Prompt Atölyesi AI değerlendirmesi (Faz 14):** yalnız yerel modda ve yalnız `ai:true` alıştırmada (her
  kategorinin son "birleştir" alıştırması); `AtolyeService.degerlendir` `ai:false` için model çağırmadan 400 döner.
- **İçerik ve dil (Faz 14):** `content/` tek doğruluk kaynağı (HTML dönüştürücü çalıştırılmaz). Türkçe metin
  `docs/yazim-kilavuzu.md`'ye uyar; `IcerikYazimKurallariTest` içerik, prompt ve HTML'de uzun çizgiyi (—) yakalar.
- **`mcp` / `mcp-http`** (ADR-0007/0008): MCP sunucusu; `mcp-http` JWT kimlikli.

## Doğrulama
`./mvnw verify`: 542 test (5 atlanan PostgreSQL opt-in) + ön yüz için 16 Playwright gerçek tarayıcı testi (ADR-0021).
Bu kurallar `src/test/java/tr/velzon/aiogretmen/MimariTestleri.java` (ArchUnit) ile test edilir; kural ihlali =
build kırmızı. Sunucu tarafı CI yok: kapı, merge/push öncesi yerelde koşan `./mvnw verify` ve push öncesi sızıntı
taraması kancasıdır (AGENTS.md "PR / commit kuralları").
