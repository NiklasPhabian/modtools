#!/bin/bash

NUM_INSTANCES=2
LOAD_CHUNK_SIZE=100000
SCIDB_FOLDER=/home/scidb/mydb-DB/0/
BANDS=( "1" "2" "3" "4" "5" "6" "7" "8" "9" "10" "11" "12" "13lo" "13hi" "14lo" "14hi" "15" "16" "17" "18" "19" "20" "21" "22" "23" "24" "25" "26" "27" "28" "29" "30" "31" "32" "33" "34" "35" "36" )

BANDS=("20")

waitn() {
        local N=$1
        while [[ $(jobs | grep "Running" | wc -l) -ge $N ]];
        do
                sleep 0.2;
        done
}

copy_load_files() {
	local IN_FOLDER=$1
	cp -r $IN_FOLDER/* $SCIDB_FOLDER > /dev/null
}

remove_array() {
	local ARRAY_NAME=$1
	iquery -anq "remove("$ARRAY_NAME")" > /dev/null 2>&1
}

remove_load_arrays() {
	remove_array "load_granule_metadata"
	remove_array "load_band_metadata"
	remove_array "load_geodata"
        for BAND in "${BANDS[@]}"
        do
                remove_array "load_band_"$BAND"_measurements"
        done
}

remove_load_files() {
	for ((i=0;i<$NUM_INSTANCES;i++)) do
		rm $SCIDB_FOLDER/$i/*.dlf > /dev/null 2>&1
	done
}

load_file() {
        local DLF_FILE=$1
        local LOAD_NAME=$2
        local LOAD_ATTRS=$3
        local LOAD_DIMS="[i=0:*,"$LOAD_CHUNK_SIZE",0]"
        if [ -e $SCIDB_FOLDER/0/$DLF_FILE ]; then
                iquery -anq "CREATE ARRAY $LOAD_NAME $LOAD_ATTRS $LOAD_DIMS" >  /dev/null
                iquery -anq "LOAD("$LOAD_NAME",'"$DLF_FILE"',-1)" > /dev/null
        fi
}

load_all_data() {
	local MAX_TASKS=$1

	local DLF_FILE="Granule_Metadata.dlf"
	local LOAD_NAME
	local LOAD_ATTRS
	
        DLF_FILE="Granule_Metadata.dlf"
        LOAD_NAME="load_granule_metadata"
        LOAD_ATTRS="<start_time:int64,platform_id:int64,resolution_id:int64,scans:uint8,track_measurements:uint16,scan_measurements:uint16,day_night_flag:string,file_id:string,geo_file_id:string>"
        load_file $DLF_FILE $LOAD_NAME $LOAD_ATTRS &
	waitn $MAX_TASKS

        DLF_FILE="Band_Metadata.dlf"
        LOAD_NAME="load_band_metadata"
        LOAD_ATTRS="<start_time:int64,platform_id:int64,resolution_id:int64,band_id:int64,radiance_scale:double,radiance_offset:float,reflectance_scale:double,reflectance_offset:float,corrected_counts_scale:double,corrected_counts_offset:float,specified_uncertainty:float,uncertainty_scaling_factor:float>"
        load_file $DLF_FILE $LOAD_NAME $LOAD_ATTRS &
	waitn $MAX_TASKS

        DLF_FILE="Geodata.dlf"
        LOAD_NAME="load_geodata"
        LOAD_ATTRS="<longitude_e4:int64,latitude_e4:int64,start_time:int64,platform_id:int64,resolution_id:int64,track_index:int16,scan_index:int16,height:int16,sensor_zenith:float,sensor_azimuth:float,range:uint32,solar_zenith:float,solar_azimuth:float,land_sea_mask:uint8>"
        load_file $DLF_FILE $LOAD_NAME $LOAD_ATTRS &
	waitn $MAX_TASKS

	for BAND in "${BANDS[@]}"
	do
		DLF_FILE="Band_"$BAND"_Measurements.dlf"
		LOAD_NAME="load_band_"$BAND"_measurements"
		LOAD_ATTRS="<longitude_e4:int64,latitude_e4:int64,start_time:int64,platform_id:int64,resolution_id:int64,band_id:int64,si_value:uint16,radiance:double,reflectance:double,uncertainty_index:uint8,uncertainty_pct:float>"
		load_file $DLF_FILE $LOAD_NAME $LOAD_ATTRS &
		waitn $MAX_TASKS
	done
	wait
}


remove_load_arrays
remove_load_files

for IN_FOLDER in $*
        do
        if [ -d $IN_FOLDER ]; then
        	MAX_TASKS=2
        	if [[ $IN_FOLDER == *HKM* ]]; then
        	        MAX_TASKS=7
        	elif [[ $IN_FOLDER == *1KM* ]]; then
        	        MAX_TASKS=7
        	fi

            copy_load_files $IN_FOLDER

            START=$(date +%s.%N)
            load_all_data $MAX_TASKS
            STOP=$(date +%s.%N)
                ELAPSED=$(echo "$STOP - $START" | bc)
                echo "LOAD,$IN_FOLDER,$ELAPSED"
		
            remove_load_files
        else
            echo "ERROR: Folder: "$IN_FOLDER" does not exist."
        fi
done

echo 'loading complete' 
