package org.himalayas.filereader.reader.camstar;

import java.util.LinkedHashMap;
import java.util.List;

public class CamRpt {
    
    private String rptName;
    private String rptPath;
    private String rptType;
    private String rptLastSize;
    private String rptCurrSize;
    private String rptLastTime;
    private String rptCurrTime;
    private String rptDateFolderName;
    private String rptTypeFolderName;
    private List<String> rptErrors;
    private List<LinkedHashMap<String, String>> rptGoodLines;
    private List<LinkedHashMap<String, String>> rptBadLines;
    private boolean rptUpdated = true;
    private boolean rptBad = false;        // Config.KdfRename.badFormat
    private boolean rptEmpty = false;      // Config.KdfRename.Done
    private boolean rptReadFail = false;   // Config.KdfRename.openErr
    private boolean rptWriteFail = false;  // Config.KdfRename.IOError
    private boolean rptHasError = false;   // Config.KdfRename.Exception
    private boolean rptDone = false;       // Config.KdfRename.Done
    private boolean rptSkip = true;

    public void setRptName(String rptName) {
        this.rptName = rptName;
    }

    public void setRptPath(String rptPath) {
        this.rptPath = rptPath;
    }

    public void setRptType(String rptType) {
        this.rptType = rptType;
    }

    public void setRptLastSize(String rptLastSize) {
        this.rptLastSize = rptLastSize;
    }

    public void setRptCurrSize(String rptCurrSize) {
        this.rptCurrSize = rptCurrSize;
    }
    public void setRptLastTime(String rptModifyTime) {
        this.rptLastTime = rptModifyTime;
    }

    public void setRptCurrTime(String rptCurrTime) {
        this.rptCurrTime = rptCurrTime;
    }

    public void setRptDateFolderName(String rptDateFolderName) {
        this.rptDateFolderName = rptDateFolderName;
    }

    public void setRptTypeFolderName(String rptTypeFolderName) {
        this.rptTypeFolderName = rptTypeFolderName;
    }
    
    public void setRptErrors(List<String> rptErrors) {
        this.rptErrors = rptErrors;
    }

    public void setRptGoodLines(List<LinkedHashMap<String, String>> rptGoodLines) {
        this.rptGoodLines = rptGoodLines;
    }

    public void setRptBadLines(List<LinkedHashMap<String, String>> rptBadLines) {
        this.rptBadLines = rptBadLines;
    }

    public void setRptBad(boolean rptBad) {
        this.rptBad = rptBad;
    }

    public void setRptEmpty(boolean isRptEmpty) {
        this.rptEmpty = isRptEmpty;
    }

    public void setRptUpdated(boolean isRptUpdated) {
        this.rptUpdated = isRptUpdated;
    }
    
    public void setRptReadFail(boolean rptReadFail) {
        this.rptReadFail = rptReadFail;
    }

    public void setRptWriteFail(boolean rptWriteFail) {
        this.rptWriteFail = rptWriteFail;
    }

    public void setRptDone(boolean rptDone) {
        this.rptDone = rptDone;
    }

    public void setRptHasError(boolean rptHasError) {
        this.rptHasError = rptHasError;
    }

    public void setRptSkip(boolean rptSkip) {
        this.rptSkip = rptSkip;
    }

    public String getRptName() {
        return rptName;
    }
    
    public String getRptPath() {
        return rptPath;
    }
    
    public String getRptType() {
        return rptType;
    }
    
    public String getRptLastSize() {
        return rptLastSize;
    }

    public String getRptCurrSize() {
        return rptCurrSize;
    }

    public String getRptLastTime() {
        return rptLastTime;
    }

    public String getRptCurrTime() {
        return rptCurrTime;
    }

    public String getRptDateFolderName() {
        return rptDateFolderName;
    }

    public String getRptTypeFolderName() {
        return rptTypeFolderName;
    }
    
    public List<String> getRptErrors() {
        return rptErrors;
    }

    public List<LinkedHashMap<String, String>> getRptGoodLines() {
        return rptGoodLines;
    }

    public List<LinkedHashMap<String, String>> getRptBadLines() {
        return rptBadLines;
    }

    public boolean isRptBad() {
        return rptBad;
    }

    public boolean isRptEmpty() {
        return rptEmpty;
    }

    public boolean isRptUpdated() {
        return rptUpdated;
    }

    public boolean isRptReadFail() {
        return rptReadFail;
    }

    public boolean isRptWriteFail() {
        return rptWriteFail;
    }

    public boolean isRptDone() {
        return rptDone;
    }

    public boolean isRptHasError() {
        return rptHasError;
    }

    public boolean isRptSkip() {
        return rptSkip;
    }
    
}
