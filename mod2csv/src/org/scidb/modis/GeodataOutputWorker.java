package org.scidb.modis;

import java.io.BufferedWriter;
import org.apache.log4j.Logger;
import org.scidb.modis.model.GeoDatum;
import org.scidb.modis.util.LogUtil;

public class GeodataOutputWorker implements Runnable {

    private static final Logger log = LogUtil.getLogger();
    private final BufferedWriter writer;
    private final int scanNumber;
    private final GeoDatum[] geoData;

    public GeodataOutputWorker(BufferedWriter writer, int scanNumber, GeoDatum[] geoData) {
        this.writer = writer;
        this.scanNumber = scanNumber;
        this.geoData = geoData;
    }

    @Override
    public void run() {
        try {
            if (scanNumber == 0) {
                writer.write(GeoDatum.getCSVHeader());
                writer.newLine();
            }
            for (int i = 0; i < geoData.length; i++) {
                String csv = geoData[i].toCSV();
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
