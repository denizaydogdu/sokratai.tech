---
baslik: "Önce Sözleşme: OpenAPI, Hata Modeli, Sürümleme ve İdempotentlik"
yazar: "AI Öğretmen ek okuma metni: API sözleşmesi konusu"
tarih: "2026-11-28"
dil: "Türkçe kavram özeti (diyagramlı)"
etiketler: 6,9
---

### Bu metin ne anlatıyor?
İki program konuşacaksa önce neyi, hangi biçimde konuşacaklarında anlaşmaları gerekir. Bu anlaşmanın
yazılı hâline **API sözleşmesi** (contract) denir. Bu metin, sözleşmeyi koddan **önce** yazmanın ne
kazandırdığını ve bir sözleşmenin dört kritik konusunu anlatıyor: belgenin kendisi (OpenAPI), hataların
biçimi (RFC 9457), değişikliklerin yönetimi (sürümleme) ve aynı isteğin iki kez gelmesi
(idempotentlik).

Konu yeni değil, ama bir şey değişti: artık kodun bir kısmını **ajanlar** (agent) yazıyor. Ajan, insan
gibi "bunu kim kullanıyor?" diye durup düşünmez; önüne ne konduysa ona uyar, konmayan şeyi de
uydurur. Makinenin denetleyebildiği bir sözleşme, ajanın uyduracağı alanı daraltır. Her başlıkta
SokratAI deposuna bakacağız: hangi parça var, hangisi yok, olmayan parça ne zaman gerekir.

### Bu metinde geçen temel terimler
- **İstemci** (client): isteği gönderen taraf. SokratAI'de tarayıcıdaki `app.js`.
- **Uç** (endpoint): bir HTTP yöntemi ile bir adresin çifti, örneğin `POST /api/gorev/durum`.
- **Şema** (schema): bir verinin biçim kuralı: hangi alanlar var, türleri ne, hangileri zorunlu.
- **Kırıcı değişiklik** (breaking change): var olan bir istemciyi bozan değişiklik.
- **İdempotent:** aynı isteği N kez göndermenin etkisi, bir kez göndermenin etkisiyle aynıdır.
  Bu özelliğin adı **idempotentlik**tir (idempotency).
- **Sözleşme testi** (contract test): kodun sözleşmeye hâlâ uyup uymadığını denetleyen test.

## Neden bu özet
[REST ve gRPC](api-tasarim-grpc.md) metni REST'in kaynak modelini anlattı ve OpenAPI'yi tek paragrafta
"REST'in yazılı sözleşmesi" diye andı; idempotentlik ve sürümleme orada birer madde olarak geçti. Bu
metin o maddeleri açıyor. Kaynak modelini ve REST ile gRPC karşılaştırmasını tekrar etmiyor.

Hafta 6'nın stilleri (servis tabanlı, mikroservis, olay güdümlü) sonunda birer sözleşmeye dayanır. Hafta 9'un
altıgen (hexagonal) mimarisinde HTTP sözleşmesi bir **gelen adaptörün** (driving adapter) dış yüzüdür:
doğrulama ve hata kodu orada, çekirdek HTTP'yi bilmez. Ortak soru: kapının dış yüzünü kim, ne zaman ve ne
kadar kesin yazar?

Üçüncü bağ Hafta 3 ile. Hafta 3, Gün 1 (`content/hafta-03/gun-1.md`) görev tarifinin (task spec) bitiş
kriterinin **mekanik** olmasını ister: çalıştırılan bir komut yeşil ya da kırmızı yanmalıdır.
Makinenin okuyabildiği bir sözleşme, "API'yi bozma" gibi yumuşak bir kısıtı böyle bir komuta
çevirmenin en kısa yoludur.

Üç diyagram fikri kısaca gösterir; ayrıntı metindedir.

## Türkçe çalışma metni

### 1. Sözleşme önce mi, kod önce mi: doğruluk kaynağı nerede?
Bir HTTP API'nin sözleşmesini iki yoldan elde edersin.

**Kod önce (code-first):** Önce controller'ları yazarsın; sözleşme belgesi, varsa, koddan üretilir.
Spring dünyasında bunu yapan yaygın kütüphane `springdoc-openapi`'dir: sınıflara ve açıklama notlarına
(annotation) bakıp bir OpenAPI belgesi çıkarır. Hızlıdır ve belge kodla kendiliğinden uyumludur. Bedeli
şudur: alan adları, hata biçimi, sayfalama gibi tartışmalı kararlar kod yazıldıktan **sonra** görünür
olur ve düzeltmek artık kodu değiştirmek demektir. Belge kodun aynasıdır; kodda bir yanlış varsa belgede
de vardır.

**Sözleşme önce (contract-first, "API first" diye de geçer):** Önce sözleşme belgesini yazarsın,
tartışırsın, onaylarsın. Sunucu kodu bu belgeye göre yazılır ya da iskeleti belgeden üretilir; istemci
tipleri ve taklit sunucu (mock server) da belgeden üretilir; testler kodun belgeden sapıp sapmadığını
denetler. Zalando'nun REST API kılavuzu bunu ilk kurallarına koyar: "API first ilkesine uy" ve "API
spesifikasyonunu OpenAPI ile ver". Bedeli disiplindir: belge kodla birlikte güncellenmezse yalan
söylemeye başlar. Yalan söyleyen bir belge, hiç belge olmamasından kötüdür.

İki yolun asıl farkı **doğruluk kaynağının** (single source of truth) yeridir. Kod önce yaklaşımda doğru
olan koddur, belge onu anlatır. Sözleşme önce yaklaşımda doğru olan belgedir, kod ona uymak zorundadır.
Bu bir ödünleşimdir (trade-off): hız ile önceden düşünmek arasında seçim yaparsın. Tek istemcili, tek
ekipli küçük bir uygulamada kod önce çoğu zaman yeter. İstemci ve ekip sayısı arttıkça sözleşme önce
yaklaşımın getirisi büyür, çünkü bir tartışmanın en ucuz olduğu an, kodun henüz yazılmadığı andır.

**Ajanlar bu dengeyi neden değiştiriyor?** İnsan geliştirici bir alanın adını değiştirmeden önce çoğu
zaman durup kimin kullandığını sorar. Ajan sormaz; ona verilen hedefe en kısa yoldan gider. "Yanıta
tamamlanma zamanını ekle" görevinde `tamamlandi` alanını "daha açık olsun" diye `tamamlandiMi` yapabilir,
bir hata durumunda 200 ile hata metni dönebilir ya da README'deki tabloyu güncellemeyi unutabilir. Bunların
hiçbirinde derleyici hata vermez. Hafta 3, Gün 1'in örnek görev tarifinde tam bu yüzden bir kısıt var:
"Mevcut API yanıtına alan **eklenir**, var olan alan silinmez." Bu cümle bir geriye dönük uyum (backward
compatibility) kuralıdır, ama düz yazıdır; denetimi incelemeyi yapan insana kalır. Makinenin okuduğu bir
sözleşme bu kısıtı bir komuta çevirir: "Koddan üretilen belge ile depodaki belge arasındaki fark yalnız
ekleme mi?" Bitiş kriteri artık bir yargı değil, bir komutun çıktısıdır.

**Sözleşme testleri, kısaca.** Sözleşmeyi yazmak yetmez; kodun ona uyduğunu bir şeyin sürekli denetlemesi
gerekir. Üç yaygın biçim var:

- **Sağlayıcı tarafı (provider) testi:** Sunucunun gerçek yanıtları sözleşmedeki şemaya uyuyor mu? Ucuzdur
  ve sunucunun deposunda koşar.
- **Tüketici güdümlü sözleşme testi (consumer-driven contract testing):** Her istemci, sunucudan **gerçekte**
  ne beklediğini örnek istek ve yanıtlarla yazar; sunucu bu beklentilere karşı test edilir. Bilinen aracı
  Pact'tir.
- **Taklidi gerçekle karşılaştıran test:** Martin Fowler'ın 2011 tarihli "ContractTest" notu, testlerde
  kullandığın sahte servisin (test double) zamanla gerçek servisten kaydığını anlatır; çaresi ikisini periyodik
  olarak karşılaştıran ayrı bir test kümesidir.

**SokratAI deposunda durum.** Depoda bir OpenAPI belgesi yok; `pom.xml`'de `springdoc` ya da başka bir
OpenAPI bağımlılığı (dependency) da yok. Yani depo ne sözleşme önce ne de belge üreten kod önce çalışıyor.
HTTP sözleşmesi üç yerde yaşıyor:

1. **README'nin "HTTP API uç noktaları" tabloları:** her uç için yöntem, yol ve amaç; bazı satırlarda gövde
   ve hata kodları. İnsan için yazılmış, okunur bir sözleşme.
2. **Controller kodu:** `web/ProgramController`, `web/ZiyaretciController` gibi sınıflardaki `record` istek
   tipleri ve `web/HataYonetimi`'ndeki hata eşlemeleri.
3. **Testler:** `web/ProgramControllerTest`'teki `olmayanHaftaHataSozlesmesiyleDoner`,
   `bozukGovde400VeSozlesmeliDoner`, `gorevDurumundaTamamlandiZorunlu_yoksa400` ve
   `gorevDurumundaKonumAlanlariZorunlu_yoksa400` testleri durum kodunu ve hata alanlarını (`$.hata`, kimi
   zaman `$.detay`) iddia eder; başarılı yanıttaki `$.tamamlandi` alanını ayrı bir test kilitler. Bunlar fiilen sağlayıcı tarafı sözleşme
   testleridir; tek farkla: şemayı bir belgeden değil, elle yazılmış iddialardan alırlar.

Makinenin okuduğu bir belge olmadan ne kaybediliyor? (a) Kırıcı değişikliği yakalayan otomatik bir fark
denetimi yok. README ile kod arasında sapma (drift) olursa onu ancak okuyan biri görür. Küçük bir örnek:
README'nin "Girdi sınırları ve hata kodları" maddesi uzun süre "sağlayıcı hatası ya da bozuk YZ çıktısı → 502"
diyordu. `HataYonetimi.aiSaglayiciHatasi` ise sunucu anahtarıyla gelen sağlayıcı hatalarının çoğunu (yanıt
yok, yoğun, kredi bitti, anahtar geçersiz) **503**, yalnız sağlayıcının reddettiği isteği **502** yapar.
Kısa özet yanlış sayılmazdı ama ayrıntıyı kaybediyordu; istemci yazacak bir ajan hangisine güveneceğini
bilemezdi. Fark bu metin yazılırken kodu okuyarak bulundu ve README düzeltildi. Makinenin okuduğu bir belge
olsaydı, bu farkı bir test yakalardı.
(b) İstemci tipi üretilemez; `app.js` alan adlarını elle bilir. (c) Bir ajan uçların biçimini öğrenmek için
Java kodunu okumak zorundadır; bağlam (context) penceresine tek bir tablo yerine birkaç sınıf girer.

Bugün bunun bedeli küçük, çünkü HTTP API'nin tek istemcisi aynı uygulamayla gelen ön yüzdür (`static/app.js`,
mülakat modülü açıksa `static/mulakat.js`). Bunun ne zaman değişeceği §5'in sonunda.

