/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.Die;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
public
    class WatDie extends Die {

    private
        String dieType = null;

    public
        WatDie(int x, int y, String dieType) {
        super(x,y);
        this.dieType = dieType;
    }

    public
        String getDocValue() {
        if (this.dieType.equals(FieldType.MasterDie)) {
            return "," + FieldType.X0 + "=" + this.getX()
                + "," + FieldType.Y0 + "=" + this.getY()
                + "," + FieldType.DieType + "=" + this.dieType;
        }
        else {
            return "," + Config.watFormat.getUnit().getxCoordNode().getName() + "=" + this.getX()
                + "," + Config.watFormat.getUnit().getyCoordNode().getName() + "=" + this.getY()
                + "," + FieldType.DieType + "=" + this.dieType;
        }
    }

}
