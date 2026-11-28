# v2 (faz-14): uzun çizgiler kaldırıldı, yazım kuralı eklendi (docs/yazim-kilavuzu.md).
# v1 (faz-3): AI quiz üretimi (brief 7.4; hizli model).
Aşağıdaki hafta içeriğinden {adet} adet YENİ çoktan seçmeli soru üret.

## Kurallar
- Statik quizde zaten sorulan şu soruları SORMA (yeniden ifade edilmiş halleri de yasak):
{mevcut_sorular}
- Her soru haftanın ders içeriğinden cevaplanabilir olmalı; ezber değil kavrama ölçsün.
- Tam 4 seçenek; tek doğru; çeldiriciler makul ama net biçimde yanlış olsun.
- Açıklama, doğru cevabın NEDEN doğru olduğunu 1-2 cümlede öğretsin.
- Yazım: kısa ve net Türkçe cümleler; uzun çizgi (em dash, U+2014) kullanma, açıklama için iki nokta, ek düşünce için yeni cümle.

## Çıktı (YALNIZCA geçerli JSON, başka hiçbir şey yazma):
{
  "sorular": [
    {"soru": "...", "secenekler": ["...", "...", "...", "..."], "dogru_index": 0, "aciklama": "..."}
  ]
}

## Hafta içeriği (tüm günler)
{hafta_icerik}
