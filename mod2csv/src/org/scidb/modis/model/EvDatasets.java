package org.scidb.modis.model;

import java.util.List;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;

public class EvDatasets {

    // 250m data
    public Dataset ev250_UI_DS;
    public Dataset ev250_DS;
    // 500m data
    public Dataset ev500_UI_DS;
    public Dataset ev500_DS;
    // 1km refsb data
    public Dataset ev1kmR_UI_DS;
    public Dataset ev1kmR_DS;
    // 1km emissive data
    public Dataset ev1kmE_UI_DS;
    public Dataset ev1kmE_DS;
    // 1km band 26 data
    public Dataset evB26_UI_DS;
    public Dataset evB26_DS;

    public EvDatasets(FileFormat mod02File) throws Exception {
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
                        ev1kmR_UI_DS = ds;
                    } else {
                        // Measurements
                        ev1kmR_DS = ds;
                    }
                } else if (dfName.startsWith("EV_1KM_E")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        ev1kmE_UI_DS = ds;
                    } else {
                        // Measurements
                        ev1kmE_DS = ds;
                    }
                } else if (dfName.startsWith("EV_B")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        evB26_UI_DS = ds;
                    } else {
                        // Measurements
                        evB26_DS = ds;
                    }
                } else if (dfName.startsWith("EV_500")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        ev500_UI_DS = ds;
                    } else {
                        // Measurements
                        ev500_DS = ds;
                    }
                } else if (dfName.startsWith("EV_250")) {
                    if (dfName.endsWith("Indexes")) {
                        // Uncertainty indices
                        ev250_UI_DS = ds;
                    } else {
                        // Measurements
                        ev250_DS = ds;
                    }
                }
            }
        }
    }
}
