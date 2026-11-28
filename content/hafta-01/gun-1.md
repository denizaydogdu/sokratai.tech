---
hafta: 1
gun: 1
baslik: "İnsan yönlendirir, ajan uygular (Humans steer, agents execute) ilkesi"
---

## Ders

### Bugün ne öğreneceksin?
Bugün tek bir şeyi kavrayacaksın: yazılım mühendisinin işi "kod yazmak"tan "ajanın güvenilir çalışacağı ortamı kurmak"a kayıyor. Bu ortamın adı **harness**. Bunu bilmezsen ajanla çalışırken vaktinin çoğu onun çıktısını düzeltmekle geçer. Bilirsen düzeltme işini sistemin kendisine yaptırırsın. Sloganı ezberle: *"Humans steer. Agents execute."* (İnsanlar yönlendirir, ajanlar uygular.)

### Temel kavramlar
- **Ajan (agent):** senin adına adım adım iş yapan, kendi kararıyla dosya okuyup yazan, komut çalıştıran yazılım. Sohbet botundan farkı: cevap vermekle kalmaz, **uygular**.
- **Harness (koşum takımı):** ajanın etrafına kurduğun ortam: kurallar, örnekler ve otomatik kontroller. Kelime, atı arabaya bağlayan dizgin ve kayış takımından gelir. Benzetme tam yerinde: **arabayı sen itmezsin, gücü zaten olan atı yönlendirirsin.**
- **Guide (yol gösterici):** iş BAŞLAMADAN önce ajana "ne yapacağını ve neyin doğru sayıldığını" söyleyen metin. Örnek: AGENTS.md, spec, kod şablonu.
- **Sensor (sensör):** iş BİTTİKTEN sonra sonucu ölçen ve hatayı ajana **anlatan** otomatik kontrol. Örnek: test, derleyici, CI.
- **Spec (spesifikasyon):** yapılacak işin ve "bitti" sayılma koşulunun yazılı hâli.

### Somut örnek
Guide ile sensor'ün birlikte nasıl çalıştığını, şu an kullandığın uygulamanın kendi deposunda görebilirsin:
- **Guide:** Depo kökündeki `AGENTS.md` şunu yazar: *"katman yönü: types → config → repo → service → ai → web"* ve *"Claude çağrısı yalnız ai/ paketinden"*. Ajan kod yazmadan **önce** bu dosyayı okur.
- **Sensor:** `src/test/java/tr/velzon/aiogretmen/MimariTestleri.java` (ArchUnit) aynı kuralı **otomatik** olarak denetler. Ajan web katmanından doğrudan repo katmanına erişirse `./mvnw verify` başarısız olur (kırmızı yanar) ve hatanın ne olduğunu testin adı söyler: `katman_kurali`.
- **Döngü:** ajan kodu yazar → test başarısız olur → ajan hata mesajını okur → hatayı kendisi düzeltir → test geçer. Sen katman kuralını elle kontrol etmezsin; kuralı test korur.

Bu döngünün büyük ölçekte uygulanmış hâli OpenAI'ın deneyidir: üç mühendis boş bir depodan başlayıp beş ayda yaklaşık 1 milyon satırlık gerçek bir ürünü **elle tek satır kod yazmadan** çıkardı. Asıl önemli olan bu sayı değil, insanların hangi işlere geçtiği: önceliklendirme, spec yazma, doğrulama ve harness'ı iyileştirme. Kod incelemesini bile büyük ölçüde ajanlar birbirine yaptı. Yazı buna esprili bir ad veriyor: **"Ralph Wiggum döngüsü"** (adını Simpsonlar dizisinin saf ama yılmayan karakterinden alır). İnceleme ve düzeltme, bütün inceleyiciler tatmin olana kadar sabırla tekrarlanır. İnsan bu döngüye yalnız karar vermek gerektiğinde girer.

### Sık yapılan hata
Ajan yanlış yaptığında refleks olarak **"bir daha dene", "prompt'u güzelleştir"** demek. Bu, aynı ortamda aynı hatayı yeniden üretir. Doğru soru şudur: *"Ortamda hangi yetenek eksikti? Bu eksiği ajan için hem **okunur** (bir yerde yazılı) hem **zorlanabilir** (otomatik kontrol edilen) bir hâle nasıl getiririm?"* Yani hataları tek tek düzeltmezsin; ajanın o hatayı bir daha yapamayacağı ortamı kurarsın. OpenAI deneyinde de başta ilerleme yavaştı. Sebep ajanın yetersizliği değil, ortamın henüz kurulmamış olmasıydı.

### Görevlere geçmeden önce
Aşağıdaki görevler bu kavramı kendi işine uyarlaman için: önce kaynağı okuyacaksın, sonra yeni rolünü kendi cümlelerinle yazacaksın (kavramı gerçekten anlayıp anlamadığın ancak orada belli olur), en sonda hâlâ elle yaptığın işleri listeleyeceksin. O liste, programın geri kalanında kuracağın harness'ın ilk malzemesi olacak.

## Görevler
- [ ] OpenAI Harness Engineering yazısını oku (belgedeki metin sende var)
- [ ] Kendi cümlelerinle 5 maddede 'benim yeni rolüm ne?' yaz
- [ ] Kendi (ya da bildiğin bir ekibin) iş akışında insanların hâlâ elle yaptığı 3 işi listele
