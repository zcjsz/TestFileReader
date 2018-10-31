/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader;

import java.io.File;
import java.util.Arrays;
import org.himalayas.filereader.kdf.KDFReader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;

/**
 *
 * @author ghfan
 */
public
	class FileReader {

	public
		FileReader(File configFile) {
		long startTime = System.currentTimeMillis();
		new Config(configFile.getAbsolutePath());
		KDFReader loader = new KDFReader();

		for (DataFormat format : Config.dataFormats.values()) {
			int fileCnt = 0;
			int fileLimit = 30;
			if (format.isEnabled() 
				&& (format.getDataType().equals(Config.DataTypes.WaferSort)
				|| format.getDataType().equals(Config.DataTypes.ATE)
				|| format.getDataType().equals(Config.DataTypes.SLT))) {
				if(format.getDataType().equals(Config.DataTypes.SLT)){
					fileLimit = 1000;
				}
				loader.setFormat(format);
				for(File dateFile: new File(format.getKdfPath()).listFiles()) {
					if(dateFile.isDirectory()
						&& (dateFile.getName().length() == 8 || dateFile.getName().length()==10)){
					
					}
					else{
						continue;
					}
				
					for (File kdfFile : dateFile.listFiles()) {
						if (kdfFile.isFile()) {
							String fileName = kdfFile.getName();
							if (fileName.endsWith(Config.KdfRename.badFormat.name())
								|| fileName.endsWith(Config.KdfRename.done.name())
								|| fileName.endsWith(Config.KdfRename.exception.name())
								|| fileName.endsWith(Config.KdfRename.openErr.name())
								|| fileName.endsWith(Config.KdfRename.skip.name())) {
								continue;

							}
							if(!kdfFile.canRead()) {
								System.out.println("Error: tdni has no permission to read this file");
								continue;
							}
							if(kdfFile.length() < 100){
								System.out.printf("Error: file size = %d error, less than 1k\n", kdfFile.length());
								continue;
							}
							try {
								if(loader.loadFile(kdfFile)){
									fileCnt ++;
								}
								if(fileCnt > fileLimit){
									System.out.println("kdf done file cnt is " + (fileLimit + 1));
									System.out.println("Have break now, bye");
									System.exit(0);
								}
							}
							catch (Exception e) {
								loader.logExceptionToES();
								loader.renameOrArchiveKDF(loader.getExceptionArchiveFile(), Config.KdfRename.exception);
								//TODO how to handler the exception  kdf file?
								System.out.println();
								e.printStackTrace();
							}
						}

					}
				}
			}
		}

//		File stageFile = new File("./testdata/KDF/SORT");
//		for (File file : stageFile.listFiles()) {
//			if (loader.chooseFormat(file)) {
//				loader.loadFile(file);
//			}
//		}
//		stageFile = new File("./testdata/KDF/SLT");
//		for (File file : stageFile.listFiles()) {
//			if (loader.chooseFormat(file)) {
//				loader.loadFile(file);
//			}
//		}
//		stageFile = new File("./testdata/KDF/FT");
//		for (File file : stageFile.listFiles()) {
//			if (loader.chooseFormat(file)) {
//				loader.loadFile(file);
//			}
//		}
		//System.out.println(loader.allFields.toString());
		System.out.println("total time = " + (System.currentTimeMillis() - startTime));

	}

	public static
		void main(String[] args) {
		boolean debug = true;
		if (args.length == 0) {
			System.out.println("please set the config file path");

			if (debug) {
				File configFile = new File("config/dataformat.xml");
				new FileReader(configFile);
			}
			else {
				System.exit(1);
			}
		}
		else{
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
			System.out.println("please set the config file path");
			System.exit(1);
		}

	}

}
