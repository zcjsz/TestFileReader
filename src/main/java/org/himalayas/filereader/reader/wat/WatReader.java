/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import java.io.File;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;

/**
 *
 * @author ghfan
 */
final public
    class WatReader extends Reader {

    public
        WatReader(DataFormat format) {
        super(format);
    }

    @Override
    public
        boolean readFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public
        void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public
        boolean writeLogFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static
        void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new Config("config/dataformat.xml");
        Reader reader = new WatReader(Config.smapFormat);
        Config.smapFormat.setProductionMode(false);

        File testDataFile = new File("./testdata/extend/hygon_source_data/SMAP");
        for (File lotFile : testDataFile.listFiles()) {
            for (File file : lotFile.listFiles()) {
                reader.loadFile(file);
            }
        }

        System.out.println("total time = " + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

    }
}
