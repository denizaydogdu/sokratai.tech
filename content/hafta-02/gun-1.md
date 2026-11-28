---
hafta: 2
gun: 1
baslik: "AGENTS.md felsefesi: ansiklopedi değil, içindekiler"
---

## Ders

### Bugün ne öğreneceksin?
Bugün tek bir cümleyi kavrayacaksın: **AGENTS.md bir ansiklopedi değil, içindekiler tablosudur.** Nedenini anlarsan "kural dosyasını büyüterek iyileştirme" refleksinden kurtulursun. Bu refleks, harness mühendisliğinin en yaygın ve en sinsi hatasıdır. Bu haftanın konusu, Hafta 1'de okuduğun harness yazısındaki "repo = kayıt sistemi" (bilginin tek doğru kaynağı repodur) bölümünün doğrudan uygulamasıdır.

### Temel kavramlar
- **AGENTS.md:** deponun kökünde duran, ajanın her oturumda okuduğu yönlendirme dosyası. Yaygın bir konvansiyondur; Claude Code aynı içeriği `CLAUDE.md` adıyla arar.
- **Bağlam (context):** ajanın o an önünde duran metnin tamamı: isteğin, okuduğu dosyalar, kurallar, konuşma. Benzetme: bir **valiz**. Büyük olabilir ama sonsuz değildir; kışlık montu koyarsan diş fırçasına yer kalmaz.
- **İşaretçi (pointer):** cevabın kendisi değil, cevabın **adresi**. "Kararlar: docs/adr/" bir işaretçidir.
- **Talimat şişmesi (instruction bloat) → bağlam çürümesi (context rot):** kural dosyasının sürekli büyümesi ve sonunda ajanın uzun metindeki kuralı fiilen görmemesi.

Bağlam kıtlığının **iki** yüzü var; karıştırma:
1. **Fiziksel sınır (yer bitiyor).** Modelin alabildiği metin miktarı sabittir. Dolarsa taşan kısım kırpılır ya da özetlenir. Özetlenirken de hangi ayrıntının önemli olduğu bilinemez.
2. **Dikkat sınırı (yer var ama gözden kaçıyor).** Valiz yarı doluyken bile, 100 satırlık bir metindeki kritik cümleyi bulmak 2000 satırlıktakinden çok daha kolaydır. Gürültü arttıkça sinyal kaybolur. *"Yer var, hepsini koyalım"* savunması bu yüzden yanlıştır.

### Somut örnek
Bu deponun kendi `AGENTS.md`'si: bugün **~100 satır, 5 başlık** (güncel sayıyı `wc -l AGENTS.md` ile ölç; neden ölçtüğümüzü Gün 5'te göreceksin). Başlıklardan üçü haritanın iskeletidir:
- **Komutlar:** `./mvnw verify`, `./mvnw -Dtest=MimariTestleri test`. Ajan "nasıl doğrularım?" sorusunu tek satırda çözer; Maven kılavuzunu okumasına gerek kalmaz.
- **Sınırlar:** *"ALWAYS: types → config → repo → service → ai → web katman yönüne uy"*, *"ASK FIRST: yeni bağımlılık (önce docs/adr/'ye ADR)"*. Kuralın kendisi burada; kuralın **gerekçesi** burada DEĞİL.
- **Nerede ne var:** *"Mimari: ARCHITECTURE.md · Kararlar: docs/adr/ (0001–0021) · Planlar: docs/exec-plans/"*. Bu satırlar birer işaretçidir: içerik değil, yol tarifi verirler.
- **Sonuç:** Ajan "OpenRouter'ı neden seçmişiz?" diye merak ederse cevabı AGENTS.md'de bulamaz ama `docs/adr/0004-openrouter-saglayici.md`'ye gitmesi gerektiğini öğrenir. Bu bir eksiklik değil, **tasarımdır**: 21 ADR'nin tamamı valizde değil, **rafta** durur; ajan sadece gerekeni indirir.

Aynı bilgi ansiklopedi tarzında yazılsaydı, AGENTS.md'nin içinde 21 ADR'nin, mimari tablosunun ve borç listesinin **tam metni** olurdu. Ajan her küçük görevde bu yükü taşır, asıl kodu okuyacak yeri kalmazdı.

### Sık yapılan hata
Her aksilikten sonra AGENTS.md'ye "makul" bir kural daha eklemek. Tek tek hepsi haklıdır; toplamı zehirdir. **Bu depoda gerçekten yaşandı:** `AGENTS.md` tek bir günde, üç ayrı commit'le 29 satırdan 85 satıra çıktı. Yani ilkeyi öğreten metin ile o ilkeyi çiğneyen davranış aynı depoda, aynı gün yan yana duruyordu. Talimat şişmesi kötü niyetten değil, **iyi niyetin birikiminden** doğar ve ancak ölçülürse fark edilir: `wc -l AGENTS.md`.

İkinci hata: ajanın zaten bildiği genel iyi pratikleri yazmak ("okunaklı kod yaz", "önce düşün"). Süzgeç şudur: *bu satır, ajanın **varsayılan** davranışından sapmayı mı anlatıyor?* Sapmıyorsa sil, çünkü ajan onu zaten biliyor. Sapıyorsa kalsın, ama tek satır artı tek işaretçi olsun.

> Ölçüt cümlesi: AGENTS.md'ye eklemek istediğin satır bir **cevap** mı, yoksa **cevabın adresi** mi? Cevaplar docs/'a, adresler AGENTS.md'ye.

### Görevlere geçmeden önce
Aşağıdaki görevler sırasıyla: örneği incele, kendi haritanı yaz (satır sınırı kasıtlıdır: kısıt olmazsa ansiklopedi yazarsın), sonra symlink ile CLAUDE.md'yi aynı dosyaya bağla. Üçüncüsü küçük görünür ama iki dosyanın zamanla birbirinden sapmasını (drift) fiziksel olarak imkânsız kılar.

## Görevler
- [ ] AGENTS.md şablonunu incele: ~100 satır, yalnızca 'içindekiler' (komutlar, sınırlar, nerede-ne-var)
- [ ] Kendi projen için taslak AGENTS.md yaz (<120 satır)
- [ ] ln -s AGENTS.md CLAUDE.md symlink'ini kur
