#!/usr/bin/env bash
# Bir commit'in AĞACINI ve ÜSTVERİSİNİ (yazar + commit eden adı/e-postası + mesaj) sızıntıya karşı tarar (genel IPv4/IPv6,
# root@ SSH hedefi, kişisel e-posta, özel anahtar, API anahtarı, şifre ataması, sunucu adı; git dışı kalması
# gereken yollar). Repo herkese açık (denizaydogdu/sokratai.tech).
#
# Kullanım:  bash tools/sizinti-tara.sh [<commit>|<etiket>]     (varsayılan HEAD)
#            Açıklamalı etiket verilirse etiket zincirinin metni (etiket adı, tagger, mesaj) üstveri desenleriyle
#            taranır, sonra işaret ettiği commit taranır; etiket commit dışı bir nesneye (blob/ağaç) işaret ediyorsa ENGEL.
# Çıkış:     0 temiz · 1 bulgu (bulgular stderr'e, her desen için en çok 10 satır, satır başına 200 karakter)
#            · 2 tarama YAPILAMADI (commit çözülemedi, git grep -P çalışmıyor ya da bir desende git grep hatası)
#            — kapalı-güvenli: tarama yapılamazsa push engellenir.
# Push öncesi otomatik çalışır: tools/git-kancalari/pre-push (kilit: git config core.hooksPath tools/git-kancalari).
# Yalnız COMMIT EDİLMİŞ hâl taranır; çalışma ağacındaki değişiklikler taranmaz. İkili dosyalar (-I) içerik
# taramasına giremez → tools/sizinti-ikili-izinli.txt dışındaki her ikili dosya ENGEL; .gitattributes'ta bir kalıbı
# ikili sayan (`binary`, `-diff`) satır da, kalıp izin listesinde değilse ENGEL (içeriği taramadan saklardı).
# Yollar core.quotePath=false + NUL ayraçla okunur: Türkçe (ASCII dışı) adlar tırnaklanıp denetimi atlatamaz.
# Senaryo testi: tools/sizinti-tara-test.sh (JUnit: ToolsSizintiTaramaTest).
set -euo pipefail

KAYNAK="${1:-HEAD}"

cd "$(dirname "$0")/.."

yapilamadi() {  # $1 neden
  echo "HATA: tarama yapılamadı ($1) — push engellendi." >&2
  exit 2
}

sizinti=0

bildir() {  # $1 başlık, $2 bulgu metni
  echo "  ENGEL — $1:" >&2
  # here-string: büyük bulguda üretici süreç yok → SIGPIPE taramayı yarıda kesmez
  head -10 <<<"$2" | cut -c1-200 >&2
  sizinti=1
}

gecici=$(mktemp -d)
trap 'rm -rf "$gecici"' EXIT

# Etiket zinciri: her açıklamalı etiketin metni ustveri-etiket-N.txt'ye yazılır (aşağıda taranır); en sondaki
# nesne commit olmalı — blob/ağaç etiketi içerik taramasından kaçan veri taşırdı.
nesne=$(git rev-parse -q --verify "$KAYNAK" 2>/dev/null) || yapilamadi "nesne çözülemedi: $KAYNAK"
etiket_no=0
tur=$(git cat-file -t "$nesne") || yapilamadi "nesne türü okunamadı: $KAYNAK"
while [ "$tur" = tag ]; do
  etiket_no=$((etiket_no + 1))
  [ "$etiket_no" -le 20 ] || yapilamadi "etiket zinciri çok uzun: $KAYNAK"
  git cat-file tag "$nesne" >"$gecici/ustveri-etiket-$etiket_no.txt" || yapilamadi "etiket okunamadı"
  nesne=$(sed -n '1s/^object //p' "$gecici/ustveri-etiket-$etiket_no.txt")
  [ -n "$nesne" ] || yapilamadi "etiketin hedefi okunamadı"
  tur=$(git cat-file -t "$nesne") || yapilamadi "etiket hedefinin türü okunamadı"
done
if [ "$tur" != commit ]; then
  echo "  ENGEL — commit olmayan nesne ($tur) push/tarama hedefi: içeriği taranamaz." >&2
  echo "HATA: $KAYNAK sızıntı taramasından geçmedi." >&2
  exit 1
fi
AD="$KAYNAK"
KAYNAK="$nesne"

# PCRE öz-sınaması: git PCRE'siz derlenmişse (ya da git grep bozuksa) her desen "eşleşme yok" (1) sanılmasın.
# Her yol dışlanır → beklenen tek sonuç 1 (eşleşme yok); başka her şey (128 vb.) taramayı geçersiz kılar.
oz=0
git grep -q -P -e 'x' "$KAYNAK" -- ':(exclude)*' >/dev/null 2>&1 || oz=$?
[ "$oz" -eq 1 ] || yapilamadi "git grep -P öz-sınaması çıkış $oz verdi (PCRE desteği?)"

