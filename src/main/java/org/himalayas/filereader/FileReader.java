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
import org.himalayas.filereader.es.ESHelper;
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
        Reader reader = null;
    private
        ESHelper esHelper = null;

    public
        FileReader(File configFile) {
        long startTime = System.currentTimeMillis();
        new Config(configFile.getAbsolutePath());

        esHelper = ESHelper.getInstance();

        System.out.println(LocalDateTime.now().toString() + ": Task start...");
        this.readFile();
        System.out.println();
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
