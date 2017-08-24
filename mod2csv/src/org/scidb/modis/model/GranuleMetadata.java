package org.scidb.modis.model;

import org.scidb.modis.constant.PlatformEnum;
import org.scidb.modis.constant.ResolutionEnum;
import PIRL.PVL.Parameter;
import PIRL.PVL.Parser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import org.scidb.modis.util.ModUtil;

public class GranuleMetadata {

    public PlatformEnum platform;
    public long startTime;
    public ResolutionEnum resolution;
    public int numScans;
    public int trackMeasurements;
    public int scanMeasurements;
    public String fileId;
    public String dayNightFlag;
    public String geoFileId;

    public GranuleMetadata(FileFormat mod02File) throws Exception {
        // Get some values out of the core metadata.
        Attribute coreMetadataAttr = ModUtil.getAttribute(mod02File.get("/"), "CoreMetadata.0");
        
        // Some values we need are embedded in PVL format string which is the value of this attribute.
        String[] cmVals = (String[]) coreMetadataAttr.getValue();
        Parser pvl = new Parser(cmVals[0]);
        Parameter inventoryMetadataParam = pvl.Get();

        // Get the associated platform (satellite name).
        Parameter platformParam = inventoryMetadataParam.Find("ASSOCIATEDPLATFORMSHORTNAME/VALUE");
        platform = PlatformEnum.valueOf(platformParam.Value().String_Data());

        // Get the range beginning date.
        Parameter rangeBeginningDateParam = inventoryMetadataParam.Find("RANGEBEGINNINGDATE/VALUE");
        String rangeBeginningDateString = rangeBeginningDateParam.Value().String_Data();

        // Get the range beginning time.
        Parameter rangeBeginningTimeParam = inventoryMetadataParam.Find("RANGEBEGINNINGTIME/VALUE");
        String rangeBeginningTimeString = rangeBeginningTimeParam.Value().String_Data().substring(0, 12);

        // Determine the timestamp value.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date st = sdf.parse(rangeBeginningDateString + " " + rangeBeginningTimeString);
        sdf.applyPattern("yyyyMMddHHmm");
        startTime = Long.parseLong(sdf.format(st));

        // Get the number of scans.
        Attribute numScansAttr = ModUtil.getAttribute(mod02File.get("/"), "Number of Scans");
        int[] nsVals = (int[]) numScansAttr.getValue();
        numScans = nsVals[0];

        // Get the meter resolution and expected data dimensions.
        Parameter shortNameParam = inventoryMetadataParam.Find("SHORTNAME/VALUE");
        String shortName = shortNameParam.Value().String_Data();
        if (shortName.endsWith("QKM")) {
            resolution = ResolutionEnum.QKM;
        } else if (shortName.endsWith("HKM")) {
            resolution = ResolutionEnum.HKM;
        } else if (shortName.endsWith("1KM")) {
            resolution = ResolutionEnum.FKM;
        } else {
            throw new Exception("Unrecognized short name convention.");
        }
        trackMeasurements = resolution.trackDetectors * numScans;
        scanMeasurements = resolution.scanLength;

        // Get the ID that NASA uses for the granule.
        Parameter fileIdParam = inventoryMetadataParam.Find("LOCALGRANULEID/VALUE");
        fileId = fileIdParam.Value().String_Data();

        // Get the day/night flag value.
        Parameter dayNightFlagParam = inventoryMetadataParam.Find("DAYNIGHTFLAG/VALUE");
        dayNightFlag = dayNightFlagParam.Value().String_Data();

        // Get the ID for the corresponding geolocation data file.
        Parameter geoFileIdParam = inventoryMetadataParam.Find("ANCILLARYINPUTPOINTER/VALUE");
        geoFileId = geoFileIdParam.Value().String_Data();
    }

    public static String getCSVHeader() {
        return "start_time,platform_id,resolution_id,scans,track_measurements,scan_measurements,day_night_flag,file_id,geo_file_id";
    }

    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(startTime);
        sb.append(",");
        sb.append(platform.ordinal());
        sb.append(",");
        sb.append(resolution.ordinal());
        sb.append(",");
        sb.append(numScans);
        sb.append(",");
        sb.append(trackMeasurements);
        sb.append(",");
        sb.append(scanMeasurements);
        sb.append(",\"");
        sb.append(dayNightFlag);
        sb.append("\",\"");
        sb.append(fileId);
        sb.append("\",\"");
        sb.append(geoFileId);
        sb.append("\"");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Selected mod02 metadata:");
        sb.append("\n  - startTime = ");
        sb.append(startTime);
        sb.append("\n  - platform = ");
        sb.append(platform);
        sb.append("\n  - resolution = ");
        sb.append(resolution);
        sb.append("\n  - scans = ");
        sb.append(numScans);
        sb.append("\n  - scanMeasurements = ");
        sb.append(scanMeasurements);
        sb.append("\n  - trackMeasurements = ");
        sb.append(trackMeasurements);
        sb.append("\n  - dayNightFlag = ");
        sb.append(dayNightFlag);
        sb.append("\n  - fileId = ");
        sb.append(fileId);
        sb.append("\n  - geoFileId = ");
        sb.append(geoFileId);
        return sb.toString();
    }
}
