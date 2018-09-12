/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kdf.reader;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ghfan
 */
public
	class KDFReader {

	private
		Loader loader = null;

	public
		KDFReader() {
	}

	public
		void startJob() {
		System.out.println("Start Task on " + LocalDateTime.now().toString());
		loader = new Loader();
		for (DataFormat dataFormat : Config.dataFormats.values()) {
			loader.setFormat(dataFormat);
			// start job 
			String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("uuuuMMdd"));
			String today = LocalDate.now().format(DateTimeFormatter.ofPattern("uuuuMMdd"));

			for (File dateFile : new File(dataFormat.getKdfPath()).listFiles()) {
				if (dateFile.getName().equals(yesterday) || dateFile.getName().equals(today)) {
					long startTime = System.currentTimeMillis();

					int proceedKdfCnt = 0;
					int skipKdfCnt = 0;
					int errorKdfCnt = 0;

					for (File kdfFile : dateFile.listFiles()) {
						try {
							if (!loader.loadFile(kdfFile)) {
								if (loader.getFailType().equals(Config.FailureCase.OpenFailure)) {
									writeOpenFile(kdfFile);
									errorKdfCnt++;
								}
								else if (loader.getFailType().equals(Config.FailureCase.Exception)) {
									writeExceptionFile(kdfFile);
									errorKdfCnt++;
								}
								else {
									skipKdfCnt++;
									if (kdfFile.getName().endsWith("")) {
										this.writeSkippedFile(kdfFile);
									}
								}
							}
							else {
								proceedKdfCnt++;
							}
						}
						catch (Exception e) {
							writeExceptionFile(kdfFile);
							loader.setTree(null);
							errorKdfCnt++;
							e.printStackTrace();

						}
					}
					System.out.printf("procees kdf cnt = %s, error kdf cnt = %s, skip kdf cnt = %s\n", proceedKdfCnt, errorKdfCnt, skipKdfCnt);
				}
			}
		}

	}

	private
		void writeOpenFile(File kdfFile) {
		File errorKdf = new File(kdfFile.getAbsolutePath() + "." + Config.KdfRename.openErr);
		System.out.println("Error, failed to open this kdf file");
		if (kdfFile.renameTo(errorKdf)) {
			System.out.println("Successed to renamed to " + errorKdf.getName());
		}
		else {
			System.out.println("Failed to renamed to " + errorKdf.getName());
		}
	}

	private
		void writeExceptionFile(File kdfFile) {
		File exceptionKdf = new File(kdfFile.getAbsolutePath() + "." + Config.KdfRename.exception);
		if (kdfFile.renameTo(exceptionKdf)) {
			System.out.println("Successed to renamed to " + exceptionKdf.getName());
		}
		else {
			System.out.println("Failed to renamed to " + exceptionKdf.getName());
		}
	}

	private
		void writeSkippedFile(File kdfFile) {
		String kdfName = kdfFile.getName();
		if (kdfName.endsWith(Config.KdfRename.done.toString())
			|| kdfName.endsWith(Config.KdfRename.exception.toString())
			|| kdfName.endsWith(Config.KdfRename.openErr.toString())
			|| kdfName.endsWith(Config.KdfRename.skip.toString())) {
			return;
		}
		File skipKdf = new File(kdfFile.getAbsolutePath() + "." + Config.KdfRename.skip);
		if (kdfFile.renameTo(skipKdf)) {
			System.out.println("Successed to renamed to " + skipKdf.getName());
		}
		else {
			System.out.println("Failed to renamed to " + skipKdf.getName());
		}
	}

	/**
	 * @param args the command line arguments
	 */
	public static
		void main(String[] args) {
		boolean debugMode = false;
		if (args.length != 0 || debugMode) {
			String configFile = null;
			if (args.length == 1 && args[0].contains("help")) {
				System.out.println("Kdf Reader is to read kdf data, command line\n"
					+ "java -jar KDFReader.jar your-config-file\n"
					+ "it will read all kdf in your kdf path");
				System.exit(0);
			}
			else if (args.length == 1 || debugMode) {
				if (args.length == 1) {
					configFile = args[0];
				}
				else {
					configFile = "./config/dataformat.xml";
				}

				if (configFile.endsWith(".xml")
					&& new File(configFile).exists()) {
					new Config(configFile);
					File lockFile = new File(Config.lockFilePath + "/lock.txt");
					if ((!debugMode) && lockFile.exists()) {
						System.out.println("There's already a live instance running!");
						System.exit(0);
					}
					else {
						try {
							lockFile.deleteOnExit();
							if ((!debugMode) && (!lockFile.createNewFile())) {
								System.err.println("failed to create lock file");
								System.exit(0);
							}
							new KDFReader().startJob();
						}
						catch (IOException ex) {
							Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
							System.exit(0);
						}
					}

				}
				else {

					System.out.println("Kdf Reader is to read kdf data, command line\n"
						+ "java -jar KDFReader.jar your-config-file\n"
						+ "it will read all kdf in your kdf path");
					System.exit(0);

				}
			}
			else {
				System.out.println("Kdf Reader is to read kdf data, command line\n"
					+ "java -jar KDFReader.jar your-config-file \n"
					+ "it will read all kdf in your kdf path");
				System.exit(0);
			}
		}
	}

}
