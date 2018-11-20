package org.himalayas.filereader.reader.camstar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.Config;

public class CamRptWriter {
    
    private static Logger logger = CamstarLogger.getInstance();
    private static String LineSeparator = System.getProperty("line.separator");
    private static String logFieldSplitter = ",";
    private static String logKVAssignSymbol = "=";
    private static String archivePath;
    private static String archiveBadPath;
    
    private CamRpt camRpt;
    private Reader reader;
    private String rptGoodLogPath = "./rptGoodLog.log";
    private String rptBadLogPath = "./rptBadLog.log";
    private List<LinkedHashMap<String, String>> rptGoodLines;
    private List<LinkedHashMap<String, String>> rptBadLines;
    private String fileTime;
    private String fileName;
    
    public CamRptWriter() {}
    
    public CamRptWriter(Reader reader, CamRpt camRpt){
        this.reader = reader;
        this.camRpt = camRpt;
        this.rptGoodLogPath = reader.getTestLogFile().getPath();
        this.rptBadLogPath = archiveBadPath + "/" + reader.getTestLogFile().getName() + ".badlog";
        this.rptGoodLines = camRpt.getRptGoodLines();
        this.rptBadLines = camRpt.getRptBadLines();
        this.fileTime = reader.getTransferTime();
        this.fileName = reader.getFileName();
    }

    public static boolean init() {
        archivePath = Config.camFormat.getXmlPath();
        archiveBadPath = Config.camFormat.getXmlPath() + "/bad";
        File archiveFolder = new File(archivePath);
        File archiveBadFolder = new File(archiveBadPath);
        
        try {
            
            if(!archiveFolder.exists()) {
                if(archiveFolder.mkdirs()) throw new Exception("Can't mkdir : " + archiveFolder.getAbsolutePath());
            } else {
                if(archiveFolder.isFile()) {
                    if(archiveFolder.delete()) throw new Exception("Can't delete file : " + archiveFolder.getAbsolutePath());
                    if(archiveFolder.mkdirs()) throw new Exception("Can't mkdir : " + archiveFolder.getAbsolutePath());
                }
            }

            if(!archiveBadFolder.exists()) {
                if(archiveBadFolder.mkdirs()) throw new Exception("Can't mkdir : " + archiveBadFolder.getAbsolutePath());
            } else {
                if(archiveBadFolder.isFile()) {
                    if(archiveBadFolder.delete()) throw new Exception("Can't delete file : " + archiveBadFolder.getAbsolutePath());
                    if(archiveBadFolder.mkdirs()) throw new Exception("Can't mkdir : " + archiveBadFolder.getAbsolutePath());
                }
            }
            
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        
        return true;
    }
      
    
    public boolean writeGoodLines() {
        return (writeLines(rptGoodLines, rptGoodLogPath)>=0);
    }
    
    public boolean writeBadLines() {
        return (writeLines(rptBadLines, rptBadLogPath)>=0);
    }
    
    
    private int writeLines(List<LinkedHashMap<String, String>> lines, String logPath) {
        StringBuilder strBud = null;
        FileWriter fw = null;
        
        if(lines == null) {
            return -2; //logger.error("Can't get report data!!"); 
        }

        if(lines.size()<1) {
            return 1; //logger.error("No valid report data!!");
        }
        
        try {
            
            strBud = new StringBuilder();
            fw = new FileWriter(logPath);
            
            for(LinkedHashMap<String, String> line : lines) {
                for(Entry<String, String> entry : line.entrySet()) {
                    strBud.append(entry.getKey()).append(logKVAssignSymbol).append(entry.getValue()).append(logFieldSplitter);
                }
                strBud.append("FileName").append(logKVAssignSymbol).append(fileName).append(logFieldSplitter);
                strBud.append("FileTime").append(logKVAssignSymbol).append(fileTime).append(logFieldSplitter);
                String lineStr = strBud.substring(0, strBud.length()-logFieldSplitter.length()) + LineSeparator;
                strBud.setLength(0);
                fw.write(lineStr);
            }
            
            fw.flush();
            
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            camRpt.setRptWriteFail(true);
            return -1; 
        } finally {
            try {
               if(fw!=null) fw.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        
        return 0;

    }
    
    
}
