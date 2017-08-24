#!/bin/bash

#=Parameters=================================
platform=1
resolution=0
startLongitude=-180
stopLongitude=180
startLatitude=-90
stopLatitude=90
startTime=201200000000
stopTime=201300000000

#=Derivations================================
startLongitude_e4=$(echo $startLongitude 10000 | awk '{printf "%d", int($1 * $2)}')
stopLongitude_e4=$(echo $stopLongitude 10000 | awk '{printf "%d", int($1 * $2)}')
startLatitude_e4=$(echo $startLatitude 10000 | awk '{printf "%d", int($1 * $2)}')
stopLatitude_e4=$(echo $stopLatitude 10000 | awk '{printf "%d", int($1 * $2)}')

#============================================
targetSchema="<ndvi:double null>[longitude_e4=$startLongitude_e4:$stopLongitude_e4,10000,0,latitude_e4=$startLatitude_e4:$stopLatitude_e4,10000,0]"
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
      ndvi, (nir - red) / (nir + red)
    ),
    ndvi_points,
    max(ndvi) as ndvi
  )"
echo "Done."
