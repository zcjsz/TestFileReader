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
		double totalTouchDownTestTime = 0;
	private
		double totalUnitTestTime = 0;
	private
		double avgTouchDownTestTime = 0;
	private
		double avgUnitTestTime = 0;
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
	private
		int totalTestedUnitCnt = 0;
	private
		int totalUniqueUnitCnt = 0;
	private
		int totalTouchDownCnt = 0;
	private
		long totalFileCnt = 0;

	public
		LotInfo() {
	}

	public
		HashMap<String, DataSet> getDataSets() {
		return dataSets;
	}

	public
	double getTotalTouchDownTestTime() {
		return totalTouchDownTestTime;
	}

	public
	void setTotalTouchDownTestTime(double totalTouchDownTestTime) {
		this.totalTouchDownTestTime = totalTouchDownTestTime;
	}
	
	public
	void totalTouchDownTestTimeInc(double totalTouchDownTestTime) {
		this.totalTouchDownTestTime += totalTouchDownTestTime;
	}

	public
	double getTotalUnitTestTime() {
		return totalUnitTestTime;
	}

	public
	void setTotalUnitTestTime(double totalUnitTestTime) {
		this.totalUnitTestTime = totalUnitTestTime;
	}
	public
	void totalUnitTestTimeInc(double totalUnitTestTime) {
		this.totalUnitTestTime += totalUnitTestTime;
	}
	

	public
	double getAvgTouchDownTestTime() {
		return avgTouchDownTestTime;
	}

	public
	void setAvgTouchDownTestTime(double avgTouchDownTestTime) {
		this.avgTouchDownTestTime = avgTouchDownTestTime;
	}

	public
	double getAvgUnitTestTime() {
		return avgUnitTestTime;
	}

	public
	void setAvgUnitTestTime(double avgUnitTestTime) {
		this.avgUnitTestTime = avgUnitTestTime;
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

	public
	void totalTestedUnitCntInc(int totalTestedUnitCnt) {
		this.totalTestedUnitCnt += totalTestedUnitCnt;
	}

	public
	void totalUniqueUnitCntInc(int totalUniqueUnitCnt) {
		this.totalUniqueUnitCnt += totalUniqueUnitCnt;
	}

	public
	void setTotalTestedUnitCnt(int totalTestedUnitCnt) {
		this.totalTestedUnitCnt = totalTestedUnitCnt;
	}

	public
	void setTotalFileCnt(long totalFileCnt) {
		this.totalFileCnt = totalFileCnt;
	}
	
	
	
	

}
