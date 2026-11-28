---
baslik: "Teslimat Hattı ve Gözlemlenebilirlik: AI Ajanı Çağında CI, Yayın, Sağlık Yoklaması ve Telemetri"
yazar: "AI Öğretmen ek okuma metni: teslimat hattı ve gözlemlenebilirlik konusu"
tarih: "2026-11-28"
dil: "Türkçe kavram özeti (diyagramlı)"
etiketler: 4,8,11
---

### Bu metin ne anlatıyor?
Bir yapay zekâ ajanına (agent) "şu özelliği ekle" dediğinde kod birkaç dakikada hazır olur. Ama yazılmış
kod, kullanıcıya güvenle ulaşmış kod değildir. Arada bir yol var: kod denetlenir, paketlenir, sunucuya
çıkar, çalıştığı doğrulanır ve izlenir. Bu yola **teslimat hattı** (delivery pipeline) denir. Metin bu yolun
üç sorusunu anlatıyor: **kod canlıya çıkmadan önce neyle denetlenir**, **canlıya nasıl güvenle çıkar ve
bozulursa nasıl geri alınır**, **çıktıktan sonra çalıştığını nereden bilirsin**. Üçüncü soru seni
**gözlemlenebilirliğe** (observability) götürür: ölçümler, günlükler, izler ve telemetri (sistemin kendisi
hakkında topladığın ölçüm verisi). Her adımda SokratAI'ın gerçekte ne yaptığını ve neyi neden yapmadığını
göreceksin.

## Neden bu özet
Hafta 1 Gün 2 (`content/hafta-01/gun-2.md`) harness'ı (koşum takımı) ikiye böldü: ajanı işten önce
yönlendiren **guide**'lar ve işten sonra ölçen **sensor**'ler. Sürekli tümleştirme (continuous integration,
CI) orada bir sensör olarak geçti, ama bir hattın neye benzediği anlatılmadı. Hafta 4 guardrail'ları (koruma
kuralları) işledi; Gün 4, sır taramasını (secret scanning) ve SAST'ı (statik kod güvenlik analizi) "CI'ya
ekle" diye önerdi. Hafta 8 Gün 4 gecikme (latency) ve maliyeti, Hafta 11 Gün 1 güvenilirliği kavram olarak
verdi. Bu derslerin hiçbiri canlı bir sistemin sağlığının nasıl yoklandığını ve yeni sürümün adım adım nasıl
açıldığını göstermedi.

AI ajanı çağında bu boşluk büyür. Kod üretimi ucuzladıkça darboğaz yazmaktan **doğrulamaya ve teslimata**
kayar: ajanın önerdiği her değişikliğin denetlenmesi, güvenle yayına alınması ve izlenmesi gerekir.

Burada tekrar etmeyeceklerimiz: LLM'e özgü gözlemlenebilirlik (span öznitelikleri, OpenTelemetry GenAI
adlandırma kuralları) [`evals-llm-observability.md`](evals-llm-observability.md)'de; LLM kanaryası ve AI
gateway [`llm-uretim-yolu.md`](llm-uretim-yolu.md)'de; maliyet mimarisi
[`finops-ai-tokenomics.md`](finops-ai-tokenomics.md)'de. Kubernetes yalnız kavram düzeyinde geçer; ayrı bir
Kubernetes metni sonra gelecek.

> **Tek cümle:** Teslimat hattı, ajanın atlayamadığı sensörlerin sıralı hâlidir. Gözlemlenebilirlik ise
> hattın bittiği yerde başlayan ve "canlıda gerçekten ne oluyor?" sorusunu cevaplayan son sensördür.

## Türkçe çalışma metni

### 1. Teslimat hattı bir sensör dizisidir
Sürekli tümleştirme, her değişikliği ana koda katılmadan önce otomatik denetimlerden geçirme alışkanlığıdır;
bu denetimlerin sıralı hâli teslimat hattıdır. Hafta 1 Gün 2'nin diliyle her denetim bir **sensördür**: iş
bittikten sonra çalışır, modelden bağımsızdır ve ajan onu atlayamaz. Bir denetimi sensör yapan, aracın adı
değil, ne zaman çalıştığı ve sonucunu zorlayıp zorlamadığıdır.

Tipik bir hatta şu sensörler sırayla durur:

- **Derleme:** En ucuz ve en hızlı sensör; bu yüzden ilk sırada.
- **Testler:** Birim testleri, mimari testler (bu depoda ArchUnit ile yazılmış `MimariTestleri`), gerçek
  tarayıcıda koşan uçtan uca testler.
- **SAST:** Kodu çalıştırmadan güvenlik açığı kalıplarını arar (SQL enjeksiyonu, sabit kodlanmış parola).
- **Sır taraması:** Koda ya da commit geçmişine girmiş anahtar, parola, özel anahtar arar.
- **Bağımlılık taraması (SCA, software composition analysis):** Kütüphanelerin bilinen açıklarını ve
  lisanslarını denetler. Ajan çağında ek bir işi var: ajanın uydurduğu ya da adı gerçek bir pakete benzeyen
  bağımlılığı yakalamak.
- **AI kod incelemesi:** Bir modelin değişikliği okuyup yorum yazması. Farkı **olasılıksal** olmasıdır: aynı
  değişikliğe iki çalıştırmada farklı yorum yazabilir.

Sıralamanın mantığı şu: **ucuz ve hızlı olan önce koşar** (fail fast: erken başarısız ol). Derlenmeyen bir
değişikliğe beş dakikalık tarama harcanmaz. Bütün denetimleri geçen kod bir kez paketlenir (§5) ve yayına
alınır (§7).

| Sensör | Ne yakalar | Deterministik mi | Önerilen rol |
|---|---|---|---|
| Derleme | Sözdizimi ve tip hatası | Evet | Kapı |
| Testler | Davranış hatası, mimari kural ihlali | Evet (kararsız testler hariç) | Kapı |
| SAST | Bilinen güvenlik açığı kalıbı | Evet, ama yanlış alarm verebilir | Kapı (kurallar ayıklandıktan sonra) |
| Sır taraması | Sızmış anahtar, parola | Evet | Kapı |
| Bağımlılık taraması | Bilinen açık, lisans, sahte paket | Evet | Önem derecesine göre kapı ya da danışman |
| AI kod incelemesi | Mantık hatası, eksik kenar durumu, okunabilirlik | Hayır | Danışman |

### 2. Kapı mı, danışman mı?
**Kapı** (gate), sonucu kırmızıysa değişikliğin ana koda birleştirilmesini (merge) engeller. **Danışman**
(advisory) yalnız yorum bırakır; kararı başkası verir.

Hangi sensörün kapı olacağını iki soru belirler: **aynı girdiye her seferinde aynı sonucu veriyor mu** ve
**yanlış alarm oranı düşük mü**? Derleyici ve iyi testler iki soruya da evet der; AI incelemesi birincisine
hayır der. Kararsız kapının bedeli yüksektir: rastgele kırmızı yanan kapıya ekip güvenmez, "bir daha
çalıştır" alışkanlığı başlar ve gerçek bir hata da yeniden çalıştırılıp geçirilir.
([`evals-llm-observability.md`](evals-llm-observability.md) §4 aynı sorunu eval (değerlendirme testi)
kapıları için anlatır.)

Önerimiz: **olasılıksal bir AI incelemesi tek başına birleştirmeyi engellememeli; danışman olarak çalışmalı,
son kararı insan vermeli.** Üç gerekçe:

1. **İki yönde de yanılır.** Gerçek hatayı kaçırabilir, olmayan sorunu işaretleyebilir. GitHub'ın Copilot
   kod incelemesi belgesi de Copilot'un bir PR'daki (pull request: birleştirme isteği) bütün sorunları
   bulmasının garanti olmadığını yazar; geri bildirimi dikkatle doğrulamayı ve insan incelemesiyle
   tamamlamayı önerir.
2. **Kodu yazanla inceleyen aynı kör noktayı paylaşabilir.** Bu bir ölçüm değil, bir akıl yürütmedir; ama
   ikinci gözün **farklı** bir göz olması gerektiğini hatırlatır.
3. **Sorumluluk devredilemez.** Canlıyı bozan değişikliğin hesabını model vermez.

AI incelemesi yine de değerlidir: ucuz ve hızlı bir ilk okumadır, insanın dikkatini doğru yere çeker. Hafta
1 Gün 2'nin kuralını uygula: **AI incelemesi aynı bulguyu tekrar tekrar yazıyorsa, o bulguyu deterministik
bir sensöre çevir** (bir ArchUnit kuralı, bir SAST kuralı ya da bir test). Olasılıksal bulgu böylece kesin
bir kapıya dönüşür.

İnsan onayı da sunucu tarafında zorlanabilir. GitHub'da korumalı dal (protected branch) kuralı, birleştirmeden
önce belirli sayıda onaylayan inceleme ve başarılı durum denetimleri (status check) şart koşabilir. GitHub
belgesine göre bu kurallar varsayılan olarak depo yöneticilerine uygulanmaz; yöneticiyi de bağlamak için
"ayarların atlanmasına izin verme" seçeneği ayrıca açılır. Tek kişilik depoda yönetici sensin; kural seni
bağlamıyorsa kapı değil, hatırlatmadır. Copilot kod incelemesi belgesine göre ise Copilot incelemeleri
varsayılan olarak zorunlu onay sayısına girmez, ama ayarlardan açılırsa Copilot'un onayı zorunlu onay kuralını karşılayabilir. O kutuyu
işaretlemek bir mimari karardır (bkz. §12).

### 3. Bir hattın iskeleti: GitHub Actions örneği
Bu bir araç eğitimi değil, yalnız bir hattın neye benzediğini gösteren kısa bir örnek. GitHub Actions'ta
hat bir iş akışıdır (workflow) ve `.github/workflows/` dizinindeki bir YAML dosyasıyla tanımlanır. Aşağıdaki
taslak, bu depo için yazılsaydı nasıl görüneceğini gösterir. **Bu depoda böyle bir dosya yok ve taslak denenmedi** (§4).

```yaml
# .github/workflows/dogrula.yml (taslak; bu depoda yok, denenmedi)
name: dogrula
on:
  pull_request:
  push:
    branches: [main]
permissions:
  contents: read                  # GITHUB_TOKEN yalnız okuyabilir
jobs:
  dogrula:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6        # gerçek hatta commit SHA'sına sabitle
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "17"
      - name: Chromium kur (Playwright testleri için)
        run: ./mvnw -B exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium" -Dexec.classpathScope=test
      - name: Derleme, test, ArchUnit, Playwright
        run: ./mvnw -B verify
      - name: Sızıntı taraması (ikinci kilit, bkz. §4)
        run: bash tools/sizinti-tara.sh HEAD
```

Okuma kılavuzu: `on` hattı başlatan olaylardır (her PR ve `main` dalına her push). `permissions`, hattın
`GITHUB_TOKEN` jetonunun yetkisini daraltır; GitHub'ın güvenli kullanım belgesi bu jetona en az yetkiyi
önerir. `jobs` altındaki her iş bir çalıştırıcıda (runner) koşar, `steps` sırayla çalışan adımlardır.
Üçüncü taraf eylemleri değişebilen bir etikete değil, sabit bir sürüme (commit SHA'sı ya da GitHub'ın
önerdiği kilit dosyası) bağlamak tedarik zinciri riskini azaltır. `--with-deps`, Playwright'ın CI
belgesindeki biçimdir ve tarayıcının sistem kütüphanelerini de kurar.

