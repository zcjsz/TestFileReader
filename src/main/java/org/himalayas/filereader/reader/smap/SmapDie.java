/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.smap;

import org.himalayas.filereader.util.Die;

/**
 *
 * @author guanghao
 */
public
    class SmapDie extends Die {
    
    private
        String pickBin = null;

    public
        SmapDie(int x, int y, String bin) {
        super(x,y);
        this.pickBin = bin;
    }

    public
        String getPickBin() {
        return pickBin;
    }

}
