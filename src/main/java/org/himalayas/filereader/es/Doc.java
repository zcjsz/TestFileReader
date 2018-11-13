/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.es;

/**
 *
 * @author ghfan
 */
public
    class Doc {

    private
        String index = null;

    private
        int binType = 0;
    private
        String startTime = null;

    private
        String endTime = null;

    private
        String id = null;
    private
        String waferNumber = null;
    private
        String motherLotInsertion = null;
    private
        double testTime = 0;

    /**
     *
     * @param id
     * @param binType
     * @param startTime
     */
    public
        Doc(String id, int binType, String startTime, String endTime, String index, double testTime) {
        this.id = id;
        this.binType = binType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.index = index;
        this.testTime = testTime;
    }

    public
        int getBinType() {
        return binType;
    }

    public
        void setBinType(int binType) {
        this.binType = binType;
    }

    public
        String getStartTime() {
        return startTime;
    }

    public
        void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public
        String getEndTime() {
        return endTime;
    }

    public
        void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public
        String getId() {
        return id;
    }

    public
        void setId(String id) {
        this.id = id;
    }

    public
        String getWaferNumber() {
        return waferNumber;
    }

    public
        void setWaferNumber(String waferNumber) {
        this.waferNumber = waferNumber;
    }

    public
        String getMotherLotInsertion() {
        return motherLotInsertion;
    }

    public
        void setMotherLotInsertion(String motherLotInsertion) {
        this.motherLotInsertion = motherLotInsertion;
    }

    public
        String getIndex() {
        return index;
    }

    public
        double getTestTime() {
        return testTime;
    }

}
