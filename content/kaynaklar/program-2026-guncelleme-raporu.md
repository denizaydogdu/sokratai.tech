---
baslik: "Program 2026 Güncelleme Raporu: Kaynak Tazeliği ve Boşluk Analizi"
yazar: "3 paralel web-araştırma ajanı (Claude), Temmuz 2026"
tarih: 2026-07-04
url: ""
etiketler: 1,2,3,4,7,8,10,11,12
---

# Program 2026 Güncelleme Raporu

### Bu metin ne anlatıyor?
Bu bir ders metni değil, programın kendi **bakım kaydıdır**. 12 haftalık eğitimin
kaynakları belirli aralıklarla taranıp "hangi bilgi eskidi, hangi konu eksik kaldı, neyi
eklemeye karar verdik, neyi bilerek eklemedik" diye yazıldı. Konuları burada öğrenmezsin;
burada programın neden şu anki hâlinde olduğunu okursun.

Bu yüzden burada diyagram yok: anlatılan bir konu değil, tutulan bir defter var.

### Bu raporu nasıl okumalı: üç kural
1. **Tarih koyarak oku.** Her madde yazıldığı günün fotoğrafıdır. Model adları (GPT-5.5,
   Sonnet 5), sürüm numaraları ve ürün isimleri en hızlı eskiyen kısımdır; bir karar
   vermeden önce güncel hâlini doğrula.
2. **İddianın kaynağını sor.** Bir rakamı kim ölçtü? Ölçen tarafın o sonuçtan **ticari
   çıkarı** var mı? Bir güvenlik şirketinin "kodların %45'i açıklı" ölçümü ile bağımsız bir
   akademik çalışma aynı ağırlıkta değildir. Aşağıda satıcı kaynaklı rakamlara ⚠️ ile şerh
   düşüldü.
3. **"Eklemedik" maddeleri de karardır.** Raporun sonundaki kapanışlar başarısızlık değil;
   kanıt bulunamadığı için içerik üretilmemesidir. Bir konunun müfredata girmemesi de
   gerekçelendirilmesi gereken bir mimari karardır.

### Raporda geçen kısaltmalar
- **Ajan** (agent): Kendi başına adım adım iş yapan yapay zekâ programı.
- **Harness** (koşum takımı): Bir yapay zekâ modelinin etrafına kurulan araç, kural ve
  geri besleme düzeni. Model aynıyken bile harness sonucu değiştirir.
- **Eval** (değerlendirme): Bir yapay zekâ çıktısının kalitesini otomatik ölçen test.
- **Guardrail** (koruma kuralı): Modelin izin verilen sınırlar içinde kalmasını sağlayan denetim.
- **RCT** (Randomized Controlled Trial, randomize kontrollü deney): Katılımcıların rastgele
  iki gruba ayrıldığı, en güvenilir sayılan deney türü.
- **RAG** (Retrieval-Augmented Generation): Modele cevap verdirmeden önce ilgili belgeleri
  bulup bağlamına koyma yöntemi.
- **MCP** (Model Context Protocol): Yapay zekâ istemcilerinin dış araçlara/verilere bağlandığı
  açık protokol.
- **OWASP**: Uygulama güvenliği alanında en bilinen açık kaynaklı topluluk; "Top 10" listeleri
  yayımlar.
- **OTel** (OpenTelemetry): Yazılımın içini izlemek (log, metrik, iz) için açık standart.
- **SBOM** (Software Bill of Materials): Yazılımın içindeki tüm bileşenlerin listesi, yani
  "malzeme listesi".
- **CRA** (Cyber Resilience Act): AB'nin dijital ürün güvenliği yasası.
- **SLM** (Small Language Model): Küçük, çoğu zaman cihaz üstünde çalışan dil modeli.
- **PoC** (Proof of Concept): Denemelik prototip; üretimde kullanılan sistem değil.
- **GA** (General Availability): Ürünün deneme aşamasından çıkıp genel kullanıma açılması.
- **Allowlist** (izin listesi): Yalnızca listedekilere izin veren güvenlik kuralı.
- **Drift** (sapma): Sistemin zamanla tasarlanan hâlinden sessizce uzaklaşması.

12 haftalık programın kaynakları 4 Temmuz 2026'da ~45 web aramasıyla denetlendi.
**Genel hüküm: programın omurgası doğru ve öncü çıktı** (iSAQB'nin yeni SWARC4AI
modülü ve SFIA AI beceri çerçevesi programın yönünü bağımsız olarak doğruluyor). Ama
kaynak sürümleri tazelenmeli ve beş boşluk kapatılmalı.

## Hafta 1: Harness Mühendisliği

