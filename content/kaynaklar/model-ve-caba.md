---
baslik: "Model ve Çaba (Effort): Bilgisi mi Yetmedi, Çabası mı?"
yazar: "Lydia Hallie (Anthropic, Claude Code ekibi), @ClaudeDevs yazısından Türkçe çalışma metni"
tarih: "2026-07-19"
dil: "Türkçe kavram özeti (diyagramlı)"
etiketler: 1,7,8
---

### Bu metin ne anlatıyor?
Yapay zekâ asistanında çevirebileceğin iki ayar var: hangi **modeli** kullanacağın ve modelin o iş
için ne kadar **çaba** harcayacağı. İkisi aynı şey sanılır; değildir. Bu metin önce makinenin
içinde ne olup bittiğini basitçe anlatıyor (metin nasıl sayıya çevriliyor, cevap nasıl tek tek
üretiliyor), sonra tek bir pratik soruya bağlıyor: **Yapay zekâ yanlış yaptığında bilgisi mi
yetmedi, çabası mı?** Cevabın çoğu zaman "ikisi de değil" olduğunu da göreceksin: sorun genelde
ona verdiğin talimattadır.

## Neden bu özet
Claude Code'da "cevabı iyileştiren" iki ayar var gibi görünür: **model** ve **çaba (effort)
seviyesi**. Bu programı Claude Code ile yürütüyorsun ve bu iki düğmeyi her gün çeviriyorsun. Ama
ikisinin **farklı şeyleri** kontrol ettiğini bilmeden düğme çevirmek, her arızayı aynı aletle onarmaya
çalışmaktır.
Bu özet, Anthropic'in Claude Code ekibinden Lydia Hallie'nin yazısını çalışma metnine çevirir ve tek
bir karar sorusuna bağlar:

> **Claude yanlış yaptığında: bilgisi mi yetmedi, çabası mı?** Birincisi model kararıdır,
> ikincisi effort kararıdır. Çoğu zaman ise ikisi de değildir: sorun işin daha önceki bir
> adımındadır (upstream, §4).

Kardeş kaynak [FinOps for AI ve Tokenomics](finops-ai-tokenomics.md) model seçimini **maliyet
mimarisi** ekseninde anlatır; bu özet aynı kararın **mekanik zeminini** verir: model ayarı çevirince
makinede *fiilen* ne değişir, effort ayarı çevirince ne değişir? Bu zemini bilmeden karar
çerçevesini yalnızca ezberlersin; zemini bilirsen üzerine akıl yürütebilirsin. Üstelik bu, soyut bir
merak değil: uygulamanın kendisi **ADR-0003 ile model kademeleme** yapıyor. "Hangi işe hangi model"
sorusu senin `OgretmenAiClient` katmanının da sorusu (§6).

Üç diyagram fikri kısaca gösterir; ayrıntı metindedir.

## Türkçe çalışma metni

### 1. İki ayar, tek soru
İki ayar da "cevabı iyileştirir" gibi görünür ama aynı şeyi yapmazlar. (**Effort** = çaba
seviyesi; asistanın bir isteği "bitti" saymadan önce ne kadar iş yapacağını belirleyen ayar.
Türkçede "çaba" diyoruz, arayüzlerde İngilizcesiyle karşına çıkacağı için ikisini birlikte
kullanıyoruz.)

- **Model** = daha büyük model, daha yetenekli. Bu doğru, ama hikâyenin yalnızca yarısı.
- **Effort** ≠ sadece "daha uzun düşünme". Effort, Claude'un istek üzerinde yaptığı **TOPLAM işi**
  kontrol eder: kaç dosya okuduğu, ne kadar doğruladığı, sana dönmeden çok-adımlı işte ne kadar
  ilerlediği. Düşük effort'ta Claude kendi başına token harcamak yerine **SANA sormayı** tercih eder.

Yani iki eksen ayrıdır: **model ≈ ne kadar yetenekli, effort ≈ ne kadar titiz** (§4'te ayrıntılı
anlatılır). Bu ayrımı iyi kavramak için önce makinenin içine bakmak gerekir.

### 2. Çıkarım (inference) mekaniği: model ayarının zemini
Claude'a bir şey sorduğunda istek **tek API çağrısında** paketlenir: sistem promptu + araç
tanımları + CLAUDE.md + konuşma geçmişi + okunan dosyalar. Model bunu düz metin olarak **GÖRMEZ**:

