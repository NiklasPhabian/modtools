platform=1
resolution=1
startLongitude_e4=-1180000
stopLongitude_e4=-1080001
startLatitude_e4=250000
stopLatitude_e4=349999
startTime=201204111825
stopTime=201204111825

iquery -anq "remove(rgb)" > /dev/null 2>&1
attrs="<red:double null,green:double null,blue:double null>"
dims="[x=1:100000,10000,0,y=1:100000,10000,0]"
schema=$attrs$dims
iquery -anq "create immutable empty array rgb $schema" > /dev/null 2>&1

echo "Building RGB values..."
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
          join(
            attribute_rename(
              band_4_measurements,
              reflectance,
              green
            ),
            attribute_rename(
              band_3_measurements,
              reflectance,
              blue
            )
          )
        ),
        $startLongitude_e4, $startLatitude_e4, $startTime, $platform, $resolution,
        $stopLongitude_e4, $stopLatitude_e4, $stopTime, $platform, $resolution
      ),
      x, longitude_e4 - $startLongitude_e4 + 1,
      y, latitude_e4 - $startLatitude_e4 + 1
    ),
    rgb,
    avg(red) as red,
    avg(green) as green,
    avg(blue) as blue
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
iquery -aq "count(rgb)"
: <<'END'
echo
echo "Generating RGB grid centers..."
xTiles=500
xCellsPerTile=200
yTiles=500
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
      red, double(null),
      green, double(null),
      blue, double(null)
    ),
    grid_centers
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
echo "Gridding RGB results..."
iquery -anq "remove(rgb_gridded)" > /dev/null 2>&1
xStart=$(echo $xCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')
yStart=$(echo $yCellsPerTile 2 | awk '{printf "%d", int($1 / $2 + 0.5)}')

START=$(date +%s.%N)
iquery -anq "
  store(
    thin(
      window(
        merge(rgb, grid_centers),
        $xBlurWindowSize,
        $yBlurWindowSize,
        avg(red) as red,
        avg(green) as green,
        avg(blue) as blue
      ),
      $xStart, $xCellsPerTile,
      $yStart, $yCellsPerTile
    ),
    rgb_gridded
  )"
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

echo
echo "Exporting results to CSV..."
START=$(date +%s.%N)
iquery -aq "scan(rgb_gridded)" > ~/rgb_gridded.csv
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo $ELAPSED "s"

iquery -anq "remove(grid_centers)" > /dev/null 2>&1
END
