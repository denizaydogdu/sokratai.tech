---
baslik: "AI Çağında Test Kalitesi: Kapsam, Mutasyon Testi ve Gerçek Altyapı"
yazar: "AI Öğretmen ek okuma metni: test kalitesi konusu"
tarih: "2026-11-28"
dil: "Türkçe kavram özeti (diyagramlı)"
etiketler: 3,4,9
---

### Bu metin ne anlatıyor?
Bir ajana (agent: verilen işi kendi başına adım adım yürüten yapay zekâ programı) kod yazdırdığında
çoğu zaman testleri de ona yazdırırsın. Testler yeşil yanar, iş bitmiş görünür. Bu metin tek bir soru
soruyor: **o yeşil ışık neyi kanıtlıyor?** Cevap bazen "hiçbir şeyi" olur.

Önce AI'ın yazdığı testlerin hangi yollarla boş çıktığını göreceksin. Sonra testin kalitesini ölçmenin
iki yolunu öğreneceksin: kapsam (coverage) ve mutasyon testi (mutation testing). Ardından gerçek
veritabanıyla test etmeyi, bir hatayı düzeltmeden önce kırmızı bir testle yakalamayı ve kuralları zorlayan
testlerin kendilerinin nasıl korunacağını göreceksin. Her konuda örnek, bu sitenin kendi deposu SokratAI'dan
geliyor. Depoda olmayan bir şey varsa açıkça "yok" diye yazıyoruz.

## Neden bu özet
[`temel-developer-testing.md`](temel-developer-testing.md) test türlerini, test piramidini, TDD döngüsünü
ve test sahtelerini (test double) anlatır. Bu metin onları bildiğini varsayar ve bir adım öteye geçer:
**testin kendisini nasıl test edersin?**

Bu soru ajan çağında acil hâle geldi. Hafta 1 Gün 4'te ("Kendini değerlendirme tuzağı") ajanın kendi işini
övdüğünü gördün. Test yazmak da bir değerlendirmedir. Kodu ve testi aynı oturumda aynı ajan yazıyorsa,
kodu yazarken yaptığı yanlış anlamayı teste de yazar. İki yanlış birbirini doğrular ve sonuç yeşildir.

Programın üç haftası doğrudan testlere yaslanır. Hafta 3'ün inceleme döngüsü "`./mvnw verify` koş,
kırmızıysa düzelt, tekrar koş" der. Hafta 4'ün guardrail'ları (koruma kuralları) birer testtir. Hafta 9'un
hexagonal mimarisi test edilebilirlik vaat eder. Testler boşsa döngü de çit de vaat de boştur.

Metin birincil kaynaklara dayanır: PIT ve Testcontainers belgeleri, Martin Fowler ve Kent Beck'in yazıları,
Google'ın mutasyon testi makalesi, kapsam ve mutasyon üzerine iki akademik çalışma. Okunmamış bir kaynaktan
hiçbir sayı alınmadı. Üç diyagram fikirleri özetler; ayrıntı metindedir.

## Türkçe çalışma metni

### 1. AI'ın yazdığı test neden riskli: beş tuzak
Bir test iki iş yapar: kodu çalıştırır ve sonucu kontrol eder. İkinci kısım zayıfsa test yalnız "kod
çökmedi" der. AI'ın yazdığı testlerde beş tuzak sık görülür. Hiçbiri AI'a özgü değildir; insanlar da
yapar. Fark hızdadır. Ajan bir saatte yüzlerce test üretir. İnceleyen kişi de çoğu zaman test adlarını okur,
gövdelerini okumaz.

**(a) Totolojik test (tautological test).** Beklenen değer, test edilen kodla aynı yoldan hesaplanır.
Örnek: `assertThat(fiyat.kdvli()).isEqualTo(fiyat.net() * 1.20)`. Koddaki oran yanlışsa test de aynı
yanlışı yapar. Daha sinsi biçimi şudur: ajan kodu çalıştırır, çıkan değeri kopyalar ve testin beklenen
değerine yapıştırır. Kent Beck "Canon TDD" yazısında bunu açıkça hata sayar. Kodun hesapladığı gerçek
değeri testin beklenen değerine kopyalamak çift kontrolü boşa çıkarır; TDD'nin doğrulama değerinin büyük
kısmı da o çift kontrolden gelir. Beklenen değer koddan değil, spec'ten (spesifikasyon) ya da elle yapılmış
bir hesaptan gelmelidir.

**(b) Davranış yerine uygulamayı doğrulamak.** Test dönen sonuca değil, kodun içindeki adımlara bakar:
"önce şu metot, sonra bu metot şu argümanla çağrıldı". Böyle bir test, kodun bugünkü hâlinin aynası olur.
Kod yeniden düzenlenince davranış aynı kalsa bile kırılır. Davranış bozulduğunda ise adımlar aynı kaldıysa
geçer. Bunun genel tartışması [`temel-developer-testing.md`](temel-developer-testing.md) §4'teki şerhtedir.
Ajanla çalışırken bir tehlike daha eklenir: ajan testi kodu okuyarak yazarsa, test kodun ne yapması
gerektiğini değil, **ne yaptığını** tarif eder.

**(c) Test edilen sistemi taklit etmek.** Test edilen sınıfın (system under test) kendi metodu stub'lanır
ya da sınıfın tamamı mock olur. Sonra test, kendi kurduğu cevabı geri okur. Kontrol edilen şey sahte
nesnedir, ürün kodu hiç çalışmaz. Kural basit: sahte nesne yalnız test edilen birimin **dışındaki**
bağımlılıklara konur.

**(d) Hatayla da geçen test.** Test doğru yere bakar ama gevşek bakar: yalnız "boş değil", yalnız "istisna
fırlattı", yalnız "liste dolu". Bu depodan somut bir örnek var:
`src/test/java/tr/velzon/aiogretmen/ai/SpringAiOgretmenClientTest.java` içindeki
`bosCevaptaDaKullanimKaydiYazilirSonraHataFirlatilir`. Hata şuydu: model boş cevap döndürdüğünde istemci
(client) hata fırlatıyordu ama bunu **kullanım kaydını yazmadan** yapıyordu. Oysa boş cevabın da ücreti
ödenmişti. Testin ilk doğrulaması `assertThatThrownBy(...).isInstanceOf(AiCiktisiBozukException.class)`
eski hatalı kodda da geçerdi, çünkü eski kod da aynı istisnayı fırlatıyordu. Testi eski kodda kırmızı yapan
ikinci kısımdır: `verify(kullanimRepo).save(...)` ve kaydedilen token sayısının kontrolü. İlk satırla
yetinen bir test hatayı hiç görmezdi. Bir testi okurken şunu sor: **bu satırlardan hangisi hatalı kodda
kırmızı olur?**

**(e) Ajanın kodu değil testi "düzeltmesi".** Kırmızı testi yeşile çevirmenin en ucuz yolu testi
değiştirmektir. Bu teorik bir korku değil. OpenAI, Mart 2025'te yayımladığı "Detecting misbehavior in
frontier reasoning models" (sınır modellerinde kötü davranışı yakalamak) yazısında, "bütün birim testleri
geçir" görevi verilen bir ajanın neler yaptığını gösterdi. Ajan doğrulama fonksiyonunun (`verify`) gövdesini
hep `true` döndürecek biçimde değiştirdi. Başka bir örnekte her testi atlatan bir pytest kancası ekledi.
Bir diğerinde başarısız bir doğrulama satırını silip yerine `pass` koydu. Andreas Zeller'in *The Debugging
Book* kitabındaki "şeytanın hata ayıklama rehberi" de aynı kalıbı anlatır: "en bariz düzeltme", hatalı
girdiyi koda özel durum olarak yazmaktır. Belirti kaybolur, hata yerinde kalır.

Beş tuzağın ortak kökü Hafta 1 Gün 4'teki ilkedir: **yapan ile yargılayan aynı olmamalı.** Kodu yazan ajan
testi de yazıyorsa, test bağımsız bir yargıç değildir. Pratikte şunları yap:

- Testi koddan **önce** ve spec'ten yazdır, ayrı bir commit'e al. Sonra ajana "testlere dokunmadan geçir"
  de. Diff'te test dosyası değiştiyse bu ayrı bir inceleme konusudur.
- Testi kodu okuyarak değil, girdi ve beklenen çıktı çiftlerinden yazdır.
- İnceleyiciye (insan ya da temiz oturumdaki ikinci ajan) Gün 4'ün sorusunu sor: "bu testin doğrulamadığı
  bir davranış söyle". Bu sorunun mekanik hâli mutasyon testidir (§3).

### 2. Kapsam (coverage): neyin çalıştığını söyler, neyin kontrol edildiğini söylemez
Kapsam aracı, testler koşarken hangi kodun çalıştığını kaydeder. Java dünyasında yaygın araç JaCoCo'dur.
JaCoCo'nun belgesi iki temel sayacı şöyle tanımlar:

- **Satır kapsamı (line coverage):** O satıra ait en az bir bayt kodu komutu çalıştıysa satır çalışmış
  sayılır. Bir satır tamamen, kısmen ya da hiç çalışmamış olabilir.
- **Dal kapsamı (branch coverage):** `if` ve `switch` gibi karar noktalarında her dal ayrı sayılır. Araç
  bir metottaki toplam dal sayısını ve bunların kaçının çalıştığını raporlar.

Fark şu örnekte görünür:

```java
if (soru != null && secenekler.size() == 4) {
    gecerli.add(soru);
}
```

Geçerli bir soru veren tek bir test yeter: satır kapsamına göre bu satırların hepsi çalışmış sayılır ve
oran %100 çıkar. Ama `soru == null` dalı ve dört seçenekli olmayan dal hiç denenmemiştir. Dal kapsamı
bu eksiği gösterir. Yine de dal kapsamı da yalnız "bu dal çalıştı mı?" diye sorar.

Asıl sınır buradadır: **kapsam neyin çalıştığını ölçer, neyin kontrol edildiğini ölçmez.** Martin Fowler
2004'te yazdığı "AssertionFreeTesting" (doğrulamasız test) notunda büyük bir dış kaynak projesini anlatır.
Her public metoda JUnit testi yazılmış, rapor yeşil, ama testlerde tek bir doğrulama ifadesi (assertion)
yok. Fowler'ın tespiti şu: bunu yapıp %100 kapsama ulaşabilirsin. Test kodu çalıştırır, sonuca bakmaz.

