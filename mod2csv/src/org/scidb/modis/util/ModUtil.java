package org.scidb.modis.util;

import org.scidb.modis.model.GranuleMetadata;
import org.scidb.modis.model.Measurement;
import org.scidb.modis.model.GeoDatum;
import org.scidb.modis.constant.BandEnum;
import org.scidb.modis.constant.ResolutionEnum;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;
import org.scidb.modis.model.BandMetadata;
import org.scidb.modis.model.EvDatasets;
import org.scidb.modis.model.EvMetadata;

public class ModUtil {

    private static EvDatasets evDatasets;

    public static void init() {
        evDatasets = null;
    }

    public static Map<BandEnum, BandMetadata> loadBandMetadata(GranuleMetadata metadata, FileFormat mod02File) throws Exception {
        EnumMap<BandEnum, BandMetadata> bandMetadata = new EnumMap<BandEnum, BandMetadata>(BandEnum.class);
        EvMetadata evmeta = new EvMetadata(mod02File);
        if (evmeta.ev250_UI_Meta != null && evmeta.ev250_Meta != null) {
            BandEnum[] bands = {BandEnum.b1, BandEnum.b2};
            bandMetadata.putAll(loadBandMetadata(metadata, bands, evmeta.ev250_UI_Meta, evmeta.ev250_Meta));
        }
        if (evmeta.ev500_UI_Meta != null && evmeta.ev500_Meta != null) {
            BandEnum[] bands = {BandEnum.b3, BandEnum.b4, BandEnum.b5, BandEnum.b6, BandEnum.b7};
            bandMetadata.putAll(loadBandMetadata(metadata, bands, evmeta.ev500_UI_Meta, evmeta.ev500_Meta));
        }
        if (evmeta.ev1kmR_UI_Meta != null && evmeta.ev1kmR_Meta != null) {
            BandEnum[] bands = {BandEnum.b8, BandEnum.b9, BandEnum.b10, BandEnum.b11,
                BandEnum.b12, BandEnum.b13lo, BandEnum.b13hi, BandEnum.b14lo, BandEnum.b14hi,
                BandEnum.b15, BandEnum.b16, BandEnum.b17, BandEnum.b18, BandEnum.b19};
            bandMetadata.putAll(loadBandMetadata(metadata, bands, evmeta.ev1kmR_UI_Meta, evmeta.ev1kmR_Meta));
        }
        if (evmeta.ev1kmE_UI_Meta != null && evmeta.ev1kmE_Meta != null) {
            BandEnum[] bands = {BandEnum.b20, BandEnum.b21, BandEnum.b22, BandEnum.b23,
                BandEnum.b24, BandEnum.b25, BandEnum.b27, BandEnum.b28, BandEnum.b29,
                BandEnum.b30, BandEnum.b31, BandEnum.b32, BandEnum.b33, BandEnum.b34,
                BandEnum.b35, BandEnum.b36};
            bandMetadata.putAll(loadBandMetadata(metadata, bands, evmeta.ev1kmE_UI_Meta, evmeta.ev1kmE_Meta));
        }
        if (evmeta.evB26_UI_Meta != null && evmeta.evB26_Meta != null) {
            BandEnum[] bands = {BandEnum.b26};
            bandMetadata.putAll(loadBandMetadata(metadata, bands, evmeta.evB26_UI_Meta, evmeta.evB26_Meta));
        }
        return bandMetadata;
    }

