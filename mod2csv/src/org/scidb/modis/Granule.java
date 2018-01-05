package org.scidb.modis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import ncsa.hdf.object.FileFormat;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.scidb.modis.constant.BandEnum;
import org.scidb.modis.model.BandMetadata;
import org.scidb.modis.model.GeoDatum;
import org.scidb.modis.model.GranuleMetadata;
import org.scidb.modis.model.Measurement;
import org.scidb.modis.upsampler.Upsampler;
import org.scidb.modis.upsampler.UpsamplerFactory;
import org.scidb.modis.util.LogUtil;
import org.scidb.modis.util.ModUtil;
import org.scidb.modis.util.ThreadUtil;

public class Granule {

    private static final Logger log = LogUtil.getLogger();
    private FileFormat mod02File;
    private FileFormat mod03File;
    private GranuleMetadata granuleMetadata;
    private Map<BandEnum, BandMetadata> bandMetadata;
    private static final long MILLIS_PER_SECOND = 1000;
    private static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    private static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
    private static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;

    public static void main(String[] args) {
        try {
            String osName = System.getProperty("os.name");
            if ("Linux".equals(osName)) {
                Logger.getRootLogger().addAppender(getFileAppender("../log/mod2csv.log"));
            }
            int numArgs = args.length;
            String baseOutputFolder = System.getProperty("user.dir");
            boolean quietMode = false;
            boolean outputFolderSpecified = false;
            if (numArgs >= 1) {
                if ("-q".equals(args[0])) {
                    if (numArgs == 1) {
                        printUsage();
                        return;
                    }
                    quietMode = true;
                    Logger.getRootLogger().removeAppender("CONSOLE");
                } else if ("-o".equals(args[0])) {
                    printUsage();
                    return;
                }
                if (numArgs >= 3) {
                    String nextToLast = args[numArgs - 2];
                    if ("-o".equals(nextToLast)) {
                        baseOutputFolder = args[numArgs - 1];
                        outputFolderSpecified = true;
                    }
                }
                if (!baseOutputFolder.endsWith(File.separator)) {
                    baseOutputFolder += File.separator;
                }
                int startIndex = quietMode ? 1 : 0;
                int stopIndex = outputFolderSpecified ? numArgs - 3 : numArgs - 1;
                int numProcessed = 0;
                long batchStartTime = System.currentTimeMillis();
                for (int argIndex = startIndex; argIndex <= stopIndex; argIndex++) {
                    String inputFilePath = args[argIndex];
                    Granule granule = new Granule();
                    try {
                        long startTime = System.currentTimeMillis();
                        log.info("Processing mod02 file: " + inputFilePath);
                        granule.open(inputFilePath);
                        String outputFolder = baseOutputFolder
                                + granule.granuleMetadata.fileId.toUpperCase().replace(".HDF", "");
                        granule.saveGranuleMetadata(outputFolder);
                        granule.saveBandMetadata(outputFolder);
                        granule.saveMeasurementData(outputFolder);
                        long stopTime = System.currentTimeMillis();
                        log.info("Processing completed. (" + formatMillis(stopTime - startTime) + ")");
                        numProcessed++;
                    } catch (Exception e) {
                        log.error(LogUtil.getStackTraceString(e));
                    } finally {
                        try {
                            granule.close();
                        } catch (Exception e) {
                            log.error(LogUtil.getStackTraceString(e));
                        }
                    }
                }
                long batchStopTime = System.currentTimeMillis();
                if (numProcessed > 1) {
                    log.info("\nGranules Processed: " + numProcessed);
                    long elapsed = batchStopTime - batchStartTime;
                    log.info("Total Elapsed Time: " + formatMillis(elapsed));
                    log.info("Average Time per Granule: " + formatMillis(elapsed / numProcessed));
                }
            } else {
                printUsage();
                return;
            }
        } catch (Exception e) {
            log.error(LogUtil.getStackTraceString(e));
        }
    }

