/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reader.smap;

import java.io.File;
import reader.util.Config;

/**
 *
 * @author ghfan
 */
public
	class SmapReader {

	public
		SmapReader() {
	}

	public
		boolean loadFile(File file) {
		return false;
	}

	public static
		void main(String[] args) {
		SmapReader reader = new SmapReader();
		File smapPath = new File(Config.smapFormat.getKdfPath());
		for (File lotFile : smapPath.listFiles()) {
			reader.loadFile(lotFile);
		}

	}

}
