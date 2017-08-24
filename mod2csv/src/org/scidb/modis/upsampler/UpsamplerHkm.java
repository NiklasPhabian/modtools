package org.scidb.modis.upsampler;

import org.apache.log4j.Logger;
import org.scidb.modis.constant.ResolutionEnum;
import org.scidb.modis.model.GeoDatum;
import org.scidb.modis.util.LogUtil;

public class UpsamplerHkm extends Upsampler {

    private static final Logger log = LogUtil.getLogger();
    private static final int hkmValuesPerScan = ResolutionEnum.HKM.trackDetectors * ResolutionEnum.HKM.scanLength;
    private int hkmBaseTrackIndex;
    private GeoDatum[] fkmGeoData;
    private GeoDatum[] hkmGeoData;

    @Override
    public GeoDatum[] processScan(int scanNumber, GeoDatum[] fkmScanGeoData) {
        this.hkmGeoData = new GeoDatum[hkmValuesPerScan];
        this.hkmBaseTrackIndex = scanNumber * ResolutionEnum.HKM.trackDetectors;
        this.fkmGeoData = fkmScanGeoData;
        for (int fkmTrackOffset = 0; fkmTrackOffset < ResolutionEnum.FKM.trackDetectors - 1; fkmTrackOffset++) {
            int fkmTrackIndex = fkmTrackOffset * ResolutionEnum.FKM.scanLength;
            boolean isLeadingBlock = true;
            for (int fkmScanOffset = 0; fkmScanOffset < ResolutionEnum.FKM.scanLength - 1; fkmScanOffset++) {
                int fkmBlockIndex = fkmTrackIndex + fkmScanOffset;
                interpolateBlock(fkmBlockIndex, isLeadingBlock);
                isLeadingBlock = false;
            }
        }
        extrapolateEdges();
        return hkmGeoData;
    }

    private void interpolateBlock(int fkmBlockIndex, boolean isLeadingBlock) {
        // Get the upper control row.
        int ulIndex = fkmBlockIndex;
        GeoDatum upperLeft = fkmGeoData[ulIndex];
        int urIndex = ulIndex + 1;
        GeoDatum upperRight = fkmGeoData[urIndex];
        GeoDatum[] upperControlRow = getControlRow(upperLeft, upperRight);

        // Get the lower control row.
        int llIndex = fkmBlockIndex + ResolutionEnum.FKM.scanLength;
        GeoDatum lowerLeft = fkmGeoData[llIndex];
        int lrIndex = llIndex + 1;
        GeoDatum lowerRight = fkmGeoData[lrIndex];
        GeoDatum[] lowerControlRow = getControlRow(lowerLeft, lowerRight);

        // Get the hkmBlockIndex 
        int fkmTrackIndex = fkmBlockIndex / ResolutionEnum.FKM.scanLength;
        int fkmScanOffset = fkmBlockIndex % ResolutionEnum.FKM.scanLength;
        int hkmTrackIndex = (2 * fkmTrackIndex + 1) * ResolutionEnum.HKM.scanLength;
        int hkmBlockIndex = hkmTrackIndex + (2 * fkmScanOffset);

        // Fill in the first column if this is a leftmost block.
        if (isLeadingBlock) {
            fillDataColumn(upperControlRow[0], lowerControlRow[0], hkmBlockIndex);
        }
        for (int i = 1; i < 3; i++) {
            fillDataColumn(upperControlRow[i], lowerControlRow[i], hkmBlockIndex + i);
        }
    }

    private GeoDatum[] getControlRow(GeoDatum value1, GeoDatum value2) {
        GeoDatum[] controlRow = new GeoDatum[5];
        controlRow[0] = value1;
        controlRow[1] = Upsampler.interpolate(value1, value2, 0.5f);
        controlRow[2] = value2;
        return controlRow;
    }

    private void fillDataColumn(GeoDatum value1, GeoDatum value2, int hkmColumnIndex) {
        for (int hkmRowOffset = 0; hkmRowOffset < 2; hkmRowOffset++) {
            int hkmValueIndex = hkmColumnIndex + (hkmRowOffset * ResolutionEnum.HKM.scanLength);
            float relativeOffset = ((2.0f * hkmRowOffset) + 1.0f) / 4.0f;
            GeoDatum result = Upsampler.interpolate(value1, value2, relativeOffset);
            result.scanIndex = hkmValueIndex % ResolutionEnum.HKM.scanLength;
            result.trackIndex = hkmBaseTrackIndex + (hkmValueIndex / ResolutionEnum.HKM.scanLength);
            hkmGeoData[hkmValueIndex] = result;
        }
    }

    private void extrapolateEdges() {
        // Extrapolate top track.
        int hkmTrack1Index = ResolutionEnum.HKM.scanLength;
        int hkmTrack2Index = 2 * ResolutionEnum.HKM.scanLength;

        for (int hkmScanOffset = 0; hkmScanOffset < ResolutionEnum.HKM.scanLength - 1; hkmScanOffset++) {
            GeoDatum control2 = hkmGeoData[hkmTrack1Index + hkmScanOffset];
            GeoDatum control1 = hkmGeoData[hkmTrack2Index + hkmScanOffset];

            GeoDatum result = Upsampler.extrapolate(control1, control2, 1);
            result.trackIndex = hkmBaseTrackIndex;
            result.scanIndex = hkmScanOffset;
            hkmGeoData[hkmScanOffset] = result;
        }

        // Extrapolate bottom track.
        int hkmTrack17Index = 17 * ResolutionEnum.HKM.scanLength;
        int hkmTrack18Index = 18 * ResolutionEnum.HKM.scanLength;
        int hkmTrack19Index = 19 * ResolutionEnum.HKM.scanLength;

        for (int hkmScanOffset = 0; hkmScanOffset < ResolutionEnum.HKM.scanLength - 1; hkmScanOffset++) {
            GeoDatum control1 = hkmGeoData[hkmTrack17Index + hkmScanOffset];
            GeoDatum control2 = hkmGeoData[hkmTrack18Index + hkmScanOffset];

            GeoDatum result = Upsampler.extrapolate(control1, control2, 1);
            result.trackIndex = hkmBaseTrackIndex + 19;
            result.scanIndex = hkmScanOffset;
            hkmGeoData[hkmTrack19Index + hkmScanOffset] = result;
        }

        // Extrapolate right columns.
        int hkmValue1Index = ResolutionEnum.HKM.scanLength - 3;
        int hkmValue2Index = ResolutionEnum.HKM.scanLength - 2;
        int hkmResultScanOffset = ResolutionEnum.HKM.scanLength - 1;

        for (int hkmTrackOffset = 0; hkmTrackOffset < 20; hkmTrackOffset++) {
            int hkmTrackIndex = hkmTrackOffset * ResolutionEnum.HKM.scanLength;
            GeoDatum control1 = hkmGeoData[hkmTrackIndex + hkmValue1Index];
            GeoDatum control2 = hkmGeoData[hkmTrackIndex + hkmValue2Index];

            GeoDatum result = Upsampler.extrapolate(control1, control2, 1);
            result.trackIndex = hkmBaseTrackIndex + hkmTrackOffset;
            result.scanIndex = hkmResultScanOffset;
            hkmGeoData[hkmTrackIndex + hkmResultScanOffset] = result;
        }
    }
}