    private static Map<BandEnum, BandMetadata> loadBandMetadata(GranuleMetadata metadata, BandEnum[] bands, List uncertaintyMeta, List valuesMeta) throws Exception {
        EnumMap<BandEnum, BandMetadata> bmList = new EnumMap<BandEnum, BandMetadata>(BandEnum.class);
        float[] specifiedUncertainty = (float[]) (ModUtil.getAttribute(uncertaintyMeta, "specified_uncertainty").getValue());
        float[] uncertaintyScalingFactor = (float[]) (ModUtil.getAttribute(uncertaintyMeta, "scaling_factor").getValue());
        float[] radianceScales = (float[]) (ModUtil.getAttribute(valuesMeta, "radiance_scales").getValue());
        float[] radianceOffsets = (float[]) (ModUtil.getAttribute(valuesMeta, "radiance_offsets").getValue());
        float[] reflectanceScales = null;
        float[] reflectanceOffsets = null;
        float[] correctedCountsScales = null;
        float[] correctedCountsOffsets = null;
        // Not every dataset has these.
        Attribute reflectanceScalesAttr = ModUtil.getAttribute(valuesMeta, "reflectance_scales");
        if (reflectanceScalesAttr != null) {
            reflectanceScales = (float[]) (reflectanceScalesAttr.getValue());
            reflectanceOffsets = (float[]) (ModUtil.getAttribute(valuesMeta, "reflectance_offsets").getValue());
            correctedCountsScales = (float[]) (ModUtil.getAttribute(valuesMeta, "corrected_counts_scales").getValue());
            correctedCountsOffsets = (float[]) (ModUtil.getAttribute(valuesMeta, "corrected_counts_offsets").getValue());
        }
        for (int i = 0; i < bands.length; i++) {
            BandMetadata bm = new BandMetadata();
            bm.metadata = metadata;
            bm.band = bands[i];
            bm.radianceScale = radianceScales == null ? 0 : radianceScales[i];
            bm.radianceOffset = radianceOffsets == null ? 0 : radianceOffsets[i];
            bm.reflectanceScale = reflectanceScales == null ? 0 : reflectanceScales[i];
            bm.reflectanceOffset = reflectanceOffsets == null ? 0 : reflectanceOffsets[i];
            bm.correctedCountsScale = correctedCountsScales == null ? 0 : correctedCountsScales[i];
            bm.correctedCountsOffset = correctedCountsOffsets == null ? 0 : correctedCountsOffsets[i];
            bm.specifiedUncertainty = specifiedUncertainty == null ? 0 : specifiedUncertainty[i];
            bm.uncertaintyScalingFactor = uncertaintyScalingFactor == null ? 0 : uncertaintyScalingFactor[i];
            bmList.put(bm.band, bm);
        }
        return bmList;
    }

