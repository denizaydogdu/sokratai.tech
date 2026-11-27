#!/usr/bin/env bash
# tools/sizinti-tara.sh ve tools/git-kancalari/pre-push için otomatik test (Faz 11 inceleme düzeltmeleri).
# Geçici bir depo (mktemp -d) + çıplak (bare) uzak depo kurar, senaryoları push ederek/tarayarak dener, sonunda siler.
# Çalıştırma:  bash tools/sizinti-tara-test.sh      (JUnit: ToolsSizintiTaramaTest → ./mvnw verify koşar)
# Çıkış: 0 tüm senaryolar beklendiği gibi · 1 en az bir senaryo başarısız.
#
# DİKKAT: bu dosya repoda tarayıcıdan geçer → sızıntı örnekleri ÇALIŞMA ANINDA birleştirilerek üretilir
# (dosya metninde desenle eşleşen bir dize YOK).
set -uo pipefail

KOK=$(cd "$(dirname "$0")/.." && pwd)
GERCEK_GIT=$(command -v git) || { echo "git yok" >&2; exit 1; }

GECICI=$(mktemp -d)
trap 'rm -rf "$GECICI"' EXIT

# Kullanıcının küresel/sistem git ayarları (imzalama, kancalar, varsayılan dal) testi etkilemesin.
export GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1
export GIT_AUTHOR_NAME=deneme GIT_AUTHOR_EMAIL=deneme@ornek.invalid
export GIT_COMMITTER_NAME=deneme GIT_COMMITTER_EMAIL=deneme@ornek.invalid

DEPO="$GECICI/depo"
UZAK="$GECICI/uzak.git"
git init -q --bare "$UZAK"
git init -q -b main "$DEPO"
mkdir -p "$DEPO/tools/git-kancalari"
cp "$KOK/tools/sizinti-tara.sh" "$DEPO/tools/sizinti-tara.sh"
cp "$KOK/tools/git-kancalari/pre-push" "$DEPO/tools/git-kancalari/pre-push"
cp "$KOK/tools/sizinti-ikili-izinli.txt" "$DEPO/tools/sizinti-ikili-izinli.txt" 2>/dev/null || true
chmod +x "$DEPO/tools/git-kancalari/pre-push"
cd "$DEPO" || exit 1
git config core.hooksPath tools/git-kancalari
git config commit.gpgsign false
git remote add origin "$UZAK"
echo "tools/" >> .git/info/exclude   # tarayıcı/kanca izlenmez; yalnız senaryo dosyaları commit edilir
# İkili izin listesi ise gerçek repodaki gibi COMMIT EDİLİR (tarayıcı listeyi commit'ten / yayınlanmış sürümden okur)
git add -f tools/sizinti-ikili-izinli.txt

# Çalışma anında üretilen sızıntı örnekleri
IP4="198.51.100"; IP4="$IP4.7"
ROOT="ro""ot@sunucu.ornek"
IP6="2a02:47""80:1::5"
SIFRE="SPRING_DATASOURCE_PASS""WORD=gizli123"

basarisiz=0
sonuc() {  # $1 ad, $2 beklenen ("gecer"|"engel"|kod), $3 gerçek çıkış kodu, $4 çıktı
  local ad="$1" beklenen="$2" kod="$3" tamam=0
  case "$beklenen" in
    gecer) [ "$kod" -eq 0 ] && tamam=1 ;;
    engel) [ "$kod" -ne 0 ] && tamam=1 ;;
    *)     [ "$kod" -eq "$beklenen" ] && tamam=1 ;;
  esac
  if [ "$tamam" -eq 1 ]; then
    echo "GEÇTİ  $ad (çıkış $kod)"
  else
    echo "HATA   $ad — beklenen $beklenen, çıkış $kod"
    printf '%s\n' "$4" | head -20 | sed 's/^/       | /'
    basarisiz=1
  fi
}
commitle() {  # $1 mesaj; önceden eklenmiş değişiklikleri commit eder
  git add -A . && git commit -q -m "$1"
}
pushla() {  # $@ push argümanları; çıkış kodunu döndürür, çıktıyı CIKTI'ya yazar
  CIKTI=$(git push "$@" 2>&1); return $?
}
tara() {  # tarayıcıyı doğrudan çalıştırır
  CIKTI=$(bash tools/sizinti-tara.sh "$@" 2>&1); return $?
}

