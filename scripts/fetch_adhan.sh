#!/usr/bin/env bash
# Downloads freely licensed Adhan recordings from Wikimedia Commons and prepares
# full and short (5 s) versions in app/src/main/res/raw.
#   adhan_1: "Beautiful adhan.ogg" by Adam-synagda, CC0 1.0
#            https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg
#   adhan_2: "Azan.ogg" by Andrewler, CC BY-SA 4.0
#            https://commons.wikimedia.org/wiki/File:Azan.ogg
set -euo pipefail
cd "$(dirname "$0")/.."
RAW=app/src/main/res/raw
WORK=scripts/work
mkdir -p "$RAW" "$WORK"
UA="SalahTimesOnly-build/1.0 (https://github.com; build script)"
SHORT_SECONDS="${SHORT_SECONDS:-5}"

fetch() { # $1 = Commons file name, $2 = output
  curl -fsSL -A "$UA" -o "$2" "https://commons.wikimedia.org/wiki/Special:FilePath/$1"
}
prepare() { # $1 = source, $2 = id
  # Full: loudness-normalised mono Ogg Vorbis
  ffmpeg -hide_banner -loglevel error -y -i "$1" -ac 1 -ar 44100 \
    -af "silenceremove=start_periods=1:start_threshold=-45dB,loudnorm=I=-18:TP=-2" \
    -c:a libvorbis -q:a 3 "$RAW/adhan_${2}_full.ogg"
  # Short: first SHORT_SECONDS seconds with a gentle fade-out
  local fade_start
  fade_start=$(python3 -c "print(max(0, $SHORT_SECONDS - 1.2))")
  ffmpeg -hide_banner -loglevel error -y -i "$1" -ac 1 -ar 44100 \
    -af "silenceremove=start_periods=1:start_threshold=-45dB,atrim=0:${SHORT_SECONDS},afade=t=out:st=${fade_start}:d=1.2,loudnorm=I=-18:TP=-2" \
    -c:a libvorbis -q:a 3 "$RAW/adhan_${2}_short.ogg"
}

fetch "Beautiful_adhan.ogg" "$WORK/a1.ogg"
fetch "Azan.ogg" "$WORK/a2.ogg"
prepare "$WORK/a1.ogg" 1
prepare "$WORK/a2.ogg" 2
ls -la "$RAW"
