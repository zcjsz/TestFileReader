/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.util;

import org.himalayas.filereader.reader.kdf.*;

public
    class SlaveUnit {

    private
        String unitId = null;
    private
        String comHash = null;

    /**
     *
     * @param unitId
     * @param comHash
     */
    public
        SlaveUnit(String unitId, String comHash) {
        this.comHash = comHash;
        this.unitId = unitId;
    }

    public
        String getComHash() {
        return comHash;
    }

    public
        String getUnitId() {
        return unitId;
    }
}
