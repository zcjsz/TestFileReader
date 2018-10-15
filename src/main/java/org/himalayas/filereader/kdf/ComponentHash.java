/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.kdf;

public
	class ComponentHash {

	private
		String comClass = null;
	private
		String comName = null;

	/**
	 *
	 * @param comClass
	 * @param comName
	 */
	public
		ComponentHash(String comClass, String comName) {
		this.comClass = comClass.replace(',', '_').replace('=', '_');
		this.comName = comName.replace(',', '_').replace('=', '_');
	}

	public
		String getComClass() {
		return comClass;
	}

	public
		String getComName() {
		return comName;
	}

	public
		String getKVString() {
		return ",comName=" + this.getComName() + ",comClass=" + this.getComClass();
	}

}