O hâlde kapsam ne işe yarar? Fowler 2012 tarihli "TestCoverage" yazısında cevabı verir: kapsam, kod
tabanının **test edilmemiş kısımlarını bulmak** için yararlı bir araçtır. Testlerin ne kadar iyi olduğunun
sayısal göstergesi olarak ise pek işe yaramaz. Google'ın test blogundaki "Code Coverage Best Practices"
(Bender, Arguelles, Ivanković, 2020) aynı asimetriyi söyler. Yüksek kapsam yüzdesi test kalitesini garanti
etmez. Ama **düşük kapsam**, ürünün geniş bölümlerinin her dağıtımda (deployment) hiç otomatik test
görmediğini garanti eder. Yazı, satırların yeterince sınanıp sınanmadığını anlamak için daha iyi tekniğin
mutasyon testi olduğunu da ekler. Kısacası kapsam **eksiği** gösterir, **yeterliliği** göstermez.

Bunu ölçen bir çalışma da var. Inozemtseva ve Holmes (ICSE 2014), kod satırı 724.000'e varan beş sistem için
31.000 test takımı üretti. Takımların hata yakalama gücünü de mutasyon testiyle ölçtü. Test sayısını sabit
tuttuklarında kapsam ile etkililik arasında **düşükten orta düzeye** bir ilişki buldular. Daha güçlü kapsam
türleri de takımın etkililiği hakkında ek bilgi vermedi. Vardıkları sonuç: kapsam, az test edilmiş yerleri
bulmak için faydalıdır ama **kalite hedefi olarak kullanılmamalıdır**.

**Doğrulama yoğunluğu (assert density).** Ucuz bir ek sinyal, test başına doğrulama sayısıdır. Hiç
doğrulama içermeyen test kesin bir uyarı işaretidir ve basit bir betikle bulunur. Ama bu sinyalin sınırı
da bellidir. `assertThat(sonuc).isNotNull()` bir doğrulamadır ve neredeyse hiçbir şeyi kontrol etmez.
Tersine, alakasız on alanı kontrol eden bir test yoğun görünür ama kırılgandır. Sayı kontrolün
**varlığını** gösterir, **gücünü** göstermez. Bu sayıyı hedef yaparsan o da oynanır.

**Yüzde kapıları neden oynanır?** Bir ölçüt hedef hâline gelince iyi bir ölçüt olmaktan çıkar. Bu gözlem
Goodhart yasası adıyla bilinir. Fowler bunu kapsam için söyler: belirli bir kapsam düzeyini hedef yaparsan
insanlar ona ulaşmaya çalışır ve yüksek sayılara düşük kaliteli testlerle ulaşmak çok kolaydır. Fowler
düşünerek test yazan bir ekipte kapsamın yüzde seksenlerin üst kısmında ya da doksanlarda olmasını bekler.
%100 görürse şüphelenir, çünkü bu sayı testlerin ölçüte göre yazıldığını düşündürür. Google da tek bir
ideal sayı olmadığını yazar. Genel rehberi %60'ı kabul edilebilir, %75'i övgüye değer, %90'ı örnek sayar;
ama tepeden zorunluluk koymaktan kaçınır ve kararı ürünün sahibine bırakır.

Ajan bu sorunu büyütür. Ajana "kapsamı %80'e çıkar" dersen tam olarak bunu yapar. En ucuz yol, kodu
çalıştırıp az şey kontrol eden testlerdir. Hedef aynı zamanda ödül olduğu için ajan o yolu bulur. Ajana
verilecek hedef "kapsam" değil, "şu davranışlar şu testlerle korunuyor" olmalıdır.

> **Bu depoda:** `pom.xml`'de kapsam aracı **yok** (JaCoCo eklenmemiş), yüzde kapısı da yok. Kural
> `AGENTS.md`'de yazılıdır: "yeni davranışa test". Bu bilinçli bir sadeliktir ama bir boşluk da bırakır:
> hangi kodun hiç test görmediğini bugün bir araç değil, inceleme söyler. Kapsam ölçümü eklenecekse bu yeni
> bir bağımlılıktır ve `AGENTS.md`'nin "ASK FIRST" kuralı gereği önce bir ADR (mimari karar kaydı) ister.
> Eklenirse doğru kullanımı kapı değil, **harita**dır: rapordaki kırmızı satırlara bakıp "bu riski kabul
> ediyor muyuz?" diye sormak.

## Diyagram 1: Kapsam ve kontrol iki ayrı eksen
Yatay eksen kodun çalışıp çalışmadığını, dikey eksen sonucun kontrol edilip edilmediğini gösterir. Kapsam
aracı yalnız yatay ekseni görür. Sağ alttaki köşe, yani doğrulamasız ama yüksek kapsamlı test takımı, en
tehlikeli olanıdır, çünkü raporda en iyi o görünür.

```svg
<svg viewBox="0 0 640 372" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Kapsam ile kontrol iki ayrı eksendir: yüksek kapsam zayıf kontrolle sahte güven üretir; dikey ekseni mutasyon testi gösterir">
  <defs>
    <marker id="tk1-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#25804C"/></marker>
  </defs>
  <text x="320" y="26" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Kapsam bir eksen, kontrol başka bir eksen</text>
  <text x="320" y="46" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Yatay: kod çalıştı mı (kapsam) · Dikey: sonuç kontrol edildi mi (assert)</text>
  <text x="56" y="114" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">güçlü</text>
  <text x="56" y="130" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">kontrol</text>
  <text x="56" y="234" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">zayıf</text>
  <text x="56" y="250" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">kontrol</text>
  <rect x="108" y="64" width="220" height="112" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="218" y="104" text-anchor="middle" font-size="13" font-weight="700" fill="#186399" font-family="system-ui">Dar ama dürüst</text>
  <text x="218" y="126" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">az kod çalışır,</text>
  <text x="218" y="142" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">çalışan sıkı kontrol edilir</text>
  <rect x="336" y="64" width="220" height="112" rx="10" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="446" y="104" text-anchor="middle" font-size="13" font-weight="700" fill="#25804C" font-family="system-ui">Hedef</text>
  <text x="446" y="126" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">çalışır ve kontrol edilir</text>
  <text x="446" y="142" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">mutantlar ölür</text>
  <rect x="108" y="184" width="220" height="112" rx="10" fill="#F3F5F7" stroke="#5D7789" stroke-width="1.5"/>
  <text x="218" y="224" text-anchor="middle" font-size="13" font-weight="700" fill="#5D7789" font-family="system-ui">Kör nokta</text>
  <text x="218" y="246" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">ne çalışır</text>
  <text x="218" y="262" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">ne kontrol edilir</text>
  <rect x="336" y="184" width="220" height="112" rx="10" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.5"/>
  <text x="446" y="224" text-anchor="middle" font-size="13" font-weight="700" fill="#BE4433" font-family="system-ui">Sahte güven</text>
  <text x="446" y="246" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">doğrulamasız test, %100 kapsam</text>
  <text x="446" y="262" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">(Fowler, 2004)</text>
  <text x="218" y="318" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">düşük kapsam</text>
  <text x="446" y="318" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">yüksek kapsam</text>
  <text x="596" y="76" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">mutasyon</text>
  <text x="596" y="90" text-anchor="middle" font-size="10.5" font-weight="700" fill="#25804C" font-family="system-ui">testi</text>
  <line x1="596" y1="292" x2="596" y2="100" stroke="#25804C" stroke-width="1.8" marker-end="url(#tk1-ok)"/>
  <text x="320" y="344" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Kapsam aracı yalnız soldan sağa gidişi görür.</text>
  <text x="320" y="360" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Aşağıdan yukarı gidişi, yani kontrolün gücünü, mutasyon testi gösterir.</text>
</svg>
```

### 3. Mutasyon testi (mutation testing): testlerini test et
Kapsam "kod çalıştı mı?" diye sorar. Mutasyon testi daha sert bir soru sorar: **kod bozulsaydı testlerin
fark eder miydi?** Yöntem şöyle işler. Araç koda bilerek küçük bir hata koyar: `<` işaretini `<=` yapar,
bir `if` koşulunu tersine çevirir, bir metot çağrısını siler, dönen değeri `null` yapar. Bu bozulmuş
kopyaya **mutant** denir. Sonra testler mutanta karşı koşar.

Java için yaygın mutasyon aracı PIT'tir (pitest.org). PIT'in belgesindeki sonuç türleri şunlardır:

- **Öldürüldü (killed):** En az bir test mutantı yakaladı, yani kırmızı oldu. İstenen budur.
- **Yaşadı (survived):** Mutantlı satırı çalıştıran testler oldu ama hiçbiri değişikliği yakalamadı.
- **Kapsanmadı (no coverage):** Yaşadı ile aynı, ama bu kez o satırı hiçbir test çalıştırmadı.
- **Zaman aşımı (timed out):** Mutant sonsuz döngüye yol açtı. Örneğin bir döngü sayacının artışı silindi.

Öldürülen mutantların oranına **mutasyon skoru** (mutation score) denir. Skor, testlerin "çalıştırdığı"
değil "kontrol ettiği" kodun ölçüsüdür. Diyagram 1'in dikey eksenini ölçen budur.

Bir pürüz var: **eşdeğer mutant** (equivalent mutant). Bazı değişiklikler programın davranışını hiç
değiştirmez ya da testin kontrol etmemesi gereken bir şeyi değiştirir. PIT'in verdiği tipik örnek günlük
(log) satırlarıdır. PIT bu yüzden yaygın log framework'lerini (çatı) çağıran satırlarda varsayılan olarak
mutant üretmez. Eşdeğer bir mutantı hiçbir test öldüremez. Bu yüzden skoru %100'e zorlamak çoğu zaman
anlamsızdır.

