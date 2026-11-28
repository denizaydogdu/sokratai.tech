---
baslik: "Tehdit Modelleme ve STRIDE: AI ile Geliştirirken Ne Ters Gidebilir?"
yazar: "AI Öğretmen ek okuma metni: tehdit modelleme (STRIDE) konusu"
tarih: "2026-11-28"
dil: "Türkçe kavram özeti (diyagramlı)"
etiketler: 4,7
---

### Bu metin ne anlatıyor?
Bir sistemi kurmadan önce kendine "burada ne ters gidebilir?" diye sorup cevabı yazıya dökmeye
**tehdit modelleme** (threat modeling) denir. Bu metin, tehdit modellemeyi alışkanlığa çeviren dört soruyu,
sistemi çizmenin en kullanışlı yolu olan **veri akış diyagramını** ve tehdit ararken hiçbir türü
atlamamak için kullanılan altı harfli kontrol listesi **STRIDE**'ı anlatıyor. Sonra hepsini bu
platformun kendisi, SokratAI'ın girişsiz site modu üzerinde baştan sona uyguluyoruz: gerçek önlemler,
gerçek dosya yolları ve hâlâ açık duran riskler. Son bölümler iki soruyu cevaplıyor: tehdit modeli bir
spec'e (spesifikasyon) nasıl girer ve bu işte bir AI ajanından (agent) ne kadar yardım alabilirsin?

## Neden bu özet
Hafta 4 guardrail'ları (koruma kurallarını) ve güvenliği, Hafta 7 AI engineering'in temellerini
anlatıyor. İkisinde de güvenlik çoğunlukla kod yazıldıktan **sonra** çalışan araçlar üzerinden görünüyor:
ArchUnit, statik analiz, bağımlılık taraması, prompt enjeksiyonu (prompt injection) savunmaları. Eksik
halka, kod yazılmadan **önce** yapılan iştir: neyi koruduğunu, kime karşı koruduğunu ve hangi riski
bilerek taşıdığını yazmak. Programdaki mevcut anlatım yalnız LLM'e özgü riskleri kapsıyordu
([`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md)). Bu metin genel yöntemi verir ve LLM
risklerini o yönteme bağlar.

AI çağında bunun ayrıca önemli olmasının nedeni basit: ajan kodu senden hızlı yazar, ama neyin tehlikeli
olduğunu ancak senin söylediğin kadar bilir. Ajana verdiğin spec'teki "şunu yapma, şunu mutlaka test et"
satırlarının kaynağı tehdit modelidir. Mimar olarak senin işin, satır yazmaktan çok bu soruları sormak ve
kararları kayda geçirmektir.

Örnek olarak bu platformun kendisini kullanıyoruz, çünkü kodu önünde ve her iddiayı dosya yoluyla
doğrulayabilirsin. Metindeki SokratAI iddialarının hepsi 2026-11-28 tarihli depo durumuna göre kontrol
edildi. Bir önlem yoksa "yok" diye yazıldı.

## Türkçe çalışma metni

### 1. Tehdit modelleme nedir ve ne zaman yapılır?
Tehdit Modelleme Manifestosu (Threat Modeling Manifesto: Kasım 2020'de 15 kişilik bir çalışma grubunun
yayımladığı kısa metin) tanımı tek cümleyle verir: tehdit modelleme, bir sistemin **temsillerini**
(diyagram, tarif, kod) analiz ederek güvenlik ve gizlilik açısından kaygı verici noktaları öne
çıkarmaktır. Tanımdaki kilit kelime "temsil"dir. Tehdit modelini çalışan sistem üzerinde değil, onun bir
çizimi ya da tarifi üzerinde yaparsın. Bu yüzden kod daha yokken de yapılabilir.

Yöntemin iskeleti Adam Shostack'ın dört sorusudur (Four Question Framework):

1. **Ne üzerinde çalışıyoruz?** Sistemin bir modeli: bileşenler, veri akışları, güven sınırları.
2. **Ne ters gidebilir?** Tehditler. STRIDE burada devreye girer.
3. **Bu konuda ne yapacağız?** Her tehdit için bir karar: azalt, ortadan kaldır, devret ya da kabul et.
4. **İyi bir iş çıkardık mı?** Model gerçeği yansıtıyor mu, önlemler gerçekten var mı, test ediliyor mu?

Manifesto dördüncü soruyu "**yeterince** iyi bir iş çıkardık mı?" diye yazar. Küçük ama önemli bir fark:
hedef kusursuzluk değil, karar vermeye yetecek doğruluktur.

**Ne zaman?** OWASP'ın Tehdit Modelleme Kopya Kâğıdı (Threat Modeling Cheat Sheet) iki şey söyler: ideal
olarak yazılım yaşam döngüsünün başında, tasarım aşamasında yapılır; sonra sistemle birlikte bakımı
yapılır ve güncellenir. Manifesto da "erken ve sık analizi" ilke olarak koyar. Pratikte bu iki kurala
iner:
- **Spec ile birlikte, koddan önce.** Yeni bir özelliğin tarifini yazarken dört soruyu sorarsın; çıkan
  kararlar spec'e girer (§6).
- **Modeli değiştiren her değişiklikte yeniden.** Her commit değil, modeli değiştiren değişiklik: yeni bir
  uç, yeni bir veri türü, yeni bir dış servis, yeni bir güven sınırı, yeni bir AI aracı ya da hafıza türü.
  SokratAI'da girişli siteden girişsiz siteye geçiş (ADR-0017) ve systemd'den Docker'a geçiş (ADR-0018)
  tam böyle anlardı. İki ADR'nin "Bağlam" bölümünde "Başarısızlık ölçütleri" paragrafı var; ADR-0018'in
  "Güvenlik notu"nda "Kalan risk", ADR-0017'de "bilinçli kabuller" listesi bulunur. Bu parçalar küçük birer
  tehdit modelidir.

Manifesto beş değer sayar: tasarım sorunlarını bulup düzelten bir kültür, insanlar ve iş birliği, bir
anlama yolculuğu, tehdit modellemeyi **yapmak** (üzerine konuşmak değil) ve sürekli iyileştirme. Ayrıca
dört karşı desen (anti-pattern) adlandırır; dördü de AI'lı ekiplerde tanıdık gelecek:
- **Kahraman tehdit modelcisi (Hero Threat Modeler):** işi tek bir uzmana bırakmak. Bugün bu kahraman
  bazen bir araçtır.
- **Problemi seyretmek (Admiration for the Problem):** tehditleri listeleyip karara bağlamamak.
- **Aşırı odaklanma (Tendency to Overfocus):** tek bir tehdit türüne gömülüp gerisini kaçırmak. Bugün
  çoğu ekipte bu tür prompt enjeksiyonudur.
- **Kusursuz temsil (Perfect Representation):** "önce mükemmel diyagram" diye beklemek. Birkaç kaba
  temsil, tek bir kusursuz temsilden daha çok şey gösterir.

## Diyagram 1: Dört soru ve döngü
Her soru bir çıktı üretir ve dördüncü soru seni birinciye geri götürür. Alttaki şerit döngünün ne zaman
yeniden çalıştığını söyler.

```svg
<svg viewBox="0 0 640 360" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Tehdit modellemenin dört sorusu bir döngü oluşturur: ne üzerinde çalışıyoruz, ne ters gidebilir, ne yapacağız, iyi bir iş çıkardık mı; döngü spec ile başlar ve modeli değiştiren her değişiklikte yeniden çalışır">
  <defs>
    <marker id="tm1-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#1C3247"/></marker>
  </defs>
  <text x="320" y="26" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Tehdit modelleme: dört soru, sürekli döngü</text>
  <text x="320" y="46" text-anchor="middle" font-size="12" fill="#5D7789" font-family="system-ui">Shostack'ın dört sorusu · her cevap bir çıktı üretir</text>
  <rect x="40" y="66" width="250" height="78" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="165" y="94" text-anchor="middle" font-size="12.5" font-weight="700" fill="#186399" font-family="system-ui">1 · Ne üzerinde çalışıyoruz?</text>
  <text x="165" y="114" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">DFD · güven sınırları · kapsam</text>
  <text x="165" y="130" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">olanı çiz, olmasını istediğini değil</text>
  <rect x="350" y="66" width="250" height="78" rx="10" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.5"/>
  <text x="475" y="94" text-anchor="middle" font-size="12.5" font-weight="700" fill="#BE4433" font-family="system-ui">2 · Ne ters gidebilir?</text>
  <text x="475" y="114" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">STRIDE · LLM tehdit eşlemesi</text>
  <text x="475" y="130" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">her öğe ve her sınır için sor</text>
  <rect x="350" y="196" width="250" height="78" rx="10" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="475" y="224" text-anchor="middle" font-size="12.5" font-weight="700" fill="#C77A17" font-family="system-ui">3 · Bu konuda ne yapacağız?</text>
  <text x="475" y="244" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">azalt · kaldır · devret · kabul et</text>
  <text x="475" y="260" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">kabul de yazılı bir karardır</text>
  <rect x="40" y="196" width="250" height="78" rx="10" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="165" y="224" text-anchor="middle" font-size="12.5" font-weight="700" fill="#25804C" font-family="system-ui">4 · İyi bir iş çıkardık mı?</text>
  <text x="165" y="244" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">test · bağımsız inceleme</text>
  <text x="165" y="260" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">model hâlâ kodla aynı mı?</text>
  <line x1="290" y1="105" x2="344" y2="105" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm1-ok)"/>
  <line x1="475" y1="144" x2="475" y2="190" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm1-ok)"/>
  <line x1="350" y1="235" x2="296" y2="235" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm1-ok)"/>
  <line x1="165" y1="196" x2="165" y2="150" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm1-ok)"/>
  <text x="320" y="174" text-anchor="middle" font-size="11" font-weight="600" fill="#5D7789" font-family="system-ui">döngü</text>
  <rect x="40" y="292" width="560" height="54" rx="10" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.2" stroke-dasharray="5 4"/>
  <text x="320" y="314" text-anchor="middle" font-size="11.5" font-weight="600" fill="#1C3247" font-family="system-ui">Ne zaman? Spec ile birlikte, koddan önce. Sonra yeniden:</text>
  <text x="320" y="333" text-anchor="middle" font-size="10.5" fill="#5D7789" font-family="system-ui">yeni uç · yeni veri · yeni dış servis · yeni güven sınırı · yeni AI aracı ya da hafıza</text>