- OpenAI "Harness Engineering" (Şub 2026) hâlâ kurucu metin; ekosistem hızla büyüdü.
  **LangChain'in ampirik kanıtı**: aynı model, farklı harness → Terminal Bench'te
  %52,8 → %66,5. "Harness engineering 2026'nın en etkili terimi."
  ⚠️ **Şerh:** Bu ölçümü, harness araçları satan bir şirket kendi yaptı ve tek bir
  kıyaslama testi (Terminal Bench) üzerinde yaptı. "Harness fark yaratır" yönü makul; ama
  **belirli yüzde** genellenemez ve "2026'nın en etkili terimi" bir ölçüm değil, bir
  pazarlama cümlesidir. Doğru kullanım: yönü öğret, rakamı "LangChain'in kendi ölçümü"
  diye atfet.
- **Böckeler'in kısa notu (memo) Nisan 2026'da tam bir makaleye dönüştü**
  (martinfowler.com/articles/harness-engineering.html): harness öğeleri
  **guides (ileri besleme) ve sensors (geri besleme)** olarak ikiye ayrılıyor; her biri
  hesaplamalı (computational) ya da çıkarımsal (inferential) olabiliyor. Kurum içi paylaşım
  için harness şablonları (harness templates) var. Devam yazısı: "Maintainability sensors for
  coding agents" (kodlama ajanları için bakım kolaylığı sensörleri). Ders bu sınıflandırmayla
  (taksonomi) işlenmeli.
- Terimin kökeni: **Mitchell Hashimoto** (Şub 2026): "Ajan bir hata yaptığında, çözümü öyle
  tasarla ki ajan o hatayı bir daha asla yapmasın."
- YENİ ALT MODÜL: **Context Engineering** (bağlam mühendisliği). Karpathy'nin terimi (2025),
  Anthropic'in resmî rehberiyle (Eyl 2025) tanımı netleşti; prompt mühendisliğinin (prompt
  engineering) yerini büyük ölçüde aldı. Formül genişledi: Harness = Model + Context.
  Pratik teknikler: sıkıştırma (compaction), araç sonucu (tool result) temizliği, bellek
  (memory) tasarımı, RAG'e karşı tam zamanında (just-in-time) bağlam.

## Hafta 2: Repo Bilgi Sistemi

- AGENTS.md kurumsallaştı: Linux Foundation / Agentic AI Foundation (AAIF,
  170+ üye) yönetiminde ve 60.000'den fazla projede kullanılıyor. "Kısa AGENTS.md + docs/"
  pratiği hâlâ önerilen desen.