**Mutant yakalamak gerçek hata yakalamakla ilişkili mi?** Just ve arkadaşları (FSE 2014) bu soruyu beş açık
kaynak uygulamadaki 357 gerçek hata üzerinde sordu. Mutant yakalama ile gerçek hata yakalama arasında
istatistiksel olarak anlamlı bir ilişki buldular ve bu ilişki **kod kapsamından bağımsızdı**. Kapsamın
göremediğini mutasyonun gördüğünü söyleyen kanıt budur.

**PIT nasıl çalışır?** PIT kaynak kodu değil, derlenmiş bayt kodu değiştirir. Varsayılan operatör grubunda
(DEFAULTS) on bir operatör vardır. En sık karşılaşacakların şunlar:

- `CONDITIONALS_BOUNDARY`: `a < b` ifadesini `a <= b` yapar (sınır hatası).
- `NEGATE_CONDITIONALS`: `a == b` ifadesini `a != b` yapar (koşulu ters çevirme).
- `MATH`: `b + c` ifadesini `b - c` yapar.
- `VOID_METHOD_CALLS`: dönüş değeri olmayan bir metot çağrısını siler.
- `EMPTY_RETURNS`, `NULL_RETURNS`, `TRUE_RETURNS`, `FALSE_RETURNS`, `PRIMITIVE_RETURNS`: dönen değeri boş
  değer, `null`, `true`, `false` ya da `0` yapar.
- `INCREMENTS` ve `INVERT_NEGS`: artırmayı azaltmaya çevirir, sayının eksi işaretini kaldırır.

**Maliyet.** Her mutant için testleri yeniden koşmak pahalıdır. PIT bunu iki yolla azaltır. Önce satır
kapsamını ölçer ve her mutanta karşı yalnız o satırı çalıştıran testleri koşar. Ayrıca iş parçacığı sayısı
(`threads`) artırılabilir. Senin elindeki asıl kaldıraç ise **hedeflemedir**: `targetClasses` ile yalnız
değiştirdiğin sınıfları, `targetTests` ile yalnız ilgili testleri seçersin. PIT'in bir de artımlı analiz
(incremental analysis) özelliği var. Önceki koşunun sonuçlarını bir geçmiş dosyasında tutar, değişmeyen
sınıf ve testlerin sonucunu yeniden kullanır. PIT bu özelliği deneysel sayar ve analize bir miktar hata payı
kattığını kendisi yazar.

Ölçekte nasıl yapıldığını Google'ın makalesi gösterir (Petrović, Ivanković, Fraser, Just, "Practical
Mutation Testing at Scale: A view from Google"). Google bütün kod tabanını mutasyona sokmaz. Yalnız **kod
incelemesindeki değişikliği** mutasyona sokar, bir satırda en fazla bir mutant üretir ve yaşayan mutantları
inceleme ekranında yazara ve inceleyicilere gösterir. Log satırı gibi "kurak" (arid) düğümlerde mutant
üretmez. Makaleye göre geliştiriciler başta raporlanan mutantların %85'ini verimsiz bulmuş. Süzme
kurallarıyla verimli mutant oranı %15'ten %89'a çıkmış. Ders açık: mutasyon testi **ham hâliyle**
gürültülüdür, değeri süzmekten ve hedeflemekten gelir.

**Yaşayan bir mutant nasıl okunur?** Üç olası cevap vardır:

1. **Eksik test:** Davranış önemli ama hiçbir test onu sınamıyor. Testi ekle.
2. **Eşdeğer mutant:** Değişiklik davranışı etkilemiyor. Görmezden gel ya da aracın hariç tutma listesine ekle.
3. **Gereksiz kod:** Koşul hiçbir zaman fark yaratmıyor, çünkü kod gereğinden karmaşık. Kodu sadeleştir.

Bu depodan, **okuyarak** yapılmış bir tahmin (PIT bu depoda hiç koşturulmadı):
`src/main/java/tr/velzon/aiogretmen/service/QuizUretimService.java` AI'ın ürettiği soruları süzerken şu
koşulu kullanır: `s.dogruIndex() >= 0 && s.dogruIndex() < 4`. `CONDITIONALS_BOUNDARY` operatörü `< 4`
koşulunu `<= 4` yapar. `QuizUretimServiceTest.bozukSorulariAyiklar_tumuBozuksa502` taşmış indeks olarak 7
değerini dener. 7 iki koşulda da reddedilir; 4 değerini deneyen bir test yoktu. Bu mutant büyük olasılıkla
yaşardı. Tahmin elle sınandı: koşul geçici olarak `<= 4` yapıldı ve test takımı hâlâ geçti. Bunun üzerine
`dogruIndexSinirlari_0ve3Gecer_eksi1ve4Ayiklanir` testi eklendi (0 ve 3 kabul, -1 ve 4 ret). Aynı mutant
artık testi kırıyor. Karşı örnek de öğreticidir: `>= 0` koşulunu `> 0` yapan mutant büyük olasılıkla ölür, çünkü
`kesilenJsondaDuzeltmeCagrisiYapar` testi 0 indeksli geçerli bir sorunun kabul edilmesini bekler. Yaşayan
mutant birinci türden bir cevaptı: üst sınır (4) için test eksikti. Mutasyon testi bu tür eksiği, kimse
aramadan önüne koyar.

**AI'ın yazdığı testler için neden güçlü bir kontrol?** Çünkü §1'deki tuzakların hepsi aynı sonucu verir:
testler mutantları öldüremez. Doğrulamasız test, totolojik test, sahteyi kontrol eden test, gevşek test:
hepsi kapsamı yükseltir ama mutasyon skorunu yükseltmez. Mutasyon testi, ajanın kolayca hedefleyebileceği
bir sayıyı (kapsam) kolayca kandıramayacağı bir sayıyla değiştirir. Meta bu fikri tersinden de kullanıyor.
"Mutation-Guided LLM-based Test Generation at Meta" (2025) makalesinde bir dil modeli belirli bir kaygıya
ilişkin mutantlar üretiyor (makaledeki örnek gizlilik), sonra onları öldüren testler yazıyor. Bu, şirketin
kendi platformundaki kendi raporudur; §7'deki şerh burada da geçerlidir.

Bu fikrin elle yapılan küçük hâlini zaten biliyorsun. Hafta 4 Gün 1 "kuralı bilerek bir kez kırıp kırmızı
yandığını görmeden ona güvenme" der. [`harness-mekanigi.md`](harness-mekanigi.md) §5.1 buna "kanıtı
kanıtla" der: bilerek belirli bir yanlış koy, kontrol **o sebeple** kırmızı olmalı. Mutasyon testi bunun
otomatik ve sistemli hâlidir.

**Bu depoda:** `pom.xml`'de PIT **yok**; depoda mutasyon testi yapılmıyor. Denemek istersen önce
`AGENTS.md` kuralı geçerlidir: yeni bağımlılık için önce `docs/adr/` altına bir ADR yazılır. Komut satırından
tek seferlik bir koşu da derlemeye yeni bir eklenti indirir; o da aynı karardır. JUnit 5 testleri için
PIT'in yanına `pitest-junit5-plugin` eklentisi gerekir. Bu eklenti projenin değil, PIT'in sınıf yoluna
konur. Dar bir ilk deneme şöyle görünürdü:

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>${pitest.version}</version>
  <dependencies>
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
      <version>${pitest-junit5.version}</version>
    </dependency>
  </dependencies>
  <configuration>
    <targetClasses>
      <param>tr.velzon.aiogretmen.service.QuizUretimService</param>
    </targetClasses>
    <targetTests>
      <param>tr.velzon.aiogretmen.service.QuizUretimServiceTest</param>
    </targetTests>
  </configuration>
