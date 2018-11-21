/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.camstar;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;
import org.himalayas.filereader.util.XmlNode;

/**
 *
 * @author ghfan
 */
public
    class CamstarReader extends Reader {
    
    private static Map<File, CamRpt> camRptFileMap = new HashMap<>();

    public 
        CamstarReader(DataFormat format) {
        super(format);
    }

    
//====================================================================================================
    @Override
    public void init() {
    }

    @Override
    public boolean readFile() {
        CamRptReader camRptReader = new CamRptReader(camRptFileMap.get(this.getFile()));
        if(camRptReader.read()<0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean writeLogFile() {
        CamRptWriter camRptWriter = new CamRptWriter(this, camRptFileMap.get(this.getFile()));
        return camRptWriter.writeGoodLines() && camRptWriter.writeBadLines();
    }
        
/*
    @Override
    public boolean setFileDate() {
        String fileDateTime = this.getFileName().split(".xls")[0].split("_")[this.getFormat().getKdfMonthIndex()];
        String[] aryDateTime = fileDateTime.split("-");
        if(aryDateTime.length != 6) return false;
        this.setFileOpenTime(fileDateTime.replaceAll("-", ""));
        this.setKdfMonth(aryDateTime[0] + aryDateTime[1]);
        this.setKdfDate(aryDateTime[0] + aryDateTime[1] + aryDateTime[2]);
        return true;
    }
*/
/*
    @Override
    public boolean setArchive() {        
        String archiveFolder = this.getFormat().getKdfArchivePath();
        String mappingFolder = this.getFormat().getMappingPath();
        String typeFolderName = this.getFile().getParentFile().getParentFile().getName();
        String dateFolderName = this.getFile().getParentFile().getName();
        String archivePath = archiveFolder + "/" + "EventType" + "/" + typeFolderName + "/" + dateFolderName + "/" + this.getFileName();
        String mappingPath = mappingFolder + "/" + typeFolderName + "/" + dateFolderName + "/" + this.getFileName();       
        this.setDoneArchiveFile(new File(archivePath.replace("EventType", Config.EventType.KDFDone.name())));
        this.setBadFormatArchiveFile(new File(archivePath.replace("EventType", Config.EventType.KDFBadFormat.name())));
        this.setOpenErrorArchiveFile(new File(archivePath.replace("EventType", Config.EventType.KDFOpenFailure.name())));
        this.setRepeatArchiveFile(new File(archivePath.replace("EventType", Config.EventType.KDFRepeat.name())));
        this.setExceptionArchiveFile(new File(archivePath.replace("EventType", Config.EventType.KDFException.name())));
        this.setMappingFile(new File(mappingPath));
        return true;
    }
*/

    @Override
    public void logRepeatFileToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFRepeat,
            FieldType.FileName, this.getFileName(),
            FieldType.FileMonth, this.getFileMonth(),
            FieldType.FileDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    @Override
    public void logOpenFailureToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFOpenFailure,
            FieldType.FileName, this.getFileName(),
            FieldType.FileMonth, this.getFileMonth(),
            FieldType.FileDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType()
        ); 
    }

    @Override
    public void logIoErrorToES(String error) {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.IOError,
            FieldType.Failure, error,
            FieldType.FileName, this.getFileName(),
            FieldType.FileMonth, this.getFileMonth(),
            FieldType.FileDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType()
        );
    }
    
    @Override
    public void logExceptionToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFException,
            FieldType.FileName, this.getFileName(),
            FieldType.FileMonth, this.getFileMonth(),
            FieldType.FileDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType()
        );
    }
    
    @Override
    public void logFileDoneToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%d,%s=%d,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFDone,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            FieldType.FileName, this.getFileName(),
            FieldType.CamGoodCnt, camRptFileMap.get(this.getFile()).getRptGoodLines().size(),
            FieldType.CamBadCnt, camRptFileMap.get(this.getFile()).getRptBadLines().size(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    
//====================================================================================================
    
    public static
        void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new Config("config/dataformat.xml");
        
        Logger logger = CamstarLogger.getInstance();
        if(!CamRptReader.init()) return;
        if(!CamRptWriter.init()) return;
        
        //Config.camFormat.setProductionMode(true);
        
        CamRptScanner camstarScanner = new CamRptScanner();
        List<CamRpt> camRpts = camstarScanner.scan();
        if(camRpts==null || camRpts.size()<1) { return; }
        
        Reader reader = new CamstarReader(Config.camFormat);
        for (CamRpt camRpt : camRpts) {
            File camRptFile = new File(camRpt.getRptPath());
            CamstarReader.camRptFileMap.put(camRptFile, camRpt);
            if(!camRpt.isRptBad() && reader.loadFile(camRptFile)) {
                String resultMsg = "Read Camstar Successfully : " + camRpt.getRptPath();
                logger.info(resultMsg);
                if(camRpt.isRptEmpty())    { resultMsg = "Camstar Report is Empty : "  + camRpt.getRptName(); }
                if(camRpt.isRptHasError()) { resultMsg = "Camstar Report has Line Error : " + camRpt.getRptName(); }
                if(camRpt.isRptDone())     { resultMsg = "Camstar Report is Done : "      + camRpt.getRptName(); }
                logger.info(resultMsg);
            } else {
                String errorMsg = "";
                if(reader.getFailType()!=null) {
                    switch(reader.getFailType()) {
                        case BadFormat:   { errorMsg = "Read Camstar Error - BadFormat : "   + camRpt.getRptPath(); break; }
                        case OpenFailure: { errorMsg = "Read Camstar Error - OpenFailure : " + camRpt.getRptPath(); break; }
                        case Exception:   { errorMsg = "Read Camstar Error - Exception : "   + camRpt.getRptPath(); break; }
                        case RepeatKDF:   { errorMsg = "Read Camstar Error - RepeatKDF : "   + camRpt.getRptPath(); break; }
                        case IOError:     { errorMsg = "Read Camstar Error - IOError : "     + camRpt.getRptPath(); break; }
                        default: break;
                    }
                    logger.info(errorMsg);
                }
                if(camRpt.isRptSkip())      { errorMsg = "Camstar Report is Skipped : "     + camRpt.getRptName(); }
                if(camRpt.isRptBad())       { errorMsg = "Camstar Report is a Bad File : "     + camRpt.getRptName(); }
                if(camRpt.isRptReadFail())  { errorMsg = "Camstar Report has Read Failure : "  + camRpt.getRptName(); }
                if(camRpt.isRptWriteFail()) { errorMsg = "Camstar Report has Write Failure : " + camRpt.getRptName(); }
                logger.info(errorMsg);
            }
        }
        
        camstarScanner.logFileTimeSize(camRpts);

        System.out.println("total time = " + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

    }

}
