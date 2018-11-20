package org.himalayas.filereader.reader.camstar;

import com.amd.kdf.KDFFieldData;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.FieldType;
import org.himalayas.filereader.util.XmlNode;

public class CamRptReader {

    private static Logger logger = CamstarLogger.getInstance();
    private static String logFieldSplitter;
    private static String logKVAssignSymbol;
    private static String timeZone;
    private static Map<String, Map<String, String>> camFieldConfByName = new LinkedHashMap<>();
    private static Map<Integer, Map<String, String>> camFieldConfByIndex = new LinkedHashMap<>();
    private static SimpleDateFormat fmtDateTime1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS");
    private static SimpleDateFormat fmtDateTime2 = new SimpleDateFormat("yyyyMMddHHmmss");

    private CamRpt camRpt;
    private List<LinkedHashMap<String, String>> goodLines = new ArrayList<>();
    private List<LinkedHashMap<String, String>> badLines = new ArrayList<>();
    private LinkedHashMap<Integer, String> colNumVsHead = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> headVsColNum = new LinkedHashMap<>();

    public CamRptReader() {
    }

    public CamRptReader(CamRpt camRpt) {
        this.camRpt = camRpt;
        this.camRpt.setRptSkip(false);
    }

    public static boolean init() {
        logFieldSplitter = ",";
        logKVAssignSymbol = "=";
        timeZone = "+08:00";
        
        LinkedHashMap<String, XmlNode> lotHead = Config.camFormat.getLotHead();
        
        for(Entry<String, XmlNode> entry : lotHead.entrySet()) {
            String camLotHeadName = entry.getKey();
            if(camLotHeadName.equals("camSpeed")) {
                System.out.println("");
            }
            XmlNode camNode = entry.getValue();
            int camFieldIndex = camNode.getIndex();
            String camFieldName = camNode.getCamColumnName();
            String camFieldType = "";
            String camFieldEnableLog = camNode.isEnabledLog() ? "Y" : "N";
            String camFieldAllowEmpty = camNode.isAllowEmpty() ? "Y" : "N";
            
            switch (camNode.getCamColumnType()) {
                case KDFFieldData.STRING:
                    camFieldType = "String";
                    break;
                case KDFFieldData.INT:
                    camFieldType = "Integer";
                    break;
                case KDFFieldData.FLOAT:
                    camFieldType = "Float";
                    break;
                case FieldType.Date:
                    camFieldType = "Date";
                    break;
                default:
                    camFieldType = "String";
                    break;
            }
            
            Map<String, String> map = new HashMap<>();            
            map.put("Index", String.valueOf(camFieldIndex));
            map.put("Name", camFieldName);
            map.put("Type", camFieldType);
            map.put("AllowEmpty", camFieldAllowEmpty);
            map.put("Alias", camLotHeadName);
            map.put("EnableLog", camFieldEnableLog);
            
            camFieldConfByName.put(camFieldName, map);
            camFieldConfByIndex.put(camFieldIndex, map);
        }
        
        return true;
    }