İlginç bir karşıtlık var: aynı uygulamanın **ajanlara açılan kapısı** zaten makinenin okuduğu bir sözleşme
taşıyor. MCP araçları (`mcp/OgretmenMcpAraclari`) girdi şemalarını JSON Schema olarak bildirir:
`McpAdaptorDestek.sema(...)` her araç için `type: object`, alan türleri, zorunlu alan listesi ve
`additionalProperties: false` üretir. MCP belirtimi (2026-07-28 sürümü) aracın `inputSchema`'sının bir
JSON Schema olduğunu ve `$schema` verilmezse 2020-12 sürümünün varsayıldığını söyler. Yani depoda tarayıcı
kapısı insan için yazılmış bir tabloyla, ajan kapısı makine için yazılmış bir şemayla tarif ediliyor. MCP
tarafının ayrıntısı [MCP adapter](mcp-adapter.md) §7'de.

## Diyagram 1: Kod önce ve sözleşme önce
Üstte kod önce: belge koddan çıkar ve kodu izler. Altta sözleşme önce: tek bir belge dört şeyi besler.
En altta SokratAI'nin bugünkü durumu.

```svg
<svg viewBox="0 0 640 400" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Kod önce yaklaşımda belge koddan çıkarılır ve kodu izler; sözleşme önce yaklaşımda tek sözleşme belgesi sunucu kodunu, istemci tiplerini, taklit sunucuyu ve sözleşme testini besler; SokratAI bugün OpenAPI belgesi taşımaz">
  <defs>
    <marker id="as-ok1" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#186399"/></marker>
    <marker id="as-ok2" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#25804C"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Doğruluk kaynağı nerede: kodda mı, sözleşmede mi?</text>
  <text x="20" y="56" font-size="12" font-weight="700" fill="#186399" font-family="system-ui">1 · Kod önce (code-first)</text>
  <rect x="20" y="66" width="180" height="54" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="110" y="89" text-anchor="middle" font-size="12.5" font-weight="700" fill="#1C3247" font-family="system-ui">Kod (controller)</text>
  <text x="110" y="107" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">önce yazılır</text>
  <rect x="230" y="66" width="180" height="54" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="320" y="89" text-anchor="middle" font-size="12.5" font-weight="700" fill="#1C3247" font-family="system-ui">Üretilen belge</text>
  <text x="320" y="107" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">koddan çıkarılır</text>
  <rect x="440" y="66" width="180" height="54" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="530" y="89" text-anchor="middle" font-size="12.5" font-weight="700" fill="#1C3247" font-family="system-ui">İstemci ve ajan</text>
  <text x="530" y="107" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">belgeyi okur</text>
  <line x1="200" y1="93" x2="226" y2="93" stroke="#186399" stroke-width="1.8" marker-end="url(#as-ok1)"/>
  <line x1="410" y1="93" x2="436" y2="93" stroke="#186399" stroke-width="1.8" marker-end="url(#as-ok1)"/>
  <text x="320" y="142" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Belge kodu izler: alan adı ve hata biçimi tartışması kod yazıldıktan sonra başlar.</text>
  <text x="20" y="180" font-size="12" font-weight="700" fill="#25804C" font-family="system-ui">2 · Sözleşme önce (contract-first)</text>
  <rect x="20" y="192" width="180" height="146" rx="12" fill="#E9F4EE" stroke="#25804C" stroke-width="1.8"/>
  <text x="110" y="250" text-anchor="middle" font-size="14" font-weight="700" fill="#25804C" font-family="system-ui">Sözleşme</text>
  <text x="110" y="270" text-anchor="middle" font-size="11" fill="#1C3247" font-family="system-ui">openapi.yaml</text>
  <text x="110" y="288" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">tek doğruluk kaynağı</text>
  <rect x="290" y="192" width="330" height="30" rx="8" fill="#FFFFFF" stroke="#25804C" stroke-width="1.3"/>
  <text x="455" y="212" text-anchor="middle" font-size="11.5" fill="#1C3247" font-family="system-ui">Sunucu kodu: sözleşmeye uyar</text>
  <rect x="290" y="231" width="330" height="30" rx="8" fill="#FFFFFF" stroke="#25804C" stroke-width="1.3"/>
  <text x="455" y="251" text-anchor="middle" font-size="11.5" fill="#1C3247" font-family="system-ui">İstemci tipleri: sözleşmeden üretilir</text>
  <rect x="290" y="270" width="330" height="30" rx="8" fill="#FFFFFF" stroke="#25804C" stroke-width="1.3"/>
  <text x="455" y="290" text-anchor="middle" font-size="11.5" fill="#1C3247" font-family="system-ui">Taklit sunucu: ön yüz beklemeden başlar</text>
  <rect x="290" y="309" width="330" height="30" rx="8" fill="#FFFFFF" stroke="#25804C" stroke-width="1.3"/>
  <text x="455" y="329" text-anchor="middle" font-size="11.5" fill="#1C3247" font-family="system-ui">Sözleşme testi: kod sözleşmeden saptı mı?</text>
  <line x1="200" y1="265" x2="286" y2="207" stroke="#25804C" stroke-width="1.6" marker-end="url(#as-ok2)"/>
  <line x1="200" y1="265" x2="286" y2="246" stroke="#25804C" stroke-width="1.6" marker-end="url(#as-ok2)"/>
  <line x1="200" y1="265" x2="286" y2="285" stroke="#25804C" stroke-width="1.6" marker-end="url(#as-ok2)"/>
  <line x1="200" y1="265" x2="286" y2="324" stroke="#25804C" stroke-width="1.6" marker-end="url(#as-ok2)"/>
  <text x="320" y="368" text-anchor="middle" font-size="11" font-weight="600" fill="#C77A17" font-family="system-ui">SokratAI bugün: OpenAPI belgesi yok; sözleşme = README tabloları + kod + MockMvc testleri</text>
  <text x="320" y="388" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Ajan kapısı (MCP araçları) ise girdi şemasını JSON Schema olarak bildirir.</text>
</svg>
```

### 2. OpenAPI 3.1: bir sözleşme belgesinin içinde ne var?
OpenAPI, HTTP API'lerini dilden bağımsız biçimde tarif eden, YAML ya da JSON ile yazılan bir belge
standardıdır; eski adı Swagger'dır. 3.1.0 sürümü 18 Şubat 2021'de, 3.1.1 düzeltme sürümü 24 Ekim 2024'te
yayımlandı; 3.1 hattında 3.1.2 de var. 19 Eylül 2025'te 3.2.0, ardından 3.2.1 çıktı; 3.2 yeni alanlar ekler (`QUERY` yöntemi, akış medya
türleri gibi) ama aşağıdaki temel yapı taşları aynıdır. Araçlarının hangi sürümü desteklediğine ayrıca bak.

Belgenin kökünde şu alanlar bulunur:

