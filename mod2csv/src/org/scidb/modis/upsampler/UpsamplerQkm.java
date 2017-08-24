package org.scidb.modis.upsampler;

import org.apache.log4j.Logger;
import org.scidb.modis.constant.ResolutionEnum;
import org.scidb.modis.model.GeoDatum;
import org.scidb.modis.util.LogUtil;

public class UpsamplerQkm extends Upsampler {

    private static final Logger log = LogUtil.getLogger();
    private static final int qkmValuesPerScan = ResolutionEnum.QKM.trackDetectors * ResolutionEnum.QKM.scanLength;
    private int qkmBaseTrackIndex;
    private GeoDatum[] fkmGeoData;
    private GeoDatum[] qkmGeoData;

    @Override
    public GeoDatum[] processScan(int scanNumber, GeoDatum[] fkmScanGeoData) {
        this.qkmGeoData = new GeoDatum[qkmValuesPerScan];
        this.qkmBaseTrackIndex = scanNumber * ResolutionEnum.QKM.trackDetectors;
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
        return qkmGeoData;
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

        // Get the qkmBlockIndex 
        int fkmTrackIndex = fkmBlockIndex / ResolutionEnum.FKM.scanLength;
        int fkmScanOffset = fkmBlockIndex % ResolutionEnum.FKM.scanLength;
        int qkmTrackIndex = (4 * fkmTrackIndex + 2) * ResolutionEnum.QKM.scanLength;
        int qkmBlockIndex = qkmTrackIndex + (4 * fkmScanOffset);

        // Fill in the first column if this is a leftmost block.
        if (isLeadingBlock) {
            fillDataColumn(upperControlRow[0], lowerControlRow[0], qkmBlockIndex);
        }
        for (int i = 1; i < 5; i++) {
            fillDataColumn(upperControlRow[i], lowerControlRow[i], qkmBlockIndex + i);
        }
    }

    private GeoDatum[] getControlRow(GeoDatum value1, GeoDatum value2) {
        GeoDatum[] controlRow = new GeoDatum[5];
        controlRow[0] = value1;
        controlRow[1] = Upsampler.interpolate(value1, value2, 0.25f);
        controlRow[2] = Upsampler.interpolate(value1, value2, 0.50f);
        controlRow[3] = Upsampler.interpolate(value1, value2, 0.75f);
        controlRow[4] = value2;
        return controlRow;
    }

    private void fillDataColumn(GeoDatum value1, GeoDatum value2, int qkmColumnIndex) {
        for (int qkmRowOffset = 0; qkmRowOffset < 4; qkmRowOffset++) {
            int qkmValueIndex = qkmColumnIndex + (qkmRowOffset * ResolutionEnum.QKM.scanLength);
            float relativeOffset = ((2.0f * qkmRowOffset) + 1.0f) / 8.0f;
            GeoDatum result = Upsampler.interpolate(value1, value2, relativeOffset);
            result.scanIndex = qkmValueIndex % ResolutionEnum.QKM.scanLength;
            result.trackIndex = qkmBaseTrackIndex + (qkmValueIndex / ResolutionEnum.QKM.scanLength);
            qkmGeoData[qkmValueIndex] = result;
        }
    }

    private void extrapolateEdges() {
        // Extrapolate top tracks.
        int qkmTrack1Index = ResolutionEnum.QKM.scanLength;
        int qkmTrack2Index = 2 * ResolutionEnum.QKM.scanLength;
        int qkmTrack3Index = 3 * ResolutionEnum.QKM.scanLength;

        for (int qkmScanOffset = 0; qkmScanOffset < ResolutionEnum.QKM.scanLength - 3; qkmScanOffset++) {
            GeoDatum control2 = qkmGeoData[qkmTrack2Index + qkmScanOffset];
            GeoDatum control1 = qkmGeoData[qkmTrack3Index + qkmScanOffset];

            GeoDatum result1 = Upsampler.extrapolate(control1, control2, 2);
            result1.trackIndex = qkmBaseTrackIndex;
            result1.scanIndex = qkmScanOffset;
            qkmGeoData[qkmScanOffset] = result1;

            GeoDatum result2 = Upsampler.extrapolate(control1, control2, 1);
            result2.trackIndex = qkmBaseTrackIndex + 1;
            result2.scanIndex = qkmScanOffset;
            qkmGeoData[qkmTrack1Index + qkmScanOffset] = result2;
        }

        // Extrapolate bottom tracks.
        int qkmTrack36Index = 36 * ResolutionEnum.QKM.scanLength;
        int qkmTrack37Index = 37 * ResolutionEnum.QKM.scanLength;
        int qkmTrack38Index = 38 * ResolutionEnum.QKM.scanLength;
        int qkmTrack39Index = 39 * ResolutionEnum.QKM.scanLength;

        for (int qkmScanOffset = 0; qkmScanOffset < ResolutionEnum.QKM.scanLength - 3; qkmScanOffset++) {
            GeoDatum control1 = qkmGeoData[qkmTrack36Index + qkmScanOffset];
            GeoDatum control2 = qkmGeoData[qkmTrack37Index + qkmScanOffset];

            GeoDatum result1 = Upsampler.extrapolate(control1, control2, 1);
            result1.trackIndex = qkmBaseTrackIndex + 38;
            result1.scanIndex = qkmScanOffset;
            qkmGeoData[qkmTrack38Index + qkmScanOffset] = result1;

            GeoDatum result2 = Upsampler.extrapolate(control1, control2, 2);
            result2.trackIndex = qkmBaseTrackIndex + 39;
            result2.scanIndex = qkmScanOffset;
            qkmGeoData[qkmTrack39Index + qkmScanOffset] = result2;
        }

        // Extrapolate right columns.
        int qkmValue1Index = ResolutionEnum.QKM.scanLength - 5;
        int qkmValue2Index = ResolutionEnum.QKM.scanLength - 4;
        int qkmResult1ScanOffset = ResolutionEnum.QKM.scanLength - 3;
        int qkmResult2ScanOffset = ResolutionEnum.QKM.scanLength - 2;
        int qkmResult3ScanOffset = ResolutionEnum.QKM.scanLength - 1;

        for (int qkmTrackOffset = 0; qkmTrackOffset < 40; qkmTrackOffset++) {
            int qkmTrackIndex = qkmTrackOffset * ResolutionEnum.QKM.scanLength;
            GeoDatum control1 = qkmGeoData[qkmTrackIndex + qkmValue1Index];
            GeoDatum control2 = qkmGeoData[qkmTrackIndex + qkmValue2Index];

            GeoDatum result1 = Upsampler.extrapolate(control1, control2, 1);
            result1.trackIndex = qkmBaseTrackIndex + qkmTrackOffset;
            result1.scanIndex = qkmResult1ScanOffset;
            qkmGeoData[qkmTrackIndex + qkmResult1ScanOffset] = result1;

            GeoDatum result2 = Upsampler.extrapolate(control1, control2, 2);
            result2.trackIndex = qkmBaseTrackIndex + qkmTrackOffset;
            result2.scanIndex = qkmResult2ScanOffset;
            qkmGeoData[qkmTrackIndex + qkmResult2ScanOffset] = result2;

            GeoDatum result3 = Upsampler.extrapolate(control1, control2, 3);
            result3.trackIndex = qkmBaseTrackIndex + qkmTrackOffset;
            result3.scanIndex = qkmResult3ScanOffset;
            qkmGeoData[qkmTrackIndex + qkmResult3ScanOffset] = result3;
        }
    }
}
