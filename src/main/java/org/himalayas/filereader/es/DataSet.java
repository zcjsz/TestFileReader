/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.es;

import java.util.ArrayList;
import java.util.Comparator;

/**
 *
 * @author ghfan
 */
public
    class DataSet {

    private
        String unitId = null;
    private
        boolean masterDie = false;
    private
        ArrayList<Doc> unitData = new ArrayList();

    public
        DataSet(String unitID) {
        this.unitId = unitID;
    }

    public
        ArrayList<Doc> getUnitData() {
        return unitData;
    }

    public
        String getUnitId() {
        return unitId;
    }

//	public
//		void sortUnitDataByStartTestTime() {
//		unitData.sort(Comparator.comparing(Doc::getStartTime));
//		this.size = this.unitData.size();
//	}
    public
        void sortMotherLotInserction() {
        unitData.sort(Comparator.comparing(Doc::getStartTime));
        int i = 0;
        if (unitData.size() == 1) {
            this.unitData.get(0).setMotherLotInsertion("FL");
        }
        else {
            for (Doc unit : this.unitData) {
                i++;
                if (i != this.unitData.size()) {
                    unit.setMotherLotInsertion(String.valueOf(i));
                }
                else {
                    unit.setMotherLotInsertion("L");
                }

            }
        }
    }

    public
        Doc getFirstUnit() {
        return this.unitData.get(0);
    }

    public
        Doc getLastUnit() {
        return this.unitData.get(this.unitData.size() - 1);
    }

    public
        boolean isMasterDie() {
        return masterDie;
    }

    public
        void setMasterDie(boolean masterDie) {
        this.masterDie = masterDie;
    }

}