</svg>
```

### 2. Veri akış diyagramı (DFD) ve güven sınırları
Birinci soruyu cevaplamanın en yaygın yolu **veri akış diyagramıdır** (data flow diagram, DFD).
Microsoft'un Tehdit Modelleme Aracı (Threat Modeling Tool) ve OWASP Threat Dragon gibi araçlar da bu
dili kullanır. Beş öğe vardır:
- **Dış varlık (external entity):** senin kontrol etmediğin şey: kullanıcı, tarayıcı, üçüncü taraf
  servis. Düz dikdörtgenle çizilir.
- **Süreç (process):** veriyi işleyen kod: uygulaman, bir vekil (proxy), bir arka plan işi. Daire ya da
  yuvarlak köşeli kutu.
- **Veri deposu (data store):** verinin durduğu yer: veritabanı, dosya, önbellek (cache), log. İki
  paralel çizgi.
- **Veri akışı (data flow):** bir öğeden ötekine giden veri. Ok.
- **Güven sınırı (trust boundary):** iki tarafın güven düzeyinin değiştiği çizgi. Kesikli çizgi.

En önemli öğe güven sınırıdır, çünkü tehditler sınırı geçen akışlarda yoğunlaşır. Sınır genellikle
sahipliğin ya da yetkinin değiştiği yerdedir: internetten sunucuya, bir konteynerden ötekine, senin
kodundan üçüncü taraf bir servise. [`temel-secure-coding.md`](temel-secure-coding.md) güven sınırını
güvenmediğin bölge ile güvendiğin çekirdek arasındaki çizgi olarak tanıtmış ve doğrulamanın tam bu sınırın
üstünde yapıldığını söylemişti. DFD o çizgiyi sistemin tamamında görünür kılar.

İki pratik kural:
- **Olanı çiz, olmasını istediğini değil.** Bir belge "konteynere yalnız vekil ulaşır" diyebilir. Ama
  aynı ağdaki başka konteynerler de o porta ulaşabiliyorsa, DFD'de o komşu da görünmelidir.
- **Karar verilen düzeyde çiz.** C4 diyagramı ([`hafta06-mimari-stiller-c4.md`](hafta06-mimari-stiller-c4.md))
  yapıyı anlatır; DFD ise verinin nereden nereye aktığını ve hangi sınırı geçtiğini. Elindeki konteyner
  diyagramını DFD'ye çevirmek iyi bir başlangıçtır: her ok için "bu ok hangi sınırı geçiyor, üstünde ne
  taşınıyor?" diye sor.

### 3. STRIDE: altı tehdit, altı özellik
"Ne ters gidebilir?" açık uçlu bir sorudur ve insan açık uçlu soruda aklına ilk geleni söyler. STRIDE bu
yüzden vardır: altı kategoriyi tek tek sorarak aramayı sistemli hâle getirir. Loren Kohnfelder ve Praerit
Garg, 1 Nisan 1999'da Microsoft'un iç dergisinde yayımlanan "The Threats to Our Products" (ürünlerimize
yönelik tehditler) yazısında tanıttı. Asıl metinde R harfi "repudiability" (inkâr edilebilirlik) diye
geçer; bugün "repudiation" denir.

Her kategori, korumak istediğin bir güvenlik özelliğinin çiğnenmesidir. Aşağıdaki eşleme Microsoft Learn'ün
tehdit modelleme eğitimindeki tablodan alındı:

| Harf | Tehdit | Çiğnenen özellik | Sorulacak soru | Tipik önlem |
|---|---|---|---|---|
| S | Kimlik taklidi (spoofing) | Kimlik doğrulama (authentication) | Biri kendini başkası gibi gösterebilir mi? | Güçlü kimlik doğrulama, tahmin edilemez oturum kimliği, imzalı token |
| T | Kurcalama (tampering) | Bütünlük (integrity) | Veri ya da kod yolda veya depoda değiştirilebilir mi? | TLS, imza ya da özet, girdi doğrulama, CSRF koruması |
| R | İnkâr (repudiation) | İnkâr edilemezlik (non-repudiation) | Biri "bunu ben yapmadım" diyebilir mi, aksini kanıtlayabilir misin? | Denetim kaydı (audit log), zaman damgası, kaydın korunması |
| I | Bilgi ifşası (information disclosure) | Gizlilik (confidentiality) | Görmemesi gereken biri veriyi görebilir mi? | Şifreleme, erişim kontrolü, iç ayrıntı vermeyen hata mesajı, log maskeleme |
| D | Hizmet engelleme (denial of service) | Erişilebilirlik (availability) | Sistem yavaşlatılabilir ya da durdurulabilir mi? | Hız sınırı (rate limit), girdi boyutu sınırı, zaman aşımı, kota |
| E | Yetki yükseltme (elevation of privilege) | Yetkilendirme (authorization) | Biri yetkisi olmayan bir işi yapabilir mi? | En az yetki, sunucuda yetki kontrolü, kapalı varsayılanlar |

Bazı kaynaklar S için "özgünlük" (authenticity) der. İkisi aynı soruya bakar: karşındaki gerçekten iddia
ettiği kişi mi?

**STRIDE'ı DFD öğelerine uygulamak.** Shostack'ın kitabı (*Threat Modeling: Designing for Security*,
Wiley, 2014) STRIDE'ı öğe başına uygulamayı (STRIDE-per-element) önerir. Kaba rehber şöyledir: dış
varlıkta S ve R; süreçte altısı da; veri akışında T, I ve D; veri deposunda T, I ve D, depo bir log ise R
de. Bu rehber bir başlangıçtır; kaçırılan bir tehdit, rehberin "burada aranmaz" dediği bir hücrede de
olabilir.

**Üçüncü soru: dört karar.** OWASP kopya kâğıdı her tehdit için dört cevap tanır:
- **Azalt (mitigate):** gerçekleşme olasılığını ya da etkisini düşür.
- **Ortadan kaldır (eliminate):** özelliği ya da bileşeni kaldır.
- **Devret (transfer):** sorumluluğu sözleşmeyle ya da mali olarak başka bir tarafa ver.
- **Kabul et (accept):** hiçbirini yapma, riski bilerek taşı.

"Kabul et" tembellik değil, bir karardır; ama ancak **yazılırsa** karardır. Yazılmayan kabul, unutulmuş
risktir.

### 4. Çalışılmış örnek: SokratAI site modu

#### 4a. Ne üzerinde çalışıyoruz?
**Kapsam:** `site` profiliyle çalışan, herkese açık, girişsiz site (ADR-0017, ADR-0018). **Kapsam
dışı:** yerel mod (tek kullanıcı, yalnız `127.0.0.1`), MCP profilleri, sunucunun işletim sistemi ve
Dokploy panelinin güvenliği, OpenRouter'ın kendi iç güvenliği.

Sistemin özeti (kodla doğrulandı):
- **Giriş yok.** Ziyaretçi `aio_ziyaretci` çereziyle tanınır: 128 bitlik rastgele kimlik
  (`SecureRandom`), `HttpOnly`, `SameSite=Lax`, `Path=/`, 365 gün ömür; `Secure` bayrağı
  `APP_SITE_GUVENLI_CEREZ` açıksa her zaman, değilse istek HTTPS ile geldiyse konur
  (`config/ZiyaretciCerezi`). Oturum yoktur
  (`SessionCreationPolicy.STATELESS`, `config/SiteGuvenlikYapilandirma`).
- **İlerleme kodu** aynı kimliğin kendisidir. `GET /api/ziyaretci/kod` kodu gösterir,
  `POST /api/ziyaretci/geri-yukle` ilerlemeyi başka bir tarayıcıya taşır (`web/ZiyaretciController`).
  Sınıfın yorumu açıkça yazar: "Kod bir parola gibidir."
- **AI yalnız sohbette ve ziyaretçinin kendi anahtarıyla.** Anahtar tarayıcıda durur ve yalnız
  `POST /api/sohbet` isteğine `X-OpenRouter-Anahtar` başlığıyla eklenir (`static/app.js`). Sunucu biçimi
  denetler: anahtar `sk-or-` ile başlamalı, ardından 10–200 URL-güvenli karakter gelmelidir
  (`config/BaslikAiAnahtarBaglami`). Sohbet dışındaki AI uçları sitede hiç kayıtlı değildir:
  `web/OgretmenAiOzellikController`, `web/AtolyeAiController` ve `web/KitapController` sınıflarında
  `@Profile("!site")` var.
- **"OpenRouter ile bağlan"** (Faz 13): tarayıcı OpenRouter'dan PKCE ile anahtar alır. PKCE (Proof Key
  for Code Exchange), OAuth (şifreni vermeden bir uygulamaya yetki verme protokolü) akışına eklenen ve
  çalınan bir yetki kodunu tek başına işe yaramaz kılan bir adımdır. Doğrulayıcı ve `state` değeri tarayıcının `sessionStorage`'ında durur, anahtar değişimi tarayıcıdan
  doğrudan `openrouter.ai`'a yapılır, `state` eşleşmezse anahtar alınmaz (`static/app.js`). Sunucumuz
  bu akışı hiç görmez.
- **Dağıtım (deployment):** Dokploy üzerinde Docker konteyneri. Önde Traefik TLS'i (HTTPS şifrelemesini)
  sonlandırır. Uygulama konteyner içinde `0.0.0.0:8090`'a bağlanır, host portu yayınlanmaz. Konteyner root olmayan
  kullanıcıyla (uid 10001) çalışır ve tek yazılabilir dizini kalıcı volume `/app/data`'dır
  (`Dockerfile`, ADR-0018, `docs/kurulum-site.md` §10).

## Diyagram 2: SokratAI site modunun DFD'si
Kesikli kırmızı çizgiler güven sınırlarıdır (GS). Traefik'in altındaki gri kesikli kutu, ADR-0018'in hesaba
kattığı tehdit kaynağıdır: aynı Docker ağındaki başka bir projenin konteyneri.

```svg
<svg viewBox="0 0 640 528" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="SokratAI site modunun veri akış diyagramı: ziyaretçi tarayıcısı HTTPS ile Traefik'e, Traefik Docker ağı üzerinden Spring uygulamasına, uygulama PostgreSQL'e, hafıza volume'üne ve ziyaretçinin anahtarıyla OpenRouter'a bağlanır; tarayıcı OpenRouter'dan PKCE ile anahtar alır; üç güven sınırı ve komşu konteyner tehdidi işaretli">
  <defs>
    <marker id="tm2-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#1C3247"/></marker>
    <marker id="tm2-bas" markerWidth="10" markerHeight="10" refX="0" refY="3" orient="auto"><path d="M7,0 L0,3 L7,6 Z" fill="#1C3247"/></marker>
    <marker id="tm2-gri" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#5D7789"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">SokratAI site modu: veri akış diyagramı (DFD)</text>
  <text x="320" y="44" text-anchor="middle" font-size="11.5" fill="#5D7789" font-family="system-ui">Kesikli kırmızı çizgi = güven sınırı · numaralı ok = veri akışı</text>
  <rect x="376" y="58" width="200" height="74" rx="6" fill="none" stroke="#BE4433" stroke-width="1.4" stroke-dasharray="6 4"/>
  <text x="368" y="74" text-anchor="end" font-size="10.5" font-weight="600" fill="#BE4433" font-family="system-ui">GS3 · üçüncü taraf</text>
  <rect x="388" y="68" width="176" height="54" rx="2" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="476" y="90" text-anchor="middle" font-size="13" font-weight="700" fill="#1C3247" font-family="system-ui">OpenRouter</text>
  <text x="476" y="108" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">dış servis · model sağlayıcı</text>
  <rect x="16" y="214" width="132" height="92" rx="2" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="82" y="240" text-anchor="middle" font-size="13" font-weight="700" fill="#1C3247" font-family="system-ui">Ziyaretçi</text>
  <text x="82" y="256" text-anchor="middle" font-size="13" font-weight="700" fill="#1C3247" font-family="system-ui">tarayıcısı</text>
  <text x="82" y="276" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">çerez · XSRF token</text>
  <text x="82" y="292" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">anahtar tarayıcıda</text>
  <line x1="170" y1="170" x2="170" y2="412" stroke="#BE4433" stroke-width="1.4" stroke-dasharray="6 4"/>
  <text x="170" y="162" text-anchor="middle" font-size="10.5" font-weight="600" fill="#BE4433" font-family="system-ui">GS1 · TLS</text>
  <rect x="190" y="222" width="120" height="76" rx="38" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="250" y="252" text-anchor="middle" font-size="13" font-weight="700" fill="#1C3247" font-family="system-ui">Traefik</text>
  <text x="250" y="270" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">TLS sonlandırır</text>
  <text x="250" y="285" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">vekil (proxy)</text>
  <line x1="338" y1="170" x2="338" y2="412" stroke="#BE4433" stroke-width="1.4" stroke-dasharray="6 4"/>
  <text x="338" y="162" text-anchor="middle" font-size="10.5" font-weight="600" fill="#BE4433" font-family="system-ui">GS2 · Docker ağı</text>
  <rect x="362" y="214" width="170" height="92" rx="24" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="447" y="240" text-anchor="middle" font-size="13" font-weight="700" fill="#1C3247" font-family="system-ui">Spring uygulaması</text>
  <text x="447" y="258" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">site profili · konteyner</text>
  <text x="447" y="274" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">girişsiz · oturumsuz</text>
  <text x="447" y="290" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">CSRF · hız sınırı</text>
  <line x1="352" y1="350" x2="470" y2="350" stroke="#25804C" stroke-width="1.6"/>
  <line x1="352" y1="406" x2="470" y2="406" stroke="#25804C" stroke-width="1.6"/>
  <text x="411" y="368" text-anchor="middle" font-size="12" font-weight="700" fill="#25804C" font-family="system-ui">PostgreSQL</text>
  <text x="411" y="384" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">ilerleme · sohbet</text>
  <text x="411" y="398" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">ai_kullanim</text>
  <line x1="488" y1="350" x2="624" y2="350" stroke="#25804C" stroke-width="1.6"/>
  <line x1="488" y1="406" x2="624" y2="406" stroke="#25804C" stroke-width="1.6"/>
  <text x="556" y="368" text-anchor="middle" font-size="12" font-weight="700" fill="#25804C" font-family="system-ui">/app/data</text>
  <text x="556" y="384" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">ziyaretçi hafızası</text>
  <text x="556" y="398" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">kalıcı volume</text>
  <rect x="190" y="344" width="124" height="48" rx="6" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.2" stroke-dasharray="4 3"/>
  <text x="252" y="364" text-anchor="middle" font-size="10.5" font-weight="600" fill="#5D7789" font-family="system-ui">komşu konteyner</text>
  <text x="252" y="380" text-anchor="middle" font-size="9.5" fill="#5D7789" font-family="system-ui">başka proje, aynı ağ</text>
  <line x1="314" y1="356" x2="372" y2="308" stroke="#5D7789" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#tm2-gri)"/>
  <line x1="148" y1="252" x2="186" y2="252" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm2-ok)"/>
  <line x1="310" y1="260" x2="358" y2="260" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm2-ok)"/>
  <line x1="411" y1="306" x2="411" y2="346" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm2-ok)"/>
  <line x1="500" y1="306" x2="540" y2="346" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm2-ok)"/>
  <line x1="476" y1="214" x2="476" y2="126" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm2-ok)"/>
  <path d="M82,210 L82,95 L382,95" fill="none" stroke="#1C3247" stroke-width="1.8" marker-start="url(#tm2-bas)" marker-end="url(#tm2-ok)"/>
  <circle cx="158" cy="236" r="8" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.2"/>
  <text x="158" y="240" text-anchor="middle" font-size="10" font-weight="700" fill="#1C3247" font-family="system-ui">1</text>
  <circle cx="324" cy="244" r="8" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.2"/>
  <text x="324" y="248" text-anchor="middle" font-size="10" font-weight="700" fill="#1C3247" font-family="system-ui">2</text>
  <circle cx="398" cy="326" r="8" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.2"/>
  <text x="398" y="330" text-anchor="middle" font-size="10" font-weight="700" fill="#1C3247" font-family="system-ui">3</text>
  <circle cx="534" cy="320" r="8" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.2"/>
  <text x="534" y="324" text-anchor="middle" font-size="10" font-weight="700" fill="#1C3247" font-family="system-ui">4</text>
  <circle cx="476" cy="172" r="8" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.2"/>
  <text x="476" y="176" text-anchor="middle" font-size="10" font-weight="700" fill="#1C3247" font-family="system-ui">5</text>
  <circle cx="230" cy="95" r="8" fill="#FFFFFF" stroke="#1C3247" stroke-width="1.2"/>
  <text x="230" y="99" text-anchor="middle" font-size="10" font-weight="700" fill="#1C3247" font-family="system-ui">6</text>
  <text x="16" y="440" text-anchor="start" font-size="10" fill="#1C3247" font-family="system-ui">1  Tarayıcı → Traefik: HTTPS; ziyaretçi ve XSRF çerezleri; anahtar başlığı yalnız sohbet isteğinde</text>
  <text x="16" y="455" text-anchor="start" font-size="10" fill="#1C3247" font-family="system-ui">2  Traefik → uygulama: düz HTTP + X-Forwarded-For; uygulama yalnız güvenilir vekile inanır</text>
  <text x="16" y="470" text-anchor="start" font-size="10" fill="#1C3247" font-family="system-ui">3  Uygulama → PostgreSQL: ziyaretçi, ilerleme, sohbet geçmişi, ai_kullanim satırları</text>
  <text x="16" y="485" text-anchor="start" font-size="10" fill="#1C3247" font-family="system-ui">4  Uygulama → /app/data: ziyaretçi başına hafıza dosyası (her 4 mesajda bir yeniden yazılır)</text>
  <text x="16" y="500" text-anchor="start" font-size="10" fill="#1C3247" font-family="system-ui">5  Uygulama → OpenRouter: prompt + ziyaretçinin anahtarı (yalnız istek ömrü boyunca bellekte)</text>
  <text x="16" y="515" text-anchor="start" font-size="10" fill="#1C3247" font-family="system-ui">6  Tarayıcı ↔ OpenRouter: PKCE ile anahtar alma; sunucumuz bu akışı hiç görmez</text>