# 1) Temiz commit geçer
echo "merhaba" > oku.md
commitle "ilk temiz commit"   # izin listesi de bu commit'te
pushla origin main; sonuc "temiz commit push edilir" gecer $? "$CIKTI"

# 2) Sızıntı eklenip sonraki commit'te silinirse ENGEL (geçmişte kalır)
git checkout -q -b sizinti-silindi
echo "sunucu $IP4" > not.md; commitle "not ekle"
git rm -q not.md; git commit -q -m "notu sil"
tara HEAD; sonuc "uç commit tek başına temiz" gecer $? "$CIKTI"
pushla origin sizinti-silindi; sonuc "eklenip silinen sızıntı push'u" engel $? "$CIKTI"
git checkout -q main

# 3) eski-gecmis köklü commit ENGEL
git checkout -q --orphan eski-gecmis
git rm -rq --cached . >/dev/null 2>&1; rm -f oku.md
echo "eski" > eski.md; commitle "eski kök"
git checkout -q -b eski-turev
echo "türev" > turev.md; commitle "eskiden türeyen"
pushla origin eski-turev; sonuc "eski-gecmis kökünden türeyen push" engel $? "$CIKTI"
git checkout -q -f main

# 4) Silme push'u geçer
git checkout -q -b silinecek
echo "geçici" > gecici.md; commitle "silinecek dal"
pushla origin silinecek; sonuc "silinecek dal push edilir" gecer $? "$CIKTI"
git checkout -q main
pushla origin --delete silinecek; sonuc "silme push'u" gecer $? "$CIKTI"

# 5) git grep hatası → tarama yapılamadı → çıkış 2 (kapalı-güvenli)
SAHTE="$GECICI/sahte-bin"; mkdir -p "$SAHTE"
cat > "$SAHTE/git" <<EOF
#!/usr/bin/env bash
for a in "\$@"; do [ "\$a" = grep ] && exit 128; done
exec "$GERCEK_GIT" "\$@"
EOF
chmod +x "$SAHTE/git"
CIKTI=$(PATH="$SAHTE:$PATH" bash tools/sizinti-tara.sh HEAD 2>&1); sonuc "git grep 128 (öz-sınama)" 2 $? "$CIKTI"
# yalnız tek desenin grep'i bozulursa da 2 (öz-sınama geçer, desen başarısız)
cat > "$SAHTE/git" <<EOF
#!/usr/bin/env bash
bozuk=0; ozel=0
for a in "\$@"; do
  [ "\$a" = grep ] && ozel=1
  case "\$a" in *PRIVATE*) bozuk=1 ;; esac
done
[ "\$ozel" -eq 1 ] && [ "\$bozuk" -eq 1 ] && exit 128
exec "$GERCEK_GIT" "\$@"
EOF
CIKTI=$(PATH="$SAHTE:$PATH" bash tools/sizinti-tara.sh HEAD 2>&1); sonuc "tek desende git grep 128" 2 $? "$CIKTI"

# 6) .env.prod ENGEL; deploy/site.env.ornek ve .env.ornek geçer
git checkout -q -b env-dosyalari
mkdir -p deploy
echo "APP_SITE_GUVENLI_CEREZ=true" > deploy/site.env.ornek
echo "OPENROUTER_API_KEY=" > .env.ornek
commitle "env şablonları"
tara HEAD; sonuc "deploy/site.env.ornek + .env.ornek" gecer $? "$CIKTI"
echo "APP_SITE_GUVENLI_CEREZ=true" > .env.prod; git add -f .env.prod; git commit -q -m "env prod"
tara HEAD; sonuc ".env.prod" engel $? "$CIKTI"
git checkout -q -f main
git checkout -q -b env-alt
mkdir -p deploy; echo "X=1" > deploy/canli.env; git add -f deploy/canli.env; git commit -q -m "canli env"
tara HEAD; sonuc "deploy/canli.env" engel $? "$CIKTI"
git checkout -q -f main