# Ağaç içeriği (git grep; 0 = bulgu, 1 = temiz, diğer = tarama yapılamadı)
tara() {  # $1 açıklama, $2 PCRE
  local bulgu rc=0
  bulgu=$(git grep -n -I -P -e "$2" "$KAYNAK" -- .) || rc=$?
  case "$rc" in
    0) bildir "$1" "$bulgu" ;;
    1) ;;
    *) yapilamadi "'$1' deseninde git grep çıkış $rc" ;;
  esac
}

# --- Desenler (ağaç ve commit üstverisi için ORTAK) ---
# 127.0.0.1, 0.0.0.0 ve özel ağ blokları (10/8, 172.16/12, 192.168/16) ders örneği sayılır; gerisi gerçek sunucu.
D_IPV4='(?<![\d.])(?!127\.0\.0\.1(?![\d.]))(?!0\.0\.0\.0(?![\d.]))(?!10\.)(?!192\.168\.)(?!172\.(1[6-9]|2\d|3[01])\.)(\d{1,3}\.){3}\d{1,3}(?![\d.])'
# Genel IPv6 = küresel tek yayın 2000::/3 (ilk grup 2xxx/3xxx, en az iki ':'); belge bloğu 2001:db8::/32 ve
# bloğun kendi gösterimi (2000:: — adres değil, ağ öneki) hariç. ::1, link-local fe80::/10 ve ULA fc00::/7 bu
# bloğun dışında kaldığından zaten eşleşmez.
D_IPV6='(?i)(?<![\w:.])(?!2001:0?db8:)(?!2000::(?![0-9a-f]))[23][0-9a-f]{3}:(?:[0-9a-f]{0,4}:){1,6}(?:[0-9a-f]{1,4})?(?![\w:.])'
D_ROOT='root@[A-Za-z0-9.-]+'
D_ANAHTAR_OZEL='-----BEGIN [A-Z ]*PRIVATE KEY'
D_API='(sk-or-v1-[A-Za-z0-9]{20,}|sk-ant-[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{30,}|AKIA[0-9A-Z]{16})'
# Ortam/env biçiminde şifre ataması (ANAHTAR=değer, '=' çevresinde boşluk yok). Eşleşmez: <yer-tutucu>, boş değer,
# ${...}, "..." ve XSRF-TOKEN gibi tireli adlar (çerez adı); Java `X = "..."` sabitleri (boşluklu) yanlış pozitif vermesin diye.
D_SIFRE='(?<![\w-])(?:[A-Z0-9_]*_)?(?:PASSWORD|SECRET|TOKEN|API_KEY)=["\x27]?(?!\.\.\.)[^<\s$"\x27\x60]'
# Spring/YAML/kabuk biçimleri (satır başında anahtar; büyük-küçük harf duyarsız): spring.datasource.password=x,
# YAML `  password: x`, PGPASSWORD=x, export DB_PASS=x. Eşleşmez: ${...}, <yer-tutucu>, ..., boş değer; satır
# başına bağlı olduğundan Java/JS kodundaki `String password = ...` gibi tanımlar da eşleşmez.
D_SIFRE_AYAR='(?i)^\s*(export\s+)?[\w.-]*(password|passwd|_pass|secret|api[_-]?key)\s*[=:]\s*["\x27]?(?!\$\{|<|\.\.\.|["\x27]?\s*$)[^\s"\x27\x60]'

tara "genel IPv4 adresi"   "$D_IPV4"
tara "genel IPv6 adresi"   "$D_IPV6"
tara "root SSH hedefi"     "$D_ROOT"
tara "e-posta adresi"      '[A-Za-z0-9._%+-]+@(gmail|hotmail|outlook|yahoo|icloud)\.com'
tara "özel anahtar"        "$D_ANAHTAR_OZEL"
tara "API anahtarı"        "$D_API"
tara "şifre ataması"       "$D_SIFRE"
tara "şifre ataması (ayar dosyası biçimi)" "$D_SIFRE_AYAR"
tara "sunucu adı (Hostinger)" 'hstgr\.cloud'

# Git dışı kalması gereken yollar (ls-tree hatası da tarama hatasıdır — `|| true` ile yutulmaz)
# core.quotePath=false + -z: ASCII dışı adlar "thoughts/oturum-\304\261.md" gibi tırnaklanıp ^thoughts/ denetimini
# atlatamaz (NUL → satır sonu; adında satır sonu olan yol fazladan satır üretir → en kötü yanlış pozitif).
yollar=$(git -c core.quotePath=false ls-tree -r -z --name-only "$KAYNAK" | tr '\0' '\n'; exit "${PIPESTATUS[0]}") \
  || yapilamadi "git ls-tree başarısız"
yasak=$(grep -E '^(thoughts/|data/|tools/deploy\.env$)' <<<"$yollar" || true)
# .env, .env.<her şey>, <ad>.env — şablonlar (*.ornek) hariç
envler=$(grep -E '(^|/)\.env(\..+)?$|(^|/)[^/]*\.env$' <<<"$yollar" | grep -vE '\.ornek$' || true)
yasak=$(printf '%s\n%s\n' "$yasak" "$envler" | sed '/^$/d')
[ -z "$yasak" ] || bildir "git dışı kalması gereken dosyalar" "$yasak"

