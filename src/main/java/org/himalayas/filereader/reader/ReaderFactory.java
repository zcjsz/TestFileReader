/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader;

import org.himalayas.filereader.reader.camstar.CamstarReader;
import org.himalayas.filereader.reader.smap.SmapReader;
import org.himalayas.filereader.reader.wat.WatReader;
import org.himalayas.filereader.util.DataFormat;

/**
 *
 * @author guanghao
 */
public class ReaderFactory {

    public static Reader creatReader(DataFormat dataFormat) {
        if (dataFormat != null && dataFormat.isEnabled()) {
            switch (dataFormat.getDataType()) {
                case WAT:
                    return WatReader.getInstance();
                case SMAP:
                    return SmapReader.getInstance();
                case CAMSTAR:
                    return CamstarReader.getInstance();
                default:
                    return null;
            }
        } else {
            return null;
        }
    }
}
