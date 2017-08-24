package org.scidb.modis.model;

import org.scidb.modis.constant.BandEnum;

public class BandMetadata implements Comparable<BandMetadata> {

    public GranuleMetadata metadata;
    public BandEnum band;
    public float radianceScale;
    public float radianceOffset;
    public float reflectanceScale;
    public float reflectanceOffset;
    public float correctedCountsScale;
    public float correctedCountsOffset;
    public float specifiedUncertainty;
    public float uncertaintyScalingFactor;

    public static String getCSVHeader() {
        return "start_time,platform_id,resolution_id,band_id,radiance_scale,radiance_offset,reflectance_scale,reflectance_offset,corrected_counts_scale,corrected_counts_offset,specified_uncertainty,uncertainty_scaling_factor";
    }

    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(metadata.startTime);
        sb.append(",");
        sb.append(metadata.platform.ordinal());
        sb.append(",");
        sb.append(metadata.resolution.ordinal());
        sb.append(",");
        sb.append(band.ordinal());
        sb.append(",");
        sb.append(radianceScale);
        sb.append(",");
        sb.append(radianceOffset);
        sb.append(",");
        sb.append(reflectanceScale);
        sb.append(",");
        sb.append(reflectanceOffset);
        sb.append(",");
        sb.append(correctedCountsScale);
        sb.append(",");
        sb.append(correctedCountsOffset);
        sb.append(",");
        sb.append(specifiedUncertainty);
        sb.append(",");
        sb.append(uncertaintyScalingFactor);
        return sb.toString();
    }

    @Override
    public int compareTo(BandMetadata other) {
        if (this.metadata.startTime < other.metadata.startTime) {
            return -1;
        } else if (this.metadata.startTime == other.metadata.startTime) {
            if (this.band.ordinal() < other.band.ordinal()) {
                return -1;
            } else if (this.band.ordinal() == other.band.ordinal()) {
                return 0;
            }
        }
        return 1;
    }
}
