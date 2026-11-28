---
hafta: 11
gun: 3
baslik: "Replikasyon"
---

## Ders

### Bugün ne öğreneceksin?
Bugün aynı veriyi birden çok makinede tutmayı (**replikasyon**), bunun kaçınılmaz yan etkisi olan **gecikmeyi** ve bu gecikmenin kullanıcıya nasıl "sistem bozuk" gibi göründüğünü öğreneceksin. Sonunda şu cümleyi kurabileceksin: "Bu ekranda şu tutarlılık garantisini veriyorum, şunu vermiyorum."

### Temel kavramlar
**Replikasyon**, bir muhasebe defterinin **birden çok kopyasını** tutmaya benzer. Ortada bir **ANA DEFTER** (leader / lider) vardır; yeni kayıtlar yalnız oraya yazılır. Yanında **KOPYA DEFTERLER** (follower / replika) durur; bir kâtip, ana deftere düşen her satırı kopyalara geçirir. Neden? (a) Ana defter yanarsa veri kaybolmasın, (b) "bakiyem ne?" diye soran yüzlerce kişi ana deftere üşüşmesin, okumalar kopyalardan yapılsın.

Can alıcı nokta: **kâtip anında yetişemez.** Satırın kopyaya geçmesi 5 milisaniye de sürebilir, 5 saniye de. Bu gecikmeye **replikasyon gecikmesi (replication lag)** denir ve bir hata değil, **fiziğin faturasıdır**, çünkü kopyalar başka makinede, hatta başka şehirdedir.

Üç replikasyon modeli:

| Model | Kim yazabilir? | Ne zaman? | Ana bedeli |
|---|---|---|---|
| **Single-leader** (tek lider) | Tek düğüm. Kopyalar salt okunur. | Varsayılan seçim; klasik PostgreSQL/MySQL. | Lider darboğaz ve tek arıza noktası; çökerse yeni lider seçilene (failover) kadar yazma durur. |
| **Multi-leader** (çok lider) | Birden çok düğüm; sonra birbirlerine kopyalarlar. | Ayrı veri merkezleri; çevrimdışı çalışan istemciler (takvim uygulaması). | **Yazma çakışması:** iki lider aynı satırı farklı değiştirirse biri kazanmalı. Çakışma çözümü, çoğu ekibin başını yaktığı yerdir. |
| **Leaderless** (lidersiz) | Herhangi bir düğüm; istemci birden çok düğüme yazar. | Erişilebilirlik şartsa; Dynamo tarzı (Cassandra, Riak). | Tutarlılığı **istemci** hesaplar: kaça yaz (w), kaçtan oku (r)? `w + r > n` ise güncel veriyi görme ihtimalin yüksektir. Eskimiş düğümü onarmak (read repair) sana kalır. |

**Pratik kural:** Emin değilsen single-leader ile başla. Diğer ikisi sana erişilebilirlik verir, karşılığında **çakışma çözümünü** sırtına yükler. Üstelik bunlar kod değil, iş kuralı kararlarıdır ("aynı anda iki şehirden değişen adres alanında hangisi kazanır?").

### Somut örnek
Bir alışveriş sitesinde ürünün altına **yorum yazıyorsun**. Yazma ANA DEFTERE gider; sayfayı yenilediğinde okuma **rastgele bir KOPYAdan** gelir.

- **read-your-writes (kendi yazdığını görebilme):** "Gönder"e bastın, yazma ana deftere düştü. Sayfa yenilendi ama okuma kâtibin yetişemediği kopyaya gitti: **yorumun yok**. Kullanıcı "yorumum kayboldu mu?" deyip **bir daha** yazar; artık iki yorum var. Veri kaybı YOK; kaybolmuş gibi **görünen** şey, geride kalmış bir kopyaydı. Garanti: *kullanıcı KENDİ yazdığını her zaman görmelidir.* Nasıl sağlanır: kullanıcının kendi değiştirdiği veriyi bir süre (örn. 1 dk) doğrudan liderden oku.
- **monotonic reads (okumanın geriye gitmemesi):** Bu kez yorumun göründü. Bir kez daha yeniledin; bu okuma **daha geride kalmış başka bir kopyaya** düştü ve yorumun **yine kayboldu**. Bu daha rahatsız edicidir: kullanıcı veriyi bir kez GÖRDÜ, sonra geri alındı. Garanti: *okumalar zamanda geriye gitmesin* (dikkat: "hep en güncelini görürsün" demek değildir). Nasıl sağlanır: aynı kullanıcı hep aynı kopyadan okusun (kullanıcı kimliğinin özetine göre kopya seç).

### Sık yapılan hata
**"Eventual consistency (nihai tutarlılık) kullanıyoruz" demeyi bir cevap sanmak.** Nihai tutarlılık yalnızca şunu söyler: yazmayı durdurursan kopyalar **eninde sonunda** eşitlenir. "Sonunda ne zaman?" sorusuna cevap vermez; ölçmezsen "sonunda" saatler sürebilir. Bu ifadeyi bir pazarlama lafı olarak değil, bir **mühendislik sözleşmesi** olarak ele almalısın ve sözleşme iki şeyi sayıyla yazmanı ister: (1) kabul edilen gecikme ("replikasyon gecikmesi p99 < 500 ms; 5 sn'yi aşarsa alarm"), (2) verdiğin garanti ("sepette read-your-writes var; yorum listesinde yalnızca nihai tutarlılık, en fazla ~1 sn geride"). Bunu yazabiliyorsan sözleşmen var; yazamıyorsan yalnızca **umudun** var.

### Görevlere geçmeden önce
Yukarıdaki iki sahnenin hiçbirinde veri kaybolmadı ve kod hatalı değildi; yine de sistem bozuk göründü. Mimarın işi "gecikme olacak mı?" diye sormak değil (olacak), **"hangi ekranda hangi garantiyi veriyorum?"** diye karar vermektir. Bu bir mimari karardır: bir ADR'ye ve bir danışmaya (Hafta 10, Advice Process) konu olur.

## Görevler
- [ ] Bu haftanın kaynaklarındaki DDIA Türkçe çalışma metnini oku (İngilizce kitabın replikasyon bölümü isteğe bağlı tamamlayıcıdır)
- [ ] Sistemindeki 1 okuma yolunda replikasyon gecikmesinin (replication lag) kullanıcıya etkisini analiz et
