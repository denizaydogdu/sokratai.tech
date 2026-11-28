# Faz 15: SDLC boşluk metinleri (ve Faz 16 ön planı: mimarın temel konuları)

- **Durum:** sürüyor (başlangıç 2026-11-28)
- **İstek (kullanıcı, 2026-11-28):** dışarıdan gelen bir eğitim tanıtımının konu listesi müfredata karşı
  karşılaştırıldı; "4 metinle başla", "mimarın bilmesi gereken Kubernetes, Kafka gibi ana konular ayrı bir alana",
  "işi fazlandır". Araç kullanım dersleri (OpenCode vb.): "sonra bakalım".
- **İlke:** o eğitimin içeriği elimizde değil ve kullanılmaz; konu listesi yalnız boşluk kontrol listesidir. Metinler
  birincil kaynaklardan (resmî belge, şartname, RFC, OWASP) özgün yazılır; eğitimin ya da eğitmenin adı içerikte geçmez.
- **Kurallar:** kaynak metni konvansiyonu (AGENTS.md: 3 inline SVG, öz-sınav, ⚠️ ZORUNLU ŞERH), `docs/yazim-kilavuzu.md`,
  SokratAI deposu çalışan örnek: depoyla ilgili her iddia dosya yoluyla doğrulanır, depoda olmayan şey "yok" diye yazılır.

## Boşluk analizi (özet, 2026-11-28)

