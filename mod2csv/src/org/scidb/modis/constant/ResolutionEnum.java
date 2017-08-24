package org.scidb.modis.constant;

import java.util.EnumSet;

public enum ResolutionEnum {

    QKM("250m", 40, 5416, EnumSet.range(BandEnum.b1, BandEnum.b2)),
    HKM("500m", 20, 2708, EnumSet.range(BandEnum.b1, BandEnum.b7)),
    FKM("1km", 10, 1354, EnumSet.allOf(BandEnum.class));
    private final String name;
    public final int trackDetectors;
    public final int scanLength;
    public final EnumSet<BandEnum> bands;

    ResolutionEnum(String name, int trackDetectors, int scanLength, EnumSet<BandEnum> bands) {
        this.name = name;
        this.trackDetectors = trackDetectors;
        this.scanLength = scanLength;
        this.bands = bands;
    }

    @Override
    public String toString() {
        return name;
    }
}