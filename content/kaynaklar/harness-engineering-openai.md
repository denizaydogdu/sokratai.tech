---
baslik: "Harness Mühendisliği: Ajan-Öncelikli Dünyada Codex'ten Yararlanmak"
yazar: "Ryan Lopopolo (OpenAI, Teknik Kadro)"
tarih: "2026-02-11"
url: "https://openai.com/index/harness-engineering-leveraging-codex-in-an-agent-first-world/"
dil: "İngilizce aslından Türkçe çalışma metni"
etiketler: 1,2,3,4
---

### Bu metin ne anlatıyor?
Bu metin, üç mühendisin beş ay boyunca **tek satır kodu elle yazmadan**, bütün kodu yapay zekâ
ajanlarına yazdırarak gerçek bir ürün çıkarmasının hikâyesini anlatıyor. Asıl konu hikâye değil:
insanın işi kod yazmaktan, **ajanın çalışacağı ortamı kurmaya** kayıyor. Bu ortamın adı
**harness**tir ve metin onu kurmanın somut yollarını gösterir: ajana ne anlatılır, ne ölçülür,
hangi kural makineyle zorunlu kılınır. Sonda, yazının kendi büyük iddialarına nasıl temkinli
yaklaşman gerektiğini ayrıca işliyoruz (§11).