| Durum | Konular |
|---|---|
| Güçlü | bağlam mühendisliği / AGENTS.md (H2), spec ve ExecPlan (H3), ADR (H2, H10), AI kod incelemesi (H12, H1), NFR (H5), Semgrep/gitleaks kavramı (H4), LLM gözlemlenebilirliği ve maliyet (`evals-llm-observability`, `finops-ai-tokenomics`) |
| Kısmi | TDD (kısa), GIVEN/WHEN/THEN, OpenAPI (bir paragraf), tehdit modelleme (yalnız LLM'e özgü), alt ajan / worktree, yerel model |
| Yok | Testcontainers, mutation testi, coverage sınırları, sistematik hata ayıklama, CI hattı, probe / rollout / rollback, OpenTelemetry → Prometheus → Grafana, ajan token/maliyet telemetrisi |

## Fazlar

- [x] **15.0 Boşluk analizi ve plan** (bu belge)
- [x] **15.1 Test kalitesi:** `content/kaynaklar/test-kalitesi-ai-cagi.md` (etiket 3,4,9)
- [x] **15.2 Tehdit modelleme:** `content/kaynaklar/tehdit-modelleme-stride.md` (etiket 4,7)
- [x] **15.3 Önce sözleşme ile API:** `content/kaynaklar/api-sozlesme-once.md` (etiket 6,9)
- [x] **15.4 Teslimat hattı ve gözlemlenebilirlik:** `content/kaynaklar/teslimat-hatti-gozlemlenebilirlik.md` (etiket 4,8,11)
- [x] **15.5 Denetim:** doğrulama betiği (uzun çizgi, 3 SVG, CSP-temiz, XML geçerli, marker öneki), `IcerikYazimKurallariTest`,
  `/api/kaynak/{slug}` → 3 svg, bağımsız olgu incelemesi (SokratAI iddiaları depoya karşı), `./mvnw verify`
  - Tehdit modeli yazılırken çıkan bulgular (doğrulandı): (a) `docs/SECURITY.md` "500 yanıtında `detay: e.getMessage()`"
    kabulü eskidi, `HataYonetimi` artık sabit mesaj + referans dönüyor → düzelt; (b) GA ölçüm kimliği verilince CSP
    `googletagmanager.com` betiğine izin veriyor; aynı origin'de çalışan bu betik "Bu cihazda hatırla" ile `localStorage`'a
    yazılan ziyaretçi anahtarını okuyabilir. ADR-0019'da kayıtlı değil → kullanıcı kararı + ADR/SECURITY kaydı.
    **Kapandı (2026-11-28):** (a) SECURITY.md maddesi kapatıldı; (b) kullanıcı "kabul" seçti → ADR-0019 "Sonuçlar" ve
    SECURITY.md "Bilinçli kabul edilen riskler"e yazıldı (yeniden açma koşulu: başka üçüncü taraf betik). Not: `sessionStorage`
    da aynı origin'deki betiklerce okunur; azaltma sayılmaz.
  - Test kalitesi metni yazılırken çıkan iki test zayıflığı **kapandı** (önce kırmızı, sonra yeşil; uygulama kodu değişmedi):
    `SemaTasinabilirlikTest` `toUpperCase()` → `Locale.ROOT` (surefire tr_TR'de küçük harfli H2 sözdizimi kaçıyordu) +
    `kucukHarfliH2SozdizimiDeYakalanir`; `QuizUretimServiceTest.dogruIndexSinirlari_0ve3Gecer_eksi1ve4Ayiklanir`
    (`< 4` → `<= 4` mutantı elle denendi, artık ölüyor). Kod değişikliği içerikten ayrı commit.
  - API ve teslimat metinlerinden çıkan bulgular **kapandı**: `POST /api/gorev/durum` konum alanları zorunlu (400) +
    `IlerlemeService` görevin müfredatta varlığını denetler (404, kayıt yok; MCP de aynı yol); dilim testleri sabit
    test müfredatına bağlandı (`TestIcerikYapilandirma`). README 502/503 eşlemesi, ARCHITECTURE "CI'da bloklayıcı",
    kurulum-site imaj etiketi düzeltildi.
  - Olgu kontrolü (bağımsız ajan): yüksek önemde hata yok; düzeltilenler: ana sayfa CSP'si sabit (GA alanları ve
    `googletagmanager.com` kimlik olmasa da listede) → tehdit metni, ADR-0019, SECURITY.md, `llm-uygulama-guvenligi`
    (`img-src` GA alanlarını da içerir); API metninde `detay`/`referans` tablosu, Spring 6.0/RFC 7807, OpenAPI 3.1.2,
    YAML'a 404; "root@ SSH hedefi"; SRE Bölüm 3.
  - Sonuç: dört metin `/api/kaynak/{slug}` → 3 svg; kaynak sayısı 38 → 42; `./mvnw verify` **542 test**, 0 hata,
    5 atlanan; qa-code-reviewer: engelleyici bulgu kapandı (`KullaniciIzolasyonTest` bağlamı).
- [ ] **15.6 Yayın:** içerik commit'i, push, Dokploy Redeploy (kullanıcı onayıyla)
- [ ] **15.7 Küçük eklemeler:** Hafta 3 Gün 1 spec şablonuna GIVEN/WHEN/THEN; `harness-mekanigi`'ne alt ajan / worktree
  bölümü; `model-ve-caba`'ya yerel model ödünleşimi; Hafta 12'ye "SokratAI deposunda bir işin yolu"; atölyeye 4–6 alıştırma

## Faz 16 (ön plan, karar bekliyor): "Mimarın temel konuları" alanı

- **Bugün böyle bir alan yok:** kaynaklar yalnız hafta etiketiyle görünür (`app.js` `kaynakBolumu`), `types/Kaynak`'ta
  kategori alanı yok. "Temeller Tazeleme" metinleri (`temel-*`) yalnız ad önekiyle ayrışıyor.
- **Öneri:** frontmatter'a isteğe bağlı `alan: temeller` (çoklu üyelik: metin kendi haftasında da görünür); sol menüde
  Prompt Atölyesi gibi kilitsiz ayrı görünüm; JS'siz `/temeller` sayfası + sitemap + llms.txt; testler. Kod değişikliği
  içerikten ayrı commit; yeni bağımlılık yok.
- **Aday ilk dalga:** Kubernetes temelleri; Kafka ve olay akışı (mesaj kuyruğundan farkı); önbellekleme (Redis, stratejiler);
  veritabanı seçimi ve indeksler; dağıtık sistem temelleri (CAP/PACELC, tutarlılık modelleri); ağ temelleri (DNS, HTTP/2-3,
  TLS, yük dengeleyici, CDN); kimlik ve yetki (OAuth 2.x, OIDC, JWT tuzakları); dayanıklılık desenleri (timeout, retry,
  circuit breaker, bulkhead); API gateway ve service mesh; arama motorları.
  **Kullanıcı eklemesi (2026-11-28), bilgi amaçlı ve basit düzeyde:** (1) mimari stillere giriş: monolit, modüler monolit,
  mikroservis nedir, ne zaman hangisi (Hafta 6 Gün 1–2 ve `mikroservis-monolit-hatalar` derinlemesine anlatıyor; alan
  metni kısa bir kapı olur ve onlara bağlanır); (2) programlama dilleri turu: Java, C#, Python, Go, Rust, TypeScript/JavaScript
  (her biri: nerede güçlü, çalışma modeli, ekosistem, mimara etkisi; dil savaşı değil, karar ölçütleri). Hafta 6 (dağıtık stiller) ve Hafta 11 (DDIA:
  replikasyon, bölümleme) ile tekrar değil bağlantı.
- **Uygulayıcı görüşü (mikroservis kodu yazan başka bir Claude oturumu, 2026-11-28; genel, proje adı yok):** eğitimlerde az
  anlatılan ama şart konular. Faz 16 adaylarına eklendi:
  - **Veri tutarlılığı ve idempotency (ayrı başlık):** dual write, outbox; inbox / idempotent tüketici; en az bir kez
    teslimin (at-least-once) sonuçları; saga ve telafi.
  - **Olay sırası:** partition key, aynı anahtarda sıranın çok pod'da bozulması, sıraya bağımlı tüketici tasarlamamak.
    Kafka tüketici ayarlarının anlamı (max.poll.interval, max.poll.records, rebalance, retry topic ve DLT).
  - **Çok pod'da zamanlanmış iş:** dağıtık kilit (ShedLock), kilit süresi ile iş süresi payı, claim tablosu ve SKIP LOCKED.
  - **Uzak çağrı ve transaction:** uzak çağrıyı DB transaction'ı ve kilit dışında yapmak; timeout + circuit breaker + retry'ı
    idempotency ile birlikte düşünmek; servisler arası hata sözleşmesi (retryable / kalıcı ayrımı).
  - **Migration ve sıfır kesintili deploy:** önce migration sonra yeni imaj; add-nullable → backfill → sıkılaştır; concurrent index.
  - **ORM tuzakları:** persistence context ve bayat okuma, persist/merge farkı, N+1, batch insert'i bozan flush.
  - **Bağlantı havuzu bütçesi:** pod sayısı × havuz ≤ DB bağlantı sınırı; statement/lock timeout; graceful shutdown.
  - Dağıtık izleme (correlation id), metrik ve alarmın birlikte tasarımı: Faz 15.4 metnine bağlanır.
  - Ajanların mikroservis kodunda sık hataları (kodu doğrulamadan eski bulguya göre değiştirmek, transaction/thread sınırını
    okumadan davranış iddiası, eski kodda da geçen test, kapsam genişletme): Faz 15.1 ve ajan disiplinine örnek.
  - Görüş: dil turu bu dalgada dağıtıcı olabilir, ayrı dalga olarak tutulabilir (karar kullanıcıda).
- **Alana bağlanacak mevcut metinler:** `temel-*`, `cloud-saglayici-turu`, `api-tasarim-grpc`, `mikroservis-monolit-hatalar`,
  `system-design-interview` ve Faz 15'in dört metni.
- **Açık sorular:** alanın adı; ilk dalganın konu ve sırası; başlıklar ürün adıyla mı (Kafka) kavramla mı (olay akışı).

## Ertelenen

- Araç kullanım dersleri (OpenCode vb.): araçlar hızlı değişiyor; kullanıcı "sonra bakalım" dedi (2026-11-28).
- Kubernetes uygulamalı laboratuvarı: platform cluster işini doğrulayamaz; Faz 16'da kavram metni olarak.