</svg>
```

#### 4b. Ne ters gidebilir, ne yaptık? STRIDE ile tek tek
Her kategori için üç satır var: somut tehdit, bugün kodda olan önlem ve kalan risk.

**S · Kimlik taklidi**
- *Tehdit:* Başkasının `aio_ziyaretci` kimliğini ele geçirip ilerlemesini ve sohbet geçmişini okumak;
  kurbana saldırganın bildiği bir kimliği yerleştirmek (kimlik sabitleme, session fixation); ilerleme
  kodunu tahmin etmek.
- *Önlem:* Kimlik 128 bit rastgeledir, tahmin edilemez. Biçimi geçersiz çerez yok sayılıp yenisi
  üretilir; çerezle gelen keyfi metin `kullanici_id`'ye ya da dosya yoluna giremez (`ZiyaretciCerezi`).
  Aynı adla farklı değerli birden çok çerez gelirse hiçbirine güvenilmez (ADR-0018 "Güvenlik notu", test
  `SiteZiyaretciCokluCerezTest`). Geri yükleme IP başına saatte 10 denemeyle sınırlıdır ve yalnız kayıtlı
  kodu kabul eder (`web/HizSiniriInterceptor`, `ZiyaretciController`). Hiçbir uç dışarıdan `kullanici_id`
  almaz (ADR-0017).
- *Kalan risk:* (1) Çerez `__Host-` önekli değil. Alan adı altındaki bir alt alan adını kontrol eden biri,
  ziyaretçinin ilk gelişinde tek bir çerez yerleştirip kimliği sabitleyebilir. Karar yazılı: tech-debt
  #19, işletme kuralı "güvenilmeyen alt alan adı açma", tetik koşulu "alan adına başka proje bağlanırsa
  `__Host-` önekine geç". (2) İlerleme kodu bir taşıyıcı sırdır (bearer secret): kodu bilen herkes o
  ziyaretçidir. Kimlik `HttpOnly` çerezde durur, ama `GET /api/ziyaretci/kod` onu aynı kökten çalışan
  JavaScript'e bilerek verir. Yani sayfaya sızan bir XSS (cross-site scripting: saldırganın betiğinin
  kurbanın tarayıcısında çalışması), `HttpOnly`'ye rağmen kimliği okuyabilir. Bu durumda asıl duvar
  CSP (Content Security Policy: tarayıcının hangi kaynaktan betik ve içerik yükleyeceğini belirleyen
  kural) ve güvenilmeyen metnin `textContent` ile basılmasıdır (T maddesi).

**T · Kurcalama**
- *Tehdit:* Başka bir sitenin, kurbanın tarayıcısı üzerinden ziyaretçi adına durum değiştiren istek
  göndermesi (CSRF, cross-site request forgery); sayfaya betik sokulması; verinin yolda değiştirilmesi.
- *Önlem:* CSRF'e karşı iki katman var. Birincisi Spring Security'nin çerez tabanlı token'ı:
  `XSRF-TOKEN` çerezindeki değer `X-XSRF-TOKEN` başlığıyla geri gelmelidir
  (`SiteGuvenlikYapilandirma`). İkincisi `web/CsrfKoruma`: `Sec-Fetch-Site: cross-site` ya da izin
  listesinde (allowlist) olmayan bir `Origin` gelirse API uçları 403 döner; izinli kökler
  `APP_SITE_IZINLI_ORIGINLER`'den okunur. `SameSite=Lax` çerezler üçüncü bir engeldir. XSS'e karşı CSP,
  `src/main/resources/static/index.html` içinde bir `<meta>` etiketiyle verilir: `default-src 'self'`,
  satır içi betik yok, `connect-src` kendi kök, `openrouter.ai` ve GA alanları. Bu politika sabittir: GA
  alanları ve `script-src` içindeki `www.googletagmanager.com` ölçüm kimliği verilmese de listededir; yalnız
  GA betiğinin yüklenmesi kimliğe bağlıdır. JS'siz ders sayfalarında `web/AcikSayfa`, ölçüm kimliği yoksa
  `script-src 'none'` verir. İnternet tarafında
  yoldaki kurcalamaya karşı TLS vardır (Traefik ve Let's Encrypt).
- *Kalan risk:* CSP bir HTTP başlığı değil, `<meta>` etiketidir. Bu yolla `frame-ancestors` gibi bazı
  yönergeler verilemez; çerçeveye gömmeye karşı koruma başka bir başlığa kalır (`deploy/nginx-site.conf.ornek`
  içindeki yorum, `X-Frame-Options` başlığını Spring Security'nin yazdığını not eder). Traefik'ten
  uygulamaya giden trafik Docker ağı içinde düz HTTP'dir (`docs/kurulum-site.md` §10); veritabanı
  bağlantısı için de depoda bir TLS ayarı yoktur. Yani bu ağ güvenilir sayılır (GS2, aşağıda §4c).

**R · İnkâr**
- *Tehdit:* "Bu harcamayı ben yapmadım", "bu mesajı ben göndermedim"; kötüye kullanımın sonradan
  izlenememesi.
- *Önlem:* Her AI çağrısı `ai_kullanim` tablosuna bir satır yazar: ziyaretçi kimliği (`z-` önekli),
  işlem, istenen ve gerçekte cevap veren model, token sayıları, süre ve tarih
  (`ai/SpringAiOgretmenClient`, `types/AiKullanim`). Satır, boş ya da süzülmüş cevap hatasından **önce**
  yazılır, çünkü o cevabın da ücreti ödenmiştir. Beklenmeyen bir 500 hatasında istemciye (client) kısa bir
  referans döner ve aynı referans log satırında da yer alır (`web/HataYonetimi`). Böylece bir şikâyet,
  hiçbir iç ayrıntı sızdırmadan log kaydıyla eşleştirilebilir.
- *Kalan risk:* Girişsiz bir sitede inkâr edilemezlik baştan zayıftır: kimlik bir kişiye değil bir
  çereze bağlıdır ve kimliği bilen herkes o ziyaretçi gibi istek atabilir. `ai_kullanim` satırları 180
  günlük temizlikle silinir (ADR-0017 §8). "AI çağrısı" log satırı ziyaretçi kimliğini taşımaz. Ücretin
  gerçek kaydı ziyaretçinin kendi OpenRouter hesabındadır. Bu düzey bir eğitim sitesi için kabul
  edilebilir, bir ödeme sistemi için kabul edilemezdi. Bu kabul bugün hiçbir belgede açıkça yazılı değil;
  örtük bir karardır. Tehdit modelinin değeri tam da bunu yazdırmasıdır.

**I · Bilgi ifşası**
- *Tehdit:* Ziyaretçinin OpenRouter anahtarının sunucuda bir yere düşmesi (log, veritabanı, hata
  gövdesi, istisna zinciri); bir ziyaretçinin başkasının verisini görmesi; hata mesajlarından iç yapının
  sızması; sunucu adresinin ya da bir sırrın herkese açık depoya girmesi.
- *Önlem:* Anahtar yalnız isteğin ömrü boyunca bellekte yaşar. Veritabanına, `ai_kullanim`'a, loga,
  hafıza dosyasına ve hata gövdesine yazılmaz; anahtar başına önbellek yoktur (ADR-0017 §3–4,
  `ai/ZiyaretciChatModelKaynagi`). Sağlayıcı hata verdiğinde `ai/AiHataCevirici` ham gövdeyi atar,
  ziyaretçiye Türkçe bir mesaj üretir ve loga yalnız HTTP kodu ile kategoriyi yazar. Ziyaretçi
  anahtarıyla oluşan hatada asıl istisna `cause` olarak bile taşınmaz, çünkü onun mesajı sağlayıcının ham
  gövdesini içerir. Bunları `SiteSohbetAnahtarTest` ve `AiHataCeviriciTest` kilitler. Hafıza dizini
  kimliğin SHA-256 özetiyle ayrılır (`service/KisiselDizin`). Son çare hata işleyicisi istemciye sabit bir
  metin döner. `service/KitapService` de ayrıştırılamayan model cevabını değil, yalnız uzunluğunu loglar
  (Faz 14). Depo tarafında push öncesi kanca (`tools/git-kancalari/pre-push` → `tools/sizinti-tara.sh`)
  genel IP adreslerini, `root@` biçimli SSH hedeflerini, özel anahtarları, API anahtarlarını ve şifre atamalarını
  engeller. Sunucu adresi depoda yoktur.
- *Kalan risk:* Anahtar tarayıcıda durur; "Bu cihazda hatırla" seçilirse `localStorage`'da. GA ölçüm
  kimliği ayarlıysa Google'ın betiği sayfaya yüklenir. Ayrıca ana sayfanın CSP'si
  `www.googletagmanager.com`'dan betiğe kimlik olmasa da izin verir; bir HTML enjeksiyonu açılırsa bu izin,
  CSP'yi aşmak için kullanılabilecek bir kapıdır (bugün güvenilmeyen metin `textContent` ile basıldığından
  açık bir delik değil). Sayfaya yüklenen her üçüncü
  taraf betik, sayfanın kendi betiği gibi `localStorage`'ı da `sessionStorage`'ı da okuyabilir. ADR-0019,
  Google'a hangi verinin **gönderilmediğini** yazıyordu (kimlik, ilerleme kodu, sohbet metni, anahtar), ama
  yüklenen betiğe duyulan bu güveni bir risk olarak kaydetmiyordu. Bu tehdit modeli yazılırken ortaya çıktı
  ve aynı gün kabul edilen risk olarak ADR-0019'a ve `docs/SECURITY.md`'ye yazıldı (§4c).

**D · Hizmet engelleme**
- *Tehdit:* Bir betiğin her istekte yeni çerezle sınırsız satır açması, sohbet ucunu boğması, dev
  girdiyle belleği ve bağlamı (context) tüketmesi.
- *Önlem:* `web/HizSiniriInterceptor` ve `service/HizSiniri`: yazan istekler IP başına dakikada 60,
  sohbet ziyaretçi başına dakikada 20, geri yükleme IP başına saatte 10, yeni ziyaretçi IP başına saatte
  20; aşımda 429 ve `Retry-After` başlığı. Sınır, yönlendirmenin kullandığı çözülmüş yola bakar;
  `/api/%73ohbet` gibi yüzde kodlamalı bir yol işleyiciye ulaşmadan 400 ile elenir (`SiteHizSiniriTest`).
  Serbest metin girdileri 20.000 karakterle sınırlıdır (`web/GirdiSiniri`). Ziyaretçi satırı yalnız ilk
  yazmada açılır. AI maliyeti ziyaretçinin anahtarına yazıldığı için sunucunun AI faturası yoktur.
- *Kalan risk:* `HizSiniri`'nin kendi yorumu dürüsttür: sayaçlar bellektedir, yeniden başlatma onları
  sıfırlar; bu bir "nezaket/maliyet sınırıdır, güvenlik sınırı değil". IP başına sınır, aynı genel IP'yi
  paylaşan bir sınıfı (tek NAT arkası) tek kullanıcı sayar (`docs/kurulum-site.md` §7.6). En sinsi risk
  bir işletme hatasıdır. Uygulama `X-Forwarded-For` başlığına yalnız güvenilir saydığı vekilden gelince
  inanır; canlıda bu liste `APP_SITE_GUVENILIR_VEKIL` ile Traefik'e daraltılır. Traefik konteynerinin
  IP'si değişirse bütün ziyaretçiler Traefik'in IP'siyle görünür, aynı sayacı paylaşır ve **sitenin
  tamamı 429 alır** (`docs/kurulum-site.md` §10.5). Yani S'ye karşı alınan önlem (vekil güvenini
  daraltmak), yanlış ayarlandığında D'ye dönüşür. Ağ katmanındaki hacimli
  saldırılara karşı uygulamada önlem yoktur; bu, barındırma tarafının alanıdır.

**E · Yetki yükseltme**
- *Tehdit:* Bir ziyaretçinin sitede kapalı olması gereken bir işlevi çalıştırması (sunucu anahtarıyla AI,
  mülakat, kitap notu, MCP araçları); yerel modun (girişsiz, sunucu anahtarlı) yanlışlıkla internete
  açılması.
- *Önlem:* Kapalı varsayılanlar profil düzeyinde kurulu. Sohbet dışı AI uçları `@Profile("!site")` ile
  hiç kayıtlı değildir (404). Site profili sunucu anahtarını zorla boşaltır (`application-site.yml`,
  `spring.ai.openai.api-key: ""`) ve site AI yolu o özelliği hiç okumaz
  (`SiteSohbetAnahtarTest.sunucuAnahtariSitedeKullanilmaz`). Mülakat sitede zorla kapalıdır
  (`config/MulakatEtkin`). Site ve MCP birlikte açılamaz; denenirse uygulama açılışta hata verir
  (`SiteGuvenlikYapilandirma`). `config/BindAdresiDenetimi`, bağlanılan adres yerel döngü (loopback,
  `127.0.0.1` gibi) değilse ve `site` profili yoksa uygulamayı hiç açmaz; imajdaki profil ezilse bile yerel mod dış arayüzde çalışamaz
  (`BindAdresiDenetimiTest`). Konteynerde `content/` ve `prompts/` root'a ait ve salt okunurdur. Yerel
  modda bile atölyenin `ai:false` alıştırmaları ücretli model çağrısından önce reddedilir
  (`service/AtolyeService`); sitede o uç zaten yoktur.
- *Kalan risk:* Sitede rol yoktur; herkes aynı yetkide bir ziyaretçidir. Geriye iki hedef kalır: başka
  bir ziyaretçi olmak (S) ve kapalı bir işlevi açmak. İkincisi dış bir yapılandırmaya, profil ayarına
  bağlıdır; `BindAdresiDenetimi` en tehlikeli yanlış ayarı yakalar, ama her yanlış ayarı değil.

#### 4c. İyi bir iş çıkardık mı? Modelin yüzeye çıkardıkları
Dördüncü soruyu ciddiye alınca, o güne kadar belgelerde yazmayan dört şey görünür:

1. **GA betiği ve tarayıcıdaki anahtar (I).** Yukarıdaki kalan risk. Seçenekler: kabul et (Google'ın
   betiğine güven ve bunu yaz), azalt (sohbet paneli açıkken analitiği yüklememek) ya da ortadan kaldır
   (analitiği kapatmak). Anahtarı `localStorage` yerine `sessionStorage`'da tutmak bir azaltma **değildir**:
   ikisini de aynı origin'deki her betik okur. Karar: kabul edildi ve yazıldı (ADR-0019 "Sonuçlar", yeniden
   açma koşulu: sayfaya başka bir üçüncü taraf betik eklenmesi). Örtük bir güven, yazılı bir karara dönüştü.
2. **`HttpOnly` varsayımı (S).** "Kimlik `HttpOnly` çerezde, JavaScript okuyamaz" cümlesi tek başına doğru,
   ama ilerleme kodu ucu aynı kimliği JSON olarak verdiği için bir XSS'e karşı koruma sağlamaz. Bu
   bilinçli bir özelliktir; tehdit modeli yalnız hangi önlemin gerçekte duvar olduğunu (CSP ve
   `textContent`) netleştirir.
3. **Belge kodun gerisinde kalmış.** `docs/SECURITY.md`'nin "Bilinçli kabul edilen riskler" listesinde
   "500 yanıtında `detay: e.getMessage()`" maddesi hâlâ duruyordu. Oysa `web/HataYonetimi`'nin son çare
   işleyicisi bugün istemciye istisna mesajı değil, sabit bir Türkçe metin ve kısa bir referans döner.
   Bu sefer kod güvenli yönde ilerlemiş, belge geride kalmış (madde bu bulgu üzerine kapatıldı). Tersi de olabilirdi: belge "önlem var"
   der, kod onu çoktan kaybetmiştir. Dördüncü soru tam bunu yakalamak içindir.
4. **Docker ağındaki komşu (S, T).** ADR-0018, aynı ağdaki başka bir projenin konteynerinin
   `X-Forwarded-For` taklit edebileceğini kabul eder ve vekil güvenini daraltarak çözer. Ama o komşu,
   uygulamanın 8090 portuna Traefik'i atlayarak yine ulaşabilir. Bugün bu bir sızıntı değildir: komşu,
   internetteki herhangi bir istemcinin gördüğü uçları aynı kurallarla görür ve kendi IP'siyle sayılır. Ancak "konteynere yalnız Traefik ulaşır" cümlesi
   DFD'ye bu hâliyle girerse, bir sonraki karar (ör. "iç ağdan gelen isteğe yönetim ucu açalım") yanlış
   bir varsayıma dayanır.

Bu tür bulgular, yöntemin neden araçtan çok bir alışkanlık olduğunu gösterir: hiçbiri bir güvenlik
tarama aracının ya da statik analizin bulacağı türden değildir.

### 5. LLM ve ajan tehditlerini STRIDE'a eşlemek
LLM'li bir sistemde DFD'ye yeni bir süreç eklenir: model. Ama bu süreç tuhaftır. Güvenilir talimatı ve
güvenilmez veriyi **aynı girdi akışında** alır; kod ile veri aynı token dizisinde karışır (ayrıntı:
[`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md) §2). Bu yüzden güven sınırı modelin
içinden geçer. DFD'de bunu göstermenin pratik yolu, bağlam penceresine (context window) giren her
parçayı ayrı bir akış olarak çizmek ve yanına kaynağını yazmaktır: sistem prompt'u (senin), ders içeriği
(senin, salt okunur), kullanıcı mesajı (güvenilmez), hafıza (güvenilmez girdiden türediği için
güvenilmez), araç çıktısı (güvenilmez). Microsoft'un AI ve makine öğrenmesi sistemleri için tehdit
modelleme rehberi (2019) de aynı yolu izler: mevcut tehdit modellemesini değiştirmez, AI'a özgü tehdit
listesiyle tamamlar. Yöntem aynı kalır, katalog genişler.