- **EN BÜYÜK EKLEME: Agent Skills / SKILL.md**. Anthropic'in açık standardı
  (Ara 2025, agentskills.io); Haziran 2026'da Codex, Copilot, Cursor, Gemini CLI
  dahil ~40 ürün destekliyor, yani farklı ajanların ortak kullandığı bir standart. İşbölümü:
  AGENTS.md = her oturumda yüklenen repo kuralları; SKILL.md = isteğe bağlı
  yüklenen yetenek paketi (kademeli açma, progressive disclosure). Haftanın yeni çerçevesi:
  **repo bilgi sisteminin üç katmanı: AGENTS.md + docs/ + skills/**.
- ADR'ler artık ajanları da hesaba katıyor: AGENTS.md'ye "mimari karardan önce
  docs/adr/ kontrol et; yeni karar için önce ADR taslağı" kuralını yazmak
  2026'nın yaygın pratiği.

## Hafta 3: Spec-Driven Akış

- Model adları eskidi: Codex'te güncel öneri **GPT-5.5** (karmaşık işler) +
  5.4-mini (alt-ajanlar). Codex Remote genel kullanıma açıldı (GA); alan adı izin listesiyle
  (domain allowlist) internet erişimi ve çoklu ajana iş devretme (multi-agent delegation) geldi.
- Claude Code: plan modu duruyor; yeni gelenler **Ultraplan** (planı bulutta taslakla,
  uzaktan çalıştır) ve Auto mode (otomatik kip); varsayılan model Sonnet 5 (1M bağlam).
  ⚠️ **Şerh (model ve sürüm adları):** Yukarıdaki iki madde raporun **en hızlı eskiyen**
  kısmıdır ve tamamı satıcı duyurularına dayanır. Model adlarını ezberleme; öğrenilecek şey
  ürün ismi değil, **desendir**: plan modu, uzaktan çalıştırma, alt-ajanlara devretme, geniş
  bağlam. Bir ada göre karar vereceksen sağlayıcının güncel belgesinden doğrula.
- Spec-driven yaklaşım artık niş değil, standart: GitHub Spec Kit 90 binden fazla yıldız aldı
  (v0.8.7, May 2026);
  Amazon **Kiro** uluslararası lansman yaptı ve Amazon Q Developer'ın yerini aldı.
- Çerçeve değişimi: R→P→E→R→S akışı geçerli, ama spec-driven artık
  **context engineering şemsiyesinin alt kümesi**. Yeni pratik: kod incelemesine
  benzer biçimde **spec incelemesi / bağlam denetimi (spec review / context audit)**.

## Hafta 4: Guardrails

- Araçlar hâlâ geliştiriliyor: import-linter v2.12 (Haz 2026; yeni acyclic_siblings/protected
  kontratları), ArchUnit 1.4.2.
- **Veracode Spring 2026 (bahar sürümü) güncellemesi**: rakamlar değişmedi. AI'ın ürettiği
  kodun %45'i hâlâ OWASP listesindeki açıkları içeriyor; Java en riskli dil (%72 başarısızlık);
  XSS testinden yalnız %15'i, log injection testinden %13'ü geçiyor. "Modeller iddialara rağmen
  güvenlikte gelişmiyor."
  ⚠️ **Şerh:** Veracode bir **kod güvenliği tarama ürünü satan şirkettir**; "üretilen kodun
  yarısı açıklı" bulgusu kendi ürününe talep yaratır. Ayrıca yüzdeler, şirketin kendi
  seçtiği görev kümesi ve kendi tarayıcısının tanımına göre hesaplanır; başka bir tarayıcı
  başka bir sayı verir. Dersin dayanağı **yön** olmalı ("üretilen kod güvenli varsayılamaz,
  kapı koy"), yüzde değil. Kaynağı derste "Veracode'un ölçümü" diye adlandır.
- **Thoughtworks Radar Vol 34 (Nis 2026) bu haftanın ders kitabı**:
  "architecture drift reduction with LLMs" (LLM'lerle mimari sapmayı azaltma: ArchUnit/Spring
  Modulith gibi deterministik araçlar + LLM değerlendirmesi) ve "feedback sensors for coding
  agents" (kodlama ajanları için geri besleme sensörleri: derleyici/linter/test → ajan
  döngüsüne otomatik düzeltme sensörü). Temalar: fazla yetki isteyen ajanları güvenceye almak,
  ajanları kısa tasmada tutmak, **AI kaynaklı bilişsel borç (cognitive debt)**.
- KRİTİK BOŞLUK: **Ajan güvenliği**. OWASP "Top 10 for Agentic Applications
  2026" (ajan uygulamaları için en önemli 10 risk; 9 Ara 2025): Agent Goal Hijack (ajanın
  hedefinin ele geçirilmesi), Tool Misuse (aracın kötüye kullanımı), Identity & Privilege
  Abuse (kimlik ve yetki istismarı),
  **Agentic Supply Chain (ajan tedarik zinciri; MCP sunucuları da tedarik zinciridir!)**,
  Memory Poisoning (belleğin zehirlenmesi), Rogue Agents (kontrolden çıkmış ajanlar). Mevcut H4 "AI'ın ürettiği kodun güvenliği"ne
  bakıyor; "AI sisteminin KENDİSİNİN güvenliği" ayrı bir tehdit modeli. Savunma:
  en az yetkiyle (least-privilege) araç erişimi, sandbox içinde yürütme, talimat hiyerarşisi,
  çıktının şemaya göre doğrulanması.
- EKLEME: **SBOM/CRA**. AB Cyber Resilience Act raporlaması 11 Eylül 2026'da
  başlıyor (24 saatte ENISA bildirimi; ceza 15M€/%2,5 ciro). CycloneDX ile
  Maven SBOM üretimi temel beceri.

## Hafta 7-8: AI Engineering (kitap notlarına ek)

- EKLEME: **FinOps/tokenomics**. State of FinOps 2026 raporuna göre kuruluşların %98'i
  AI harcamasını yönetiyor (2024'te %31'di); "AI token ekonomisi" artık adı konmuş bir
  disiplin. Model seçimi + caching + prompt optimizasyonu = %30-40 tasarruf.
  Maliyet, FoSA anlamında birinci sınıf mimari karakteristik.
  ⚠️ **Şerh:** "%98" bir **anket** sonucudur: ankete katılanlar zaten maliyet yönetimiyle
  ilgilenen kişilerdir (öz-seçilim). "%30-40 tasarruf" ise bir **aralık vaadi**dir; hangi
  yükte, hangi modelde, hangi başlangıç noktasından ölçüldüğü belirtilmezse kanıt değil
  beklentidir. Doğru öğretim: tasarruf kalemlerini (model seçimi, önbellekleme, prompt
  kısaltma) **mekanizma** olarak öğret, tasarruf oranını **kendi uygulamanda ölç**.
- EKLEME: **AI gözlemlenebilirliği (AI Observability)**. OpenTelemetry GenAI anlamsal
  kuralları (semantic conventions) fiili standart (LLM çağrı span'leri, ajan span'leri, MCP
  araç izleme, token metrikleri). Mart 2026'da hâlâ "deneysel" (experimental) statüdedir, yani
  alan adları değişebilir. Buna karşın satıcıdan bağımsızdır: aynı izleme verisi farklı
  gözlemlenebilirlik araçlarına taşınabilir. Eval'ler çevrimdışı kaliteyi ölçer; üretimde izleme (tracing, maliyet atfı,
  sapma) bunu tamamlar.
- Eval mühendisliği ayrı bir meslek oldu (OpenAI "Applied Evals", Microsoft
  "Evaluations Engineering" kadroları). H8 dersi "eval pipeline'ı + CI
  regresyon kapısı" pratiğine yükseltilmeli.

## Hafta 10: The Hard Parts

- 2. baskı YOK; saga/dağıtık transaction/veri sahipliği (data ownership) içeriği hâlâ temel
  başvuru (kanonik).
- **2026 okuma notu**: mikroservisleri birleştirme (konsolidasyon) yönündeki genel kanı, kitabın
  ayrıştırma düzeyi (granularity) bölümlerini yeniden çerçeveliyor. Varsayılan cevap artık "böl"
  değil **"bölme, gerekirse çıkar"**; birleştirme gerekçeleri (integrators) tarafı ağırlık kazandı.
  [DÜZELTME Tem 2026: önceki sürümde geçen "maliyet 3,75-6x" rakamı üçüncü
  tarama turunda KAYNAKSIZ/UYDURMA çıktı (Medium'daki hayali örnekten türemiş;
  Villamizar/IEEE 2016 tersini bulmuştu). Eğilim gerçek, o sayı değil.
  Sidecar-mesh düşüşü %50(2023)→%42(2024, CNCF); %18→%8 rakamı da teyitsiz.]
- Yazarların **"Architecture as Code"** (kod olarak mimari) kitabı (Eki 2026) Hard Parts'ın
  yerini almıyor, onun devamı: Hard Parts "hangi kararı vereceğim", AaC "kararı nasıl kod
  olarak zorlarım" sorusunu yanıtlar.

## Hafta 11: DDIA

- **DDIA 2e YAYIMLANDI: Mart 2026** (Kleppmann + Riccomini, O'Reilly, 670 sf).
  Programdaki soru işareti kalkabilir. Yenilikler: bulut-yerel (cloud-native) yapı ve nesne
  depolama (object storage) artık varsayılan; AI iş yüklerinin depolama/indekslemeye etkisi;
  genişletilmiş akış işleme (stream processing); 1. baskıda olmayan iki bölüm:
  **"Trade-offs in Data Systems Architecture"** (veri sistemleri mimarisinde ödünleşimler) ve
  **"Defining Nonfunctional Requirements"** (işlevsel olmayan gereksinimleri tanımlamak; FoSA
  haftalarıyla doğal köprü). Önerilen çekirdek okuma: bu iki bölüm +
  "The Trouble with Distributed Systems" (dağıtık sistemlerin sorunları). Okur tepkisi olumlu:
  "hâlâ başucu kitabı."
  ⚠️ **Şerh:** "Hâlâ başucu kitabı" bir okur yorumudur, bir ölçüm değil. Kitabın iyi olması, her
  bölümünün senin bağlamında geçerli olduğu anlamına gelmez; özellikle somut ürün ve
  sürüm örnekleri her baskıda eskir.

## Hafta 12: Liderlik ve Capstone

- Kaynağın tam kimliği: **"How AI assistance impacts the formation of coding
  skills"** ("AI yardımı kodlama becerilerinin oluşmasını nasıl etkiler"; Anthropic, 29 Oca
  2026, RCT, 52 yeni başlayan (junior) geliştirici): AI kullanan grup sınavda %17 daha düşük
  puan aldı (en büyük açık: hata ayıklama, debugging). KRİTİK AYRINTI: kavramsal soru soranlar
  %65'in üstünde, kodu tümüyle AI'a bırakanlar %40'ın altında kaldı. **Sorun AI değil, kullanım
  biçimi**. Devam raporu: Economic Index "Learning curves" (öğrenme eğrileri, 24 Mar 2026):
  6 aydan fazla deneyimi olan kullanıcılar yaklaşık %10 daha başarılı ve daha işbirlikçi.
  ⚠️ **Şerh:** Her iki çalışmayı da **model satan şirketin kendisi** (Anthropic) yayımladı.
  Yöntem güçlü (randomize kontrollü deney) ama örneklem küçük (52 kişi), tek bağlam ve
  birinci taraf. "AI kullanmak öğrenmeyi %17 düşürür" diye genelleme **çıkarılamaz**;
  çıkarılabilen şey daha dar ve zaten programın tezidir: *kavramsal soru soranlar ile işi
  tümüyle delege edenler farklı sonuç alıyor.* Sayıyı değil, ayrımı öğret.
- **Bu bulgu programın yöntemini doğruluyor**: "Değerlendir'e teslim et,
  AI'dan çözüm isteme" akışı, ölçümde başarılı çıkan kullanım biçimiyle örtüşüyor. Kata
  pratiğinin 2026 hâli: önce kendin çöz, sonra LLM'i **karşı-mimar/jüri** olarak kullan.
- nealford.com/katas hâlâ yayında; O'Reilly "AI Kata" formatı ekledi. Yeni liderlik
  okumaları: Addy Osmani "Leading Effective Engineering Teams in the Age of
  GenAI" (üretken AI çağında etkili mühendislik ekiplerini yönetmek); iSAQB SWARC4AI
  müfredatı öz-denetim listesi olarak kullanılabilir.
- EKLEME: **Mevzuatı uygulamaya dökmek (operasyonelleştirme)**. AI Act Madde 50 şeffaflık
  yükümlülükleri **2 Ağustos 2026'da yürürlükte** (Omnibus bunu ERTELEMEDİ):
  sohbet botunun AI olduğunun kullanıcıya bildirilmesi + sentetik içeriğin makinenin
  okuyabileceği biçimde işaretlenmesi. KVKK Üretken YZ Rehberi (24 Kas 2025, 64 sayfa,
  15 soru); bunu kendi uygulamanın data/hafiza/ altındaki öğrenci verisine uygula.

## Öncelikli "Programa Ekle" Listesi

1. **Ajan güvenliği → H4**: OWASP Agentic Top 10 + least-privilege + sandbox.
2. **AI observability → H8**: OTel GenAI ile LLM tracing (experimental notuyla).
3. **Context engineering → H1**: Anthropic rehberi eksenli alt modül.
4. **Mevzuat → H12/H4**: AI Act Md. 50 (2 Ağu 2026!) + KVKK rehberi.
5. **FinOps/tokenomics → H7-8**: kendi OpenRouter uygulamasında maliyet ölçümü.

Ayrıca ayrı hafta olarak değil, lab olarak: MCP sunucu yazma (Spring AI @McpTool ile
kendi uygulamanın bir portunu MCP adapter olarak dışa açmak; H9) ve Spring
Modulith sınır lab'ı (H9).

## KAPATILDI (2026-07-15)

Yukarıdaki boşlukların büyük kısmı yeni kaynak/atölye kategorisiyle kapatıldı
(yeni hafta gerektirmeden; ilgili haftalara etiketle bağlandı):

1. **Ajan/LLM güvenliği → H4** ✓ `kaynaklar/llm-uygulama-guvenligi.md`
   (etiket 4,7,8): prompt enjeksiyonu (doğrudan/dolaylı), OWASP LLM01 + Agentic
   Top 10, savunma derinliği, red-teaming. 3 diyagram + 10 öz-sınav.
2. **Evals + LLM observability → H8** ✓ `kaynaklar/evals-llm-observability.md`
   (etiket 8): Değerlendirme-Güdümlü Geliştirme (EDD), CI kapısı, OTel GenAI,
   sapma (drift) izleme, cost-per-successful-output (başarılı çıktı başına maliyet).
   3 diyagram + 10 öz-sınav.
3. **Context engineering** ✓ Prompt Atölyesi'ne 8. kategori olarak
   `07-context-muhendisligi.md` eklendi: bağlam bütçesi, bellek/durum, retrieval
   orkestrasyonu, araç tanımı, gürültü atma, bağlam hijyeni (7 alıştırma).
4. **Mevzuat/yönetişim → H8/H11** ✓ `kaynaklar/ai-yonetisim-uyum.md`
   (etiket 8,11): KVKK/GDPR, EU AI Act (2 Ağu 2026), PII maskeleme, RAG'de
   silme/embedding tuzağı, veri soyağacı. 3 diyagram + 10 öz-sınav.
5. **FinOps/tokenomics** ~ kısmen: eval ve yönetişim kaynaklarında
   "başarılı çıktı başına maliyet" çerçevesi olarak işlendi (ayrı kaynak olarak değil).
   → **2026-07-17'de tam kapatıldı**: `kaynaklar/finops-ai-tokenomics.md` (etiket 7,8).

**MCP sunucu yazma lab'ı → KAPANDI (2026-07-16).** Lab'ın ötesine geçildi: uygulamanın MCP
adapter'ı yazıldı (8 araç / 46 kaynak / 5 prompt), OAuth 2.1 arkasında `ai.velzon.tr/mcp`
adresinde **canlıya alındı** ve token modeli üretimde ölçüldü (sunucu sürecinde LLM anahtarı
yok, çağrı sıfır). Bkz. ADR-0007 Faz 2b + `kaynaklar/mcp-adapter.md`. *Sonraki not:* bu canlı
sunucu 2026-10-21'de kapatıldı; MCP kodu duruyor ve `mcp-http` profiliyle yalnız yerelde
çalıştırılabilir. Uygulamanın bugünkü canlı yüzü https://sokratai.tech sitesidir (ADR-0017, ADR-0018).

Açık kalan (lab): **Spring Modulith sınır lab'ı**.

## İKİNCİ TUR: kanıtlı boşluk analizi (2026-07-17)

Yeni bir araştırma turu (26 kaynak → 124 iddia → 25'i çekişmeli doğrulamadan geçirildi;
18 onaylandı, 7 çürütüldü) müfredatın en büyük boşluğunu **teknoloji değil, ÖLÇÜM** olarak
buldu. Kapatıldı:

6. **AI verimlilik ölçümü / kanıt okuma** ✓ `kaynaklar/ai-verimlilik-olcumu.md`
   (etiket 4,7,8,12): algı–gerçeklik uçurumu (METR RCT), üç ayrı ölçüm aracı (RCT / anket /
   telemetri), DORA'nın "Great Amplifier" (büyük güçlendirici) tezi: *AI bir ekibi düzeltmez,
   mevcut durumu büyütür*. Bu tez **Hafta 4'ün (guardrail) ampirik gerekçesidir**.
   Benimseme–güven makası (Stack Overflow).
7. **A2A sınırı** ✓ `kaynaklar/mcp-adapter.md`'ye bölüm eklendi: A2A, MCP'nin rakibi değil,
   **üst katmanı** (A2A kurumlar arası ajan koordinasyonu, MCP iç araç erişimi; ikisi de Linux
   Foundation'da). Ayrı hafta AÇILMADI. Gerekçe: v1.0 kararlı ama kullanımın çoğu hâlâ PoC.

**EKLENMEDİ (bilinçli):** *"Vibe coding"* (kodu incelemeden AI'a sezgiyle yazdırmak), bir teknik
olarak. Stack Overflow 2025 anketinde bu soruyu yanıtlayan 26.564 kişinin
**%77,5'i profesyonel işinde kullanmıyor** (%17,2 kullanıyor); terimin mucidi bile Şubat 2026'da onu "agentic
engineering" (ajan mühendisliği) diye yeniden adlandırdı. Hafta 1 (harness) +
Hafta 3 (spec-driven) bu alanı zaten **doğru kavramlarla** kaplıyor; başlık eklemek terminolojik
gerileme olurdu. Programdaki tek yeri, kaçınılması gereken bir kalıp (anti-pattern) alıştırması.

## ÜÇÜNCÜ TUR: kalan 5 konu karara bağlandı (2026-07-17)

İkinci turda kanıtsız kalan beş konu ayrı bir araştırma turuyla hedeflendi (25 iddia doğrulandı:
14 onay / 10 çürütme / 1 belirsiz). Sonuç olarak **üçü girdi, ikisi kalıcı olarak kapandı:**

8. **Ajan sandbox izolasyonu** ✓ `kaynaklar/ajan-sandbox-izolasyonu.md` (etiket 4,9).
   Bu bir tekrar değil, **ayrı bir katman**: `llm-uygulama-guvenligi` *önlemeyi*, bu kaynak ise
   *sınırlamayı* (containment) anlatır; yani enjeksiyon başarılı olduğunda hasarın yayılabileceği
   alanı (blast radius) daraltmayı. Kanıt güçlü: **ICML 2026'da sözlü sunuma (Oral) seçilen** bir
   çalışma (Oxford + UK AI Security Institute) en ileri modellerin (frontier models) yanlış
   yapılandırılmış konteynerden kaçış oranını ölçtü (zorluk 1-2'de oran 1.00'e kadar çıkıyor;
   zorluk 4-5'te **tüm modeller 0.00**; sertleştirilmiş sandbox hiç aşılamadı;
   **GPT-5.2 geriledi** → "ölçekle risk monoton artar" desteklenmiyor). İkinci kanıt, Google'ın kendisinin
   ödül (bounty) ödeyip yamaladığı **Antigravity RCE** (uzaktan kod çalıştırma) vakası. Asıl ders:
   *bütün yürütme yollarının içinden geçmediği bir kontrol düzlemi, gerçek bir kontrol düzlemi
   değildir*. Bu, projenin ArchUnit kurgusunun birebir benzeridir.
9. **SLM ekseni** ✓ (sınırlı): `finops-ai-tokenomics.md` §5.1'e eklendi. Üretim kanıtı **Apple**
   (cihaz üstünde çalışan ~3B parametreli model, iki nesil). Ama **NVIDIA'nın "SLM'ler
   geleceğidir" tezi kanıt olarak ÖĞRETİLMİYOR**: bu, şirketin kendi ilan ettiği bir
   *"value statement"* (değer beyanı); dolaşan %60/%40/%70 rakamları **ölçülmemiş**, masa başı
   tahminler; satıcının kendi savunma sayfasında bile adı verilen bir üretim vakası yok.
   Bölümün asıl dersi bu ayrım: **satıcı tezi ≠ bağımsız üretim kanıtı.**
10. **Talimat şişmesi → bağlam çürümesi** ✓ `harness-engineering-openai.md` §4.1'e eklendi.
    Araç (Spec Kit) **eklenmedi**: Radar Vol.34'te **Assess** halkasında ("keşfetmeye değer, henüz üretim için değil"),
    ekipler *deniyor*, üretimde kullanmıyor; üstelik araç hızla eskiyor (v0.10.0 bir grup komut
    satırı bayrağını (flag) kaldırdı, öğreticiler bozuldu). Araç yerine
    **araçtan bağımsız bir arıza modeli** öğretiliyor. Canlı vaka olarak
    projenin kendi `AGENTS.md`'sinin bir günde 29→85 satıra çıkması kullanıldı.

### KALICI OLARAK KAPANDI: iki turda kanıt üretilemedi
- **AI ile eski sistem (legacy) modernizasyonu:** iki bağımsız turda doğrulamayı geçen
  **tek bir iddia yok**.
  Satıcı ürünleri (IBM watsonx Code Assistant for Z, Amazon Q Code Transformation) *mevcut* olabilir;
  ama bağımsız vaka ya da ölçülmüş sonuç toplanamadı. **Satıcı ürün sayfasının varlığı, kurumsal
  pratik kanıtı değildir.** Bir konu 12 haftalık müfredata ancak doğrulanmış kanıtla girer; iki
  turda da kanıt bulunamaması ("cevapsız" kalması) konuyu eklemek için gerekçe olamaz.
- **Spring Modulith sınır lab'ı:** sürümü, benimsenme durumu ve "ArchUnit'ten farkı": hiçbiri
  doğrulanamadı.
  Ayrıca gerekçe kanıttan bağımsız olarak da geçerli: proje **zaten** ArchUnit ile katman sınırlarını
  derleme zamanında zorluyor (`MimariTestleri`, `./mvnw verify` yeşil) → *"sınır zorlama mimari bir
  karardır ve otomatik test edilebilir"* hedefine **ulaşılmış**. Hafta 9'da hexagonal mimari zaten
  canlı olarak uygulandı; dikey sınır kavramı başka yoldan verilmiş. Kanıtsız yeni bağımlılık,
  projenin **ASK FIRST** (önce sor) kuralına da aykırı. **Kavramı ArchUnit'le öğretmek yeter.**
- **Durable execution (kesintiye dayanıklı yürütme) / ajan determinizmi:** kanıt yok → sandbox
  kaynağına dahil **edilmedi**
  (o kaynağın kararı yalnızca izolasyon boyutunu kapsar).

> Bu iki kapanış bir başarısızlık değil, **yöntemin çalıştığının kanıtı**: iki tur araştırma sonunda
> "eklemiyoruz" demek, zorlayarak doğrulanmamış içerik üretmekten iyidir.

## DÖRDÜNCÜ TUR: dünya programlarıyla kıyas (benchmark, 2026-07-19)

Önceki turlar **konu** soruyordu; bu tur **biçim** sordu: iSAQB CPSA, SEI/CMU (ATAM/QAW),
O'Reilly/Neal Ford kataları, üniversite dersleri, AI mühendisliği kursları, staff mühendisliği
(staff-eng) kaynakları. 22 iddia doğrulandı (21'i üç doğrulayıcının oybirliğiyle, 3-0). Bulgu:
eksik olan konu değil, **ölçme ve pratik biçimi**. Dünyadaki programlar mimarı *"kurduğunu
savunabiliyor ve başkasınınkini sorgulayabiliyor mu?"* sorusuyla ölçüyor.

11. **Mimari değerlendirme + kata serisi + savunma** ✓ `kaynaklar/mimari-degerlendirme-kata.md`
    (etiket 5,6,10,12). Üç doğrulanmış boşluk tek kaynakta:
    - **ATAM/QAW yoktu:** iSAQB'de değerlendirme ayrı modül (ARCEVAL); SEI'nin 9 adımlı ATAM'ı
      ölçülebilir çıktılar (artifact) üretir (fayda ağacı, riskler/risk-olmayanlar, duyarlılık,
      ödünleşim (trade-off) noktaları); QAW aynı işi mimari YOKKEN yapar. Biz kurmayı
      öğretiyorduk, sorgulamayı değil.
    - **Kata tek seferlikti:** O'Reilly katası çok turlu, jüri geri bildirimli ve izleyici oylamalı
      bir SERİ (2020-2025'te 9'dan fazla döngü). Çözüm: 2 haftada bir mini-kata bankası + capstone.
    - **Savunma (design defense) yoktu:** CPSA-A sınavı ~40 saatlik yazılı vaka + iki bağımsız
      denetçi + telefonda **sözlü savunma**; kata'nın 2. fazı "düşüncendeki delikleri arayan"
      çapraz sorgu. Tek kişilik uyarlama: AI öğretmen müşteri (Moderatör) ve sorgucu rolü oynar.
- **DÜRÜST SINIRLAR (metne gömüldü):** (a) Kata'nın çekirdeği yabancılarla **takım tartışmasıdır**;
  bu kısım tek kişilik biçime **UYARLANAMAZ**. AI sorgusu üç insanın üç sezgisinin yerini tutmaz.
  (b) "AI, ATAM'ı kısmen otomatikleştirir" genellemesi doğrulamada **çürütüldü** (0-3); ayakta
  kalan yalnız dar kapsamlı bir ilk çalışmanın bulgusu (LLM, öğrenci senaryolarındaki riskleri çoğu
  durumda daha isabetli buldu). Yani AI bir "idman eşi" (sparring partner), jüri değil.
  (c) **Aralıklı tekrar / geri getirme** iddiaları benchmark turunda oturum limitine takılmıştı;
  **sonradan ayrıca doğrulandı** (2026-07-19): Dunlosky'nin çığır açan derlemesinde 10 çalışma
  tekniğinden yalnız İKİSİ "yüksek fayda" sınıfında: geri getirme pratiği (retrieval practice /
  testing effect) ve aralıklı tekrar. Roediger & Karpicke: 1 hafta sonra test grubunda %61,
  yeniden okuma grubunda %40; etki, aralık uzadıkça BÜYÜR. Mekanizma eklendi:
  H6 Gün 5'e program ortası karma tekrar quizi (hafta 1-5), H12 Gün 5'e karma final tekrarı
  (hafta 1-11). Mevcut AI quiz üretimi yeterli olduğu için kod gerekmedi.
- **EKLENMEDİ:** ayrı "dokümantasyon" kaynağı (iSAQB ADOC ayrı modül tutuyor ama bizde ADR+C4
  zaten var; eksik olan doküman *yazmak* değil, dokümanın **notlandırılan teslimat** olmasıydı;
  bu da capstone protokolüne girdi). Sertifika hedefi (CPSA-F/A) yalnız bir dipnot, çünkü program
  kişisel.

*Teyit edilmemiş noktalar: CNCF %42 konsolidasyon rakamı (ikincil kaynak), OpenAI
prompt-injection rehberi birincil URL'si, %280 ilan büyümesi.*
*Tek kaynak/satıcı uyarısı: GitClear'ın "blok tekrarı %81 arttı" ölçümü doğrulamayı geçti ama
tek kaynak ve satıcı çıkarı var; bu yüzden "sektör gerçeği" diye değil, "GitClear'ın ölçümü"
diye atfedilmeli. Aynı raporun diğer metrikleri (moved code: taşınan kod, function connectivity:
fonksiyon bağlantısallığı, legacy update: eski kodun güncellenmesi) çürütüldü.*

## Şerh özeti: bu rapordaki rakamların kaynak sınıfı

Derste bir rakam kullanacaksan önce buraya bak. Sınıflar:

| İddia | Kaynak sınıfı | Nasıl kullanılmalı |
|---|---|---|
| Harness fark yaratır (%52,8 → %66,5) | Satıcı (LangChain), tek kıyaslama | Yönü öğret, rakamı atıfla ver |
| AI kodunun %45'i açıklı | Satıcı (Veracode, tarama ürünü satıyor) | "Kod güvenli varsayılamaz" dersi; yüzdeye yaslanma |
| FinOps %98 / %30-40 tasarruf | Anket + vaat aralığı | Mekanizmayı öğret, oranı kendi uygulamanda ölç |
| Öğrenme üzerine RCT (%17 düşüş) | Birinci taraf (Anthropic), n=52 | Sayıyı değil, "kullanım biçimi" ayrımını öğret |
| NVIDIA "SLM'ler geleceğidir" | Satıcı değer beyanı, ölçüm yok | Öğretilmiyor; yalnızca *satıcı tezi ≠ kanıt* örneği |
| GitClear "blok tekrarı %81" | Tek kaynak + satıcı çıkarı | "GitClear'ın ölçümü" diye atfet |
| ATAM'ı LLM değerlendirir | Tek ders, tek model, ilk çalışma | Genelleme yok; "idman eşi" düzeyinde |
| Model/sürüm adları (GPT-5.5, Sonnet 5…) | Satıcı duyurusu, hızla eskir | Deseni öğret, adı kullanmadan önce doğrula |
| ATAM/QAW adımları, ADR, C4 | Kurumsal/akademik yöntem belgesi | Doğrudan öğretilebilir |
| AI Act Md. 50, CRA tarihleri | Mevzuat metni | Doğrudan öğretilebilir; tarihi teyit et |

**Kural:** Satıcının kendi ürününü doğrulayan ölçümü, bağımsız kanıt değildir. Bu rapor bunu
maddelerinde birkaç kez uyguladı ("maliyet 3,75-6x" rakamının uydurma çıkması, NVIDIA SLM
tezinin öğretilmemesi, legacy modernizasyon konusunun kanıtsız diye kapanması). Okurken aynı
süzgeci sen de uygula.