# Commit üstverisi: yazar + commit eden adı/e-postası + mesaj ve (varsa) etiket metinleri (içerik desenleri;
# kişisel e-posta deseni HARİÇ — yazar e-postası kullanıcı kararıdır). Aynı PCRE motoru: git grep --no-index
# geçici dizindeki ustveri*.txt dosyalarında.
git log -1 --format='%an%n%ae%n%cn%n%ce%n%B' "$KAYNAK" >"$gecici/ustveri.txt" || yapilamadi "git log başarısız"
tara_ustveri() {  # $1 açıklama, $2 PCRE
  local bulgu rc=0
  bulgu=$(git -C "$gecici" grep --no-index -n -I -P -e "$2" -- 'ustveri*.txt') || rc=$?
  case "$rc" in
    0) bildir "commit/etiket üstverisinde $1" "$bulgu" ;;
    1) ;;
    *) yapilamadi "commit üstverisi '$1' deseninde git grep çıkış $rc" ;;
  esac
}
tara_ustveri "genel IPv4 adresi" "$D_IPV4"
tara_ustveri "genel IPv6 adresi" "$D_IPV6"
tara_ustveri "root SSH hedefi"   "$D_ROOT"
tara_ustveri "özel anahtar"      "$D_ANAHTAR_OZEL"
tara_ustveri "API anahtarı"      "$D_API"

# İkili dosyalar (-I ile içerik taramasına girmedi): izin listesi dışındaki her biri ENGEL. Liste repoda, satır başına
# tam yol (# yorum, boş satır serbest). Yerel .git/info/attributes ya da küresel öznitelik dosyası bir metni ikili
# saydırırsa o dosya da burada görünür → ENGEL (kapalı-güvenli).
izin_dosyasi="tools/sizinti-ikili-izinli.txt"
[ -f "$izin_dosyasi" ] || yapilamadi "ikili izin listesi yok: $izin_dosyasi"
izinliler=$(sed -e 's/[[:space:]]*#.*$//' -e 's/[[:space:]]*$//' -e '/^$/d' "$izin_dosyasi")
izinli_mi() {  # $1 yol
  [ -n "$izinliler" ] && grep -qxF -e "$1" <<<"$izinliler"
}
bos_agac=$(git hash-object -t tree /dev/null)
ikililer=$(git -c core.quotePath=false diff --no-ext-diff --no-textconv --numstat -z "$bos_agac" "$KAYNAK" \
  | tr '\0' '\n' | awk -F'\t' '$1 == "-" { sub(/^[^\t]*\t[^\t]*\t/, ""); print }'; exit "${PIPESTATUS[0]}") \
  || yapilamadi "ikili dosya listesi alınamadı"
izinsiz=""
while IFS= read -r yol; do
  [ -n "$yol" ] || continue
  izinli_mi "$yol" || izinsiz="$izinsiz$yol"$'\n'
done <<<"$ikililer"
[ -z "$izinsiz" ] || bildir "izin listesi ($izin_dosyasi) dışında ikili dosya (içeriği taranamaz)" "${izinsiz%$'\n'}"

# .gitattributes: `binary` ya da `-diff` bir metni git grep -I için ikili yapar → kalıp izin listesinde değilse ENGEL
ozniteliksiz=""
while IFS= read -r oz_yol; do
  [ -n "$oz_yol" ] || continue
  icerik=$(git cat-file blob "$KAYNAK:$oz_yol") || yapilamadi ".gitattributes okunamadı: $oz_yol"
  while IFS= read -r satir; do
    case "$satir" in \#*) continue ;; esac
    read -r -a alanlar <<<"$satir"   # read ile böl: `*.md` gibi kalıplar dosya adına genişlemez
    [ "${#alanlar[@]}" -ge 2 ] || continue
    kalip="${alanlar[0]}"
    ikili_mi=0
    for oz in "${alanlar[@]:1}"; do
      case "$oz" in binary|-diff) ikili_mi=1 ;; esac
    done
    [ "$ikili_mi" -eq 1 ] || continue
    izinli_mi "$kalip" || ozniteliksiz="$ozniteliksiz$oz_yol: $satir"$'\n'
  done <<<"$icerik"
done < <(grep -E '(^|/)\.gitattributes$' <<<"$yollar" || true)
[ -z "$ozniteliksiz" ] || bildir ".gitattributes içeriği taramadan saklayan satır (binary/-diff)" "${ozniteliksiz%$'\n'}"

if [ "$sizinti" -ne 0 ]; then
  echo "HATA: $AD sızıntı taramasından geçmedi; önce yukarıdakileri temizle." >&2
  exit 1
fi
echo "Sızıntı taraması temiz ($AD)."
