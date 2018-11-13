/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
public
	class LotInfo {
	
        private static Pattern pat1 = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2})\\.\\d{2}(\\+\\d{2}:\\d{2})");
    
	private
		String doc_Id = null;

	private
		String lotNumberName = null;
	private
		String operationName = null;

	private
		String lotNumber = null;
	private
		String operation = null;

	private
		boolean waferSort = false;

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
		int uphGood = 0;
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
	private
		double totalPassTestTime = 0;
	private
		double totalFailTestTime = 0;
	private
		float avgPassTestTime = 0;
	private
		float avgFailTestTime = 0;
	private
		int firstPassCnt = 0;
	private
		int firstFailCnt = 0;
	private
		int lastFailCnt = 0;
	private
		int lastPassCnt = 0;
	private
		int totalFailCnt = 0;
	private
		int totalPassCnt = 0;
        
        private String lotStartTime = "1970-01-01 00:00:00.00 +0800";
        private String lotEndTime   = "2099-12-31 23:59:59.00 +0800";
        private Set<String> lotStartTimeSet = new TreeSet<>();
        private Set<String> lotEndTimeSet = new TreeSet<>();
        

	public
		LotInfo() {
	}

	public
		void reset() {
		this.lotNumber = null;
		this.lotNumberName = null;
		this.operation = null;
		this.operationName = null;
		
		this.uph = 0;
		this.uphGood = 0;
		this.waferSort = false;
		this.avgFailTestTime = 0;
		this.avgIndexTime = 0;
		this.avgPassTestTime = 0;
		this.avgTouchDownTestTime = 0;
		this.avgUnitTestTime = 0;
		this.dataSets.clear();
		this.firstFailCnt = 0;
		this.firstPassCnt = 0;
		this.grossTestTime = 0;
		this.kdfFinalYield = 0;
		this.kdfFirstYield = 0;
		this.kdfFtrd = 0;
		this.kdfIr = 0;
		this.lastFailCnt = 0;
		this.lastPassCnt = 0;
		this.totalFailCnt = 0;
		this.totalFailTestTime = 0;
		this.totalFileCnt = 0;
		this.totalIndexTime = 0;
		this.totalPassCnt = 0;
		this.totalPassTestTime = 0;
		this.totalTestedUnitCnt = 0;
		this.totalTouchDownCnt = 0;
		this.totalTouchDownTestTime = 0;
		this.totalUniqueUnitCnt = 0;
		this.totalUnitTestTime = 0;
                
                this.lotStartTime = null;
                this.lotEndTime = null;
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

        public String getLotStartTime() {
            return lotStartTime;
        }

        public void setLotStartTime(String lotStartTime) {
            this.lotStartTime = lotStartTime;
        }

        public String getLotEndTime() {
            return lotEndTime;
        }

        public void setLotEndTime(String lotEndTime) {
            this.lotEndTime = lotEndTime;
        }
                
                
        

	public
		void calInsertion() {
		for (DataSet dataSet : this.getDataSets().values()) {
			dataSet.sortMotherLotInserction();

			if ((!this.waferSort) && (!dataSet.isMasterDie())) {
				continue;
			}

			// first and last unit cnt
			Doc firstUnit = dataSet.getFirstUnit();
			Doc lastUnit = dataSet.getLastUnit();

			if (firstUnit.getBinType() == 1) {
				this.firstPassCnt += 1;
			}
			else {
				this.firstFailCnt += 1;
			}

			if (lastUnit.getBinType() == 1) {
				this.lastPassCnt += 1;
			}
			else {
				this.lastFailCnt += 1;
			}

			// unique unit cnt
			this.totalUniqueUnitCntInc(1);

			// 
			for (Doc doc : dataSet.getUnitData()) {
                            if (doc.getBinType() == 1) {
                                    this.totalPassCnt += 1;
                                    this.totalPassTestTime += doc.getTestTime();
                            }
                            else {
                                    this.totalFailCnt += 1;
                                    this.totalFailTestTime += doc.getTestTime();
                            }
                            this.totalUnitTestTimeInc(doc.getTestTime());
                            this.totalTestedUnitCntInc(1);
                            
                            lotStartTimeSet.add(doc.getStartTime());
                            lotEndTimeSet.add(doc.getEndTime());
                            
			}
		}
	}


        public void calStartEndTime() {
            
            List<String> tmpStartTimeList = new ArrayList<>(lotStartTimeSet);
            List<String> tmpEndTimeList = new ArrayList<>(lotEndTimeSet);
            
            for(int i=0; i<tmpStartTimeList.size(); i++) {
                String tmpStartTime = tmpStartTimeList.get(i);
                Matcher matcher = pat1.matcher(tmpStartTime);
                if(matcher.find()){
                    lotStartTime = matcher.group(0);
                    break;
                } else {
                    continue;
                }
            }
            
            for(int j=tmpEndTimeList.size()-1; j>=0; j--) {
                String tmpEndTime = tmpEndTimeList.get(j);
                Matcher matcher = pat1.matcher(tmpEndTime);
                if(matcher.find()){
                    lotEndTime = matcher.group(0);
                    break;
                } else {
                    continue;
                }
            }
            
        }
                
                
	public void calKPI() {

            if (this.totalTestedUnitCnt != 0) {
                this.uph = (int) (this.totalTestedUnitCnt * 3600 / this.grossTestTime);
                this.uphGood = (int) (this.totalPassCnt * 3600 / this.grossTestTime);

                this.avgUnitTestTime = this.totalUnitTestTime / this.totalTestedUnitCnt;

                this.kdfFirstYield = ((double) this.firstPassCnt) / this.totalUniqueUnitCnt;
                this.kdfFinalYield = ((double) this.lastPassCnt) / this.totalUniqueUnitCnt;
                this.kdfFtrd = this.kdfFinalYield - this.kdfFirstYield;

                this.kdfIr = ((double) this.totalTestedUnitCnt) / this.totalUniqueUnitCnt;

            }
            if (this.totalPassCnt != 0) {
                    this.avgPassTestTime = (float) (this.totalPassTestTime / this.totalPassCnt);
            }

            if (this.totalFailCnt != 0) {
                    this.avgFailTestTime = (float) (this.totalFailTestTime / this.totalFailCnt);
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

	public
		int getTotalUniqueUnitCnt() {
		return totalUniqueUnitCnt;
	}

	public
		void setTotalUniqueUnitCnt(int totalUniqueUnitCnt) {
		this.totalUniqueUnitCnt = totalUniqueUnitCnt;
	}

	public
		int getTotalTouchDownCnt() {
		return totalTouchDownCnt;
	}

	public
		void setTotalTouchDownCnt(int totalTouchDownCnt) {
		this.totalTouchDownCnt = totalTouchDownCnt;
	}

	public
		double getTotalPassTestTime() {
		return totalPassTestTime;
	}

	public
		void setTotalPassTestTime(double totalPassTestTime) {
		this.totalPassTestTime = totalPassTestTime;
	}

	public
		double getTotalFailTestTime() {
		return totalFailTestTime;
	}

	public
		void setTotalFailTestTime(double totalFailTestTime) {
		this.totalFailTestTime = totalFailTestTime;
	}

	public
		float getAvgPassTestTime() {
		return avgPassTestTime;
	}

	public
		void setAvgPassTestTime(float avgPassTestTime) {
		this.avgPassTestTime = avgPassTestTime;
	}

	public
		float getAvgFailTestTime() {
		return avgFailTestTime;
	}

	public
		void setAvgFailTestTime(float avgFailTestTime) {
		this.avgFailTestTime = avgFailTestTime;
	}

	public
		int getFirstPassCnt() {
		return firstPassCnt;
	}

	public
		void setFirstPassCnt(int firstPassCnt) {
		this.firstPassCnt = firstPassCnt;
	}

	public
		int getFirstFailCnt() {
		return firstFailCnt;
	}

	public
		void setFirstFailCnt(int firstFailCnt) {
		this.firstFailCnt = firstFailCnt;
	}

	public
		int getLastFailCnt() {
		return lastFailCnt;
	}

	public
		void setLastFailCnt(int lastFailCnt) {
		this.lastFailCnt = lastFailCnt;
	}

	public
		int getLastPassCnt() {
		return lastPassCnt;
	}

	public
		void setLastPassCnt(int lastPassCnt) {
		this.lastPassCnt = lastPassCnt;
	}

	public
		void setWaferSort(boolean waferSort) {
		this.waferSort = waferSort;
	}

	@Override
	public
		String toString() {
		return FieldType.Type + "=" + FieldType.Lot_KDFLot + ","
			+ this.lotNumberName + "=" + this.lotNumber + ","
			+ this.operationName + "=" + this.operation + ","
			+ FieldType.Lot_Doc_id + "=" + this.doc_Id + ","
			+ FieldType.Lot_kdfAvgFailTestTime + "=" + this.valueOf2f(this.avgFailTestTime) + ","
			+ FieldType.Lot_kdfAvgPassTestTime + "=" + this.valueOf2f(this.avgPassTestTime) + ","
			+ FieldType.Lot_kdfAvgTestTime + "=" + this.valueOf2f(this.avgUnitTestTime) + ","
			+ FieldType.Lot_FileCnt + "=" + this.totalFileCnt + ","
			+ FieldType.Lot_kdfFirstFailCount + "=" + this.firstFailCnt + ","
			+ FieldType.Lot_kdfFirstPassCount + "=" + this.firstPassCnt + ","
			+ FieldType.Lot_kdfGrossTestTime + "=" + this.valueOf2f(this.grossTestTime) + ","
			+ FieldType.Lot_kdfFinalYield + "=" + this.valueOf4f(this.kdfFinalYield) + ","
			+ FieldType.Lot_kdfFirstYield + "=" + this.valueOf4f(this.kdfFirstYield) + ","
			+ FieldType.Lot_kdfFTRd + "=" + this.valueOf4f(this.kdfFtrd) + ","
			+ FieldType.Lot_kdfIR + "=" + this.valueOf4f(this.kdfIr) + ","
			+ FieldType.Lot_kdfLastFailCount + "=" + this.lastFailCnt + ","
			+ FieldType.Lot_kdfLastPassCount + "=" + this.lastPassCnt + ","
			+ FieldType.Lot_kdfTotalFailCount + "=" + this.totalFailCnt + ","
			+ FieldType.Lot_kdfTotalFailTestTime + "=" + this.valueOf2f(this.totalFailTestTime) + ","
			+ FieldType.Lot_kdfTotalPassCount + "=" + this.totalPassCnt + ","
			+ FieldType.Lot_kdfTotalPassTestTime + "=" + this.valueOf2f(this.totalPassTestTime) + ","
			+ FieldType.Lot_kdfTotalTestedCount + "=" + this.totalTestedUnitCnt + ","
			+ FieldType.Lot_kdfQtyIn + "=" + this.totalUniqueUnitCnt + ","
			+ FieldType.Lot_kdfTotalTestTime + "=" + this.valueOf2f(this.totalUnitTestTime) + ","
			+ FieldType.Lot_kdfUPH + "=" + this.uph + ","
			+ FieldType.Lot_kdfUPHGood + "=" + this.uphGood + ","
                        + FieldType.Lot_LotStartTime + "=" + this.lotStartTime + ","
                        + FieldType.Lot_LotEndTime + "=" + this.lotEndTime;
	}

	public
		Map<String, Object> getJsonMap() {
		Map<String, Object> jsonMap = new HashMap<>();
		jsonMap.put(FieldType.Type, FieldType.Lot_KDFLot);
		jsonMap.put(this.lotNumberName, this.lotNumber);
		jsonMap.put(this.operationName, this.operation);
//		jsonMap.put(FieldType.Lot_Doc_id, this.doc_Id);
		jsonMap.put(FieldType.Lot_kdfAvgFailTestTime, this.valueOf2f(this.avgFailTestTime));
		jsonMap.put(FieldType.Lot_kdfAvgPassTestTime, this.valueOf2f(this.avgPassTestTime));
		jsonMap.put(FieldType.Lot_kdfAvgTestTime, this.valueOf2f(this.avgUnitTestTime));
		jsonMap.put(FieldType.Lot_FileCnt, this.totalFileCnt);
		jsonMap.put(FieldType.Lot_kdfFirstFailCount, this.firstFailCnt);
		jsonMap.put(FieldType.Lot_kdfFirstPassCount, this.firstPassCnt);
		jsonMap.put(FieldType.Lot_kdfGrossTestTime, this.valueOf2f(this.grossTestTime));
		jsonMap.put(FieldType.Lot_kdfFinalYield, this.valueOf4f(this.kdfFinalYield));
		jsonMap.put(FieldType.Lot_kdfFirstYield, this.valueOf4f(this.kdfFirstYield));
		jsonMap.put(FieldType.Lot_kdfFTRd, this.valueOf4f(this.kdfFtrd));
		jsonMap.put(FieldType.Lot_kdfIR, this.valueOf4f(this.kdfIr));
		jsonMap.put(FieldType.Lot_kdfLastFailCount, this.lastFailCnt);
		jsonMap.put(FieldType.Lot_kdfLastPassCount, this.lastPassCnt);
		jsonMap.put(FieldType.Lot_kdfTotalFailCount, this.totalFailCnt);
		jsonMap.put(FieldType.Lot_kdfTotalFailTestTime, this.valueOf2f(this.totalFailTestTime));
		jsonMap.put(FieldType.Lot_kdfTotalPassCount, this.totalPassCnt);
		jsonMap.put(FieldType.Lot_kdfTotalPassTestTime, this.valueOf2f(this.totalPassTestTime));
		jsonMap.put(FieldType.Lot_kdfTotalTestedCount, this.totalTestedUnitCnt);
		jsonMap.put(FieldType.Lot_kdfQtyIn, this.totalUniqueUnitCnt);
		jsonMap.put(FieldType.Lot_kdfTotalTestTime, this.valueOf2f(this.totalUnitTestTime));
		jsonMap.put(FieldType.Lot_kdfUPH, this.uph);
		jsonMap.put(FieldType.Lot_kdfUPHGood, this.uphGood);
                jsonMap.put(FieldType.Lot_LotStartTime, this.lotStartTime);
                jsonMap.put(FieldType.Lot_LotEndTime, this.lotEndTime);
		return jsonMap;

	}

	public
		void setOperation(String operation) {
		this.operation = operation;
	}

	public
		void setLotNumber(String lotNumber) {
		this.lotNumber = lotNumber;
	}

	private
		String valueOf4f(float in) {
		return String.format("%.4f", in);
	}

	private
		String valueOf4f(double in) {
		return String.format("%.4f", in);
	}

	private
		String valueOf2f(float in) {
		return String.format("%.2f", in);
	}

	private
		String valueOf2f(double in) {
		return String.format("%.2f", in);
	}

	public
		void setLotNumberName(String lotNumberName) {
		this.lotNumberName = lotNumberName;
	}

	public
		void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public
	void setDoc_Id(String doc_Id) {
		this.doc_Id = doc_Id;
	}

	public
	String getDoc_Id() {
		return doc_Id;
	}

	public
	String getLotNumber() {
		return lotNumber;
	}

	public
	String getOperation() {
		return operation;
	}

	
}
