package org.scidb.modis.upsampler;

import org.apache.log4j.Logger;
import org.scidb.modis.model.GeoDatum;
import org.scidb.modis.util.LogUtil;

public class UpsamplerFkm extends Upsampler {

    private static final Logger log = LogUtil.getLogger();

    @Override
    public GeoDatum[] processScan(int scanNumber, GeoDatum[] fkmGeoData) {
        return fkmGeoData;
    }
}
