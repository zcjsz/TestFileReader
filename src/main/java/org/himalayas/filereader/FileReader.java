/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.es.ESHelper;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.reader.ReaderFactory;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
public final

    class FileReader {

    private
        int totalFileCnt = 0;
    private
        Reader reader = null;
    private
        ESHelper esHelper = null;

    public
        FileReader(File configFile) {
        long startTime = System.currentTimeMillis();
        if (Config.dataFormats.isEmpty()) {
            new Config(configFile.getAbsolutePath());
            Config.readLotList();
        }
        esHelper = ESHelper.getInstance();

        System.out.println(LocalDateTime.now().toString() + ": Task start...");
        this.readFile();
        esHelper.closeConn();
        System.out.println();
        this.logAppEvent2ES(FieldType.CATEGORY_APP, totalFileCnt, ZonedDateTime.now().toOffsetDateTime().toString(), (System.currentTimeMillis() - startTime));
        System.out.println(LocalDateTime.now().toString() + ": All task completed,total time = " + (System.currentTimeMillis() - startTime));
    }

    private
        void readFile() {

        for (DataFormat dataFormat : Config.dataFormats.values()) {
            if (dataFormat.isEnabled()) {
                reader = ReaderFactory.creatReader(dataFormat);
                if (reader == null) {
                    continue;
                }
                System.out.println("*****************************************************************");
                System.out.println("**********                                     ******************");
                System.out.printf("**********        start to proceed %s        ******************\n", dataFormat.getDataType());
                System.out.println("**********                                     ******************");
                System.out.println("*****************************************************************");

                File rootFile = new File(dataFormat.getKdfPath());
                for (File dateFile : rootFile.listFiles()) {
                    if (!this.checkDateFile(dateFile, dataFormat.getMinDateString())) {
                        System.out.printf("Warning: skip %s since minDate is %s\n", dateFile.getName(), dataFormat.getMinDateString());
                        continue;
                    }

                    try {
                        Files.walk(rootFile.toPath(), 3)
                            .filter(path -> path.toFile().isFile() && path.toFile().canRead())
                            .forEach(path -> {
                                if (path.toFile().length() < 100) {
                                    System.out.println("Error: " + path.toString());
                                    System.out.printf("Error: file size = %d error, less than 100 byte\n", path.toFile().length());
                                }
                                else {
                                    reader.loadFile(path.toFile());
                                }
//                                    if (reader.kdfDoneCnt >= Config.smapFormat.getFileLimit()) {
//                                        System.out.println("kdf done file cnt is " + (reader.kdfDoneCnt));
//                                        System.out.println("Have break now, bye");
//                                        return;
//                                    }
                            });
                    }
                    catch (IOException ex) {
                        Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                totalFileCnt += reader.getKdfDoneCnt();
                logAppEvent2ES(dataFormat.getSourceType(), reader.getKdfDoneCnt(), ZonedDateTime.now().toOffsetDateTime().toString(), reader.getRunningTime());

                System.out.println("*****************************************************************");
                System.out.println("**********                                     ******************");
                System.out.printf("**********      complete to proceed %s       ******************\n", dataFormat.getDataType());
                System.out.println("**********                                     ******************");
                System.out.println("*****************************************************************");

            }
            if (esHelper.isInitilized()) {
                if (dataFormat.isKdfData() && (!dataFormat.getLotList().isEmpty())) {
                    esHelper.initDataForamt(dataFormat);
                    esHelper.updateIsCalFlag2N(dataFormat.getLotList());
                    esHelper.proceedUncaledLot();
                }
                else if (dataFormat.getDataType().equals(Config.DataTypes.CAMSTAR)) {
                    esHelper.initDataForamt(dataFormat);
                    esHelper.upsertCamDataToLot();
                }
            }
        }
    }

    /**
     *
     * @param readerType
     * @param cnt
     * @param startTime
     * @param runTime
     */
    public
        void logAppEvent2ES(String readerType, int cnt, String startTime, long runTime) {
        System.out.printf("%s=%s,%s=%d,%s=%s,%s=%s,%s=%s,%s=%d\n",
            FieldType.EventType, readerType,
            FieldType.FILE_CNT, cnt,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            FieldType.START_TIME, startTime,
            FieldType.CATEGORY, FieldType.CATEGORY_APP,
            FieldType.RUN_TIME, runTime
        );

    }

    public
        boolean checkDateFile(File dateFile, String minDate) {

        if (dateFile.isDirectory()
            && (dateFile.getName().length() == 8 || dateFile.getName().length() == 10)
            && dateFile.getName().startsWith("20")) {
            try {
                int temp = Integer.valueOf(dateFile.getName());
                return dateFile.getName().compareTo(minDate) >= 0;
            }
            catch (NumberFormatException e) {
                return false;
            }

        }
        else {
            return false;
        }

    }

    public static
        void main(String[] args) {
        boolean debug = true;
        if (args.length == 0) {
            if (!debug) {
                System.out.println("please set the config file path");
            }
            if (debug) {
                File engConfigFile = new File("config/dataformat.xml");
                File prodConfigFile = new File("config/fileReader/config/dataformat.xml");
                new Config(prodConfigFile.getAbsolutePath());
                for (DataFormat format : Config.dataFormats.values()) {
                    format.setLatestDays(200);
                    format.setGenerateMappingFile(false);
                    format.setProductionMode(false);
                }
                new FileReader(prodConfigFile);
            }
            else {
                System.exit(1);
            }
        }
        else {
            System.out.println("args: " + Arrays.toString(args));
        }
        if (args.length == 1) {
            if (args[0].toLowerCase().contains("help")
                || args[0].toLowerCase().contains("-h")) {

                System.out.println("usage: java -jar FileReader [configfile]");
                System.exit(1);
            }
            File configFile = new File(args[0]);
            if (configFile.exists() && configFile.isFile()) {
                new FileReader(configFile);
            }
        }
        else {
            if (!debug) {
                System.out.println("please set the config file path");
                System.exit(1);
            }

        }

    }

}
