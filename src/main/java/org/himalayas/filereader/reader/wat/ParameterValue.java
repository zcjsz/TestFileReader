/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

/**
 *
 * @author wsilen
 */
public class ParameterValue {

    private int paramId = 0;
    private ParameterDesc paramDesc;
    private double paramVal = 0.0;

    public ParameterValue(int paramId, ParameterDesc paramDesc, double paramVal) {
        this.paramId = paramId;
        this.paramDesc = paramDesc;
        this.paramVal = paramVal;
    }

    public int getParamId() {
        return paramId;
    }

    public ParameterDesc getParamDesc() {
        return paramDesc;
    }

    public double getParamVal() {
        return paramVal;
    }

}