    private static String formatMillis(long millis) {
        StringBuilder sb = new StringBuilder();
        boolean needSpace = false;
        long days = millis / MILLIS_PER_DAY;
        millis = millis % MILLIS_PER_DAY;
        if (days > 0) {
            sb.append(days);
            sb.append("d");
            needSpace = true;
        }
        long hours = millis / MILLIS_PER_HOUR;
        millis = millis % MILLIS_PER_HOUR;
        if (hours > 0) {
            if (needSpace) {
                sb.append(" ");
            }
            sb.append(hours);
            sb.append("h");
            needSpace = true;
        }
        long minutes = millis / MILLIS_PER_MINUTE;
        millis = millis % MILLIS_PER_MINUTE;
        if (minutes > 0) {
            if (needSpace) {
                sb.append(" ");
            }
            sb.append(minutes);
            sb.append("m");
            needSpace = true;
        }
        long seconds = millis / MILLIS_PER_SECOND;
        millis = millis % MILLIS_PER_SECOND;
        if (seconds > 0) {
            if (needSpace) {
                sb.append(" ");
            }
            sb.append(seconds);
            sb.append("s");
        }
        if (millis > 0) {
            if (needSpace) {
                sb.append(" ");
            }
            sb.append(millis);
            sb.append("ms");
        }
        return sb.toString();
    }

    private static FileAppender getFileAppender(String fileName) throws Exception {
        PatternLayout layout = new PatternLayout("%d [%p] %t :: %F:%L (%M) :: %m%n");
        FileAppender fa = new FileAppender(layout, fileName, true);
        fa.setThreshold(Level.INFO);
        fa.activateOptions();
        return fa;
    }

    private static void printUsage() {
        System.out.println("USAGE: mod2csv [-q] <input file> [<input file> ...] [-o <output folder>]");
        System.out.println("   -q              - Quiet mode. No messages printed to console.");
        System.out.println("   <input file>    - Path to a mod02 product file.");
        System.out.println("   <output folder> - Path to the folder where output files should be placed.");
        System.out.println();
        System.out.println("EXAMPLES: mod2csv file.hdf");
        System.out.println("          mod2csv -q /data/file1.hdf /data/file2.hdf -o /data/output");
        System.out.println("          mod2csv *.hdf -o output");
    }

    public void open(String mod02FilePath) throws Exception {
        ModUtil.init();
        mod02File = openFile(mod02FilePath);
        granuleMetadata = new GranuleMetadata(mod02File);
        bandMetadata = ModUtil.loadBandMetadata(granuleMetadata, mod02File);
        log.trace(granuleMetadata);
        String mod03FilePath;
        File parentFile = mod02File.getParentFile();
        if (parentFile == null) {
            mod03FilePath = findMod03File(".", granuleMetadata.geoFileId);
        } else {
            mod03FilePath = findMod03File(parentFile.getPath(), granuleMetadata.geoFileId);
        }
        try {
            mod03File = openFile(mod03FilePath);
        } catch (Exception e) {
            log.error(LogUtil.getStackTraceString(e));
            throw new Exception("Failed to open ancillary mod03 geodata file: " + mod03FilePath);
        }
        log.trace("Located ancillary mod03 geodata file: " + mod03FilePath);
    }

    private String findMod03File(String filePath, String mod03FileId) {
        String prefix = mod03FileId.substring(0, 23);
        File dir = new File(filePath);
        FileFilter fileFilter = new WildcardFileFilter(prefix + "*.hdf");
        File[] files = dir.listFiles(fileFilter);
        if (files.length == 0) {
            log.warn("Could not find a mod03 file in " + filePath
                    + " with prefix matching " + prefix);
            return mod03FileId;
        } else if (files.length > 1) {
            log.warn("Multiple mod03 files were found in " + filePath
                    + " with prefix matching " + prefix + ". Returning the first match.");
        }
        return files[0].getPath();
    }

    public void close() throws Exception {
        closeFile(mod03File);
        closeFile(mod02File);
    }

    private void saveGranuleMetadata(String outputFolderPath) throws Exception {
        File folder = new File(outputFolderPath);
        folder.mkdirs();
        String outputFileName = outputFolderPath + File.separator + "Granule_Metadata.csv";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        try {
            writer.write(GranuleMetadata.getCSVHeader());
            writer.newLine();
            String csv = granuleMetadata.toCSV();
            if (!"".equals(csv)) {
                writer.write(csv);
                writer.newLine();
            }
        } finally {
            writer.close();
        }
        log.trace("Granule metadata written to file: " + outputFileName);
    }

