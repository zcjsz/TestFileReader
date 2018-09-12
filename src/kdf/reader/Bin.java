/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kdf.reader;

/**
 *
 * @author ghfan
 */
public
	class Bin {

	private
		String binNumber = null;
	private
		String binDescription = null;
	private
		String flag = null;

	/**
	 *
	 * @param binNumber
	 * @param binDesc
	 * @param flag
	 */
	public
		Bin(String binNumber, String binDesc, String flag) {
		this.binNumber = binNumber;
		this.binDescription = binDesc.replace(',', '-').replace('=', '-');
		if (flag.equals("70")) {
			this.flag = "F";
		}
		else if (flag.equals("80")) {
			this.flag = "P";
		}
		else if (flag.toLowerCase().equals("p")) {
			this.flag = "P";
		}
		else if (flag.toLowerCase().equals("f")) {
			this.flag = "F";
		}
		else {
			this.flag = "F";
		}
	}

	public
		String getBinDescription() {
		return binDescription;
	}

	public
		String getFlag() {
		return flag;
	}

	public static
		void main(String[] args) {
		char P = 80;
		System.out.println(P);
	}

}