    public static Map<Integer, GeoDatum[]> loadGeoData(GranuleMetadata metadata, FileFormat mod03File) throws Exception {
        float[] longitudeVals = null;
        float[] latitudeVals = null;
        short[] heightVals = null;
        short[] sensorZenithVals = null;
        short[] sensorAzimuthVals = null;
        int[] rangeVals = null;
        short[] solarZenithVals = null;
        short[] solarAzimuthVals = null;
        short[] landSeaMaskVals = null;

        Group geoFieldsGroup = (Group) mod03File.get("/MODIS_Swath_Type_GEO/Geolocation Fields");
        List<HObject> geoFieldList = geoFieldsGroup.getMemberList();
        for (HObject geoField : geoFieldList) {
            String geoFieldName = geoField.getName();
            if ("Longitude".equals(geoFieldName)) {
                Dataset longitudeDS = (Dataset) geoField;
                longitudeDS.init();
                longitudeVals = (float[]) longitudeDS.getData();
            } else if ("Latitude".equals(geoFieldName)) {
                Dataset latitudeDS = (Dataset) geoField;
                latitudeDS.init();
                latitudeVals = (float[]) latitudeDS.getData();
            }
        }

        Group dataFieldsGroup = (Group) mod03File.get("/MODIS_Swath_Type_GEO/Data Fields");
        List<HObject> dataFieldList = dataFieldsGroup.getMemberList();
        for (HObject dataField : dataFieldList) {
            String dataFieldName = dataField.getName();
            if ("Height".equals(dataFieldName)) {
                Dataset heightDS = (Dataset) dataField;
                heightDS.init();
                heightVals = (short[]) heightDS.getData();
            } else if ("SensorZenith".equals(dataFieldName)) {
                Dataset sensorZenithDS = (Dataset) dataField;
                sensorZenithDS.init();
                sensorZenithVals = (short[]) sensorZenithDS.getData();
            } else if ("SensorAzimuth".equals(dataFieldName)) {
                Dataset sensorAzimuthDS = (Dataset) dataField;
                sensorAzimuthDS.init();
                sensorAzimuthVals = (short[]) sensorAzimuthDS.getData();
            } else if ("Range".equals(dataFieldName)) {
                Dataset rangeDS = (Dataset) dataField;
                rangeDS.init();
                short[] unsignedVals = (short[]) rangeDS.getData();
                rangeVals = new int[unsignedVals.length];
                Dataset.convertFromUnsignedC(unsignedVals, rangeVals);
            } else if ("SolarZenith".equals(dataFieldName)) {
                Dataset solarZenithDS = (Dataset) dataField;
                solarZenithDS.init();
                solarZenithVals = (short[]) solarZenithDS.getData();
            } else if ("SolarAzimuth".equals(dataFieldName)) {
                Dataset solarAzimuthDS = (Dataset) dataField;
                solarAzimuthDS.init();
                solarAzimuthVals = (short[]) solarAzimuthDS.getData();
            } else if ("Land/SeaMask".equals(dataFieldName)) {
                Dataset landSeaMaskDS = (Dataset) dataField;
                landSeaMaskDS.init();
                byte[] unsignedVals = (byte[]) landSeaMaskDS.getData();
                landSeaMaskVals = new short[unsignedVals.length];
                Dataset.convertFromUnsignedC(unsignedVals, landSeaMaskVals);
            }
        }

        if (longitudeVals == null || latitudeVals == null || heightVals == null
                || sensorZenithVals == null || sensorAzimuthVals == null || rangeVals == null
                || solarZenithVals == null || solarAzimuthVals == null || landSeaMaskVals == null) {
            throw new Exception("Not all expected datasets were located in the provided mod03 file.");
        }

        int scanLength = ResolutionEnum.FKM.trackDetectors * ResolutionEnum.FKM.scanLength;
        int numScans = longitudeVals.length / scanLength;
        if (numScans != metadata.numScans) {
            throw new Exception("Mod03 data does not match the number of scans reported in the mod02 granule.");
        }

        HashMap<Integer, GeoDatum[]> geoData = new HashMap<Integer, GeoDatum[]>();
        for (int scanNumber = 0; scanNumber < numScans; scanNumber++) {
            int scanOffset = scanNumber * scanLength;
            GeoDatum[] scanGeoData = new GeoDatum[scanLength];
            for (int relativeIndex = 0; relativeIndex < scanLength; relativeIndex++) {
                int globalIndex = scanOffset + relativeIndex;
                GeoDatum sample = new GeoDatum();
                sample.metadata = metadata;
                sample.longitude = longitudeVals[globalIndex];
                sample.latitude = latitudeVals[globalIndex];
                sample.trackIndex = globalIndex / ResolutionEnum.FKM.scanLength;
                sample.scanIndex = globalIndex % ResolutionEnum.FKM.scanLength;
                sample.height = heightVals[globalIndex];
                sample.sensorZenith = sensorZenithVals[globalIndex];
                sample.sensorAzimuth = sensorAzimuthVals[globalIndex];
                sample.range = rangeVals[globalIndex];
                sample.solarZenith = solarZenithVals[globalIndex];
                sample.solarAzimuth = solarAzimuthVals[globalIndex];
                sample.landSeaMask = landSeaMaskVals[globalIndex];
                scanGeoData[relativeIndex] = sample;
            }
            geoData.put(scanNumber, scanGeoData);
        }
        return geoData;
    }

    private static EvDatasets getEvDatasets(FileFormat mod02File) throws Exception {
        if (evDatasets == null) {
            evDatasets = new EvDatasets(mod02File);
        }
        return evDatasets;
    }