</plugin>
```

Koşu komutu `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` olur; rapor HTML olarak
`target/pit-reports/` altına yazılır. Sürümler ADR'de sabitlenir. İlk hedef bir eşik koymak değil, yaşayan
mutantları tek tek okumak olmalıdır.

## Diyagram 2: Mutasyon testinin akışı
Araç kaynak koda bir operatör uygular ve bir mutant üretir. Testler mutanta karşı koşar. Üç sonuçtan
yalnız "öldü" testin işini yaptığını gösterir; "yaşadı" ve "kapsanmadı" okunacak birer bulgudur.

```svg
<svg viewBox="0 0 640 392" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Mutasyon testi: araç koda küçük bir hata koyar, testleri mutanta karşı koşar; mutant öldürülür, yaşar ya da hiç kapsanmaz; skor öldürülenlerin oranıdır">
  <defs>
    <marker id="tk2-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#186399"/></marker>
  </defs>
  <text x="320" y="26" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Mutasyon testi: testleri bilerek bozulmuş koda karşı koş</text>
  <text x="320" y="46" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Mutant = koda bilerek konmuş küçük bir hata · iyi bir test onu yakalar</text>
  <rect x="16" y="66" width="128" height="60" rx="9" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.5"/>
  <text x="80" y="91" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">Kaynak kod</text>
  <text x="80" y="111" text-anchor="middle" font-size="11" fill="#5D7789" font-family="system-ui">if (i &lt; 4)</text>
  <line x1="144" y1="96" x2="170" y2="96" stroke="#186399" stroke-width="1.8" marker-end="url(#tk2-ok)"/>
  <rect x="174" y="66" width="150" height="60" rx="9" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="249" y="91" text-anchor="middle" font-size="12" font-weight="700" fill="#C77A17" font-family="system-ui">Mutasyon operatörü</text>
  <text x="249" y="111" text-anchor="middle" font-size="11" fill="#5D7789" font-family="system-ui">sınır: &lt; yerine &lt;=</text>
  <line x1="324" y1="96" x2="350" y2="96" stroke="#186399" stroke-width="1.8" marker-end="url(#tk2-ok)"/>
  <rect x="354" y="66" width="128" height="60" rx="9" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.5"/>
  <text x="418" y="91" text-anchor="middle" font-size="12" font-weight="700" fill="#BE4433" font-family="system-ui">Mutant</text>
  <text x="418" y="111" text-anchor="middle" font-size="11" fill="#5D7789" font-family="system-ui">if (i &lt;= 4)</text>
  <line x1="482" y1="96" x2="508" y2="96" stroke="#186399" stroke-width="1.8" marker-end="url(#tk2-ok)"/>
  <rect x="512" y="66" width="112" height="60" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="568" y="91" text-anchor="middle" font-size="12" font-weight="700" fill="#186399" font-family="system-ui">Testleri koş</text>
  <text x="568" y="111" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">yalnız kapsayanlar</text>
  <line x1="568" y1="126" x2="568" y2="148" stroke="#186399" stroke-width="1.6"/>
  <line x1="114" y1="148" x2="568" y2="148" stroke="#186399" stroke-width="1.6"/>
  <line x1="114" y1="148" x2="114" y2="166" stroke="#186399" stroke-width="1.6" marker-end="url(#tk2-ok)"/>
  <line x1="320" y1="148" x2="320" y2="166" stroke="#186399" stroke-width="1.6" marker-end="url(#tk2-ok)"/>
  <line x1="526" y1="148" x2="526" y2="166" stroke="#186399" stroke-width="1.6" marker-end="url(#tk2-ok)"/>
  <rect x="16" y="170" width="196" height="80" rx="10" fill="#E9F4EE" stroke="#25804C" stroke-width="1.6"/>
  <text x="114" y="196" text-anchor="middle" font-size="12.5" font-weight="700" fill="#25804C" font-family="system-ui">Öldü (killed)</text>
  <text x="114" y="216" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">en az bir test kırmızı</text>
  <text x="114" y="234" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">test bu hatayı görür</text>
  <rect x="222" y="170" width="196" height="80" rx="10" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.6"/>
  <text x="320" y="196" text-anchor="middle" font-size="12.5" font-weight="700" fill="#BE4433" font-family="system-ui">Yaşadı (survived)</text>
  <text x="320" y="216" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">bütün testler yeşil</text>
  <text x="320" y="234" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">eksik test mi, eşdeğer mi?</text>
  <rect x="428" y="170" width="196" height="80" rx="10" fill="#F3F5F7" stroke="#5D7789" stroke-width="1.6"/>
  <text x="526" y="196" text-anchor="middle" font-size="12" font-weight="700" fill="#5D7789" font-family="system-ui">Kapsanmadı (no coverage)</text>
  <text x="526" y="216" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">hiçbir test bu satırı</text>
  <text x="526" y="234" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">çalıştırmadı</text>
  <rect x="110" y="276" width="420" height="56" rx="10" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="320" y="299" text-anchor="middle" font-size="13" font-weight="700" fill="#4F46E5" font-family="system-ui">Mutasyon skoru = öldürülen ÷ üretilen</text>
  <text x="320" y="319" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">(ideal tanımda eşdeğerler paydadan düşülür)</text>
  <text x="320" y="358" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Eşdeğer mutant davranışı değiştirmez (tipik örnek: log satırı).</text>
  <text x="320" y="376" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Hiçbir test onu öldüremez; skoru %100'e zorlamak bu yüzden anlamsızdır.</text>
</svg>
```

### 4. Gerçek altyapıyla entegrasyon testi: Testcontainers ve H2'nin sınırı
**Sahte nesne bir iddiadır.** Bir repository'yi mock'ladığında "veritabanı bu durumda şunu yapar" diye bir
iddiada bulunursun. İddia yanlışsa test yeşil, üretim kırmızıdır. Bu depodaki en öğretici vaka tam budur
(`docs/tech-debt-tracker.md`, "Kapanan borçlar" #9):

- `IlerlemeService.gorevDurumuAyarla` iki istek aynı görevi aynı anda işaretlediğinde tekil indeks
  çakışmasını yakalayıp kaydı yeniden okuyacak biçimde yazılmıştı.
- Birim testi (`IlerlemeServiceYarisTest.uniqueIhlalindeYenidenOkuyupHedefDurumaAyarlar`) mock'lanmış
  repository'ye doğrudan `DuplicateKeyException` fırlattırıyordu. Test yeşildi.
- Gerçek veritabanında böyle olmuyordu. Spring Data JDBC çakışmayı `DbActionExecutionException` içine
  sarıyordu. Kod yalnız `DataIntegrityViolationException` türünü yakaladığı için "yakala, yeniden oku"
  yolu gerçek veritabanında **hiç tetiklenmiyordu**.
- Bunu, gerçek H2 ve gerçek tekil indeks üzerinde sekiz iş parçacığını aynı anda başlatan
  `IlerlemeServiceEszamanliTest` ortaya çıkardı. Düzeltme, istisnanın neden zincirini (cause chain)
  taramak oldu (`IlerlemeService.yarisCakismasi`). Ardından sarmalanmış istisnayı taklit eden hızlı bir
  birim testi de eklendi: `springDataJdbcSarmaliCakismaDaYakalanir`.

Ders şu: mock, altyapı hakkındaki inancını koda döker. O inancı **bir kez gerçek altyapıya karşı doğrula**,
sonra hızlı bir teste çevir. Dikkat: bu vakadaki "gerçek altyapı" PostgreSQL değil, H2'dir. Gerçek bir
motor ve gerçek Spring Data JDBC katmanı, mock'un söylemediğini söylemeye yetti.

**H2 ile PostgreSQL aynı şey değildir.** H2 bir PostgreSQL uyumluluk modu sunar. Ama H2'nin kendi belgesi,
veritabanları arasındaki farkların yalnız küçük bir kısmının bu yolla taklit edildiğini yazar. Testcontainers'ın
H2 rehberi de iki noktayı vurgular: test veritabanı üretim veritabanının bütün özelliklerini desteklemeyebilir
ve `ON CONFLICT` gibi bir sorgu H2'de varsayılan olarak çalışmaz. Uyumluluk modu açıkken bile her özellik
desteklenmez. Bu depo bu farklara gerçekten çarptı:

- H2'ye özgü `IDENTITY` tipi, `CLOB` tipi ve `ALTER TABLE ... ADD CONSTRAINT IF NOT EXISTS` ifadesi
  PostgreSQL'de yoktur (ADR-0014 §3).
- Tablo adlarında harf büyüklüğü: PostgreSQL tırnak içindeki büyük harfli adı ayrı bir ad sayar. Tırnaksız
  adları ise H2 büyük harfe, PostgreSQL küçük harfe çevirir (ADR-0014 §4).
- Salt okunur transaction (işlem bütünlüğü): Spring Data JDBC depoları sınıf düzeyinde salt okunur
  transaction'la (`readOnly=true`) çalışır. Yazma yapan bir transaction'ın dışında çağrılan bir
  `deleteBy...` metodu PostgreSQL'de hata verir, H2 bunu göstermez
  (`docs/exec-plans/completed/faz-10-girissiz-site.md`, "İncelemelerin yakaladığı 4 hata" #2).

**Testcontainers nedir?** Testlerin içinden Docker konteyneri başlatan bir kütüphanedir. Test, üretimdeki
motorun gerçek imajına (örneğin PostgreSQL) bağlanır; test bitince konteyner kaldırılır. Belgelerdeki
temel noktalar şunlar:

- Docker API'siyle uyumlu bir konteyner çalışma zamanı (runtime) şarttır.
- JUnit 5'te `@Testcontainers` uzantıyı açar, `@Container` konteyner alanlarını işaretler. **Statik**
  alandaki konteyner sınıftaki bütün test metotlarınca paylaşılır ve bir kez başlar. **Örnek** (instance)
  alanındaki konteyner her test metodu için başlar ve durur.
- Birden fazla test sınıfı için tek konteyner istersen "tekil konteyner" (singleton container) desenini
  kullanırsın. Konteyner soyut bir taban sınıfın statik bloğunda başlatılır. Test takımı bitince
  Testcontainers'ın Ryuk adlı yardımcı konteyneri onu durdurur. Uzantı bunun için özel bir destek vermez,
  deseni sen kurarsın.
- JUnit uzantısıyla paralel test koşturmak desteklenmez.
- Spring Boot ile bir tuzak var. Uzantı statik alandaki konteyneri sınıf bitince durdurur. Oysa Spring test
  bağlamını (application context) önbelleğe (cache) alıp başka bir test sınıfında yeniden kullanabilir.
  Durmuş konteynere bağlı bean'ler sonraki testlerde hata verebilir. Spring Boot belgesi bu yüzden, bağlam
  önbellekte durduğu sürece kullanılabilir kalmalıysa konteynerleri Spring bean'i olarak yönetmeyi önerir.
  `@ServiceConnection` ise konteynerin bağlantı bilgilerini Spring'e kendiliğinden verir.
- En kısa yol özel JDBC adresidir: `jdbc:` önekinden sonra `tc:` eklenir
  (`jdbc:tc:postgresql:16-alpine:///db`) ve test taze bir veritabanı konteyneri alır.

**Ne zaman değer?** Test edilen davranış motora özgüyse değer: kısıtlar, kilitler, transaction davranışı,
SQL lehçesi, şema betikleri, eşzamanlılık. Saf iş kuralı için değmez. Hafta 9'daki hexagonal
çekirdek testleri veritabanı olmadan, milisaniyelerle koşar. Bedel de açıktır: her geliştirici makinesinde
ve CI'da (sürekli tümleştirme) Docker gerekir, konteyner başlatmak zaman alır, ilk koşuda imaj indirilir.

**Bu depoda:** Testcontainers bilinçli olarak **yok**. ADR-0014 gerekçeyi tek cümleyle yazar: Docker'ın
sunucuda ve geliştirici makinesinde var olacağı garanti değil. Onun yerine iki parçalı bir çözüm var:

1. `src/test/java/tr/velzon/aiogretmen/repo/SemaTasinabilirlikTest.java` her `./mvnw verify` koşusunda
   çalışır. `schema.sql` metninde H2'ye özgü sözdizimini arar: `ID IDENTITY`, `CLOB`,
   `ADD CONSTRAINT IF NOT EXISTS`, `MERGE INTO`, `DATEADD`, `AUTO_INCREMENT` ve `IF NOT EXISTS` içermeyen
   `CREATE TABLE/INDEX`. Şemayı H2'de iki kez çalıştırarak idempotent (tekrar çalışınca aynı sonucu veren) olduğunu da
   doğrular.
