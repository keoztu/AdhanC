#!/usr/bin/env python3
"""Builds app/src/main/assets/cities.tsv from GeoNames (CC BY 4.0, https://www.geonames.org).

Columns: name, search names (| separated), region, country code, lat, lng, time zone, population.
Sorted by population so larger places come first in search results.
"""
import csv, io, os, re, sys, urllib.request, zipfile

UA = {"User-Agent": "SalahTimesOnly-build/1.0"}
BASE = "https://download.geonames.org/export/dump/"
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "cities.tsv")
LATIN = re.compile(r"^[A-Za-zÀ-ɏ' .\-]+$")

def get(name):
    with urllib.request.urlopen(urllib.request.Request(BASE + name, headers=UA), timeout=120) as r:
        return r.read()

try:
    admin_txt = get("admin1CodesASCII.txt").decode("utf-8")
    cities_zip = get("cities15000.zip")
except Exception as e:  # network trouble: fall back to the small bundled list so the build still works
    print(f"WARNING: GeoNames download failed ({e}); using scripts/cities_fallback.tsv")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    import shutil
    shutil.copy(os.path.join(os.path.dirname(__file__), "cities_fallback.tsv"), OUT)
    sys.exit(0)

admin1 = {}
for line in admin_txt.splitlines():
    parts = line.split("\t")
    if len(parts) >= 2:
        admin1[parts[0]] = parts[1]

with zipfile.ZipFile(io.BytesIO(cities_zip)) as z:
    data = z.read("cities15000.txt").decode("utf-8")

rows = []
for line in data.splitlines():
    f = line.split("\t")
    if len(f) < 19:
        continue
    name, ascii_name, alts, lat, lng, fcode, cc, a1, pop, tz = f[1], f[2], f[3], f[4], f[5], f[7], f[8], f[10], f[14], f[17]
    if not tz or fcode in ("PPLX", "PPLH", "PPLQ", "PPLW"):  # skip city sections and historical places
        continue
    pop = int(pop or 0)
    names = [name]
    if ascii_name and ascii_name != name:
        names.append(ascii_name)
    if pop >= 300_000 and alts:
        extra = [a for a in alts.split(",") if LATIN.match(a) and 2 < len(a) < 40]
        for a in extra[:25]:
            if a not in names:
                names.append(a)
    region = admin1.get(f"{cc}.{a1}", "")
    rows.append((pop, [name, "|".join(n.replace("\t", " ") for n in names), region, cc,
                       f"{float(lat):.4f}", f"{float(lng):.4f}", tz, str(pop)]))

rows.sort(key=lambda r: -r[0])
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8", newline="") as fh:
    for _, r in rows:
        fh.write("\t".join(r) + "\n")
print(f"Wrote {len(rows)} places to {os.path.normpath(OUT)} ({os.path.getsize(OUT)//1024} KB)")
if len(rows) < 20_000:
    sys.exit("Unexpectedly few places; aborting")