- `openapi` (belgenin uyduğu sürüm, ör. `3.1.1`) ve `info` (API'nin adı, sürümü, açıklaması, lisansı).
  İkisi de **zorunludur**.
- `servers`: API'nin hangi adreslerde sunulduğu.
- `paths`: uçların listesi. Her yol (`/api/gorev/durum`) bir Path Item nesnesidir; altında HTTP yöntemi
  başına bir **işlem** (operation) durur.
- `webhooks`: 3.1 ile gelen, API'nin dışarıya yaptığı çağrıları tarif eden bölüm.
- `components`: tekrar kullanılan parçaların deposu: `schemas` (veri şemaları), `responses`, `parameters`,
  `requestBodies`, `headers`, `securitySchemes` ve 3.1 ile gelen `pathItems`.
- `security`, `tags`, `externalDocs`.

3.1'de `paths` artık zorunlu değil: belgede `paths`, `components` ya da `webhooks` alanlarından en az biri
bulunmalı. Böylece yalnız şema içeren, paylaşılan bir "bileşen kütüphanesi" belgesi yazılabiliyor.

Bir işlem nesnesinin (Operation Object) önemli alanları şunlar: `operationId` (bütün API'de benzersiz
olmalı; kod üreticiler metot adını buradan alır), `parameters` (yol, sorgu, başlık ve çerez parametreleri),
`requestBody`, `responses` ve `deprecated`. `responses` durum kodlarını yanıt tariflerine eşler. En az bir
yanıt kodu içermek zorundadır; `2XX` gibi aralıklar ve geri kalan her şeyi karşılayan `default` anahtarı
kullanılabilir. Belirtim, başarılı yanıtın yanında bilinen hata yanıtlarının da belgelenmesini önerir. Bu
öneri §3'ün konusudur: hatalar da sözleşmenin parçasıdır.

**JSON Schema 2020-12 ile hizalanma.** 3.1'in en büyük değişikliği budur. 3.0'daki Şema Nesnesi (Schema
Object), JSON Schema'nın eski bir taslağının "genişletilmiş alt kümesi"ydi: bazı anahtarlar farklı anlam
taşıyor, bazıları hiç bulunmuyordu. 3.1'de Şema Nesnesi JSON Schema 2020-12'nin **üst kümesidir**
(superset): geçerli bir 2020-12 şeması, geçerli bir OpenAPI şemasıdır. Kökteki `jsonSchemaDialect` alanı,
belgedeki şemaların varsayılan lehçesini (dialect) belirler. Pratik sonucu şu: aynı şema dilini API
sözleşmesinde, mesaj doğrulamada ve (yukarıda gördüğün gibi) MCP araç tanımlarında ortak kullanabilirsin.

**3.0'dan 3.1'e geçişte değişenler (üst düzeyde):**

- `nullable: true` kalktı. Boş değere izin vermek için tür listesine `"null"` eklenir:
  `type: ["string", "null"]`.
- `exclusiveMinimum` ve `exclusiveMaximum` artık doğrudan sayı alır; 3.0'da `minimum` yanında bir evet/hayır
  işaretiydi.
- Tekil `example` yerine JSON Schema'nın dizi alan `examples` anahtarı kullanılır.
- Dosya yüklemede `format: binary` ve `format: base64` yerine `contentMediaType` ve `contentEncoding` gelir.
- `webhooks` kök alanı eklendi, `paths` isteğe bağlı oldu, lisans için SPDX kimliği geldi.

OpenAPI Initiative'in geçiş kılavuzuna göre kırıcı değişiklikler büyük ölçüde Şema Nesnesi ile sınırlıdır.

Aşağıdaki parça, README'deki `POST /api/gorev/durum` satırının OpenAPI 3.1 ile nasıl yazılabileceğini
gösterir. **Depoda böyle bir dosya yok.** Bu, insan için yazılmış tablonun makine diline çevrilmiş hâli
olurdu.

```yaml
openapi: 3.1.1
info:
  title: SokratAI HTTP API (örnek; depoda yok)
  version: "0.1.0"
paths:
  /api/gorev/durum:
    post:
      operationId: gorevDurumuAyarla
      summary: Görevi istenen duruma ayarlar (idempotent)
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/GorevDurumIstek"
      responses:
        "200":
          description: Görevin yeni (istenen) durumu
          content:
            application/json:
              schema:
                type: object
                required: [tamamlandi]
                properties:
                  tamamlandi: { type: boolean }
        "400":
          description: Bozuk gövde ya da eksik alan
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Hata"
        "404":
          description: Görev müfredatta yok; kayıt yazılmaz
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Hata"
components:
  schemas:
    GorevDurumIstek:
      type: object
      required: [hafta, gun, gorevIndex, tamamlandi]
      properties:
        hafta: { type: integer, minimum: 1, maximum: 12 }
        gun: { type: integer, minimum: 1, maximum: 5 }
        gorevIndex: { type: integer, minimum: 0 }
        tamamlandi: { type: boolean }
    Hata:
      type: object
      required: [hata]
      properties:
        hata: { type: string }
        detay: { type: string }
        neden: { type: string }
```

Bu parçayı yazmaya kalktığın anda bir soru çıkıyor: `hafta`, `gun` ve `gorevIndex` gerçekten zorunlu mu,
aralıkları ne? Bu metin yazılırken kodda bu üç alan `ProgramController.GorevDurumIstek` içinde ilkel `int`
türündeydi. Gövdede yoklarsa Jackson onlara sessizce `0` veriyordu; sunucu da bu üçlünün müfredatta var
olup olmadığına bakmıyor, olmayan bir görev için de kayıt yazıyordu. Zarar küçüktü, çünkü ziyaretçi yalnız
kendi ilerlemesine yazar. Ama belgeyi yazmak bu boşluğu görünür kıldı ve boşluk aynı gün kapandı: alanlar
kutulu `Integer` oldu (eksikse 400), `IlerlemeService` de görevin müfredatta var olduğunu denetliyor
(yoksa 404, kayıt yazılmaz; MCP aracı da aynı yoldan geçer). İkisini de önce kırmızı yanan bir test
kanıtladı. Sözleşme önce yaklaşımın sessiz faydası budur: belgeyi yazmak, kodun hiç sormadığı soruları
sordurur.

### 3. Tutarlı hata modeli: RFC 9457 Problem Details
Başarılı yanıtlar çoğu zaman özenle tasarlanır; hatalar ise o gün akla gelen biçimde döner: bir uçta
`{"error": "..."}`, ötekinde `{"message": "..."}`. İstemci hata metnine bakarak dallanmaya başlar ve metin
değiştiği gün bozulur. Tutarlı bir hata modeli iki şey ister: **her hatanın aynı biçimde dönmesi** ve insana
gösterilen metinden ayrı, **makinenin dallanacağı kararlı bir kimlik**.

**RFC 9457** (Temmuz 2023, IETF standart yolu) bunun ortak biçimidir: "Problem Details for HTTP APIs",
yani HTTP API'leri için problem ayrıntıları. 2016 tarihli RFC 7807'nin yerini alır. Bir problem nesnesi beş
standart üye taşır:

- `type`: problem türünü tanımlayan URI. **Birincil kimlik budur**; istemci buna göre dallanır. Yoksa
  `"about:blank"` varsayılır, bu da "durum kodunun söylediğinden fazlası yok" demektir.
- `title`: problem türünün kısa, insan için özeti. Aynı türün her oluşumunda aynı kalmalıdır (yerelleştirme
  dışında).
- `status`: sunucunun ürettiği HTTP durum kodu. Yalnız bilgi amaçlıdır; belirtim, gerçek HTTP yanıtında da
  aynı kodun kullanılmasını şart koşar.
- `detail`: bu oluşuma özgü, istemcinin sorunu düzeltmesine yardım eden açıklama.
- `instance`: bu **belirli oluşumu** tanımlayan URI.

Bunların dışında probleme özgü **uzantı üyeleri** (extension members) eklenebilir; RFC'nin örneğinde bakiye
(`balance`) ve hesap listesi (`accounts`) böyle iki üyedir. Kural önemli: istemci, **tanımadığı uzantı
üyesini yok saymak zorundadır**. Bu kural, sözleşmeye yeni alan eklemeyi kırıcı olmaktan çıkarır (§5'teki
hoşgörülü okuyucu ilkesi). Medya türü (media type) `application/problem+json`'dur; XML karşılığı
`application/problem+xml`.

RFC 9457, 7807'ye üç şey ekler: sık kullanılan problem türleri için bir IANA kayıt defteri ("HTTP Problem
Types"), bir yanıtta birden çok problem varsa en ilgili ya da en acil olanı dönme önerisi ve çözülemeyen
`type` URI'leri için açıklama. Bu açıklamadan bir ders çıkar: `type` bir kimliktir ve sonradan değiştirmek
kırıcı bir değişikliktir; bu yüzden belirtim, ileride açıklama sayfası koyabileceğin çözülebilir
(`https://...`) URI'leri teşvik eder. RFC bir de güvenlik notu düşer: problem ayrıntıları bir hata ayıklama
aracı değildir; yığın dökümü (stack trace) gibi uygulama ayrıntıları dışarı sızdırılmaz.

Örnek (varsayımsal: depoda böyle bir yanıt yok, `type` adresindeki sayfa da yok):

```http
HTTP/1.1 422 Unprocessable Content
Content-Type: application/problem+json

{
  "type": "https://sokratai.tech/sorunlar/ilerleme-kodu-yok",
  "title": "Bu kodla kayıtlı ilerleme bulunamadı.",
  "status": 422,
  "detail": "Kod biçimce doğru, ama bu kodla kaydedilmiş bir ilerleme yok.",
  "hata": "Bu kodla kayıtlı ilerleme bulunamadı."
}
```

Sondaki `hata` bir uzantı üyesidir. Neden orada durduğunu aşağıdaki taşıma planı anlatıyor.

**SokratAI'nin hata sözleşmesi RFC 9457 değil.** `web/HataYonetimi` (bir `@RestControllerAdvice`) sınıf
yorumunda kendini "API hata sözleşmesi" diye tanımlar: tüm hatalar `{hata, detay}` JSON'uyla ve uygun HTTP
koduyla döner. Koddan okunan gerçek biçim biraz daha zengindir:

| Alan | Ne zaman | Görevi | RFC 9457'deki yakın karşılığı |
|---|---|---|---|
| `hata` | her hatada | kısa başlık ("Geçersiz istek", "Kayıt bulunamadı") | `title` |
| `detay` | çoğu hatada (422'de, hız sınırı 429'unda ve `ANAHTAR_GEREKLI` yanıtında bulunmaz) | kullanıcıya gösterilen açıklama | `detail` |
| `neden` | anahtar ve AI sağlayıcı hatalarında | kararlı makine kodu (`ANAHTAR_GEREKLI`, `YOGUN` gibi) | `type` |
| `referans` | beklenmeyen (son çare) 5xx hatalarında; AI kaynaklı 502/503'te yok | 8 karakterlik kısa kimlik; aynı değer sunucu günlüğüne de yazılır | uzantı üyesi ya da `instance` |
| yok | hiçbir hatada | durum kodu gövdede tekrarlanmaz | `status` |

Ön yüz bu sözleşmeye bağlıdır. `app.js`'teki `api()` işlevi hata yanıtında mesajı `govde.detay ||
govde.hata` ile seçer, `status` ve `neden` değerlerini çağırana taşır. Anahtar kartını yeniden açan dalın
yorumu açıkça "metne değil sunucunun kararlı koduna bakılır" der ve dallanma
`ANAHTAR_400_NEDENLERI = ["ANAHTAR_GEREKLI", "ANAHTAR_BICIMI"]` listesine göre yapılır. Yani depo, RFC
9457'nin asıl fikrini (makine kimliğini insan metninden ayırmak) kendi alan adlarıyla zaten uyguluyor.
Eksik olan, standart biçim ve **her** hatada kararlı bir kimlik.

Bir ayrıntı daha: Spring Framework 6.0'dan beri bu biçimi destekler (`ProblemDetail`, `ErrorResponse`; o gün
RFC 7807 adıyla, bugünkü belgeler 9457'yi anar);
Spring Boot'ta `spring.mvc.problemdetails.enabled=true` ile açılır. Depo Spring Boot 3.5 kullandığı için taşıma
yeni bağımlılık istemez. Bugün bu ayar kapalıdır ve `HataYonetimi`'nin son çare işleyicisi Spring'in kendi
istisnalarından yalnız durum kodunu alıp gövdeyi `{hata, detay}` biçiminde kurar; sözleşme tek biçimde kalır.

**Var olan bir hata sözleşmesi neden hafife değiştirilmez?** Çünkü ona dayanan her istemci de sözleşmenin bir
parçasıdır. `{hata, detay}`'ı tek adımda `{title, detail}` yaptığın gün
`app.js`'teki `govde.detay || govde.hata` boş döner ve kullanıcı her hatada yalnız "HTTP 400" gibi bir kod
görür. Üstelik
dağıtımdan (deployment) önce açılmış bir sekme, eski `app.js`'i bellekte tutarak yeni sunucuyla konuşmayı
sürdürür. Tek istemcili bir uygulamada bile "sunucuyu ve istemciyi aynı anda değiştiririm" varsayımı bu
yüzden tam doğru değildir. Doğru sıra **genişlet, geçir, daralt** (expand, migrate, contract) sırasıdır:

1. **Genişlet:** Sunucu eski alanları koruyarak yeni alanları **ekler**: `type`, `title`, `status`, `detail`.
   Geçiş süresince `hata`, `detay` ve `neden` uzantı üyesi olarak kalır. Eski istemci bozulmaz, çünkü
   tanımadığı alanları yok sayar.
2. **Geçir:** İstemci yeni alanları okumaya başlar, yoksa eskilerine geri düşer (`detail ?? detay ?? hata`).
   Dallanma `neden` yerine `type` üzerinden yapılır. Testler yeni alanları iddia eder.
3. **Daralt:** Hiçbir istemci eski alanları okumadığında (açık sekmelerin de yenilendiğinden emin olacak kadar
   zaman geçtiğinde) eski alanlar kaldırılır.

Medya türünü `application/problem+json` yapmak ayrı bir karardır. `app.js` gövdeyi `yanit.json()` ile içerik
türüne bakmadan ayrıştırdığı için bundan etkilenmez; ama `Content-Type: application/json` bekleyen katı bir
istemci etkilenebilir. Böyle bir taşıma kendi işi olarak yapılmalı ve kararı bir ADR'ye yazılmalı. Bir özellik
işinin içine "bu arada hata biçimini de modernleştirdim" diye sıkıştırılmamalı.

### 4. Sınırda doğrulama: 400 mü, 422 mi?
Hafta 9'un altıgen mimarisinde dış dünya çekirdeğe yalnız adaptörler üzerinden girer. Doğrulamanın en ucuz ve
en doğru yeri bu sınırdır: içeri giren her değer orada denetlenir, çekirdek geçersiz bir değerle hiç
karşılaşmaz. Sınırda iki tür denetim yapılır ve ikisinin cevabı farklı durum kodlarıdır.

RFC 9110 (HTTP Semantics, Haziran 2022) iki kodu şöyle tanımlar:

- **400 (Bad Request):** Sunucu, istemci hatası olarak algıladığı bir şey yüzünden isteği işleyemez ya da
  işlemez. RFC'nin örnekleri: bozuk istek sözdizimi, geçersiz mesaj çerçevelemesi, aldatıcı yönlendirme.
- **422 (Unprocessable Content):** Sunucu içerik türünü anlar (yani 415 uygun değildir), içeriğin sözdizimi
  doğrudur, ama içerdiği talimatları işleyemez. RFC'nin örneği: biçimce doğru ama anlamca hatalı bir XML.
  Bu kod WebDAV'dan gelir ve eski adı "Unprocessable Entity"dir; Spring'deki `HttpStatus.UNPROCESSABLE_ENTITY`
  sabiti bu eski adı taşır.

RFC, "hangi doğrulama hatası 400, hangisi 422" diye bir bölüşüm **yapmaz**. Yaygın gelenek şudur: istek
**biçim olarak** bozuksa ya da şemaya uymuyorsa (JSON ayrıştırılamıyor, zorunlu alan yok, tür yanlış,
uzunluk sınırı aşıldı) **400**; biçim tamam ama **anlam olarak** işlenemiyorsa, özellikle karar için kayıtlı
duruma ya da bir iş kuralına bakmak gerekiyorsa **422**. Kısa bir sınama: "Bu hatayı veritabanına hiç
bakmadan, yalnız isteğe bakarak bulabilir miydim?" Evetse 400, hayırsa 422. Bazı ekipler bütün doğrulama
hatalarına 400 der; bu da savunulabilir bir seçimdir. Önemli olan seçimin sözleşmede yazılı olması ve her
uçta aynı uygulanmasıdır.

Sık karışan bir ayrım daha var: **404 adresle ilgilidir.** `GET /api/hafta/99` adresin gösterdiği kaynak
olmadığı için 404 döner. Ama `POST /api/ziyaretci/geri-yukle` adresi vardır; bulunamayan şey **gövdedeki**
bir göndermedir (ilerleme kodu). Burada 404 dönmek istemciye "yanlış adrese geldin" demek olurdu. Bu yüzden
bu durumda çoğu zaman 422 seçilir; bu depo da öyle yapıyor.

**SokratAI'de bu ayrım uygulanmış:**

- `POST /api/ziyaretci/geri-yukle` (yalnız site modu): `ZiyaretciService.geriYukle` önce kodun **biçimine**
  bakar (`ZiyaretciCerezi.gecerliMi`). Biçimsizse `IllegalArgumentException` fırlatır ve `HataYonetimi` bunu
  **400** yapar. Biçim doğruysa veritabanına bakar; kayıt yoksa `IlerlemeKoduBulunamadiException` fırlatır ve
  `HataYonetimi` bunu **422** yapar. `ZiyaretciController`'ın yorumu kuralı tek satırda yazar: "Geçersiz biçim
  → 400; kayıtsız kod → 422". README'nin ziyaretçi tablosu aynı sözleşmeyi verir; `web/SiteIlerlemeKoduTest`'teki
  `bozukKod400` ve `olmayanKod422` testleri onu kilitler. Ön yüz de iki kodu farklı ele alır: 400'de "Bu bir
  ilerleme kodu değil." uyarısını, 422'de sunucunun mesajını gösterir.
- **Çekirdek HTTP bilmez:** `ZiyaretciService` yalnız istisna fırlatır; durum kodunu web katmanındaki
  `HataYonetimi` seçer. Hafta 9'daki, çekirdeğin dış dünyanın teknolojisini bilmemesi ilkesinin küçük ama
  gerçek bir örneği.
- **Girdi uzunluğu:** `web/GirdiSiniri` serbest metin girdilerine ortak bir üst sınır koyar
  (`MAX_GIRDI = 20_000` karakter). Boş ya da çok uzun girdi `IllegalArgumentException` olur ve 400 döner.
  Sınıf yorumu gerekçeyi yazar: dev bir yapıştırma bağlam sınırını aşıp anlamsız bir 500'e dönüşmesin. Bu, LLM
  çağıran uygulamalara özgü bir sınır denetimidir: korunan şey veritabanı değil, modelin bağlam bütçesidir.
- **Eksik alan sessizce varsayılana düşmesin:** `ProgramController.GorevDurumIstek`'te `tamamlandi` ilkel
  `boolean` değil, kutulu `Boolean`'dır. İlkel olsaydı, alan gövdede yokken Jackson `false` atar ve istek
  görevi sessizce **geri açardı**. Spring Boot'un varsayılan Jackson ayarı tanımadığı alanları da yok saydığı
  için, alan adını yanlış yazan bir istemci (`"tamamlanmis": true`) aynı sonuca düşerdi. Kutulu tür sayesinde
  alan `null` gelir ve controller 400 döner; test: `gorevDurumundaTamamlandiZorunlu_yoksa400`. Aynı desen AI
  çıktısında da var: `types/UretilenQuiz`'te doğru şık numarası kutulu `Integer`'dır ve `QuizUretimService` bu
  alanı boş ya da aralık dışı olan soruları eler. Modelin çıktısı da sınırdan giren bir girdidir.
- **Elle yazılmış denetim:** Depoda Bean Validation (`jakarta.validation`, `@Valid`) kullanılmıyor; bütün
  denetimler kodla yazılmış. §2'de gördüğün gibi `hafta`, `gun` ve `gorevIndex` için varlık denetimi bu
  metin yazılırken yoktu ve sonradan eklendi: eksik alan 400, müfredatta olmayan görev 404.

Ajan için bu ayrım bir davranış belirler: 400 "isteği düzelt, aynısını tekrar gönderme", 422 "istek doğru,
durum izin vermiyor; düzeltme çoğu zaman kullanıcıdan gelir" demektir. Hata gövdesi bunu açıkça söylerse
ajanın boşuna yeniden denemesi kesilir.

## Diyagram 2: Sınırda karar sırası: 400, 404, 422
Ucuz ve duruma bakmayan denetim önce, veritabanı gerektiren denetim sonra. Her sonucun yanında bu depodan
bir örnek var.

```svg
<svg viewBox="0 0 640 450" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Sınırda karar sırası: gövde okunamıyorsa 400, alanlar sözleşmeye uymuyorsa 400, adresteki kaynak yoksa 404, kayıtlı durum ya da iş kuralı izin vermiyorsa 422, hepsi geçerse işlem yapılır">
  <defs>
    <marker id="as-ok3" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#1C3247"/></marker>
    <marker id="as-ok4" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#BE4433"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Sınırda karar: 400 mü, 404 mü, 422 mi?</text>
  <text x="320" y="44" text-anchor="middle" font-size="11.5" fill="#5D7789" font-family="system-ui">Ucuz ve durumsuz denetim önce, veritabanı gerektiren denetim sonra</text>
  <rect x="95" y="56" width="170" height="28" rx="14" fill="#1C3247"/>
  <text x="180" y="75" text-anchor="middle" font-size="12" font-weight="700" fill="#FFFFFF" font-family="system-ui">İstek geldi</text>
  <line x1="180" y1="84" x2="180" y2="94" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok3)"/>
  <rect x="30" y="98" width="300" height="50" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="180" y="119" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">1. Gövde okunabiliyor mu?</text>
  <text x="180" y="137" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">geçerli JSON, doğru alan türleri</text>
  <line x1="330" y1="123" x2="376" y2="123" stroke="#BE4433" stroke-width="1.6" marker-end="url(#as-ok4)"/>
  <text x="353" y="116" text-anchor="middle" font-size="10" fill="#BE4433" font-family="system-ui">hayır</text>
  <rect x="380" y="101" width="240" height="44" rx="9" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.4"/>
  <text x="500" y="120" text-anchor="middle" font-size="12" font-weight="700" fill="#BE4433" font-family="system-ui">400 · bozuk istek</text>
  <text x="500" y="137" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">{bozuk json · /api/hafta/abc</text>
  <line x1="180" y1="148" x2="180" y2="164" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok3)"/>
  <text x="190" y="160" font-size="10" fill="#25804C" font-family="system-ui">evet</text>
  <rect x="30" y="168" width="300" height="50" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="180" y="189" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">2. Alanlar sözleşmeye uyuyor mu?</text>
  <text x="180" y="207" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">zorunlu alan, biçim, uzunluk (DB'ye bakmadan)</text>
  <line x1="330" y1="193" x2="376" y2="193" stroke="#BE4433" stroke-width="1.6" marker-end="url(#as-ok4)"/>
  <text x="353" y="186" text-anchor="middle" font-size="10" fill="#BE4433" font-family="system-ui">hayır</text>
  <rect x="380" y="171" width="240" height="44" rx="9" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.4"/>
  <text x="500" y="190" text-anchor="middle" font-size="12" font-weight="700" fill="#BE4433" font-family="system-ui">400 · geçersiz alan</text>
  <text x="500" y="207" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">tamamlandi yok · kod biçimsiz · çok uzun</text>
  <line x1="180" y1="218" x2="180" y2="234" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok3)"/>
  <text x="190" y="230" font-size="10" fill="#25804C" font-family="system-ui">evet</text>
  <rect x="30" y="238" width="300" height="50" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="180" y="259" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">3. Adresteki kaynak var mı?</text>
  <text x="180" y="277" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">URI'nin gösterdiği şey (ör. hafta 99)</text>
  <line x1="330" y1="263" x2="376" y2="263" stroke="#BE4433" stroke-width="1.6" marker-end="url(#as-ok4)"/>
  <text x="353" y="256" text-anchor="middle" font-size="10" fill="#BE4433" font-family="system-ui">hayır</text>
  <rect x="380" y="241" width="240" height="44" rx="9" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.4"/>
  <text x="500" y="260" text-anchor="middle" font-size="12" font-weight="700" fill="#C77A17" font-family="system-ui">404 · bulunamadı</text>
  <text x="500" y="277" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">GET /api/hafta/99</text>
  <line x1="180" y1="288" x2="180" y2="304" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok3)"/>
  <text x="190" y="300" font-size="10" fill="#25804C" font-family="system-ui">evet</text>
  <rect x="30" y="308" width="300" height="50" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="180" y="329" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">4. Kayıtlı durum ve iş kuralı uygun mu?</text>
  <text x="180" y="347" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">veritabanına ya da iş kuralına bakmak gerekir</text>
  <line x1="330" y1="333" x2="376" y2="333" stroke="#BE4433" stroke-width="1.6" marker-end="url(#as-ok4)"/>
  <text x="353" y="326" text-anchor="middle" font-size="10" fill="#BE4433" font-family="system-ui">hayır</text>
  <rect x="380" y="311" width="240" height="44" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.4"/>
  <text x="500" y="330" text-anchor="middle" font-size="12" font-weight="700" fill="#4F46E5" font-family="system-ui">422 · işlenemez içerik</text>
  <text x="500" y="347" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">geri-yukle: biçim doğru, kod kayıtsız</text>
  <line x1="180" y1="358" x2="180" y2="374" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok3)"/>
  <text x="190" y="370" font-size="10" fill="#25804C" font-family="system-ui">evet</text>
  <rect x="95" y="378" width="170" height="30" rx="9" fill="#25804C"/>
  <text x="180" y="398" text-anchor="middle" font-size="12" font-weight="700" fill="#FFFFFF" font-family="system-ui">2xx · işlem yapılır</text>
  <text x="320" y="436" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">400: isteği düzelt, aynısını tekrar gönderme · 422: istek doğru, durum izin vermiyor</text>