    public static Measurement[] loadBandMeasurements(GranuleMetadata granuleMetadata, FileFormat mod02File, BandEnum band, int scanNumber) throws Exception {
        EvDatasets evds = getEvDatasets(mod02File);
        if (evds.ev250_UI_DS != null && evds.ev250_DS != null && band.dsPrefix.equals("EV_250")) {
            return loadBandMeasurements(evds.ev250_UI_DS, evds.ev250_DS, granuleMetadata.resolution, band, scanNumber);
        }
        if (evds.ev500_UI_DS != null && evds.ev500_DS != null && band.dsPrefix.equals("EV_500")) {
            return loadBandMeasurements(evds.ev500_UI_DS, evds.ev500_DS, granuleMetadata.resolution, band, scanNumber);
        }
        if (evds.ev1kmR_UI_DS != null && evds.ev1kmR_DS != null && band.dsPrefix.equals("EV_1KM_R")) {
            return loadBandMeasurements(evds.ev1kmR_UI_DS, evds.ev1kmR_DS, granuleMetadata.resolution, band, scanNumber);
        }
        if (evds.ev1kmE_UI_DS != null && evds.ev1kmE_DS != null && band.dsPrefix.equals("EV_1KM_E")) {
            return loadBandMeasurements(evds.ev1kmE_UI_DS, evds.ev1kmE_DS, granuleMetadata.resolution, band, scanNumber);
        }
        if (evds.evB26_UI_DS != null && evds.evB26_DS != null && band.dsPrefix.equals("EV_B")) {
            return loadBandMeasurements(evds.evB26_UI_DS, evds.evB26_DS, granuleMetadata.resolution, band, scanNumber);
        }
        return null;
    }

    private static Measurement[] loadBandMeasurements(Dataset uiDS, Dataset mDS, ResolutionEnum resolution, BandEnum band, int scanNumber) throws Exception {
        byte[] unsignedUncertainties = (byte[]) getMeasurementData(uiDS, resolution, band, scanNumber);
        short[] uncertainties = new short[unsignedUncertainties.length];
        Dataset.convertFromUnsignedC(unsignedUncertainties, uncertainties);
        short[] unsignedValues = (short[]) getMeasurementData(mDS, resolution, band, scanNumber);
        int[] values = new int[unsignedValues.length];
        Dataset.convertFromUnsignedC(unsignedValues, values);
        Measurement[] measurements = new Measurement[unsignedUncertainties.length];
        for (int i = 0; i < measurements.length; i++) {
            Measurement m = new Measurement();
            m.uncertaintyIndex = uncertainties[i];
            m.siValue = values[i];
            measurements[i] = m;
        }
        return measurements;
    }

    private static Object getMeasurementData(Dataset ds, ResolutionEnum resolution, BandEnum band, int scanNumber) throws Exception {
        if (band == BandEnum.b26) {
            return getB26MeasurementData(ds, resolution, scanNumber);
        }
        long dims[] = ds.getDims();
        long start[] = ds.getStartDims();
        start[0] = band.dsOffset;
        start[1] = scanNumber * resolution.trackDetectors;
        start[2] = 0;
        long selected[] = ds.getSelectedDims();
        selected[0] = 1;
        selected[1] = resolution.trackDetectors;
        selected[2] = dims[2];
        Object data = ds.getData();
        ds.clearData();
        return data;
    }

    private static Object getB26MeasurementData(Dataset ds, ResolutionEnum resolution, int scanNumber) throws Exception {
        long dims[] = ds.getDims();
        long start[] = ds.getStartDims();
        start[0] = scanNumber * resolution.trackDetectors;
        start[1] = 0;
        long selected[] = ds.getSelectedDims();
        selected[0] = resolution.trackDetectors;
        selected[1] = dims[1];
        return ds.getData();
    }

    public static Attribute getAttribute(HObject obj, String attributeName) throws Exception {
        List metadata = obj.getMetadata();
        return getAttribute(metadata, attributeName);
    }

    public static Attribute getAttribute(List metadata, String attributeName) throws Exception {
        for (Object o : metadata) {
            if (o instanceof Attribute) {
                Attribute a = (Attribute) o;
                if (attributeName.equals(a.getName())) {
                    return a;
                }
            }
        }
        return null;
    }
}
