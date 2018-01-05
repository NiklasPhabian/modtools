package org.scidb.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Processor {

    private final String scidbFolder;
    private final int numInstances;
    private final int chunkSize;
    private final int linesToSkip;
    private final String inputFile;
    private final String outputFile;
    private BufferedReader reader;
    private BufferedWriter[] writers;
    private boolean[] chunkWrittenFlags;
    private boolean[] lineWrittenFlags;


    public static void main(String[] args) {
        if (args.length != 6) {
            printUsage();
            return;
        }
        String scidbFolder = args[0];
        int numInstances = Integer.parseInt(args[1]);
        int chunkSize = Integer.parseInt(args[2]);
        int linesToSkip = Integer.parseInt(args[3]);
        String inputFile = args[4];
        String outputFile = args[5];
        Processor p = new Processor(scidbFolder, numInstances, chunkSize, linesToSkip, inputFile, outputFile);
        p.process();
    }


    public static void printUsage() {
        System.out.println("USAGE: pcsv2scidb <scidbFolder> <numInstances> <chunkSize> <linesToSkip> <inputFile> <outputFile>");
        System.out.println("   <scidbFolder>    - Path to the SciDB data folder (parent of the instance folders).");
        System.out.println("   <numInstances>   - Number of SciDB instances to stripe across.");
        System.out.println("   <chunkSize>      - Chunk size of the 1-D load array.");
        System.out.println("   <linesToSkip>    - Number of lines to skip in the inputFile.");
        System.out.println("   <input file>     - Path to the input CSV file.");
        System.out.println("   <output file>    - Base name of the ouput file that should be created in the folder under each instance.");
        System.out.println();
    }

    public Processor(String scidbFolder, int numInstances, int chunkSize, int linesToSkip, String inputFile, String outputFile) {
        this.scidbFolder = scidbFolder;
        this.numInstances = numInstances;
        this.chunkSize = chunkSize;
        this.linesToSkip = linesToSkip;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.chunkWrittenFlags = new boolean[numInstances];
        this.lineWrittenFlags = new boolean[numInstances];
        this.writers = new BufferedWriter[numInstances];
    }

    public void process() {
        try {
            openFiles();
            skipLines(reader, linesToSkip);
            int rowIndex = 0;
            String csvRow = null;
            while ((csvRow = reader.readLine()) != null) {
                int chunkNumber = rowIndex / chunkSize;
                int instanceId = chunkNumber % numInstances;
                BufferedWriter writer = writers[instanceId];
                if (rowIndex % chunkSize == 0) {
                    if (chunkWrittenFlags[instanceId]) {
                        writer.newLine();
                        writer.write("];");
                        writer.newLine();
                    } else {
                        chunkWrittenFlags[instanceId] = true;
                    }
                    writer.write("{" + chunkNumber * chunkSize + "}[");
                    lineWrittenFlags[instanceId] = false;
                }
                if (lineWrittenFlags[instanceId]) {
                    writer.write(",");
                } else {
                    lineWrittenFlags[instanceId] = true;
                }
                writer.newLine();
                writer.write("(" + csvRow + ")");
                rowIndex++;
            }
            for (int i = 0; i < numInstances; i++) {
                if (chunkWrittenFlags[i]) {
                    writers[i].newLine();
                    writers[i].write("]");
                    writers[i].newLine();
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        } finally {
            closeFiles();
        }
    }

    private void skipLines(BufferedReader r, int n) throws Exception {
        for (int i = 0; i < n; i++) {
            r.readLine();
        }
    }

    private void openFiles() throws Exception {
        reader = new BufferedReader(new FileReader(inputFile));
        for (int i = 0; i < numInstances; i++) {
            String folderPath = scidbFolder + File.separator + i + File.separator;
            File folder = new File(folderPath);
            folder.mkdirs();
            writers[i] = new BufferedWriter(new FileWriter(folderPath + outputFile));
        }
    }

    private void closeFiles() {
        try {
            for (BufferedWriter writer : writers) {
                if (writer != null) {
                    writer.close();
                }
            }
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}