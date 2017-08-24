#=Parameters=================================
platform=1
resolution=0
startTime=201200000000
stopTime=201300000000
startLongitude=-1250000
startLatitude=320000
#============================================
lonTiles=2000
lonCellsPerTile=2000
lonSize=$(($lonTiles * $lonCellsPerTile))
stopLongitude=$(($startLongitude + $lonSize - 1))

latTiles=2000
latCellsPerTile=2000
latSize=$(($latTiles * $latCellsPerTile))
stopLatitude=$(($startLatitude + $latSize - 1))

iquery -r /dev/null -avq "
    apply(
      join(
        attribute_rename(
          between(
            band_1_measurements,
            $startLongitude, $startLatitude, $startTime, $platform, $resolution,
            $stopLongitude, $stopLatitude, $stopTime, $platform, $resolution
          ),
          reflectance,
          red
        ),
        attribute_rename(
          between(
            band_2_measurements,
            $startLongitude, $startLatitude, $startTime, $platform, $resolution,
            $stopLongitude, $stopLatitude, $stopTime, $platform, $resolution
          ),
          reflectance,
          nir
        )
      ),
      x, longitude_e4 - $startLongitude + 1,
      y, latitude_e4 - $startLatitude + 1,
      ndvi, uint8((((nir - red) / (nir + red)) * 200) + 50)
    )"