Eşleme (OWASP Top 10 for LLM Applications, 2025 sürümü):

| LLM ya da ajan tehdidi | STRIDE | Neden bu harf? |
|---|---|---|
| Prompt enjeksiyonu (LLM01) | T + E | Talimatın bütünlüğü bozulur; model saldırganın isteğini **senin** yetkinle yapar |
| Sistem prompt'u sızıntısı (LLM07), hassas bilgi ifşası (LLM02) | I | Gizli sanılan metin ve içindekiler açığa çıkar |
| Aşırı yetki, araç kötüye kullanımı (LLM06) | E (+T) | Enjeksiyonun hasarı, aracın yetkisi kadardır |
| Uygunsuz çıktı işleme (LLM05), render edilen markdown ile veri taşıma | I (+T) | Çıktı XSS'e ya da veriyi dışarı taşıyan bir adrese dönüşür |
| Veri ve model zehirlenmesi (LLM04), kalıcı hafıza zehirlenmesi | T (+R) | Kalıcı veri değişir; köken yazılmadıysa kimin yazdığı bilinmez |
| Tedarik zinciri (LLM03), araç tanımı zehirlenmesi | T + E | Başkasının yazdığı metin ya da kod senin yetkinle çalışır |
| Sınırsız tüketim (LLM10) | D | Token, süre ve para tükenir |
| Yanlış bilgi (LLM09) | tam oturmaz | Güvenlikten çok doğruluk sorunudur; eval (değerlendirme testi) ile ele alınır |

