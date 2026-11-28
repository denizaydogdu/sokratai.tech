---
hafta: 6
gun: 5
baslik: "Sentez + quiz"
---

## Ders

### Bugün ne öğreneceksin?
Bugün yeni kavram yok; bu hafta kurduğun **karar kasını** çalıştırıyorsun. Gün sonunda önüne konan bir senaryoya stil önerip önerinin bedelini aynı nefeste söyleyebilmelisin.

### Temel kavramlar
Haftanın zinciri: **stil kataloğu (Gün 1–2) → C4 ile iletişim (Gün 3) → ajanın okuyabildiği diyagram (Gün 4)**. Okunuşu: önce sistemin nasıl bölüneceğine karar verirsin, sonra bu kararı başkasına anlatabilmen gerekir, en sonunda anlattığın şeyin hem insana hem ajana ulaşan bir biçimde repoda durması gerekir.

Hepsi Hafta 5'in birinci yasasına dayanır: **stil seçmek = takas (trade-off) seçmektir.** Bir stil önerisi ancak şu üç parçayı içeriyorsa öneridir:
- **Bağlam:** kaç kullanıcı, kaç kişilik ekip, hangi yük profili? (Conway yasasını unutma: ekip yapısı mimarinin sınırıdır.)
- **Birinci sınıf nitelik:** bu senaryoda hangi "-ility" pazarlık dışı? (Hafta 5, Gün 2)
- **Feda edilen:** karşılığında neyi kaybediyorsun ve neden bu kayıp bu bağlamda ucuz?

Bu üçlüyü kuramıyorsan elindeki şey bir öneri değil, bir tercihtir.

### Somut örnek
Bir senaryonun tam çözümü, kopyalayacağın şablon:

> **Senaryo:** İki kişilik ekip, ayda 500 kullanıcılı bir randevu uygulaması yazıyor; yatırımcı demosu altı hafta sonra.
> **Bağlam:** küçük ekip, düşük yük, sıkı süre. **Birinci sınıf nitelik:** teslim hızı ve basitlik.
> **Öneri:** Modüler monolit (Layered), tek dağıtım.
> **Takas 1:** Elasticity'den vazgeçtim, çünkü 500 kullanıcıda ani sıçrama yok; gerekirse tüm uygulamanın kopyasını çıkarmak yeter.
> **Takas 2:** Bağımsız dağıtımdan vazgeçtim, çünkü iki kişilik ekipte zaten kimse kimseyi beklemiyor (Conway); mikroservisin çözdüğü örgütsel sorun bu ekipte yok.

Karşı örnek olarak bu projeyi hatırla: AI Öğretmen tek dağıtım birimi. Yerelde tek kişilik, 127.0.0.1'e bağlı bir araç; canlıda tek konteynerde çalışan, trafiği küçük, herkese açık bir eğitim sitesi. Aynı üçlü burada Layered'ı işaret ediyor ve `MimariTestleri.katman_kurali` bu kararı her derlemede sınıyor. Yani stil bu projede bir etiket değil, **sınanabilir bir taahhüt**.

### Sık yapılan hata
- **Senaryosuz stil tartışmak.** "Mikroservis mi monolit mi?" sorusunun bağlamsız cevabı yoktur; ilk yapılacak iş soruyu bağlamla doldurmaktır.
- **Takas yerine avantaj listelemek.** İki maddenin de kazanç olduğu bir öneri, mimari değil pazarlamadır.
- **Quiz'i ezberle çözmeye çalışmak.** Sorular tanım değil ayrım ölçer: mikroservisin kazancı ile bedeli, puan kartının doğru okunuşu, Conway'in pratik sonucu.
- **Karma tekrarı atlamak.** En zor görünen görev, en çok işe yarayanıdır.

### Görevlere geçmeden önce
Üç senaryoyu **bilerek farklı** seç: biri küçük ekip/düşük yük, biri çok ekipli/yüksek yük, biri veri işleme ağırlıklı (pipeline stilinin en güçlü olduğu yer). Aynı stili üç kez önerdiysen senaryolar birbirine çok benziyordur.

Üçüncü görev, programın ortasındaki **karma tekrardır** ve zor gelmesi normaldir: hatırlamaya çalışmak, yeniden okumaktan daha kalıcı öğretir (geri getirme pratiği / testing effect) ve bu etki aradan geçen süre uzadıkça **büyür**. Bu yüzden beş hafta önceki harness sorusu tam yerinde. Zorlandığın konuyu not al; Hafta 7'ye girmeden önce kapatman gereken açık odur.

## Görevler
- [ ] 3 mini senaryo yaz, her birine stil öner + 2 takas (trade-off) yaz
- [ ] Haftalık quiz'i çöz
- [ ] Program ortası KARMA tekrar: AI öğretmenden 1-5. haftalardan karışık bir quiz iste ve çöz.
      (Geri getirme pratiği, yani testing effect: hatırlamaya çalışmak, yeniden okumaktan daha kalıcı öğretir;
      etki, aradan geçen süre uzadıkça BÜYÜR. Bu yüzden 5 hafta önceki harness sorusu tam yerinde.)
