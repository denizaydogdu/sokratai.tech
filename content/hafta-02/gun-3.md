---
hafta: 2
gun: 3
baslik: "ADR yazmayı öğren"
---

## Ders

### Bugün ne öğreneceksin?
Bugün dün açtığın `docs/adr/` klasörünü dolduracak biçimi öğreneceksin: **ADR**. Amaç, kararın kendisini değil, kararın **gerekçesini** kalıcı kılmak. Kod "ne yaptığımızı" söyler; ADR "neden başka türlü yapmadığımızı" söyler. Bu bilgi başka hiçbir yerde bulunmaz.

### Temel kavramlar
- **ADR (Architecture Decision Record / Mimari Karar Kaydı):** tek bir kararı belgeleyen, numaralı, kısa markdown dosyası. **Michael Nygard**'ın önerdiği biçim fiilî standarttır.
- **Klasik dört bölüm:** **Durum (Status):** Proposed / Accepted / Superseded / Rejected (önerildi / kabul edildi / yerini yenisi aldı / reddedildi). **Bağlam (Context):** kararı zorunlu kılan durum. **Karar (Decision):** ne yaptık. **Sonuçlar (Consequences):** artılar ve eksiler, ikisi birden.
- **Alternatifler (Considered Options):** elenen seçenekler ve eleme gerekçesi. Nygard'ın orijinalinde yoktur; MADR (Markdown ADR) gibi sonradan çıkan şablonların eklediği bir bölümdür. Bu programda tutulur, çünkü zamanla kaybolan asıl bilgi budur.
- **Yük taşıyan karar (load-bearing decision):** geri alınması pahalı olan ve üstüne başka kararlar binen karar. Yalnız bunlar ADR'yi hak eder.
- **Supersede etmek (yerine yenisini koymak):** eski kararı silmeden "bu artık geçersiz, yerine ADR-00XX geçti" diye işaretlemek.

### Somut örnek
`docs/adr/0004-openrouter-saglayici.md` dosyasını aç ve bölümleri say:
- **Durum:** Accepted (kabul edildi), 2026-07-03.
- **Bağlam:** Faz 2'ye kadar doğrudan Anthropic API hedefleniyordu; kullanıcı tek anahtarla çok sağlayıcı, kullanım paneli ve harcama limiti istedi.
- **Karar:** OpenAI uyumlu OpenRouter API'si kullanılır; `base-url` ve model kimlikleri yapılandırma özelliği olarak tanımlanır; model istemcisi yalnızca `ai/` paketinde kurulur.
- **Alternatifler:** doğrudan Anthropic API. Resmi yol buydu ve satıcıya özgü özelliklerin hepsi kullanılabilirdi; ama kullanıcı OpenRouter istedi. Kritik cümle şurada: *"Geri dönüş kolay: bu ADR'yi supersede et, starter'ı ve base-url'ü değiştir."* Yani ADR yalnız kararı değil, **kararı geri almanın maliyetini** de kaydetmiş.
- **Sonuçlar:** Artı: değişiklik yalnız `ai/` paketine ve config'e dokundu. Eksi: satıcıya özgü gelişmiş özellikler sınırlandı, araya bir sağlayıcı daha girdi.

Bu dosyanın değeri şurada: altı ay sonra biri (insan ya da ajan) `base-url` satırını görüp *"yanlışlıkla kalmış"* diye düzeltmeye kalkarsa, ADR onu durdurur. Fikir gerçekten değişirse de yapılacak iş bellidir. Dosyayı düzenlemezsin; `0010-...` adlı yeni bir ADR yazar, 0004'ün durumunu *"Superseded by ADR-0010"* (yerini yeni ADR aldı) yaparsın.

### Sık yapılan hata
- **Kabul edilmiş ADR'yi güncellemek.** En yaygın hata. Fikir değişti diye Karar bölümünü yeniden yazarsan tarihçe yok olur: dosya artık "biz hep böyle düşünmüştük" der ve düşünce değişiminin sebebi kaybolur. Accepted bir ADR **dondurulmuştur**; üstüne yenisi yazılır.
- **Rejected ile Superseded'ı karıştırmak.** Rejected = hiç kabul edilmedi, öneri elendi. Superseded = kabul edildi, uygulandı, sonradan aşıldı. İki farklı hikâye, iki farklı ders.
- **Her şeye ADR yazmak.** Değişken adı, girinti tercihi, sprint sırası ADR değildir. Gürültüden ibaret otuz ADR'nin arasında üç gerçek karar görünmez olur.
- **Sonuçlar bölümüne yalnız artıları yazmak.** Eksiyi yazmayan ADR bir pazarlama metnidir; onu okuyan ajan da riski göremez, dolayısıyla riski yönetemez.

### Görevlere geçmeden önce
Aşağıdaki iki görev ADR'yi alışkanlığa çevirir. İlki kasıtlı olarak **geriye dönüktür**: zaten verilmiş bir kararı yazmak, biçimi öğrenmenin en ucuz yoludur, çünkü sonucu zaten biliyorsun; tek işin, gerekçeyi unutulmadan kayda geçirmek. İkincisi bağı ters yönde kurar: koddan ADR'ye yorum bırakırsan, kararı okumadan kodu değiştirmek zorlaşır.

## Görevler
- [ ] Kendi projende (ya da bildiğin bir projede) geçmişte alınmış 1 önemli kararı geriye dönük ADR-0001 olarak yaz
- [ ] Koddan ADR'ye yorum bağı ekle (// bkz docs/adr/0001)
