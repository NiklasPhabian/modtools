package org.scidb.modis;

import java.io.BufferedWriter;
import org.apache.log4j.Logger;
import org.scidb.modis.constant.BandEnum;
import org.scidb.modis.model.BandDatum;
import org.scidb.modis.model.BandMetadata;
import org.scidb.modis.model.GeoDatum;
import org.scidb.modis.model.GranuleMetadata;
import org.scidb.modis.model.Measurement;
import org.scidb.modis.util.LogUtil;

public class BandDataOutputWorker implements Runnable {

    private static final Logger log = LogUtil.getLogger();
    private final BufferedWriter writer;
    private final GranuleMetadata metadata;
    private final int scanNumber;
    private final GeoDatum[] geoData;
    private final BandMetadata bandMetadata;
    private final BandEnum band;
    private final Measurement[] measurements;

    public BandDataOutputWorker(BufferedWriter writer, GranuleMetadata metadata, int scanNumber,
            GeoDatum[] geoData, BandMetadata bandMetadata, BandEnum band, Measurement[] measurements) {
        this.writer = writer;
        this.metadata = metadata;
        this.scanNumber = scanNumber;
        this.geoData = geoData;
        this.bandMetadata = bandMetadata;
        this.band = band;
        this.measurements = measurements;
    }

    @Override
    public void run() {
        try {
            if (scanNumber == 0) {
                writer.write(BandDatum.getCSVHeader());
                writer.newLine();
            }
            for (int i = 0; i < measurements.length; i++) {
                BandDatum bd = new BandDatum();
                bd.metadata = metadata;
                bd.geoDatum = geoData[i];
                bd.bandMetadata = bandMetadata;
                bd.band = band;
                bd.measurement = measurements[i];
                String csv = bd.toCSV();
                if (!"".equals(csv)) {
                    writer.write(csv);
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            log.error(LogUtil.getStackTraceString(e));
        }
    }
}
