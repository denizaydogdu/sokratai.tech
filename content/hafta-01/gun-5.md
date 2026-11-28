---
hafta: 1
gun: 5
baslik: "Haftalık sentez + quiz"
---

## Ders

### Bugün ne öğreneceksin?
Bugün iki iş var: haftanın kavramlarını **birbirine bağlamak** ve son kavramı öğrenmek. Son kavram şu: harness kalıcı bir yapı değildir, **sökülebilir (rippable)** olmalıdır. Bu son parça olmadan harness zamanla kendisi bir yük hâline gelir.

### Temel kavramlar
- **Sökülebilirlik (rippable):** harness'ın her parçası, modelin BUGÜNKÜ bir eksiği varsayılarak kurulur. Model güçlenip o eksik kapanınca parça yük taşımayı bırakır ve **çıkarılır**.
- **İskele (scaffolding):** binayı inşa ederken kurulan, bina bitince sökülen geçici yapı. Harness de böyledir: kalıcı mimari değil, işe yaradığı sürece duran destek.
- **Yük taşıyan (load-bearing):** çıkarıldığında bir şeyin çöktüğü parça. Yük taşımayan parça yalnızca maliyettir: bakım, gecikme, bağlam israfı.

### Somut örnek
Önce haftanın zincirine bak. Her gün bir öncekinin üstüne kurulur:
1. **Rol (Gün 1):** insan yönlendirir, ajan uygular. "Harness" fikri buradan doğar.
2. **Mekanizma (Gün 2):** harness = guide (işten önce yönlendirir) + sensor (işten sonra ölçer).
3. **Koşul (Gün 3):** guide ve sensor ancak repoda yazılıysa ajana görünür. Buna okunabilirlik (legibility) denir.
4. **Kalite (Gün 4):** sensor'un güvenilir olması için işi yapan ile yargılayan ayrılır.
5. **Evrim (Gün 5):** harness sabit değil; varsayımı geçersizleşen parça sökülür.

Sökülebilirliğin somut hâli: diyelim model uzun dosyaları güvenilir özetleyemediği için, ajana dosyaları 200 satırlık parçalara bölen bir adım kurdun. Altı ay sonra bağlam penceresi büyüdü, model dosyayı bütün okuyabiliyor. O parçalama adımı artık yük taşımıyor ama hâlâ orada duruyor: her çalıştırmada zaman ve bağlam harcıyor, üstelik bazen dosyayı yanlış yerden bölüp yeni hata üretiyor. **Yeni bir model geldiğinde yapılacak iş budur:** harness'ı parça parça gözden geçir ve her biri için şunu sor: *"bu parça hangi eksiği varsayıyordu, o eksik hâlâ var mı?"* Cevap hayırsa sil.

Tersi de doğru ve önemlidir: sensor'lar (testler, ArchUnit kuralları) genelde sökülmez, çünkü onlar modelin eksiğini değil **senin mimari kararını** korur. Model ne kadar güçlenirse güçlensin, "web katmanı repo'ya doğrudan erişmesin" kuralı senin tercihindir. Sökülen tipik olarak guide tarafındaki koltuk değnekleridir: fazladan hatırlatmalar, artık gereksiz ara adımlar, modelin çoktan öğrendiği el kitapları.

### Sık yapılan hata
- **Harness'ı bir kez kurup bir daha dokunmamak.** Varsayımlar sessizce eskir: hiçbir yerde hata çıkmaz, hiçbir test kırmızı yanmaz; sistem sadece gereğinden yavaş ve pahalı çalışır. Sessiz bozulma, gürültülü bozulmadan daha tehlikelidir.
- **Her yeni modelde harness'ı sıfırdan yazmak.** Bu da öbür uç ve aynı derecede yanlış. Doğrusu parça parça gözden geçirmedir.
- **"Sökülebilir"i "modüler" sanmak.** Modülerlik parçaların birbirine nasıl bağlandığıyla ilgilidir; sökülebilirlik parçanın **hâlâ gerekli olup olmadığıyla**. Mükemmel modüler ama tamamen gereksiz bir harness parçası, yine de silinmelidir.

### Görevlere geçmeden önce
Yeni rolün üçüncü direği olan **niyet/spec yazma** bu hafta yalnız tanıtıldı; Hafta 3'te (Spec-Driven Akış) derinleşecek. Aşağıdaki iki görevden ilkinde yukarıdaki beş halkayı kendi cümlelerinle anlatacaksın; anlamadığın halka tam orada ortaya çıkar. İkinci görev, öğrendiklerini quiz ile ölçer.

## Görevler
- [ ] 1 sayfalık 'Harness 101' (harness'a giriş) özetini kendi kelimelerinle yaz (yukarıdaki 5 halkayı kendi cümlelerinle birbirine bağla)
- [ ] Haftalık quiz'i çöz (aşağıda)
