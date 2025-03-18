# Extraction of data from PBF files

## Requirements
 - `osmosis-bin` AUR package
 - `gdal` package

## Process

Use Osmosis to filter only shoreline (coastline) ways:
```bash
osmosis --read-pbf src/main/resources/maps/kiel.pbf --tf accept-ways natural=coastline --used-node --write-xml src/main/resources/maps/kiel_coastline.osm
```

If you get the error: 
```bash
Warning 1: Input datasource uses random layer reading, but output datasource does not support random layer writing
0...10.ERROR 1: Layer 'lines' does not already exist in the output dataset, and cannot be created by the output driver.
```
You can identify layers available in the osm file with:
```bash
 ogrinfo src/main/resources/maps/kiel_coastline.osm
```
And then convert the specific one:
```bash
ogr2ogr -f "GeoJSON" src/main/resources/maps/kiel_coastline.geojson src/main/resources/maps/kiel_coastline.osm lines
```