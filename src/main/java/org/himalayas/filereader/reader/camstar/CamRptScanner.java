package org.himalayas.filereader.reader.camstar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.himalayas.filereader.util.Config;

/**
 *
 * @author cjzhang
 */
public class CamRptScanner {
    
    private static Logger logger = CamstarLogger.getInstance();
    
    private static String LineSeparator = System.getProperty("line.separator");
    private static SimpleDateFormat fmtDate = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat fmtDateTime = new SimpleDateFormat("yyyyMMddHHmmss");
    private static Pattern pat1 = Pattern.compile("^TMP_SHIFT_HYGON_\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}.xls.\\d{14}$");   // TMP_SHIFT_HYGON_2018-10-25-07-50-04.xls.20181025080211 
    private static Pattern pat2 = Pattern.compile("^TMP_Weekly_HYGON_\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}.xls.\\d{14}$");   // TMP_Weekly_HYGON_2018-10-07-15-03-04.xls.20181025172758
    private static Pattern pat3 = Pattern.compile("(20\\d{2}[0-1]\\d[0-3]\\d)(\\?[0-2]\\d)?");  // 2018102508 || 20181025

    private String fileTimeLogPath = Config.camFormat.getMappingPath() + File.separator + "scanned_file_logs";
    private String fileTimeAssignSymbol = " => ";
    private Map<String, String> lastFilesVsTime = new HashMap<>();
    private Map<String, String> lastFilesVsSize = new HashMap<>();
    private Map<String ,String> fileTimeVsLogs = new TreeMap<>();
    
//------------------------------------------------------------------------------------------------------------------------

