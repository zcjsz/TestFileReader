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
	class TestDesc {
	private
		String baseClass = null;
	private
		String subClass = null;
	private
		String value = null;

	/**
	 * 
	 * @param base
	 * @param sub
	 * @param value 
	 */
	public
	TestDesc(String base, String sub, String value) {
		this.subClass = sub;
		this.baseClass = base;
		this.value = value;
	}

	public
	String getBaseClass() {
		return baseClass;
	}

	public
	String getSubClass() {
		return subClass;
	}

	public
	String getValue() {
		return value;
	}
	
}
