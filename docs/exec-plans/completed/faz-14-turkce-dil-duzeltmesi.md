# Faz 14: Türkçe dil düzeltmesi (anlam kaybı, çeviri kokusu, uzun çizgi)

- **Durum:** bitti (2026-11-28)
- **İstek (kullanıcı):** tüm yazıları okuyup anlam kaybı olan ve Türkçede anlaşılmayan cümleleri düzelt;
  gerekirse İngilizce teknik terim kullan; cümle içindeki anlamsız uzun çizgileri (—) kaldır. Adım adım, fazlara bölerek.
- **Kural kaynağı:** `docs/yazim-kilavuzu.md` (bağlayıcı)

## Kapsam ve ölçüm (başlangıç, HEAD b6ac8f9)

| Alan | Dosya | Kelime | Uzun çizgi |
|---|---|---|---|
| Dersler (`content/hafta-01..12`, `program.md`) | 73 | ~39.900 | 396 |
| Kaynak metinleri (`content/kaynaklar`) | 38 | ~145.300 | 1.849 |
| Prompt atölyesi (`content/prompt-atolyesi`) | 8 | ~13.500 | 182 |
| Arayüz (index.html, app.js metinleri, hata sayfaları), herkese açık sayfa metinleri (Java), AI prompt'ları (`prompts/`) | - | - | ~60 görünür |

Kapsam dışı: `content/mulakat/` (310 dosya, ~1 milyon kelime; sitede kapalı). Ayrı bir faz olarak
istenebilir. Kod yorumları ve geliştirici belgeleri de kapsam dışı (okuyucuya görünmüyor).

## Fazlar

- [x] **14.0 Hazırlık:** ölçüm, yazım kılavuzu, terim sözlüğü, yapı denetimi betiği (HEAD ile karşılaştırır:
  kod/SVG işaretlemesi, bağlantılar, başlık yapısı, quiz doğru şıkkı, görev sayısı ve türü, tablo/liste yapısı
  değişmemeli; uzun çizgi kalmamalı), örnek dosya: `hafta-01/gun-1.md`.
- [x] **14.1 Dersler:** 73 dosya; ~400 uzun çizgi, ~350 anlam düzeltmesi; 5 olgu/gönderme hatası düzeltildi.
- [x] **14.2 Kaynak metinleri:** 38 dosya; ~1.850 uzun çizgi, ~750 anlam düzeltmesi; Türkçesi olmayan İngilizce alıntılara çeviri; sayma/gönderme hataları (üç→dört, üç→beş, Assess halkası tanımı) düzeltildi; terim birliği (guardrail, ödünleşim, altın küme), `yazar` alanı tek biçim.
- [x] **14.3 Atölye, arayüz, herkese açık sayfa metinleri, AI prompt'ları:** AI öğretmenin kendi çıktısında da
  uzun çizgi kullanmaması için sistem prompt'una kural.
- [x] **14.4 Denetim:** yapı denetimi 119 dosyada temiz (3 bilinçli istisna: iki SVG aria-label metni, bir atölye başlığı), `IcerikYazimKurallariTest` eklendi, `./mvnw clean verify` 530 test yeşil. Codex'in bulduğu iki eski sorun da kapandı (QuizUretimService null liste, KitapService ham AI yanıtını log'lama).
- [ ] **14.5 Yayın:** commit, push, Dokploy'da Redeploy (kullanıcı onayıyla).

## Notlar

- `content/` artık tek doğruluk kaynağı: `mimar-programi_1.html`'den yeniden üretmek (`tools/html-to-content.mjs`)
  bu düzeltmeleri SİLER. Dönüştürücü yalnız ilk üretim içindi.
- Görev türü görev cümlesindeki kelimeden çıkar (`app.js` `gorevTipiCikar`), ilerleme görev sırasına göre
  tutulur: görev cümleleri düzeltilirken tür ve sıra korunur (denetim betiği kontrol eder).

## Açık kalanlar da kapandı (kullanıcı onayı 2026-11-28, mülakat hariç)

- **Eski uygulama bilgileri güncellendi:** AGENTS.md/ADR sayıları (ölçme komutuyla birlikte), iki mod (yerel 127.0.0.1 + H2 /
  site: girişsiz, PostgreSQL, Docker + Dokploy + Traefik, ziyaretçi kendi anahtarıyla öder), MCP canlı sunucularının
  2026-10-21'de kapandığı, pre-push sızıntı taraması, `HizSiniri`; Hafta 6 Gün 4 mermaid diyagramı güncel mimariye göre.
- **Atölye `ai:true/false` bayrağı uygulanıyor:** sunucu `ai:false` alıştırmada model çağırmadan 400 döner (mevcut
  `IllegalArgumentException` → `{hata, detay}` yolu; 422'ye eşlenen tek istisna ilerleme koduna özel); arayüz düğmeyi yalnız
  her bölümün son "birleştir" alıştırmasında gösterir; giriş metni doğru bilgiyle yazıldı. +6 test (2'si Playwright).
- **Belirsiz cümleler çözüldü:** ~20 yer (hata sınırı, MRTR açılımı, diyagram numaraları, ters mantık, bölüm göndermeleri,
  "Serpiştirme" yazar etiketi → "AI Öğretmen ek okuma metni: … konusu").
- **Codex bulgusu:** boş/süzülmüş completion'da kullanım satırı artık hata fırlatılmadan önce yazılıyor (+1 test).
- **Son durum:** `./mvnw clean verify` 537 test, 0 hata, 5 atlanan (16 Playwright).
- **Kapsam dışı (kullanıcı kararı):** `content/mulakat/`.
