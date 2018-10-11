/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reader.wat;

import java.io.File;
import reader.util.Config;

/**
 *
 * @author ghfan
 */
public
	class WatReader {

	public
		WatReader() {
	}

	public
		boolean loadFile(File file) {
		return false;
	}

	public static
		void main(String[] args) {
		WatReader reader = new WatReader();
		File watPath = new File(Config.watFormat.getKdfPath());
		for (File lotFile : watPath.listFiles()) {
			reader.loadFile(lotFile);
		}

	}

}
