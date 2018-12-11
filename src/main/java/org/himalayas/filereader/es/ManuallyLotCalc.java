package org.himalayas.filereader.es;

import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;

public class ManuallyLotCalc {

    public static void main(String[] args) {
        
        new Config("config/dataformat.xml");
        ESHelper es = ESHelper.getInstance();
        if (es == null) {
            return;
        }
        
        for (DataFormat dataFormat : Config.dataFormats.values()) {
            if(dataFormat.isEnabled()) {
                es.initDataForamt(dataFormat);
                es.proceedUncaledLot();
            }
        }
        es.closeConn();
        
    }
    
}
