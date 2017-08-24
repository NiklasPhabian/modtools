package org.scidb.modis.upsampler;

import org.scidb.modis.model.GeoDatum;

public abstract class Upsampler {

    public abstract GeoDatum[] processScan(int scanNumbr, GeoDatum[] fkmGeoData);

    public static GeoDatum interpolate(GeoDatum value1, GeoDatum value2, float offset) {
        GeoDatum newPoint = new GeoDatum();
        newPoint.metadata = value1.metadata;
        newPoint.latitude = interpFloatValue(value1.latitude, value2.latitude, offset);
        newPoint.longitude = interp180CircularAngle(value1.longitude, value2.longitude, offset);
        newPoint.height = (short) interpIntValue(value1.height, value2.height, offset);
        newPoint.sensorZenith = (short) interpIntValue(value1.sensorZenith, value2.sensorZenith, offset);
        newPoint.sensorAzimuth = (short) interp180CircularAngle(100, value1.sensorAzimuth, value2.sensorAzimuth, offset);
        newPoint.range = interpIntValue(value1.range, value2.range, offset);
        newPoint.solarZenith = (short) interpIntValue(value1.solarZenith, value2.solarZenith, offset);
        newPoint.solarAzimuth = (short) interp180CircularAngle(100, value1.solarAzimuth, value2.solarAzimuth, offset);
        newPoint.landSeaMask = (short) interpIntValue(value1.landSeaMask, value2.landSeaMask, offset);
        return newPoint;
    }

    private static float interp180CircularAngle(float value1, float value2, float offset) {
        return interp180CircularAngle(1.0f, value1, value2, offset);
    }

    private static float interp180CircularAngle(float scaleFactor, float value1, float value2, float offset) {
        if (Math.abs(value1 - value2) > (180.0f * scaleFactor)) {
            float delta = ((360.0f * scaleFactor) - Math.abs(value2 - value1)) * offset;
            float result;
            if (value1 < 0.0f) {
                result = value1 - delta;
            } else {
                result = value1 + delta;
            }
            return normalizeAngle(scaleFactor, -180.0f, 180.0f, result);
        }
        return interpFloatValue(value1, value2, offset);
    }

    private static float interpFloatValue(float value1, float value2, float offset) {
        float delta = (value2 - value1) * offset;
        float result = value1 + delta;
        return result;
    }

    private static int interpIntValue(int value1, int value2, float offset) {
        float delta = (value2 - value1) * offset;
        float result = value1 + delta;
        return Math.round(result);
    }

    public static GeoDatum extrapolate(GeoDatum value1, GeoDatum value2, int steps) {
        GeoDatum newPoint = new GeoDatum();
        newPoint.metadata = value1.metadata;
        newPoint.latitude = clipFloatValue(-90, 90, extrapFloatValue(value1.latitude, value2.latitude, steps));
        newPoint.longitude = normalizeAngle(-180, 180, extrapFloatValue(value1.longitude, value2.longitude, steps));
        newPoint.height = (short) extrapIntValue(value1.height, value2.height, steps);
        newPoint.sensorZenith = (short) clipIntValue(0, 18000, extrapIntValue(value1.sensorZenith, value2.sensorZenith, steps));
        newPoint.sensorAzimuth = (short) normalizeAngle(100, -180, 180, extrapIntValue(value1.sensorAzimuth, value2.sensorAzimuth, steps));
        newPoint.range = extrapIntValue(value1.range, value2.range, steps);
        newPoint.solarZenith = (short) clipIntValue(0, 18000, extrapIntValue(value1.solarZenith, value2.solarZenith, steps));
        newPoint.solarAzimuth = (short) normalizeAngle(100, -180, 180, extrapIntValue(value1.solarAzimuth, value2.solarAzimuth, steps));
        newPoint.landSeaMask = value2.landSeaMask;
        return newPoint;
    }

    private static float extrapFloatValue(float value1, float value2, int steps) {
        float delta = value2 - value1;
        float result = value2 + (delta * steps);
        return result;
    }

    private static int extrapIntValue(int value1, int value2, int steps) {
        int delta = value2 - value1;
        int result = value2 + (delta * steps);
        return result;
    }

    private static float normalizeAngle(float minAngle, float maxAngle, float value) {
        return normalizeAngle(1.0f, minAngle, maxAngle, value);
    }

    private static float normalizeAngle(float scaleFactor, float minAngle, float maxAngle, float value) {
        if (value < (minAngle * scaleFactor)) {
            float newValue = value + (360.0f * scaleFactor);
            return normalizeAngle(scaleFactor, minAngle, maxAngle, newValue);
        } else if (value > (maxAngle * scaleFactor)) {
            float newValue = value - (360.0f * scaleFactor);
            return normalizeAngle(scaleFactor, minAngle, maxAngle, newValue);
        }
        return value;
    }

    private static int clipIntValue(int minValue, int maxValue, int value) {
        if (value < minValue) {
            return minValue;
        } else if (value > maxValue) {
            return maxValue;
        }
        return value;
    }

    private static float clipFloatValue(float minValue, float maxValue, float value) {
        if (value < minValue) {
            return minValue;
        } else if (value > maxValue) {
            return maxValue;
        }
        return value;
    }
}