Tablodaki E satırlarının ortak fikri eski bir güvenlik kavramıdır: **şaşkın vekil** (confused deputy).
Model kendi yetkisiyle iş yapan bir vekildir; enjeksiyon, o vekile başkasının işini yaptırır. Bu yüzden
LLM'de E'nin önlemi modeli "ikna etmek" değil, vekilin yetkisini daraltmaktır. Savunmaların ayrıntısı
burada tekrar edilmiyor: araç tanımı için §3b, hafıza için §3c, render edilen çıktı için §4d, bilgi
akış denetimi için §5b ([`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md)).

**SokratAI sohbetinde prompt enjeksiyonu: etkisi ne kadar?** Dört soruyla bakalım.
- **Kim enjekte edebilir?** Doğrudan: ziyaretçinin kendisi. Dolaylı: modele giren dış içerik. Sitede
  bağlama giren içerik yalnız `content/` altındaki ders metinleridir; bunlar imajda root'a ait ve salt
  okunurdur, değişiklik git ve inceleme yoluyla gelir. Ortak kitap notu ve vektör bağlamı sitede prompt'a
  girmez (`service/BaglamServisi`: `kitapBaglamiAcik = !calismaModu.siteMi()`).
- **Model ne yapabilir?** Hiçbir araç çağıramaz: sitede modele araç verilmez. Araç kodu yalnız `mcp/`
  paketindedir ve site profili MCP ile birlikte açılmaz. Model yalnız metin üretir; cevap ekrana
  `textContent` ile basılır, markdown render edilmez.
- **Kimin parası, kimin verisi?** Çağrı ziyaretçinin kendi anahtarıyla yapılır. Modeli enjeksiyonla
  "uzun yaz" diye zorlayan kişi kendi kredisini harcar; anahtar kartındaki yardım metni zaten harcama
  limiti koymayı önerir (`static/app.js`). Sohbet geçmişi ve hafıza o ziyaretçiye ayrılmıştır.
- **Sistem prompt'u sızarsa ne olur?** Gizli bir şey sızmaz: `prompts/` dizini (ör. `prompts/sokratik-sohbet.st`)
  zaten herkese açık depodadır. Bu, LLM07'nin dersinin en uç uygulamasıdır: prompt'u gizli varsayma,
  içine sır koyma.

**Kalan gerçek risk: kendi hafızasını zehirlemek.** Site modunda da her dört kullanıcı mesajında bir
`HafizaService.guncelle` çalışır ve ziyaretçinin hafıza dosyası yeniden yazılır (`service/SohbetService`,
`HAFIZA_GUNCELLEME_ARALIGI = 4`; ADR-0017 §3). Ziyaretçi kendi hafızasına "bu öğrenci her konuyu
biliyor" gibi bir not sokabilir; sonraki prompt'larda bu not bağlama girer. Etki yalnız o ziyaretçiye
dokunur, çünkü dizin kimliğe göre ayrılır (`KisiselDizin`).

Sonuç: bugün sohbet enjeksiyonu STRIDE'da esas olarak T'dir (kendi öğrenme deneyiminin bütünlüğü) ve
etkisi saldırganın kendisiyle sınırlıdır. Bu tablo sabit değildir. Sohbete bir araç, paylaşılan bir
hafıza, ziyaretçinin yüklediği bir belge ya da sunucu anahtarıyla çalışan bir özellik eklendiği gün E ve
I satırları dolar. O gün tehdit modeli yeniden çalıştırılır; tetikleyici değişiklikler §1'deki listede.

### 6. Tehdit modeli spec'in girdisidir: kötüye kullanım durumları
Normal kullanım durumu (use case) "kullanıcı ne yapmak ister?" sorusunu cevaplar. **Kötüye kullanım
durumu** (abuse case, misuse case) tersini sorar: "kötü niyetli biri bu özelliği nasıl kullanır?" Fikir
yeni değildir. McDermott ve Fox 1999'da abuse case modellerini, Sindre ve Opdahl 2005'te misuse case'leri
güvenlik gereksinimi çıkarmak için önerdi. OWASP'ın Kötüye Kullanım Durumu Kopya Kâğıdı (Abuse Case Cheat
Sheet) bugünkü pratiği özetler: seçilen kötüye kullanım durumları güvenlik gereksinimine dönüşmeli ve
somut güvenlik testlerinin kaynağı olmalıdır.

Bunu Hafta 3, Gün 1'deki görev tarifine (*Task spec anatomisi*, `content/hafta-03/gun-1.md`) bağlayalım.
Orada dört başlık vardı: hedef, bağlam, kısıtlar, bitiş kriteri. Tehdit modeli bunların üçüne girer:
- **Bağlam:** ilgili güven sınırı ve var olan önlemler ("kimlik `ZiyaretciCerezi`'nde çözülür; bkz.
  ADR-0018 Güvenlik notu").
- **Kısıtlar:** tehdit modelinden çıkan yasaklar ("anahtar hiçbir log satırına girmez", "yeni uç
  dışarıdan `kullanici_id` almaz").
- **Bitiş kriteri:** her kötüye kullanım durumu için bir kabul ölçütü ve onu çalıştıran bir test.

SokratAI'dan gerçek bir zincir:

```text
Tehdit (S):      Aynı adla birden çok aio_ziyaretci çerezi gelirse filtre ilkine güveniyor.
Kötüye kullanım: Kardeş alt alan adını kontrol eden saldırgan, ana alan adına kendi bildiği
                 kimliği yazar; kurbanın ilerlemesi saldırganın bildiği koda kaydolur.
Kabul ölçütü:    Aynı adla FARKLI değerli birden çok çerez gelirse hiçbirine güvenilmez,
                 yeni kimlik üretilip yazılır; aynı değerin tekrarı tek sayılır.
Test:            SiteZiyaretciCokluCerezTest (./mvnw verify içinde koşar)
Kalan risk:      İlk gelişte tek çerez hâlâ kabul edilir → tech-debt #19 (__Host- öneki)
```

Aynı kalıptan üç örnek daha:
- "Saldırgan geçersiz bir anahtarla sohbet ucunu çağırır ve hata gövdesinden anahtarı ya da sağlayıcının
  ham cevabını okumaya çalışır." Ölçüt: 401, Türkçe mesaj; yanıt gövdesinde, logda ve `ai_kullanim`'da anahtar ya da ham sağlayıcı mesajı
  yok. Test:
  `SiteSohbetAnahtarTest.gecersizAnahtar401TurkceVeSizintisiz`.
- "Betik, sohbet sınırını yüzde kodlamalı bir yolla atlamaya çalışır." Ölçüt: kodlanmış yol işleyiciye
  ulaşmaz, 400 ile elenir. Test: `SiteHizSiniriTest`.
- "İmajdaki profil ezilir ve yerel mod dış arayüzde açılır." Ölçüt: uygulama hiç açılmaz. Test:
  `BindAdresiDenetimiTest`.

Kural: kabul ölçütü olumsuz bir cümle olabilir ama çalıştırılabilir olmalıdır. "Güvenli olsun" bir ölçüt
değildir; "farklı değerli iki çerezde yeni kimlik yazılır" bir ölçüttür. Bu, Hafta 3'teki "mekanik bitiş
kriteri" kuralının güvenlikteki karşılığıdır. Tersten de okunur: testi olmayan bir önlem, tehdit modelinde
"var" diye işaretlenmemelidir. Dördüncü soru en ucuz biçimde testle cevaplanır.

## Diyagram 3: Tehditten teste
Üst sıra bir bulgunun spec'e ve teste dönüşmesini, alt sıra bu işte AI'ın ve insanın rolünü gösterir.

```svg
<svg viewBox="0 0 640 340" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Tehdit modelinden teste: STRIDE bulgusu kötüye kullanım durumuna, kabul ölçütüne ve teste dönüşür; altta ajan taslak yazar, farklı bir model bağımsız inceler, insan karar verir">
  <defs>
    <marker id="tm3-ok" markerWidth="10" markerHeight="10" refX="7" refY="3" orient="auto"><path d="M0,0 L7,3 L0,6 Z" fill="#1C3247"/></marker>
  </defs>
  <text x="320" y="24" text-anchor="middle" font-size="15" font-weight="700" fill="#1C3247" font-family="system-ui">Tehdit modelinden spec'e ve teste</text>
  <text x="320" y="44" text-anchor="middle" font-size="11.5" fill="#5D7789" font-family="system-ui">Örnek: aio_ziyaretci ile kimlik sabitleme (ADR-0018 Güvenlik notu)</text>
  <rect x="18" y="62" width="136" height="96" rx="10" fill="#FBEAE7" stroke="#BE4433" stroke-width="1.5"/>
  <text x="86" y="84" text-anchor="middle" font-size="12" font-weight="700" fill="#BE4433" font-family="system-ui">STRIDE bulgusu</text>
  <text x="86" y="106" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">S: kimlik taklidi</text>
  <text x="86" y="122" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">iki farklı çerez</text>
  <text x="86" y="138" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">gelirse ilkine güven</text>
  <rect x="174" y="62" width="136" height="96" rx="10" fill="#FBF0DE" stroke="#C77A17" stroke-width="1.5"/>
  <text x="242" y="84" text-anchor="middle" font-size="12" font-weight="700" fill="#C77A17" font-family="system-ui">Kötüye kullanım</text>
  <text x="242" y="106" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">saldırgan alt alan</text>
  <text x="242" y="122" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">adından bildiği</text>
  <text x="242" y="138" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">kimliği yerleştirir</text>
  <rect x="330" y="62" width="136" height="96" rx="10" fill="#EAF1F7" stroke="#186399" stroke-width="1.5"/>
  <text x="398" y="84" text-anchor="middle" font-size="12" font-weight="700" fill="#186399" font-family="system-ui">Kabul ölçütü</text>
  <text x="398" y="106" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">farklı değerli çoklu</text>
  <text x="398" y="122" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">çerez: hiçbirine</text>
  <text x="398" y="138" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">güvenme, yeni kimlik</text>
  <rect x="486" y="62" width="136" height="96" rx="10" fill="#E9F4EE" stroke="#25804C" stroke-width="1.5"/>
  <text x="554" y="84" text-anchor="middle" font-size="12" font-weight="700" fill="#25804C" font-family="system-ui">Test</text>
  <text x="554" y="106" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">SiteZiyaretci</text>
  <text x="554" y="122" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">CokluCerezTest</text>
  <text x="554" y="138" text-anchor="middle" font-size="10" fill="#5D7789" font-family="system-ui">./mvnw verify</text>
  <line x1="154" y1="110" x2="170" y2="110" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm3-ok)"/>
  <line x1="310" y1="110" x2="326" y2="110" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm3-ok)"/>
  <line x1="466" y1="110" x2="482" y2="110" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm3-ok)"/>
  <text x="320" y="186" text-anchor="middle" font-size="11" font-weight="600" fill="#5D7789" font-family="system-ui">Bu zinciri kim kurar? Taslağı AI yazar, kararı insan verir.</text>
  <rect x="18" y="204" width="180" height="64" rx="10" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="108" y="230" text-anchor="middle" font-size="12" font-weight="700" fill="#4F46E5" font-family="system-ui">Ajan taslağı</text>
  <text x="108" y="250" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">DFD + kod yolu → STRIDE</text>
  <rect x="230" y="204" width="180" height="64" rx="10" fill="#EEF0FF" stroke="#4F46E5" stroke-width="1.5"/>
  <text x="320" y="230" text-anchor="middle" font-size="12" font-weight="700" fill="#4F46E5" font-family="system-ui">Farklı model inceler</text>
  <text x="320" y="250" text-anchor="middle" font-size="10" fill="#1C3247" font-family="system-ui">kodu ver, sonucu değil</text>
  <rect x="442" y="204" width="180" height="64" rx="10" fill="#1C3247" stroke="#1C3247" stroke-width="1.5"/>
  <text x="532" y="230" text-anchor="middle" font-size="12" font-weight="700" fill="#FFFFFF" font-family="system-ui">İnsan karar verir</text>
  <text x="532" y="250" text-anchor="middle" font-size="10" fill="#FFFFFF" font-family="system-ui">azalt · kabul et · yaz</text>
  <line x1="198" y1="236" x2="226" y2="236" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm3-ok)"/>
  <line x1="410" y1="236" x2="438" y2="236" stroke="#1C3247" stroke-width="1.8" marker-end="url(#tm3-ok)"/>
  <rect x="18" y="286" width="604" height="40" rx="8" fill="#FFFFFF" stroke="#5D7789" stroke-width="1.2" stroke-dasharray="5 4"/>
  <text x="320" y="311" text-anchor="middle" font-size="10.5" fill="#1C3247" font-family="system-ui">Kabul edilen kalan risk de bir çıktıdır: tetik koşuluyla yazılır (tech-debt #19: __Host- öneki)</text>
</svg>
```

### 7. AI yardımı ve sınırları
**Ajanla taslak çıkarmak.** Bir ajan tehdit modelinin geniş ve yorucu kısmında iyidir: koddan DFD
çıkarmak, her öğe için STRIDE sorularını tek tek sormak, önlemleri dosya yollarıyla eşlemek, unutulan bir
kategoriyi hatırlatmak. İyi bir istek üç şey verir: kapsam (hangi profil, hangi dağıtım), kaynaklar
(ADR'ler, güvenlik yapılandırması, dağıtım belgesi) ve çıktı biçimi ("her satırda tehdit, önlem, dosya
yolu, kalan risk; önlem yoksa 'yok' yaz").

Ajan iki yerde zayıftır:
- **Bağlamında olmayanı bilemez.** Bu metindeki en pahalı tehditlerden biri koddan değil, canlı
  işletmeden çıktı. Dokploy'un varsayılan Swarm VIP modunda (trafiği sanal bir IP üzerinden dağıtan Docker
  Swarm ayarı) uygulama tüm istemcileri bir yük dengeleyicinin iç adresi olarak görüyordu; kurulum belgesi bunu "ilk canlı kurulumda yaşandı" diye not
  eder (`docs/kurulum-site.md` §10.5). Depoyu okuyan hiçbir model, depoda yazmayan bir platform
  varsayılanını tahmin edemezdi.
- **Kendinden emin uydurur.** "CSP başlığı ayarlı" diyebilir; oysa SokratAI'da CSP bir `<meta>`
  etiketidir ve bu fark `frame-ancestors` gibi yönergeleri etkiler. Kural: ajanın her önlem iddiası bir
  dosya yoluyla gelir ve sen o dosyayı açarsın. Bu metindeki her SokratAI iddiası bu kuralla doğrulandı.

**Çok modelli çapraz kontrol (multi-model cross-check).** Taslağı yazan modelden farklı bir model,
mümkünse başka bir sağlayıcının modeli, taslağı ya da kodu bağımsız inceler. Gerekçe: aynı modelin
kopyaları aynı kör noktaları paylaşır ([`ajan-mimarisi-desenleri.md`](ajan-mimarisi-desenleri.md),
oylama bölümü). Bunun ölçülmüş bir karşılığı da var. Kim ve arkadaşlarının ICML 2025'te yayımlanan
"Correlated Errors in Large Language Models" (büyük dil modellerinde ilişkili hatalar) çalışması, bir
lider tablosu veri kümesinde iki model birlikte yanıldığında **vakaların %60'ında aynı yanlış cevabı**
verdiklerini raporlar. Aynı çalışmaya göre daha büyük ve daha isabetli modellerin hataları, farklı
mimarilerde ve farklı sağlayıcılarda bile yüksek oranda ilişkilidir. Bu bulgunun iki yüzü var:
- **Çapraz kontrol işe yarar:** farklı eğitim, farklı prompt ve farklı araç, farklı kör nokta ihtimali
  demektir; ikinci model ilkinin atladığını bulabilir.
- **Ama garanti değildir:** sağlayıcı değiştirmek hataları bağımsız yapmaz. İki model aynı şeyi
  kaçırabilir; ikisinin **anlaşması** doğruluğun kanıtı değildir.

Uygulamada üç kural:
1. **İnceleyiciye sonucu değil, kodu ver.** "Şu tehdit modelini onayla" dersen ikinci model ilkinin
   çerçevesine takılır. Kapsamı, DFD'yi ve dosyaları ver; tehditleri kendisi çıkarsın; sonra iki listeyi
   karşılaştır.
2. **Ortak noktaya değil, farka bak.** Asıl bilgi iki modelin ayrıştığı yerdedir.
3. **Kararı insan verir.** "Kabul et" bir iş kararıdır: hangi riski, kimin parasıyla, hangi tetik
   koşuluna kadar taşıyacağına ancak sistemin sahibi karar verebilir. Model bir öneri yazar; imza senindir.

**Bu depoda nasıl uygulanıyor?** AGENTS.md'nin "PR / commit kuralları" bölümündeki bir OVERRIDE, genel
standardın "Dockerfile, dağıtım, güvenlik ya da migration (veritabanı şema değişikliği) değişikliğinde
ikinci bir insan gözden geçirici" kuralının yerine şunu koyar: bu değişikliklerde `qa-code-reviewer`
(bağımsız inceleme ajanı) **ve** Codex incelemesi (başka bir sağlayıcının modeli) zorunludur ve bulgular
push'tan önce kapatılır. Gerekçe açıkça yazılı: repoda tek geliştirici var, ikinci insan yok. Kayıtlar
bu düzenin bir şeyler bulduğunu gösteriyor. ADR-0018, çoklu çerezle kimlik sabitlemeyi "Otomatik
inceleme bulgusu (Faz 11)" olarak kaydeder (hangi incelemenin bulduğunu yazmaz). Faz 14 planı (`docs/exec-plans/completed/faz-14-turkce-dil-duzeltmesi.md`)
bir "Codex bulgusu" kaydeder: boş ya da süzülmüş cevapta `ai_kullanim` satırı yazılmıyordu, şimdi hata
fırlatılmadan önce yazılıyor. Bu, R satırındaki önlemin ta kendisidir. Sınırı da aynı kayıtlarda
görürsün: hiçbir inceleme `__Host-` önekini "düzeltmedi", çünkü düzeltmenin bir bedeli vardı (yerel http
denemesi kırılır, mevcut çerezler geçersiz olur). O risk, tetik koşuluyla birlikte bir insan kararı olarak
tech-debt #19'a yazıldı. Modeller bulur; insan karar verir ve kararı yazar.

### 8. ⚠️ ZORUNLU ŞERH: "AI tehdit modelini otomatik çıkarır" ve satıcı vaatleri
Pazarlama dilinde karşına çıkabilecek üç iddia var. Üçü de yarı doğrudur.

**"Araç tehditleri otomatik üretir."** Doğru ve yeni değil. Microsoft'un Tehdit Modelleme Aracı (bir
Windows masaüstü uygulaması) STRIDE tabanlı tehdit üretimini şablonlardan yapar. OWASP Threat Dragon bir
kural motoruyla tehditleri ve önlemleri otomatik önerir; STRIDE'ın yanında LINDDUN (gizlilik) ve PLOT4ai
(yapay zekâ) gibi kataloglarla da çalışır. Microsoft'un araç sayfası (Kasım 2026 güncellemesi) ayrıca
tarayıcıda çalışan, açık kaynaklı SecureDesignStudio'yu tanıtıyor. Bu araçların ürettiği şey, DFD'deki
öğe türüne göre **genel** bir listedir: "veri akışında kurcalama", "veri deposunda ifşa". Liste ikinci
soruya bir başlangıç verir. Ama ne birinci soruyu (sistemin doğru bir modeli) ne üçüncü soruyu
(kararlar) cevaplar. Bir LLM'in aynı listeyi akıcı Türkçeyle yazması onu daha doğru yapmaz.

**"Dakikalar içinde eksiksiz tehdit modeli."** Tehdit modelinin zor kısmı yazmak değil, **bilmektir**.
Traefik'in IP'si yeniden oluşturmada değişir mi? Alan adı altına başka bir proje bağlanacak mı? Bir
ziyaretçinin kendi hafızasını zehirlemesi bizim için sorun mu? Bu soruların hiçbirinin cevabı kodda
yoktur. Manifesto'nun "kahraman tehdit modelcisi" karşı deseni burada yeni bir kılık giyer: kahraman artık
bir uzman değil, bir araçtır. "Eksiksiz" kelimesi de şüphelidir, çünkü STRIDE bile eksiksiz bir
sınıflandırma değildir. Gizlilik tehditleri (LINDDUN gibi ayrı yöntemler bu yüzden var), iş mantığının
kötüye kullanımı, maliyet tüketimi ve modelin yanlış bilgi üretmesi altı harfe tam oturmaz. STRIDE bir
düşünme listesidir, tamlık kanıtı değildir.

**"Aracımız açıkların %X'ini yakalıyor."** Böyle bir rakam gördüğünde dört soru sor: hangi açıklara karşı
ölçüldü, kim doğruladı, yanlış alarm oranı ne, araç bu açıkları önceden biliyor muydu? Bu metin yöntemi
belli olmayan hiçbir yüzde alıntılamıyor; senin tasarım belgen de alıntılamasın. Kendi tehdit modelin için
daha dürüst ölçütler var: kaç önlemin bir testi var, kaç "kabul et" kararının yazılı bir tetik koşulu var,
model en son ne zaman koda karşı kontrol edildi?

Sonuç: AI tehdit modelini hızlandırır ve genişletir, ama sahipliğini almaz. İyi kullanım şudur: ajan
taslak yazar, farklı bir model bağımsız inceler, insan kararları verir ve yazar, testler kararları kilitler.

### Bu programdaki ilgili kaynaklar
- Aynı dizinin kardeş metinleri (AI çağında teslimat döngüsü): [`test-kalitesi-ai-cagi.md`](test-kalitesi-ai-cagi.md), [`api-sozlesme-once.md`](api-sozlesme-once.md), [`teslimat-hatti-gozlemlenebilirlik.md`](teslimat-hatti-gozlemlenebilirlik.md).
- [`llm-uygulama-guvenligi.md`](llm-uygulama-guvenligi.md): prompt enjeksiyonu, araç tanımı ve hafıza
  zehirlenmesi, render edilen çıktı, bilgi akış denetimi; bu metindeki LLM eşlemesinin ayrıntısı.
- [`temel-secure-coding.md`](temel-secure-coding.md): güven sınırı, girdi doğrulama, hata yönetimi;
  STRIDE önlemlerinin kod düzeyi.
- [`ajan-sandbox-izolasyonu.md`](ajan-sandbox-izolasyonu.md): ajanın kendi makinende çalışırken
  sınırlanması; E ve I satırlarının geliştirici tarafı.
- [`ajan-calisma-zamani-kontrolleri.md`](ajan-calisma-zamani-kontrolleri.md): harcama tavanı, yetki
  merdiveni, acil durdurma; D ve E'nin ajan çalışma zamanındaki karşılığı.
- [`ajan-mimarisi-desenleri.md`](ajan-mimarisi-desenleri.md): aynı modelin kopyalarıyla oylamanın neden
  bağımsız teyit olmadığı.
- [`hafta06-mimari-stiller-c4.md`](hafta06-mimari-stiller-c4.md): C4 diyagramları; DFD'ye başlangıç
  noktası.
- [`mimari-degerlendirme-kata.md`](mimari-degerlendirme-kata.md): senaryoyla mimari sorgulama (ATAM);
  "iyi bir iş çıkardık mı?" sorusunun yakın akrabası.
- [`ai-yonetisim-uyum.md`](ai-yonetisim-uyum.md): KVKK, GDPR; I satırının hukuki tarafı.
- Hafta 3, Gün 1 (*Task spec anatomisi*): kötüye kullanım durumlarının kabul ölçütü olarak girdiği spec
  şablonu. Hafta 4 (*Guardrail'lar*) ve Hafta 7 (*AI Engineering I*): bu metnin bağlı olduğu haftalar.

## Öz-sınav (kendini test et)
1. Shostack'ın dört sorusunu yaz. Manifesto dördüncü soruyu nasıl değiştirir ve bu değişiklik neden
   önemlidir?
2. Tehdit modeli ne zaman yapılır? "Değişiklikte yeniden" kuralını uygulanabilir kılan, modeli değiştiren
   değişiklik türlerini say.
3. DFD'nin beş öğesini say. Tehditler neden en çok güven sınırını geçen akışlarda yoğunlaşır? SokratAI
   DFD'sindeki üç güven sınırını adlandır.
4. STRIDE'ın altı harfini, her birinin çiğnediği güvenlik özelliğiyle eşleştir.
5. Öğe başına STRIDE rehberine göre bir veri akışında hangi kategoriler aranır? Bir veri deposu log ise
   neden R de eklenir?
6. Bir tehdide verilebilecek dört cevabı yaz. "Kabul et" hangi koşulda karar, hangi koşulda unutulmuş
   risk olur? SokratAI'dan "kabul et", "ortadan kaldır" ve "devret" için birer örnek ver.
7. `aio_ziyaretci` çerezi `HttpOnly`. Buna rağmen sayfaya sızan bir XSS ziyaretçinin kimliğini nasıl
   okuyabilir? Bu durumda asıl duvar hangi önlemlerdir?
8. `APP_SITE_GUVENILIR_VEKIL` hangi tehdide karşı alınmış bir önlemdir ve yanlış ayarlandığında neden
   başka bir STRIDE kategorisine dönüşür?
9. SokratAI'da inkâr edilemezlik neden zayıftır? Bu zayıflık bir eğitim sitesinde neden kabul edilebilir,
   bir ödeme sisteminde neden değildir?
10. Prompt enjeksiyonunu STRIDE'da hangi iki harfe eşlersin, neden? SokratAI sohbetinde bugünkü etkisini
    dört soruyla (kim, ne yapabilir, kimin parası, sızarsa ne olur) değerlendir. Hangi değişiklik bu
    değerlendirmeyi geçersiz kılar?
11. Bir tehdit bulgusunu kötüye kullanım durumuna, kabul ölçütüne ve teste dönüştür. Hafta 3, Gün 1'deki
    dört başlıktan hangisine ne girer?
12. İkinci modeli neden farklı bir sağlayıcıdan seçersin? "Correlated Errors" bulgusu bu uygulamanın hem
    gerekçesini hem sınırını nasıl gösterir? İnceleyiciye neyi verirsin, neyi vermezsin?
13. Ajanın çıkardığı tehdit modelinde "CSP başlığı ayarlı" yazıyor. Bunu nasıl doğrularsın, SokratAI'da
    gerçek durum nedir ve fark neden önemlidir? Hangi tehdit depodan hiçbir modelle çıkarılamazdı?
14. **Şerh sorusu:** Bir satıcı "AI aracımız tehdit modelinizi dakikalar içinde eksiksiz çıkarır ve
    açıkların %90'ını yakalar" diyor. Cümlenin her parçasını nasıl okursun? Manifesto'nun hangi karşı
    deseni burada yeni bir kılıkla karşına çıkar? STRIDE'ın kendisi neden "eksiksiz" değildir?

### Cevap anahtarı (önce kendin cevapla, sonra bak)
1. Ne üzerinde çalışıyoruz? Ne ters gidebilir? Bu konuda ne yapacağız? İyi bir iş çıkardık mı? Manifesto
   "yeterince iyi" der: hedef kusursuzluk değil, karar vermeye yetecek doğruluktur.
2. Tasarımda, spec ile birlikte, koddan önce; sonra sistemle birlikte güncellenir. Tetikleyiciler: yeni
   uç, yeni veri türü, yeni dış servis, yeni güven sınırı, yeni AI aracı ya da hafıza türü. SokratAI'da:
   girişsiz siteye geçiş (ADR-0017), Docker'a geçiş (ADR-0018).
3. Dış varlık, süreç, veri deposu, veri akışı, güven sınırı. Sınırda güven, sahiplik ya da yetki değişir;
   bir tarafın varsayımı öteki tarafta geçmez, doğrulama orada yapılır. GS1: internetten sunucuya (TLS).
   GS2: Docker ağı (Traefik'ten uygulamaya, komşu konteyner). GS3: üçüncü taraf (OpenRouter).
4. S kimlik doğrulama, T bütünlük, R inkâr edilemezlik, I gizlilik, D erişilebilirlik, E yetkilendirme.
5. T, I ve D. Log "kim ne yaptı" sorusunun kanıtıdır; silinir ya da değiştirilirse inkâr mümkün olur.
6. Azalt, ortadan kaldır, devret, kabul et. Yazılı ve tetik koşullu ise karar, yazılmamışsa unutulmuş
   risk. Kabul: `__Host-` öneki (tech-debt #19). Ortadan kaldırma: sohbet dışı AI uçlarının sitede hiç
   kayıtlı olmaması. Devretme: AI maliyetinin ziyaretçinin kendi anahtarına yazılması (ADR-0017).
7. `GET /api/ziyaretci/kod` kimliği aynı kökten çalışan JavaScript'e verir; XSS bu ucu çağırıp okur. Asıl
   duvar: `index.html`'deki CSP ve güvenilmeyen metnin `textContent` ile basılması.
8. Kaynak IP taklidine karşı (S): komşu konteyner sahte `X-Forwarded-For` ile hız sınırını atlatmasın.
   Yanlış ayarda herkes Traefik'in IP'siyle görünür ve site toptan 429 alır (D); bu yüzden işletme adımı
   ve doğrulama gerekir (`docs/kurulum-site.md` §10.5).
9. Kimlik kişiye değil çereze bağlı; log satırı kimlik taşımaz; satırlar 180 günde silinir; ücretin
   gerçek kaydı OpenRouter'da. Eğitim sitesinde para ve yetki yoktur. Ödemede "ben yapmadım" itirazı
   parayla ilgilidir ve kanıt ister: kimlik doğrulama ve korunan bir denetim kaydı.
10. T (talimatın bütünlüğü) ve E (şaşkın vekil). Kim: ziyaretçinin kendisi; dolaylı kaynak yalnız salt
    okunur `content/`. Ne yapabilir: araç yok, yalnız metin. Kimin parası: ziyaretçinin. Sızarsa:
    `prompts/` zaten herkese açık. Kalan risk: kendi hafızasını zehirlemek. Geçersiz kılan: araç,
    paylaşılan hafıza, yüklenen belge ya da sunucu anahtarlı bir özellik.
11. Örnek: §6'daki çoklu çerez zinciri. Bağlam: güven sınırı ve mevcut önlem (`ZiyaretciCerezi`,
    ADR-0018). Kısıt: tehdit modelinden çıkan yasak. Bitiş kriteri: çalıştırılabilir kabul ölçütü ve test
    (`SiteZiyaretciCokluCerezTest`). Hedef değişmez.
12. Aynı sağlayıcının modelleri benzer eğilimleri paylaşır. Çalışmaya göre birlikte yanılan iki model
    vakaların %60'ında aynı yanlışı veriyor ve büyük modellerin hataları sağlayıcılar arasında bile
    ilişkili: bu hem gerekçe hem sınırdır, anlaşma kanıt değildir. İnceleyiciye kapsamı, DFD'yi ve kodu
    verirsin; ilk modelin sonucunu vermezsin.
13. Dosyayı açarsın: CSP `index.html` içinde bir `<meta http-equiv>` etiketidir, HTTP başlığı değildir;
    `frame-ancestors` gibi yönergeler `<meta>` ile verilemez. Depodan çıkarılamayan tehdit: Dokploy'un
    Swarm VIP varsayılanı ve Traefik IP'si değişince toptan 429; canlı kurulumda yaşanarak öğrenildi.
14. "Dakikalar": yazmak hızlıdır, bilmek değil; birinci ve üçüncü sorunun cevabı kodda yoktur.
    "Eksiksiz": STRIDE bile değildir (gizlilik, iş mantığı, maliyet, yanlış bilgi). "%90": neye karşı,
    kim doğruladı, yanlış alarm oranı ne? Yöntemsiz rakam alıntılanmaz. Karşı desen: kahraman tehdit
    modelcisi, bu kez bir araç kılığında.

## Kaynaklar
- **Adam Shostack, Four Question Frame for Threat Modeling** (CC-BY):
  https://github.com/adamshostack/4QuestionFrame ve https://shostack.org/resources/threat-modeling
- **Adam Shostack, *Threat Modeling: Designing for Security*** (Wiley, Şubat 2014):
  https://www.wiley.com/en-us/Threat+Modeling%3A+Designing+for+Security-p-9781118809990
- **Threat Modeling Manifesto** (tanım, değerler, ilkeler, karşı desenler; CC BY 4.0):
  https://www.threatmodelingmanifesto.org/ (yayın duyurusu, 17 Kasım 2020:
  https://shostack.org/blog/threat-modeling-manifesto/)
- **Loren Kohnfelder, Praerit Garg, "The Threats to Our Products"** (Microsoft, 1 Nisan 1999; STRIDE'ın
  ilk metni): https://adam.shostack.org/resources/early-threat-modeling ; tarihçe:
  https://shostack.org/blog/20-years-of-stride-looking-back-looking-forward/
- **Microsoft Learn, STRIDE kategorileri ve güvenlik denetimleri eşlemesi** (eğitim modülü):
  https://learn.microsoft.com/en-us/training/modules/tm-use-a-framework-to-identify-threats-and-find-ways-to-reduce-or-eliminate-risk/1b-threat-modeling-framework
- **Microsoft Threat Modeling Tool:** tehdit kategorileri
  https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats ; DFD öğeleri
  https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-feature-overview ; araç
  sayfası (TMT ve SecureDesignStudio) https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool
- **Microsoft, Threat Modeling AI/ML Systems and Dependencies** (Kasım 2019):
  https://learn.microsoft.com/en-us/security/engineering/threat-modeling-aiml
- **OWASP Threat Modeling Cheat Sheet:**
  https://cheatsheetseries.owasp.org/cheatsheets/Threat_Modeling_Cheat_Sheet.html
- **OWASP Abuse Case Cheat Sheet:** https://cheatsheetseries.owasp.org/cheatsheets/Abuse_Case_Cheat_Sheet.html
- **OWASP Threat Dragon:** https://owasp.org/www-project-threat-dragon/
- **OWASP Top 10 for LLM Applications 2025:** https://genai.owasp.org/llm-top-10/
- **J. McDermott, C. Fox, "Using abuse case models for security requirements analysis"**, ACSAC 1999,
  doi:10.1109/CSAC.1999.816013
- **G. Sindre, A. L. Opdahl, "Eliciting security requirements with misuse cases"**, Requirements
  Engineering 10(1), 2005, doi:10.1007/s00766-004-0194-4
- **E. Kim, A. Garg, K. Peng, N. Garg, "Correlated Errors in Large Language Models"**, ICML 2025:
  https://arxiv.org/abs/2506.07962
- **Bu depo (çalışılmış örnek):** `docs/adr/0017-girissiz-site-ve-kendi-anahtarin.md`,
  `docs/adr/0018-dokploy-konteyner-dagitimi.md`, `docs/adr/0019-google-analytics-4-ve-seo.md`,
  `docs/SECURITY.md`, `docs/kurulum-site.md` (§7.6, §10), `docs/tech-debt-tracker.md` (#19), `AGENTS.md`
  ("PR / commit kuralları"); kod yolları metin içinde verildi.