# 7) Commit mesajında IP ENGEL (ağaç temiz)
git checkout -q -b mesaj-ip
echo "değişiklik" > degisiklik.md; commitle "sunucuya $IP4 üzerinden kuruldu"
tara HEAD; sonuc "commit mesajında IP" engel $? "$CIKTI"
pushla origin mesaj-ip; sonuc "commit mesajında IP (push)" engel $? "$CIKTI"
git checkout -q -f main

# 8) Şifre ataması ENGEL; yer tutucu / boş / ${...} / ... geçer
git checkout -q -b sifre
printf '%s\n' "SPRING_DATASOURCE_PASSWORD=<veritabani-sifresi>" "OPENROUTER_API_KEY=" \
  'DB_PASSWORD=${DB_PASSWORD}' "PG_TEST_PASSWORD=..." "XSRF-TOKEN=abc" > yer-tutucu.md
commitle "yer tutucular"
tara HEAD; sonuc "şifre yer tutucuları" gecer $? "$CIKTI"
echo "$SIFRE" > gercek.md; commitle "gerçek şifre"
tara HEAD; sonuc "gerçek şifre ataması" engel $? "$CIKTI"
git checkout -q -f main

# 9) Genel IPv6 ENGEL; belge/yerel IPv6 geçer
git checkout -q -b ipv6
printf '%s\n' "belge 2001:db8::1" "blok 2000::/3" "yerel ::1" "link fe80::1" "ula fd00::1" "saat 12:30:45" "String::valueOf" > ipv6.md
commitle "yerel ipv6"
tara HEAD; sonuc "belge/yerel IPv6" gecer $? "$CIKTI"
echo "sunucu $IP6" > sunucu.md; commitle "genel ipv6"
tara HEAD; sonuc "genel IPv6" engel $? "$CIKTI"
git checkout -q -f main

# 10) Büyük bulgu (binlerce satır) taramayı yarıda kesmez: çıkış tam 1 (SIGPIPE 141 değil)
git checkout -q -b buyuk
for i in $(seq 1 4000); do echo "ssh $ROOT # satır $i ................................................"; done > buyuk.md
commitle "büyük"
tara HEAD; sonuc "büyük bulgu (SIGPIPE yok)" 1 $? "$CIKTI"
git checkout -q -f main

# 11) İzin listesi dışındaki ikili dosya ENGEL (içerik taramasına girmediği için her şeyi saklayabilirdi)
git checkout -q -b ikili
printf '\x00\x01\x02ikili' > veri.bin; commitle "ikili dosya"
tara HEAD; sonuc "izin listesi dışı ikili dosya" engel $? "$CIKTI"
git checkout -q -f main
#     NUL baytlı .txt içinde IP → ikili sayılır → ENGEL
git checkout -q -b ikili-txt
printf 'sunucu %s\000\n' "$IP4" > not.txt; commitle "nul baytlı not"
tara HEAD; sonuc "NUL baytlı .txt (içinde IP)" engel $? "$CIKTI"
git checkout -q -f main
#     İzin listesindeki ikili (.mvn/wrapper/maven-wrapper.jar) geçer
git checkout -q -b ikili-izinli
mkdir -p .mvn/wrapper; printf '\x00\x01jar' > .mvn/wrapper/maven-wrapper.jar; commitle "wrapper jar"
tara HEAD; sonuc "izin listesindeki ikili dosya" gecer $? "$CIKTI"
#     .gitattributes: izinli yol için binary geçer; başka kalıba binary / -diff ENGEL
echo ".mvn/wrapper/maven-wrapper.jar binary" > .gitattributes; commitle "izinli gitattributes"
tara HEAD; sonuc ".gitattributes izinli yol binary" gecer $? "$CIKTI"
git checkout -q -f main
git checkout -q -b gitattr-binary
echo "*.md binary" > .gitattributes; commitle "md ikili"
tara HEAD; sonuc ".gitattributes *.md binary" engel $? "$CIKTI"
git checkout -q -f main
git checkout -q -b gitattr-diff
mkdir -p alt; echo "*.txt -diff" > alt/.gitattributes; echo "metin" > alt/a.txt; commitle "txt diff kapalı"
tara HEAD; sonuc "alt/.gitattributes *.txt -diff" engel $? "$CIKTI"
git checkout -q -f main

