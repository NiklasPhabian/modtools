#!/bin/bash

print_usage()
{
  echo 'Usage : pcsv2scidb_batch <out_folder> <num_instances> <max_tasks> <in_folder_1> <in_folder_2> <in_folder_n>'   
}

#IN_FOLDER=$1
OUT_FOLDER=$1
NUM_INSTANCES=$2
MAX_TASKS=$3
LOAD_CHUNK_SIZE=100000
LINES_TO_SKIP=1
BANDS=( "1" "2" "3" "4" "5" "6" "7" "8" "9" "10" "11" "12" "13lo" "13hi" "14lo" "14hi" "15" "16" "17" "18" "19" "20" "21" "22" "23" "24" "25" "26" "27" "28" "29" "30" "31" "32" "33" "34" "35" "36" )

waitn() {
        local N=$1
        while [[ $(jobs | grep "Running" | wc -l) -ge $N ]]; do
                sleep 0.2;
        done
}

csv_to_dlf() {
	IN_FOLDER=$1
	CSV_FILE=$2
	DLF_FILE=$3
	BASENAME=$(basename $IN_FOLDER)	
	if [ -e $IN_FOLDER/$CSV_FILE ]; then
        	./pcsv2scidb.sh $OUT_FOLDER/$BASENAME $NUM_INSTANCES $LOAD_CHUNK_SIZE $LINES_TO_SKIP $IN_FOLDER/$CSV_FILE $DLF_FILE
	fi
}

process_csv() {
	IN_FOLDER=$1
	csv_to_dlf $IN_FOLDER "Granule_Metadata.csv" "Granule_Metadata.dlf" &
	waitn $MAX_TASKS
	csv_to_dlf $IN_FOLDER "Band_Metadata.csv" "Band_Metadata.dlf" &
	waitn $MAX_TASKS
	csv_to_dlf $IN_FOLDER "Geodata.csv" "Geodata.dlf" &
 	waitn $MAX_TASKS
	for BAND in "${BANDS[@]}"; do
		CSV_FILE="Band_"$BAND"_Measurements.csv"
		DLF_FILE="Band_"$BAND"_Measurements.dlf"
		csv_to_dlf $IN_FOLDER $CSV_FILE $DLF_FILE &
		waitn $MAX_TASKS
	done
	wait
}

if [ "$#" -lt 3 ]; then  
  print_usage
  exit 1
fi

PARAM_NUM=1
for ARG; do 
    
	if [ $PARAM_NUM -gt 3 ]; then        
		if [ -d $ARG ]; then
            
			START=$(date +%s.%N)
			process_csv $ARG
			STOP=$(date +%s.%N)
       	  	ELAPSED=$(echo "$STOP - $START" | bc)
            echo "CSV2DLF,"$ARG","$ELAPSED
		else
			echo "ERROR: Input folder: "$ARG" does not exist."
		fi  
	fi
	let "PARAM_NUM++"
done