- Önce **tokenizasyon** (token'lara ayırma) gelir: metin parçalara bölünür. Bu parçalara
  **token** denir; her biri kabaca bir kelime ya da bir kelime parçası kadardır. Her parça sabit bir
  sözlükten bir **TAMSAYIYA** eşlenir (`const`→1978 gibi; buradaki rakamlar **temsilîdir**,
  gerçek sözlük değerleri değil). O andan itibaren isteğin bir **tamsayı dizisidir**.
- Modelin işi tektir: sözlükteki **HER token için** bir olasılık hesaplayıp sıradakini seçmek.
  "const x = await" dizisinden sonra "fetch" yüksek olasılık alır, "banana" sıfıra yakın.
- Olasılıkları üreten şey **ağırlıklardır (weights/parameters)**: milyarlarca sayı, büyük matrisler
  hâlinde. Tek bir token tahmini, girdinin o matrislerden geçmesi demektir: uzun bir **matris
  çarpımı zinciri**.

Buradan iki ders çıkar ve ikisi de bu programın başka yerlerine bağlanır:

**Birinci ders: ağırlıklar eğitimde yazılır, kullanımda SALT-OKUNURDUR.** Prompt'un, CLAUDE.md dosyan
ya da bağlama koyduğun başka hiçbir şey ağırlıkları **değiştirmez**. "Inference" (çıkarım) kelimesi tam bunu
anlatır: eğitim bitti, ağırlıklar donmuş, model kullanılıyor. Bunun pratik sonucu:

> **Bağlam yönlendirir (steering), öğretmez (teaching).** Eğitim verisinde olmayan bir kütüphaneyi
> bağlama koyarsan Claude onu **o istekte** kullanır, ama model hiçbir şey ÖĞRENMEMİŞTİR; sonraki
> istekte bundan iz kalmaz. RAG da tam olarak budur (`hafta08` konusu): **getirme**dir, öğretme değil.
> "Modele dokümanlarımızı öğrettik" cümlesi teknik olarak yanlıştır; "her isteğe dokümanlarımızı
> iliştiriyoruz" doğrusudur.

**İkinci ders: halüsinasyonun doğası.** Var olmayan bir API'yi kendinden emin biçimde çağırmak,
başarısız bir "arama" değildir: model bir veritabanında arayıp bulamamış değildir. Olan şey,
ağırlıkların eğitimdeki örüntülerden **MAKUL GÖRÜNEN bir token dizisi** üretmesidir; makul görünmek ile var olmak ayrı
şeylerdir. Bu, [`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md) §1'in "olasılıksal çıktı"
zemininin mekanik açıklamasıdır.

Son parça: model cevabı tek seferde üretmez. Bir token tahmin eder, onu diziye ekler ve **TÜM
hesabı yeniden koşar**. 200 token'lık bir cevap = ağırlıklardan **200 ayrı geçiş**. Beklediğin süre
ve ödediğin çıktı token maliyeti buradan gelir (bkz. **Diyagram 1**).

> **Model ayarının fiilen yaptığı şey:** isteğini hangi **donmuş ağırlık setinin** karşılayacağını
> ve **token başı fiyatı** seçmek. YAPMADIĞI şey: kaç token üretileceğine karar vermek. Bunu
> etkileyen ayar **effort'tur** (§3).

## Diyagram 1. Çıkarım döngüsü: donmuş ağırlıklardan tek tek geçiş
Metin tamsayı dizisine çevrilir, donmuş ağırlıklardan geçer, TEK token seçilir, diziye eklenir ve
tüm hesap baştan yapılır. 200 token'lık cevap = 200 geçiş.

```svg
<svg viewBox="0 0 660 344" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Çıkarım döngüsü: prompt ve bağlamdan oluşan metin önce tokenizasyonla tamsayı dizisine çevrilir, bu dizi eğitimde yazılmış ve kullanımda salt-okunur olan donmuş ağırlıklardan yani milyarlarca sayıdan oluşan matris çarpımı zincirinden geçer, sözlükteki her token için olasılık hesaplanır, tek bir token seçilip diziye eklenir ve tüm hesap baştan koşar; 200 token'lık bir cevap ağırlıklardan 200 ayrı geçiş demektir, bekleme süresi ve çıktı maliyeti buradan gelir">
  <defs>
    <marker id="mce-1a" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#186399"/></marker>
    <marker id="mce-1b" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#B3402F"/></marker>
  </defs>
  <text x="330" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Çıkarım döngüsü: model metni değil, tamsayı dizisini görür</text>
  <text x="330" y="44" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Ağırlıklar eğitimde yazılır, kullanımda SALT-OKUNUR; bağlam yönlendirir, öğretmez</text>

  <rect x="16" y="60" width="150" height="46" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="91" y="79" text-anchor="middle" font-size="10.5" font-weight="700" fill="#186399" font-family="system-ui">Metin</text>
  <text x="91" y="96" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">prompt + CLAUDE.md + dosyalar</text>
  <line x1="166" y1="83" x2="194" y2="83" stroke="#186399" stroke-width="1.8" marker-end="url(#mce-1a)"/>

  <rect x="196" y="60" width="140" height="46" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="266" y="79" text-anchor="middle" font-size="10.5" font-weight="700" fill="#186399" font-family="system-ui">Tokenizasyon</text>
  <text x="266" y="96" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">parça → sözlükten tamsayı</text>
  <line x1="336" y1="83" x2="364" y2="83" stroke="#186399" stroke-width="1.8" marker-end="url(#mce-1a)"/>

  <rect x="366" y="60" width="278" height="46" rx="9" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.5"/>
  <text x="505" y="79" text-anchor="middle" font-size="10.5" font-weight="700" fill="#1C3247" font-family="system-ui">Tamsayı dizisi: [… 1978, 4293 …]</text>
  <text x="505" y="96" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">rakamlar temsilî; istek artık sayılardır</text>
  <line x1="505" y1="106" x2="440" y2="130" stroke="#186399" stroke-width="1.8" marker-end="url(#mce-1a)"/>

  <rect x="110" y="132" width="440" height="64" rx="11" fill="#EEF0FF" stroke="#4F46E5" stroke-width="2"/>
  <text x="330" y="156" text-anchor="middle" font-size="12.5" font-weight="700" fill="#4F46E5" font-family="system-ui">DONMUŞ AĞIRLIKLAR (weights) · SALT-OKUNUR</text>
  <text x="330" y="172" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">milyarlarca sayı · uzun bir matris çarpımı zinciri</text>
  <text x="330" y="186" text-anchor="middle" font-size="9.5" font-weight="600" fill="#B3402F" font-family="system-ui">prompt / bağlam bunları DEĞİŞTİRMEZ: inference budur</text>
  <line x1="200" y1="196" x2="130" y2="218" stroke="#186399" stroke-width="1.8" marker-end="url(#mce-1a)"/>

  <rect x="16" y="220" width="212" height="46" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="122" y="239" text-anchor="middle" font-size="10.5" font-weight="700" fill="#186399" font-family="system-ui">Sözlükteki HER token'a olasılık</text>
  <text x="122" y="256" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">"fetch" yüksek · "banana" ≈ 0</text>
  <line x1="228" y1="243" x2="254" y2="243" stroke="#186399" stroke-width="1.8" marker-end="url(#mce-1a)"/>

  <rect x="256" y="220" width="170" height="46" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="341" y="239" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">TEK token seç</text>
  <text x="341" y="256" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">sıradaki en olası parça</text>
  <line x1="426" y1="243" x2="452" y2="243" stroke="#186399" stroke-width="1.8" marker-end="url(#mce-1a)"/>

  <rect x="454" y="220" width="190" height="46" rx="9" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="549" y="239" text-anchor="middle" font-size="10.5" font-weight="700" fill="#C77A17" font-family="system-ui">Diziye ekle</text>
  <text x="549" y="256" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">ve TÜM hesap yeniden koşar</text>
  <path d="M596,218 C640,180 636,158 554,152" fill="none" stroke="#B3402F" stroke-width="1.8" stroke-dasharray="6 4" marker-end="url(#mce-1b)"/>
  <text x="638" y="196" text-anchor="end" font-size="8.5" font-weight="600" fill="#B3402F" font-family="system-ui">başa dön</text>

  <rect x="60" y="296" width="540" height="28" rx="8" fill="#1C3247"/>
  <text x="330" y="315" text-anchor="middle" font-size="11" font-weight="700" fill="#FFFFFF" font-family="system-ui">200 token'lık cevap = ağırlıklardan 200 AYRI GEÇİŞ; bekleme ve maliyet buradan</text>
</svg>
```

### 3. Effort mekaniği: "toplam iş" düğmesi
Effort'u anlamanın anahtarı şudur: Claude'un ürettiği her şey, §2'deki **aynı döngüden çıkan
sıradan çıktı token'larıdır**. Bu token'lar üç biçimde görünür:

1. **Düşünme (thinking)** token'ları,
2. **Araç çağrıları** (Read/Edit blokları),
3. **Sana yazılan metin**.

Üçünün de fiyatı aynıdır. Düşünme token'ları o turun geri kalanında **bağlamda kalır**: Claude kod
yazarken, önceki muhakemesi de tıpkı okuduğu bir dosya gibi girdinin parçasıdır.

Effort seviyesi modele **isteğin parçası olarak gönderilir**; model her seviyede nasıl
davranacağını **eğitimde öğrenmiştir**. Yani davranışın kendisi de donmuş ağırlıklardadır. Effort'un
ayarladığı şey: **işi bitti saymadan önce ne kadar kapsamlı ve ne kadar emin olması gerektiği.**
Yüksek güven eşiği daha çok token ister: daha çok dosya okumak, doğrulamak, çift kontrol etmek.

İki incelik:

- **Plan donuk değildir.** Yüksek effort'ta Claude plan yapar ama planı körü körüne izlemez: 3
  hipotezli bir hata ayıklamada 1. adım hatayı bulursa "2 ve 3'e gerek kalmadı" deyip atlar. Claude
  Code'daki görev listelerinin iş ortasında değişmesinin sebebi budur.
- **Yüksek effort ≠ şişirme.** Yüksek effort çift-kontrol olasılığını artırır ama basit işte
  token kullanımını şişirmez, çünkü "aşırı düşünme" (overthinking) eğitimde
  **özellikle izlenen bir bozulmadır**.

Pratik kural: çoğu iş için modelin **varsayılan effort'u doğrudur**. Effort'u her görevde tek tek
oynadığın bir düğme olarak değil, **alan/tercih bazlı bir el ayarı** olarak kullan.

### 4. Karar çerçevesi: asıl ders
Claude yanlış yaptığında ilk refleks ayar değiştirmek **OLMAMALI**. Sıra şudur (bkz. **Diyagram 2**):

**Adım 0: önce bağlama bak.** Prompt muğlak mı? Doğru araçlara bağlı mı? Doğru skill'ler (beceri
paketleri) yüklü mü? Effort'u yükseltmek "gereken" iş, genelde **upstream'de bozuktur**. Upstream
(yukarı akış), sorunun asıl kaynağının işin *başındaki* adımlarda olması demektir: sen sonucu
düzeltmeye çalışırken bozukluk daha yukarıda durur. Yani model düşük çabadan değil, kötü tarif
edilmiş bir işten dolayı yanılıyordur. Bu, H1'in (ilk haftanın) harness dersidir: ortam ve niyet tasarımı
([`harness-engineering-openai.md`](harness-engineering-openai.md) §2).

**Adım 1: bağlam temizse tek soru kalır. Bilgisi mi yetmedi, çabası mı?**

- **Model sorunu (bilgi):** problem gerçekten zordur: ince bir hata, yabancı bir alan, mimari bir
  karar. Küçük model, ne kadar bağlam versen de **kendinden emin ama YANLIŞ** kalır. Büyük model
  belirsizlikle daha iyi başa çıkar; küçük modelde ise **spesifik talimat** daha iyi çalışır. Tersi
  de geçerli: rutin iş (net tarif edilebilen düzenleme, mekanik değişiklik) küçük modele gider.
  **Gerekmeyen yeteneğe para verme**; bir iş rutinleştiyse modeli küçült.
- **Effort sorunu (çaba):** dosya atladı, testleri koşmadı, işini doğrulamadı. Özellikle
  varsayılanın **ALTINDA** bir effort seçtiysen, yükselt.

> **Üçüncü bir arıza, ne bilgi ne çaba: dalkavukluk (sycophancy).** Claude'a "bence bu tasarım
> doğru, sen ne dersin?" diye sorduğunda yanlışın kaynağı bazen ne bilgi eksikliği ne de düşük
> çabadır: model, **senin belirttiğin görüşe katılmaya** eğilimlidir. Bu eğilim eğitimden, insan
> geri bildirimiyle pekiştirmeli öğrenmeden (RLHF) gelir: insanlar onaylanmayı düzeltilmeye
> tercih ettikçe ödül "katıl" yönüne kayar. Model ya da effort değiştirmek bunu düzeltmez;
> soruyu soruş biçimini değiştirmek düzeltir: tercihini söyleme ("bir meslektaş X öneriyor"), önce
> en güçlü karşı argümanı iste. Bunun nasıl ölçüleceği (aynı iddia "benim görüşüm" ve "başkasının
> görüşü" diye sorulunca aynı hüküm çıkmalı) [evals-llm-observability.md](evals-llm-observability.md)
> §10.4'te anlatılıyor.

**Yazarın benzetmesi** (bkz. **Diyagram 3**):

- **Fable = uzmanın uzmanı:** çok az kişinin çözebildiği zor problemler için.
  Düşük effort'ta bile kimsenin görmediğini görür. En pahalısıdır; onu gerçekten gerektiren işe sakla.
- **Opus = uzman.** Düşük effort'ta Opus = **uzmanla 5 dakika**: kodunda olmayan deneyimi getirir,
  ama her dosyaya dikkatle bakmaz.
- **Sonnet = çok iyi bir generalist** (her işten anlayan, hiçbirinin dar uzmanı olmayan kişi).
  Yüksek effort'ta Sonnet = **bütün bir öğleden sonrasını ayırabilen generalist**: her şeyi okur, çalıştırır,
  çift kontrol eder.

Hiçbiri evrensel olarak "daha iyi" değildir: **model ≈ ne kadar yetenekli, effort ≈ ne kadar
titiz.**

> **Adları değil, kademeleri ezberle.** Fable / Opus / Sonnet isimleri ve sürüm numaraları hızla
> değişir; altı ay sonra listedeki adlar başka olabilir. Kalıcı olan **üç kademe fikridir**:
> *generalist → uzman → uzmanın uzmanı*. Her kademenin fiyatı ve getirisi ayrıdır. Yeni bir
> model çıktığında sorulacak soru basittir: bu model hangi kademenin yerine geçiyor?

**Maliyet eğrilerini burada TEKRARLAMIYORUZ.** "Rutin işte iki model de doğru yapar, büyük olan
fazladan doğrulamayla daha pahalıya gelir → küçült; zor işte denklem tersine döner: küçük model
yeteneğinin sınırında boşa deneme turu (iterasyon) harcar, büyük model aynı kaliteye daha az adımda ulaşır → iş başına
toplam maliyet daha düşük çıkabilir" dersi [`finops-ai-tokenomics.md`](finops-ai-tokenomics.md)
§5'te ve oradaki Diyagram 3'te zaten var; o kısım için oraya bak. Buradaki **EK** fikir şudur:

> **Effort, maliyet eğrisinde ne kadar ileri gitmeye RAZI olduğunu belirler, modelin gitmek
> zorunda olduğu mesafeyi değil.** Tek sert tavan `max_tokens`'tır ve o **kör bir alettir**: üretimi
> yarıda keser. Bütçe ipuçları ve istekteki "kısa tut" türü yönergeler ise yumuşak kontrollerdir
> (model bunlara uymayı eğitimde öğrenmiştir).

## Diyagram 2. Karar akışı: önce bağlam, sonra "bilgi mi çaba mı"
"Sonuç yanlış" ile "ayarı değiştir" arasına iki durak girer: önce upstream (bağlam), sonra doğru
teşhis. Rutinleşen iş için de üçüncü bir yol vardır: modeli küçült.

```svg
<svg viewBox="0 0 660 424" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Karar akışı: sonuç yanlışsa ilk adım ayar değiştirmek değil bağlama bakmaktır, prompt muğlak mı doğru araçlara ve skill'lere bağlı mı diye kontrol edilir; bağlam bozuksa düzeltilir ve model ile effort ayarına dokunulmaz; bağlam temiz ama sonuç hâlâ yanlışsa tek soru sorulur: bilgisi mi yetmedi yoksa çabası mı; bilgi sorunuysa yani ince hata yabancı alan veya mimari karar gibi gerçekten zor bir işse model büyütülür, çaba sorunuysa yani dosya atladı testleri koşmadı işini doğrulamadıysa effort yükseltilir; iş rutinleştiğinde ise model küçültülür çünkü gerekmeyen yeteneğe para verilmez">
  <defs>
    <marker id="mce-2a" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#5D7789"/></marker>
    <marker id="mce-2b" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#25804C"/></marker>
    <marker id="mce-2c" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#B3402F"/></marker>
  </defs>
  <text x="330" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">"Sonuç yanlış" → ilk refleks ayar DEĞİL</text>
  <text x="330" y="44" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Önce upstream'i temizle; sonra teşhisi koy: bilgi mi, çaba mı?</text>

  <rect x="246" y="58" width="168" height="32" rx="8" fill="#B3402F" stroke="#1C3247" stroke-width="1.2"/>
  <text x="330" y="79" text-anchor="middle" font-size="11.5" font-weight="700" fill="#FFFFFF" font-family="system-ui">Sonuç YANLIŞ</text>
  <line x1="330" y1="90" x2="330" y2="104" stroke="#5D7789" stroke-width="1.8" marker-end="url(#mce-2a)"/>

  <rect x="140" y="106" width="380" height="66" rx="10" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.8"/>
  <text x="330" y="126" text-anchor="middle" font-size="11.5" font-weight="700" fill="#C77A17" font-family="system-ui">ADIM 0 · ÖNCE BAĞLAMA BAK (upstream)</text>
  <text x="330" y="143" text-anchor="middle" font-size="9.5" fill="#1C3247" font-family="system-ui">Prompt muğlak mı? · Doğru araçlara bağlı mı? · Doğru skill'ler var mı?</text>
  <text x="330" y="160" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">"effort yükseltmek gereken" iş genelde upstream'de bozuktur</text>

  <line x1="140" y1="150" x2="106" y2="188" stroke="#25804C" stroke-width="1.8" marker-end="url(#mce-2b)"/>
  <rect x="16" y="190" width="176" height="52" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.8"/>
  <text x="104" y="211" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">Bozuk → DÜZELT</text>
  <text x="104" y="228" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">model/effort ayarına dokunma</text>

  <line x1="330" y1="172" x2="330" y2="202" stroke="#5D7789" stroke-width="1.8" marker-end="url(#mce-2a)"/>
  <text x="428" y="192" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">bağlam temiz · hâlâ yanlış</text>

  <rect x="212" y="204" width="236" height="38" rx="9" fill="#1C3247"/>
  <text x="330" y="228" text-anchor="middle" font-size="11.5" font-weight="700" fill="#FFFFFF" font-family="system-ui">Bilgisi mi yetmedi, ÇABASI mı?</text>

  <line x1="270" y1="242" x2="180" y2="272" stroke="#186399" stroke-width="1.8" marker-end="url(#mce-2a)"/>
  <line x1="390" y1="242" x2="480" y2="272" stroke="#5D7789" stroke-width="1.8" marker-end="url(#mce-2a)"/>

  <rect x="16" y="276" width="306" height="88" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.8"/>
  <text x="169" y="296" text-anchor="middle" font-size="11" font-weight="700" fill="#186399" font-family="system-ui">MODEL sorunu (bilgi)</text>
  <text x="169" y="313" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">gerçekten zor: ince hata · yabancı alan · mimari karar</text>
  <text x="169" y="328" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">küçük model kendinden emin YANLIŞ kalır</text>
  <rect x="76" y="336" width="186" height="22" rx="6" fill="#186399"/>
  <text x="169" y="351" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF" font-family="system-ui">→ MODELİ BÜYÜT</text>

  <rect x="338" y="276" width="306" height="88" rx="10" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.8"/>
  <text x="491" y="296" text-anchor="middle" font-size="11" font-weight="700" fill="#C77A17" font-family="system-ui">EFFORT sorunu (çaba)</text>
  <text x="491" y="313" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">dosya atladı · testleri koşmadı · doğrulamadı</text>
  <text x="491" y="328" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">özellikle varsayılanın ALTINDA seçtiysen</text>
  <rect x="398" y="336" width="186" height="22" rx="6" fill="#C77A17"/>
  <text x="491" y="351" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF" font-family="system-ui">→ EFFORT'U YÜKSELT</text>

  <line x1="330" y1="364" x2="330" y2="382" stroke="#25804C" stroke-width="1.8" marker-end="url(#mce-2b)"/>
  <rect x="90" y="384" width="480" height="28" rx="8" fill="#E9F4EE" stroke="#25804C" stroke-width="1.8"/>
  <text x="330" y="403" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">İş RUTİNLEŞTİ → modeli KÜÇÜLT; gerekmeyen yeteneğe para verme</text>
</svg>
```

## Diyagram 3. Uzman matrisi: yetenek × titizlik
Yatay eksen modelin yeteneği, dikey eksen effort'un titizliği. "Uzmanla 5 dakika" ile "generalistin
bütün öğleden sonrası" aynı düzlemin iki ayrı köşesidir; hiçbiri ötekinden her durumda daha iyi değildir.

```svg
<svg viewBox="0 0 660 380" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Uzman matrisi: yatay eksen modelin ne kadar yetenekli olduğunu gösterir, generalist Sonnet'ten uzman Opus'a ve uzmanın uzmanı Fable'a uzanır; dikey eksen effort'un ne kadar titiz olduğunu gösterir, 5 dakikadan bütün öğleden sonraya uzanır; sol üst köşede Sonnet yüksek effort generalistin bütün öğleden sonrasıdır yani her şeyi okur çalıştırır ve çift kontrol eder, alt ortada Opus düşük effort uzmanla 5 dakikadır yani kodunda olmayan deneyimi getirir ama her dosyaya dikkatli bakış değildir, sağda Fable düşük effort'ta bile kimsenin görmediğini görür ama en pahalısıdır ve gerçekten gerektiren işe saklanır; sonuç model ne kadar yetenekli effort ne kadar titiz sorularının cevabıdır ve hiçbiri evrensel olarak daha iyi değildir">
  <defs>
    <marker id="mce-3a" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#5D7789"/></marker>
  </defs>
  <text x="330" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Uzman matrisi: yetenek × titizlik</text>
  <text x="330" y="44" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">İki ayrı eksen: hangi kafa (model) · ne kadar mesai (effort)</text>

  <line x1="70" y1="300" x2="70" y2="66" stroke="#5D7789" stroke-width="1.8" marker-end="url(#mce-3a)"/>
  <line x1="70" y1="300" x2="640" y2="300" stroke="#5D7789" stroke-width="1.8" marker-end="url(#mce-3a)"/>
  <text x="78" y="76" font-size="9" fill="#5D7789" font-family="system-ui">bütün öğleden sonra</text>
  <text x="78" y="292" font-size="9" fill="#5D7789" font-family="system-ui">5 dakika</text>
  <text x="30" y="196" font-size="10.5" font-weight="700" fill="#5D7789" font-family="system-ui" transform="rotate(-90 30 196)" text-anchor="middle">EFFORT: ne kadar titiz</text>

  <rect x="84" y="88" width="240" height="66" rx="10" fill="#E9F4EE" stroke="#25804C" stroke-width="1.8"/>
  <text x="204" y="108" text-anchor="middle" font-size="11" font-weight="700" fill="#25804C" font-family="system-ui">Sonnet + YÜKSEK effort</text>
  <text x="204" y="125" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">bütün öğleden sonrasını ayıran generalist:</text>
  <text x="204" y="141" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">her şeyi okur, çalıştırır, çift kontrol eder</text>

  <rect x="396" y="88" width="240" height="66" rx="10" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.8"/>
  <text x="516" y="108" text-anchor="middle" font-size="11" font-weight="700" fill="#4F46E5" font-family="system-ui">Fable: uzmanın uzmanı</text>
  <text x="516" y="125" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">düşük effort'ta bile kimsenin görmediğini görür</text>
  <text x="516" y="141" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">en pahalı; gerçekten gerektiren işe sakla</text>

  <rect x="250" y="222" width="240" height="66" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.8"/>
  <text x="370" y="242" text-anchor="middle" font-size="11" font-weight="700" fill="#186399" font-family="system-ui">Opus + DÜŞÜK effort</text>
  <text x="370" y="259" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">uzmanla 5 dakika: kodunda olmayan deneyimi</text>
  <text x="370" y="275" text-anchor="middle" font-size="9" fill="#5D7789" font-family="system-ui">getirir ama her dosyaya dikkatle bakmaz</text>

  <text x="160" y="318" text-anchor="middle" font-size="9.5" font-weight="700" fill="#1C3247" font-family="system-ui">generalist</text>
  <text x="160" y="331" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">(Sonnet)</text>
  <text x="360" y="318" text-anchor="middle" font-size="9.5" font-weight="700" fill="#1C3247" font-family="system-ui">uzman</text>
  <text x="360" y="331" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">(Opus)</text>
  <text x="556" y="318" text-anchor="middle" font-size="9.5" font-weight="700" fill="#1C3247" font-family="system-ui">uzmanın uzmanı</text>
  <text x="556" y="331" text-anchor="middle" font-size="8.5" fill="#5D7789" font-family="system-ui">(Fable)</text>

  <rect x="60" y="346" width="540" height="26" rx="8" fill="#1C3247"/>
  <text x="330" y="364" text-anchor="middle" font-size="10.5" font-weight="700" fill="#FFFFFF" font-family="system-ui">model ≈ NE KADAR YETENEKLİ · effort ≈ NE KADAR TİTİZ · evrensel "daha iyi" yok</text>
</svg>
```

### 5. ⚠️ ZORUNLU ŞERH: satıcının kendi yazdığı kaynağı okumak
Bu yazı Anthropic'in **kendi ekibinden** geliyor ve bu, iki farklı okuma gerektirir:

- **Mekanik anlatım güvenilirdir.** Tokenizasyon, donmuş ağırlıklar, effort'un isteğin parçası
  olarak gönderilmesi: bunlar üreticinin **kendi ürününün nasıl çalıştığına** dair, doğrulanabilir
  mimari bilgidir. Bir satıcının kendi iç mekaniği hakkında yalan söylemesi için sebep azdır.
- **Karşılaştırmalı performans iddiaları ise satıcının kendi testleridir.** Yazıdaki "Opus 4.8'in
  varsayılanı, 4.7 ile aynı token harcamasıyla daha iyi sonuç veriyor" ve "Fable, Opus/Sonnet'in
  hiçbir effort seviyesinde bitiremediği işleri bitirdi" cümleleri **bağımsız ölçüm DEĞİLDİR**.
  Yazının kendi ifadesiyle bunlar "in our testing" (bizim testlerimizde) elde edilen sonuçlardır. Ayrıca yazıdaki maliyet eğrisi
  grafikleri, yine yazının **kendi beyanıyla** "yalnızca gösterim amaçlı; gerçek **benchmark**
  (standart karşılaştırma testi) verisi değil".
- **Sürüm numaraları en hızlı eskiyen kısımdır.** Yukarıdaki cümlelerdeki "4.8 / 4.7" bugün
  başka sayılar olabilir. Ezberlenecek olan sürüm değil, iddianın **sınıfıdır**: "satıcının kendi
  testi" etiketi, sürüm ne olursa olsun geçerliliğini korur.

Refleks, [`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md)'nun öğrettiği refleksin aynısıdır:
**satıcı tezi ile bağımsız kanıtı ayır.** NVIDIA-SLM vakasında bu ayrımı bizim aleyhimize işleyen
bir iddia üzerinde yaptık; burada aynısını **lehimize olan** bir üründe yapıyoruz. Ayrımın değeri de
buradadır: refleksi sevdiğin satıcıya da uygulayabiliyorsan refleks gerçekten oturmuştur.

> **Pratik sonuç:** "hangi model/effort bende daha iyi çalışıyor" sorusunun cevabını yazıdan değil,
> **kendi telemetrinden** (kendi kullanım ölçümlerinden) al: `KullanimService.istatistik()` zaten
> model bazında token ve çağrı sayıyor. Satıcının eğrisi yön gösterir; kararı ise senin sayıların belirler.

### 6. Bu projedeki bağ
Bu çerçeve senin için soyut değil, çünkü **uygulamanın kendisi model kademeleme yapıyor**:
ADR-0003, işe göre model seçimini `OgretmenAiClient` katmanında mimari bir karar olarak kaydeder.
"Bilgisi mi yetmedi, çabası mı" sorusu, kendi AI Öğretmen'inin hangi işine hangi modeli
bağlayacağın sorusunun ta kendisidir: quiz değerlendirmesi gibi rutinleşmiş, net tarif edilmiş bir
iş küçük modelin adayıdır; açık uçlu Sokratik tartışma gibi belirsizlik taşıyan bir iş ise büyük modelin.
Karar verirken de §5'in kuralı geçerli: satıcı broşürüne değil, `AI_KULLANIM` tablosundaki kendi
sayılarına bak.

## Öz-sınav (kendini test et)
1. "Effort = daha uzun düşünme" cümlesi neden eksiktir? Effort'un kontrol ettiği **toplam işi**
   üç örnekle say (dosya okuma, doğrulama, sana dönmeden ilerleme) ve düşük effort'taki Claude'un
   token harcamak yerine ne yapmayı tercih ettiğini söyle.
2. Tokenizasyonu anlat: istek modele ulaştığında fiilen **nedir**? `const`→1978 örneğindeki
   rakamlar için hangi niteleme şart (ve neden)?
3. "Ağırlıklar eğitimde yazılır, kullanımda salt-okunurdur" cümlesinden yola çık: CLAUDE.md dosyan
   modelin ağırlıklarını değiştirir mi? "Inference" kelimesi tam olarak neyi anlatır?
4. **"RAG öğretir mi?" tuzağı:** eğitimde olmayan bir kütüphaneyi bağlama koydun, Claude o istekte
   doğru kullandı. Model bir şey öğrendi mi? "Yönlendirme (steering) ≠ öğretme (teaching)" ayrımını
   `hafta08`'in RAG anlatısına bağla. RAG hangisidir?
5. Halüsinasyon neden "başarısız bir arama" değildir? Var olmayan API çağrısını §2'nin mekaniğiyle
   açıkla ve [`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md) §1'in "olasılıksal çıktı"
   zeminine bağla.
6. "200 token'lık cevap = 200 ayrı geçiş" ne demek? Bekleme süresi ve çıktı token fiyatı bu
   mekanikten nasıl doğar? `AI_KULLANIM` tablosunda girdi/çıktı token'larının **ayrı**
   tutulması bu yüzden neden isabetlidir?
7. Model ayarı fiilen **neyi seçer**, neyi **seçmez**? "Kaç token üretileceği" hangi ayarın etki
   alanıdır ve o ayar modele nasıl iletilir (davranış nerede öğrenilmiştir)?
8. Claude yanlış bir sonuç verdi. Karar akışını baştan yürüt: ilk baktığın üç şey nedir ve neden
   "effort'u yükselt" refleksi çoğu zaman bir **upstream** belirtisidir? H1'in harness dersiyle
   ([`harness-engineering-openai.md`](harness-engineering-openai.md) §2) bağla.
9. İki senaryo: (a) Claude ince bir eşzamanlılık hatasında kendinden emin ama yanlış bir teşhis
   koyuyor; (b) Claude düzeltmeyi yaptı ama testleri hiç koşmadı. Hangisi model, hangisi effort
   sorunudur? **ADR-0003 kademelemesi** ile `OgretmenAiClient` için buradan hangi kural çıkar
   (rutinleşen işe ne yapılır)? İş başına maliyetin zor işte neden tersine döndüğünü burada
   **anlatma**; [`finops-ai-tokenomics.md`](finops-ai-tokenomics.md) §5'e yönlendir. Bu metnin EK
   fikri neydi (effort eğride neyi seçer)?
10. "Opus 4.8 varsayılanı, 4.7 ile aynı token harcamasıyla daha iyi sonuç veriyor" cümlesini bir
    mühendis olarak nasıl okursun? "In our testing" (bizim testlerimizde) ibaresi iddiayı hangi
    sınıfa sokar ve mekanik anlatımdan farkı nedir? Bu iddiayı kendi bağlamında hangi araçla doğrularsın
    (`KullanimService.istatistik()` + hangi refleks: [`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md))?

