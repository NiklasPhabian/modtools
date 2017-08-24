package org.scidb.modis.upsampler;

import org.apache.log4j.Logger;
import org.scidb.modis.constant.ResolutionEnum;
import org.scidb.modis.util.LogUtil;

public class UpsamplerFactory {

    private static final Logger log = LogUtil.getLogger();

    public static Upsampler getSampler(ResolutionEnum targetResolution) throws Exception {
        if (targetResolution == ResolutionEnum.FKM) {
            return new UpsamplerFkm();
        } else if (targetResolution == ResolutionEnum.HKM) {
            log.trace("Mod03 geodata will be upsampled from 1km to 500m resolution.");
            return new UpsamplerHkm();
        } else if (targetResolution == ResolutionEnum.QKM) {
            log.trace("Mod03 geodata will be upsampled from 1km to 250m resolution.");
            return new UpsamplerQkm();
        }
        throw new Exception("Unhandled resolution: " + targetResolution);
    }
}
