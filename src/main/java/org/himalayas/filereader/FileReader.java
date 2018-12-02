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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.kdf.KDFReader;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.reader.ReaderFactory;
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
        Reader reader = null;
    
    public
        FileReader(File configFile) {
        long startTime = System.currentTimeMillis();
        new Config(configFile.getAbsolutePath());
        this.init();

        System.out.println(LocalDateTime.now().toString() + ": Task start...");
        this.readFile();
//        for (DataFormat format : Config.dataFormats.values()) {
//            if (format.getDataType().equals(Config.DataTypes.ATE)
//                || format.getDataType().equals(Config.DataTypes.WaferSort)
//                || format.getDataType().equals(Config.DataTypes.SLT)) {
//                this.readKDF(format);
//            }
//        }
        System.out.println();
        System.out.println(LocalDateTime.now().toString() + ": All task completed,total time = " + (System.currentTimeMillis() - startTime));
    }


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
                    System.out.printf("Warning: skip %s since minDate is %s\n", dateFile.getName(), format.getMinDateString());
                    continue;
                }

                for (File kdfFile : dateFile.listFiles()) {
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
        void readFile(){
        for(DataFormat dataFormat: Config.dataFormats.values()){
            if(dataFormat.isEnabled()){
                reader = ReaderFactory.creatReader(dataFormat);
                if(reader == null) {
                    continue;
                }

                File rootFile = new File(dataFormat.getKdfPath());
                for (File dateFile : rootFile.listFiles()) {
                    if (!this.checkDateFile(dateFile, dataFormat.getMinDateString())) {
                        System.out.printf("Warning: skip %s since minDate is %s\n", dateFile.getName(), dataFormat.getMinDateString());
                        continue;
                    }
                    System.out.println("*****************************************************************");
                    System.out.println("**********                                     ******************");
                    System.out.printf("**********        start to proceed %s        ******************\n", dataFormat.getDataType());
                    System.out.println("**********                                     ******************");
                    System.out.println("*****************************************************************");

                    try {
                        Files.walk(rootFile.toPath(), 3)
                                .filter(path -> path.toFile().isFile() && path.toFile().canRead())
                                .forEach(path ->{
                                    if (path.toFile().length() < 100) {
                                        System.out.println("Error: " + path.toString());
                                        System.out.printf("Error: file size = %d error, less than 100 byte\n", path.toFile().length());
                                    }
                                    else{
                                        reader.loadFile(path.toFile());
                                    }
//                                    if (reader.kdfDoneCnt >= Config.smapFormat.getFileLimit()) {
//                                        System.out.println("kdf done file cnt is " + (reader.kdfDoneCnt));
//                                        System.out.println("Have break now, bye");
//                                        return;
//                                    }
                                });
                    } catch (IOException ex) {
                        Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
                    }



                    System.out.println("*****************************************************************");
                    System.out.println("**********                                     ******************");
                    System.out.printf("**********      complete to proceed %s       ******************\n", dataFormat.getDataType());
                    System.out.println("**********                                     ******************");
                    System.out.println("*****************************************************************");
                }
            }
        }
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