Taslağı ilginç kılan bir ayrıntı var: GitHub Actions her çalıştırmada `CI=true` değişkenini koyar. ADR-0021'e
göre Playwright testleri, Chromium kurulu değilse yerelde **atlanır**; ortamda `CI` değişkeni varsa ise
**hata verir**, gerekçe "CI'da davranış testleri sessizce kaybolmasın". Bu deponun testleri bir sunucu hattını
önceden düşünerek yazılmış; hattın kendisi henüz yok.

### 4. SokratAI'da durum: kapılar geliştiricinin makinesinde
Depoda `.github/` dizini yok; GitHub Actions ya da başka bir sunucu tarafı CI çalışmıyor. Bugünkü kapılar:

1. **`./mvnw verify`.** AGENTS.md'ye göre birleştirmeden önce yeşil olmalı. İçinde derleme, 542 test (5'i
   isteğe bağlı PostgreSQL testi olduğu için atlanır), ArchUnit `MimariTestleri`, yazım kuralını zorlayan
   `IcerikYazimKurallariTest` ve 16 Playwright tarayıcı testi var.
2. **Push öncesi sızıntı taraması.** `tools/git-kancalari/pre-push` kancası (hook), push edilen her yeni
   commit'in ağacını, dosya yollarını ve üstverisini `tools/sizinti-tara.sh` ile tarar: genel IP adresi,
   `root@` biçimli SSH hedefi, kişisel e-posta, özel anahtar, API anahtarı, şifre ataması. Yalnız yerelde kalması gereken
   eski geçmişten türeyen commit'leri de engeller. Tarama yapılamazsa da durdurur (kapalı-güvenli: emin
   olamayınca kapalı kal). Kanca her klonda bir kez `git config core.hooksPath tools/git-kancalari` ile açılır.
3. **İnceleme.** AGENTS.md'deki OVERRIDE kuralına göre Dockerfile, dağıtım (deployment), güvenlik ya da
   migration (veritabanı şema değişikliği) değişikliklerinde `qa-code-reviewer` ve Codex incelemesi
   zorunludur, bulgular push'tan önce kapatılır; gerekçe, repoda ikinci bir insan gözden geçiricinin
   olmaması. Zorunlu olan **incelemeyi çalıştırmaktır**; bulguyu kabul etme kararı geliştiricidedir. Yani iki
   AI incelemesi danışmandır, kapı geliştiricinin kendisidir.
4. **İmaj testleri tekrar koşturmaz.** `Dockerfile` konteyner imajını (container image)
   `-Donyuz.atla -Dmaven.test.skip=true` ile derler: Playwright profili kapalıdır, testler derlenmez bile.
   Push ile yayın arasında testleri yeniden çalıştıran bir adım yok.

Belge ile gerçek arasındaki farka dikkat et: `ARCHITECTURE.md` uzun süre mimari kuralların "CI'da
bloklayıcı olarak" test edildiğini yazdı; bu depoda o "CI", geliştiricinin makinesindeki `./mvnw verify`
koşusuydu. Fark bu metin yazılırken bulundu ve belge düzeltildi. Bir belgeyi
okurken "bu iddiayı hangi mekanizma zorluyor?" diye sor.

**Yerel kapı mı, sunucu kapısı mı?** Bu bir ödünleşimdir (trade-off): bir şeyi kazanırken başka bir şeyden
vazgeçersin. Yerel kapının zayıflıkları bu depoda gözlenebilir:

- **Atlanabilir.** Git belgesine göre `git push --no-verify` pre-push kancasını tamamen devre dışı bırakır.
- **Klonla gelmez.** Yeni bir klonda `core.hooksPath` ayarlanana kadar tarama çalışmaz; başka bir makinede
  çalışan bir ajan için de öyle.
- **Yeşil, eksik olabilir.** Chromium kurulu değilse Playwright testleri yerelde atlanır ve `verify` yine
  yeşil biter.
- **Kayıt bırakmaz.** "Bu sürüm hangi testlerden geçti?" sorusunun cevabı bir sunucu kaydında değil,
  geliştiricinin hafızasındadır.

Sunucu tarafı CI bunların çoğunu çözer: temiz ortamda koşar, korumalı dal kuralına bağlanınca atlanamaz, her koşunun
kaydını tutar. Bedeli kurulum, bakım ve çalıştırma kotasıdır. Tek geliştiricili, otomatik dağıtımı kapalı ve
her yayını elle başlatan bir depoda yerel kapılar savunulabilir bir seçimdir; yeter ki seçim olduğu bilinsin.

Öte yandan önemli bir ayrıntı var: **sunucu tarafı CI, sır sızıntısı için geç kalır.** CI, kod uzak depoya
ulaştıktan sonra koşar; herkese açık bir depoda sızan sır, CI yakaladığında çoktan yayılmıştır. Bu yüzden
sızıntı taraması doğru yerde, push'tan **önce** durur. GitHub'ın push koruması (push protection) desteklediği
sır türlerini push depoya ulaşmadan engeller; ama "eski geçmişten türeyen commit'i hiçbir yere gönderme" gibi
depoya özgü kurallar sır kalıbı değildir, onları yerel kanca denetler. Sunucu hattı eklense bile kanca
kalmalı; hattaki tarama ikinci kilit olur.

**Sunucu tarafı CI ne zaman gerekli olur?** İkinci bir katkıcı ya da dışarıdan PR geldiğinde (depo herkese
açık); ajanlar senin kanca ayarın olmayan ortamlarda PR açtığında (bulutta çalışan kodlama ajanları gibi);
otomatik dağıtım açıldığında (push doğrudan canlıyı güncelliyorsa kapı sunucuda olmak zorundadır); atlanan
bir yerel koşu yüzünden bir hata canlıya çıktığında; ve hazırlık ortamı (staging) gibi ikinci bir ortam
eklendiğinde (§5).

## Diyagram 1: Teslimat hattı ve SokratAI'ın bugünkü hattı
Üstte tipik bir ekip hattı: deterministik sensörler kapıdır, AI incelemesi danışmandır, insan onayı son
kapıdır. Altta SokratAI bugün: kapılar geliştiricinin makinesindedir, sunucu tarafı CI yoktur.

```svg
<svg viewBox="0 0 640 380" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Teslimat hattı: üstte tipik ekip hattında derleme, test ve güvenlik taramaları kapı, AI kod incelemesi danışman, insan onayı son kapı; altta SokratAI'da kapılar yerelde, yayın elle, sunucu tarafı CI yok">
  <defs>
    <marker id="th1-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#5D7789"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Teslimat hattı: her adım bir sensör</text>
  <text x="320" y="44" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Deterministik sensör kapı olur · olasılıksal AI incelemesi danışmandır</text>
  <text x="16" y="72" font-size="11.5" font-weight="700" fill="#186399" font-family="system-ui">A · Tipik ekip hattı (sunucu tarafında zorlanır)</text>
  <rect x="16" y="82" width="138" height="46" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="85" y="101" text-anchor="middle" font-size="11" font-weight="700" fill="#186399" font-family="system-ui">PR açılır</text>
  <text x="85" y="117" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">ajan ya da insan</text>
  <rect x="168" y="82" width="138" height="46" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="237" y="101" text-anchor="middle" font-size="11" font-weight="700" fill="#25804C" font-family="system-ui">Derleme + test</text>
  <text x="237" y="117" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">kapı · deterministik</text>
  <rect x="320" y="82" width="138" height="46" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="389" y="101" text-anchor="middle" font-size="11" font-weight="700" fill="#25804C" font-family="system-ui">SAST · sır · SCA</text>
  <text x="389" y="117" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">kapı · kural tabanlı</text>
  <rect x="472" y="82" width="138" height="46" rx="9" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="541" y="101" text-anchor="middle" font-size="11" font-weight="700" fill="#C77A17" font-family="system-ui">AI kod incelemesi</text>
  <text x="541" y="117" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">danışman · yorum yazar</text>
  <line x1="154" y1="105" x2="166" y2="105" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="306" y1="105" x2="318" y2="105" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="458" y1="105" x2="470" y2="105" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="541" y1="128" x2="541" y2="146" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <rect x="472" y="148" width="138" height="46" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="541" y="167" text-anchor="middle" font-size="11" font-weight="700" fill="#4F46E5" font-family="system-ui">İnsan onayı</text>
  <text x="541" y="183" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">kapı · son karar</text>
  <rect x="320" y="148" width="138" height="46" rx="9" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.5"/>
  <text x="389" y="167" text-anchor="middle" font-size="11" font-weight="700" fill="#1C3247" font-family="system-ui">İmaj bir kez</text>
  <text x="389" y="183" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">derlenir, terfi eder</text>
  <rect x="168" y="148" width="138" height="46" rx="9" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.5"/>
  <text x="237" y="167" text-anchor="middle" font-size="11" font-weight="700" fill="#1C3247" font-family="system-ui">Kademeli yayın</text>
  <text x="237" y="183" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">hazırlık yoklamasıyla</text>
  <rect x="16" y="148" width="138" height="46" rx="9" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.5"/>
  <text x="85" y="167" text-anchor="middle" font-size="11" font-weight="700" fill="#1C3247" font-family="system-ui">Gözlem</text>
  <text x="85" y="183" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">RED · SLO · maliyet</text>
  <line x1="471" y1="171" x2="460" y2="171" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="319" y1="171" x2="308" y2="171" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="167" y1="171" x2="156" y2="171" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="16" y1="214" x2="624" y2="214" stroke="#5D7789" stroke-width="1" stroke-dasharray="4 4"/>
  <text x="16" y="238" font-size="11.5" font-weight="700" fill="#1C3247" font-family="system-ui">B · SokratAI bugün (kapılar geliştiricinin makinesinde)</text>
  <rect x="16" y="248" width="138" height="48" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="85" y="268" text-anchor="middle" font-size="11" font-weight="700" fill="#25804C" font-family="system-ui">./mvnw verify</text>
  <text x="85" y="284" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">birim · ArchUnit · UI</text>
  <rect x="168" y="248" width="138" height="48" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="237" y="268" text-anchor="middle" font-size="11" font-weight="700" fill="#25804C" font-family="system-ui">pre-push kancası</text>
  <text x="237" y="284" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">sızıntı taraması</text>
  <rect x="320" y="248" width="138" height="48" rx="9" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="389" y="268" text-anchor="middle" font-size="11" font-weight="700" fill="#C77A17" font-family="system-ui">AI inceleme</text>
  <text x="389" y="284" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">qa-code-reviewer · Codex</text>
  <rect x="472" y="248" width="138" height="48" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="541" y="268" text-anchor="middle" font-size="11" font-weight="700" fill="#4F46E5" font-family="system-ui">Elle Redeploy</text>
  <text x="541" y="284" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">autoDeploy kapalı</text>
  <line x1="154" y1="272" x2="166" y2="272" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="306" y1="272" x2="318" y2="272" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <line x1="458" y1="272" x2="470" y2="272" stroke="#5D7789" stroke-width="1.8" marker-end="url(#th1-ok)"/>
  <text x="320" y="320" text-anchor="middle" font-size="10.5" fill="#BE4433" font-family="system-ui">Eksik halka: sunucu tarafı CI yok. Yerel kanca --no-verify ile atlanabilir ve</text>
  <text x="320" y="337" text-anchor="middle" font-size="10.5" fill="#BE4433" font-family="system-ui">yeni klonda kendiliğinden açılmaz; Chromium yoksa UI testleri yerelde atlanır.</text>
  <rect x="96" y="353" width="14" height="12" rx="2" fill="#E9F4EE" stroke="#25804C" stroke-width="1.3"/>
  <text x="116" y="363" font-size="10.5" fill="#1C3247" font-family="system-ui">kapı (deterministik)</text>
  <rect x="262" y="353" width="14" height="12" rx="2" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.3"/>
  <text x="282" y="363" font-size="10.5" fill="#1C3247" font-family="system-ui">danışman (olasılıksal)</text>
  <rect x="448" y="353" width="14" height="12" rx="2" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.3"/>
  <text x="468" y="363" font-size="10.5" fill="#1C3247" font-family="system-ui">insan kararı</text>
</svg>
```

