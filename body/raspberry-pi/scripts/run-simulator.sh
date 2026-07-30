#!/usr/bin/env sh
set -eu

kiko_script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
kiko_body_root=$(dirname -- "$kiko_script_dir")

PYTHONDONTWRITEBYTECODE=1 PYTHONPATH="$kiko_body_root/src" \
  exec python3 -m kiko_body
