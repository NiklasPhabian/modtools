package org.scidb.modis.model;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import org.scidb.modis.constant.BandEnum;

public class BandDatum {

    public GranuleMetadata metadata;
    public GeoDatum geoDatum;
    public BandMetadata bandMetadata;
    public BandEnum band;
    public Measurement measurement;
    private static final DecimalFormat threeDigits = new DecimalFormat("0.###");

    static {
        threeDigits.setRoundingMode(RoundingMode.HALF_UP);
    }

    public static String getCSVHeader() {
        return "longitude_e4,latitude_e4,start_time,platform_id,resolution_id,band_id,si_value,radiance,reflectance,uncertainty_index,uncertainty_pct";
    }

    public String toCSV() {
        if (measurement.uncertaintyIndex >= 15 || measurement.siValue > 32767) {
            return "";
        }
        if (geoDatum.longitude < -180 || geoDatum.longitude > 180) {
            return "";
        }
        if (geoDatum.latitude < -90 || geoDatum.latitude > 90) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Math.round(geoDatum.longitude * 10000));
        sb.append(",");
        sb.append(Math.round(geoDatum.latitude * 10000));
        sb.append(",");
        sb.append(metadata.startTime);
        sb.append(",");
        sb.append(metadata.platform.ordinal());
        sb.append(",");
        sb.append(metadata.resolution.ordinal());
        sb.append(",");
        sb.append(band.ordinal());
        sb.append(",");
        sb.append(measurement.siValue);
        sb.append(",");
        sb.append(bandMetadata.radianceScale * (measurement.siValue - bandMetadata.radianceOffset));
        sb.append(",");
        sb.append(bandMetadata.reflectanceScale * (measurement.siValue - bandMetadata.reflectanceOffset));
        sb.append(",");
        sb.append(measurement.uncertaintyIndex);
        sb.append(",");
        sb.append(threeDigits.format(bandMetadata.specifiedUncertainty * Math.exp(measurement.uncertaintyIndex / bandMetadata.uncertaintyScalingFactor)));
        return sb.toString();
    }
}