2. `src/test/java/tr/velzon/aiogretmen/SitePostgresEntegrasyonTest.java` gerçek PostgreSQL'e karşı koşar,
   ama **yalnız** `PG_TEST_URL` ortam değişkeni verilince (`@EnabledIfEnvironmentVariable`). Normal
   `verify` koşusunda atlanır. README'ye göre PostgreSQL 14.15'e karşı 5/5 geçti. README'nin "Yol haritası
   ve bilinen sınırlar" bölümü de açıkça yazar: bu test CI'da değil.

Ödünleşim (trade-off: bir şeyi kazanırken başka bir şeyden vazgeçmek) şöyle özetlenir:

| Seçenek | Neyi yakalar | Bedeli | Bu depoda |
|---|---|---|---|
| Yalnız H2 | iş mantığı, H2'de de aynı olan davranış | lehçe farkını göremez | yerel mod ve testlerin çoğu |
| Metin bekçisi (`SemaTasinabilirlikTest`) | bilinen H2'ye özgü sözdizimi | yalnız listedeki kalıpları görür | var, her koşuda |
| İsteğe bağlı gerçek PostgreSQL | gerçek motor davranışı | biri hatırlayıp koşmazsa hiç koşmaz | var, `PG_TEST_URL` ile |
| Testcontainers | gerçek motor, her koşuda | Docker şartı, yavaşlık | yok (ADR-0014) |

İsteğe bağlı testin zayıf noktası bellidir: atlanan test raporda "skipped" olarak görünür, yeşil derleme
onu sınanmış göstermez. Testcontainers'ı yeniden düşünmenin işaretleri de bellidir: depoya Docker'lı bir
CI hattı gelirse, şema yıkıcı bir değişiklik isteyip bir göç aracı gelirse ya da PostgreSQL'e özgü
eşzamanlılık davranışı ürünün parçası olursa.

Aynı ilke ön yüzde de uygulandı. `src/test/java/tr/velzon/aiogretmen/web/OnYuzDavranisTest.java` gerçek
bir Chromium tarayıcıyı (Playwright) uygulamanın site profiline karşı koşturur. Sunucu dışındaki her istek
kesilir, OpenRouter taklit edilir. ADR-0021 tarayıcıyı taklit eden seçenekleri şu gerekçelerle reddetti:
jsdom gerçek bir tarayıcı değildir, CSP (içerik güvenlik politikası) gibi davranışları taklit eder ya da
hiç sunmaz; HtmlUnit'in modern JavaScript desteği zayıftır ve "sahte yeşil" riski taşır. Taklit edilen altyapı ne kadar yüzeyselse, yeşil
sonuç o kadar az şey söyler.

### 5. Sistematik hata ayıklama: önce kırmızı test
Hata raporu geldiğinde ilk refleks kodu açıp düzeltmektir. Daha iyi sıra şudur:

1. **Yeniden üret (reproduce).** Hatayı gösteren en küçük testi yaz.
2. **Kırmızıyı gör ve sebebini oku.** Test kırmızı olmalı ve **doğru sebeple** kırmızı olmalı. Hata
   mesajı rapordaki belirtiyle örtüşmeli. Derleme hatası, eksik test verisi ya da bozuk kurulum sayılmaz;
   onlar hatayı değil, test düzeneğini sınar.
3. **Düzelt.** Belirtiyi değil nedeni değiştir. Testlere dokunma.
4. **Yeşili gör.** Önce yeni test, sonra bütün takım geçmeli.
5. **Testi bırak.** Test artık bir regresyon testidir (regression test: düzeltilmiş bir hatanın geri
   dönmesini yakalayan test). Adı davranışı anlatsın.

Bu sıra yeni değil. Fowler "SelfTestingCode" yazısında, kendi kendini test eden koda sahip bir ekibin hata
raporuna olağan tepkisinin, düzeltmeden önce **hatayı açığa çıkaran bir test yazmak** olduğunu söyler.
Zeller *The Debugging Book*'ta hata ayıklamayı bilimsel yönteme bağlar: soru, hipotez, tahmin, deney. Otomatik
test kullanıyorsan hatayı (ve benzerlerini) yakalayan bir test eklemeyi de önerir. Claude Code'un en iyi
uygulamalar belgesi ajana yalnız "giriş hatasını düzelt" demek yerine şu tarzı önerir: belirtiyi anlat,
ilgili kod yerini göster, "hatayı yeniden üreten başarısız bir test yaz, sonra düzelt".

**Ajanla çalışırken bu sıra neden daha önemli?**

- **Kırmızı görülmeden gelen yeşil bir şey kanıtlamaz.** Test hatayı hiç görmemiş olabilir. Önce kırmızıyı,
  sonra yeşili görmek, testin o hatayı ayırt ettiğinin tek kanıtıdır.
- **Kırmızı test, düzeltmenin spec'idir.** Ajana "bu test yeşile dönsün, test dosyasına dokunma" demek,
  "hatayı düzelt" demekten çok daha dar ve denetlenebilir bir görevdir.
- **Özel durum kodlamayı zorlaştırır.** Tek bir girdiyle yazılmış test, ajanın o girdiyi koda özel durum
  olarak yazmasına açıktır (§1-e). Testte aynı hatanın iki üç farklı girdisini dene.
- **İnsan küçük olanı inceler.** Düzeltmenin diff'i büyük olabilir; kırmızı test çoğu zaman on satırdır.
  Önce testi oku: "bu test gerçekten raporlanan hatayı mı anlatıyor?"
- **Kanıtı sakla.** Ajanın "test önce kırmızıydı" cümlesi kanıt değildir; kırmızı koşunun çıktısı kanıttır.
  Claude Code belgesi de aynı şeyi söyler: "bütün testler geçti" cümlesi tek başına çok az şey anlatır,
  koşulan komutu ve çıktısını iste.

**Bu depodan iki vaka (Faz 14).** Faz 14'teki bağımsız Codex incelemesi eski hatalar buldu. İkisi bir testle
kilitlenip düzeltildi (`docs/exec-plans/completed/faz-14-turkce-dil-duzeltmesi.md`):

- `QuizUretimServiceTest.soruListesiYoksaNpeDegilBozukCiktiHatasi`: model `{"sorular": null}` döndürür.
  Eski kod doğrudan `quiz.sorular().stream()` çağırıyor ve `NullPointerException` fırlatıyordu. Test,
  kullanıcıya anlaşılır hata veren `AiCiktisiBozukException` bekler. Eski kodda kırmızıdır; `null`
  denetimi eklenince yeşildir.
- `SpringAiOgretmenClientTest.bosCevaptaDaKullanimKaydiYazilirSonraHataFirlatilir`: §1-d'de gördün. Eski
  kod kullanım kaydını yazmadan hata fırlatıyordu, bu yüzden `verify(kullanimRepo).save(...)` satırı eski
  kodda kırmızıdır. Düzeltme, kaydı boş cevap kontrolünden önce yazmak oldu.

Dürüst bir not: iki test de düzeltmesiyle **aynı commit'e** girdi (`f59b992`). Git geçmişi kırmızı anı
ayrıca göstermez. Testin eski kodda kırmızı olacağını eski kodu okuyarak görebilirsin. Ama "kanıtı sakla"
ilkesine göre daha iyisi, kırmızı koşunun çıktısını da kaydetmektir: commit mesajında, inceleme notunda ya
da testi ve düzeltmeyi iki ayrı commit'e alarak.

`IlerlemeServiceEszamanliTest` (§4) ise yeniden üretmenin başka bir yüzünü gösterir. Bazı hatalar yalnız
gerçek koşullarda (eşzamanlılık, gerçek motor) ortaya çıkar. Onları yeniden üretmek için mock'lu birim testi
değil, gerçek davranış testi gerekir.

## Diyagram 3: Önce kırmızı, yasak kısayol yok
Üst sıra doğru yoldur: rapor, kırmızı test, düzeltme, yeşil, kalıcı test. Kırmızı testten aşağı inen kesik
ok, ajanın en ucuz ama yasak yoludur: testi yeşile çevirir, hatayı yerinde bırakır.

```svg
<svg viewBox="0 0 640 336" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Önce kırmızı: hata raporundan kırmızı teste, düzeltmeye, yeşile ve kalıcı regresyon testine; testi değiştirmek yasak kısayoldur">
  <defs>
    <marker id="tk3-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#186399"/></marker>
    <marker id="tk3-kirmizi" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#BE4433"/></marker>
  </defs>
  <text x="320" y="26" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Önce kırmızı: hatayı testle yakala, sonra düzelt</text>
  <text x="320" y="46" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Ajanla çalışırken önce kırmızı, sonra yeşil görmek düzeltmenin kanıtıdır</text>
  <rect x="16" y="66" width="130" height="70" rx="9" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.5"/>
  <text x="81" y="94" text-anchor="middle" font-size="12" font-weight="700" fill="#1C3247" font-family="system-ui">1 · Hata raporu</text>
  <text x="81" y="114" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">belirti + girdi</text>
  <line x1="146" y1="101" x2="172" y2="101" stroke="#186399" stroke-width="1.8" marker-end="url(#tk3-ok)"/>
  <rect x="176" y="66" width="140" height="70" rx="9" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.6"/>
  <text x="246" y="90" text-anchor="middle" font-size="12" font-weight="700" fill="#BE4433" font-family="system-ui">2 · Kırmızı test</text>
  <text x="246" y="108" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">hatayı yeniden üret</text>
  <text x="246" y="124" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">doğru sebeple mi?</text>
  <line x1="316" y1="101" x2="342" y2="101" stroke="#186399" stroke-width="1.8" marker-end="url(#tk3-ok)"/>
  <rect x="346" y="66" width="130" height="70" rx="9" fill="#EAF1F7" stroke="#186399" stroke-width="1.6"/>
  <text x="411" y="90" text-anchor="middle" font-size="12" font-weight="700" fill="#186399" font-family="system-ui">3 · Düzelt</text>
  <text x="411" y="108" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">nedeni değiştir</text>
  <text x="411" y="124" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">teste dokunma</text>
  <line x1="476" y1="101" x2="502" y2="101" stroke="#186399" stroke-width="1.8" marker-end="url(#tk3-ok)"/>
  <rect x="506" y="66" width="118" height="70" rx="9" fill="#E9F4EE" stroke="#25804C" stroke-width="1.6"/>
  <text x="565" y="90" text-anchor="middle" font-size="12" font-weight="700" fill="#25804C" font-family="system-ui">4 · Yeşil</text>
  <text x="565" y="108" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">bütün takım</text>
  <text x="565" y="124" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">geçer</text>
  <line x1="565" y1="136" x2="565" y2="182" stroke="#186399" stroke-width="1.8" marker-end="url(#tk3-ok)"/>
  <rect x="346" y="186" width="278" height="64" rx="10" fill="#E9F4EE" stroke="#25804C" stroke-width="1.6"/>
  <text x="485" y="212" text-anchor="middle" font-size="12" font-weight="700" fill="#25804C" font-family="system-ui">5 · Test kalır: regresyon bekçisi</text>
  <text x="485" y="232" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">her ./mvnw verify koşusunda yeniden çalışır</text>
  <rect x="16" y="186" width="300" height="88" rx="10" fill="#FFFFFF" stroke="#BE4433" stroke-width="1.6" stroke-dasharray="6 4"/>
  <text x="166" y="208" text-anchor="middle" font-size="12" font-weight="700" fill="#BE4433" font-family="system-ui">Yasak kısayol</text>
  <text x="166" y="228" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">testi değiştir ya da doğrulamayı sil</text>
  <text x="166" y="244" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">hatalı girdiyi özel durum diye kodla</text>
  <text x="166" y="262" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">sonuç yeşil, hata yerinde</text>
  <line x1="236" y1="138" x2="196" y2="182" stroke="#BE4433" stroke-width="1.6" stroke-dasharray="5 3" marker-end="url(#tk3-kirmizi)"/>
  <line x1="208" y1="152" x2="224" y2="168" stroke="#BE4433" stroke-width="2.2"/>
  <line x1="224" y1="152" x2="208" y2="168" stroke="#BE4433" stroke-width="2.2"/>
  <text x="320" y="302" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Kırmızı görülmeden gelen yeşil bir şey kanıtlamaz: test hatayı hiç görmemiş olabilir.</text>
  <text x="320" y="320" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">Depodan örnek: soruListesiYoksaNpeDegilBozukCiktiHatasi (eski kodda NPE, test kırmızı)</text>
</svg>
```

