/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.es;

import java.util.HashMap;

/**
 *
 * @author ghfan
 */
public
	class LotInfo {

	private
		HashMap<String, DataSet> dataSets = new HashMap();
	private
		double totalTestTime = 0;
	private
		double avgTestTime = 0;
	private
		double kdfFirstYield = 0;
	private
		double kdfFinalYield = 0;
	private
		double kdfFtrd = 0;
	private
		double kdfIr = 0;
	private
		double grossTestTime = 0;
	private
		double totalIndexTime = 0;
	private
		int uph = 0;
	private
		double avgIndexTime = 0;

	public
		LotInfo() {
	}

	public
		HashMap<String, DataSet> getDataSets() {
		return dataSets;
	}

	public
		double getTotalTestTime() {
		return totalTestTime;
	}

	public
		void setTotalTestTime(double totalTestTime) {
		this.totalTestTime = totalTestTime;
	}

	public
		double getAvgTestTime() {
		return avgTestTime;
	}

	public
		void setAvgTestTime(double avgTestTime) {
		this.avgTestTime = avgTestTime;
	}

	public
		double getKdfFirstYield() {
		return kdfFirstYield;
	}

	public
		void setKdfFirstYield(double kdfFirstYield) {
		this.kdfFirstYield = kdfFirstYield;
	}

	public
		double getKdfFinalYield() {
		return kdfFinalYield;
	}

	public
		void setKdfFinalYield(double kdfFinalYield) {
		this.kdfFinalYield = kdfFinalYield;
	}

	public
		double getKdfFtrd() {
		return kdfFtrd;
	}

	public
		void setKdfFtrd(double kdfFtrd) {
		this.kdfFtrd = kdfFtrd;
	}

	public
		double getKdfIr() {
		return kdfIr;
	}

	public
		void setKdfIr(double kdfIr) {
		this.kdfIr = kdfIr;
	}

	public
		double getGrossTestTime() {
		return grossTestTime;
	}

	public
		void setGrossTestTime(double grossTestTime) {
		this.grossTestTime = grossTestTime;
	}

	public
		double getTotalIndexTime() {
		return totalIndexTime;
	}

	public
		void setTotalIndexTime(double totalIndexTime) {
		this.totalIndexTime = totalIndexTime;
	}

	public
		int getUph() {
		return uph;
	}

	public
		void setUph(int uph) {
		this.uph = uph;
	}

	public
		double getAvgIndexTime() {
		return avgIndexTime;
	}

	public
		void setAvgIndexTime(double avgIndexTime) {
		this.avgIndexTime = avgIndexTime;
	}

	public
		void calInsertion() {
		for (DataSet dataSet : this.getDataSets().values()) {
			dataSet.sortMotherLotInserction();
		}
	}

}
