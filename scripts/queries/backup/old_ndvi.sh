#!/bin/bash

#=Parameters=================================
platform=1
resolution=0
startLongitude=-180
startLatitude=-90
startTime=201200000000
stopTime=201300000000
xTiles=2000
xCellsPerTile=1805
yTiles=2000
yCellsPerTile=905

#=Derivations================================
startLongitude_e4=$(echo $startLongitude 10000 | awk '{printf "%d", int($1 * $2)}')
xDimSize=$(($xTiles * $xCellsPerTile))
stopLongitude_e4=$(($startLongitude_e4 + $xDimSize - 1))

startLatitude_e4=$(echo $startLatitude 10000 | awk '{printf "%d", int($1 * $2)}')
yDimSize=$(($yTiles * $yCellsPerTile))
stopLatitude_e4=$(($startLatitude_e4 + $yDimSize - 1))

#qkmOverlapCells=23
qkmOverlapCells=0
xBlurWindowSize=$(($xCellsPerTile + $qkmOverlapCells))
yBlurWindowSize=$(($yCellsPerTile + $qkmOverlapCells))

#============================================
targetSchema="<ndvi:double null>[x=1:$xDimSize,10000,0,y=1:$yDimSize,10000,0]"
iquery -anq "remove(ndvi_points)" > /dev/null 2>&1
iquery -anq "create immutable empty array ndvi_points $targetSchema" > /dev/null 2>&1

echo "Calculating NDVI point values..."
time iquery -anq "
  redimension_store(
    apply(
      join(
        attribute_rename(
          between(
            band_1_measurements,
            $startLongitude_e4, $startLatitude_e4, $startTime, $platform, $resolution,
            $stopLongitude_e4, $stopLatitude_e4, $stopTime, $platform, $resolution
          ),
          reflectance,
          red
        ),
        attribute_rename(
          between(
            band_2_measurements,
            $startLongitude_e4, $startLatitude_e4, $startTime, $platform, $resolution,
            $stopLongitude_e4, $stopLatitude_e4, $stopTime, $platform, $resolution
          ),
          reflectance,
          nir
        )
      ),
      x, longitude_e4 - $startLongitude_e4 + 1,
      y, latitude_e4 - $startLatitude_e4 + 1,
      ndvi, (nir - red) / (nir + red)
    ),
    ndvi_points,
    max(ndvi) as ndvi
  )"
echo "Done."

echo "Generating grid centers..."
iquery -anq "remove(ndvi_grid_centers)" > /dev/null 2>&1
iquery -anq "create immutable empty array ndvi_grid_centers $targetSchema" > /dev/null 2>&1

time iquery -anq "
  redimension_store(
    apply(
      cross(
        build(<x:int64>[i=1:$xTiles,$xTiles,0], int64($xCellsPerTile * (i - 0.5) + 0.5)),
        build(<y:int64>[i=1:$yTiles,$yTiles,0], int64($yCellsPerTile * (i - 0.5) + 0.5))
      ),
      ndvi, double(null)
    ),
    ndvi_grid_centers
  )"
echo "Done."

iquery -anq "remove(ndvi_gridded)" > /dev/null 2>&1
xStart=$(echo $xCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')
yStart=$(echo $yCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')
xNewChunk=$((10000 / $xCellsPerTile))
yNewChunk=$((10000 / $yCellsPerTile))

echo "Gridding NDVI results..."
time iquery -anq "
  store(
    merge(
      thin(
        window(
          merge(ndvi_points, ndvi_grid_centers),
          $xBlurWindowSize,
          $yBlurWindowSize,
          max(ndvi) as ndvi
        ),
        $xStart, $xCellsPerTile,
        $yStart, $yCellsPerTile
      ),
      build(<ndvi:double null>[x=0:$(($xTiles - 1)),$xNewChunk,0,y=0:$(($yTiles - 1)),$yNewChunk,0], -2)
    ),
    ndvi_gridded
  )"
echo "Done."

iquery -anq "remove(ndvi_points)" > /dev/null 2>&1
iquery -anq "remove(ndvi_grid_centers)" > /dev/null 2>&1