# 12) Commit EDEN (committer) adında IP ENGEL (yazar temiz)
git checkout -q -b committer-ip
echo "c" > c.md; git add -A .
GIT_COMMITTER_NAME="sunucu $IP4" git commit -q -m "temiz mesaj"
tara HEAD; sonuc "committer adında IP" engel $? "$CIKTI"
git checkout -q -f main

# 13) Türkçe (ASCII dışı) yol adı: git'in yol tırnaklaması (core.quotePath) yasak yol denetimini atlatmasın
git checkout -q -b turkce-yol
mkdir -p thoughts; echo "not" > "thoughts/oturum-ı.md"; git add -f "thoughts/oturum-ı.md"; git commit -q -m "oturum"
tara HEAD; sonuc "thoughts/oturum-ı.md" engel $? "$CIKTI"
git checkout -q -f main
git checkout -q -b turkce-env
mkdir -p deploy; echo "X=1" > "deploy/canlı.env"; git add -f "deploy/canlı.env"; git commit -q -m "canlı env"
tara HEAD; sonuc "deploy/canlı.env" engel $? "$CIKTI"
git checkout -q -f main

# 14) Spring/kabuk şifre biçimleri ENGEL; ${...} ve <yer-tutucu> geçer
P="pass""word"
sifre_dene() {  # $1 ad, $2 beklenen, $3 dosya içeriği
  git checkout -q -b sifre-deneme
  printf '%s\n' "$3" > ayar.txt; commitle "ayar"
  tara HEAD; sonuc "$1" "$2" $? "$CIKTI"
  git checkout -q -f main; git branch -q -D sifre-deneme
}
sifre_dene "spring.datasource.$P=…"   engel "spring.datasource.$P=Hunter2real"
sifre_dene "YAML '  $P: …'"           engel "  $P: Hunter2real"
sifre_dene "PGPASSWORD=…"             engel "PGPASS""WORD=Hunter2real"
sifre_dene "export DB_PASS=…"         engel "export DB_PA""SS=Hunter2real"
sifre_dene "YAML $P: \${X}"           gecer "$P: \${X}"
sifre_dene "YAML $P: <yer-tutucu>"    gecer "$P: <yer-tutucu>"

# 15) Etiketler: açıklamalı etiket mesajında IP ENGEL; blob'a hafif etiket ENGEL; temiz etiket geçer
git tag -a temiz-etiket -m "sürüm notu" main
pushla origin temiz-etiket; sonuc "temiz açıklamalı etiket" gecer $? "$CIKTI"
git tag -a ip-etiket -m "sunucu $IP4" main
tara ip-etiket; sonuc "etiket mesajında IP (tarayıcı)" engel $? "$CIKTI"
pushla origin ip-etiket; sonuc "etiket mesajında IP (push)" engel $? "$CIKTI"
blob=$(echo "blob içerik" | git hash-object -w --stdin)
git tag blob-etiket "$blob"
pushla origin blob-etiket; sonuc "blob'a hafif etiket (push)" engel $? "$CIKTI"

# 16) Eski kök commit kancaya SABİT yazılı: `eski-gecmis` dalı yoksa da eski köklü push ENGEL
git checkout -q --orphan eski-kok-2
git rm -rq --cached . >/dev/null 2>&1; rm -f oku.md
echo "eski2" > eski2.md; commitle "eski kök 2"
eski_kok2=$(git rev-parse HEAD)
git checkout -q -b eski2-turev-a
echo "a" > a2.md; commitle "eski2 türev a"
pushla origin eski2-turev-a; sonuc "kök sabit listede değilken (kontrol)" gecer $? "$CIKTI"
git checkout -q -b eski2-turev-b
echo "b" > b2.md; commitle "eski2 türev b"
git checkout -q -f main
git branch -q -D eski-kok-2
kanca=tools/git-kancalari/pre-push
sed "s/^ESKI_KOKLER=.*/ESKI_KOKLER=\"$eski_kok2\"/" "$kanca" > "$GECICI/kanca"; cat "$GECICI/kanca" > "$kanca"
pushla origin eski2-turev-b; sonuc "sabit eski kökten türeyen push (dal yok)" engel $? "$CIKTI"

