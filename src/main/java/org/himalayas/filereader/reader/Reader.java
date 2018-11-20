/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;
import org.himalayas.filereader.util.XmlNode;

/**
 *
 * @author ghfan
 */
public abstract
    class Reader {

    private
        File file;
    private
        boolean debugMode = false;
    private
        File testLogFile = null;
    private
        File mappingFile = null;
    private
        File doneArchiveFile = null;
    private
        File repeatArchiveFile = null;
    private
        File openErrorArchiveFile = null;
    private
        File badFormatArchiveFile = null;
    private
        File exceptionArchiveFile = null;
    private
        String kdfMonth = null;
    private
        String kdfDate = null;
    private
        String transferTime = null;
    private
        String fileName = null;
    private
        String lotNumber = null;
    private
        int fileLevelDocCnt = 0;
    private
        String fileOpenTime = null;
    private
        long jobStartTime = 0;
    private
        Config.FailureCase failType = null;
    private
        DataFormat format = null;
    private
        int unitCnt = 0;
    private
        int kdfDoneCnt = 0;

    public
        void resetAll() {
        this.testLogFile = null;
        this.mappingFile = null;
        this.doneArchiveFile = null;
        this.repeatArchiveFile = null;
        this.openErrorArchiveFile = null;
        this.badFormatArchiveFile = null;
        this.exceptionArchiveFile = null;
        this.kdfMonth = null;
        this.kdfDate = null;
        this.transferTime = null;
        this.fileName = null;
        this.lotNumber = null;
        this.fileLevelDocCnt = 0;
        this.fileOpenTime = null;
        this.failType = null;
        this.unitCnt = 0;

    }

    public
        Reader(DataFormat format) {
        this.format = format;
        this.debugMode = this.format.isDebugMode();
    }

    public final
        boolean loadFile(File file) {
        jobStartTime = System.currentTimeMillis();
        System.out.printf("\n%s: start proceed %s\n", LocalDateTime.now(), file.getName());
        try {
            this.resetAll();
            this.init();

            this.file = file;
            if (!this.preValidate()) {
                return false;
            }
            if (!this.validateFile()) {
                this.failType = Config.FailureCase.BadFormat;
                this.logBadFormatFileToES();
                this.renameOrArchiveKDF(this.badFormatArchiveFile, Config.KdfRename.badFormat);
                return false;
            }
            if (!validateKDFDate()) {
                return false;
            }

            if (this.isRepeatFile()) {
                this.failType = Config.FailureCase.RepeatKDF;
                this.logRepeatFileToES();
                this.renameOrArchiveKDF(this.repeatArchiveFile, Config.KdfRename.skip);
                return false;
            }
            if (!this.readFile()) {
                this.failType = Config.FailureCase.OpenFailure;
                this.logOpenFailureToES();
                this.renameOrArchiveKDF(this.openErrorArchiveFile, Config.KdfRename.openErr);
                return false;
            }
            if (!this.setupLogFile()) {
                return false;
            }
            if (!this.writeLogFile()) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailWriteLogFile");

                if (!this.removeTempLogFile()) {
                    this.logIoErrorToES("FailDeleteLogFile");
                }
                return false;
            }
            if (!this.generateMapFile()) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailCreateMapFile");
                if (!this.removeTempLogFile()) {
                    this.logIoErrorToES("FailDeleteLogFile");
                }
                return false;
            }
            if (!this.closeLogFile()) {
                return false;
            }
            this.logFileDoneToES();
            this.renameOrArchiveKDF(this.doneArchiveFile, Config.KdfRename.done);
            kdfDoneCnt++;

            System.out.printf("%s: successed to proceed %s\n", LocalDateTime.now(), file.getName());
            System.out.printf("%s: total reading time is : %d\n", LocalDateTime.now(), (System.currentTimeMillis() - jobStartTime));
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            this.logExceptionToES();
            this.renameOrArchiveKDF(this.getExceptionArchiveFile(), Config.KdfRename.exception);
            return false;
        }
    }

    public abstract
        boolean readFile();

    /**
     * initialization
     */
    public abstract
        void init();

    /**
     *
     * @return
     */
    protected
        boolean validateFile() {
        this.failType = null;
        if (!this.file.isFile()) {
            return false;
        }
        String names[] = null;
        String fileName = this.file.getName();
        // here please add the split word in the config file....
        if (this.getFormat().getDataType().equals(Config.DataTypes.SMAP)) {
            names = fileName.split("smap.");
        }
        else if (this.getFormat().getDataType().equals(Config.DataTypes.WAT)) {
            names = fileName.split("dis.");
        }
        else if (this.getFormat().getDataType().equals(Config.DataTypes.CAMSTAR)) {
            names = fileName.split("xls.");
        }
        else {
            System.err.println("Fatal Error: bad Data Type found: " + this.getFormat().getDataType().toString());
            System.exit(1);
        }

        if (names.length != 2) {
            return false;
        }
        // the timestamp when the kdf is moved to this place
        if (names[names.length - 1].length() != 14 || (!names[names.length - 1].startsWith("20"))) {
            return false;
        }
        this.transferTime = this.formatTimeStr(names[1]);

        this.fileName = fileName.substring(0, fileName.length() - 15);

        names = fileName.split("_");
        if (names.length != this.getFormat().getUnderLineCnt()) {
            System.out.println("Skip this kdf since underline cnt is not " + this.getFormat().getUnderLineCnt());
            return false;
        }

        if (!this.setFileDate()) {
            System.out.println("Skip since bad Format of KDF date");
            return false;
        }
        if (this.format.isLotFile()) {
            this.lotNumber = names[format.getLotNumberIndex()];
        }
        // archive file
        this.doneArchiveFile = new File(this.getFormat().getDoneArchivePath()
            + "/" + this.kdfDate
            + (this.format.isLotFile() ? ("/" + lotNumber) : "")
            + "/" + this.file.getName());

        this.badFormatArchiveFile = new File(this.getFormat().getBadFormatArchivePath()
            + "/" + this.kdfDate
            + (this.format.isLotFile() ? ("/" + lotNumber) : "")
            + "/" + this.file.getName());

        this.openErrorArchiveFile = new File(this.getFormat().getOpenErrorArchivePath()
            + "/" + this.kdfDate
            + (this.format.isLotFile() ? ("/" + lotNumber) : "")
            + "/" + this.file.getName());

        this.repeatArchiveFile = new File(this.getFormat().getRepeatArchivePath()
            + "/" + this.kdfDate
            + (this.format.isLotFile() ? ("/" + lotNumber) : "")
            + "/" + this.file.getName());
        this.exceptionArchiveFile = new File(this.getFormat().getExceptionArchivePath()
            + "/" + this.kdfDate
            + (this.format.isLotFile() ? ("/" + lotNumber) : "")
            + "/" + this.file.getName());

        // mapping file
        this.mappingFile = new File(this.getFormat().getMappingPath()
            + "/" + this.kdfDate
            + (this.format.isLotFile() ? ("/" + lotNumber) : "")
            + "/" + this.fileName);

        return true;
    }

    private
        boolean validateKDFDate() {
        if (this.getFormat().getKdfStartDate() != null
            && this.kdfDate.compareTo(this.getFormat().getKdfStartDate()) < 0) {
            System.out.printf("Warnning: this kdf file is skipped, KDFDate = %s since KDFDate Start Date filter = %s\n", this.kdfDate, this.getFormat().getKdfStartDate());
            return false;
        }
        if (this.getFormat().getKdfEndData() != null
            && this.getFormat().getKdfEndData().compareTo(this.kdfDate) < 0) {
            System.out.printf("Warnning: this kdf file is skipped, KDFDate = %s since KDFDate End Date filter = %s\n", this.kdfDate, this.getFormat().getKdfEndData());
            return false;
        }
        return true;
    }

    private
        boolean setFileDate() {
        String[] names = this.file.getName().split("_");

        if (this.format.getFileOpenTimeFormat() != null) {
            String tempDate = "";
            for (int index : format.getFileOpenTimeIndex()) {
                if (names[index].contains(".")) {
                    names[index] = names[index].substring(0, names[index].indexOf('.'));
                }
                tempDate += names[index];
            }
            if (tempDate.length() >= this.getFormat().getFileOpenTimeFormat().length()) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(tempDate.subSequence(0, this.getFormat().getFileOpenTimeFormat().length()), DateTimeFormatter.ofPattern(this.getFormat().getFileOpenTimeFormat()));
                    this.kdfMonth = dateTime.format(DateTimeFormatter.ofPattern("uuuuMMdd"));
                    this.fileOpenTime = dateTime.format(DateTimeFormatter.ofPattern("uuuuMMddHHmmss"));
                    this.kdfDate = this.kdfMonth.substring(0, 8);
                    return true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            else {
                return false;
            }
        }

        this.kdfMonth = names[format.getKdfMonthIndex()];
        if (this.kdfMonth.length() == 4) {
            //0604
            this.kdfMonth = "20" + kdfMonth;
        }
        else if (this.kdfMonth.length() == 6) {
            //180604
            this.kdfMonth = "20" + kdfMonth.substring(0, 4);
        }
        else if (this.kdfMonth.length() == 8) {
            this.kdfMonth = this.kdfMonth.substring(0, 6);
        }
        else {
            return false;
        }

        this.fileOpenTime = "";
        // set file open time
        for (int index : format.getFileOpenTimeIndex()) {
            if (names[index].contains(".")) {
                names[index] = names[index].substring(0, names[index].indexOf('.'));
            }
            fileOpenTime += names[index];
        }
        if (fileOpenTime.length() == 12) {
            fileOpenTime = "20" + fileOpenTime;
        }
        if (fileOpenTime.length() != 14) {
            System.out.println("FATAL Error: failed to get file open time from the file name");
            return false;
        }
        this.kdfDate = fileOpenTime.substring(0, 8);
        return true;
    }

    protected
        boolean preValidate() {

        if (this.file.getName().endsWith(Config.KdfRename.badFormat.name())
            || this.file.getName().endsWith(Config.KdfRename.done.name())
            || this.file.getName().endsWith(Config.KdfRename.exception.name())
            || this.file.getName().endsWith(Config.KdfRename.openErr.name())
            || this.file.getName().endsWith(Config.KdfRename.skip.name())) {
            return false;
        }

        boolean skip = false;
        for (String filter : this.getFormat().getFilters()) {
            if (this.getFile().getName().contains(filter)) {
                skip = true;
                break;
            }
        }
        if (skip) {
            return false;
        }

        for (String selector : this.getFormat().getSelectors()) {
            if (!this.getFile().getName().contains(selector)) {
                skip = true;
                break;
            }
        }
        if (skip) {
            return false;
        }
        return true;

    }

    /**
     * return the local datatime with offset this input time string format must
     * be 'uuuuMMddhhmmss'
     *
     * @param timeStr
     * @return
     */
    private
        String formatTimeStr(String timeStr) {
        return timeStr.substring(0, 4) + "-" + timeStr.substring(4, 6) + "-" + timeStr.substring(6, 8)
            + "T" + timeStr.substring(8, 10) + ":" + timeStr.substring(10, 12) + ":"
            + timeStr.substring(12, 14) + ".00+08:00";
    }

    private
        boolean setupLogFile() {
        testLogFile = new File(this.getFormat().getXmlPath() + "/" + this.file.getName());
        if (!this.removeTempLogFile()) {
            this.logIoErrorToES("FailDeleteLogFile");
            this.failType = Config.FailureCase.IOError;
            return false;
        }
        return true;
    }

    /**
     * generate the kdf mapping file and for repeat kdf file check
     *
     * @return
     */
    private
        boolean generateMapFile() {
        if (!this.getFormat().isGenerateMappingFile()) {
            return true;
        }
        try {
            File parentFile = this.mappingFile.getParentFile();
            if (parentFile.exists() && parentFile.isDirectory()) {
            }
            else {
                if (!parentFile.mkdirs()) {
                    return false;
                }
            }
            if (this.mappingFile.createNewFile()) {
                return true;
            }
            else {
                return false;
            }
        }
        catch (IOException ex) {
            Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private
        boolean closeLogFile() {
        // only rename the log file in production mode
        if ((this.getFormat().isProductionMode()) && (!this.renameTempLogFile())) {
            this.failType = Config.FailureCase.IOError;
            this.logIoErrorToES("FailRenameLog");
            if (!this.removeTempLogFile()) {
                this.logIoErrorToES("FailDeleteLogFile");
            }

            if (!this.removeMapFile()) {
                this.logIoErrorToES("FailDeleteMapFile");
            }
            return false;
        }
        return true;
    }

    /**
     * method to remove the temp log file if any failure
     *
     * @return
     */
    private
        boolean removeTempLogFile() {
        if (this.testLogFile.exists()) {
            try {
                Files.delete(this.testLogFile.toPath());

            }
            catch (IOException ex) {
                Logger.getLogger(Reader.class
                    .getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return true;

    }

    /**
     * method to remove the mapping file if any failure
     *
     * @return
     */
    private
        boolean removeMapFile() {
        if (this.mappingFile.exists()) {
            try {
                Files.delete(this.mappingFile.toPath());

            }
            catch (IOException ex) {
                Logger.getLogger(Reader.class
                    .getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return true;
    }

    /**
     * rename the source file
     *
     * @return
     */
    private
        boolean renameSourceFile() {
        if (Config.renameKDF) {
            File kdfedFile = new File(this.file.getAbsolutePath() + "." + Config.KdfRename.done);
            return this.file.renameTo(kdfedFile);
        }
        return true;
    }

    private
        boolean renameTempLogFile() {
        File esTestFile = new File(this.testLogFile.getAbsolutePath() + ".log");
        return this.testLogFile.renameTo(esTestFile);
    }

    private
        boolean renameKDFFile(Config.KdfRename rename) {
        if (Config.renameKDF) {
            File kdfedFile = new File(this.file.getAbsolutePath() + "." + rename);
            return this.file.renameTo(kdfedFile);
        }
        return true;
    }

    public
        boolean renameOrArchiveKDF(File destinationFile, Config.KdfRename rename) {
        if (destinationFile == null) {
            return false;
        }
        if (this.getFormat().isProductionMode()) {
            if ((!Config.renameKDF) && (!this.moveFileToArchive(destinationFile))) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailArchiveKDF");

                /**
                 * here we can rename the kdf if failed archive to avoid ....
                 */
                if (!this.renameKDFFile(rename)) {
                    this.failType = Config.FailureCase.IOError;
                    this.logIoErrorToES("FailRenameKDF");
                    return false;
                }
                else {
                    return true;
                }
            }

            if (!this.renameKDFFile(rename)) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailRenameKDF");
                return false;
            }
            return true;
        }
        return true;
    }

    private
        boolean moveFileToArchive(File destinationFile) {
        try {
            if (destinationFile.exists()) {
                Files.delete(this.file.toPath());
            }
            else {
                if (!destinationFile.getParentFile().exists()) {
                    if (!destinationFile.getParentFile().mkdirs()) {
                        this.logIoErrorToES("FailMkDIR");
                        return false;
                    }
                }
            }
            Files.move(this.file.toPath(), destinationFile.toPath(), ATOMIC_MOVE);
        }
        catch (IOException ex) {
            System.out.println("EventType:ArchieveFailure");
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private
        void logBadFormatFileToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFBadFormat,
            FieldType.FileName, this.file.getName(),
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    private
        boolean isRepeatFile() {
        if (this.mappingFile.exists()) {
            System.out.println("Skip since this is a repeat file");
            return true;
        }
        return false;
    }

    public
        void logRepeatFileToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFRepeat,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.fileName,
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    public
        void logOpenFailureToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFOpenFailure,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.fileName,
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    public
        void logIoErrorToES(String error) {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.IOError,
            FieldType.Failure, error,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.file.getName(),
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    public 
        void logFileDoneToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%d,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFDone,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            FieldType.KdfName, this.fileName,
            FieldType.UnitCnt, this.unitCnt,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    public
        void logExceptionToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFException,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.fileName,
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    protected
        String generateLotHeadKVStr() {
        String value = "";
        String[] names = this.file.getName().split("_");
        int i = 0;
        for (XmlNode node : this.getFormat().getLotHead().values()) {
            if (node.isEnabled() && node.isEnabledLog()) {
                if (i != 0) {
                    value += ",";
                }
                value += node.getName() + "=" + names[node.getIndex()];
                i++;
            }
        }
        value += "," + FieldType.FileTime + "=" + this.formatTimeStr(this.fileOpenTime);
        if (this.getFormat().isAddFileName()) {
            value += "," + FieldType.FileName + "=" + this.fileName;
        }
        return value;
    }

    /**
     * write the kv string in to log file
     *
     * @return
     */
    protected
        boolean writeKVString(String dataContent) {
        if (this.getFormat().isLogToFile()) {
            try {
                Files.write(testLogFile.toPath(), dataContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            }
            catch (IOException ex) {
                Logger.getLogger(Reader.class
                    .getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
        }
        return true;
    }

    public
        File getFile() {
        return file;
    }

    public
        boolean isDebugMode() {
        return debugMode;
    }

    public
        File getTestLogFile() {
        return testLogFile;
    }

    public
        File getMappingFile() {
        return mappingFile;
    }

    public
        String getFileMonth() {
        return kdfMonth;
    }

    public
        String getFileDate() {
        return kdfDate;
    }

    public
        String getTransferTime() {
        return transferTime;
    }

    public
        String getFileName() {
        return fileName;
    }

    public
        String getLotNumber() {
        return lotNumber;
    }

    public
        int getFileLevelDocCnt() {
        return fileLevelDocCnt;
    }

    public
        long getJobStartTime() {
        return jobStartTime;
    }

    public
        Config.FailureCase getFailType() {
        return failType;
    }

    public
        DataFormat getFormat() {
        return format;
    }

    public
        void setUnitCnt(int unitCnt) {
        this.unitCnt = unitCnt;
    }

    public
        int getKdfDoneCnt() {
        return kdfDoneCnt;
    }

    private
        File getExceptionArchiveFile() {
        return exceptionArchiveFile;
    }

    protected abstract
        boolean writeLogFile();

}
