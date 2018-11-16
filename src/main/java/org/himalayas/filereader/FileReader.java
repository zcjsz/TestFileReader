/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.himalayas.filereader.kdf.KDFReader;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.reader.smap.SmapReader;
import org.himalayas.filereader.reader.wat.WatReader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;

/**
 *
 * @author ghfan
 */
public
    class FileReader {

    private
        KDFReader kdfReader = null;
    private
        Reader smapReader = null;
    private
        Reader watReader = null;

    private
        void init() {
        for (DataFormat format : Config.dataFormats.values()) {
            if (!format.isEnabled()) {
                continue;
            }

            Config.DataTypes dataType = format.getDataType();
            if (kdfReader == null
                && (dataType.equals(Config.DataTypes.ATE)
                || dataType.equals(Config.DataTypes.SLT)
                || dataType.equals(Config.DataTypes.WaferSort))) {
                kdfReader = new KDFReader();
            }
            if (smapReader == null && dataType.equals(Config.DataTypes.SMAP)) {
                smapReader = new SmapReader(format);
            }
            if (watReader == null && dataType.equals(Config.DataTypes.WAT)) {
                smapReader = new WatReader(format);
            }
        }
    }

    private
        void readKDF(DataFormat format) {
        if (kdfReader != null && format.isEnabled()
            && (format.getDataType().equals(Config.DataTypes.WaferSort)
            || format.getDataType().equals(Config.DataTypes.ATE)
            || format.getDataType().equals(Config.DataTypes.SLT))) {
            System.out.println("*****************************************************************");
            System.out.println("**********                                     ******************");
            System.out.printf("**********           start to proceed %s\n", format.getSourceType());
            System.out.println("**********                                     ******************");
            System.out.println("*****************************************************************");

            kdfReader.setFormat(format);
            for (File dateFile : new File(format.getKdfPath()).listFiles()) {
                if (!this.checkDateFile(dateFile, format.getMinDateString())) {
                    continue;
                }

                for (File kdfFile : dateFile.listFiles()) {
                    if (kdfFile.isFile()) {
                        String fileName = kdfFile.getName();
                        if (fileName.endsWith(Config.KdfRename.badFormat.name())
                            || fileName.endsWith(Config.KdfRename.done.name())
                            || fileName.endsWith(Config.KdfRename.exception.name())
                            || fileName.endsWith(Config.KdfRename.openErr.name())
                            || fileName.endsWith(Config.KdfRename.skip.name())) {
                            continue;

                        }
                        if (!kdfFile.canRead()) {
                            System.out.println("Error: tdni has no permission to read this file");
                            continue;
                        }
                        if (kdfFile.length() < 100) {
                            System.out.printf("Error: file size = %d error, less than 100 byte\n", kdfFile.length());
                            continue;
                        }
                        try {
                            kdfReader.loadFile(kdfFile);
                            if (kdfReader.getKdfDoneCnt() >= kdfReader.getFormat().getFileLimit()) {
                                System.out.println("kdf done file cnt is " + (kdfReader.getKdfDoneCnt()));
                                System.out.println("Have break now, bye");
                                return;
                            }
                        }
                        catch (Exception e) {
                            kdfReader.logExceptionToES();
                            kdfReader.renameOrArchiveKDF(kdfReader.getExceptionArchiveFile(), Config.KdfRename.exception);
                            System.out.println();
                            e.printStackTrace();
                        }
                    }
                }
            }

            /**
             * @todo for each lotOperation in dataformat upsert lot operations
             * to es index
             */
            System.out.println("*****************************************************************");
            System.out.println("**********                                     ******************");
            System.out.printf("**********           complete to proceed %s\n", format.getSourceType());
            System.out.println("**********                                     ******************");
            System.out.println("*****************************************************************");
        }
    }

    private
        void readSmap() {
        if (smapReader != null) {
            System.out.println("*****************************************************************");
            System.out.println("**********                                     ******************");
            System.out.println("**********        start to proceed smap        ******************");
            System.out.println("**********                                     ******************");
            System.out.println("*****************************************************************");
            File rootFile = new File(Config.smapFormat.getKdfPath());
            for (File dateFile : rootFile.listFiles()) {
                if (!this.checkDateFile(dateFile, Config.smapFormat.getMinDateString())) {
                    continue;
                }
                for (File lotFile : dateFile.listFiles()) {
                    if (lotFile.isDirectory()) {
                        for (File kdfFile : lotFile.listFiles()) {
                            if (kdfFile.isFile()) {
                                String fileName = kdfFile.getName();
                                if (!kdfFile.canRead()) {
                                    System.out.println("Error: tdni has no permission to read this file");
                                    continue;
                                }
                                if (kdfFile.length() < 100) {
                                    System.out.printf("Error: file size = %d error, less than 100 byte\n", kdfFile.length());
                                    continue;
                                }

                                smapReader.loadFile(kdfFile);
                                if (smapReader.getKdfDoneCnt() >= smapReader.getFormat().getFileLimit()) {
                                    System.out.println("kdf done file cnt is " + (smapReader.getKdfDoneCnt()));
                                    System.out.println("Have break now, bye");
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("*****************************************************************");
            System.out.println("**********                                     ******************");
            System.out.println("**********      complete to proceed smap       ******************");
            System.out.println("**********                                     ******************");
            System.out.println("*****************************************************************");
        }

    }

    public
        FileReader(File configFile) {
        long startTime = System.currentTimeMillis();
        new Config(configFile.getAbsolutePath());
        this.init();

        System.out.println(LocalDateTime.now().toString() + ": Task start...");
        this.readSmap();
        this.readKDF(Config.getSLTFormat());
        for (DataFormat format : Config.dataFormats.values()) {
            if (format.getDataType().equals(Config.DataTypes.ATE)
                || format.getDataType().equals(Config.DataTypes.WaferSort)) {
                this.readKDF(format);
            }
        }
        System.out.println();
        System.out.println(LocalDateTime.now().toString() + ": All task completed,total time = " + (System.currentTimeMillis() - startTime));
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
                System.out.printf("Warning: skip %s since minDate is %s\n", dateFile.getName(), minDate);
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
                File configFile = new File("config/dataformat.xml");
                new FileReader(configFile);
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
