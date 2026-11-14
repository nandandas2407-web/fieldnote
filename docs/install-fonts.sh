#!/data/data/com.termux/files/usr/bin/bash
# Extracts Playfair Display, Plus Jakarta Sans and JetBrains Mono zips (downloaded from
# fonts.google.com / jetbrains.com) from ~/storage/downloads into the app's assets/fonts.
F="${1:-$HOME/Fieldnote}/app/src/main/assets/fonts"
mkdir -p "$F"
pkg list-installed 2>/dev/null | grep -q '^unzip/' || pkg install -y unzip
for pat in '*layfair*' '*akarta*' '*etBrains*'; do
  for z in $HOME/storage/downloads/$pat.zip; do
    [ -f "$z" ] || continue
    echo "Reading $z"
    T=$(mktemp -d); unzip -q -o "$z" -d "$T"
    # prefer static weights, otherwise use the variable file
    S=$(find "$T" -iname '*.ttf' -path '*static*' ! -iname '*italic*')
    [ -z "$S" ] && S=$(find "$T" -iname '*.ttf' -path '*ttf*' ! -iname '*italic*' ! -iname '*NL*' ! -ipath '*variable*')
    if [ -n "$S" ]; then
      echo "$S" | while read -r f; do cp "$f" "$F/"; done
    else
      find "$T" -iname '*.ttf' ! -iname '*italic*' | while read -r f; do
        b=$(basename "$f"); cp "$f" "$F/$(echo "$b" | sed 's/\[.*\]/-VAR/')"
      done
    fi
    rm -rf "$T"
  done
done
echo "Installed fonts:"; ls "$F"