## Neden okumalısın
Programının 1. haftasının ana kaynağı bu yazı. OpenAI'dan üç mühendis, boş bir **git
deposundan** (repo: bir projenin bütün dosyalarını ve değişiklik geçmişini tutan klasör)
başlayıp 5 ayda ~1 milyon satırlık gerçek bir ürünü **tek satır elle kod yazmadan** çıkardı;
her satırı **Codex** (OpenAI'ın kod yazan yapay zekâ ajanı) yazdı. Yazının değeri sayılarda
değil (sayılara §11'de şerh düşüyoruz). Değeri, insan mühendisin işinin NEYE dönüştüğünü
göstermesinde: kod işçiliğinden **ortam, niyet ve geri-besleme döngüsü tasarımına**. Sloganı:
"Humans steer. Agents execute." (İnsanlar yönlendirir, ajanlar uygular.)

**Harness ne demek?** Kelime karşılığı **koşum takımı**: atı arabaya bağlayan donanım. Yazılımda
ajanın *etrafına* kurduğun her şeydir: hangi talimatı okuduğu, hangi araçlara erişebildiği,
işini kimin ve neyin denetlediği. Model satıcının malıdır; **harness senindir.** Bu metnin tamamı
tek bir denklemin ikinci terimini anlatır: **Ajan = Model + Harness.**

## Türkçe çalışma metni

### 1. Deney: boş depodan bir milyon satıra
İlk **commit** (depoya kaydedilen tek bir değişiklik paketi) Ağustos 2025 sonunda boş bir depoya
atıldı. İskeleti bile Codex üretti: depo yapısı, **CI** (sürekli entegrasyon: her değişiklikte testleri ve
denetimleri otomatik koşan sistem), biçimlendirme kuralları, uygulama çatısı ve
ajanlara yol gösteren ilk **AGENTS.md** dosyası (deponun ajanlar için yazılmış talimat/harita
dosyası; bu projedeki karşılığı `CLAUDE.md`). Beş ay sonra: ~1 milyon satır kod, ~1.500
birleştirilmiş **PR** (pull request: "şu değişikliği ana koda al" isteği), mühendis başına günde
ortalama 3,5 PR. Ekip 3 kişiden 7 kişiye çıktıkça mühendis başına verim **düşmedi, arttı**. Ürün
gerçek: şirket içi günlük kullanıcıları ve dış **alfa testçileri** (ürünü henüz olgunlaşmadan
kullanan dış deneyciler) var; yazarların tahminine göre elle yazmaktan ~10 kat hızlı.
Temel ilke: **hiç elle yazılmış kod yok**. Bu kısıt bilinçli seçildi, çünkü ekip
ancak böylece ajanların güvenilir çalışması için gereken her şeyi kurmak
zorunda kaldı.

> ⚠️ Bu paragraftaki sayılar (**~10 kat**, günde **3,5 PR**, **1 milyon satır**) yazının kendi
> beyanıdır, bağımsız ölçüm değildir. Nasıl okunacakları §11'de; şimdilik şunu akılda tut: **kısıt
> ("hiç elle kod yok") bir başarı ölçüsü değil, harness'a yatırım yapmayı zorunlu kılan bir
> tasarım kararıdır.**

### 2. Mühendisin yeni rolü
İlk ilerleme beklenenden yavaştı. Sebep Codex'in yetersizliği değil, **ortamın yeterince
tanımlanmamış** olmasıydı. Ekip şunu öğrendi: bir şey başarısız olduğunda çözüm asla
"daha çok dene" demek değil; şu soruyu sormak: **"Hangi yetenek eksik? Bu eksiği ajan
için hem okunur (legible) hem de zorlanabilir (enforceable) nasıl yaparız?"**
Buradaki iki kelime metnin çekirdeğidir: **okunur (legible)** = bilgi ajanın çalışırken
görebildiği bir yerde duruyor mu; **zorlanabilir (enforceable)** = kural, ajan unutsa bile
makine tarafından uygulatılabiliyor mu.
İnsanlar sistemle neredeyse yalnızca **istem** (prompt: modele verilen görev metni) üzerinden
konuşur: görevi tarif et, ajanı çalıştır, PR açtır. PR'ı bitirmek için Codex kendi değişikliğini
yerelde inceler, ek ajan incelemeleri ister, gelen geri bildirime yanıt verir ve bu inceleme-düzeltme
döngüsünü bütün inceleyiciler tatmin olana kadar sabırla tekrarlar. Yazı buna esprili bir ad
veriyor: "Ralph Wiggum döngüsü" (adını Simpsonlar dizisinin saf ama yılmayan karakterinden alır).
İnsanlar da PR inceleyebilir ama bu zorunlu değil; incelemeyi büyük ölçüde ajanlar birbirine yapar.

```svg
<svg viewBox="0 0 640 226" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Harness döngüsü: insan yönlendirir, ajan yürütür">
  <defs>
    <marker id="okC" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#186399"/></marker>
    <marker id="okG" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#25804C"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Harness = Guide'lar + Sensor'lar</text>
  <text x="320" y="43" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">İnsan yönlendirir · Ajan yürütür</text>
  <rect x="34" y="112" width="176" height="88" rx="12" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="122" y="148" text-anchor="middle" font-size="15" font-weight="700" fill="#186399" font-family="system-ui">İNSAN</text>
  <text x="122" y="170" text-anchor="middle" font-size="11" fill="#1C3247" font-family="system-ui">niyet · öncelik</text>
  <text x="122" y="186" text-anchor="middle" font-size="11" fill="#1C3247" font-family="system-ui">doğrulama · harness</text>
  <rect x="430" y="112" width="176" height="88" rx="12" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="518" y="152" text-anchor="middle" font-size="15" font-weight="700" fill="#4F46E5" font-family="system-ui">AJAN</text>
  <text x="518" y="174" text-anchor="middle" font-size="11" fill="#1C3247" font-family="system-ui">kodu yürütür</text>
  <line x1="210" y1="134" x2="424" y2="134" stroke="#186399" stroke-width="1.8" marker-end="url(#okC)"/>
  <text x="317" y="106" text-anchor="middle" font-size="11.5" font-weight="600" fill="#186399" font-family="system-ui">Guide'lar → ileri besleme</text>
  <text x="317" y="122" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">kural · spec · örnek</text>
  <line x1="424" y1="178" x2="216" y2="178" stroke="#25804C" stroke-width="1.8" marker-end="url(#okG)"/>
  <text x="317" y="197" text-anchor="middle" font-size="11.5" font-weight="600" fill="#25804C" font-family="system-ui">Sensor'lar ← geri besleme</text>
  <text x="317" y="213" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">test · lint · CI · derleyici</text>
</svg>
```

**Diyagramdaki iki kelimenin tanımı** (bütün metnin iskeleti bu ikisidir):

- **Guide (yönlendirici):** iş **başlamadan önce** devreye girer, "ne yapılacağını" söyler.
  Kural dosyaları, spesifikasyon, örnek kod, AGENTS.md. Trafik ışığı gibidir: önden uyarır,
  ama sürücü kırmızıda geçebilir.
- **Sensor (algılayıcı):** iş **bittikten sonra** devreye girer, sonucu **ölçüp** bildirir.
  Test, lint (kod denetleyici), CI, derleyici. Kaza tespit kamerası gibidir: olayı sonradan
  yakalar, ama yakaladığını kimse tartışamaz.

Guide ileri besleme (feed-forward), sensor geri beslemedir (feedback). Harness ikisinin toplamıdır;
yalnız guide kurarsan ajan unutur, yalnız sensor kurarsan ajan ne yapacağını bilmeden dener.

### 3. Uygulamanın kendisini ajana okunur yapmak
Ajanlar daha çok kod üretmeye başlayınca darboğaz, insanların **QA** (quality assurance: ürünü
elle deneyip doğrulama) kapasitesi oldu. Çözüm: uygulamanın arayüzünü, loglarını ve metriklerini
Codex'in DOĞRUDAN okuyabileceği hâle getirmek.
- Uygulama her git çalışma kopyasında (worktree) ayrı ayağa kalkabiliyor; ajan kendi
  değişikliğini denemek için uygulamanın ayrı bir kopyasını çalıştırabiliyor.
- Chrome DevTools Protokolü (tarayıcının geliştirici araçlarını dışarıya açan arayüz)
  ajanın çalışma ortamına bağlandı: **DOM** (Document Object Model: tarayıcının sayfayı
  tuttuğu ağaç yapısı) anlık görüntüleri, ekran görüntüleri ve sayfada gezinme. Böylece ajan
  hatayı kendisi yeniden üretiyor, düzeltmeyi kendisi doğruluyor.
- Loglar, metrikler ve izler (traces: bir isteğin sistemde izlediği yol) de ajana açıldı.
  Bunun için her worktree'ye özel, geçici bir gözlemlenebilirlik (observability) yığını
  kuruldu; yani logları ve metrikleri toplayıp sorgulanabilir kılan bir araç seti (LogQL/PromQL:
  log ve metrik sorgu dilleri). Böylece "servis açılışı 800ms altında tamamlansın"
  gibi istekler ajan için ÇÖZÜLEBİLİR görevlere dönüşüyor.
- Codex bazen tek bir görev üzerinde 6 saatten uzun çalışıyor (çoğu zaman insanlar uyurken).

### 4. Bilginin asıl kaydı depodur (AGENTS.md bir harita, ansiklopedi değil)
"Tek dev AGENTS.md" yaklaşımı denendi ve tahmin edilebileceği gibi başarısız oldu:
- Bağlam kıt bir kaynaktır: dev bir talimat dosyası, görevi ve kodu bağlamın dışına iter.
- Her şey "önemli" olunca hiçbir şey önemli olmaz.
- Hemen eskir; tek parça dev bir metin otomatik olarak doğrulanamaz, sapma (drift) kaçınılmazdır.
Çözüm: ~100 satırlık AGENTS.md yalnızca bir İÇİNDEKİLER TABLOSU olur; ayrıntılı bilgi düzenli bir
docs/ dizininde durur (design-docs, exec-plans/active+completed, product-specs,
references, QUALITY_SCORE, RELIABILITY, SECURITY...). Planlar da birinci sınıf belgedir:
küçük işler için geçici planlar, karmaşık işler için depoya kaydedilen ExecPlan'ler
(ilerleme ve karar günlükleriyle). Bu düzen **kademeli açma** (progressive disclosure: bilgiyi
gerektiği anda vermek) sağlar: ajan küçük, sabit bir giriş noktasından başlar ve nereye bakacağını
oradan öğrenir. Bu düzen de otomatik olarak zorlanır: **linter'lar** (kodu/metni kurallara göre
otomatik denetleyen küçük programlar) ve CI işleri, bilgi tabanının güncel olduğunu ve belgelerin
birbirine doğru bağlantı verdiğini doğrular; düzenli aralıklarla çalışan bir "doküman bahçıvanlığı"
(doc-gardening) ajanı eskimiş belgeleri tarayıp düzeltme PR'ları açar.

```svg
<svg viewBox="0 0 640 230" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Repo bilgi sistemi: AGENTS.md haritası ve docs/ ağacı">
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Repo bilgi sistemi: kademeli açma</text>
  <rect x="200" y="42" width="240" height="52" rx="12" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="320" y="66" text-anchor="middle" font-size="14" font-weight="700" fill="#186399" font-family="system-ui">AGENTS.md</text>
  <text x="320" y="84" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">~100 satır · yalnızca İÇİNDEKİLER</text>
  <line x1="320" y1="94" x2="320" y2="116" stroke="#C3D3DF" stroke-width="1.5"/>
  <rect x="200" y="116" width="240" height="42" rx="10" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="320" y="142" text-anchor="middle" font-size="13" font-weight="600" fill="#4F46E5" font-family="system-ui">docs/ (derin bilgi)</text>
  <line x1="320" y1="158" x2="320" y2="172" stroke="#C3D3DF" stroke-width="1.5"/>
  <line x1="85" y1="172" x2="555" y2="172" stroke="#C3D3DF" stroke-width="1.5"/>
  <g font-family="system-ui" font-size="10.5" fill="#1C3247">
    <line x1="85" y1="172" x2="85" y2="182" stroke="#C3D3DF"/><rect x="20" y="182" width="130" height="34" rx="8" fill="#F2F6F9" stroke="#C3D3DF"/><text x="85" y="203" text-anchor="middle">design-docs</text>
    <line x1="245" y1="172" x2="245" y2="182" stroke="#C3D3DF"/><rect x="180" y="182" width="130" height="34" rx="8" fill="#F2F6F9" stroke="#C3D3DF"/><text x="245" y="203" text-anchor="middle">exec-plans</text>
    <line x1="405" y1="172" x2="405" y2="182" stroke="#C3D3DF"/><rect x="340" y="182" width="130" height="34" rx="8" fill="#F2F6F9" stroke="#C3D3DF"/><text x="405" y="203" text-anchor="middle">references</text>
    <line x1="555" y1="172" x2="555" y2="182" stroke="#C3D3DF"/><rect x="490" y="182" width="130" height="34" rx="8" fill="#F2F6F9" stroke="#C3D3DF"/><text x="555" y="203" text-anchor="middle">kalite · güvenlik</text>
  </g>
</svg>
```

### 4.1. Bu ilkenin arıza modeli: talimat şişmesi → bağlam çürümesi
"Harita, ansiklopedi değil" güzel bir ilke ama **ihlal edilmesi kolay**, çünkü her ihlal o an
*iyi bir fikir* gibi görünür. Her yeni kural tek başına makuldür; hepsinin toplamı zehirdir. **Thoughtworks
Technology Radar** (bir danışmanlık şirketinin altı ayda bir yayımladığı, teknikleri "benimse /
dene / yakından bak / dikkat" halkalarına yerleştiren sektör raporu) bu desene bir ad verdi ve
**Caution (dikkat)** halkasına koydu:

> **Talimat şişmesi (instruction bloat):** proje bağlamını sürekli eklemek, ajanın talimat setini
> büyütür ve sonunda **bağlam çürümesine (context rot)** yol açar.

Zincir şu: dosya büyür → ajanın bağlam penceresinde daha çok yer kaplar → asıl işe ayrılan bağlam azalır →
ve daha kötüsü, **uzun metinde ortadaki talimatlar görünmez olur.** Yani kuralı yazdın, dosyada
duruyor, ama ajan onu fiilen dikkate almıyor. Kural *var gibi görünüyor*; bu arızanın en tehlikeli hâli budur.
(Aynı desenin sohbet bağlamındaki karşılığı Prompt Atölyesi'nin **bağlam mühendisliği** kategorisinde.)

**Panzehir: kademeli bağlam açma (progressive context disclosure).** Her şeyi baştan yükleme;
haritayı küçük tut, ayrıntıyı ajanın **ihtiyaç duyduğunda** gideceği yerlere koy. Bu, §4'ün "AGENTS.md
bir içindekiler tablosudur" ilkesinin uygulamadaki hâlidir: kural dosyası **yol tarifi** verir,
**ansiklopedi** tutmaz.

> **Canlı vaka: bu projeden ve dürüst olmak gerekirse utanç verici.** Bu metnin anlattığı ilkeyi
> uygulayan bu projenin kendi `AGENTS.md`'si **tek bir günde 29 satırdan 85 satıra çıktı** (üç ayrı
> commit; her biri "makul" bir kural ekliyordu). Yani ilkeyi öğreten metin ile onu ihlal eden davranış
> aynı depoda, aynı gün. **Ders:** talimat şişmesi kötü niyetten değil, **iyi niyetin birikiminden**
> doğar ve ancak **ölçülürse** fark edilir (`wc -l AGENTS.md`). Sezgi burada da yanıltır; bkz.
> [`ai-verimlilik-olcumu`](ai-verimlilik-olcumu.md): inandığın şey, ölçtüğün şey değildir.

**Araç sorusu ve bu programın neden araç öğretmediği:** **spec-driven** (şartname-güdümlü: önce
ne yapılacağını yazılı bir şartnameye dökme, sonra ajanı ona göre koşturma) akışı otomatikleştiren
araçlar var (GitHub Spec Kit vb.). Thoughtworks Radar onları **Assess** halkasında tutuyor;
yani "keşfetmeye değer; ne işe yaradığını anlamak için incele, ama henüz üretimde kullanma". Ekipler *deniyor*, üretimde kullanmıyor. Araçlar da çabuk eskiyor: Spec Kit
`v0.10.0` sürümünde bir grup komut seçeneğini (flag) kaldırdı, eski öğreticiler çalışmaz oldu. Sahadan gelen şikâyetler de tanıdık:
*"yapılmış iş yanılsaması üretiyor, bir yığın metin çıkarıyor"*, *"yapı, isteğe sadık çıktı yerine hacim üretiyor"*.

Bu yüzden bu program sana **aracı değil arıza modelini** öğretiyor: araç 6 ayda değişir, "talimat
şişmesi → bağlam çürümesi" değişmez. Radar'ın kendi uyarısı da bunu destekliyor: *"spec-driven
development"* ve *"harness engineering"* gibi terimler anlamları oturmadan tutarsız kullanılıyor
(**anlam yayılması / semantic diffusion**). Kavramı bil, aracı seçerken de bu bilgiyle seç.

**Canlı vaka 2: "Karpathy skills" reposu (2026'nın viral örneği).** İnternette karşılaşacaksın:
Karpathy'nin bir tweet'inden türetilmiş, dört ilkelik **2,3 KB'lık** bir `CLAUDE.md` altı ayda
**194 bin GitHub yıldızı** aldı (üçüncü bir kişinin derlemesi; Karpathy'nin onayı yok ama kaynak dürüstçe
belirtilmiş). İki ders: (1) **Kısalık işe yarıyor:** viral olan şişmiş bir ansiklopedi değil, iki sayfalık
bir harita; "talimat şişmesi"nin tam tersi. (2) **Ama içeriğine dikkat:** dört ilkenin üçü (önce düşün,
önce sadelik, hedef-güdümlü yürütme) modelin *zaten eğitildiği* genel iyi pratiklerdir. Bunlar bu
programda Hafta 3 (spec-driven), atölye ("emin değilsen sor", "her işe ajan gerekmez") ve EDD olarak
derinlemesine işleniyor. Bu dosyayı kendi reponla birleştirmeden önce §4'ün testini uygula:
*hangi satır, senin projenin varsayılan davranıştan nerede ayrıldığını anlatıyor?* Genel bir ilke,
popüler diye talimat dosyana girmemeli. Girerse, 194 bin kişinin beğendiği dosya senin bağlam bütçeni yer.

**Lab (bu depoda, şimdi):** `wc -l AGENTS.md` ve `.claude/rules/` toplamını ölç. Her kural için sor:
*bu, ajanın **varsayılan** davranışından sapıyor mu?* Sapmıyorsa sil, çünkü ajan bunu zaten biliyor.
Sapıyorsa kalsın ama **tek satır + tek örnek** olsun, gerisi için bağlantı ver. Sildiğin her satır,
asıl işe ayrılan bağlama geri döner.

### 5. Ajan okunurluğu (agent legibility) hedeftir
Depodaki her şeyi ajan ürettiği için depo öncelikle ajanın okuyup anlayabileceği biçimde düzenlenir.
Kritik ilke: **ajanın çalışırken bağlamından erişemediği bilgi fiilen YOK demektir.**
Google Docs'ta, Slack'te veya insanların kafasındaki bilgi sistem için görünmezdir; tıpkı
üç ay sonra işe girecek biri için görünmez olacağı gibi. Ekibin ortak karara vardığı o Slack
tartışması mı? Depoya markdown dosyası olarak girmediyse ajan için hiç yaşanmamıştır.
Bu bakış birçok tercihi netleştirdi: ajanın depo içinden tamamen kavrayabildiği "sıkıcı"
(yerleşik, sürprizsiz) teknolojiler seçildi; bazı durumlarda opak (içi görülmeyen) bir kütüphaneyle
boğuşmak yerine işlevin küçük bir alt kümesini ajana yeniden yazdırmak daha ucuz oldu (ör. genel bir
eşzamanlılık paketi yerine, telemetriye bağlı ve %100 test kapsamına sahip kendi yardımcı fonksiyonları).

### 6. Mimariyi ve zevki otomatik kontrollerle zorlamak
Dokümantasyon tek başına tutarlılığı korumaz. Kural: **değişmezleri (invariants)
zorla, nasıl uygulanacağına tek tek karışma.** Örn. "veriyi sınırda parse et" (sisteme girdiği
noktada ayrıştır) kuralı zorunlu, ama hangi kütüphaneyle yapılacağı serbest.
Her iş alanı sabit katmanlara bölünür (Types → Config → Repo → Service → Runtime → UI),
bağımlılık yönleri kesin kurala bağlıdır; her yere dokunan ortak kaygılar (cross-cutting concerns:
oturum/yetki, telemetri, özellik bayrağı/feature flag) tek açık arayüzden (Providers) girer.

```svg
<svg viewBox="0 0 640 160" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Sabit katmanlar, tek yönlü bağımlılık">
  <defs><marker id="okL" markerWidth="9" markerHeight="9" refX="6.5" refY="3" orient="auto"><path d="M0,0 L6,3 L0,6 Z" fill="#5D7789"/></marker></defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Sabit katmanlar: bağımlılık TEK yönlü</text>
  <g font-family="system-ui">
    <rect x="14" y="52" width="84" height="48" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.3"/><text x="56" y="81" text-anchor="middle" font-size="12" font-weight="600" fill="#186399">Types</text>
    <rect x="126" y="52" width="84" height="48" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.3"/><text x="168" y="81" text-anchor="middle" font-size="12" font-weight="600" fill="#186399">Config</text>
    <rect x="238" y="52" width="84" height="48" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.3"/><text x="280" y="81" text-anchor="middle" font-size="12" font-weight="600" fill="#186399">Repo</text>
    <rect x="350" y="52" width="84" height="48" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.3"/><text x="392" y="81" text-anchor="middle" font-size="12" font-weight="600" fill="#4F46E5">Service</text>
    <rect x="462" y="52" width="84" height="48" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.3"/><text x="504" y="81" text-anchor="middle" font-size="12" font-weight="600" fill="#4F46E5">Runtime</text>
    <rect x="558" y="52" width="68" height="48" rx="9" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.3"/><text x="592" y="81" text-anchor="middle" font-size="12" font-weight="600" fill="#4F46E5">UI</text>
    <line x1="98" y1="76" x2="124" y2="76" stroke="#5D7789" stroke-width="1.5" marker-end="url(#okL)"/>
    <line x1="210" y1="76" x2="236" y2="76" stroke="#5D7789" stroke-width="1.5" marker-end="url(#okL)"/>
    <line x1="322" y1="76" x2="348" y2="76" stroke="#5D7789" stroke-width="1.5" marker-end="url(#okL)"/>
    <line x1="434" y1="76" x2="460" y2="76" stroke="#5D7789" stroke-width="1.5" marker-end="url(#okL)"/>
    <line x1="546" y1="76" x2="556" y2="76" stroke="#5D7789" stroke-width="1.5" marker-end="url(#okL)"/>
  </g>
  <text x="320" y="132" text-anchor="middle" font-size="11" fill="#5D7789" font-family="system-ui">Bu projede: types → config → repo → service → ai → web  (ArchUnit zorlar)</text>
</svg>
```

Bunlar özel linter'lar ve yapısal testlerle zorlanır;
linter'ları da Codex yazdı. Linter'lar özel yazıldığı için hata mesajlarına düzeltme talimatı
konur; böylece hata mesajı, ne yapılması gerektiğini doğrudan ajanın bağlamına taşır.
Çarpıcı gözlem: normalde şirket yüzlerce mühendise ulaşınca kurulan bu tür bir mimari,
ajanlarla çalışırken ERKEN bir önkoşul hâline geliyor. Kısıtlar, kod tabanı çürümeden hızlı gitmeyi sağlar.
"İnsanların öncelikli olduğu bir akışta bu kurallar ukalaca gelebilir; ajanlarla ise çarpan etkisi yapar:
bir kez kodlandı mı her yerde birden uygulanır." Çıktı insanın üslup zevkine uymayabilir. Bu
sorun değil; kodun doğru, bakımı yapılabilir ve sonraki ajan çalıştırmaları için okunur olması yeter.

```svg
<svg viewBox="0 0 640 210" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Guide ve Sensor karşılaştırması">
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Guide (ileri besleme) · Sensor (geri besleme)</text>
  <rect x="30" y="44" width="278" height="150" rx="12" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="169" y="72" text-anchor="middle" font-size="14" font-weight="700" fill="#186399" font-family="system-ui">GUIDE →</text>
  <text x="169" y="96" text-anchor="middle" font-size="12" fill="#1C3247" font-family="system-ui">işten ÖNCE devreye girer</text>
  <text x="169" y="116" text-anchor="middle" font-size="12" fill="#1C3247" font-family="system-ui">"ne yapılacağını" söyler</text>
  <text x="169" y="140" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">kural · spec · örnek · AGENTS.md</text>
  <text x="169" y="176" text-anchor="middle" font-size="12.5" fill="#186399" font-family="system-ui">≈ trafik ışığı</text>
  <rect x="332" y="44" width="278" height="150" rx="12" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="471" y="72" text-anchor="middle" font-size="14" font-weight="700" fill="#25804C" font-family="system-ui">← SENSOR</text>
  <text x="471" y="96" text-anchor="middle" font-size="12" fill="#1C3247" font-family="system-ui">işten SONRA devreye girer</text>
  <text x="471" y="116" text-anchor="middle" font-size="12" fill="#1C3247" font-family="system-ui">sonucu ÖLÇÜP bildirir</text>
  <text x="471" y="140" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">test · lint · CI · derleyici</text>
  <text x="471" y="176" text-anchor="middle" font-size="12.5" fill="#25804C" font-family="system-ui">≈ kaza tespit kamerası</text>
</svg>
```

> **İki ekseni karıştırma.** Bu yazının guide/sensor ayrımı **zaman** eksenlidir: guide işten
> *önce*, sensor işten *sonra*. Kardeş metin [`harness-mekanigi.md`](harness-mekanigi.md) §5 aynı
> kelimeleri **bağlayıcılık** ekseninde kullanır: guide *önerir* (model atlayabilir), sensor
> *zorlar* (atlanamaz). İkisi çoğu zaman örtüşür ama her zaman değil. Örneğin istem gönderilmeden önce
> çalışan deterministik bir hook, **zaman** ekseninde guide tarafında, **bağlayıcılık** ekseninde
> sensor tarafındadır. Bir harness parçasını sınıflarken iki soruyu ayrı sor: *ne zaman koşuyor?*
> ve *atlanabilir mi?*

### 7. Verim, birleştirme (merge) felsefesini değiştirir
Ajanların ürettiği iş, insanların dikkatle inceleyebileceğinden fazla olunca klasik kurallar ters
teper. Depo çok az sayıda **bloklayan merge kapısıyla** (kod ana dala girmeden önce geçmesi zorunlu
tutulan otomatik kontroller) çalışır; PR'lar kısa ömürlüdür; **test kararsızlıkları** (flaky test:
aynı kodda bazen geçip bazen kalan test) süreci süresiz durdurmak yerine sonraki çalıştırmalarla ele
alınır. Mantık: **düzeltmeler ucuz, beklemek pahalı.** Yazarın uyarısı: verimi düşük bir ortamda bu
yaklaşım sorumsuzluk olurdu; burada ise (güçlü otomatik sensörler varken) çoğu zaman doğru ödünleşimdir.

### 8. "Ajan üretimi" ne demek ve artan özerklik
Her şey ajan üretimi: ürün kodu, testler, CI, iç araçlar, dokümantasyon, değerlendirme
düzenekleri, inceleme yorumları, depo yönetim betikleri, üretim panoları. İnsan hâlâ döngünün
içinde ama daha üst bir soyutlama katında çalışıyor: önceliklendirir, kullanıcı geri bildirimini kabul
kriterine çevirir, sonuçları doğrular. Ajan zorlandığında bu bir SİNYALDİR: eksik
araç, koruma kuralı (guardrail) ya da belge tespit edilir; eksiği gideren değişiklik yine
Codex'e yazdırılıp depoya eklenir.
Depo yakın zamanda önemli bir eşiği geçti: ajan tek bir prompt'la bir özelliği uçtan uca
götürebiliyor. Adımlar: mevcut durumu doğrula, hatayı yeniden üret, hatayı gösteren video kaydet,
düzelt, uygulamayı kullanarak doğrula, çözümü gösteren ikinci videoyu kaydet, PR aç,
geri bildirime yanıt ver, build hatalarını gider, yalnızca karar gerektiğinde insana
danış (eskale et), birleştir. Yazarın dürüst notu: bu davranış BU deponun yapısına ve
araçlarına sıkı sıkıya bağlı; benzer yatırım olmadan genelleşeceğini varsayma.

### 9. Entropi ve çöp toplama
Ajanlar depodaki mevcut kalıpları, kötü olanlar dahil, çoğaltır; zamanla **sapma** (drift:
kodun kendi kurallarından yavaşça uzaklaşması) kaçınılmaz olur. (**Entropi**: fizikten alınma bir
benzetme; kendi hâline bırakılan bir sistemin düzensizliğinin artması.) Başta ekip her Cuma'yı (haftanın %20'si) "AI slop" (savruk, düşük kaliteli AI çıktısı)
temizliğine ayırıyordu; bu yöntem ölçeklenmedi. Çözüm: "altın ilkeler" (golden principles), yani
depoya yazılmış, otomatik denetlenen, net tavır alan (opinionated) kurallar (ör. elde yazılmış yardımcılar
yerine paylaşılan util paketleri; verinin biçimini tahmine dayanarak yoklamak yasak, ya
sınırda doğrula ya da tipli SDK kullan).
Düzenli aralıklarla arka plan Codex görevleri sapmaları tarar, kalite notlarını
günceller, küçük hedefli **refactor** (davranışı değiştirmeden kodun iç düzenini
iyileştirme) PR'ları açar; çoğu 1 dakikada incelenip otomatik birleştirilir. Bu bir **çöp toplayıcı** gibi çalışır. Teknik borç yüksek faizli kredi
gibidir: küçük taksitlerle sürekli öde, biriktirip sonra bir seferde acı verici büyük temizliklerle uğraşma.

### 10. Hâlâ öğrenilmekte olanlar
Bilinmeyenler: tamamen ajanların ürettiği bir sistemde mimari tutarlılık YILLAR içinde nasıl
değişir? İnsan yargısı en çok nerede fark yaratır ve nasıl kodlanırsa etkisi zamanla katlanarak
büyür? Modeller güçlendikçe sistem nasıl değişir? Netleşen ise şu: yazılım
kurmak hâlâ disiplin ister ama disiplin artık kodda değil **iskelede** (scaffolding)
görünür: kod tabanını tutarlı tutan araçlarda, soyutlamalarda ve geri-besleme döngülerinde.

### 11. ⚠️ ZORUNLU ŞERH: bu bir satıcı vakası, sayıları böyle oku
Yazı OpenAI'ın kendi sitesinde, kendi ürünüyle (Codex) elde ettiği sonucu anlatıyor. Bu, yazıyı
değersiz yapmaz ama iki tür içeriği **ayırmanı** zorunlu kılar:

- **Mekanizma anlatımı değerlidir.** AGENTS.md'nin harita olması, guide/sensor ayrımı, ajan
  okunurluğu, altın ilkeler, çöp toplayıcı ajanlar: bunlar ekibin sistemi *nasıl kurduğunu* anlatır ve
  kendi deponda **denenebilir**. Denemenin sonucunu da sen ölçersin.
- **Çarpan iddiaları öz-bildirimdir.** "~10 kat hız" cümlesinin kaynağı bir ölçüm değil,
  **yazarların tahminidir**. [`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md) §1'in merkezî
  bulgusu tam burada önem kazanır: METR'in kontrollü deneyinde geliştiriciler **yavaşladıkları hâlde**
  hızlandıklarını sandılar. Yani hız algısı, kim söylerse söylesin, bu konudaki **en zayıf** kanıt
  sınıfıdır.

**"1 milyon satır" ve "günde 3,5 PR" neden ölçü değil?** İkisi de **vekil metriktir** (proxy
metric: asıl ilgilendiğin şeyi doğrudan değil, dolaylı ölçen sayı). Ölçtükleri şey üretilen
*değer* değil, üretilen **hacimdir**; ajanlar da tam olarak hacmi ucuzlatır. Dolayısıyla bu iki
sayı, AI kullanan bir ekipte **kendiliğinden** şişer ve şişmesi hiçbir şeyi kanıtlamaz. Dahası
[`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md) §6'nın "yayılma vergisi" bulgusu tersini akla
getirir: daha çok satır, daha çok **kopyalanmış** satır da olabilir. Kural (Goodhart yasası):
*bir ölçüt hedefe dönüştüğü an ölçüt olmaktan çıkar.*

**Karşılaştırma grubu yok.** "Elle yazsaydık 10 kat yavaş olurduk" cümlesi bir **karşı-olgudur**
(counterfactual: gerçekleşmemiş alternatif). Ölçmek için aynı ekibin aynı ürünü ajansız yazması
gerekirdi; kimse bunu yapmadı. METR'in Şubat 2026 güncellemesi zaten bu karşı-olgunun sahada artık
**erişilemez** hâle geldiğini söylüyor.

**En güvenilir cümle, satıcının kendi lehine olmayan cümledir.** Yazının §8'deki dürüst notu:
*"bu davranış BU deponun yapısına ve araçlarına sıkı sıkıya bağlı; benzer yatırım olmadan
genelleşeceğini varsayma."* Aynı refleks [`claude-code-koken-miti.md`](claude-code-koken-miti.md)
§4'te de var: markanın kendi aleyhine olan ifadelerine daha çok güven.

> **Pratik sonuç:** Yazıdan **mekanizmayı** al (harita-AGENTS.md, guide/sensor, okunurluk, altın
> ilkeler, çöp toplama); **çarpan iddiasını** ise çöpe atma, *askıya al* (doğrulanana kadar beklet).
> "Bende de 10 kat oldu mu?" sorusunun cevabı yazıda değil, senin telemetrindedir: bu depoda
> `KullanimService.istatistik()`, `./mvnw verify` kırmızı sayısı ve eval puanları. Satıcının vakası
> yön gösterir; kararı senin ölçtüğün sayılar verir.

## Programınla bağlantısı
- **Hafta 1**: "Humans steer, agents execute" (Gün 1), Guides/Sensors çerçevesi bu
  yazının pratiğidir (Gün 2), ajan okunurluğu ilkesi §5'te (Gün 3), ajanın kendini değerlendirmesi
  yerine ayrı bir inceleyiciyle çalışan döngü §2'de (Gün 4).
- **Hafta 2**: AGENTS.md = içindekiler + docs/ yapısı + ADR/ExecPlan kültürü §4'ün birebir uygulaması.
- **Hafta 3**: Spec/plan akışı, Ralph Wiggum inceleme döngüsü, bağlam hijyeni §2 ve §4.
- **Hafta 4**: Katman zorlama (ArchUnit karşılığı §6), minimal merge kapısı uyarısı §7,
  çöp toplama/doc-gardening §9.
- **Bu projenin kendisi** (AI Öğretmen) bu deseni uygular: AGENTS.md haritası,
  docs/adr + exec-plans, ArchUnit bloklayan sensör, prompts/ versiyonlu.

## Öz-sınav (kendini test et)
1. "Humans steer, agents execute" cümlesinde insan mühendisin işi hangi üç şeye dönüşür? Bir şey
   başarısız olduğunda ekibin sorduğu doğru soru neydi? Neden "daha çok dene" **değil**?
   "Okunur (legible)" ve "zorlanabilir (enforceable)" kelimelerini kendi cümlenle tanımla.
2. **Guide ile sensor farkı:** hangisi işten *önce*, hangisi *sonra* devreye girer; hangisi "ne
   yapılacağını" söyler, hangisi "sonucu ölçer"? Bu depodaki `AGENTS.md`/`CLAUDE.md`, ArchUnit
   `MimariTestleri` ve `./mvnw verify` kapısını doğru sınıflara yerleştir. Yalnız guide kurmanın
   ve yalnız sensor kurmanın ayrı ayrı arızası nedir?
3. "Tek dev AGENTS.md" denendi ve başarısız oldu: üç sebebini say. Yerine ne kondu, **kademeli
   açma** (progressive disclosure) ne demek ve bilgi tabanının bayatlamadığını **hangi
   mekanizma** doğruluyor?
4. **Talimat şişmesi → bağlam çürümesi** zincirini adım adım anlat. Bu arızanın en tehlikeli hâli
   neden "kural dosyada duruyor ama ajan onu fiilen okumuyor"dur? Bu depodaki AGENTS.md vakası
   (bir günde 29 → 85 satır) bunu nasıl gösterir ve şişmeyi hangi tek komut görünür kılar?
5. "Ajanın çalışırken bağlamından erişemediği bilgi fiilen yoktur" ilkesi hangi günlük alışkanlığı
   bozar (Slack tartışması, Google Docs'taki karar)? Aynı ilke, "sıkıcı teknoloji tercih et" ve
   "opak bir kütüphaneyle boğuşmak yerine küçük bir alt kümesini ajana yeniden yazdır" kararlarını
   nasıl açıklar?
6. §7'nin "düzeltmeler ucuz, beklemek pahalı" mantığı hangi ön koşul olmadan **sorumsuzluğa**
   dönüşür? Bu ön koşulu §9'un "altın ilkeler + arka plan temizlik ajanları" mekanizmasıyla
   birleştirip, teknik borcun neden "yüksek faizli kredi" gibi ele alındığını açıkla.
7. **Şerh sorusu:** "~10 kat hız", "günde 3,5 PR" ve "1 milyon satır" iddialarını hangi kanıt
   sınıfına koyarsın? Satır ve PR sayısı neden **vekil metriktir** ve AI kullanan bir ekipte neden
   kendiliğinden şişer? Bu iddiaların karşı-olgusu (counterfactual) neden ölçülemez? Yazının
   kendi cümlelerinden hangisi bu şerhi destekler?

## Kaynaklar
- **Ryan Lopopolo (OpenAI, Teknik Kadro)**: "Harness engineering: leveraging Codex in an
  agent-first world" (Harness mühendisliği: ajan öncelikli bir dünyada Codex'ten yararlanmak),
  openai.com, 11 Şubat 2026. *Şerh: satıcının kendi ajanıyla kendi vakasını
  anlattığı birinci ağızdan yazı. Mekanizma anlatımı (AGENTS.md yapısı, guide/sensor, altın
  ilkeler, çöp toplama) değerlidir; "~10 kat hız", "günde 3,5 PR" gibi sayılar **öz-bildirimdir**,
  bağımsız ölçüm değildir (§11).*
- **Thoughtworks Technology Radar**: "instruction bloat" (talimat şişmesi; Caution halkası),
  spec-driven araçlar (Assess halkası), "semantic diffusion" (anlam yayılması) uyarısı; §4.1'in
  kaynağı. *Şerh: Radar bir danışmanlık şirketinin görüşüdür; sektör istatistiği değil, deneyime
  dayalı bir kanaattir.*

### Bu programdaki ilgili kaynaklar
- [`harness-mekanigi.md`](harness-mekanigi.md): aynı harness'ın **çalışan parçaları** (skills,
  subagent'lar, hook'lar). Bu metin ilke düzeyini verir, o metin mekanizma düzeyini.
- [`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md): §11'in ölçüm tarafı (öz-bildirim neden
  kanıt değildir, vekil metrik tuzağı, "kendi telemetrin").
- [`model-ve-caba.md`](model-ve-caba.md): denklemin ilk terimi. Model donmuştur (sen değiştiremezsin), harness senindir.
- [`claude-code-koken-miti.md`](claude-code-koken-miti.md): satıcı anlatısını eleştirel okuma
  refleksinin bir kuruluş hikâyesine uygulanmış hâli.