### 5. Yayın: imajı bir kez derle, aynısını terfi ettir
Testleri geçen kod paketlenir; konteyner çağında paket bir imajdır. İlke, The Twelve-Factor App'in (on iki
etkenli uygulama) ayrımıdır: **derleme** kodu çalıştırılabilir pakete çevirir, **yayın** (release) paketi
ortamın yapılandırmasıyla birleştirir, **çalıştırma** onu ortamda koşturur. Belge her yayının benzersiz bir
kimliği olmasını ve oluşturulduktan sonra değiştirilememesini ister.

Pratik kural: **imaj bir kez derlenir, aynı imaj ortamdan ortama terfi eder** (promotion). Hazırlık ortamında
denediğin imaj ile canlıya çıkan imaj bit bit aynı olmalıdır. Her ortam için yeniden derlersen, denediğin şeyi
değil ona benzeyen başka bir şeyi yayınlarsın; arada bir bağımlılık ya da temel imaj değişmiş olabilir.
Ortamlar arasındaki fark imajda değil **yapılandırmada** (ortam değişkenleri, sırlar) durur.

Bir incelik: imaj etiketi (tag, örneğin `ai-ogretmen:1.4`) sonradan başka bir imaja taşınabilir. İmajı
değiştirilemez biçimde gösteren **digest**'tir (içerik özeti: imajın içeriğinden hesaplanan `sha256:`
değeri). Terfide digest'e bağlanmak "denediğim şey bu mu?" sorusunu kesin cevaplar.

**SokratAI'da durum.** Tek ortam var: canlı. `docs/kurulum-site.md` §10.1'e göre Dokploy uygulaması, kaynağı
Git deposu olan bir Dockerfile derlemesiyle kurulur (belge önceden derlenmiş imajı da seçenek olarak anar);
yani her Redeploy imajı sunucuda yeniden derler. `Dockerfile` temel imajları kesin etiketle sabitler
(`maven:3.9.16-eclipse-temurin-17`, `eclipse-temurin:17.0.21_9-jre`), ama bunlar etikettir, digest değildir.
Yapılandırma doğru yerdedir: imaj yalnız `SPRING_PROFILES_ACTIVE=site`, `SERVER_ADDRESS=0.0.0.0` ve JVM
bellek ayarlarını taşır; veritabanı adresi ve şifresi Dokploy'un ortam değişkenlerinden gelir (§10.3).
Hazırlık ortamı eklendiği gün kural değişir: imaj bir kayıt defterine (registry) bir kez itilir, iki ortam da
aynı digest'i çalıştırır.

