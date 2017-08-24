platform=1
resolution=1
startLongitude_e4=-1280000
stopLongitude_e4=-980001
startLatitude_e4=150000
stopLatitude_e4=449999
startTime=201200000000
stopTime=201300000000

iquery -anq "remove(ndsi)" > /dev/null 2>&1
attrs="<ndsi:double null,snowvote:double null>"
dims="[x=1:300000,10000,0,y=1:300000,10000,0]"
schema=$attrs$dims
iquery -anq "create immutable empty array ndsi $schema" > /dev/null 2>&1

echo "Calculating NDSI values..."
START=$(date +%s.%N)
iquery -anq "
  redimension_store(
    apply(
      apply(
        between(
          join(
            attribute_rename(
              band_2_measurements,
              reflectance,
              band2
            ),
            join(
              attribute_rename(
                band_4_measurements,
                reflectance,
                band4
              ),
              attribute_rename(
                band_6_measurements,
                reflectance,
                band6
              )
            )
          ),
          $startLongitude_e4, $startLatitude_e4, $startTime, $platform, $resolution,
          $stopLongitude_e4, $stopLatitude_e4, $stopTime, $platform, $resolution
        ),
        x, longitude_e4 - $startLongitude_e4 + 1,
        y, latitude_e4 - $startLatitude_e4 + 1,
        ndsi, (band4 - band6) / (band4 + band6)
      ),
      snowvote, iif(ndsi > 0.4 and band2 > 0.11 and band4 > 0.1, 1.0, 0.0)
    ),
    ndsi,
    max(ndsi) as ndsi,
    max(snowvote) as snowvote
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
iquery -aq "count(ndsi)"
: <<'END'
echo
echo "Generating NDSI grid centers..."
xTiles=1500
xCellsPerTile=200
yTiles=1500
yCellsPerTile=200

overlapCells=50
xBlurWindowSize=$(($xCellsPerTile + $overlapCells))
yBlurWindowSize=$(($yCellsPerTile + $overlapCells))

iquery -anq "remove(grid_centers)" > /dev/null 2>&1
iquery -anq "create immutable empty array grid_centers $schema" > /dev/null 2>&1

START=$(date +%s.%N)
iquery -anq "
  redimension_store(
    apply(
      cross(
        build(<x:int64>[i=1:$xTiles,$xTiles,0], int64($xCellsPerTile * (i - 0.5) + 0.5)),
        build(<y:int64>[i=1:$yTiles,$yTiles,0], int64($yCellsPerTile * (i - 0.5) + 0.5))
      ),
      ndsi, double(null),
      snowvote, double(null)
    ),
    grid_centers
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
echo "Gridding NDSI results..."
iquery -anq "remove(ndsi_gridded)" > /dev/null 2>&1
xStart=$(echo $xCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')
yStart=$(echo $yCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')

START=$(date +%s.%N)
iquery -anq "
  store(
    thin(
      window(
        merge(ndsi, grid_centers),
        $xBlurWindowSize,
        $yBlurWindowSize,
        avg(ndsi) as ndsi,
        avg(snowvote) as snowvote
      ),
      $xStart, $xCellsPerTile,
      $yStart, $yCellsPerTile
    ),
    ndsi_gridded
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
echo "Exporting results to CSV..."
START=$(date +%s.%N)
iquery -aq "scan(ndsi_gridded)" > ~/ndsi_gridded.csv
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

iquery -anq "remove(grid_centers)" > /dev/null 2>&1
END
