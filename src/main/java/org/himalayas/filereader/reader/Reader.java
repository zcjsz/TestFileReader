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
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;

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
		File archiveFile = null;
	private
		String fileMonth = null;
	private
		String fileDate = null;
	private
		String transferTime = null;
	private
		String fileName = null;
	private
		String lotNumber = null;
	private
		String mfgStp = null;
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

	public
		Reader(DataFormat format) {
		this.format = format;
	}

	public final
		boolean loadFile(File file) {
		jobStartTime = System.currentTimeMillis();
		System.out.printf("\n%s: start proceed kdf %s\n", LocalDateTime.now(), file.getName());

		this.file = file;
		this.debugMode = this.format.isDebugMode();
		this.init();

		if (!this.validateFile()) {
			this.failType = Config.FailureCase.BadFormat;
			this.logBadFormatFileToES();
			return false;
		}
		if (this.isRepeatFile()) {
			this.failType = Config.FailureCase.RepeatKDF;
			this.logRepeatFileToES();
			return false;
		}
		if (this.readFile()) {
			if (!this.setupLogFile()) {
				return false;
			}
			if (!this.writeLogFile()) {
				return false;
			}
			if (!this.generateMapFile()) {
				return false;
			}
			if (!this.closeLogFile()) {
				return false;
			}
			this.logFileDoneToES();
			this.renameAndArchiveSrcFile();

		}
		System.out.printf("%s: successed to proceed %s\n", LocalDateTime.now(), file.getName());
		System.out.printf("%s: total reading time is : %d\n", LocalDateTime.now(), (System.currentTimeMillis() - jobStartTime));
		return true;
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
	public
		boolean validateFile() {
		this.failType = null;

		String fileName = this.file.getName();
		// here please add the split word in the config file....
		String names[] = fileName.split("dis.|wat.");

		if (names.length != 2) {
			return false;
		}
		if (!this.file.isFile()) {
			return false;
		}
		// the timestamp when the kdf is moved to this place
		if (names[names.length - 1].length() != 14 || (!names[names.length - 1].startsWith("20"))) {
			return false;
		}
		this.transferTime = this.formatTimeStr(names[1]);
		this.fileName = names[0] + "dis.|wat";

		boolean skip = false;
		for (String filter : this.getFormat().getFilters()) {
			if (fileName.contains(filter)) {
				skip = true;
				break;
			}
		}
		if (skip) {
			return false;
		}

		for (String selector : this.getFormat().getSelectors()) {
			if (!fileName.contains(selector)) {
				skip = true;
				break;
			}
		}
		if (skip) {
			return false;
		}

		names = fileName.split("_");
		if (names.length != this.getFormat().getUnderLineCnt()) {
			System.out.println("Skip this kdf since underline cnt is not " + this.getFormat().getUnderLineCnt());
			return false;
		}

		if (!this.setFileDate()) {
			System.out.println("Skip since bad Format of KDF date");
			return false;
		}

		this.lotNumber = names[format.getLotNumberIndex()];
////		this.unitId = names[format.getUnitIdIndex()];
		this.mfgStp = names[format.getMfgStepIndex()];

		// archive file
		this.archiveFile = new File(this.getFormat().getKdfArchivePath()
			+ "/" + this.fileDate
			+ "/" + lotNumber
			+ "/" + this.file.getName());

		// mapping file
		this.mappingFile = new File(this.getFormat().getMappingPath()
			+ "/" + this.fileDate
			+ "/" + lotNumber
			+ "/" + this.file.getName().split(".kdf")[0] + ".kdf");

		return true;
	}

	private
		boolean setFileDate() {
		String[] names = this.file.getName().split("_");
		this.fileMonth = names[format.getKdfMonthIndex()];
		if (this.fileMonth.length() == 4) {
			//0604
			this.fileMonth = "20" + fileMonth;
		}
		else if (this.fileMonth.length() == 6) {
			//180604
			this.fileMonth = "20" + fileMonth.substring(0, 4);
		}
		else if (this.fileMonth.length() == 8) {
			this.fileMonth = this.fileMonth.substring(0, 6);
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
		this.fileDate = fileOpenTime.substring(0, 8);
		return true;
	}

	private
		String formatTimeStr(String timeStr) {
		return timeStr.substring(0, 4) + "-" + timeStr.substring(4, 6) + "-" + timeStr.substring(6, 8)
			+ "T" + timeStr.substring(8, 10) + ":" + timeStr.substring(10, 12) + ":"
			+ timeStr.substring(12, 14) + ".00+08:00";
	}

	public
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
			if (this.mappingFile.createNewFile()) {
				return true;
			}
		}
		catch (IOException ex) {
			Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
		}

		this.failType = Config.FailureCase.IOError;
		this.logIoErrorToES("FailCreateMapFile");
		if (!this.removeTempLogFile()) {
			this.logIoErrorToES("FailDeleteLogFile");
		}

		return false;
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
	 * only one of the below 2 steps happens during production 1: always move
	 * source file to archive 2: or rename source file if needed
	 *
	 *
	 * @return
	 */
	private
		boolean renameAndArchiveSrcFile() {
		// only rename or archive the kdf in production mode
		if (this.getFormat().isProductionMode()) {
			if ((!Config.renameKDF) && (!this.moveFileToArchive())) {
				this.failType = Config.FailureCase.IOError;
				this.logIoErrorToES("FailArchiveKDF");

//				if (!this.removeTempLogFile()) {
//					this.logIoErrorToES("FailDeleteLogFile");
//				}
//
//				if (!this.removeMapFile()) {
//					this.logIoErrorToES("FailDeleteMapFile");
//				}
				return false;
			}

			if (!this.renameSourceFile()) {
				this.failType = Config.FailureCase.IOError;
				this.logIoErrorToES("FailRenameKDF");
//				if (!this.removeTempLogFile()) {
//					this.logIoErrorToES("FailDeleteLogFile");
//				}
//
//				if (!this.removeMapFile()) {
//					this.logIoErrorToES("FailDeleteMapFile");
//				}
				return false;
			}
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
				Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
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
				Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * move source file to archive
	 *
	 * @return
	 */
	private
		boolean moveFileToArchive() {
		try {
			if (this.archiveFile.exists()) {
				Files.delete(this.file.toPath());
			}
			else {
				if (!this.archiveFile.getParentFile().exists()) {
					if (this.archiveFile.getParentFile().mkdirs()) {
						Files.move(this.file.toPath(), this.archiveFile.toPath(), ATOMIC_MOVE);
					}
					else {
						this.logIoErrorToES("FailMkDIR");
						return false;
					}
				}
			}
		}
		catch (IOException ex) {
			System.out.println("EventType:ArchieveFailure");
			Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
			return false;
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

	public
		void logBadFormatFileToES() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public
		boolean isRepeatFile() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public
		void logRepeatFileToES() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public
		void logOpenFailureToES() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public
		void logIoErrorToES(String error) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public
		void logFileDoneToES() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
		File getArchiveFile() {
		return archiveFile;
	}

	public
		String getFileMonth() {
		return fileMonth;
	}

	public
		String getFileDate() {
		return fileDate;
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
		String getMfgStp() {
		return mfgStp;
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

	public abstract
		boolean writeLogFile();

}