</svg>
```

### 5. Sürümleme: kırıcı değişiklik, sürüm taşıma, kullanımdan kaldırma
**Önce asıl soru: değişiklik kırıcı mı?** Sürümleme bir çözümdür; asıl iş kırıcı değişikliği tanımaktır.

*Genellikle kırıcı olmayanlar (eklemeli değişiklikler):* yeni bir uç, isteğe bağlı yeni bir istek alanı ya da
parametre, yanıta yeni bir alan. Bunların bir şartı var: istemciler **hoşgörülü okuyucu** (tolerant reader)
olmalı, yani tanımadıkları alanı yok saymalı. Zalando kılavuzu bunu istemciye bir yükümlülük olarak yazar
("uyumlu API genişletmelerini kabul etmeye hazır ol"); RFC 9457 de uzantı üyeleri için aynısını şart koşar.

*Kırıcı olanlar:* bir alanı silmek ya da adını değiştirmek (ad değiştirmek, silip yeniden eklemektir), türünü
değiştirmek, isteğe yeni bir zorunlu alan eklemek, bir hatanın durum kodunu ya da makine kodunu değiştirmek, bir
ucu kaldırmak. Daha sinsi iki tür daha var: **varsayılan değeri değiştirmek** ve **anlamı değiştirmek**.
Google'ın AIP-180 kılavuzu ikisini de kırıcı sayar ve bir uyarı ekler: yanıtta dönen sabit değerli bir listeye
(enum) yeni değer eklemek bile o değeri tanımayan istemciyi bozabilir.

Anlam değişikliğinde şema değişmez. Eski `POST /api/gorev/toggle` ile bugünkü `POST /api/gorev/durum` hemen
hemen aynı alanları taşır; aradaki büyük fark **ne yaptıklarıdır** (§6). Hiçbir şema karşılaştırma aracı bunu
yakalayamaz.

**Sürümü nerede taşırsın?** Üç yaygın yer var ve büyük kılavuzlar burada anlaşamıyor:

- **Adreste (URI):** `/v1/gorevler`. Google'ın AIP-185 kılavuzu ana sürümü yolun başına koyar (`v1`), ara ve
  yama sürümlerini dışarı göstermez; alfa ve beta için `v1alpha`, `v1beta` gibi kanallar kullanır. Görünürdür
  ve tarayıcıda kolayca denenir.
- **Sorgu parametresinde:** Microsoft'un Azure REST kılavuzu her istekte zorunlu bir `api-version` sorgu
  parametresi ister. Değer bir tarihtir (`2026-11-28`; önizleme sürümleri için `-preview` eki). Sürüm
  verilmezse 400 döner.
- **Medya türünde:** Zalando kılavuzu önce "sürümlemekten kaçın" der (kural 113). Gerekirse medya türü
  sürümlemesini ister (kural 114) ve adreste sürüm taşımayı açıkça yasaklar (kural 115).

Kural aynıdır: bir yöntem seç, sözleşmeye yaz, her uçta aynı uygula. Yeni ana sürüm de bedelsiz değildir: iki
sürümü birlikte çalıştırır, test eder ve yamalarsın.

**Kullanımdan kaldırma (deprecation): istemciye zaman ve sinyal ver.** Bir ucu kaldırmadan önce bunu
istemciye makinenin okuyacağı biçimde söylemek için iki HTTP başlığı var:

- **`Deprecation`** (RFC 9745, Mart 2025, standart yolu): kaynağın kullanımdan kaldırıldığı ya da
  kaldırılacağı anı, yapılandırılmış alan (Structured Field) tarih biçimiyle verir: `@` işaretinin ardından
  Unix zaman damgası. Aynı RFC, kaldırma hakkında insan için yazılmış belgeye işaret eden `deprecation`
  bağlantı ilişkisini (link relation) de tanımlar.
- **`Sunset`** (RFC 8594, Mayıs 2019): adresin hangi andan sonra yanıt vermeyeceğinin beklendiğini HTTP tarih
  biçimiyle verir. Dikkat: RFC 8594 **bilgilendirme amaçlı** (Informational) bir RFC'dir, standart yolunda
  değildir. RFC 9745'e göre `Sunset` tarihi `Deprecation` tarihinden önce olamaz; iki başlığın tarih
  biçimlerinin farklı olması tarihsel bir kalıntıdır.

Örnek: birden çok istemcisi olan bir dünyada, eski toggle ucu kaldırılmadan önce şöyle işaretlenebilirdi.
Bu başlıklar depoda yok.

```http
HTTP/1.1 200 OK
Deprecation: @1798761599
Sunset: Wed, 30 Jun 2027 23:59:59 GMT
Link: </belgeler/api-degisiklikleri>; rel="deprecation"; type="text/html"
```

`@1798761599`, 31 Aralık 2026 23:59:59 UTC anıdır. OpenAPI'de aynı bilgi işlem nesnesindeki `deprecated: true`
ile yazılır. Zalando'nun süreci bunları birlikte ister: belgede göster, başlıkları ekle, eski ucun kullanımını
**izle**, istemciler onay vermeden kapatma.

**SokratAI'de sürüm yok.** Bütün JSON API uçları sürümsüz `/api/...` altındadır (JS'siz ders sayfaları
`/dersler`, `/hafta/N` gibi ayrı yollardadır): `ProgramController`,
`OgretmenController`, `OzellikController` gibi sınıflar `@RequestMapping("/api")`, `ZiyaretciController`
`"/api/ziyaretci"`, atölye controller'ları `"/api/atolye"`, mülakat `"/api/mulakat"` kullanır; kodda `/v1`
yok. Bugün bu doğru bir karar, çünkü HTTP API'nin dışarıdan bir istemcisi yok: ön yüz aynı uygulamayla
birlikte gelir, MCP araçları da HTTP API'yi değil servisleri doğrudan çağırır (`OgretmenMcpAraclari` →
`IlerlemeService.gorevDurumuAyarla`). Toggle ucunun kaldırılışı da bu yüzden kısa sürdü: ADR-0016 ile
`POST /api/gorev/toggle` kaldırıldı, `app.js` yeni uca geçirildi ve `ProgramControllerTest.eskiToggleUcuKaldirildi`
eski adresin artık 4xx döndüğünü kilitledi. Ne `Deprecation` başlığı ne de bekleme süresi gerekti.

MCP tarafında karar farklı verildi: `gorev_isaretle` aracının adı korundu, anlamı "çevir"den "ayarla"ya
geçti. Bunun bir kuralı neden bilerek esnettiği [MCP adapter](mcp-adapter.md) §7.1'de tartışılıyor. Kısa
hâli: o gün uzakta çalışan bir MCP sunucusu ve onun dış istemcisi yoktu.

Sürüm ne zaman gerekir? İkinci bir istemci türü (mobil uygulama, iş ortağı, sözleşmeyi okuyup HTTP'yi
doğrudan çağıran bir ajan) ortaya çıktığında; MCP sunucusu yeniden uzakta çalıştırılmaya başlandığında
(ADR-0007; uzak sunucular 2026-10-21'de kapatıldı); ya da API herkese açık bir ürün olduğunda. O noktada ilk
iş README tablolarını bir OpenAPI belgesine çevirip seçilen sürüm yöntemini oraya yazmak, ikinci iş kaldırma
sürecini (başlık, izleme, süre) tanımlamak olur.

### 6. İdempotentlik: tekrar denenen istek zarar vermemeli
RFC 9110 iki kavramı ayırır:

- **Güvenli yöntem** (safe method): tanımlı anlamı esasen salt okumadır; istemci sunucuda bir durum değişikliği
  istemez ve beklemez. GET, HEAD, OPTIONS ve TRACE güvenlidir.
- **İdempotent yöntem:** aynı isteğin birden çok kez gönderilmesinin sunucudaki **amaçlanan etkisi**, bir kez
  gönderilmesinin etkisiyle aynıdır. PUT, DELETE ve güvenli yöntemler idempotenttir. POST ve PATCH bu tanımın
  dışındadır.

Ayrımın pratik nedeni RFC'de yazılı: idempotent bir istekte, yanıt okunmadan bağlantı koparsa istek
**otomatik olarak** tekrarlanabilir, çünkü ilk istek başarılı olmuş olsa bile tekrar aynı amaçlanan etkiyi
bırakır. Tersi de bir kuraldır: istemci, isteğin anlamının gerçekten idempotent olduğunu bilmiyorsa ya da ilk
isteğin hiç uygulanmadığını anlayacak bir yolu yoksa, idempotent olmayan bir isteği otomatik olarak tekrar
**etmemelidir**. RFC, istemcinin bir POST'u "tasarımdan ya da yapılandırmadan" güvenli olduğunu biliyorsa
tekrarlayabileceğini de söyler. O bilgiyi istemciye taşımak sözleşmenin işidir.

**Neden bu kadar önemli?** Çünkü zaman aşımı (timeout) isteğin başarısız olduğunu değil, yalnız yanıtın
gelmediğini söyler; sunucu işi yapmış olabilir. SokratAI'de `app.js`'teki `api()` işlevi isteği varsayılan
olarak 180 saniyede keser ve kullanıcıya "İstek zaman aşımına uğradı. Tekrar dene." der. Ajan bu riski
büyütür: insan butona iki kez basar, ajan ise **kendi kendine** yeniden dener (retry).

İdempotent olmayan bir işlemi tekrara dayanıklı kılmanın üç yolu var:

**(a) İşlemi "hedef durumu ayarla" diye tasarla.** "Çevir" (toggle), "bir artır", "sona ekle" gibi göreli
komutlar doğası gereği idempotent değildir. "Şu duruma getir", "değeri 5 yap" gibi mutlak komutlar ise
idempotenttir: N kez çalıştırmak bir kez çalıştırmakla aynı sonucu bırakır. HTTP'de bunun doğal yöntemi
PUT'tur.

**(b) Doğal ya da ikincil anahtar kullan.** Kaydın kendi iş kimliği varsa (aynı kullanıcı, aynı hafta, aynı
gün, aynı görev), veritabanındaki tekil bir indeks çift kaydı engeller. Zalando kılavuzu idempotent POST
tasarımı için ikincil anahtar kullanmayı önerir (kural 231).

**(c) İstemcinin ürettiği bir idempotentlik anahtarı taşı.** İstemci her **iş niyeti** için rastgele bir değer
üretir ve tekrar denemelerde aynı değeri gönderir. Sunucu anahtarı ve sonucu saklar; aynı anahtarla gelen
tekrara aynı sonucu döner. IETF'nin HTTPAPI çalışma grubunda bunun için bir başlık tanımlanıyor:
`Idempotency-Key`. **Durumunu dikkatle oku:** bu bir RFC değil, bir İnternet Taslağıdır (Internet-Draft).
28 Kasım 2026 itibarıyla son sürüm `draft-ietf-httpapi-idempotency-key-header-09`'dur (21 Kasım 2026).
Hedeflenen statü standart yoludur, ama taslak henüz IESG değerlendirmesine gelmemiştir (IESG durumu:
"I-D Exists") ve değişebilir. Bugünkü metne göre değer UUID gibi rastgele bir dizedir
(`Idempotency-Key: "8e03978e-40d5-43e8-bc93-6894a57f9324"`). Hata durumları: anahtarı zorunlu tutan bir uçta
anahtar yoksa **400**; aynı anahtar farklı bir gövdeyle gelirse ve sunucu gövdenin parmak izini (fingerprint)
tutuyorsa **422**; ilk istek hâlâ işlenirken aynı anahtarla ikinci istek gelirse **409**. Tek seçenek de bu
değil: Azure kılavuzu POST için OASIS'in "Repeatable Requests" belirtimini önerir, Zalando ise
`Idempotency-Key` desteğini seçenek olarak bırakır (kural 230, "MAY"). Anahtarı, parmak izini ve asıl etkiyi
aynı transaction (işlem bütünlüğü) içinde saklamanın ayrıntısı
[araç çağrısı güvenilirliği](arac-cagrisi-guvenilirligi.md) §2'de.

**SokratAI vakası: toggle'dan hedef duruma (ADR-0016).** 2026-10-26'ya kadar görev işaretleme bir toggle'dı:
`POST /api/gorev/toggle` ve MCP aracı `gorev_isaretle` görevin durumunu tersine çeviriyordu. Zaman aşımından
sonra tekrarlanan çağrı tamamlanmış görevi geri açıyor, eşzamanlı iki çağrıda bir güncelleme kayboluyordu
(`docs/tech-debt-tracker.md`, "Kapanan borçlar" #9). Karar (a) yolunu seçti:
`IlerlemeService.gorevDurumuAyarla` hedef durumu açık bir argüman olarak alır. Kayıt yoksa o durumda oluşturur,
varsa durumu yazar, **zaten hedefteyse hiç yazmaz** (tamamlanma zamanı tekrarla ezilmesin diye). (b) yolu da
devrededir: `schema.sql`'deki `ux_gorev_ilerleme_kullanici` tekil indeksi aynı görev için ikinci satırı
engeller. Eski kodun satır satır davranışı, eşzamanlı çağrılar ve düzeltmenin testleri
[araç çağrısı güvenilirliği](arac-cagrisi-guvenilirligi.md) §8'de. Burada aynı vakaya **sözleşme** gözüyle
bakalım:

1. **Hedef durum sözleşmede zorunlu.** HTTP ucunda `tamamlandi` zorunludur, yoksa 400 döner. Varsayılan
   seçilmez, çünkü "hangi duruma?" sorusunun sessiz cevabı tam da toggle'ın yaptığı türden bir sürprizdir.
   Testteki yorum bunu söyler: "hedef durum belirtilmezse sessizce varsayılan seçilmez".
2. **İki kapı, iki varsayılan.** Aynı işlem MCP aracında farklı bir sözleşmeyle açılır: `hedef_durum`
   isteğe bağlıdır, verilmezse `true` olur; eski üç argümanlı çağrılar geçerli kalsın diye. İki kapının farklı
   olması sorun değildir; sorun, farkın yazılmamış olması olurdu. Burada araç açıklaması ve README ikisini ayrı
   ayrı yazıyor.
3. **Yöntem POST, anlam idempotent.** `POST /api/gorev/durum`, RFC 9110'a göre idempotent olmayan bir
   yöntemle çağrılır; sunucudaki davranış ise idempotenttir. Protokolü okuyan genel bir HTTP istemcisi ya da
   vekil (proxy) bunu bilemez; bilgi yalnız README satırında ("idempotent; tekrar denemek sonucu
   değiştirmez") ve kod yorumundadır. RFC'nin "tasarımdan bilinen" POST tekrarı tam olarak budur: güvence
   protokolde değil, sözleşme metnindedir. Alternatif, idempotentliği protokolün kendisinde söyleyen bir PUT olurdu; örneğin
   gövdesi `{"tamamlandi": true}` olan `PUT /api/gorev/{hafta}/{gun}/{gorevIndex}`. Bugün buna geçmek,
   kazancı ölçülmemiş bir kırıcı değişiklik olurdu. İkinci bir istemci geldiğinde OpenAPI belgesine yazılacak
   ilk notlardan biri budur.
4. **Kabul edilmiş risk, yazılı koşulla.** ADR-0016'nın ikinci kararı, quiz sonucunu kaydetmeyi bilerek
   idempotent yapmadı: `quiz_sonuc` her denemeyi bir satır olarak tutan, yalnız eklenen bir günlüktür ve
   tekrarlanan bir MCP çağrısı bir fazla satır üretir. Önlemek bir idempotentlik anahtarı kolonu ve araç
   sözleşmesi değişikliği ister; ADR bunu kazancından pahalı bulur ve **yeniden açma koşulunu** yazar: MCP
   sunucusu yeniden uzakta çalıştırılırsa ya da quiz denemesi bir değerlendirmeye veya sıralamaya girerse.
   Aynı tablo `POST /api/quiz/statik-sonuc` ucunun da arkasındadır (`IlerlemeService.statikQuizKaydet` her
   çağrıda yeni satır ekler). (c) yolu tam olarak bu tür bir uç için vardır.

## Diyagram 3: Tekrar denenen istek: çevir ve ayarla
Aynı senaryo, iki sözleşme: ilk yanıt yolda kaybolur, istemci zaman aşımından sonra aynı isteği tekrar
gönderir. Solda eski toggle, sağda bugünkü hedef durum.

```svg
<svg viewBox="0 0 640 410" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Aynı istek zaman aşımından sonra tekrarlanınca toggle görevi geri açar; hedef durumu ayarlayan uç ise kaydı zaten hedefte bulur, yazmaz ve görev tamam kalır">
  <defs>
    <marker id="as-ok5" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#1C3247"/></marker>
    <marker id="as-ok6" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#BE4433"/></marker>
    <marker id="as-ok7" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#25804C"/></marker>
  </defs>
  <text x="320" y="22" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Tekrar denenen istek: çevir (toggle) ve ayarla</text>
  <rect x="10" y="34" width="305" height="302" rx="12" fill="#FFFFFF" stroke="#BE4433" stroke-width="1.5"/>
  <rect x="325" y="34" width="305" height="302" rx="12" fill="#FFFFFF" stroke="#25804C" stroke-width="1.5"/>
  <text x="162" y="54" text-anchor="middle" font-size="12" font-weight="700" fill="#BE4433" font-family="system-ui">Eski: POST /api/gorev/toggle</text>
  <text x="477" y="54" text-anchor="middle" font-size="12" font-weight="700" fill="#25804C" font-family="system-ui">Yeni: POST /api/gorev/durum</text>
  <g font-family="system-ui">
    <rect x="20" y="66" width="60" height="24" rx="6" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
    <text x="50" y="82" text-anchor="middle" font-size="11" font-weight="600" fill="#1C3247">İstemci</text>
    <rect x="215" y="66" width="80" height="24" rx="6" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
    <text x="255" y="82" text-anchor="middle" font-size="11" font-weight="600" fill="#1C3247">Sunucu</text>
    <line x1="50" y1="90" x2="50" y2="278" stroke="#5D7789" stroke-width="1" stroke-dasharray="4 3"/>
    <line x1="255" y1="90" x2="255" y2="278" stroke="#5D7789" stroke-width="1" stroke-dasharray="4 3"/>
    <line x1="50" y1="112" x2="201" y2="112" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok5)"/>
    <text x="125" y="106" text-anchor="middle" font-size="10.5" fill="#1C3247">1. istek</text>
    <rect x="205" y="99" width="100" height="26" rx="6" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
    <text x="255" y="116" text-anchor="middle" font-size="10.5" fill="#1C3247">yok → true</text>
    <line x1="205" y1="148" x2="122" y2="148" stroke="#BE4433" stroke-width="1.4" stroke-dasharray="5 3"/>
    <line x1="106" y1="142" x2="118" y2="154" stroke="#BE4433" stroke-width="2"/>
    <line x1="106" y1="154" x2="118" y2="142" stroke="#BE4433" stroke-width="2"/>
    <text x="160" y="141" text-anchor="middle" font-size="10" fill="#BE4433">2. yanıt kayboldu</text>
    <text x="128" y="180" text-anchor="middle" font-size="10" fill="#C77A17">3. zaman aşımı: tekrar dene</text>
    <line x1="50" y1="208" x2="201" y2="208" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok5)"/>
    <text x="125" y="202" text-anchor="middle" font-size="10.5" fill="#1C3247">4. aynı istek</text>
    <rect x="205" y="193" width="100" height="30" rx="6" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.4"/>
    <text x="255" y="212" text-anchor="middle" font-size="10.5" font-weight="700" fill="#BE4433">true → false</text>
    <line x1="205" y1="250" x2="54" y2="250" stroke="#BE4433" stroke-width="1.6" marker-end="url(#as-ok6)"/>
    <text x="125" y="244" text-anchor="middle" font-size="10.5" fill="#BE4433">5. yanıt: false</text>
    <rect x="24" y="290" width="277" height="38" rx="9" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.4"/>
    <text x="162" y="306" text-anchor="middle" font-size="11.5" font-weight="700" fill="#BE4433">Sonuç: görev geri açıldı</text>
    <text x="162" y="321" text-anchor="middle" font-size="10" fill="#5D7789">iki istek, iki çevirme</text>
  </g>
  <g font-family="system-ui">
    <rect x="335" y="66" width="60" height="24" rx="6" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
    <text x="365" y="82" text-anchor="middle" font-size="11" font-weight="600" fill="#1C3247">İstemci</text>
    <rect x="530" y="66" width="80" height="24" rx="6" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
    <text x="570" y="82" text-anchor="middle" font-size="11" font-weight="600" fill="#1C3247">Sunucu</text>
    <line x1="365" y1="90" x2="365" y2="278" stroke="#5D7789" stroke-width="1" stroke-dasharray="4 3"/>
    <line x1="570" y1="90" x2="570" y2="278" stroke="#5D7789" stroke-width="1" stroke-dasharray="4 3"/>
    <line x1="365" y1="112" x2="516" y2="112" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok5)"/>
    <text x="440" y="106" text-anchor="middle" font-size="10.5" fill="#1C3247">1. istek</text>
    <rect x="520" y="99" width="100" height="26" rx="6" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
    <text x="570" y="116" text-anchor="middle" font-size="10.5" fill="#1C3247">yok → true</text>
    <line x1="520" y1="148" x2="437" y2="148" stroke="#BE4433" stroke-width="1.4" stroke-dasharray="5 3"/>
    <line x1="421" y1="142" x2="433" y2="154" stroke="#BE4433" stroke-width="2"/>
    <line x1="421" y1="154" x2="433" y2="142" stroke="#BE4433" stroke-width="2"/>
    <text x="475" y="141" text-anchor="middle" font-size="10" fill="#BE4433">2. yanıt kayboldu</text>
    <text x="443" y="180" text-anchor="middle" font-size="10" fill="#C77A17">3. zaman aşımı: tekrar dene</text>
    <line x1="365" y1="208" x2="516" y2="208" stroke="#1C3247" stroke-width="1.6" marker-end="url(#as-ok5)"/>
    <text x="440" y="202" text-anchor="middle" font-size="10.5" fill="#1C3247">4. aynı istek</text>
    <rect x="520" y="190" width="100" height="36" rx="6" fill="#E9F4EE" stroke="#25804C" stroke-width="1.4"/>
    <text x="570" y="205" text-anchor="middle" font-size="10" font-weight="700" fill="#25804C">zaten true</text>
    <text x="570" y="219" text-anchor="middle" font-size="10" fill="#25804C">yazma yok</text>
    <line x1="520" y1="250" x2="369" y2="250" stroke="#25804C" stroke-width="1.6" marker-end="url(#as-ok7)"/>
    <text x="440" y="244" text-anchor="middle" font-size="10.5" fill="#25804C">5. yanıt: true</text>
    <rect x="339" y="290" width="277" height="38" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.4"/>
    <text x="477" y="306" text-anchor="middle" font-size="11.5" font-weight="700" fill="#25804C">Sonuç: görev tamam kaldı</text>
    <text x="477" y="321" text-anchor="middle" font-size="10" fill="#5D7789">N istek, tek isteğin etkisi</text>
  </g>
  <text x="320" y="362" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">Şema neredeyse aynı: {hafta, gun, gorevIndex}. Fark anlamda; bunu şema denetimi yakalamaz.</text>
  <text x="320" y="384" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Yeni hâl: ADR-0016, IlerlemeService.gorevDurumuAyarla, tamamlandi zorunlu</text>