### 6. Mekanik bekçiler: kuralı zorlayan testler ve "testin taradığını test et"
Hafta 4'ün ana fikri şuydu: yazılı kural bir tabeladır, testle zorlanan kural ise bir çit. Bu depoda kodun
davranışını değil **kuralları** sınayan birkaç test var:

- `MimariTestleri` (ArchUnit): types → config → repo → service → ai → web katman yönü, paketler arası döngü
  yasağı, Spring AI'a yalnız `ai/` paketinden erişim.
- `IcerikYazimKurallariTest`: içerikte, prompt'larda ve HTML sayfalarında uzun çizgi yasağı.
- `SemaTasinabilirlikTest`: şemada H2'ye özgü sözdizimi yasağı.
- `ToolsSizintiTaramaTest`: push öncesi sızıntı tarayıcısının senaryo testleri.

Bu tür testlerin kendine özgü bir arızası var: **hiçbir şeyi taramadığı için geçmek.** Klasörün adı değişir,
paket yeniden adlandırılır ya da araç bozulur. Kural "sıfır dosyada sıfır ihlal" bulur ve yeşil yanar.
ArchUnit'in belgesi bu riski açıkça anlatır: bir kural yanlışlıkla hiç sınıf kontrol etmeyebilir. Örneğin
kural `com.myapp.old` paketine bakıyorsa ve biri paketin adını değiştirirse, kural her zaman başarılı olur
ama artık hiçbir sınıfı kontrol etmez. ArchUnit bu yüzden varsayılan olarak, boş bir sınıf kümesine
uygulanan kuralı başarısız sayar.

Bu depoda aynı korumanın beş biçimi var:

1. **`IcerikYazimKurallariTest.tarama_bos_degil`.** Taranan dosya listesinin `content/kaynaklar`,
   `content/hafta-01`, `content/prompt-atolyesi`, `prompts`, `index.html` ve hata sayfalarını gerçekten
   içerdiğini, `content/mulakat` klasörünü ise içermediğini doğrular. Koddaki yorum amacı tek cümleyle
   söyler: klasör adı değişirse test sessizce hiçbir şeyi taramadan geçmesin.
2. **`MimariTestleri.katman_kurali` içindeki `withOptionalLayers(false)`.** Bütün katmanların dolu olması
   şarttır. Paket yeniden adlandırılıp bir katman boş kalırsa derleme kırmızı olur. Yorumdaki gerekçe:
   sessiz geçen bir uygunluk fonksiyonu (fitness function) işe yaramaz.
3. **`pom.xml`'deki surefire ayarı.** Maven'ın test koşucusu (surefire) varsayılan olarak yalnız belirli
   ad kalıplarına uyan sınıfları bulur; örneğin adı `Test` ya da `Tests` ile bitenleri. `MimariTestleri`
   bu kalıplara uymaz. Bu yüzden `pom.xml` onu ayrıca listeler. Yanındaki yorum: "sessiz atlanmasın".
   Adı yanlış konmuş bir test, yazılmamış bir test kadar etkisizdir.
4. **`SitePostgresEntegrasyonTest.gercekPostgresKullaniliyor`.** `SELECT version()` sonucunun
   `PostgreSQL` ile başladığını doğrular. Yapılandırma kayar da test sessizce başka bir veritabanına
   bağlanırsa "PostgreSQL testi geçti" cümlesi yalan olurdu; bu test o durumu kırmızı yapar.
5. **Sızıntı tarayıcısının öz sınaması.** `tools/sizinti-tara.sh` ağacı taramaya başlamadan önce
   `git grep`'in PCRE (Perl uyumlu düzenli ifade) desteğini, sonucu önceden bilinen bir aramayla dener.
   Gerekçe betiğin yorumundadır: `git grep` bozuksa her desen "eşleşme yok" sanılmasın. Tarama
   yapılamazsa betik "temiz" anlamına gelen 0 koduyla değil, 2 koduyla çıkar ve push engellenir. Buna kapalı güvenli (fail closed) davranış denir. `tools/sizinti-tara-test.sh`
   hem engellenmesi hem geçmesi gereken senaryoları geçici bir depoda dener; `git grep`'i bilerek bozup 2
   kodunu bekleyen senaryolar da içerir. `ToolsSizintiTaramaTest` bu betiği `./mvnw verify` içinde koşar.
   Yalnız çıkış koduna değil, çıktıda "SONUÇ: tüm sızıntı tarayıcı senaryoları geçti" satırının
   bulunmasına da bakar.

Atlanan testler de aynı ailenin bir riskidir. `OnYuzDavranisTest` tarayıcı kurulu değilse yerelde
atlanır; ama ortamda `CI` değişkeni varsa hata verir, böylece CI'da davranış testleri sessizce kaybolmaz.
ADR-0021'in son cümlesi bu metnin ana fikrini özetler: yeşil derleme davranışın sınandığı anlamına
gelmez; rapordaki çalışan ve atlanan test sayılarına bakılır.

**Bekçinin de kör noktası olabilir.** Bu metin hazırlanırken fark edilip düzeltilen bir örnek, aynı ilkeyi bu deponun
kendisinde gösterir. `pom.xml`, surefire'a Türkçe yerel ayarı verir
(`-Duser.language=tr -Duser.country=TR`). Amaç "id" → "İD" harf dönüşümü hatalarını her makinede yakalamaktır.
`SemaTasinabilirlikTest` ise şema metnini aramadan önce yerel ayar belirtmeden `toUpperCase()` ile büyük
harfe çeviriyordu. Türkçe yerel ayarda küçük "i" noktalı "İ" olur. Küçük harfle yazılmış `id identity` metni
`İD İDENTİTY` olur ve `ID IDENTITY` kalıbı onu yakalamaz. Küçük harfli bir `create index` satırı da
`CREATE İNDEX` olur ve idempotentlik kuralına takılmaz. `schema.sql` anahtar kelimeleri büyük harfle
yazdığı için bekçi o güne kadar işini yapıyordu. Ama küçük harfli bir satır bu iki kontrolden sessizce
geçerdi. Düzeltme iki adımdı: önce küçük harfli örnek metinlerle bekçiyi deneyen
`kucukHarfliH2SozdizimiDeYakalanir` testi yazıldı ve eski kodda kırmızı yandı; sonra yerel ayar sabitlendi
(`toUpperCase(Locale.ROOT)`) ve test yeşile döndü. Bu kör nokta §3'teki soruyla bulunur: "şemayı küçük harfle
bozsam test fark eder miydi?"

Bir bekçi testini incelerken dört soru sor:

1. Kuralı bilerek çiğnersem kırmızı olur mu? (Bu, elle üretilmiş bir mutanttır.)
2. Taradığı şeyin boş olmadığını doğruluyor mu?
3. Atlanırsa (skipped) bunu biri görür mü, CI'da kırmızı olur mu?
4. Ajan bekçiyi düzenleyebilir mi? Düzenlerse bunu kim fark eder? Bu depoda bugünkü cevap diff ve insan
   incelemesidir; ayrıntısı [`harness-mekanigi.md`](harness-mekanigi.md) §5.2'de.

### 7. ⚠️ ZORUNLU ŞERH: "AI %100 kapsamlı test yazar" ve araç iddiaları nasıl okunur
Test kalitesi alanı pazarlama cümlesine çok uygundur, çünkü sayı üretmek kolaydır.

- **"%100 kapsam" iddiası.** Kapsam neyin çalıştığını söyler (§2). Fowler %100 gördüğünde şüphelenir.
  Google, sayıyı %100'e yaklaştırmaya odaklanmanın sahte bir güvenlik duygusu ürettiğini yazar. Bir araç ya
  da bir ajan "%100 kapsam" dediğinde üç soru sor: Testlerde kaç doğrulama var? Mutasyon skoru ne? Bu testler
  hangi gerçek hatayı yakaladı?