**Yayın öncesi kontrol listesi** (son sütundaki § numaraları `docs/kurulum-site.md`'ye aittir):

| Madde | Neden | SokratAI'daki karşılığı |
|---|---|---|
| Hat yeşil mi? | Sensörler geçmeden yayın olmaz | Yerelde `./mvnw verify` (bu metnin §4'ü) |
| Yedek alındı mı? | Geri dönüşün son çaresi | Postgres dökümü ve `/app/data` volume'ü, ikisi birlikte (§10.6) |
| Şema değişikliği geriye uyumlu mu? | Eski sürüme dönülebilmeli | `schema.sql` yalnız ekler (README: "Göç aracı yok") |
| Yapılandırma ve sırlar yerinde mi? | Eksik değişken açılışı bozar | Site, `SPRING_DATASOURCE_URL` yoksa açılmaz (§8) |
| Yayın sonrası duman testi tanımlı mı? | "Açıldı" ile "çalışıyor" farklıdır | Günlükte `Started AiOgretmenApplication`, tarayıcı kontrolleri (§4, §10.6) |
| Geri alma yolu biliniyor mu? | Kriz anında yol aranmaz | Dokploy yolu için belgede adım yok (bu metnin §7'si) |
| Ne değişti, kim onayladı? | Sonradan iz sürmek | Commit mesajı ve inceleme; sunucu kaydı yok |

Bir içerik ayrıntısı: `Dockerfile`, `content/` ve `prompts/` dizinlerini imaja kopyalar ve uygulama müfredatı
yalnız açılışta okur. Bir ders metnindeki düzeltme de yeni imaj ve Redeploy ister; kodla aynı hattan geçer.

### 6. Sağlık yoklamaları: canlılık, hazırlık, başlangıç
Bir kopyanın (replica) "ayakta" olması iki soruya bölünür: **süreç yaşıyor mu?** ve **trafik almaya hazır
mı?** Kubernetes belgeleri bunları üç yoklama (probe) türüyle ayırır; ayrım Kubernetes kullanmasan da işe
yarar:

- **Canlılık yoklaması (liveness probe):** Konteynerin ne zaman yeniden başlatılacağını belirler. Yoklama
  izin verilen sayıdan fazla başarısız olursa düğümdeki Kubernetes bileşeni (kubelet) konteyneri **yeniden
  başlatır**.
- **Hazırlık yoklaması (readiness probe):** Trafik almaya hazır olup olmadığını belirler. Başarısız olursa
  kopya, onu hedefleyen Service'lerin uç noktalarından **çıkarılır**, yeniden başlatılmaz. Konteynerin bütün
  ömrü boyunca çalışır.
- **Başlangıç yoklaması (startup probe):** Tanımlıysa Kubernetes, bu yoklama başarılı olana kadar diğer
  ikisini **çalıştırmaz**; yavaş açılan uygulama daha açılmadan öldürülmez. Başarısız olursa konteyner
  öldürülür.

**Canlılık yoklaması neden veritabanına bağlı olmamalı?** Çünkü yeniden başlatmak veritabanını düzeltmez.
Veritabanı düşünce canlılık da düşerse platform bütün kopyaları aynı anda yeniden başlatır; açılanlar yine
veritabanına ulaşamaz, yine öldürülür. Hiçbir şey düzelmez, veritabanısız sunulabilecek her şey de kaybolur.
Kubernetes belgesi uyarır: canlılık yoklaması gerçekten kurtarılamaz bir arızayı (örneğin kilitlenme,
deadlock) göstermelidir; yanlış kurulursa yük altında yeniden başlatmalara ve kalan kopyalara binen ek yükle
**zincirleme arızaya** yol açabilir. Belgedeki ayrım: canlılık yalnız uygulamanın kendisine bakar, hazırlık
ek olarak gerekli arka uç hizmetlerini de denetler.

**SokratAI'da durum: ayrım zaten var.** `web/SaglikController.java` iki uç (endpoint) sunar:

| Uç | Ne yapar | Kim yoklar |
|---|---|---|
| `GET /api/saglik/canli` | Sabit 200 `{"durum":"canli"}`; veritabanına dokunmaz | `Dockerfile` HEALTHCHECK ve Dokploy sağlık kontrolü (canlılık) |
| `GET /api/saglik` | Veritabanına `SELECT 1`; başarıda 200 `ayakta`, hatada 503 `veritabani-yok` | Dış izleme aracı (hazırlık ve izleme) |

`repo/SaglikRepo.java` kendi `JdbcTemplate`'ini 2 saniyelik sorgu zaman aşımıyla kurar; hata metni yanıt
gövdesine girmez. `docs/kurulum-site.md` §10.4'e göre veritabanı tamamen düşükken uç, havuzun bağlantı
bekleme süresi (sitede 5 saniye) ile sorgu zaman aşımının (2 saniye) toplamı kadar, yani yaklaşık 7 saniyede
503 döner; izleme
aracının zaman aşımı en az 10 saniye olmalıdır. İki uç da çerez yazmaz, ziyaretçi kaydı açmaz, hız sınırı
(rate limit) kovası tüketmez: dakikada bir gelen yoklama ziyaretçi verisini kirletmemelidir.

`Dockerfile`'daki HEALTHCHECK 60 saniye aralık, 10 saniye zaman aşımı, 3 deneme ve **90 saniyelik başlangıç
payıyla** (`--start-period`) çalışır. Docker belgesine göre başlangıç payındaki başarısızlıklar deneme
sayısına eklenmez; yani bu pay başlangıç yoklamasının işini görür. Aynı belge, HEALTHCHECK'in konteynere bir durum
(`starting`, `healthy`, `unhealthy`) verdiğini anlatır; bu duruma göre ne yapılacağına çalıştıran platform
karar verir. Spring Boot Actuator'ın hazır sağlık ucu ise kullanılmadı: ADR-0018 onu yeni bir bağımlılık ve
tek bir `SELECT 1` için gereksiz bir yüzey diye reddeder.

Dürüst bir sınır notu: belge `/api/saglik`'i trafiği yöneten bir mekanizmaya değil, dış izleme aracına bağlar.
Bugün "hazırlık" ucu bir **alarm** kaynağıdır, trafik kapısı değildir; tek kopya olduğu için trafiği
devredecek başka kopya da yoktur.

### 7. Yayın stratejileri ve geri alma
Yeni imajı canlıya çıkarmanın birkaç yolu var. Ortak soru: **eski ve yeni sürüm aynı anda çalışacak mı,
çalışacaksa ne kadar?**

- **Yeniden oluşturma (recreate):** Kubernetes belgesinin tanımıyla, yeni Pod'lar oluşturulmadan önce mevcut
  bütün Pod'lar öldürülür. İki sürüm hiç bir arada çalışmaz; bedeli aradaki kesintidir.
- **Kademeli güncelleme (rolling update):** Kopyalar küçük gruplar hâlinde yenilenir; Kubernetes
  Deployment'ın varsayılanıdır. En çok kaç kopyanın erişilemez olabileceğini (`maxUnavailable`) ve istenen
  sayının en çok ne kadar aşılabileceğini (`maxSurge`) ayarlarsın; ikisinin varsayılanı yüzde 25'tir. Yeni
  kopya hazırlık yoklamasını geçmeden trafik almaz; hazırlık yoklaması yanlışsa kademeli güncelleme de yanlış
  çalışır.
- **Mavi-yeşil (blue-green):** Martin Fowler'ın tarifiyle, olabildiğince aynı iki canlı ortam tutulur; biri
  trafiği alırken diğerine yeni sürüm kurulur, sonra yönlendirici (router) yeni ortama çevrilir. Geri alma
  tek hamledir: yönlendiriciyi geri çevir. Bedeli iki tam ortamdır; Fowler'a göre veritabanı burada çoğu
  zaman zorluk çıkarır.
- **Kanarya (canary):** Yeni sürüm önce kullanıcıların küçük bir dilimine açılır, ölçümler iyiyse dilim
  büyür. Adı, zehirli gazı erken haber vermesi için madene indirilen kanaryadan gelir. Geri alma, dilimi
  eski sürüme yönlendirmektir. Fowler'ın saydığı bedeller: birden çok sürümü birlikte yönetmek ve veritabanı
  değişikliklerine özen. Kubernetes belgesi kanaryayı her sürüm için ayrı bir Deployment kurma deseni olarak
  anlatır. LLM özelliklerinde kanaryanın neye bakacağı [`llm-uretim-yolu.md`](llm-uretim-yolu.md) §7'de.

Kademeli güncelleme, kanarya ve geçiş anında mavi-yeşil, **iki sürümü bir süre aynı anda çalıştırır**. Şema
ve API iki sürümle de uyumlu olmalıdır. Fowler'ın kanarya yazısı bunun için paralel değişime (parallel change)
yönlendirir: önce yeni sütunu boş bırakılabilir (nullable) olarak **eklersin**, uygulamayı güncellersin, eski
yapıyı ancak eski sürüm tamamen kalkınca **kaldırırsın**.

**Geri alma.** Kubernetes Deployment yayın geçmişini sürümler (revision) olarak tutar; varsayılan olarak son
10 sürüm saklanır, sınır 0 yapılırsa geri alma mümkün olmaz. Yeni bir yayın yalnız Pod şablonu (örneğin
imaj) değişince başlar; kopya sayısını değiştirmek yayın sayılmaz. Unutma: **kodu geri almak veriyi geri
almaz.** Geri alınabilir bir yayın, eski kodun yeni veriyle de çalışabilmesine dayanır.

**SokratAI'da durum.**

- **Tek kopya şart.** `docs/kurulum-site.md` §10.1 ve ADR-0018'e göre Dokploy'da replika sayısı 1'dir.
  Gerekçe: hız sınırı sayaçları JVM belleğindedir ve 180 günlük gece temizliği kilitsiz çalışır. ADR-0017 §8
  kilidin neden eklenmediğini yazar: site tek JVM'dir ve iş idempotenttir (ikinci tur silinecek satır
  bulamaz). README'nin "Yol haritası ve bilinen sınırlar" bölümü çok örnekli dağıtımı kapsam dışı sayar.
- **Bu yüzden kademeli güncelleme ve kanarya bugün kapsam dışıdır.** İki kopyada her biri kendi sayacını
  tutar; IP başına dakikada 60 yazma sınırı fiilen kopya sayısıyla çarpılır. Gece işi her kopyada ayrı koşar;
  zarar vermez ama işi iki kez yapar. Önce sayaçlar paylaşılan bir depoya taşınmalı, gece işine kilit
  eklenmelidir.
- **Yayın elle yapılır.** `docs/kurulum-site.md` §10.6'ya göre akış şöyledir: yerelde `./mvnw verify` yeşil, push, sonra Dokploy panelinde Redeploy.
  Otomatik dağıtım (autoDeploy) kapalıdır. Yeniden başlatmanın etkisi belgede dürüstçe yazılı (aynı belgenin §5'i,
  systemd yolu için): ziyaretçiler düşmez çünkü kimlik çerezdedir, bellekteki hız sınırı sayaçları sıfırlanır ve o
  anda süren bir sohbet isteği yarıda kalabilir.
- **Geri alma.** systemd yolunda belge, önceki jar'ı (`ai-ogretmen.jar.onceki`) geri kopyalayıp yeniden
  başlatmayı tarif eder; şema yalnız eklediği için eski sürüm yeni şemayla çalışır. **Dokploy yolu (belgenin §10'u)
  için bir geri alma adımı yok.** Genel yol, önceki sürümün kodunu yeniden yayına almaktır (`git revert`,
  push, Redeploy); bu da yeni bir derleme demektir. Belgeye göre Dokploy uygulamaları Docker Swarm servisi
  olarak çalışır ve Swarm'ın kendi seçenekleri vardır (`docker service update` komutunda `--update-order` ve
  `--rollback`), ama depo belgeleri onları tarif etmez. Bir platform özelliğine güvenmeden önce kendi
  sürümünün belgesinden doğrula ve runbook'a (işletme kılavuzu) yaz. [`llm-uretim-yolu.md`](llm-uretim-yolu.md)
  §8'in sorusu burada da geçerli: kapı kırmızıya döndüğünde geri dönüş kaç saniye sürüyor?

## Diyagram 2: Üç yayın stratejisi ve geri alma
Kademeli güncellemede kopyalar sırayla yenilenir; mavi-yeşilde yönlendirici iki tam ortam arasında geçer;
kanaryada küçük bir dilim yeni sürüme gider. Alttaki kutu SokratAI'ın neden bunları kullanmadığını özetler.

```svg
<svg viewBox="0 0 640 400" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Üç yayın stratejisi: kademeli güncellemede kopyalar sırayla v1'den v2'ye geçer; mavi-yeşilde yönlendirici iki tam ortam arasında çevrilir; kanaryada küçük bir trafik dilimi v2'ye gider. SokratAI tek kopya çalıştığı için bunları kullanmaz">
  <defs>
    <marker id="th2-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#5D7789"/></marker>
    <marker id="th2-yesil" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#25804C"/></marker>
    <marker id="th2-mavi" markerWidth="14" markerHeight="14" refX="9" refY="5" orient="auto" markerUnits="userSpaceOnUse"><path d="M0,0 L11,5 L0,10 Z" fill="#186399"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Üç yayın stratejisi ve geri alma</text>
  <text x="320" y="44" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Ortak koşul: yeni kopya hazırlık yoklamasını geçmeden trafik almaz</text>
  <rect x="16" y="58" width="196" height="252" rx="10" fill="#F7F9FB" stroke="#5D7789" stroke-width="1.2"/>
  <rect x="222" y="58" width="196" height="252" rx="10" fill="#F7F9FB" stroke="#5D7789" stroke-width="1.2"/>
  <rect x="428" y="58" width="196" height="252" rx="10" fill="#F7F9FB" stroke="#5D7789" stroke-width="1.2"/>
  <text x="114" y="80" text-anchor="middle" font-size="12.5" font-weight="700" fill="#186399" font-family="system-ui">Kademeli (rolling)</text>
  <rect x="34" y="94" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <rect x="74" y="94" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <rect x="114" y="94" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <rect x="154" y="94" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <text x="51" y="108" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <text x="91" y="108" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <text x="131" y="108" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <text x="171" y="108" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <rect x="34" y="122" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <rect x="74" y="122" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <rect x="114" y="122" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <rect x="154" y="122" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <text x="51" y="136" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <text x="91" y="136" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <text x="131" y="136" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <text x="171" y="136" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <rect x="34" y="150" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <rect x="74" y="150" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <rect x="114" y="150" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <rect x="154" y="150" width="34" height="20" rx="4" fill="#EAF1F7" stroke="#186399" stroke-width="1.2"/>
  <text x="51" y="164" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <text x="91" y="164" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <text x="131" y="164" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <text x="171" y="164" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <rect x="34" y="178" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <rect x="74" y="178" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <rect x="114" y="178" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <rect x="154" y="178" width="34" height="20" rx="4" fill="#E9F4EE" stroke="#25804C" stroke-width="1.2"/>
  <text x="51" y="192" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <text x="91" y="192" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <text x="131" y="192" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <text x="171" y="192" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <line x1="200" y1="96" x2="200" y2="194" stroke="#5D7789" stroke-width="1.4" marker-end="url(#th2-ok)"/>
  <text x="114" y="226" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Geri alma</text>
  <text x="114" y="242" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">önceki sürüme yine kademeli</text>
  <text x="114" y="266" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Bedel</text>
  <text x="114" y="282" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">iki sürüm bir süre birlikte</text>
  <text x="114" y="297" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">çalışır; şema ikisine uymalı</text>
  <text x="320" y="80" text-anchor="middle" font-size="12.5" font-weight="700" fill="#186399" font-family="system-ui">Mavi-yeşil (blue-green)</text>
  <rect x="270" y="94" width="100" height="24" rx="6" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.3"/>
  <text x="320" y="110" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">yönlendirici</text>
  <rect x="234" y="150" width="74" height="38" rx="7" fill="#EAF1F7" stroke="#186399" stroke-width="1.4"/>
  <text x="271" y="166" text-anchor="middle" font-size="10.5" font-weight="700" fill="#186399" font-family="system-ui">Mavi</text>
  <text x="271" y="181" text-anchor="middle" font-size="9.5" fill="#186399" font-family="system-ui">v1</text>
  <rect x="332" y="150" width="74" height="38" rx="7" fill="#E9F4EE" stroke="#25804C" stroke-width="1.4"/>
  <text x="369" y="166" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">Yeşil</text>
  <text x="369" y="181" text-anchor="middle" font-size="9.5" fill="#25804C" font-family="system-ui">v2</text>
  <line x1="332" y1="118" x2="364" y2="146" stroke="#25804C" stroke-width="2.2" marker-end="url(#th2-yesil)"/>
  <line x1="308" y1="118" x2="276" y2="146" stroke="#5D7789" stroke-width="1.3" stroke-dasharray="4 3"/>
  <text x="271" y="204" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">beklemede</text>
  <text x="369" y="204" text-anchor="middle" font-size="9" fill="#25804C" font-family="system-ui">trafik burada</text>
  <text x="320" y="226" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Geri alma</text>
  <text x="320" y="242" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">yönlendiriciyi geri çevir</text>
  <text x="320" y="266" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Bedel</text>
  <text x="320" y="282" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">iki tam ortam; veritabanı</text>
  <text x="320" y="297" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">iki sürüme birden uymalı</text>
  <text x="526" y="80" text-anchor="middle" font-size="12.5" font-weight="700" fill="#186399" font-family="system-ui">Kanarya (canary)</text>
  <rect x="476" y="94" width="100" height="24" rx="6" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.3"/>
  <text x="526" y="110" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">yönlendirici</text>
  <rect x="440" y="150" width="74" height="38" rx="7" fill="#EAF1F7" stroke="#186399" stroke-width="1.4"/>
  <text x="477" y="166" text-anchor="middle" font-size="10.5" font-weight="700" fill="#186399" font-family="system-ui">v1</text>
  <text x="477" y="181" text-anchor="middle" font-size="9" fill="#186399" font-family="system-ui">çoğunluk</text>
  <rect x="538" y="150" width="74" height="38" rx="7" fill="#E9F4EE" stroke="#25804C" stroke-width="1.4"/>
  <text x="575" y="166" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">v2</text>
  <text x="575" y="181" text-anchor="middle" font-size="9" fill="#25804C" font-family="system-ui">küçük dilim</text>
  <line x1="514" y1="118" x2="482" y2="146" stroke="#186399" stroke-width="3.5" marker-end="url(#th2-mavi)"/>
  <line x1="538" y1="118" x2="570" y2="146" stroke="#25804C" stroke-width="1.4" marker-end="url(#th2-yesil)"/>
  <text x="526" y="204" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">ölçüm iyiyse dilim büyür</text>
  <text x="526" y="226" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Geri alma</text>
  <text x="526" y="242" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">dilimi eski sürüme yönlendir</text>
  <text x="526" y="266" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Bedel</text>
  <text x="526" y="282" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">birden çok sürüm; ölçüm</text>
  <text x="526" y="297" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">ve yönlendirme altyapısı</text>
  <rect x="16" y="322" width="608" height="66" rx="10" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.5"/>
  <text x="320" y="342" text-anchor="middle" font-size="11" font-weight="700" fill="#BE4433" font-family="system-ui">SokratAI bugün: replika 1, yayın = elle Redeploy (yeniden başlatma)</text>
  <text x="320" y="360" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">Bellekteki hız sayaçları ve kilitsiz gece işi tek kopya varsayar: bu üç strateji kapsam dışı.</text>
  <text x="320" y="376" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">Şema yalnız eklediği için eski sürüme dönülebilir; Dokploy için belgelenmiş geri alma adımı yok.</text>
</svg>
```

### 8. Kubernetes'e kısa bakış (yalnız kavramlar)
Ayrıntı ayrı bir metinde gelecek; burada yalnız kavramları eşleştiriyoruz:

- **Pod:** Bir ya da birkaç konteynerin birlikte çalıştığı en küçük birim; yoklamalar Pod'daki konteynere
  tanımlanır.
- **Deployment:** Çoğunlukla durum tutmayan bir uygulamanın Pod kümesini yönetir. İstenen durumu bildirirsin
  ("bu imajdan 3 kopya"), denetleyici gerçek durumu kontrollü bir hızla ona yaklaştırır.
- **Service:** Pod'ların önündeki sabit ad ve adres; hazırlık yoklamasını geçemeyen Pod buradan çıkarılır.
- **Ingress:** Kümeye dışarıdan, tipik olarak HTTP ile gelen erişimi yönetir. Kubernetes belgesi artık
  Gateway API'yi önerir ve Ingress API'sinin dondurulduğunu yazar: kaldırılmayacak ama geliştirilmeyecek.
- **kind:** Docker konteynerlerini "düğüm" olarak kullanıp yerelde Kubernetes kümesi çalıştıran araç.
  Esas olarak Kubernetes'in kendisini test etmek için tasarlanmıştır, yerel geliştirme ve CI için de
  kullanılabilir.

SokratAI'da Traefik, Ingress denetleyicisinin rolünü oynar (alan adı, TLS, yönlendirme); Docker HEALTHCHECK
canlılık, `/api/saglik` kabaca hazırlık yoklamasının karşılığıdır (bugün trafiği kesen değil, yalnız izlenen bir uç). Bu depo Kubernetes'te çalışsaydı yoklamalar
kabaca şöyle yazılırdı:

```yaml
# Varsayımsal Pod tanımı parçası (bu depoda Kubernetes yok)
livenessProbe:
  httpGet: { path: /api/saglik/canli, port: 8090 }   # veritabanına dokunmaz
  periodSeconds: 60
  timeoutSeconds: 10
  failureThreshold: 3
readinessProbe:
  httpGet: { path: /api/saglik, port: 8090 }         # SELECT 1; en kötü yaklaşık 7 sn
  timeoutSeconds: 10
startupProbe:
  httpGet: { path: /api/saglik/canli, port: 8090 }
  periodSeconds: 5
  failureThreshold: 18                               # 18 x 5 sn = 90 sn başlangıç payı
```

Başlangıç yoklamasının canlılıkla aynı ucu yoklaması Kubernetes belgesinin önerisidir. Ama bu tanımı tartmadan
kopyalama: paylaşılan veritabanını hazırlığa koymak, veritabanı düşünce bütün kopyaların aynı anda trafikten
çekilmesi demektir. Uygulama veritabanısız da bir şey sunabiliyorsa bu bir kayıptır; sunamıyorsa doğru
davranıştır.

### 9. Gözlemlenebilirlik: ölçüm, günlük, iz
Sürüm canlıda; şimdi son sensör devreye girer. Google'ın SRE (site reliability engineering: sistem
güvenilirliği mühendisliği) kitabı izlemeyi iki soruya bağlar: **ne bozuk** (belirti) ve **neden** (sebep).
İki bakışı da ayırır: sistemin iç ölçümlerine dayanan **beyaz kutu** (white-box) izleme ve dışarıdan bir
kullanıcı gibi deneyen **kara kutu** (black-box) izleme. Dış izleme aracının `/api/saglik`'i yoklaması kara
kutudur; uygulamanın kendi günlükleri beyaz kutudur.

Üç tür veri (sinyal) vardır:

- **Ölçüm (metric):** Zamana göre kaydedilen sayılar: istek sayısı, hata oranı, gecikme dağılımı. Ucuzdur,
  "bir şey değişti mi?" sorusunu hızla cevaplar ama ayrıntı vermez.
- **Günlük (log):** Tek tek olayların kaydı. Ayrıntılı ve pahalıdır; "tam olarak ne oldu?" sorusunu cevaplar.
- **İz (trace):** Bir isteğin servisler ve adımlar arasındaki yolculuğu; "zaman nerede harcandı?" sorusunu
  cevaplar. Span ve trace tanımları [`evals-llm-observability.md`](evals-llm-observability.md) §5'te.

**OpenTelemetry** (kısaca OTel), bu sinyallerin üretilmesi, dışa aktarılması ve toplanması için bir framework
(çatı) ve araç takımıdır. Belgesine göre açık kaynaktır, satıcıdan ve araçtan bağımsızdır ve kendisi bir
arka uç değildir: veriyi saklamak ve görselleştirmek Jaeger, Prometheus gibi araçların işidir. Parçaları:
veri üretmeyi tanımlayan **API**'ler, bunları her dilde uygulayıp veriyi dışa aktaran **SDK**'lar ve veriyi
alan, işleyen ve dışa aktaran bir aracı olan **Collector** (toplayıcı). Uygulamayı bir kez OTel ile
donatırsın; arka ucu değiştirdiğinde kodu değil Collector yapılandırmasını değiştirirsin. OTel bir CNCF
(Cloud Native Computing Foundation) projesidir.

**Prometheus** bir ölçüm sistemidir. Veriyi **zaman serisi** (time series) olarak saklar; her seri bir ölçüm
adı ve **etiket** (label) denen anahtar-değer çiftleriyle tanımlanır. Veriyi **çekme** (pull) modeliyle,
HTTP üzerinden kendisi toplar: uygulama ölçümlerini bir uçta sunar, Prometheus belli aralıklarla okur. Kısa
ömürlü işler için bir itme geçidi (push gateway) vardır, sorgu dili PromQL'dir. İki uyarısı burada önemli:

1. **Kesin hesap için değildir.** Prometheus belgesi, istek başına faturalama gibi yüzde yüz doğruluk isteyen
   işler için iyi bir seçim olmadığını yazar. SokratAI'daki `ai_kullanim` tablosu, yani her AI çağrısı için
   bir veritabanı satırı, kesin muhasebenin doğru yeridir; Prometheus eğilim ve alarm içindir.
2. **Etiket kardinalitesine dikkat.** Belgeye göre her benzersiz etiket birleşimi yeni bir seridir; kullanıcı
   kimliği, e-posta gibi sınırsız değerli boyutlar etikete konmaz. `ai_kullanim.kullanici_id` bir SQL sütunu
   olarak doğrudur, bir Prometheus etiketi olarak yanlıştır.

**Grafana** bir pano (dashboard) aracıdır: pano, satırlara ya da sekmelere yerleşmiş panellerden oluşur.
Grafana veriyi üretmez, **veri kaynaklarından** okur; veri kaynağı bir SQL veritabanı, bir ölçüm deposu ya
da bir API olabilir. Bu ayrıntı §11'de işine yarayacak.

### 10. Neyi ölçmeli: RED, USE ve SLO
Neyi ölçeceğini seçmek için üç yerleşik yöntem var:

- **RED yöntemi (Tom Wilkie):** Her **hizmet** için **hız** (rate, saniyedeki istek), **hatalar** (errors,
  başarısız istek sayısı) ve **süre** (duration, isteklerin süresi ve dağılımı). Kullanıcının yaşadığını
  anlatır.
- **USE yöntemi (Brendan Gregg):** Her **kaynak** için **kullanım oranı** (utilization, kaynağın meşgul
  olduğu süre), **doygunluk** (saturation, karşılanamayıp çoğu zaman kuyruğa alınan fazladan iş) ve
  **hatalar**. İşlemci, bellek, bağlantı havuzu gibi kaynaklar içindir.
- **Dört altın sinyal (Google SRE):** Gecikme, trafik, hatalar, doygunluk. Kitap başarılı ve başarısız
  isteklerin gecikmesini ayrı ölçmeyi vurgular: hızlı dönen hata, ortalamayı iyi gösterip seni yanıltır.

Grafana'nın pano belgesine göre USE sorunların **sebeplerini**, RED kullanıcının gördüğü **belirtileri**
raporlar; belge alarmı RED panolarına kurmayı önerir. SRE kitabı da aynı yöne gider: nöbetçiyi çağıran her alarm
üzerine bir şey yapılabilir olmalıdır. SokratAI'da RED, her API ucu için istek sayısı, 4xx ve 5xx oranı ve
süre dağılımıdır. USE ise JVM belleği (imaj, konteyner belleğinin yüzde 75'ini yığına verir ve bellek biterse
süreci düşürür), işlemci ve veritabanı bağlantı havuzudur; havuzdan bağlantı bekleyen istek sayısı tam bir
doygunluk ölçümüdür.

**SLO, kısaca.** SRE kitabının tanımlarıyla: **SLI** (service level indicator: hizmet seviyesi göstergesi)
hizmetin bir yönünün dikkatle tanımlanmış sayısal ölçüsüdür; **SLO** (service level objective: hizmet
seviyesi hedefi) bir SLI için hedef değerdir; **SLA** (service level agreement: hizmet seviyesi sözleşmesi)
bu hedefleri içeren ve kaçırılınca sonucu olan sözleşmedir. Kitabın uyarıları: yüzde yüz hedeflemek
ne gerçekçidir ne de istenir; ölçümleri ortalama değil dağılım (yüzdelik dilim) olarak düşün; kullanıcıya
söylediğinden sıkı bir iç hedef tut. Hedefin izin verdiği kaçırma payına **hata bütçesi** (error budget)
denir; bütçe tükenince yeni özellik yerine güvenilirlik işine dönülür. SokratAI'da yazılı bir SLO yok
([`llm-uretim-yolu.md`](llm-uretim-yolu.md) §9 da not eder). Örnek bir hedef (ölçülmüş değer değil, şablon):
"Aylık olarak `/api/saglik` yoklamalarının yüzde 99,5'i 200 dönsün." Yüzdelik dilimli gecikme hedefi için
[`hafta11-ddia-veri-yogun.md`](hafta11-ddia-veri-yogun.md)'ye bak.

### 11. AI telemetrisi: uygulamanın çağrıları ve ajanın kendisi
İki ayrı AI harcaması izlenir: **uygulamanın** kullanıcıları adına yaptığı model çağrıları ve **kodlama
ajanının** uygulamayı geliştirirken yaptığı çağrılar. İkisi farklı kişilere fatura edilir ve farklı
sorulara cevap verir.

**(a) Uygulamanın AI çağrıları.** Her çağrı için en az dört şey kaydedilmeli: **token** (girdi ve çıktı),
**maliyet**, **gecikme**, **model**. SokratAI bunun büyük kısmını yapıyor:

- `src/main/resources/schema.sql`'deki `ai_kullanim` tablosu her çağrı için bir satır tutar: `kullanici_id`,
  `islem` (DEGERLENDIRME, SOHBET, HAFIZA, QUIZ, RAPOR gibi), `model` (istenen model), `input_tokens`,
  `output_tokens`, `sure_ms` (duvar saati süresi, yeniden denemeler dahil), `yanit_model` (sağlayıcının
  cevapta bildirdiği model) ve `tarih`.
- Satırı `ai/SpringAiOgretmenClient.java` yazar. Kullanıcı kimliği çağrıdan **önce** çözülür ki kimliksiz
  çağrı para harcamasın. Süre, Spring AI'nın kendi yeniden denemelerini (retry) de kapsar; kullanıcının
  gerçekten beklediği süre budur.
- **Faz 14 düzeltmesi:** Kullanım satırı artık boş cevap denetiminden **önce** yazılır. Koddaki gerekçe: boş
  ya da içerik filtresine takılmış cevabın da ücreti ödenmiştir, kayıt düşerse maliyet raporu eksik kalır.
  Önceden boş cevap hata fırlatıyor ve satır hiç yazılmıyordu. Ders: **ücreti ödenen her çağrı, sonucu ne
  olursa olsun bir kullanım satırı bırakmalıdır.**
- Aynı noktada bir günlük satırı yazılır:
  `AI çağrısı: islem=... model=... yanitModel=... input=... output=... token sure=...ms`. Sağlayıcı hatasında
  yalnız HTTP kodu ve hata kategorisi yazılır (`docs/kurulum-site.md` §7.1); ziyaretçinin anahtarı ne
  günlüğe ne tabloya girer.
- `GET /api/kullanim`, README'nin "YZ kullanım takibi" bölümüne göre yalnız o kullanıcının satırlarını
  toplar (toplam çağrı, girdi ve çıktı token'ı, işlem bazında dağılım). `sure_ms` ve `yanit_model`'i
  özetlemez, para birimiyle maliyet hesaplamaz; harcama OpenRouter panelinden izlenir.

İki kör nokta var. README'ye göre **başarısız çağrı satır yazmaz**: AI çağrılarının hata oranı (RED'deki E)
bugün yalnız günlüklerde görünür. Tabloda maliyet sütunu da yok; maliyet token sayısı ile sağlayıcıdaki birim
fiyatın çarpımıdır. Sitede bu bilinçli: sohbeti ziyaretçi kendi OpenRouter anahtarıyla yapar, maliyet onun
hesabına yazılır (ADR-0017).

Ek bağımlılıksız en ucuz ölçüm SQL'dir. `docs/kurulum-site.md` §7.5'te token toplayan hazır bir sorgu var;
PostgreSQL'de gecikmenin yüzdelik dilimi de hesaplanabilir:

```sql
-- Son 7 günde işlem başına çağrı sayısı ve p95 süre (PostgreSQL)
SELECT islem,
       COUNT(*) AS cagri,
       percentile_cont(0.95) WITHIN GROUP (ORDER BY sure_ms) AS p95_ms
FROM ai_kullanim
WHERE tarih > now() - interval '7 days' AND sure_ms IS NOT NULL
GROUP BY islem
ORDER BY p95_ms DESC;
```

**Prometheus ve OpenTelemetry ne zaman eklenmeli?** `pom.xml`'de Actuator, Prometheus dışa aktarıcısı ya da
OpenTelemetry bağımlılığı yok. Paketlenmiş jar'da Spring'in ölçüm kütüphanesi Micrometer'ın çekirdeği dolaylı
bağımlılık olarak bulunur (Spring AI'nın `spring-ai-commons` modülü getirir), ama ölçümleri dışarı açan bir
uç ya da dışa aktarıcı yok. Spring AI belgesine göre framework model çağrıları için hazır ölçümler tanımlar
(süre için `gen_ai.client.operation`, token için `gen_ai.client.token.usage`) ve prompt (modele yazılan
metin) ile cevap metnini varsayılan olarak dışa aktarmaz; görünür kılmak için Spring Boot'un ölçüm ve iz
desteğini açman gerekir. Bu yeni bir bağımlılık demektir; AGENTS.md yeni bağımlılığı önce sorulacak işler arasında
sayar ve önce bir ADR ister. Bu adımı haklı çıkaracak durumlar: **birden çok kopya** (günlükler dağılır, toplam hata oranı tek
yerde görünmez), **yazılı bir SLO** (uç başına yüzdelik dilimli gecikme gerekir), **belirtiye dayalı alarm**
("ayakta mı?" yetmez, "hata oranı yükseldi mi?" diye sormak gerekir) ve **uçtan uca iz** (istek birden çok
servisten geçmeye başlarsa).

**(b) Kodlama ajanının kendi telemetrisi.** Claude Code'un resmî izleme belgesi, ajanın OpenTelemetry
üzerinden ölçüm ve olay dışa aktarabildiğini anlatır:

- **Varsayılan kapalıdır**, açıkça açılır: `CLAUDE_CODE_ENABLE_TELEMETRY=1`. Ölçüm dışa aktarıcısı
  `OTEL_METRICS_EXPORTER` (`otlp`, `prometheus`, `console`, `none`), olaylarınki `OTEL_LOGS_EXPORTER`
  (`otlp`, `console`, `none`) ile seçilir; hedef `OTEL_EXPORTER_OTLP_ENDPOINT` ve `OTEL_EXPORTER_OTLP_PROTOCOL`
  ile verilir. Ölçümler varsayılan olarak 60 saniyede, olaylar 5 saniyede bir gönderilir. İz dışa aktarımı
  beta durumundadır.
- **Ölçümler:** `claude_code.session.count`, `claude_code.lines_of_code.count`,
  `claude_code.pull_request.count`, `claude_code.commit.count`, `claude_code.cost.usage` (oturum maliyeti,
  USD), `claude_code.token.usage` (`type`: `input`, `output`, `cacheRead`, `cacheCreation`; ayrıca `model`),
  `claude_code.code_edit_tool.decision` (düzenleme izni kabul ya da ret) ve `claude_code.active_time.total`.
- **Olaylar:** her model isteği için `claude_code.api_request` (model, tahmini maliyet, süre, girdi, çıktı ve
  önbellek (cache) token'ları), ayrıca `claude_code.api_error`, `claude_code.tool_result`,
  `claude_code.tool_decision`.
- **Gizlilik:** Prompt metni varsayılan olarak toplanmaz, yalnız uzunluğu kaydedilir (metin için
  `OTEL_LOG_USER_PROMPTS=1` gerekir). Bu OpenTelemetry verisi yalnız senin yapılandırdığın arka uca gider;
  Anthropic'in ayrı işletim telemetrisi başka bir konudur. Yöneticiler bu
  ayarları yönetilen ayarlar (managed settings) dosyasıyla dayatabilir; bir deponun `.claude/settings.json`
  dosyasındaki `OTEL_*` dışa aktarıcı değişkenleri ise yok sayılır.
- **Belgenin kendi şerhleri:** "Maliyet ölçümleri yaklaşık değerlerdir"; resmî faturalama için başka bir
  sayfaya yönlendirir. Ayrıca Claude Code `gen_ai.usage.*` özniteliklerini koymaz ve `input_tokens` değeri
  önbellekten okunan ve önbelleğe yazılan token'ları **içermez**; GenAI kurallarına uygun toplam girdi için
  `input`, `cacheRead` ve `cacheCreation` toplanır.

**(c) Tek pano.** Şu soruyu tek ekranda cevaplamak değerlidir: "Bu hafta ajanla geliştirmeye ne harcadım, o
geliştirmenin ürettiği özellik canlıda nasıl davranıyor?" Grafana birden çok veri kaynağını aynı panoda
okuyabildiği için bu mümkündür (Diyagram 3). Uygulama paneli (RED) bugün veri bulamaz; Micrometer dışa
aktarımı eklenirse Prometheus'tan beslenir. AI çağrısı paneli `ai_kullanim`'ı doğrudan PostgreSQL veri
kaynağından okur; uygulamaya yeni bağımlılık gerekmez. Yalnız okuma yetkili ayrı bir veritabanı rolü kullan;
Postgres'in dış portu yayınlanmadığı için (`docs/kurulum-site.md` §10.2) Grafana'nın aynı iç ağda olması
gerekir. Ajan paneli `claude_code.cost.usage` ve `claude_code.token.usage`'ı bir Collector üzerinden
Prometheus'tan alır.

Üç tuzaktan kaçın. **Birimler aynı değil:** Claude Code'un `input` token'ı önbelleği dışlar,
`ai_kullanim.input_tokens` ise Spring AI'nın bildirdiği prompt token sayısıdır; aynı çizgide toplama.
**Faturalar aynı kişiye gitmiyor:** sitede uygulamanın AI maliyeti ziyaretçiye, ajanın maliyeti geliştiriciye
yazılır; tek bir "AI maliyeti" sayısı yanlıştır. **Etkinlik verim değildir:** satır ve commit sayısı ne kadar
değer üretildiğini değil, ne kadar etkinlik olduğunu gösterir; ölçtüğün sayıyı yanlış okumanın kanıtları
[`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md)'de.

## Diyagram 3: Tek pano: uygulama, AI çağrıları ve ajan telemetrisi
Düz çizgiler bugün var olan veri yollarını, kesikli çizgiler kurulursa eklenecekleri gösterir. Bugün var
olan: `ai_kullanim` tablosu, Dokploy günlükleri ve sağlık uçları. Grafana, Collector ve Prometheus bu depoda
yok.

```svg
<svg viewBox="0 0 640 424" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Tek pano: SokratAI uygulamasının ai_kullanim tablosu PostgreSQL'e, günlükleri Dokploy'a gider; Claude Code telemetrisi OTel Collector üzerinden Prometheus'a gider; Grafana PostgreSQL ve Prometheus'u aynı panoda okur. Grafana, Collector ve Prometheus bugün yok">
  <defs>
    <marker id="th3-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#25804C"/></marker>
    <marker id="th3-kes" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#5D7789"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Tek pano: uygulama, AI çağrıları ve ajan maliyeti</text>
  <text x="320" y="44" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Düz çizgi: bugün var · kesikli çizgi: bugün yok, kurulursa</text>
  <rect x="16" y="62" width="176" height="152" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="104" y="82" text-anchor="middle" font-size="11.5" font-weight="700" fill="#186399" font-family="system-ui">SokratAI uygulaması</text>
  <rect x="28" y="94" width="152" height="24" rx="6" fill="#FFFFFF" stroke="#25804C" stroke-width="1.3"/>
  <text x="104" y="110" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">ai_kullanim satırı</text>
  <rect x="28" y="124" width="152" height="24" rx="6" fill="#FFFFFF" stroke="#25804C" stroke-width="1.3"/>
  <text x="104" y="140" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">günlük: AI çağrısı satırı</text>
  <rect x="28" y="154" width="152" height="24" rx="6" fill="#FFFFFF" stroke="#25804C" stroke-width="1.3"/>
  <text x="104" y="170" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">/api/saglik uçları</text>
  <rect x="28" y="184" width="152" height="24" rx="6" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.3" stroke-dasharray="4 3"/>
  <text x="104" y="200" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">Micrometer ölçümleri</text>
  <rect x="16" y="236" width="176" height="122" rx="10" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="104" y="256" text-anchor="middle" font-size="11.5" font-weight="700" fill="#C77A17" font-family="system-ui">Claude Code (ajan)</text>
  <rect x="28" y="266" width="152" height="24" rx="6" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.3" stroke-dasharray="4 3"/>
  <text x="104" y="282" text-anchor="middle" font-size="9.5" fill="#1C3247" font-family="system-ui">OTLP ölçüm: token, maliyet</text>
  <rect x="28" y="296" width="152" height="24" rx="6" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.3" stroke-dasharray="4 3"/>
  <text x="104" y="312" text-anchor="middle" font-size="9.5" fill="#1C3247" font-family="system-ui">OTLP olay: api_request</text>
  <text x="104" y="342" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">varsayılan kapalı (açıkça açılır)</text>
  <rect x="232" y="72" width="170" height="44" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="317" y="91" text-anchor="middle" font-size="11" font-weight="700" fill="#25804C" font-family="system-ui">PostgreSQL</text>
  <text x="317" y="107" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">ai_kullanim (SQL ile okunur)</text>
  <rect x="232" y="126" width="170" height="44" rx="9" fill="#FFFFFF" stroke="#25804C" stroke-width="1.5"/>
  <text x="317" y="145" text-anchor="middle" font-size="11" font-weight="700" fill="#1C3247" font-family="system-ui">Dokploy günlükleri</text>
  <text x="317" y="161" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">panelde okunur</text>
  <rect x="232" y="180" width="170" height="44" rx="9" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.5" stroke-dasharray="5 3"/>
  <text x="317" y="199" text-anchor="middle" font-size="11" font-weight="700" fill="#1C3247" font-family="system-ui">Dış izleme aracı</text>
  <text x="317" y="215" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">belgede önerilir</text>
  <rect x="232" y="262" width="170" height="44" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5" stroke-dasharray="5 3"/>
  <text x="317" y="281" text-anchor="middle" font-size="11" font-weight="700" fill="#4F46E5" font-family="system-ui">OTel Collector</text>
  <text x="317" y="297" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">al · işle · dışa aktar</text>
  <rect x="232" y="320" width="170" height="44" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5" stroke-dasharray="5 3"/>
  <text x="317" y="339" text-anchor="middle" font-size="11" font-weight="700" fill="#4F46E5" font-family="system-ui">Prometheus</text>
  <text x="317" y="355" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">zaman serisi · çekme (pull)</text>
  <rect x="440" y="62" width="184" height="302" rx="11" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.8" stroke-dasharray="6 3"/>
  <text x="532" y="84" text-anchor="middle" font-size="12.5" font-weight="700" fill="#1C3247" font-family="system-ui">Grafana: tek pano</text>
  <rect x="454" y="98" width="156" height="50" rx="7" fill="#E9F4EE" stroke="#25804C" stroke-width="1.3"/>
  <text x="532" y="118" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">AI çağrıları</text>
  <text x="532" y="135" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">token · p95 sure_ms</text>
  <rect x="454" y="158" width="156" height="50" rx="7" fill="#EAF1F7" stroke="#186399" stroke-width="1.3"/>
  <text x="532" y="178" text-anchor="middle" font-size="10.5" font-weight="700" fill="#186399" font-family="system-ui">RED: istek · hata · süre</text>
  <text x="532" y="195" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">Micrometer eklenirse</text>
  <rect x="454" y="218" width="156" height="50" rx="7" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.3"/>
  <text x="532" y="238" text-anchor="middle" font-size="10.5" font-weight="700" fill="#C77A17" font-family="system-ui">Ajan maliyeti</text>
  <text x="532" y="255" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">USD, belgeye göre yaklaşık</text>
  <rect x="454" y="278" width="156" height="50" rx="7" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.3"/>
  <text x="532" y="298" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Sağlık</text>
  <text x="532" y="315" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">canlı · hazır</text>
  <text x="532" y="350" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">tek zaman ekseni</text>
  <line x1="180" y1="106" x2="230" y2="96" stroke="#25804C" stroke-width="1.6" marker-end="url(#th3-ok)"/>
  <line x1="180" y1="136" x2="230" y2="146" stroke="#25804C" stroke-width="1.6" marker-end="url(#th3-ok)"/>
  <line x1="180" y1="166" x2="230" y2="198" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <line x1="180" y1="196" x2="230" y2="334" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <line x1="180" y1="278" x2="230" y2="280" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <line x1="180" y1="308" x2="230" y2="290" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <line x1="317" y1="306" x2="317" y2="318" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <line x1="402" y1="94" x2="438" y2="118" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <line x1="402" y1="202" x2="438" y2="296" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <line x1="402" y1="342" x2="438" y2="244" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#th3-kes)"/>
  <text x="320" y="392" text-anchor="middle" font-size="10" fill="#BE4433" font-family="system-ui">Tuzak: Claude Code'un input token'ı önbelleği dışlar; sitede AI maliyeti ziyaretçinin</text>
  <text x="320" y="408" text-anchor="middle" font-size="10" fill="#BE4433" font-family="system-ui">anahtarına, ajan maliyeti geliştiriciye yazılır. İki sayıyı tek toplamda birleştirme.</text>
</svg>
```

### 12. ⚠️ ZORUNLU ŞERH: "her hatayı yakalar", "sıfır kesinti", "tek ajanla tam görünürlük"
Bu alan satıcı iddiasıyla dolu. Dört tip iddiayı şöyle oku:

1. **"AI incelemesi hataları yakalar, onu kapı yapın."** Hiçbir satıcı bunu garanti etmiyor; GitHub'ın kendi
   belgesi bütün sorunları bulmanın garanti olmadığını yazıyor. "AI onayı zorunlu onay kuralını karşılasın"
   ayarının var olması, onu açmanın doğru olduğunu göstermez; ayar satıcının sunduğu bir yetenektir, riski
   taşıyan sensin. "Şu kadar hata yakaladık" rakamını gördüğünde sor: neye karşı ölçüldü, kaçırılanlar ve
   yanlış alarmlar sayıldı mı, senin kod tabanına benzer bir kümede mi denendi? Bu metin hiçbir yakalama
   oranı vermiyor, çünkü doğrulanmış bir birincil kaynak yok.
2. **"Sıfır kesinti (zero-downtime) dağıtım."** Strateji adları kesintisizliği garanti etmez. Kademeli
   güncelleme ancak hazırlık yoklaması doğruysa, eski kopyalar süren isteklerini bitirmeden kapanmıyorsa ve
   şema iki sürüme de uyuyorsa kesintisizdir. Tek kopyalı sistemde her yayın bir yeniden başlatmadır;
   SokratAI'ın belgesi bunu saklamaz: o anda süren bir sohbet isteği yarıda kalabilir. "Sıfır kesinti"
   ölçülmesi gereken bir iddiadır: yayın sırasında dışarıdan sürekli yokla ve başarısız istekleri say.
3. **"Ajanımızı kurun, her şeyi görün; kök nedeni yapay zekâ bulsun."** OpenTelemetry'nin var oluş sebebi
   bunun tersidir: veriyi tek bir satıcıya bağlamadan üretmek. Claude Code belgesi de kendi sınırını çizer:
   yalnız ham olay akışını üretir; anomali tespiti ve alarm senin arka ucunun sorumluluğudur. Veri toplamak
   içgörü değildir. Neyi neden ölçtüğünü (RED, USE, SLO) bilmeden toplanan veri faturası olan bir gürültüdür;
   yüksek kardinaliteli etiketler bu faturayı hızla büyütür.
4. **"Ajan telemetrisi verimliliği ölçer."** Satır sayısı, commit sayısı ve oturum süresi etkinliktir;
   maliyet ölçümü de belgeye göre yaklaşıktır. Bu sayılar "ne kadara mal oldu?" sorusuna kaba bir cevap
   verir, "değer üretti mi?" sorusuna vermez.

Kendi belgelerine de aynı şüpheyle bak. `ARCHITECTURE.md` "CI'da bloklayıcı" diyordu, ama sunucu tarafı CI
yok. Kurulum kılavuzu §10.1 çalışma imajını `eclipse-temurin:17-jre` diye anıyordu, `Dockerfile` ise daha
kesin `17.0.21_9-jre` etiketini kullanır. İkisi de bu metin yazılırken bulunup düzeltildi. Küçük farklar,
ama ders aynı: **bir iddiayı, onu zorlayan mekanizmayla
ya da sistemin gerçek çıktısıyla karşılaştır.**

### Bu programdaki ilgili kaynaklar
- Aynı dizinin kardeş metinleri (AI çağında teslimat döngüsü): [`test-kalitesi-ai-cagi.md`](test-kalitesi-ai-cagi.md), [`tehdit-modelleme-stride.md`](tehdit-modelleme-stride.md), [`api-sozlesme-once.md`](api-sozlesme-once.md).
- Hafta 1 Gün 2 (`content/hafta-01/gun-2.md`): guide ve sensor ayrımı, §1'in temeli.
- Hafta 4 Gün 4 (`content/hafta-04/gun-4.md`): sır taramasını ve SAST'ı hatta koymak.
- Hafta 8 Gün 4 (`content/hafta-08/gun-4.md`): gecikme ve maliyet mühendisliği.
- Hafta 11 Gün 1 (`content/hafta-11/gun-1.md`): güvenilirlik, ölçeklenebilirlik, bakım.
- [`evals-llm-observability.md`](evals-llm-observability.md): LLM'e özgü gözlemlenebilirlik ve eval kapısı.
- [`llm-uretim-yolu.md`](llm-uretim-yolu.md): LLM kanaryası, bayrakla geri alma, goodput.
- [`finops-ai-tokenomics.md`](finops-ai-tokenomics.md): maliyet damgası, harcama tavanı.
- [`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md): ajan sayılarını verim sanmamak.
- [`harness-engineering-openai.md`](harness-engineering-openai.md): harness ve sensör fikri.
- [`temel-secure-coding.md`](temel-secure-coding.md), [`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md):
  SAST'ın gördüğü ve göremediği yüzey.
- [`temel-developer-testing.md`](temel-developer-testing.md): test piramidi.
- [`hafta11-ddia-veri-yogun.md`](hafta11-ddia-veri-yogun.md): yüzdelik dilim ve hizmet hedefi.
- [`ajan-calisma-zamani-kontrolleri.md`](ajan-calisma-zamani-kontrolleri.md): ajanın harcama tavanı ve acil
  durdurma anahtarı.
- Depo belgeleri: `docs/kurulum-site.md` §10, ADR-0017, ADR-0018, ADR-0021 (`docs/adr/`).

## Öz-sınav (kendini test et)
1. Hafta 1 Gün 2'deki ayrıma göre teslimat hattındaki her adım neden bir sensördür? Bir denetimi sensör
   yapan, aracın adı değilse nedir?
2. Bir sensörün kapı mı danışman mı olacağına karar veren iki soru nedir? AI kod incelemesi neden genellikle
   danışman olmalıdır? Kararsız bir kapının ekibe zararı nedir?
3. AI incelemesinin her PR'da aynı tür bulguyu yazdığını fark ettin. Hafta 1 Gün 2'nin kuralına göre ne
   yaparsın?
4. SokratAI'da bir değişiklik canlıya çıkmadan önce hangi kapılardan geçer? Hangisi nasıl atlanabilir?
   `Dockerfile` testleri neden koşturmaz?
5. Yerelde `./mvnw verify` yeşil bittiği hâlde Playwright testleri hiç koşmamış olabilir mi? Hangi koşulda?
   GitHub Actions'ta aynı durum neden farklı sonuçlanır?
6. "Sunucu tarafı CI kurarsak pre-push sızıntı kancasına gerek kalmaz" cümlesi bu depo için neden yanlıştır?
7. "İmajı bir kez derle, aynısını terfi ettir" ilkesini açıkla. Etiket ile digest farkı buna nasıl bağlanır?
   SokratAI bu ilkenin neresinde?
8. Canlılık, hazırlık ve başlangıç yoklamalarının her biri başarısız olunca Kubernetes ne yapar? Canlılık
   yoklaması neden veritabanına bağlı olmamalıdır?
9. SokratAI'ın iki sağlık ucunu karşılaştır: hangisi veritabanına dokunur, HEALTHCHECK hangisini yoklar,
   veritabanı düşükken `/api/saglik` yaklaşık kaç saniyede 503 döner ve neden? `--start-period=90s` hangi
   yoklama türünün işini görür?
10. Kademeli güncelleme, mavi-yeşil ve kanaryada geri alma nasıl yapılır? Ortak veritabanı koşulu nedir,
    SokratAI'da bu koşulu sağlayan kural hangisidir?
11. SokratAI'da replika sayısını 2 yapıp kademeli güncellemeye geçmek neden bugün hatalı olur? Önce hangi iki
    şey değişmelidir?
12. RED ve USE yöntemlerini tanımla. Alarm hangisine kurulur, neden? SokratAI için birer örnek ölçüm ver.
13. `ai_kullanim` neden Prometheus'tan daha doğru bir muhasebe yeridir? `kullanici_id`'yi Prometheus etiketi
    yapmak neden hatalıdır? Faz 14 düzeltmesi neyi değiştirdi, hangi kör nokta duruyor?
14. Claude Code telemetrisini açmak için hangi değişken gerekir? Maliyet ve token için hangi ölçümler var?
    Bunları `ai_kullanim` ile aynı panoda gösterirken hangi üç tuzak var?
15. **Şerh sorusu:** Bir satıcı "sıfır kesinti", bir başkası "AI incelememiz hataların çoğunu yakalar" diyor.
    Her iddiayı nasıl sınarsın? SokratAI'ın belgesinde "sıfır kesinti" ile çelişen cümle hangisi?

### Cevaplar
1. İş bittikten sonra çalışır, modelden bağımsızdır, ajan atlayamaz. Belirleyici olan ne zaman çalıştığı ve
   sonucunu zorlayıp zorlamadığıdır.
2. Aynı girdiye hep aynı sonucu veriyor mu, yanlış alarm oranı düşük mü? AI incelemesi olasılıksaldır, iki
   yönde de yanılır; satıcı belgesi de garanti vermez. Kararsız kapıya güven kaybolur, "yeniden çalıştır"
   alışkanlığı gerçek bulguları da geçirir.
3. Bulguyu deterministik bir sensöre çevirirsin: ArchUnit kuralı, SAST kuralı ya da test.
4. `./mvnw verify`, pre-push sızıntı taraması ve AI incelemesi (kararı geliştirici verir). Kanca
   `git push --no-verify` ile atlanır, yeni klonda `core.hooksPath` ayarlanana kadar çalışmaz. `Dockerfile`
   `-Donyuz.atla -Dmaven.test.skip=true` ile derler; testlerin yerelde koştuğu varsayılır.
5. Evet: Chromium kurulu değilse testler yerelde atlanır (ADR-0021). GitHub Actions `CI=true` koyar; `CI`
   varken tarayıcı yokluğu hatadır.
6. CI, kod uzak depoya ulaştıktan sonra koşar; herkese açık depoda sızan sır o anda yayılmıştır. Eski
   geçmişten türeyen commit'i engellemek gibi depoya özgü kurallar da sır kalıbı değildir. Kanca kalır, CI
   ikinci kilittir.
7. Denediğin ile yayınladığın aynı olsun diye imaj bir kez derlenir; ortamlar arasında yalnız yapılandırma
   değişir. Etiket taşınabilir, digest içerikten hesaplanır. SokratAI'da tek ortam var, her Redeploy yeniden
   derler, temel imajlar etiketle sabitlidir.
8. Canlılık: konteyner yeniden başlatılır. Hazırlık: Pod, Service uç noktalarından çıkarılır. Başlangıç:
   başarılı olana kadar diğer ikisi çalışmaz, başarısızsa konteyner öldürülür. Yeniden başlatma veritabanını
   düzeltmez; bütün kopyalar birlikte yeniden başlar, zincirleme arıza doğar.
9. `/api/saglik` `SELECT 1` ile dokunur, `/api/saglik/canli` dokunmaz; HEALTHCHECK `canli` ucunu yoklar.
   Yaklaşık 7 saniye: havuz bekleme süresi (5 sn) artı sorgu zaman aşımı (2 sn). `--start-period` başlangıç
   yoklamasının işini görür.
10. Kademeli: önceki sürüme yine kademeli dönülür. Mavi-yeşil: yönlendirici geri çevrilir. Kanarya: dilim eski
    sürüme yönlendirilir. Şema iki sürümle de çalışmalıdır; SokratAI'da `schema.sql` yalnız ekler.
11. Bellekteki hız sınırı sayaçları kopya sayısıyla çarpılır, kilitsiz gece işi her kopyada koşar. Önce
    sayaçlar paylaşılan bir depoya taşınmalı, gece işine kilit eklenmelidir.
12. RED: hizmet başına hız, hata, süre. USE: kaynak başına kullanım oranı, doygunluk, hata. Alarm RED'e
    kurulur, çünkü kullanıcının yaşadığı belirtidir. Örnek: sohbet ucunun 5xx oranı (RED), havuzdan bağlantı
    bekleyen istek sayısı (USE).
13. Prometheus belgesi yüzde yüz doğruluk isteyen faturalama için uygun olmadığını yazar; `ai_kullanim` çağrı
    başına satır tutar. Her etiket birleşimi yeni seridir, kullanıcı kimliği sınırsızdır. Faz 14: satır boş
    cevap denetiminden önce yazılır. Kör nokta: başarısız çağrı satır yazmaz.
14. `CLAUDE_CODE_ENABLE_TELEMETRY=1` (ve bir dışa aktarıcı). `claude_code.cost.usage` ve
    `claude_code.token.usage`. Tuzaklar: birimler farklı (Claude Code `input` önbelleği dışlar), faturalar
    farklı kişilere gider, etkinlik verim değildir.
15. Sıfır kesinti: yayın sırasında dışarıdan sürekli yokla, başarısız istekleri say; hazırlık yoklamasını,
    bağlantı boşaltmayı, şema uyumunu sor. AI incelemesi: ölçüm kümesini, kaçırılanları ve yanlış alarmları
    sor. Çelişen cümle: yeniden başlatmada o anda süren bir sohbet isteği yarıda kalabilir.

## Kaynaklar
- Kubernetes, Liveness, Readiness, and Startup Probes:
  <https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/>
- Kubernetes, Deployments: <https://kubernetes.io/docs/concepts/workloads/controllers/deployment/>
- Kubernetes, Ingress: <https://kubernetes.io/docs/concepts/services-networking/ingress/>
- kind: <https://kind.sigs.k8s.io/>
- OpenTelemetry, What is OpenTelemetry?: <https://opentelemetry.io/docs/what-is-opentelemetry/>
- Prometheus, Overview: <https://prometheus.io/docs/introduction/overview/>
- Prometheus, Metric and label naming: <https://prometheus.io/docs/practices/naming/>
- Grafana, Dashboards: <https://grafana.com/docs/grafana/latest/visualizations/dashboards/>
- Grafana, Dashboard best practices (RED, USE, dört altın sinyal):
  <https://grafana.com/docs/grafana/latest/visualizations/dashboards/build-dashboards/best-practices/>
- Tom Wilkie, The RED Method:
  <https://grafana.com/blog/2018/08/02/the-red-method-how-to-instrument-your-services/>
- Brendan Gregg, The USE Method: <https://www.brendangregg.com/usemethod.html>
- Google SRE kitabı, Bölüm 6: <https://sre.google/sre-book/monitoring-distributed-systems/>
- Google SRE kitabı, Bölüm 3 (hata bütçesi): <https://sre.google/sre-book/embracing-risk/>
- Google SRE kitabı, Bölüm 4: <https://sre.google/sre-book/service-level-objectives/>
- GitHub Actions, Workflow syntax:
  <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- GitHub Actions, Variables: <https://docs.github.com/en/actions/reference/workflows-and-actions/variables>
- GitHub Actions, Secure use reference: <https://docs.github.com/en/actions/reference/security/secure-use>
- GitHub, About protected branches:
  <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>
- GitHub, Copilot code review: <https://docs.github.com/en/copilot/concepts/agents/code-review>
- GitHub, About push protection:
  <https://docs.github.com/en/code-security/secret-scanning/introduction/about-push-protection>
- Git, git push: <https://git-scm.com/docs/git-push>
- Docker, Dockerfile reference (HEALTHCHECK): <https://docs.docker.com/reference/dockerfile/>
- Docker, docker service update: <https://docs.docker.com/reference/cli/docker/service/update/>
- The Twelve-Factor App, Build, release, run: <https://12factor.net/build-release-run>
- Martin Fowler, BlueGreenDeployment: <https://martinfowler.com/bliki/BlueGreenDeployment.html>
- Martin Fowler, CanaryRelease: <https://martinfowler.com/bliki/CanaryRelease.html>
- Claude Code, Monitoring: <https://code.claude.com/docs/en/monitoring-usage>
- Spring AI, Observability: <https://docs.spring.io/spring-ai/reference/observability/index.html>
- Playwright for Java, Continuous Integration: <https://playwright.dev/java/docs/ci>
- Bu deponun vakası (2026-11-28'de okundu): `Dockerfile`, `docs/kurulum-site.md`, ADR-0017, ADR-0018,
  ADR-0021, `AGENTS.md`, `ARCHITECTURE.md`, `README.md`, `pom.xml`, `schema.sql`, `SpringAiOgretmenClient`,
  `SaglikController`, `SaglikRepo`, `tools/git-kancalari/pre-push`, `tools/sizinti-tara.sh`.
