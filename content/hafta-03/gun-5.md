---
hafta: 3
gun: 5
baslik: "Uzun süren işler için desenler + quiz"
---

## Ders

### Bugün ne öğreneceksin?
Bu hafta tek bir ajanla çalışmayı öğrendin. Bugün, **tek oturumda bitmeyen** işlerde işi birden fazla ajana bölmenin iki standart desenini göreceksin. Daha da önemlisi, bunları **ne zaman kullanmaman** gerektiğini öğreneceksin. Haftayı quizle kapatacaksın.

### Temel kavramlar
- **Uzun süren iş (long-running task):** tek oturumda bitmeyen, bağlamın en az bir kez sıfırlanacağı, birden çok dosya ve adım içeren iş.
- **İki ajanlı desen:** **initializer** (hazırlayıcı) araştırır, bağlamı toplar, planı ve dosya listesini çıkarır; **coder** (kodlayıcı) yalnız bu hazır paketle kod yazar. Kazanç: kodlayıcının bağlamı araştırma artıklarıyla kirlenmez.
- **Üç ajanlı desen:** **planner** (plancı) → **generator** (üretici) → **evaluator** (değerlendirici). Değerlendirici, Gün 4'teki inceleme döngüsünün ayrı bir ajana verilmiş hâlidir.
- **Sprint contract (görev sözleşmesi):** üç ajanın da aynı şeyi anlamasını sağlayan yazılı anlaşma: hedef, kapsam, kabul kriterleri. Ajanlar arası ortak dil budur; olmazsa üretici plancıyı, değerlendirici üreticiyi yanlış anlar.
- **En basit çözüm ilkesi:** her ek ajan ekstra tur, ekstra token, ekstra gecikme ve ekstra yanlış anlaşılma demektir. Ajan sayısı bir maliyettir, bir erdem değil.

Haftanın akışını hatırla: **tarif (Gün 1) → plan (Gün 2) → temiz yürütme (Gün 3) → inceleme döngüsü (Gün 4)**. Çok ajanlı desenler bu akışı değiştirmez; yalnız adımları **farklı ajanlara dağıtır**.

### Somut örnek
Bu projedeki iki işi yan yana koy:

- **Kısa iş (tek ajan yeter):** "Gün kartına ilerleme yüzdesi ekle." Tek oturumda biter, dört-beş dosyaya dokunur, doğrulaması `./mvnw verify`. Buraya değerlendirici ajan koymak, iki dakikalık işe yarım saatlik tören eklemektir.
- **Uzun süren iş (bölmeye değer):** `docs/exec-plans/active/prompt-atolyesi.md` gibi, birden çok katmana (types → repo → service → ai → web) dokunan, günlere yayılan bir modül. Burada iki ajanlı desen doğrudan işe yarar: hazırlayıcı ajan mevcut yapıyı, ilgili ADR'leri (`docs/adr/`) ve dokunulacak dosyaları çıkarır, planı `docs/exec-plans/active/` altına yazar; kodlayıcı ajan **temiz bir bağlamla** yalnız o planı uygular. Değerlendiriciyi ancak iş, modelin tek başına güvenilir yapabileceği sınırı gerçekten aşıyorsa eklersin (çok adımlı, geri alması pahalı, kriterleri makineyle tam ölçülemeyen bir işse).

Karar ölçütü tek cümledir: **model bu işi tek başına güvenilir yapabiliyorsa ek ajan koyma.** Yapamıyorsa, önce hangi adımda tökezlediğine bak; ajanı oraya ekle.

### Sık yapılan hata
En sık hata, ajan sayısını **çözüm sanmaktır**: iş yürümeyince "bir de değerlendirici ekleyelim" denir. Genelde sorun ajan eksikliği değil, **tarifin ya da planın zayıflığıdır** (Gün 1-2). Zayıf tarifle üç ajan, tek ajandan daha hızlı yanlış yapar. İkinci hata, ajanlar arasında **yazılı sözleşme olmadan** çalışmaktır: plancının "bitti" dediği ile değerlendiricinin aradığı şey farklıysa döngü hiç kapanmaz, ajanlar birbirini düzeltip durur.

### Görevlere geçmeden önce
Önce kendi işlerine bak ve dürüstçe ayır: hangisi tek oturumda biter, hangisi bitmez? "Bitmez" dediklerin, bu haftanın bütün araçlarına (spec, ExecPlan, devir notu, inceleme döngüsü) ihtiyaç duyan işlerdir. Sonra haftalık quizi çöz: her soruda hangi günün konusunun sorulduğunu fark etmeye çalış; yanlış yaptığın soru, dönüp okuman gereken günü gösterir.

## Görevler
- [ ] Kendi işlerinden hangisinin 'uzun süren iş' sınıfına girdiğini belirle
- [ ] Haftalık quiz'i çöz