</svg>
```

### 7. Ajanla çalışırken: sözleşmeyi görev tarifine bağla
Bu metnin dört konusu, ajana iş verirken görev tarifinin içine yerleşir. Hafta 3, Gün 1'in kalıbıyla
(hedef, bağlam, kısıtlar, bitiş kriteri) SokratAI'de varsayımsal bir iş: "Hafta yanıtındaki her göreve
tamamlanma zamanını ekle."

- **Hedef:** `GET /api/hafta/{no}` yanıtındaki her görev, tamamlandıysa tamamlanma zamanını da taşısın.
- **Bağlam:** Sözleşme README'nin "Program ve ilerleme" tablosunda. Hata biçimi `web/HataYonetimi`'nde
  (`{hata, detay}`, RFC 9457 değil). Tek istemci `static/app.js`. Zaman bilgisi `types/GorevIlerlemeKaydi`'nin
  `tarih` alanında duruyor.
- **Kısıtlar:** Yeni alan isteğe bağlı ve eklemeli olacak; var olan hiçbir alanın adı, türü ya da anlamı
  değişmeyecek. Hata biçimine dokunulmayacak. `POST /api/gorev/durum`'un anlamı (hedef durum, zaten hedefteyse
  yazma yok) korunacak, böylece `tarih` tekrar eden isteklerle ezilmeyecek. **Var olan bir sözleşme testini
  değiştirmen gerekirse dur ve sor.**
- **Bitiş kriteri:** `./mvnw verify` yeşil. `ProgramControllerTest`'e yeni alanı iddia eden bir test eklendi ve
  eski alanları iddia eden testler hiç değiştirilmeden geçiyor. README tablosu güncel. (Bir OpenAPI belgesi
  olsaydı bir madde daha: "üretilen belge ile depodaki belge arasındaki fark yalnız ekleme".)

Kısıtlardaki kalın cümle, ajanların bilinen bir kısa yoluna karşı konmuştur: test kırmızıysa testi
değiştirmek. Sözleşmeyi kilitleyen bir testi değiştirmek gerekiyorsa sözleşme değişiyor demektir. Bu da bir
uygulama ayrıntısı değil, §5'teki kırıcı değişiklik sorusudur ve kararı insan verir.

Genel kurallar: sözleşmenin **dosya yolunu** ajanın bağlamına koy ("API'ye bir bak" deme); hata biçimini ve
400/422 geleneğini bir kez yaz; kırıcı değişikliği ajan önerir, insan bir ADR ile karar verir; tekrar
denenebilecek her yazma ucunun idempotentlik notu (hedef durum, doğal anahtar, idempotentlik anahtarı ya da
gerekçesi yazılı kabul edilmiş risk) sözleşmede durur.

### 8. ⚠️ ZORUNLU ŞERH: "Spesifikasyonu yaz, API'yi yapay zekâ yazsın" ve araç vaatleri
Sözleşme önce yaklaşımın çevresinde, ajanlarla birlikte büyüyen bir vaat dili var: "OpenAPI belgeni ver,
ajan bütün arka ucu yazsın", "platformumuzla API'n otomatik olarak ajana hazır olur", "her uçtan bir MCP
aracı üretiriz", "bu araçla kırıcı değişiklik imkânsız". Bunlara şerh:

1. **Şema biçimi anlatır, davranışı anlatmaz.** Bir OpenAPI belgesi alanları, türleri ve durum kodlarını
   söyler. Şunları ise ya hiç söyleyemez ya da yalnız düz yazıyla söyleyebilir: işlemin idempotent olup
   olmadığı, eşzamanlı çağrılarda ne olacağı, yan etkiler, bir kaydın **kime ait olduğu** ve sahiplik
   denetimi. Toggle ile hedef durum neredeyse aynı şemayla yazılır (Diyagram 3). Belgeden "bütün API'yi"
   üreten bir ajan şemayı kusursuz karşılayıp anlamı yanlış kurabilir. Üretilen kod derleniyor ve şema
   doğrulayıcısından geçiyorsa bu yalnız **biçimin** doğru olduğunu kanıtlar.
2. **Fark araçları yapıyı görür, anlamı görmez.** OpenAPI karşılaştırma araçları alan silinmesini, tür
   değişikliğini ve yeni zorunlu alanı yakalar; bu değerlidir. Belgede yazılı olmayan bir varsayılan değer
   değişikliğini ve anlam değişikliğini ise yakalayamaz. AIP-180 de anlam değişikliğinde yargı gerektiğini
   kabul eder. "Kırıcı değişiklik imkânsız" vaadi, yapısal değişikliklerle sınırlıdır.
3. **"Her uçtan bir araç" ajan için iyi bir arayüz değildir.** Ajan aracı adından, açıklamasından ve
   şemasından seçer; araç tasarımı aynı zamanda prompt tasarımıdır ([MCP adapter](mcp-adapter.md) §7). Hangi
   işlemin ajana açılacağı ve hangisinin insan onayına bağlanacağı bir tasarım kararıdır, bir dönüştürme
   adımı değil.
4. **Yalnız sağlayıcının belgesine karşı test, entegrasyonu kanıtlamaz.** Pact'in kendi belgeleri bunu
   açıkça yazar: sağlayıcıyı kendi OpenAPI belgesine karşı test etmek, tüketicilerin sağlayıcıyı doğru
   çağırdığına dair güvence vermez.
5. **"Standart" kelimesine bak.** `Idempotency-Key` bugün bir İnternet Taslağıdır, RFC değil. "Standart
   Idempotency-Key başlığını destekliyoruz" diyen bir ürün, taslağın hangi sürümünü uyguladığını söylemelidir.
   `Sunset` (RFC 8594) bilgilendirme amaçlı bir RFC'dir; `Deprecation` (RFC 9745) ve Problem Details
   (RFC 9457) standart yolundadır. Bir aracın "RFC 7807 uyumlu" olması bugün eski bir belgeye atıftır; 9457
   onun yerini aldı.
6. **Rakamlı vaat yöntem ister.** "Entegrasyon hatalarını yüzde şu kadar azaltır" türünden iddialar için bu
   metin bilerek hiçbir rakam aktarmıyor. Böyle bir rakam gördüğünde sor: neye göre, kaç ekipte, kim ölçtü,
   karşılaştırma grubu ne?

Dürüst özet: sözleşme önce yaklaşım ajanla çalışırken **daha** değerlidir, ama sihirli değildir. Belge, ajanın
uyduracağı alanı daraltır. Anlamı, sahipliği ve tekrar davranışını ise yine sen yazarsın ve testle kilitlersin.

### Bu programdaki ilgili kaynaklar
- Aynı dizinin kardeş metinleri (AI çağında teslimat döngüsü): [`test-kalitesi-ai-cagi.md`](test-kalitesi-ai-cagi.md), [`tehdit-modelleme-stride.md`](tehdit-modelleme-stride.md), [`teslimat-hatti-gozlemlenebilirlik.md`](teslimat-hatti-gozlemlenebilirlik.md).
- [API Tasarımı: REST ve gRPC](api-tasarim-grpc.md): kaynak modeli, HTTP fiilleri, REST ile gRPC
  karşılaştırması. Bu metin onun üstüne kurulur.
- [Araç Çağrısı Güvenilirliği](arac-cagrisi-guvenilirligi.md): idempotentlik anahtarı, outbox, yeniden deneme
  sınıfları ve `gorev_isaretle` laboratuvarı (§2, §3, §8).
- [MCP: Uygulamanı Bir AI Ajanına Araç Olarak Aç](mcp-adapter.md): araç arayüzü, araç hata sözleşmesi ve araç
  adını sürümleme (§7, §7.1).
- [Hafta 9 Görsel Özet: Altıgen ve Temiz Mimari](hafta09-hexagonal-clean.md): port ve adaptör, sınırda veri
  eşleme.
- [Hafta 6 Görsel Özet: Mimari Stiller ve C4](hafta06-mimari-stiller-c4.md): parçaların birbirine bağlandığı
  stiller.
- [Hafta 11 Görsel Özet: DDIA](hafta11-ddia-veri-yogun.md): kayıp güncelleme ve eşzamanlılık.
- [LLM Uygulama Güvenliği](llm-uygulama-guvenligi.md): model çıktısı da güvenilmeyen bir girdidir.
- Dersler: Hafta 3, Gün 1 (görev tarifi ve mekanik bitiş kriteri); Hafta 9, Gün 2 (port ve adapter'lar) ve
  Gün 3 (sınırda eşleme stratejileri).

## Öz-sınav (kendini test et)
1. Kod önce ve sözleşme önce yaklaşımların asıl farkını "doğruluk kaynağı" kavramıyla anlat. Hangisinde belge
   kodun aynasıdır?
2. Hafta 3, Gün 1'deki "mevcut API yanıtına alan eklenir, var olan alan silinmez" kısıtı neden yumuşak bir
   kısıttır? Makinenin okuduğu bir sözleşme onu nasıl mekanik bir bitiş kriterine çevirir?
3. OpenAPI 3.1 belgesinde hangi kök alanlar zorunludur? `paths` neden artık zorunlu değil, yerine hangi kural
   geldi?
4. 3.0'daki `nullable: true` 3.1'de nasıl yazılır? Bu değişiklik hangi büyük hizalanmanın sonucudur?
5. RFC 9457'nin beş standart üyesini say. İstemci hangisine göre dallanmalı; neden `title` ya da `detail`'e göre
   değil?
6. SokratAI'nin `{hata, detay}` sözleşmesini RFC 9457'ye taşıyacaksın. Neden tek adımda yeniden adlandırmazsın?
   "Genişlet, geçir, daralt" adımlarını bu depoya uygula; `app.js`'te hangi ifade etkilenir?
7. `POST /api/ziyaretci/geri-yukle` iki farklı hatada iki farklı kod döner. Hangi kodlar, neden? Kayıtsız kodda
   neden 404 dönülmez?
8. `GorevDurumIstek`'te `tamamlandi` neden kutulu `Boolean`? İlkel `boolean` olsaydı ve istemci alan adını yanlış
   yazsaydı ne olurdu?
9. Hangileri kırıcı değişikliktir: (a) yanıta isteğe bağlı bir alan eklemek, (b) bir alanın varsayılan değerini
   değiştirmek, (c) aynı ad ve şemayla bir ucun anlamını "çevir"den "ayarla"ya geçirmek, (d) yanıttaki bir
   enum'a yeni değer eklemek? Hangisini şema fark aracı hiç yakalayamaz?
10. `Deprecation` ve `Sunset` başlıklarının RFC numaralarını ve statülerini söyle. Aralarındaki tarih kuralı
    nedir? Toggle ucu kaldırılırken bu depoda neden kullanılmadılar?
11. RFC 9110'a göre hangi yöntemler güvenli, hangileri idempotenttir? İstemci bir POST'u ne zaman otomatik
    tekrarlayabilir? `POST /api/gorev/durum` bu açıdan nerede durur?
12. `Idempotency-Key` başlığının bugünkü durumu nedir? Taslağa göre eksik anahtar, farklı gövdeyle gelen aynı
    anahtar ve hâlâ işlenen bir isteğin anahtarıyla gelen ikinci istek hangi kodları alır? SokratAI'de bu yönteme
    en çok aday uç hangisi ve ADR-0016 onu neden ertelemiş?
13. SokratAI'de neden `/v1` yok? Hangi gelişmeler sürüm taşımayı gerekli kılar?
14. **Şerh sorusu:** Bir satıcı diyor ki: "OpenAPI belgeni ver, ajan bütün API'yi yazsın; fark aracımız kırıcı
    değişikliği imkânsız kılar." Bu cümledeki iki iddiayı toggle vakasıyla çürüt.

### Cevap anahtarı (önce kendin cevapla, sonra bak)
1. Kod önce yaklaşımda doğru olan koddur; belge koddan üretilir ve onun aynasıdır. Sözleşme önce yaklaşımda
   doğru olan belgedir; kod, istemci tipleri, taklit sunucu ve testler ona uymak zorundadır.
2. Çünkü düz yazıdır; uyulup uyulmadığına inceleyen insan karar verir. Makinenin okuduğu sözleşmeyle kural bir
   komuta dönüşür: koddan üretilen belge ile depodaki belgenin farkı yalnız ekleme mi?
3. `openapi` ve `info`. 3.1'de belgede `paths`, `components` ya da `webhooks` alanlarından en az biri bulunmalı.
   Böylece yalnız şema içeren, paylaşılan bileşen belgeleri yazılabiliyor.
4. `type: ["string", "null"]`. 3.1'de Şema Nesnesi JSON Schema 2020-12'nin üst kümesi oldu; JSON Schema'da
   `nullable` yoktur, boş değer bir türdür.
5. `type`, `title`, `status`, `detail`, `instance`. Dallanma `type`'a göre yapılır: birincil kimlik odur ve
   değişmez. `title` yerelleştirilebilir, `detail` oluşuma özgüdür; metne göre dallanan istemci, metin
   değiştiği gün bozulur.
6. Çünkü `app.js`'teki `govde.detay || govde.hata` boş döner ve kullanıcı yalnız "HTTP 400" görür; açık
   sekmeler de eski `app.js` ile konuşur. Genişlet: sunucu `type`, `title`, `status`, `detail` ekler, eski
   alanlar uzantı üyesi olarak kalır. Geçir: `app.js` yeni alanları okur, yoksa eskilere düşer; dallanma `type`
   ile yapılır. Daralt: eski alanları okuyan istemci kalmayınca onlar kaldırılır.
7. Biçimsiz kod 400 alır: hata yalnız isteğe bakarak bulunur (`ZiyaretciCerezi.gecerliMi`). Biçimce doğru ama
   kayıtsız kod 422 alır: karar için veritabanına bakmak gerekir (`IlerlemeKoduBulunamadiException`). 404
   dönülmez, çünkü adres vardır; bulunamayan şey gövdedeki bir göndermedir.
8. Eksik alan `null` gelsin ve 400 dönsün diye. İlkel olsaydı Jackson eksik alana `false` atardı; tanımadığı
   alanı da yok saydığından `"tamamlanmis": true` yazan istemci görevi sessizce geri açardı.
9. (b), (c) ve çoğu durumda (d) kırıcıdır; (a), istemciler hoşgörülü okuyucuysa kırıcı değildir. (c)'yi hiçbir
   şema fark aracı yakalayamaz, çünkü şema değişmez; (b) de varsayılan belgede yazılı değilse yakalanmaz.
10. `Deprecation`: RFC 9745 (Mart 2025, standart yolu), değeri `@` ile başlayan bir zaman damgası. `Sunset`:
    RFC 8594 (Mayıs 2019, bilgilendirme amaçlı), değeri HTTP tarihi. `Sunset`, `Deprecation`'dan önce olamaz.
    Bu depoda kullanılmadılar, çünkü HTTP API'nin tek istemcisi aynı uygulamayla gelen `app.js`'ti ve o da yeni
    uca geçirildi; uyarılacak bir dış istemci yoktu.
11. Güvenli: GET, HEAD, OPTIONS, TRACE. İdempotent: bunlar ile PUT ve DELETE. POST yalnız, istemci anlamın
    idempotent olduğunu tasarımdan biliyorsa ya da ilk isteğin uygulanmadığını anlayabiliyorsa tekrarlanır.
    `POST /api/gorev/durum` sunucuda idempotenttir, ama bu bilgi protokolde değil sözleşme metnindedir; PUT
    bunu protokolde söylerdi.
12. İnternet Taslağı, RFC değil: 28 Kasım 2026 itibarıyla son sürüm -09 (21 Kasım 2026), hedef standart yolu,
    IESG durumu "I-D Exists". Eksik anahtar 400, farklı gövdeyle aynı anahtar (parmak izi tutuluyorsa) 422,
    hâlâ işlenen isteğin anahtarı 409. En çok aday quiz sonucunu kaydeden uç ve MCP aracıdır
    (`POST /api/quiz/statik-sonuc`, `statik_quiz_kaydet`), çünkü her çağrı yeni satır ekler. ADR-0016 bunu
    anahtar kolonu ve sözleşme değişikliği kazançtan pahalı olduğu için ertelemiş ve yeniden açma koşulunu
    yazmış (uzak MCP sunucusu ya da quiz'in değerlendirmeye girmesi).
13. HTTP API'nin dış istemcisi yok: ön yüz aynı uygulamayla gelir, MCP araçları servisleri doğrudan çağırır.
    Sürüm; ikinci bir istemci türü, MCP sunucusunun yeniden uzakta çalışması ya da API'nin herkese açık bir
    ürün olmasıyla gerekir.
14. Birinci iddia: toggle ile hedef durum neredeyse aynı şemaya sahiptir; belgeden üretilen kod şemayı
    karşılayıp toggle'ı yazabilir ve tekrar denenen istek görevi geri açar. Şema davranışı anlatmaz. İkinci
    iddia: "çevir"den "ayarla"ya geçmek (MCP'de aynı adla yapıldığı gibi) şemayı değiştirmeden anlamı
    değiştirir; fark aracı yalnız yapısal kırıcı değişikliği görür.

## Kaynaklar
- OpenAPI Initiative, **OpenAPI Specification v3.1.1** (24 Ekim 2024): https://spec.openapis.org/oas/v3.1.1.html
- OpenAPI Initiative, **OpenAPI Specification v3.2.0** (19 Eylül 2025): https://spec.openapis.org/oas/v3.2.0.html
  · yayımlanmış sürümlerin listesi: https://spec.openapis.org/oas/
- OpenAPI Initiative, "OpenAPI Specification 3.1 Released" (18 Şubat 2021):
  https://www.openapis.org/blog/2021/02/18/openapi-specification-3-1-released
- OpenAPI Initiative, "Upgrading from OpenAPI 3.0 to 3.1": https://learn.openapis.org/upgrading/v3.0-to-v3.1.html
- JSON Schema, Draft 2020-12: https://json-schema.org/draft/2020-12
- M. Nottingham, E. Wilde, S. Dalal, **RFC 9457: Problem Details for HTTP APIs** (Temmuz 2023, RFC 7807'nin
  yerini alır): https://www.rfc-editor.org/rfc/rfc9457.html
- R. Fielding, M. Nottingham, J. Reschke (ed.), **RFC 9110: HTTP Semantics** (Haziran 2022), §9.2.1 güvenli
  yöntemler, §9.2.2 idempotent yöntemler, §15.5.1 400, §15.5.21 422: https://www.rfc-editor.org/rfc/rfc9110.html
- S. Dalal, E. Wilde, **RFC 9745: The Deprecation HTTP Response Header Field** (Mart 2025):
  https://www.rfc-editor.org/rfc/rfc9745.html
- E. Wilde, **RFC 8594: The Sunset HTTP Header Field** (Mayıs 2019, Informational):
  https://www.rfc-editor.org/rfc/rfc8594.html
- IETF HTTPAPI WG, **The Idempotency-Key HTTP Header Field**, draft-ietf-httpapi-idempotency-key-header-09
  (21 Kasım 2026; İnternet Taslağı, RFC değil): https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/
- Zalando, **RESTful API and Event Guidelines** (kurallar 100, 101, 108, 113, 114, 115, 185, 188, 189, 230,
  231): https://opensource.zalando.com/restful-api-guidelines/
- Google, **AIP-180: Backwards compatibility**: https://google.aip.dev/180 · **AIP-185: API Versioning**:
  https://google.aip.dev/185
- Microsoft, **Azure REST API Guidelines** (`api-version`, hata yanıtı, tekrarlanabilir istekler):
  https://github.com/microsoft/api-guidelines/blob/vNext/azure/Guidelines.md
- Pact belgeleri (sözleşme testi, tüketici güdümlü sözleşme testi): https://docs.pact.io/
- Martin Fowler, "ContractTest" (12 Ocak 2011): https://martinfowler.com/bliki/ContractTest.html
- Spring Framework, "Error Responses" (RFC 9457, `ProblemDetail`, `ErrorResponse`):
  https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- Spring Boot, "Servlet Web Applications", Error Handling (`spring.mvc.problemdetails.enabled`):
  https://docs.spring.io/spring-boot/reference/web/servlet.html
- Model Context Protocol, Specification 2026-07-28, "Tools" (`inputSchema`, varsayılan JSON Schema 2020-12):
  https://modelcontextprotocol.io/specification/2026-07-28/server/tools
- Bu deponun dosyaları (koddan doğrulandı): `pom.xml`, `README.md` ("HTTP API uç noktaları"),
  `web/HataYonetimi`, `web/GirdiSiniri`, `web/ProgramController`, `web/ZiyaretciController`,
  `web/MulakatController`, `service/IlerlemeService`, `service/ZiyaretciService`, `service/QuizUretimService`,
  `types/IlerlemeKoduBulunamadiException`, `types/UretilenQuiz`, `types/GorevIlerlemeKaydi`,
  `mcp/OgretmenMcpAraclari`, `mcp/McpAdaptorDestek`, `static/app.js`, `schema.sql`,
  `web/ProgramControllerTest`, `web/SiteIlerlemeKoduTest`, `docs/adr/0016-arac-idempotency-ve-ai-olcum-kararlari.md`,
  `docs/tech-debt-tracker.md`, `content/hafta-03/gun-1.md`.
