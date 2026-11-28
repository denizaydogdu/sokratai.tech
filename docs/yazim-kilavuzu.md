# Yazım kılavuzu (Türkçe içerik)

Bu kılavuz `content/` altındaki ders, kaynak ve atölye metinleri, arayüz yazıları ve AI öğretmenin prompt'ları
için bağlayıcıdır. Okuyucu İngilizce bilmiyor (AGENTS.md): metin ilk okuyuşta anlaşılmalı, çeviri gibi
kokmamalı. Mülakat içeriği (`content/mulakat/`) bu kılavuzun ilk uygulamasına (Faz 14) dahil değildir.

## 1. Uzun çizgi (—) kullanılmaz

Türkçede uzun çizgi yalnız satır başındaki konuşmaları gösterir. Cümle içinde açıklama, ek düşünce ya da
ayraç olarak kullanmak İngilizceden geçmiş bir alışkanlıktır ve cümleyi bulanıklaştırır. Yerine cümlenin
işlevine uyan işaret ya da yapı gelir:

| Uzun çizginin işlevi | Yerine | Örnek (önce → sonra) |
|---|---|---|
| Açıklama, sıralama, "yani" | iki nokta | "ortam — kurallar, örnekler" → "ortam: kurallar, örnekler" |
| Ek düşünce, sonuç | yeni cümle | "kayıyor — ve bu ortamın adı harness" → "kayıyor. Bu ortamın adı harness." |
| Ara söz (iki çizgi arası) | virgül ya da parantez | "araçlar — test, derleyici — devreye girer" → "araçlar (test, derleyici) devreye girer" |
| Karşıtlık | virgül + ama/fakat | "hızlı — ama kırılgan" → "hızlı ama kırılgan" |
| Gerekçe | çünkü / bu yüzden, ya da noktalı virgül | "sırayı atlama — üst kat çöker" → "sırayı atlama, çünkü üst kat çöker" |
| Başlıkta ayraç | iki nokta | "Yol haritası — 12 hafta" → "Yol haritası: 12 hafta" |
| Sayfa başlığında marka ayracı | orta nokta (·) | "SokratAI — AI Öğretmen" → "SokratAI · AI Öğretmen" |
| Boş tablo hücresi | "yok" ya da "-" | "—" → "yok" |

Boşluklu kısa çizgi (" – ") de aynı muameleyi görür. Sayı aralığındaki bitişik kısa çizgi ("2–3 gün",
"Hafta 1–4") kalabilir.

## 2. Anlam kaybı ve çeviri kokusu

Bir cümleyi okuyup "ne demek istiyor?" diye duraksıyorsan yeniden yaz. Sık görülen sorunlar:

- **İngilizce söz dizimi:** "iki yarımın birlikte çalışması" (how the two halves work together) →
  "guide ile sensor'ün birlikte nasıl çalıştığı". Neyin "iki yarım" olduğu cümlede açıkça söylenmeli.
- **Belirsiz gönderim:** "bu", "o", "bunu" neyi gösteriyor belli değilse ismi yaz.
- **Kelimesi kelimesine deyim:** "günün sonunda" (at the end of the day) → "sonuçta"; "resmin tamamı" →
  "bütün tablo" ya da "genel durum"; "masaya getirmek" → "katmak, sunmak".
- **Hedef ile içeriğin karışması:** "Bugünün tek hedefi şu: iş kayıyor" → "Bugün tek bir şeyi kavrayacaksın:
  iş kayıyor".
- **Uzun, iç içe cümle:** iki fikir varsa iki cümle yaz. Yüklem mümkünse sonda olsun.
- **Gereksiz "olmak/yapmak":** "önemli olan şey ... olmasıdır" → "önemli olan ...".
- **Garip soyut fiiller:** "kavramı senin durumuna indirmek" → "kavramı kendi işine uyarlamak";
  "mekanik olarak korunur" → "test korur".
- **Açıklamasız kültür göndermesi:** "Ralph Wiggum döngüsü" gibi bir ad geçiyorsa bir kez açıkla.
- **Çevrilmemiş İngilizce alıntı:** İngilizce bir alıntı kalıyorsa hemen yanında Türkçesi olmalı.

Anlamı değiştirme: teknik iddia, sayı, kaynak, örnek ve sonuç aynı kalır. Belirsiz bir cümlenin ne demek
istediğinden emin değilsen dokunma, not düş.

