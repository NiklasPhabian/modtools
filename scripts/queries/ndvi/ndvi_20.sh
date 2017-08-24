platform=1
resolution=1
startLongitude_e4=-1230000
stopLongitude_e4=-1030001
startLatitude_e4=200000
stopLatitude_e4=399999
startTime=201200000000
stopTime=201300000000

iquery -anq "remove(ndvi)" > /dev/null 2>&1
attrs="<ndvi:double null>"
dims="[x=1:200000,10000,0,y=1:200000,10000,0]"
schema=$attrs$dims
iquery -anq "create immutable empty array ndvi $schema" > /dev/null 2>&1

echo "Calculating NDVI values..."
START=$(date +%s.%N)
iquery -anq "
  redimension_store(
    apply(
      between(
        join(
          attribute_rename(
            band_1_measurements,
            reflectance,
            red
          ),
          attribute_rename(
            band_2_measurements,
            reflectance,
            nir
          )
        ),
        $startLongitude_e4, $startLatitude_e4, $startTime, $platform, $resolution,
        $stopLongitude_e4, $stopLatitude_e4, $stopTime, $platform, $resolution
      ),
      x, longitude_e4 - $startLongitude_e4 + 1,
      y, latitude_e4 - $startLatitude_e4 + 1,
      ndvi, (nir - red) / (nir + red)
    ),
    ndvi,
    max(ndvi) as ndvi
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
iquery -aq "count(ndvi)"

echo
echo "Generating NDVI grid centers..."
xTiles=1000
xCellsPerTile=200
yTiles=1000
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
      ndvi, double(null)
    ),
    grid_centers
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
echo "Gridding NDVI results..."
iquery -anq "remove(ndvi_gridded)" > /dev/null 2>&1
xStart=$(echo $xCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')
yStart=$(echo $yCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')

START=$(date +%s.%N)
iquery -anq "
  store(
    thin(
      window(
        merge(ndvi, grid_centers),
        $xBlurWindowSize,
        $yBlurWindowSize,
        avg(ndvi) as ndvi
      ),
      $xStart, $xCellsPerTile,
      $yStart, $yCellsPerTile
    ),
    ndvi_gridded
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"
: '
echo
echo "Exporting results to CSV..."
START=$(date +%s.%N)
iquery -aq "scan(ndvi_gridded)" > ~/ndvi_gridded.csv
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"
'
iquery -anq "remove(grid_centers)" > /dev/null 2>&1
