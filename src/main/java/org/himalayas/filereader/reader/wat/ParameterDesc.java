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
public class ParameterDesc {

    private int paramId = 0;
    private String paramName = null;
    private String group = null;
    private String LSL = null;
    private String USL = null;
    private String unit = null;

    public ParameterDesc(int paramId, String paramName, String group, String LSL, String USL, String unit) {
        this.paramId = paramId;
        this.paramName = paramName;
        this.group = group;
        this.LSL = LSL;
        this.USL = USL;
        this.unit = unit;
    }

    public int getParamId() {
        return paramId;
    }

    public String getParamName() {
        return paramName;
    }

    public String getGroup() {
        return group;
    }

    public String getLSL() {
        return LSL;
    }

    public String getUSL() {
        return USL;
    }

    public String getUnit() {
        return unit;
    }

}