    private void saveBandMetadata(String outputFolderPath) throws Exception {
        File folder = new File(outputFolderPath);
        folder.mkdirs();
        String outputFileName = outputFolderPath + File.separator + "Band_Metadata.csv";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        try {
            writer.write(BandMetadata.getCSVHeader());
            writer.newLine();
            for (BandEnum band : BandEnum.values()) {
                BandMetadata bm = bandMetadata.get(band);
                if (bm != null) {
                    String csv = bm.toCSV();
                    if (!"".equals(csv)) {
                        writer.write(csv);
                        writer.newLine();
                    }
                }
            }
        } finally {
            writer.close();
        }
        log.trace("Band metadata written to file: " + outputFileName);
    }

    private void saveMeasurementData(String outputFolderPath) throws Exception {
        log.trace("Saving measurement data...");
        File folder = new File(outputFolderPath);
        folder.mkdirs();
        Map<Integer, GeoDatum[]> fkmGeoData = ModUtil.loadGeoData(granuleMetadata, mod03File);
        String geodataOutputFileName = outputFolderPath + File.separator + "Geodata.csv";
        BufferedWriter geodataWriter = new BufferedWriter(new FileWriter(geodataOutputFileName));
        EnumMap<BandEnum, BufferedWriter> bandDataWriters = new EnumMap(BandEnum.class);
        ThreadPoolExecutor tp = ThreadUtil.getStandardThreadPool(granuleMetadata.resolution.bands.size() + 1);
        Collection<Future<?>> futures = new LinkedList<Future<?>>();
        Upsampler upsampler = UpsamplerFactory.getSampler(granuleMetadata.resolution);
        float oneTenth = (granuleMetadata.numScans / 10) - 1;
        try {
            for (int scanNumber = 0; scanNumber < granuleMetadata.numScans; scanNumber++) {
                GeoDatum[] upsampledGeoData = upsampler.processScan(scanNumber, fkmGeoData.get(scanNumber));
                fkmGeoData.remove(scanNumber);
                GeodataOutputWorker gdWorker = new GeodataOutputWorker(geodataWriter, scanNumber, upsampledGeoData);
                futures.add(tp.submit(gdWorker));
                for (BandEnum band : granuleMetadata.resolution.bands) {
                    BufferedWriter bandDataWriter = bandDataWriters.get(band);
                    if (bandDataWriter == null) {
                        String bandDataOutputFileName = outputFolderPath + File.separator
                                + "Band_" + band + "_Measurements.csv";
                        bandDataWriter = new BufferedWriter(new FileWriter(bandDataOutputFileName));
                        bandDataWriters.put(band, bandDataWriter);
                    }
                    Measurement[] measurements = ModUtil.loadBandMeasurements(granuleMetadata, mod02File, band, scanNumber);
                    if (upsampledGeoData.length != measurements.length) {
                        throw new Exception("Geodata and measurements are not the same size for scan "
                                + scanNumber + " of band " + band);
                    }
                    BandDataOutputWorker bdWorker = new BandDataOutputWorker(bandDataWriter, granuleMetadata,
                            scanNumber, upsampledGeoData, bandMetadata.get(band), band, measurements);
                    futures.add(tp.submit(bdWorker));
                }
                for (Future<?> future : futures) {
                    future.get();
                }
                futures.clear();
                if (scanNumber % oneTenth == 0) {
                    int pct = (int) (10 * scanNumber / oneTenth);
                    log.trace(pct + "% complete.");
                }
            }
        } finally {
            tp.shutdown();
            tp.awaitTermination(1, TimeUnit.HOURS);
            log.trace("Flushing buffers and closing files...");
            geodataWriter.close();
            for (BufferedWriter bandDataWriter : bandDataWriters.values()) {
                bandDataWriter.close();
            }
        }
        log.trace("Measurement data has been saved to disk.");
    }

    private FileFormat openFile(String filePath) throws Exception {
        FileFormat hdf4Format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
        if (hdf4Format == null) {
            // Note: this is not about the ncsa.jhdf library, but about the libjhdf native library
            throw new Exception("Could not find HDF4 FileFormat (libjhdf native library is probably not in java library path).");
        }
        FileFormat file = hdf4Format.createInstance(filePath, FileFormat.READ);
        if (file == null) {
            throw new Exception("Failed to open file " + filePath);
        }
        file.open();
        return file;
    }

    private void closeFile(FileFormat file) throws Exception {
        if (file != null) {
            file.close();
        }
    }
}