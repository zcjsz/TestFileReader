/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.smap;

/**
 *
 * @author guanghao
 */
public
    class Die {

    private
        int x = 0;
    private
        int y = 0;
    private
        String pickBin = null;

    public
        Die(int x, int y, String bin) {
        this.x = x;
        this.y = y;
        this.pickBin = bin;
    }

    public
        String getPickBin() {
        return pickBin;
    }

    public
        int getX() {
        return x;
    }

    public
        int getY() {
        return y;
    }

}
