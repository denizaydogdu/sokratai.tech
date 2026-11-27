#!/usr/bin/env bash
# Bir commit'in AĞACINI, YOL ADLARINI, SEMBOLİK BAĞ HEDEFLERİNİ ve ÜSTVERİSİNİ (yazar + commit eden adı/e-postası +
# mesaj) sızıntıya karşı tarar (genel IPv4/IPv6, root@ SSH hedefi, kişisel e-posta, özel anahtar, API anahtarı, şifre
# ataması, sunucu adı; git dışı kalması gereken yollar). Repo herkese açık (denizaydogdu/sokratai.tech).
#
# Kullanım:  bash tools/sizinti-tara.sh [--ikili-izin <dosya>] [<commit>|<etiket>]     (varsayılan HEAD)
#            Açıklamalı etiket verilirse etiket zincirinin metni (etiket adı, tagger, mesaj) metin desenleriyle
#            taranır, sonra işaret ettiği commit taranır; etiket commit dışı bir nesneye (blob/ağaç) işaret ediyorsa ENGEL.
#            --ikili-izin <dosya>: ikili izin listesi bu dosyadan okunur (kanca YAYINLANMIŞ sürümü verir). Verilmezse
#            liste TARANAN COMMIT'teki tools/sizinti-ikili-izinli.txt'dir (commit'te yoksa boş) — çalışma ağacı ASLA.
#            Girdi biçimi `<yol> <blob-sha>`: ikili yalnız o yolda VE o blob'la izinli (içerik değişirse yeniden onay);
#            sha'sız (eski biçim) satır hiçbir ikiliye izin vermez (biçim göçünü kanca yapar).
#            bash tools/sizinti-tara.sh --ref-adi <ad>
#            Yalnız bir ref adını (dal/etiket; kanca yerel ve uzak adı verir) içerik desenlerinin hepsiyle tarar.
# Çıkış:     0 temiz · 1 bulgu (bulgular stderr'e, her desen için en çok 10 satır, satır başına 200 karakter)
#            · 2 tarama YAPILAMADI (commit çözülemedi, git grep -P çalışmıyor ya da bir desende git grep hatası)
#            — kapalı-güvenli: tarama yapılamazsa push engellenir.
# Push öncesi otomatik çalışır: tools/git-kancalari/pre-push (kilit: git config core.hooksPath tools/git-kancalari).
# Yalnız COMMIT EDİLMİŞ hâl taranır; çalışma ağacındaki değişiklikler taranmaz. İkili dosyalar (-I) içerik
# taramasına giremez → tools/sizinti-ikili-izinli.txt dışındaki her ikili dosya ENGEL; .gitattributes'ta bir kalıbı
# ikili sayan (`binary`, `-diff`) satır da, kalıp izin listesinde (yol olarak) değilse ENGEL (içeriği taramadan saklardı).
# Yollar core.quotePath=false + NUL ayraçla okunur: Türkçe (ASCII dışı) adlar tırnaklanıp denetimi atlatamaz.
# Metin desenleri: ağaç içeriği, commit mesajı, etiket metni, yol adları ve sembolik bağ hedefleri DOKUZ desenin
# hepsiyle taranır; yalnız yazar/commit eden/tagger kimlik satırları (ad + e-posta) e-posta deseninden muaftır
# (yazar e-postası kullanıcı kararıdır).
# Senaryo testi: tools/sizinti-tara-test.sh (JUnit: ToolsSizintiTaramaTest).
set -euo pipefail

IZIN_KAYNAGI=""   # --ikili-izin <dosya>
REF_ADI=""
ref_kipi=0
while [ $# -gt 0 ]; do
  case "$1" in
    --ikili-izin)
      [ $# -ge 2 ] || { echo "HATA: --ikili-izin bir dosya ister — tarama yapılamadı." >&2; exit 2; }
      IZIN_KAYNAGI="$2"; shift 2 ;;
    --ref-adi)
      [ $# -ge 2 ] || { echo "HATA: --ref-adi bir ad ister — tarama yapılamadı." >&2; exit 2; }
      REF_ADI="$2"; ref_kipi=1; shift 2 ;;
    --) shift; break ;;
    -*) echo "HATA: bilinmeyen seçenek: $1 — tarama yapılamadı." >&2; exit 2 ;;
    *) break ;;
  esac