    public List<CamRpt> scan() {
        
        File camstarSrcBaseFolder = new File(Config.camFormat.getKdfPath());
        if(!camstarSrcBaseFolder.exists() || !camstarSrcBaseFolder.isDirectory()) {
            logger.info("Folder is not Exist : " + Config.camFormat.getKdfPath());
            return null;
        }
        
        File[] subFolders = camstarSrcBaseFolder.listFiles();
        List<File> camstarSrcSubFolders = new ArrayList<>();
        for(File subFolder : subFolders) {
            if(subFolder.isDirectory()) camstarSrcSubFolders.add(subFolder);
        }
        
        int rptKeepDays = Config.camFormat.getRptKeepDays();
        
        List<String> folderPathList = new ArrayList<>();
        for(File camstarSrcSubFolder : camstarSrcSubFolders) {
            String readStartDate = fmtDate.format(System.currentTimeMillis() - rptKeepDays * 24 * 3600 * 1000L);
            for(File dateDir : camstarSrcSubFolder.listFiles()) {
                if(dateDir.isDirectory()) {
                    String folderName = dateDir.getName();
                    String folderPath = dateDir.getAbsolutePath();
                    Matcher matcher = pat3.matcher(folderName);
                    if(matcher.find()) {
                        String folderDate = matcher.group(1);
                        if(folderDate.compareTo(readStartDate)>=1){
                            folderPathList.add(folderPath);
                        }
                    }
                }
            }
        }
        
        if(folderPathList.size()<1){
            return null;
        }
        
        List<File> camFilesList = new ArrayList<>();
        for(String folderPath : folderPathList) {
            File folder = new File(folderPath);
            if(folder.exists() && folder.isDirectory()) {
                String dateFolderName = folder.getName();
                File[] listFiles = folder.listFiles();
                for(File file : listFiles) {
                    if(file.exists() && file.isFile()) {
                        String fileName = file.getName();
                        if(pat1.matcher(fileName).find() || pat2.matcher(fileName).find()) {
                            camFilesList.add(file);
                        }
                    }
                }
            }
        }
        
        if(camFilesList.size()<1) {
            logger.info("No New Camstar Yield Report Found!");
            return null;
        }
        
        FileReader fileReader = null;
        BufferedReader bufReader = null;
        
        try {
            File fileTimeLog = new File(fileTimeLogPath);
            if(fileTimeLog.exists() && fileTimeLog.isFile()) {
                fileReader = new FileReader(fileTimeLogPath);
                bufReader = new BufferedReader(fileReader);
                String strFileTime = null;
                while((strFileTime = bufReader.readLine()) != null){
                    String filePath = strFileTime.split(fileTimeAssignSymbol)[0];
                    String fileTime = strFileTime.split(fileTimeAssignSymbol)[1];
                    String fileSize = strFileTime.split(fileTimeAssignSymbol)[2];
                    lastFilesVsTime.put(filePath, fileTime);
                    lastFilesVsSize.put(filePath, fileSize);
                    fileTimeVsLogs.put(strFileTime, fileTime);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            try {
                if(bufReader!=null) bufReader.close();
                if(fileReader!=null) fileReader.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        
        List<CamRpt> camRpts = new ArrayList<>();
 
        for(File camFile : camFilesList) {
            
            if(!camFile.exists() || !camFile.isFile()) { continue; }
            
            String fileName = camFile.getName();
            String filePath = camFile.getAbsolutePath();
            String currFileSize = String.valueOf(camFile.length());
            String lastFileSize = lastFilesVsSize.getOrDefault(filePath, "randomsize-" + System.currentTimeMillis());
            String currFileTime = fmtDateTime.format(camFile.lastModified());
            String lastFileTime = lastFilesVsTime.getOrDefault(filePath, "19700101000000");
            String dateFolderName = camFile.getParentFile().getName();
            String typeFolderName = camFile.getParentFile().getParentFile().getName();
            
            CamRpt camRpt = new CamRpt();
            camRpt.setRptName(fileName);
            camRpt.setRptPath(filePath);
            camRpt.setRptLastSize(lastFileSize);
            camRpt.setRptCurrSize(currFileSize);
            camRpt.setRptLastTime(lastFileTime);
            camRpt.setRptCurrTime(currFileTime);
            camRpt.setRptDateFolderName(dateFolderName);
            camRpt.setRptTypeFolderName(typeFolderName);
            
            if(Long.parseLong(currFileSize) < 1000L)
                camRpt.setRptBad(true);
            if(!currFileTime.equals(lastFileTime) || !currFileSize.equals(lastFileSize)) {
                camRpt.setRptUpdated(true);
            }
            
            camRpts.add(camRpt);
        }
        
        for(CamRpt camRpt : camRpts) {
            String updateStatus = camRpt.isRptUpdated() == true ? " --- updated" : " --- unchanged";
            logger.info("Camstar report " + camRpt.getRptPath() + updateStatus);
        }
        
        return camRpts;
    }
    
    
    public void logFileTimeSize(List<CamRpt> camRpts) {
        String currDateTime = fmtDate.format(System.currentTimeMillis() - Config.camFormat.getLogKeepDays() * 24 * 3600 * 1000L) + "000000";
        StringBuilder strBud = new StringBuilder();
            for(CamRpt camRpt : camRpts) {
                String camRptPath = camRpt.getRptPath();
                String camRptLastTime = camRpt.getRptLastTime();
                String camRptCurrTime = camRpt.getRptCurrTime();
                String camRptLastSize = camRpt.getRptLastSize();
                String camRptCurrSize = camRpt.getRptCurrSize();
                if(camRpt.isRptReadFail() || camRpt.isRptWriteFail()) {
                    strBud.append(camRptPath).append(fileTimeAssignSymbol).append(camRptLastTime).append(fileTimeAssignSymbol).append(camRptLastSize);
                    fileTimeVsLogs.put(strBud.toString(), camRptLastTime);
                } else {
                    strBud.append(camRptPath).append(fileTimeAssignSymbol).append(camRptCurrTime).append(fileTimeAssignSymbol).append(camRptCurrSize);
                    fileTimeVsLogs.put(strBud.toString(), camRptCurrTime);
                }
                strBud.setLength(0);
            }
        
        FileWriter fw = null;

        try {           
            fw = new FileWriter(fileTimeLogPath);
            for(Map.Entry<String, String> entry : fileTimeVsLogs.entrySet()) {
                String fileLog = entry.getKey();
                String fileTime = entry.getValue();
                if(fileTime.compareTo(currDateTime)>=1) {
                    fw.write(fileLog + LineSeparator);
                }
            }
            fw.flush();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            try {
                if(fw!=null) fw.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
    
}
