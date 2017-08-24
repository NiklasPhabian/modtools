package org.scidb.modis.model;

import java.util.List;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;

public class EvMetadata {

    // 250m data
    public List ev250_UI_Meta;
    public List ev250_Meta;
    // 500m data
    public List ev500_UI_Meta;
    public List ev500_Meta;
    // 1km refsb data
    public List ev1kmR_UI_Meta;
    public List ev1kmR_Meta;
    // 1km emissive data
    public List ev1kmE_UI_Meta;
    public List ev1kmE_Meta;
    // 1km band 26 data
    public List evB26_UI_Meta;
    public List evB26_Meta;

    public EvMetadata(FileFormat mod02File) throws Exception {
        Group dfGroup = (Group) mod02File.get("/MODIS_SWATH_Type_L1B/Data Fields");
        List<HObject> dfList = dfGroup.getMemberList();
        for (HObject df : dfList) {
            String dfName = df.getName();
            if (dfName.startsWith("E") && !dfName.endsWith("Used")) {
                Dataset ds = (Dataset) df;
                ds.init();
                if (dfName.startsWith("EV_1KM_R")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        ev1kmR_UI_Meta = ds.getMetadata();
                    } else {
                        // Measurements
                        ev1kmR_Meta = ds.getMetadata();
                    }
                } else if (dfName.startsWith("EV_1KM_E")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        ev1kmE_UI_Meta = ds.getMetadata();
                    } else {
                        // Measurements
                        ev1kmE_Meta = ds.getMetadata();
                    }
                } else if (dfName.startsWith("EV_B")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        evB26_UI_Meta = ds.getMetadata();
                    } else {
                        // Measurements
                        evB26_Meta = ds.getMetadata();
                    }
                } else if (dfName.startsWith("EV_500")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        ev500_UI_Meta = ds.getMetadata();
                    } else {
                        // Measurements
                        ev500_Meta = ds.getMetadata();
                    }
                } else if (dfName.startsWith("EV_250")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        ev250_UI_Meta = ds.getMetadata();
                    } else {
                        // Measurements
                        ev250_Meta = ds.getMetadata();
                    }
                }
            }
        }
    }
}
