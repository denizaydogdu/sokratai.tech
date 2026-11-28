---
baslik: "AI Çağı Yazılım Mimarlığı: 12 Haftalık Program"
hafta_sayisi: 12
---

## Bu program sana ne kazandırır?

Yapay zekâ, kod yazmayı ucuzlattı. Değerli olan artık kodu yazmak değil; **ne yazılacağına karar vermek ve doğru yazıldığını güvence altına almak**. Bu 12 hafta seni o role taşır: satır üreten mühendisten, ortamı, niyeti ve geri beslemeyi tasarlayan mimara.

Program sonunda şunları yapabiliyor olacaksın:

- Bir yapay zekâ ajanının güvenle çalışabileceği bir repo kurmak: AGENTS.md, düzenli `docs/`, karar kayıtları (ADR).
- Mimari kararları ödünleşimleriyle (trade-off) birlikte savunmak ve C4 diyagramlarıyla anlatmak.
- Katman ve güvenlik kurallarını insan dikkatine bırakmadan otomatik kontrol kapılarıyla (ArchUnit, SAST) zorunlu kılmak.
- RAG, değerlendirme (evaluation), maliyet ve gecikme (latency) kararlarını gerekçeyle vermek.
- Dağıtık sistem ve veri kararlarını (servis sınırı, replikasyon, tutarlılık) bir çerçeveyle tartışmak.
- Öğrendiğini bir junior'a aktarmak ve bitirme projesiyle (capstone) hepsini tek işte birleştirmek.

## Nasıl işliyor?

Her haftanın bir teması var; tema beş güne bölünüyor. Bir gün şu ritimde geçer:

1. **Oku (~8 dakika):** günün kısa çalışma metni. Kaynak İngilizceyse Türkçesi verilir; teknik terimlerin İngilizcesi parantez içinde durur.
2. **Uygula (~10 dakika):** kendi projende küçük ama gerçek bir değişiklik yaparsın (bir ADR yazmak, bir kural eklemek, bir test kurmak gibi).
3. **Sına (~2 dakika):** kısa quiz ve özet; anlamadığın yeri sonraki güne taşı.

Toplam **günde yaklaşık 20 dakika**, haftada beş gün. Kalan iki gün telafi ve tekrar içindir; haftalık yük iki saatin altında kalır.

Ritmin üç kuralı: (1) her gün küçük ama **gerçek bir çıktı** üret (sadece okumak sayılmaz); (2) geride kalırsan günü atlama, telafi gününü kullan; (3) hafta biterken o haftanın çıktısı kendi deponda görünür olmalı.

İlerlemen kaydedilir. Her hafta bir önceki haftanın çıktısı üzerine kurulur, bu yüzden sıra önemlidir.

Programın yanında sırasız iki bölüm daha var: **Prompt Atölyesi** (yapay zekâya iş yaptırma tekniklerini deneyerek öğrenirsin; kilit yok) ve **Mülakat** (öğrendiğini soru-cevapla sınarsın). İkisi de haftaların akışını beklemez.

## Hafta indeksi

- Hafta 1: [Harness Mühendisliği Temelleri](hafta-01/hafta.md). Rolünün nasıl değiştiğini kavra: artık kod yazan değil, ortamı, niyeti ve geri beslemeyi tasarlayan mühendissin.
- Hafta 2: [Repo Bilgi Sistemi: AGENTS.md + docs/](hafta-02/hafta.md). Repoyu bilginin tek doğru kaynağı ('kayıt sistemi') yap: içindekiler tablosu görevi gören AGENTS.md, düzenli bir docs/ klasörü, ilk ADR'ler.
- Hafta 3: [Spec-Driven Akış ve Ajan Yönetimi](hafta-03/hafta.md). İyi bir görev spec'i (task spec) yazmayı ve Research → Plan → Execute → Review → Ship (araştır → planla → uygula → incele → yayınla) akışını kurmayı öğren.
- Hafta 4: [Guardrail'lar: Mimari Zorlama + Güvenlik](hafta-04/hafta.md). ArchUnit (Java) ve import-linter (Python) ile katman kurallarını otomatik denetime bağla; SAST (statik güvenlik taraması) kapılarını kur.
- Hafta 5: [Mimari Düşünme I: Nitelikler ve Trade-off](hafta-05/hafta.md). Fundamentals of Software Architecture kitabı (2. baskı), Bölüm 1–8: mimari karakteristikler, ödünleşim (trade-off) analizi, modülerlik.
- Hafta 6: [Mimari Düşünme II: Stiller ve İletişim](hafta-06/hafta.md). Fundamentals kitabının (2. baskı) ikinci yarısı: mimari stiller (katmanlı/layered, hexagonal, mikroservis…) ve C4 ile diyagram çizme.
- Hafta 7: [AI Engineering I: Stack, Modeller, Prompt](hafta-07/hafta.md). Chip Huyen'in kitabının ilk yarısı: AI katmanları (AI stack), temel model (foundation model) seçimi, prompt mühendisliği (prompt engineering) disiplini.
- Hafta 8: [AI Engineering II: RAG, Fine-tune Kararı, Evaluation](hafta-08/hafta.md). Kitabın ikinci yarısı: RAG mimarisi, ince ayarın (fine-tuning) ne zaman gerektiği, modeli hakem olarak kullanarak (AI-as-judge) değerlendirme, maliyet ve gecikme.
- Hafta 9: [Hexagonal / Clean Mimari (Ellerini Kirlet)](hafta-09/hafta.md). Get Your Hands Dirty on Clean Architecture kitabıyla port-adapter mimarisini Java'da uygula; kuralları ArchUnit testleriyle sabitle.
- Hafta 10: [The Hard Parts: Dağıtık Kararlar](hafta-10/hafta.md). Servis büyüklüğü (granularity), bağlaşım (coupling), dağıtık veri ve transaction (işlem bütünlüğü): 'kolay cevabı olmayan' kararlar için bir çerçeve.
- Hafta 11: [Veri-Yoğun Sistemler (DDIA)](hafta-11/hafta.md). DDIA kitabının (2. baskı) çekirdeği: depolama motorları, replikasyon, bölümleme (partitioning), tutarlılık. Veri mimarisi refleksi kazan.
- Hafta 12: [Liderlik, Mentorluk ve Capstone](hafta-12/hafta.md). Junior mentorluk sistemini kur, ekip anlaşmasını yayınla, mimari kata (mimari tasarım alıştırması) ile her şeyi birleştir.

## Başlarken

Hafta 1, Gün 1 ile başla. Elinde üzerinde çalışacağın gerçek bir proje olsun, çünkü alıştırmalar çıktıyı kendi deponda üretmeni ister. Temel kavramlar ("model", "prompt", "token", "bağlam penceresi", "ajan") Prompt Atölyesi'nin ilk bölümünde sıfırdan tanımlı.
