---
hafta: 6
gun: 2
baslik: "Stil kataloğu: dağıtık stiller"
---

## Ders

### Bugün ne öğreneceksin?
Bugün kodun **tek parça olmaktan çıkıp ayrı ayrı çalışan süreçlere** bölündüğü stilleri tanıyacaksın: neyi kazandırdıklarını, karşılığında hangi bedeli ödettiklerini ve bölme kararının neden aynı zamanda bir **ekip** kararı olduğunu.

### Temel kavramlar
- **Dağıtık sistem (distributed system):** Parçaların ayrı süreçlerde, çoğu zaman ayrı makinelerde çalıştığı ve birbirine **ağ üzerinden** konuştuğu sistem. Kritik fark: monolitte iki modül arasındaki çağrı ya çalışır ya derlenmez; ağda çağrı **kaybolabilir, gecikebilir, iki kez gidebilir.**
- **Service-based (servis tabanlı):** Uygulama birkaç kaba parçaya (ör. 4–8 servis) bölünür ama genelde **veritabanı ortak kalır**. Mikroservisin faydasının bir kısmını, bedelinin küçük kısmıyla verir; en sık atlanan ara duraktır.
- **Microservices (mikroservis):** Her servis kendi verisiyle, kendi başına dağıtılan küçük birim. **Kazandırdığı:** bağımsız dağıtım, servis başına ölçek, hata izolasyonu, ekip özerkliği. **Ödettiği bedel:** ağ gecikmesi, dağıtık hata ayıklama, veri tutarlılığı (tek bir `commit` yok), izleme/log altyapısı, sürüm uyumu.
- **Event-driven (olay güdümlü):** Parçalar birbirini doğrudan çağırmaz; olan biteni **olay** olarak duyurur, ilgilenen dinler. Postane benzetmesi: gönderen, alıcının o an müsait olmasını beklemez. **Kazandırdığı:** gevşek bağlaşım ve yüksek iş hacmi (throughput). **Bedeli:** akışın izlenmesi zorlaşır ("bu kayıt neden değişti?" sorusunun cevabı artık tek bir yerde yazmaz) ve tutarlılık **eninde sonunda** sağlanır, anında değil (eventual consistency).
- **Space-based (alan tabanlı):** Veritabanını darboğaz olmaktan çıkarmak için veriyi bellekte kopyalayan, aşırı yük altındaki sistemlere özgü stil. Adını bilmen yeter.
- **Conway yasası (1967):** *Bir sistemin yapısı, onu üreten organizasyonun iletişim yapısına benzemeye eğilimlidir.* Üç ayrı ekip varsa sistem er geç üç parçaya ayrılır. Pratik sonucu: **ekip yapısını değiştirmeden yapılan bölme kâğıt üstünde kalır**. Tersi de doğrudur: ekipleri istediğin mimariye göre kurarsan mimari kendiliğinden oraya akar (buna "ters Conway manevrası" denir).

Hafta 5'in birinci yasası burada en net hâlini alır: mikroservis "modern olan" değil, **belirli kazanımlar karşılığında ciddi karmaşıklık satın aldığın** bir takastır (trade-off).

### Somut örnek (bu projeden)
AI Öğretmen'i mikroservise bölmeye kalksan ortaya çıkacak parçalar bellidir: müfredat servisi, ilerleme/hafıza servisi, LLM geçidi (`ai/`). Kâğıt üstünde temiz görünür. Şimdi faturaya bak:
- Bugün müfredat okuma bir **metot çağrısı**dır (mikrosaniye). Bölünmüş hâlde bir **HTTP çağrısı**dır (milisaniye) ve **başarısız olabilir**. Yani her çağrının yapıldığı yere yeniden deneme (retry), zaman aşımı ve hata yolu yazman gerekir.
- Bugün `./mvnw verify` tek komutla her şeyi sınar. Bölünmüş hâlde üç servisi ayağa kaldırmadan uçtan uca test edemezsin.
- Bugün ekip **bir kişi**. Conway yasasının cümlesi acımasızdır: tek kişilik bir iletişim yapısı, üç servisli bir mimariyi taşıyamaz. Üç servisi de aynı kişi dağıtır, izler ve tamir eder. Yani özerklik kazancı **sıfır**, işletme yükü **üç kat**.

Buna karşılık olay güdümlü yaklaşımın küçük bir dozu bu projede zaten mantıklıdır: "öğrenci bir görevi tamamladı" olayı, ilerleme kaydı ile bildirim/özet üretimini birbirinden ayırabilir. Stil satın almak zorunda değilsin; **fikri ödünç almak** da bir seçenektir.

### Sık yapılan hata
- **Ölçek sorunu olmadan ölçek çözümü almak.** Mikroservisin çözdüğü asıl sorun teknik değil örgütseldir: çok ekibin birbirini beklemeden dağıtım yapması.
- **Dağıtık monolit üretmek.** Servisler ayrıldı ama hepsi aynı veritabanına yazıyor ve birlikte dağıtılıyorsa elinde monolitin bütün bağımlılıkları ve dağıtık sistemin bütün faturası kalır.
- **Ağı görünmez saymak.** "Nasılsa hızlı" varsayımı, ilk kesintiye kadar sürer.
- **Olay güdümlü mimariyi "asenkron yaptık" demekten ibaret sanmak.** İzleme (tracing) ve olay şeması disiplini yoksa hata ayıklama bir dedektiflik işine döner.

### Görevlere geçmeden önce
İkinci görev bilerek tersten kurulmuştur: "neden bölmeliyim" listesini herkes yazabilir; asıl olgunluk **kendi fikrini çürütebilmektir** (şeytanın avukatı). Beş maddeyi yazarken en az birini Conway yasasına, en az birini işletme yüküne (izleme, dağıtım, hata ayıklama), en az birini veri tutarlılığına dayandır. Sonunda şu soruya net bir cevabın olsun: *"Bölmeyi haklı çıkaracak olan koşul ne olurdu?"* Bu sorunun cevabı, gelecekte yazılacak ADR'nin tetikleyicisidir.

## Görevler
- [ ] Dağıtık stiller bölümünü oku
- [ ] 'Sistemimi neden mikroservise BÖLMEMELİYİM' başlıklı 5 maddelik savunma yaz (şeytanın avukatı egzersizi)
