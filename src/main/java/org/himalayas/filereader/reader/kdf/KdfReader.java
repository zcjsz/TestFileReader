/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.kdf;

import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.DataFormat;

/**
 *
 * @author guanghao
 */
final public 
    class KdfReader extends Reader{
    
    private static
        KdfReader instance = null;
    private
        KdfParser kdfParser = null;
    

    
    private
        KdfReader(DataFormat format) {
        super(format);
    }
        
    public static
        KdfReader getInstance(DataFormat dataFormat) {
        if((dataFormat == null) || (!dataFormat.isKdfData())){
            System.out.println("Fatal Error: KdfReader only should be initilized with KDF Data Type DataFormat");
            return null;
        }
        if (instance == null && dataFormat != null && dataFormat.isEnabled()) {
            instance = new KdfReader(dataFormat);
        }
        else if(instance != null && instance.getFormat() != dataFormat ){
            instance.setFormat(dataFormat);
        }
        return instance;
    }

    @Override
    protected 
        boolean readFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected 
        void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected 
        boolean writeLogFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