# 17) Ref adları: yerel VE uzak ref adı içerik desenleriyle taranır (ad tek başına sızıntı taşıyabilir)
IPT="203.0.113"; IPT="$IPT.5"
RADI="ozellik/ro""ot@ornek"
git tag "deploy-$IPT" main
pushla origin "deploy-$IPT"; sonuc "hafif etiket deploy-<IP> (push)" engel $? "$CIKTI"
case "$CIKTI" in *"deploy-$IPT"*) ;; *) sonuc "engel mesajı ref adını söyler" engel 0 "$CIKTI" ;; esac
git branch "$RADI" main
pushla origin "$RADI"; sonuc "dal ozellik/root@… (push)" engel $? "$CIKTI"
pushla origin "main:refs/heads/yedek-$IPT"; sonuc "yalnız UZAK ref adında IP (push)" engel $? "$CIKTI"
git tag v1.0 main
pushla origin v1.0; sonuc "normal etiket v1.0 (push)" gecer $? "$CIKTI"
echo "ana" > ana.md; commitle "ana dal güncellemesi"
pushla origin main; sonuc "normal dal main (push)" gecer $? "$CIKTI"

# 18) İkili izin listesi YAYINLANMIŞ sürümden okunur: aynı commit'te ikili + liste satırı eklemek onaysız ENGEL
git checkout -q -b ikili-onay
printf '\x00\x01xbin' > x.bin; echo "x.bin" >> tools/sizinti-ikili-izinli.txt; commitle "x.bin ve izin satırı"
pushla origin ikili-onay; sonuc "ikili + izin satırı aynı commit'te (onaysız push)" engel $? "$CIKTI"
case "$CIKTI" in *"+ x.bin"*) ;; *) sonuc "onay gereken girdi (x.bin) yazılır" engel 0 "$CIKTI" ;; esac
CIKTI=$(SIZINTI_IKILI_ONAY=1 git push origin ikili-onay 2>&1); sonuc "aynısı SIZINTI_IKILI_ONAY=1 ile" gecer $? "$CIKTI"
git checkout -q -f main
#     Yayınlanmış listede olan .mvn/wrapper/maven-wrapper.jar (11'deki dal) onaysız geçer
pushla origin ikili-izinli; sonuc "izinli maven-wrapper.jar (push)" gecer $? "$CIKTI"
#     Tarayıcı tek başına: liste ÇALIŞMA AĞACINDAN değil taranan commit'ten okunur
git checkout -q -b calisma-agaci
printf '\x00\x01zbin' > z.bin; commitle "z.bin izinsiz"
echo "z.bin" >> tools/sizinti-ikili-izinli.txt   # yalnız çalışma ağacında (commit edilmedi)
tara HEAD; sonuc "çalışma ağacındaki liste satırı sayılmaz (tarayıcı)" engel $? "$CIKTI"
git checkout -q -- tools/sizinti-ikili-izinli.txt
git checkout -q -f main
#     İlk push (taban yok: uzakta da refs/remotes/<uzak>/main'de de yok) → commit'in KENDİ listesi
git init -q --bare "$GECICI/ilk.git"; git remote add ilk "$GECICI/ilk.git"
git checkout -q -b ilk-push
printf '\x00\x01ybin' > y.bin; echo "y.bin" >> tools/sizinti-ikili-izinli.txt; commitle "y.bin ve izin satırı"
git checkout -q main -- tools/sizinti-ikili-izinli.txt 2>/dev/null; git reset -q tools/sizinti-ikili-izinli.txt
pushla ilk ilk-push; sonuc "ilk push (taban yok) commit'in kendi listesiyle" gecer $? "$CIKTI"
git checkout -q -f main

if [ "$basarisiz" -ne 0 ]; then
  echo "SONUÇ: sızıntı tarayıcı testleri BAŞARISIZ" >&2
  exit 1
fi
echo "SONUÇ: tüm sızıntı tarayıcı senaryoları geçti"
