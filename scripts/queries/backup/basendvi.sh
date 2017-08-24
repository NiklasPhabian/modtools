#!/bin/bash

#=Filter Parameters==========================
platform=1
resolution=0
startLongitude=-125
startLatitude=33
startTime=201200000000
stopTime=201300000000
xTiles=5000
xCellsPerTile=20
yTiles=5000
yCellsPerTile=20

#=Filter Parameter Derivations===============
lonDimSize=$(($xTiles * $xCellsPerTile))
startLongitude_e4=$(echo $startLongitude 10000 | awk '{printf "%.0f", $1 * $2}')
stopLongitude_e4=$(($startLongitude_e4 + $lonDimSize - 1))

latDimSize=$(($yTiles * $yCellsPerTile))
startLatitude_e4=$(echo $startLatitude 10000 | awk '{printf "%.0f", $1 * $2}')
stopLatitude_e4=$(($startLatitude_e4 + $latDimSize - 1))

#=Projection Constants=======================
#stdParallel1=34.0
#stdParallel2=40.5
#originLongitude=-120.0
#originLatitude=0.0
#falseEasting=0.0
#falseNorthing=-4000000.0

#=Projection Derivations=====================
#radPerDeg=0.0174532925
#n=$(echo "scale=10; 0.5 * (s($stdParallel1 * $radPerDeg) + s($stdParallel2 * $radPerDeg))" | bc -l)
#theta="$n * ((double(longitude_e6) / 1000000) - $originLongitude) * $radPerDeg"
#C=$(echo "scale=10; c($stdParallel1 * $radPerDeg)^2 + (2 * $n * s($stdParallel1 * $radPerDeg))" | bc -l)
#rho="(sqrt($C - (2 * $n * sin((double(latitude_e6) / 1000000) * $radPerDeg))) / $n)"
#rhoZero=$(echo "scale=10; (sqrt($C - (2 * $n * s($originLatitude * $radPerDeg))) / $n)" | bc -l)
#xFormula="$falseEasting + ($rho * sin($theta))"
#echo "x = $xFormula"
#yFormula="$falseNorthing + $rhoZero - ($rho * cos($theta))"
#echo "y = $yFormula"

#=Grid Constants=============================
#startX=-218961.024339044
#startY=-335478.818352272
#stopX=219038.975660956
#stopY=389521.181647728

#============================================

targetSchema="<ndvi:double null>[x=1:$lonDimSize,$lonDimSize,0,y=1:$latDimSize,$latDimSize,0]"
iquery -aq "remove(ndvi_gridded)" > /dev/null 2>&1

echo "Calculating NDVI point values..."
time iquery -anq "
  store(
    merge(
      regrid(
        redimension(
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
          $targetSchema,
          max(ndvi) as ndvi
        ),
        $xCellsPerTile,
        $yCellsPerTile,
        max(ndvi) as ndvi
      ),
      build(
        <ndvi:double null>[x=1:$xTiles,$lonDimSize,0,y=1:$yTiles,$latDimSize,0],
        -1
      )
    ),
    ndvi_gridded
  )"
echo "Done."