    public int read() {

        Workbook wb = null;
        Sheet sh = null;

        String rptPath = camRpt.getRptPath();

        try {

            wb = WorkbookFactory.create(new File(rptPath));

            if (wb == null) {
                camRpt.setRptReadFail(true);
                throw new Exception("Can't open workbook " + rptPath);
            }

            sh = wb.getSheet("Sheet1");
            if (sh == null) {
                camRpt.setRptReadFail(true);
                throw new Exception("Can't find Sheet1 from workbook " + rptPath);
            }

            int staRowNum = sh.getFirstRowNum();
            int endRowNum = sh.getLastRowNum();
            
            Row headRow = sh.getRow(staRowNum);

            int staColNum = headRow.getFirstCellNum();
            int endColNum = headRow.getLastCellNum() - 1;

            for (int colNum = staColNum; colNum <= endColNum; colNum++) {
                Cell cell = headRow.getCell(colNum);
                if (cell != null && ("STRING").equalsIgnoreCase(cell.getCellTypeEnum().name())) {
                    String headName = cell.getStringCellValue();
                    colNumVsHead.put(colNum, headName);
                    headVsColNum.put(headName, colNum);
                }
            }
            
            // Empty Report Check
            // rowNum == 1 and some key fields are empty.
            if(endRowNum == 1) {
                Row row = sh.getRow(1);
                List<String> cellNullEmptyValues = new ArrayList<>();
                cellNullEmptyValues.add("CELL-EMPTY-ERROR");
                cellNullEmptyValues.add("CELL-NULL-ERROR");
                int colNum_Pline = headVsColNum.getOrDefault("Pline", 0);
                int colNum_Insert = headVsColNum.getOrDefault("Insert", 0);
                int colNum_Device = headVsColNum.getOrDefault("Device", 3);
                int colNum_Lot = headVsColNum.getOrDefault("Lot", 5);
                int colNum_Date = headVsColNum.getOrDefault("Date", 6);
                int colNum_Oper = headVsColNum.getOrDefault("Oper", 7);
                String cellVal_Pline = getCellValue(row.getCell(colNum_Pline), 1, colNum_Pline);
                String cellVal_Insert = getCellValue(row.getCell(colNum_Insert), 1, colNum_Insert);
                String cellVal_Device = getCellValue(row.getCell(colNum_Device), 1, colNum_Device);
                String cellVal_Lot = getCellValue(row.getCell(colNum_Lot), 1, colNum_Lot);
                String cellVal_Date = getCellValue(row.getCell(colNum_Date), 1, colNum_Date);
                String cellVal_Oper = getCellValue(row.getCell(colNum_Oper), 1, colNum_Oper);
                boolean isPlineEmpty = cellNullEmptyValues.contains(cellVal_Pline);
                boolean isDeviceEmpty = cellNullEmptyValues.contains(cellVal_Device);
                boolean isLotEmpty = cellNullEmptyValues.contains(cellVal_Lot);
                boolean isDateEmpty = cellNullEmptyValues.contains(cellVal_Date);
                boolean isOperEmpty = cellNullEmptyValues.contains(cellVal_Oper);
                boolean isInsertInvalid = "0".equals(cellVal_Insert);
                if(isPlineEmpty && isDeviceEmpty && isLotEmpty && isDateEmpty && isOperEmpty && isInsertInvalid) {
                    wb.close();
                    camRpt.setRptEmpty(true);
                    return 1;
                }
            }
           
            for (int rowNum = staRowNum + 1; rowNum <= endRowNum; rowNum++) {
                
                LinkedHashMap<String, String> line = new LinkedHashMap<>();
                boolean lineHasError = false;
                Row row = sh.getRow(rowNum);
                
                String lotID = "", lotOper = "", lotDate = "";
                
                for (int colNum = staColNum; colNum <= endColNum; colNum++) {
                    Cell cell = row.getCell(colNum);
                    String cellHeadName = colNumVsHead.getOrDefault(colNum, "unknow");
                    String cellValue = getCellValue(cell, rowNum, colNum);
                    switch(cellValue) {
                        case "CELL-NULL-ERROR" : {
                            lineHasError = true;
                            line.put(cellHeadName, "CELL-NULL-ERROR");
                            break;
                        }
                        case "CELL-EMPTY-ERROR" : {
                            lineHasError = true;
                            line.put(cellHeadName, "CELL-EMPTY-ERROR");
                            break;
                        }
                        case "CELL-TYPE-ERROR" : {
                            lineHasError = true;
                            line.put(cellHeadName, "CELL-TYPE-ERROR");
                            break;
                        }
                        case "CELL-UNEXPECTED-CHARS" : {
                            lineHasError = true;
                            line.put(cellHeadName, "CELL-UNEXPECTED-CHARS");
                            break;                            
                        }
                        case "" : {
                            // skip empty cell
                            // line.put(colNumVsHead.getOrDefault(colNum, "unknow"), cellValue);
                            break;
                        }
                        default : {
                            line.put(cellHeadName, cellValue);
                            break;
                        }
                    }
                    
                    switch(cellHeadName) {
                        case "Lot" :  { lotID = cellValue;   break; }
                        case "Date" : { lotDate = cellValue; break; }
                        case "Oper" : { lotOper = cellValue; break; }
                        default: break;
                    }

                }
                
                if(lotDate.contains(timeZone)) lotDate = lotDate.replace(timeZone, "");
                lotDate = fmtDateTime2.format(fmtDateTime1.parse(lotDate));
                
                String camKey = lotID + "-" + lotDate + "-" + lotOper;
                line.put("CamKey", camKey);
                line.put("IsLotMatched", "N");
                line.put("IsLotCal", "N");
                
                if(lineHasError == false) {
                    goodLines.add(line);
                } else {
                    badLines.add(line);
                }
                
            }

            if(badLines.size()>0) {
                camRpt.setRptHasError(true);
            }
            
            camRpt.setRptGoodLines(goodLines);
            camRpt.setRptBadLines(badLines);
            
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            camRpt.setRptReadFail(true);
            return -1;
        } finally {
            try {
                if(wb!=null) wb.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        return 0;
    }


    public String getCellValue(Cell cell, int rowNum, int colNum) {
        String camRptName = camRpt.getRptName();
        String cellHeadName, cellTypeConf, cellAllowEmptyConf, cellTypeName, cellValue;
        cellHeadName = colNumVsHead.getOrDefault(colNum, "null");
        Map<String, String> headAttr = camFieldConfByName.getOrDefault(cellHeadName, null);

        if (headAttr != null) {
            cellTypeConf = headAttr.getOrDefault("Type", "");
            cellAllowEmptyConf = headAttr.getOrDefault("AllowEmpty", "");
        } else {
            cellTypeConf = "";
            cellAllowEmptyConf = "N";
        }

        if(cell == null) {
            if("N".equalsIgnoreCase(cellAllowEmptyConf)) {
            logger.info(errorMsg2(camRptName, rowNum, colNum, "Null"));
                return "CELL-NULL-ERROR";                
            } else {
                return "";
            }
        }
        
        cellValue = "";
        cellTypeName = cell.getCellTypeEnum().name();

        if("BLANK".equals(cellTypeName)) {
            if("N".equalsIgnoreCase(cellAllowEmptyConf)) {
                logger.info(errorMsg2(camRptName, rowNum, colNum, "Empty"));
                return "CELL-EMPTY-ERROR";
            } else {
                return "";
            }
        }
        
        switch (cellTypeConf) {
            case "String": {
                if("STRING".equals(cellTypeName)) {
                    cellValue = cell.getStringCellValue();
                } else {
                    //rptErrors.add(errorMsg);
                    logger.info(errorMsg(camRptName, rowNum, colNum, cellTypeConf, cellTypeName));
                    cellValue = "CELL-TYPE-ERROR";
                }
                break;
            }
            case "Integer": {
                if ("NUMERIC".equals(cellTypeName)) {
                    cellValue = String.valueOf(new DecimalFormat("0").format(cell.getNumericCellValue()));
                    if (cellValue.endsWith(".0")) {
                        cellValue = cellValue.substring(0, cellValue.length() - 2);
                    }
                } else {
                    //rptErrors.add(errorMsg);
                    logger.info(errorMsg(camRptName, rowNum, colNum, cellTypeConf, cellTypeName));
                    cellValue = "CELL-TYPE-ERROR";
                }
                break;
            }
            case "Float": {
                if ("NUMERIC".equals(cellTypeName)) {
                    cellValue = String.valueOf(new DecimalFormat("#.##").format(cell.getNumericCellValue()));
                } else {
                    //rptErrors.add(errorMsg);
                    logger.info(errorMsg(camRptName, rowNum, colNum, cellTypeConf, cellTypeName));
                    cellValue = "CELL-TYPE-ERROR";
                }
                break;
            }
            case "Date": {
                if ("NUMERIC".equals(cellTypeName)) {
                    cellValue = fmtDateTime1.format(cell.getDateCellValue()) + timeZone;
                } else {
                    //rptErrors.add(errorMsg);
                    logger.info(errorMsg(camRptName, rowNum, colNum, cellTypeConf, cellTypeName));
                    cellValue = "CELL-TYPE-ERROR";
                }
                break;
            }
            default: {
                break;
            }
        }
        
        cellValue = cellValue.trim();
        
        if("N".equalsIgnoreCase(cellAllowEmptyConf) && "".equals(cellValue)) {
            logger.info(errorMsg2(camRptName, rowNum, colNum, "Empty"));
            cellValue = "CELL-EMPTY-ERROR";
        }
        
        if(cellValue.contains(logFieldSplitter) || cellValue.contains(logKVAssignSymbol)) {
            logger.info(errorMsg2(camRptName, rowNum, colNum, "Unexpected"));
            cellValue = "CELL-UNEXPECTED-CHARS";
        }
            
        return cellValue.trim();
    }

    
    private String errorMsg(String camRptName, int cellX, int cellY, String cellFmtConf, String cellFmtType) {
        String errorMsgTmp = "CamRptName, Cell(CellX,CellY) - CellHead, Expected CellFmtConf Type But CellFmtType Found !!";
        String errorMsg = errorMsgTmp.replace("CamRptName", camRptName)
                                     .replace("CellX", String.valueOf(cellX))
                                     .replace("CellY", String.valueOf(cellY))
                                     .replace("CellFmtType", cellFmtType)
                                     .replace("CellHead",colNumVsHead.getOrDefault(cellY, "unknow cell head") );
        if("Date".equalsIgnoreCase(cellFmtConf)) {
            errorMsg = errorMsg.replace("CellFmtConf", "Date(NUMERIC)");
        } else {
            errorMsg = errorMsg.replace("CellFmtConf", cellFmtConf);
        }
        return errorMsg;
    }
    
    
    private String errorMsg2(String camRptName, int cellX, int cellY, String errorType) {
        String errorMsgTmp = "";
        switch(errorType) {
            case "Null"       : { errorMsgTmp = "CamRptName, Cell(CellX,CellY) - CellHead, Null Error!!";               break; }
            case "Empty"      : { errorMsgTmp = "CamRptName, Cell(CellX,CellY) - CellHead, Empty Error!!";              break; }
            case "Unexpected" : { errorMsgTmp = "CamRptName, Cell(CellX,CellY) - CellHead, Contain Unexpected Chars!!"; break; }
            default           : { errorMsgTmp = "CamRptName, Cell(CellX,CellY) - CellHead, Has Error!!";                break; }
        }
        String errorMsg = errorMsgTmp.replace("CamRptName", camRptName)
                                             .replace("CellX", String.valueOf(cellX))
                                             .replace("CellY", String.valueOf(cellY))
                                             .replace("CellHead",colNumVsHead.getOrDefault(cellY, "unknow cell head") );
        return errorMsg;
    }
     
}