done
KAYNAK="${1:-HEAD}"
case "$IZIN_KAYNAGI" in
  /*|"") ;;
  *) IZIN_KAYNAGI="$PWD/$IZIN_KAYNAGI" ;;   # aşağıdaki cd'den önce mutlak yola çevir
esac

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

# --- Desenler (ağaç, commit/etiket üstverisi ve ref adı için ORTAK) ---
# 127.0.0.1, 0.0.0.0 ve özel ağ blokları (10/8, 172.16/12, 192.168/16) ders örneği sayılır; gerisi gerçek sunucu.
# Sınırlar: önünde rakam ya da "rakam." yok, ardında "rakam" ya da ".rakam" yok → cümle sonundaki nokta
# (`Sunucu <IP>.`) ve `yedek/<IP>.eski` yakalanır; 1.2.3.4.5 gibi sürüm dizileri eşleşmez.
D_IPV4='(?<!\d)(?<!\d\.)(?!127\.0\.0\.1(?!\.?\d))(?!0\.0\.0\.0(?!\.?\d))(?!10\.)(?!192\.168\.)(?!172\.(1[6-9]|2\d|3[01])\.)(\d{1,3}\.){3}\d{1,3}(?!\.?\d)'
# Genel IPv6 = küresel tek yayın 2000::/3 (ilk grup 2xxx/3xxx, en az iki ':'); belge bloğu 2001:db8::/32 ve
# bloğun kendi gösterimi (2000:: — adres değil, ağ öneki) hariç. ::1, link-local fe80::/10 ve ULA fc00::/7 bu
# bloğun dışında kaldığından zaten eşleşmez. Ardından gelen nokta yalnız bir sözcük karakteri izliyorsa adresi
# sürdürür; cümle sonu noktası (`<IPv6>.`) eşleşmeyi engellemez.
D_IPV6='(?i)(?<![\w:.])(?!2001:0?db8:)(?!2000::(?![0-9a-f]))[23][0-9a-f]{3}:(?:[0-9a-f]{0,4}:){1,6}(?:[0-9a-f]{1,4})?(?![\w:]|\.\w)'
D_ROOT='root@[A-Za-z0-9.-]+'
D_ANAHTAR_OZEL='-----BEGIN [A-Z ]*PRIVATE KEY'
D_API='(sk-or-v1-[A-Za-z0-9]{20,}|sk-ant-[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{60,}|AKIA[0-9A-Z]{16})'
# Ortam/env biçiminde şifre ataması (ANAHTAR=değer, '=' çevresinde boşluk yok). DEĞER ŞART: ilk karakter boşluk,
# tırnak, < $ , ; ) ] } dışında HERHANGİ bir şey (nokta/soru/parantez/köşeli ile başlayan şifreler de yakalanır). Eşleşmez: <yer-tutucu>, boş değer, ${...}, "...", belge metnindeki `PGPASSWORD=,` /
# `X=)` gibi değersiz anılar ve XSRF-TOKEN gibi tireli adlar (çerez adı); Java `X = "..."` sabitleri (boşluklu) de.
D_SIFRE='(?<![\w-])(?:[A-Z0-9_]*_)?(?:PASSWORD|SECRET|TOKEN|API_KEY)=["\x27]?(?!\.\.\.)[^\s<$"\x27\x60,;)\]}]'
# Spring/YAML/kabuk biçimleri (satır başında anahtar; büyük-küçük harf duyarsız): spring.datasource.password=x,
# YAML `  password: x`, PGPASSWORD=x, export DB_PASS=x. Eşleşmez: ${...}, <yer-tutucu>, ..., boş değer; satır
# başına bağlı olduğundan Java/JS kodundaki `String password = ...` gibi tanımlar da eşleşmez. Değer D_SIFRE'deki
# gibi: ilk karakter boşluk, tırnak, , ; ) ] } dışında herhangi bir şey; `$X` yakalanır, yalnız `${` hariç
# (`password=,` / `password:)` belge anısı eşleşmez).
D_SIFRE_AYAR='(?i)^\s*(export\s+)?[\w.-]*(password|passwd|_pass|secret|api[_-]?key)\s*[=:]\s*["\x27]?(?!\$\{|<|\.\.\.)[^\s"\x27\x60,;)\]}]'

D_EPOSTA='[A-Za-z0-9._%+-]+@(gmail|hotmail|outlook|yahoo|icloud)\.com'
D_HOSTINGER='hstgr\.cloud'

# Metin taraması (commit mesajı, etiket metni, yol adları, sembolik bağ hedefleri, ref adı; kimlik satırları ayrı):
# geçici dizindeki dosyalar, aynı PCRE motoru (git grep --no-index).
#   metin-*.txt  → DOKUZ desenin hepsi
#   kimlik-*.txt → yazar/commit eden/tagger adı + e-postası: e-posta deseni HARİÇ hepsi
METIN_BASLIGI="metinde (mesaj/etiket/yol adı/bağ hedefi)"
tara_dosyalar() {  # $1 açıklama, $2 PCRE, $3 dosya kalıbı
  local bulgu rc=0
  bulgu=$(git -C "$gecici" grep --no-index -n -I -P -e "$2" -- "$3") || rc=$?
  case "$rc" in
    0) bildir "$METIN_BASLIGI $1" "$bulgu" ;;
    1) ;;
    *) yapilamadi "$METIN_BASLIGI '$1' deseninde git grep çıkış $rc" ;;
  esac
}
metinleri_tara() {  # metin-*.txt hepsiyle, kimlik-*.txt e-posta hariç
  local k
  for k in 'metin-*.txt' 'kimlik-*.txt'; do
    tara_dosyalar "genel IPv4 adresi" "$D_IPV4" "$k"
    tara_dosyalar "genel IPv6 adresi" "$D_IPV6" "$k"
    tara_dosyalar "root SSH hedefi"   "$D_ROOT" "$k"
    [ "$k" = 'kimlik-*.txt' ] || tara_dosyalar "e-posta adresi" "$D_EPOSTA" "$k"
    tara_dosyalar "özel anahtar"      "$D_ANAHTAR_OZEL" "$k"
    tara_dosyalar "API anahtarı"      "$D_API" "$k"
    tara_dosyalar "şifre ataması"     "$D_SIFRE" "$k"
    tara_dosyalar "şifre ataması (ayar dosyası biçimi)" "$D_SIFRE_AYAR" "$k"
    tara_dosyalar "sunucu adı (Hostinger)" "$D_HOSTINGER" "$k"
  done
}
# Yol/ad listesi: her satır hem bütün hâliyle hem '/' ile bölünmüş parçalarıyla yazılır → satır başına bağlı şifre
# deseni `a/<ANAHTAR>=<değer>/b` içinde de çalışır.
parcali_yaz() {  # stdin satırlar → stdout satırlar + parçaları
  local satir
  while IFS= read -r satir; do
    printf '%s\n%s\n' "$satir" "${satir//\//$'\n'}"
  done
}
# --ref-adi kipi: ad TÜM içerik desenleriyle (e-posta, şifre ataması, Hostinger dahil) taranır.
if [ "$ref_kipi" -eq 1 ]; then
  printf 'abc\n' >"$gecici/ozsinama.txt"
  git -C "$gecici" grep --no-index -q -P -e 'a(?=b)' -- ozsinama.txt >/dev/null 2>&1 \
    || yapilamadi "git grep -P öz-sınaması başarısız (PCRE desteği?)"
  parcali_yaz <<<"$REF_ADI" >"$gecici/metin-ref.txt"
  METIN_BASLIGI="ref adında"
  metinleri_tara
  if [ "$sizinti" -ne 0 ]; then
    echo "HATA: ref adı sızıntı taramasından geçmedi: $REF_ADI" >&2
    exit 1
  fi
  echo "Ref adı temiz ($REF_ADI)."
  exit 0
fi

# Etiket zinciri: her açıklamalı etiketin metni metin-etiket-N.txt'ye (tagger satırı kimlik-etiket-N.txt'ye) yazılır
# (aşağıda taranır); en sondaki nesne commit olmalı — blob/ağaç etiketi içerik taramasından kaçan veri taşırdı.
nesne=$(git rev-parse -q --verify "$KAYNAK" 2>/dev/null) || yapilamadi "nesne çözülemedi: $KAYNAK"
etiket_no=0
tur=$(git cat-file -t "$nesne") || yapilamadi "nesne türü okunamadı: $KAYNAK"
while [ "$tur" = tag ]; do
  etiket_no=$((etiket_no + 1))
  [ "$etiket_no" -le 20 ] || yapilamadi "etiket zinciri çok uzun: $KAYNAK"
  etiket_ham=$(git cat-file tag "$nesne") || yapilamadi "etiket okunamadı"
  # başlıktaki tagger satırı kimliktir (e-posta muaf); geri kalan her şey (etiket adı, mesaj) metin
  awk -v k="$gecici/kimlik-etiket-$etiket_no.txt" -v m="$gecici/metin-etiket-$etiket_no.txt" \
    '!govde && /^$/ { govde=1 } !govde && /^tagger / { print > k; next } { print > m }' <<<"$etiket_ham" \
    || yapilamadi "etiket metni ayrıştırılamadı"
  nesne=$(sed -n '1s/^object //p' <<<"$etiket_ham")
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

tara "genel IPv4 adresi"   "$D_IPV4"
tara "genel IPv6 adresi"   "$D_IPV6"
tara "root SSH hedefi"     "$D_ROOT"
tara "e-posta adresi"      "$D_EPOSTA"
tara "özel anahtar"        "$D_ANAHTAR_OZEL"
tara "API anahtarı"        "$D_API"
tara "şifre ataması"       "$D_SIFRE"
tara "şifre ataması (ayar dosyası biçimi)" "$D_SIFRE_AYAR"
tara "sunucu adı (Hostinger)" "$D_HOSTINGER"

# Ağaç kaydı (ls-tree hatası da tarama hatasıdır — `|| true` ile yutulmaz). core.quotePath=false + -z: ASCII dışı
# adlar "thoughts/oturum-\304\261.md" gibi tırnaklanıp denetimleri atlatamaz (NUL → satır sonu; adında satır sonu
# olan yol fazladan satır üretir → en kötü yanlış pozitif). Kayıt: "<kip> <tür> <sha>\t<yol>".
agac=$(git -c core.quotePath=false ls-tree -r -z --full-tree "$KAYNAK" | tr '\0' '\n'; exit "${PIPESTATUS[0]}") \
  || yapilamadi "git ls-tree başarısız"
yollar=$(cut -f2- <<<"$agac")

# Git dışı kalması gereken yollar
yasak=$(grep -E '^(thoughts/|data/|tools/deploy\.env$)' <<<"$yollar" || true)
# .env, .env.<her şey>, <ad>.env — şablonlar (*.ornek) hariç
envler=$(grep -E '(^|/)\.env(\..+)?$|(^|/)[^/]*\.env$' <<<"$yollar" | grep -vE '\.ornek$' || true)
yasak=$(printf '%s\n%s\n' "$yasak" "$envler" | sed '/^$/d')
[ -z "$yasak" ] || bildir "git dışı kalması gereken dosyalar" "$yasak"

# Yol adları (dosya + dizin): içerik desenlerinin hepsi — `notlar/<IP>.md`, `<ad>@gmail.com/` adı da sızıntıdır
parcali_yaz <<<"$yollar" >"$gecici/metin-yollar.txt"

# Sembolik bağ hedefleri (kip 120000): hedef metni blob'dadır ve git grep -I'ya hiç girmez → içerik desenlerinin hepsi
: >"$gecici/metin-baglar.txt"
while IFS= read -r kayit; do
  [ -n "$kayit" ] || continue
  bag_sha=$(cut -d' ' -f3 <<<"${kayit%%$'\t'*}")
  bag_hedef=$(git cat-file blob "$bag_sha") || yapilamadi "sembolik bağ hedefi okunamadı: ${kayit#*$'\t'}"
  printf '%s -> %s\n' "${kayit#*$'\t'}" "$bag_hedef" | parcali_yaz >>"$gecici/metin-baglar.txt"
done < <(grep -E '^120000 ' <<<"$agac" || true)

# Commit üstverisi: yazar + commit eden adı/e-postası kimliktir (e-posta deseni HARİÇ); mesaj gövdesi metindir (HEPSİ).
git log -1 --format='%an%n%ae%n%cn%n%ce' "$KAYNAK" >"$gecici/kimlik-commit.txt" || yapilamadi "git log başarısız"
git log -1 --format='%B' "$KAYNAK" >"$gecici/metin-mesaj.txt" || yapilamadi "git log başarısız"
metinleri_tara

# İkili dosyalar (-I ile içerik taramasına girmedi): izin listesi dışındaki her biri ENGEL. Liste satır başına
# `<tam yol> <blob-sha>` (# yorum, boş satır serbest); ikili yalnız yolu VE blob'u (`git rev-parse <commit>:<yol>`)
# girdiyle aynıysa geçer — izinli yolda içerik değişirse ENGEL (yeniden doğrulama + onay). Yerel .git/info/attributes ya da küresel öznitelik dosyası bir metni ikili
# saydırırsa o dosya da burada görünür → ENGEL (kapalı-güvenli).
# Liste ÇALIŞMA AĞACINDAN okunmaz (commit ikiliyi ve izin satırını birlikte ekleyebilirdi): kanca --ikili-izin ile
# yayınlanmış sürümü verir; tek başına çalışınca taranan commit'teki sürüm (commit'te yoksa boş liste → hiçbir
# ikiliye izin yok).
izin_yolu="tools/sizinti-ikili-izinli.txt"
if [ -n "$IZIN_KAYNAGI" ]; then
  [ -f "$IZIN_KAYNAGI" ] || yapilamadi "ikili izin listesi yok: $IZIN_KAYNAGI"
  izin_ham=$(cat "$IZIN_KAYNAGI") || yapilamadi "ikili izin listesi okunamadı: $IZIN_KAYNAGI"
  izin_dosyasi="yayınlanmış $izin_yolu"
elif git cat-file -e "$KAYNAK:$izin_yolu" 2>/dev/null; then
  izin_ham=$(git cat-file blob "$KAYNAK:$izin_yolu") || yapilamadi "ikili izin listesi commit'ten okunamadı"
  izin_dosyasi="$izin_yolu @ taranan commit"
else
  izin_ham=""
  izin_dosyasi="$izin_yolu (taranan commit'te yok → boş)"
fi
# Girdiler "<yol> <sha>" (tek boşluk) olarak normalleştirilir; sha'sız satır atlanır (hiçbir şeye izin vermez).
izinliler=""
while IFS= read -r g; do
  [[ "$g" =~ ^(.+)[[:space:]]+([0-9a-f]{40}|[0-9a-f]{64})$ ]] || continue
  izinliler="$izinliler${BASH_REMATCH[1]} ${BASH_REMATCH[2]}"$'\n'
done < <(sed -e 's/[[:space:]]*#.*$//' -e 's/[[:space:]]*$//' -e '/^$/d' <<<"$izin_ham")
izinli_yollar=$(sed -e 's/ [0-9a-f]*$//' <<<"$izinliler")
izinli_mi() {  # $1 yol → yol + taranan commit'teki blob sha'sı listede mi
  local sha
  [ -n "$izinliler" ] || return 1
  sha=$(git rev-parse -q --verify "$KAYNAK:$1" 2>/dev/null) || return 1
  grep -qxF -e "$1 $sha" <<<"$izinliler"
}
izinli_yol_mu() {  # $1 .gitattributes kalıbı → izinli bir yol mu (sha'sız karşılaştırma: kalıbın blob'u yok)
  [ -n "$izinli_yollar" ] && grep -qxF -e "$1" <<<"$izinli_yollar"
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
[ -z "$izinsiz" ] || bildir "izin listesi ($izin_dosyasi; girdi \`<yol> <blob-sha>\`) dışında ya da blob'u değişmiş ikili dosya (içeriği taranamaz)" "${izinsiz%$'\n'}"

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
    izinli_yol_mu "$kalip" || ozniteliksiz="$ozniteliksiz$oz_yol: $satir"$'\n'
  done <<<"$icerik"
done < <(grep -E '(^|/)\.gitattributes$' <<<"$yollar" || true)
[ -z "$ozniteliksiz" ] || bildir ".gitattributes içeriği taramadan saklayan satır (binary/-diff)" "${ozniteliksiz%$'\n'}"

if [ "$sizinti" -ne 0 ]; then
  echo "HATA: $AD sızıntı taramasından geçmedi; önce yukarıdakileri temizle." >&2
  exit 1
fi
echo "Sızıntı taraması temiz ($AD)."
