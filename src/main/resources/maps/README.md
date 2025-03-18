# Extraction of data from PBF files

## Requirements
 - `osmosis-bin` AUR package
 - `gdal` package

## Process

Use Osmosis to filter only shoreline (coastline) ways:
```bash
osmosis --read-pbf src/main/resources/maps/kiel.pbf --tf accept-ways natural=coastline --used-node --write-xml src/main/resources/maps/kiel_coastline.osm
```

You can identify layers available in the osm file with:
```bash
 ogrinfo src/main/resources/maps/kiel_coastline.osm
```
And then convert the specific one:
```bash
ogr2ogr -f "GeoJSON" src/main/resources/maps/kiel_coastline.geojson src/main/resources/maps/kiel_coastline.osm lines
```