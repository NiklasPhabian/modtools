#!/bin/bash

#=Parameters=================================
platform=1
startTime=201200000000
stopTime=201300000000
startLongitude=-1250000
startLatitude=320000
#============================================
qkmOverlapCells=2253

lonTiles=2000
lonCellsPerTile=2000
lonSize=$(($lonTiles * $lonCellsPerTile))
stopLongitude=$(($startLongitude + $lonSize - 1))
lonBlurWindowSize=$(($lonCellsPerTile + $qkmOverlapCells))

latTiles=2000
latCellsPerTile=2000
latSize=$(($latTiles * $latCellsPerTile))
stopLatitude=$(($startLatitude + $latSize - 1))
latBlurWindowSize=$(($latCellsPerTile + $qkmOverlapCells))

ndviSchema="<ndvi:uint8 NULL>[x=1:$lonSize,1000000,0,y=1:$latSize,1000000,0]"
iquery -anq "remove(ndvi_points)" > /dev/null 2>&1
iquery -anq "create immutable empty array ndvi_points $ndviSchema" > /dev/null 2>&1

echo "Calculating NDVI point values..."
time iquery -anq "
  redimension_store(
    apply(
      join(
        attribute_rename(
          between(
            band_1_measurements,
            $startLongitude, $startLatitude, $startTime, $platform, 0,
            $stopLongitude, $stopLatitude, $stopTime, $platform, 0
          ),
          reflectance,
          red
        ),
        attribute_rename(
          between(
            band_2_measurements,
            $startLongitude, $startLatitude, $startTime, $platform, 0,
            $stopLongitude, $stopLatitude, $stopTime, $platform, 0
          ),
          reflectance,
          nir
        )
      ),
      x, longitude_e4 - $startLongitude + 1,
      y, latitude_e4 - $startLatitude + 1,
      ndvi, uint8((((nir - red) / (nir + red)) * 200) + 50)
    ),
    ndvi_points,
    max(ndvi) as ndvi
  )"
echo "Done."


: '
echo "Generating grid centers..."
iquery -anq "remove(ndvi_grid_centers)" > /dev/null 2>&1
iquery -anq "create immutable empty array ndvi_grid_centers $ndviSchema" > /dev/null 2>&1

time iquery -anq "
  redimension_store(
    apply(
      cross(
        build(<x:int64>[i=1:$lonTiles,1000,0], $lonCellsPerTile * (i - 0.5)),
        build(<y:int64>[i=1:$latTiles,1000,0], $latCellsPerTile * (i - 0.5))
      ),
      ndvi, double (null)
    ),
    ndvi_grid_centers
  )"
echo "Done."

iquery -anq "remove(ndvi_gridded)" > /dev/null 2>&1

echo "Gridding NDVI results..."
time iquery -anq "
  store(
    thin(
      window(
        merge(ndvi_points, ndvi_grid_centers),
        $lonBlurWindowSize,
        $latBlurWindowSize,
        avg(ndvi) as ndvi
      ),
      $lonCellsPerTile / 2, $lonCellsPerTile,
      $latCellsPerTile / 2, $latCellsPerTile
    ),
    ndvi_gridded
  )"
echo "Done."

iquery -anq "remove(ndvi_points)" > /dev/null 2>&1
iquery -anq "remove(ndvi_grid_centers)" > /dev/null 2>&1
'

echo "Gridding NDVI results..."

echo "Done."
