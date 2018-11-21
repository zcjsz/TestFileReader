package org.himalayas.filereader.reader.camstar;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.himalayas.filereader.util.Config;

public class CamstarLogger {
    
    public static final Logger logger = Logger.getLogger(CamstarLogger.class);
    private String logDir;
    
    private static volatile CamstarLogger myLogger;

    public static Logger getInstance(){
        if (myLogger == null) {
            synchronized (CamstarLogger.class) {
                if (myLogger == null) {
                    myLogger = new CamstarLogger();
                    myLogger.setLogger();
                }
            }
        }
        return logger;
    }
    
    private CamstarLogger(){}
 
    
    public void setLogger(){
        String fileName = "cam.log";
        String filePath = Config.camFormat.getMappingPath() + File.separator + fileName;
        
        try {
            PatternLayout layout1 = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] %-5p %F:%L - %m%n");
            RollingFileAppender appender1 = new RollingFileAppender(layout1, filePath, true);
            appender1.setMaxFileSize("10MB");
            appender1.setMaxBackupIndex(10);
            logger.addAppender(appender1);
            logger.setLevel(Level.INFO);            
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public void resetLogger(){     
        
        String fileName = "cam.log";
        String filePath = Config.camFormat.getMappingPath() + File.separator + fileName;
        
        Enumeration loggerEnum = logger.getAllAppenders();
        
        if(loggerEnum.hasMoreElements()){
            RollingFileAppender appender1 = (RollingFileAppender) loggerEnum.nextElement();
            logger.removeAppender(appender1);
            
            PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] %-5p %F:%L - %m%n");
            RollingFileAppender appender2;
            try {
                appender2 = new RollingFileAppender(layout, filePath, true);
                appender2.setMaxFileSize("2MB");
                appender2.setMaxBackupIndex(10);
                logger.addAppender(appender2);
                logger.setLevel(Level.INFO);            
                logger.info("Reset To Log File : " + filePath);                
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(CamstarLogger.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
    }
    
}
