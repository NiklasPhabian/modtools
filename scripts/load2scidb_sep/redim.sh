redim_array() {
    IN_ARRAY=$1
    OUT_ARRAY=${IN_ARRAY//load_/}
    OUT_ARRAY_SHOW=$(iquery -aq "show($OUT_ARRAY);")
    OUT_ARRAY_DIM=${OUT_ARRAY_SHOW#*$OUT_ARRAY}
    OUT_ARRAY_DIM=${OUT_ARRAY_DIM//\'/}
    iquery -anq "STORE(REDIMENSION($IN_ARRAY, $OUT_ARRAY_DIM), $OUT_ARRAY);" > /dev/null &
}


redim_all_data() {
    local MAX_TASKS=$1        
        
    redim_array "load_granule_metadata" 
        waitn $MAX_TASKS

    redim_array "load_band_metadata"
        waitn $MAX_TASKS
    
    redim_array "load_geodata"
    	waitn $MAX_TASKS

        for BAND in "${BANDS[@]}"
        do
		DLF_FILE="Band_"$BAND"_Measurements.dlf"
                LOAD_NAME="load_band_"$BAND"_measurements"
                TARGET_NAME="band_"$BAND"_measurements"
		if [ -e $SCIDB_FOLDER/0/$DLF_FILE ]; then
                    redim_array $LOAD_NAME
                	waitn $MAX_TASKS
		fi
        done
        wait
} 



START=$(date +%s.%N)
redim_all_data $MAX_TASKS
STOP=$(date +%s.%N)
ELAPSED=$(echo "$STOP - $START" | bc)
echo "REDIM,$IN_FOLDER,$ELAPSED"

remove_load_arrays