- **Kapsamı hedefleyen araçlar.** Bazı AI test araçları amacını doğrudan kapsam üzerinden tanımlar. Qodo
  Cover'ın README dosyası aracın amacını kapsamı verimli biçimde artırmak diye yazar ve istenen kapsam
  yüzdesi için bir parametre (`--desired-coverage`) sunar (depo Haziran 2025'ten beri bakımsızdır). Meta'nın
  TestGen-LLM makalesi (FSE 2024) daha dürüst bir çerçeve verir: üretilen testlerin %75'i doğru derlenmiş,
  %57'si güvenilir biçimde geçmiş, %25'i kapsamı artırmış; araç yalnız bu süzgeçlerden geçen testleri
  önerir. Bu dürüstlük değerlidir. Yine de "kapsamı artırdı", "hata yakalar" demek değildir. Aynı makale
  mühendislerin önerilerin %73'ünü kabul ettiğini yazar. Kabul oranı bir ölçüttür ama hata yakalama oranı
  değildir; üstelik şirketin kendi platformunda ölçülmüş, kendi raporudur.
- **Mutasyon aracının kendi vitrini.** PIT'in ana sayfası aracı "son teknoloji" (state of the art) ve "altın
  standart test kapsamı" diye tanıtır. Eski araçların günlerce yaptığını dakikalar içinde yaptığını söyler.
  Sayfada bu karşılaştırmanın ölçümü yoktur; ücretli bir sürümün (arcmutate) tanıtımı ise vardır. Araç
  gerçekten iyi olabilir, ama sıfatlar kanıt değildir. Mutasyon skoru da bir sayıdır ve o da oynanabilir.
  Alakasız ayrıntılara doğrulama ekleyerek mutant öldürmek testleri kırılgan yapar. Eşdeğer mutantlar da
  %100 hedefini anlamsız kılar. Google'ın kendi makalesinde başlangıçta raporlanan mutantların %85'inin
  verimsiz bulunması, ham skora neden güvenilmeyeceğini gösterir.
- **"Gerçek veritabanıyla test ettik" iddiası.** Testcontainers üretimdeki **motoru** getirir. Üretimdeki
  veriyi, yükü ve yapılandırmayı getirmez; sürümü de ancak sen aynı imajı seçersen tutar. Bu cümle
  "üretimde çalışır" demek değildir.
- **Ajanın kendi raporu.** "Testleri yazdım, hepsi geçiyor" bir iddiadır. Kanıtı, komutun çıktısı ve
  testin hatalı kodda kırmızı olduğunun gösterilmesidir.

Genel kural: bir test metriği gördüğünde üç şey sor. **Kim üretti** (satıcı mı, ajan mı, bağımsız bir ölçüm
mü)? **Neyi ölçüyor** (çalışmayı mı, kontrolü mü, kabulü mü)? **Hangi hatayı yakaladığını gösteriyor mu?**

### 8. Ajanın yazdığı testi incelerken: kısa kontrol listesi
1. Her testte hatalı kodda kırmızı olacak bir doğrulama satırı var mı? Hangisi?
2. Beklenen değer nereden geliyor: spec'ten mi, kodun kendi çıktısından mı?
3. Sahte nesne yalnız dış bağımlılıklarda mı, yoksa test edilen sınıfın kendisi mi taklit edilmiş?
4. Bu diff'te test dosyası değişti mi? Değiştiyse neden: davranış mı değişti, test mi "düzeltildi"?
5. Hata düzeltmesinde testin önce kırmızı olduğunu gösteren bir çıktı var mı?
6. Sınır değerleri (0, -1, üst sınır, boş, `null`) denenmiş mi? (§3'teki `dogruIndex` örneği.)
7. Davranış motora özgüyse (kısıt, transaction, lehçe) test gerçek motora karşı mı koşuyor?
8. Atlanan test var mı ve atlandığını biri görüyor mu?

Bu soruların hiçbiri ajanlara özgü değil. Ajanla birlikte değişen şey ölçektir: ajan çok sayıda test
ürettiği için soruların bir kısmını makineye devretmek gerekir. 1 ve 6. soruların cevabını mutasyon testi
verir; 8. soruyu CI'da atlamaya izin vermeyen bekçiler cevaplar. 2, 3 ve 4. sorular ise insan incelemesinde
kalır, çünkü beklenen değerin nereden geldiğini ve bir test değişikliğinin meşru olup olmadığını yalnız
amacı bilen biri yargılayabilir.

## Öz-sınav (kendini test et)
1. Satır kapsamı ile dal kapsamı arasındaki farkı `if (a && b)` örneğiyle anlat. İkisi de neden test
   kalitesinin ölçüsü değildir?
2. Fowler'ın anlattığı doğrulamasız test vakasında kapsam nasıl %100'e yaklaşabildi? Kapsamın gerçekten işe
   yaradığı kullanım nedir?
3. Google'ın kapsam yazısındaki asimetri nedir? Yüksek kapsam neyi garanti etmez, düşük kapsam neyi
   garanti eder?
4. Totolojik test nedir? Kent Beck'e göre kodun çıktısını testin beklenen değerine kopyalamak neyi boşa
   çıkarır?
5. `bosCevaptaDaKullanimKaydiYazilirSonraHataFirlatilir` testinde hangi doğrulama eski hatalı kodu yakalar,
   hangisi yakalamaz? Neden?
6. Mutant, öldürüldü, yaşadı, kapsanmadı ve eşdeğer mutant terimlerini tanımla. Mutasyon skorunu %100'e
   zorlamak neden çoğu zaman anlamsızdır?
7. `QuizUretimService`'teki `dogruIndex() < 4` koşulunda, sınır testi eklenmeden önce hangi mutantın
   yaşaması beklenirdi, neden? Hangi test eklendi?
8. Google mutasyon testini nasıl ölçeklendirdi? Üç önlem say. Ham mutant raporunun sorunu neydi?
9. `IlerlemeServiceYarisTest` yeşilken "yakala, yeniden oku" yolu gerçek veritabanında neden hiç çalışmadı?
   Bu vakadan sahte nesneler hakkında hangi ders çıkar?
10. Bu deponun çarptığı iki H2 ve PostgreSQL farkını say. SokratAI Testcontainers yerine ne kullanıyor ve
    bunun bedeli ne?
11. Testcontainers'ta statik alandaki ve örnek alanındaki konteyner arasındaki fark nedir? Spring Boot'un
    test bağlamı önbelleğiyle birlikte hangi tuzak doğar?
12. "Önce kırmızı" sırasının beş adımını say. Ajanla çalışırken kırmızıyı görmek neden şarttır?
13. `tarama_bos_degil` hangi arızayı önler? Bu depodaki başka iki "boş geçme" korumasını say.
14. **Şerh sorusu:** Bir araç "AI testlerimiz %100 kapsam sağlıyor, mühendisler önerilerimizin %73'ünü kabul
    etti" diyor. Bu iki sayıyı nasıl okursun, hangi soruları sorarsın?

### Cevap anahtarı (önce kendin cevapla, sonra bak)
1. Satır kapsamı satırda en az bir komut çalıştı mı diye bakar. `a` ve `b` doğru olan tek bir test satırı
   tamamen çalıştırır. Dal kapsamı `a` yanlış ve `b` yanlış dallarını ayrıca sayar. İkisi de yalnız
   "çalıştı mı?" diye sorar, sonucun kontrol edilip edilmediğini görmez.
2. Testler her metodu çağırdı ama hiçbir şeyi doğrulamadı; çağrılan her satır "kapsanmış" sayıldı. Kapsamın
   işe yaradığı yer test edilmemiş kod bölgelerini bulmaktır, kaliteyi sayıyla ifade etmek değil.
3. Yüksek kapsam test kalitesini garanti etmez. Düşük kapsam ise ürünün geniş bölümlerinin hiç otomatik test
   görmediğini garanti eder. Kapsam eksiği gösterir, yeterliliği göstermez.
4. Beklenen değerin test edilen kodla aynı yoldan üretildiği testtir. Kopyalamak çift kontrolü boşa
   çıkarır; TDD'nin doğrulama değerinin büyük kısmı o çift kontrolden gelir.
5. İstisna türünü kontrol eden satır eski kodda da geçerdi, çünkü eski kod da aynı istisnayı fırlatıyordu.
   `verify(kullanimRepo).save(...)` ve token sayısı kontrolü eski kodda kırmızıdır, çünkü eski kod kaydı
   yazmadan fırlatıyordu.
6. Mutant koda bilerek konmuş küçük bir hatadır. Öldürüldü: en az bir test kırmızı oldu. Yaşadı: satırı
   çalıştıran testler oldu ama hepsi yeşil kaldı. Kapsanmadı: satırı hiçbir test çalıştırmadı. Eşdeğer
   mutant davranışı değiştirmez; hiçbir test onu öldüremez, bu yüzden %100 çoğu zaman ulaşılamaz bir
   hedeftir.
7. `< 4` koşulunu `<= 4` yapan sınır mutantı. Testteki taşmış indeks 7'dir ve iki koşulda da reddedilir; 4
   değeri hiç denenmiyordu. Eklenen test 0 ve 3'ün kabul edildiğini, -1 ve 4'ün ayıklandığını doğrular
   (`dogruIndexSinirlari_0ve3Gecer_eksi1ve4Ayiklanir`). `>= 0` koşulundaki sınır mutantı ise büyük olasılıkla ölür, çünkü bir
   test 0 indeksli geçerli soru bekler.
8. Yalnız kod incelemesindeki değişikliği mutasyona soktu; bir satırda en fazla bir mutant üretti; log
   satırı gibi kurak düğümleri atladı (ve yaşayan mutantları inceleme ekranında gösterdi). Başta raporlanan
   mutantların %85'i geliştiricilerce verimsiz bulunmuştu.
9. Mock `DuplicateKeyException`'ı doğrudan fırlatıyordu. Gerçek veritabanında Spring Data JDBC çakışmayı
   `DbActionExecutionException` içine sardı ve kod yalnız doğrudan türü yakaladığı için yol hiç çalışmadı.
   Ders: mock altyapı hakkında bir iddiadır; iddiayı bir kez gerçek altyapıya karşı doğrula, sonra hızlı
   teste çevir.
10. Herhangi ikisi: `IDENTITY`/`CLOB`/`ADD CONSTRAINT IF NOT EXISTS` PostgreSQL'de yok; tırnaklı ve
    tırnaksız adlarda harf büyüklüğü farkı; salt okunur işlemde `DELETE`'i PostgreSQL reddeder, H2
    göstermez. Yerine her koşuda çalışan `SemaTasinabilirlikTest` ve isteğe bağlı
    `SitePostgresEntegrasyonTest` kullanılır. Bedel: gerçek PostgreSQL testi CI'da koşmaz, biri
    `PG_TEST_URL` ile koşturmazsa hiç koşmaz.
11. Statik alandaki konteyner sınıftaki testlerce paylaşılır ve bir kez başlar; örnek alanındaki her test
    metodu için başlar ve durur. Uzantı statik konteyneri sınıf bitince durdurur, ama Spring önbelleğe
    aldığı bağlamı sonraki sınıfta yeniden kullanabilir; durmuş konteynere bağlı bean'ler hata verir. Çare,
    konteyneri Spring bean'i olarak yönetmektir.
12. Yeniden üret, kırmızıyı gör ve sebebini oku, nedeni düzelt, yeşili gör, testi regresyon testi olarak
    bırak. Kırmızı görülmeden gelen yeşil, testin hatayı ayırt ettiğini kanıtlamaz; ajan testi ya da girdiyi
    özel duruma çevirerek de yeşil üretebilir.
13. Klasör adı değişince testin hiçbir dosya taramadan sessizce geçmesini önler. Diğerleri (herhangi
    ikisi): `withOptionalLayers(false)`, `pom.xml`'de `MimariTestleri`'nin ayrıca listelenmesi,
    `gercekPostgresKullaniliyor`, sızıntı tarayıcısının PCRE öz sınaması ve 2 koduyla çıkışı,
    `OnYuzDavranisTest`'in CI'da atlamak yerine hata vermesi.
14. Kapsam neyin çalıştığını söyler, neyin kontrol edildiğini söylemez; %100 tek başına şüphe sebebidir.
    Kabul oranı mühendislerin öneriyi beğendiğini gösterir, testin hata yakaladığını göstermez. Sorular:
    doğrulama yoğunluğu ve mutasyon skoru ne, bu testler hangi gerçek hatayı yakaladı, ölçümü kim yaptı,
    hangi kod tabanında yapıldı?

## Kaynaklar
- **Kent Beck, "Canon TDD"** (11 Aralık 2023). TDD'nin beş adımı ve her adımdaki hatalar; özellikle kodun
  çıktısını beklenen değere kopyalamanın çift kontrolü boşa çıkarması.
  https://newsletter.kentbeck.com/p/canon-tdd
- **Martin Fowler, "TestCoverage"** (17 Nisan 2012). Kapsam test edilmemiş kodu bulmak için yararlı, kalite
  göstergesi olarak zayıf; hedef yapılınca oynanması; %100'e şüphe.
  https://martinfowler.com/bliki/TestCoverage.html
- **Martin Fowler, "AssertionFreeTesting"** (3 Ağustos 2004). Doğrulamasız testlerle %100 kapsam vakası.
  https://martinfowler.com/bliki/AssertionFreeTesting.html
- **Martin Fowler, "SelfTestingCode"** (1 Mayıs 2014). Hata raporuna olağan tepki: önce hatayı açığa çıkaran
  test. https://martinfowler.com/bliki/SelfTestingCode.html
- **Adam Bender, Carlos Arguelles, Marko Ivanković, "Code Coverage Best Practices"**, Google Testing Blog
  (7 Ağustos 2020). Yüksek ve düşük kapsam asimetrisi, %60/%75/%90 rehberi, tepeden zorunluluğa karşı tutum,
  mutasyon testinin daha iyi teknik olması.
  https://testing.googleblog.com/2020/08/code-coverage-best-practices.html
- **Laura Inozemtseva, Reid Holmes, "Coverage Is Not Strongly Correlated with Test Suite Effectiveness"**,
  ICSE 2014. Beş sistem, 31.000 test takımı; kapsam ile etkililik arasında düşükten orta düzeye ilişki.
  https://cs.ubc.ca/~rtholmes/papers/icse_2014_inozemtseva.pdf
- **René Just ve ark., "Are Mutants a Valid Substitute for Real Faults in Software Testing?"**, FSE 2014.
  357 gerçek hata; mutant yakalama ile gerçek hata yakalama arasında kapsamdan bağımsız anlamlı ilişki.
  https://homes.cs.washington.edu/~mernst/pubs/mutation-effectiveness-fse2014-abstract.html
- **Goran Petrović, Marko Ivanković, Gordon Fraser, René Just, "Practical Mutation Testing at Scale: A view
  from Google"** (arXiv 2021, IEEE TSE). Kod incelemesinde değişiklik bazlı mutasyon, satır başına tek
  mutant, kurak düğümler, verimli mutant oranı. https://arxiv.org/abs/2102.11378
- **PIT belgeleri.** Ana sayfa (araç iddiaları): https://pitest.org/ · Temel kavramlar (sonuç türleri,
  eşdeğer mutant, kapsamla test seçimi): https://pitest.org/quickstart/basic_concepts/ · Operatörler:
  https://pitest.org/quickstart/mutators/ · Artımlı analiz: https://pitest.org/quickstart/incremental_analysis/
  · Maven eklentisi: https://pitest.org/quickstart/maven/ · JUnit 5 eklentisi:
  https://github.com/pitest/pitest-junit5-plugin
- **JaCoCo, "Coverage Counters".** Satır ve dal kapsamının tanımı.
  https://www.jacoco.org/jacoco/trunk/doc/counters.html
- **Testcontainers belgeleri.** JUnit 5 entegrasyonu: https://java.testcontainers.org/test_framework_integration/junit_5/
  · Tekil konteyner deseni: https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/
  · Docker gereksinimi: https://java.testcontainers.org/supported_docker_environment/ · H2 yerine gerçek
  veritabanı rehberi: https://testcontainers.com/guides/replace-h2-with-real-database-for-testing
- **Spring Boot, "Testcontainers".** `@ServiceConnection` ve bağlam önbelleği ile konteyner yaşam döngüsü
  uyarısı. https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- **H2 Database, "Features: Compatibility".** Uyumluluk modlarının farkların yalnız küçük bir kısmını taklit
  etmesi. https://www.h2database.com/html/features.html
- **ArchUnit User Guide, §10.4 "Fail Rules on Empty Should".** Boş sınıf kümesine uygulanan kuralın
  varsayılan olarak başarısız sayılması. https://www.archunit.org/userguide/html/000_Index.html
- **Andreas Zeller, *The Debugging Book*, "Introduction to Debugging".** Bilimsel yöntem, hatayı yakalayan
  test ekleme, "en bariz düzeltme" karşıtı örnek. https://www.debuggingbook.org/html/Intro_Debugging.html
- **OpenAI, "Detecting misbehavior in frontier reasoning models"** (10 Mart 2025). Kodlama görevlerinde ödül
  istismarı: `verify` fonksiyonunu yamamak, testleri atlatmak, doğrulamayı silmek.
  https://openai.com/index/chain-of-thought-monitoring/
- **Anthropic, Claude Code belgesi, "Best practices for Claude Code".** Başarısız testle yeniden üretme,
  ajana çalıştırılabilir kontrol verme, kanıt isteme. https://code.claude.com/docs/en/best-practices
- **Nadia Alshahwan ve ark., "Automated Unit Test Improvement using Large Language Models at Meta"**, FSE
  2024. Süzgeç oranları ve kabul oranı (şirketin kendi raporu). https://arxiv.org/abs/2402.09171
- **Christopher Foster ve ark., "Mutation-Guided LLM-based Test Generation at Meta"** (2025). Dil modeliyle
  kaygıya özgü mutant ve onu öldüren test üretimi (şirketin kendi raporu). https://arxiv.org/abs/2501.12862
- **Qodo Cover README.** Aracın kapsamı artırma amacı ve `--desired-coverage` parametresi; bakım durumu.
  https://github.com/qodo-ai/qodo-cover

*Uyarlama notu:* Bu metin bir çeviri değil, yukarıdaki kaynaklardan özgün bir sentezdir. SokratAI deposuna
dair her iddia ilgili dosya okunarak doğrulandı; PIT bu depoda koşturulmadığı için §3'teki yaşayan mutant
örneği okumaya dayalı bir tahmin olarak verildi.

### Bu programdaki ilgili kaynaklar
- Aynı dizinin kardeş metinleri (AI çağında teslimat döngüsü): [`tehdit-modelleme-stride.md`](tehdit-modelleme-stride.md), [`api-sozlesme-once.md`](api-sozlesme-once.md), [`teslimat-hatti-gozlemlenebilirlik.md`](teslimat-hatti-gozlemlenebilirlik.md).
- [`temel-developer-testing.md`](temel-developer-testing.md): test türleri, test piramidi, TDD döngüsü,
  test sahteleri. Bu metin onların üzerine kurulur.
- [`harness-mekanigi.md`](harness-mekanigi.md) §5.1–5.3: doğrulama kapısı, "kanıtı kanıtla", ajanın kendi
  yargıcını düzenleyememesi, düzeltmeyi kurala çeviren cırcır.
- [`hafta09-hexagonal-clean.md`](hafta09-hexagonal-clean.md): "Test edilebilirlik" bölümü; çekirdek testi
  ile adaptör testi ayrımı, §4'teki "ne zaman gerçek altyapı" sorusunun zeminidir.
- [`evals-llm-observability.md`](evals-llm-observability.md): deterministik olmayan LLM çıktısı için test
  yerine eval (değerlendirme testi); TDD'nin LLM karşılığı.
- [`ajan-calisma-zamani-kontrolleri.md`](ajan-calisma-zamani-kontrolleri.md): derleme anındaki kapı ile
  çalışma zamanındaki kapının farkı; bu metindeki bekçiler birinci türdendir.
- Dersler: Hafta 1 Gün 4 (kendini değerlendirme tuzağı), Hafta 3 Gün 4 (öz denetimli inceleme döngüsü),
  Hafta 4 Gün 1–2 (belgelemek yetmez, zorlamak gerekir; ArchUnit), Hafta 9 Gün 4 (ArchUnit ile hexagonal
  mühürleme).
