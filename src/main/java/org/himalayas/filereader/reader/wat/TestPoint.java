/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import java.util.HashMap;

/**
 *
 * @author wsilen
 */
public class TestPoint {

    private int x = 0;
    private int y = 0;
    private int seq = 0;
    private HashMap<Integer, ParameterValue> paramVals;

    public TestPoint(int x, int y, int seq) {
        this.x = x;
        this.y = y;
        this.seq = seq;
        this.paramVals = new HashMap<>();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSeq() {
        return seq;
    }

    public void setParamVals(int paramId, ParameterValue paramVal) {
        if (this.paramVals.size() == 0) {
            this.paramVals.put(paramId, paramVal);
        } else {
            this.paramVals.putIfAbsent(paramId, paramVal);
        }

    }
    
    public HashMap<Integer, ParameterValue> getParamVals() {
        return paramVals;
    }

}