## 3. Terimler

İlke: Türk yazılımcının günlük işte kullandığı kelime neyse o.

- Türkçesi yerleşikse Türkçe yaz, dosyada ilk geçtiği yerde İngilizcesini parantezle ver:
  "bağımlılık (dependency)".
- Türkçesi yapay ya da anlaşılmaz bir uydurmaysa İngilizce terimi kullan, ilk geçtiği yerde kısa Türkçe
  açıklama ver: "hash (özet değeri)", "migration (veritabanı şema değişikliği)".
- Parantezi her geçişte tekrarlama: dosyada ilk geçişte bir kez yeter.
- Kod adları, dosya adları, komutlar ve ürün adları çevrilmez.

| İngilizce | Kullanılacak biçim |
|---|---|
| agent | ajan (agent) |
| harness | harness (koşum takımı): İngilizce kalır |
| prompt | prompt (gerekirse ilk geçişte: modele yazılan metin) |
| token | token |
| context / context window | bağlam (context) / bağlam penceresi (context window) |
| trade-off | ödünleşim (trade-off): bir şeyi kazanırken başka bir şeyden vazgeçmek |
| coupling / cohesion | bağlaşım (coupling) / uyum (cohesion) |
| latency | gecikme (latency) |
| retry | yeniden deneme (retry) |
| hash | hash (özet değeri) |
| migration | migration (veritabanı şema değişikliği) |
| drift | sapma (drift) |
| embedding | embedding (anlam vektörü) |
| fine-tuning | ince ayar (fine-tuning) |
| eval / evaluation | eval (değerlendirme testi) / değerlendirme (evaluation) |
| golden set | altın küme (golden set): referans test kümesi |
| guardrail | guardrail (koruma kuralı) |
| prompt injection | prompt enjeksiyonu (prompt injection) |
| sandbox | sandbox (yalıtılmış çalışma ortamı) |
| spec | spec (spesifikasyon) |
| handoff | devir notu (handoff) |
| compaction | sıkıştırma (compaction) |
| progressive disclosure | kademeli açma (progressive disclosure): bilgiyi gerektiği anda vermek |
| observability | gözlemlenebilirlik (observability) |
| allowlist | izin listesi (allowlist) |
| runtime | çalışma zamanı (runtime) |
| cache | önbellek (cache) |
| routing | yönlendirme (routing) |
| transaction | transaction (işlem bütünlüğü) |
| domain (DDD) | domain (iş alanı) |
| framework | framework (çatı) |
| deployment | dağıtım (deployment) |
| rate limit | hız sınırı (rate limit) |
| circuit breaker | devre kesici (circuit breaker) |
| failover | yedeğe geçiş (failover) |
| governance | yönetişim (governance) |
| foundation model | temel model (foundation model) |
| client | istemci (client) |
| streaming | akış (streaming) |

## 4. Dokunulmayacaklar

- Frontmatter anahtarları ve sabit alanlar (`hafta`, `gun`, `slug`, `tarih`, `etiketler`, `url`).
- Kod blokları ve satır içi kod (yalnız kod yorumundaki uzun çizgi değişebilir), bağlantı hedefleri, URL'ler.
- SVG işaretlemesi: yalnız `<text>` içindeki yazı değişebilir, kutuya sığacak uzunlukta kalmalı.
- Yapısal başlıklar: `## Ders`, `## Görevler`, `## Quiz`, `### S1:` önekleri, atölyede `# slug | ... | ai:...`
  ve `## Görev`/`## İpucu`/`## İdeal çözüm`/`## Neden`/`## Kötü örnek`.
- Görevlerin sayısı, sırası ve türü: tür görev cümlesindeki kelimeden çıkar ("oku" → okuma, "yaz/listele/
  hazırla/tablo..." → yazma, "tartış/düşün/karşılaştır/analiz/değerlendir" → tartışma; `app.js`
  `gorevTipiCikar`). İlerleme görevin sırasına göre tutulur.
- Quiz şık sayısı ve doğru şık.
- Tablo satır ve sütun sayısı, liste öğesi sayısı.

## 5. Ton

"Sen" diliyle, kısa ve net. Argo yok; günlük konuşma dili serbest ("test patlar" yerine "test başarısız
olur" daha açık). Vurgu için büyük harf ve kalın yazı ölçülü kullanılır.
