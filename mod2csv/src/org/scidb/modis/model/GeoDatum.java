package org.scidb.modis.model;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class GeoDatum {

    public GranuleMetadata metadata;
    public float longitude;
    public float latitude;
    public int trackIndex;
    public int scanIndex;
    public short height;
    public short sensorZenith;
    public short sensorAzimuth;
    public int range;
    public short solarZenith;
    public short solarAzimuth;
    public short landSeaMask;
    private static final DecimalFormat twoDigits = new DecimalFormat("0.##");

    static {
        twoDigits.setRoundingMode(RoundingMode.HALF_UP);
    }

    public static String getCSVHeader() {
        return "longitude_e4,latitude_e4,start_time,platform_id,resolution_id,track_index,scan_index,height,sensor_zenith,sensor_azimuth,range,solar_zenith,solar_azimuth,land_sea_mask";
    }

    public String toCSV() {
        if (longitude < -180 || longitude > 180) {
            return "";
        }
        if (latitude < -90 || latitude > 90) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Math.round(longitude * 10000));
        sb.append(",");
        sb.append(Math.round(latitude * 10000));
        sb.append(",");
        sb.append(metadata.startTime);
        sb.append(",");
        sb.append(metadata.platform.ordinal());
        sb.append(",");
        sb.append(metadata.resolution.ordinal());
        sb.append(",");
        sb.append(trackIndex);
        sb.append(",");
        sb.append(scanIndex);
        sb.append(",");
        sb.append(height);
        sb.append(",");
        sb.append(twoDigits.format(sensorZenith * 0.01d));
        sb.append(",");
        sb.append(twoDigits.format(sensorAzimuth * 0.01d));
        sb.append(",");
        sb.append(range * 25);
        sb.append(",");
        sb.append(twoDigits.format(solarZenith * 0.01d));
        sb.append(",");
        sb.append(twoDigits.format(solarAzimuth * 0.01d));
        sb.append(",");
        sb.append(landSeaMask);
        return sb.toString();
    }
}
