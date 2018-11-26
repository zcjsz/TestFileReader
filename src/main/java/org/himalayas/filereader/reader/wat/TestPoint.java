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

    private int x0 = 0;
    private int y0 = 0;
    private int seq = 0;
    private HashMap<Integer, ParameterValue> paramVals;

    public TestPoint(int x, int y, int seq) {
        this.x0 = x;
        this.y0 = y;
        this.seq = seq;
        this.paramVals = new HashMap<>();
    }

    public TestPoint(TestPoint tp) {
        this.x0 = tp.getX0();
        this.y0 = tp.getY0();
        this.seq = tp.getSeq();
        this.paramVals = tp.getParamVals();
    }

    public int getX0() {
        return x0;
    }

    public int getY0() {
        return y0;
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
