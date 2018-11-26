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
public class Die
        extends TestPoint {

    private int x = 0;
    private int y = 0;

    public Die(int x, int y, TestPoint tp) {
        super(tp);
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