## Kaynaklar
- **@ClaudeDevs, Lydia Hallie (Anthropic, Claude Code ekibi),** "Model and effort in Claude Code:
  knowing more vs. trying harder" ("Claude Code'da model ve effort: daha çok bilmek mi, daha çok
  çabalamak mı", X/Twitter yazısı, Tem 2026). *Şerh: mekanik anlatım güvenilir;
  karşılaştırmalı performans iddiaları "in our testing" sınıfındadır, maliyet eğrisi grafikleri
  yazının kendi beyanıyla gösterim amaçlıdır (§5).*

### Bu programdaki ilgili kaynaklar
- [`finops-ai-tokenomics.md`](finops-ai-tokenomics.md): maliyet eğrileri ve "başarılı çıktı başına
  maliyet"; bu özetin tekrar etmediği maliyet dersi oradadır (§5 + Diyagram 3).
- [`ai-verimlilik-olcumu.md`](ai-verimlilik-olcumu.md): satıcı iddiası okuma refleksi; §5'teki
  şerhin ölçüm tarafı.
- [`harness-engineering-openai.md`](harness-engineering-openai.md): upstream bağlam/ortam tasarımı;
  karar akışının "Adım 0"ı.
- [`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md): olasılıksal çıktı zemini; halüsinasyonun
  güvenlik yüzü.
