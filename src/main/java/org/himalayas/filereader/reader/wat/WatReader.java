/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
final public class WatReader extends Reader {

    private HashMap<Integer, ParameterDesc> parameterDescs = new HashMap<>();
    private ArrayList<TestPoint> testPoints = new ArrayList();
    private final String splitStr = "\\|";

    public WatReader(DataFormat format) {
        super(format);
    }

    @Override
    public boolean readFile() {
        if (this.getFileName().endsWith("limit.dis")) {
            return this.readLimitFile();
        }
        return true;
    }

    @Override
    public void init() {
        this.testPoints.clear();
        this.parameterDescs.clear();
    }

    @Override
    public boolean writeLogFile() {
        this.setUnitCnt(this.testPoints.size());
        String lotHead = this.generateLotHeadKVStr();
        int pickUnit = 0;

        for (TestPoint tp : this.testPoints) {
            String dieKVStr = this.getFormat().getUnit().getxCoordNode().getName() + "=" + tp.getX()
                    + "," + this.getFormat().getUnit().getyCoordNode().getName() + "=" + tp.getY()
                    + "," + FieldType.Type + "=" + FieldType.Unit //                    + "," + "PPBin=" + tp.getPickBin()
                    //                    + "," + "Bin=" + this.convertBin(tp.getPickBin())
                    //                    + "," + "Pick=" + isPick
                    ;
            String docValue = lotHead + "," + dieKVStr + "\n";
            if (this.isDebugMode()) {
                System.out.print(docValue);
            }

            if (!this.writeKVString(docValue)) {
                return false;
            }
        }

        // write the file
        String docValue = lotHead
                + "," + FieldType.Type + "=" + FieldType.File
                + "," + FieldType.UnitCnt + "=" + this.testPoints.size()
                + "," + FieldType.IsCaled + "=N"
                + "," + FieldType.PickUnitCnt + "=" + pickUnit
                + "\n";
        if (!this.writeKVString(docValue)) {
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new Config("config/dataformat.xml");
        Reader reader = new WatReader(Config.watFormat);
        Config.watFormat.setProductionMode(false);

        File testDataFile = new File("./testdata/extend/hygon_source_data/WAT");
        for (File lotFile : testDataFile.listFiles()) {
            for (File file : lotFile.listFiles()) {
                reader.loadFile(file);
            }
        }

        System.out.println("total time = " + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

    }

    private boolean readLimitFile() {
        try {
            List<String> lines = Files.readAllLines(this.getFile().toPath());
            if (lines.isEmpty()) {
                System.out.println("Warning: there's no data in this file");
                return true;
            }
            int lineCnt = lines.size();
            int lineNo = -1;
            boolean mapStart = false;
            while (++lineNo < lineCnt) {
                String content = lines.get(lineNo).trim();
                if (this.isDebugMode()) {
                    System.out.printf("line %d:%s\n", lineNo, content);
                }
                if (content.equals("START SECTION WET_LIMIT")) {
                    mapStart = true;
                }
                if (mapStart) {
                    if (this.validParamDescContent(content)) {
                        String[] descStrs = content.split(splitStr);
                        ParameterDesc paramDesc = new ParameterDesc(
                                Integer.valueOf(descStrs[0]), //ParamId
                                descStrs[1], //ParamName
                                descStrs[2], //Group
                                descStrs[3], //LSL
                                descStrs[4], // USL
                                descStrs[9] // unit
                        );
                        this.parameterDescs.putIfAbsent(Integer.valueOf(descStrs[0]), paramDesc);
                    }
                }
            }
            if (this.validDataFileExists()) {
                return this.readDataFile();
            } else {
                System.out.println("Warning: there's no data file for " + this.getFile().getPath());
            }
        } catch (IOException ex) {
            Logger.getLogger(WatReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private boolean validParamDescContent(String content) {
        return (content.split(splitStr).length == 12);
    }

    private boolean validParamValContent(String content) {
        return (content.split(splitStr).length == 2);
    }

    private boolean validTestPointInfo(String content) {
        return (content.split(splitStr).length == 3);
    }

    private boolean validDataFileExists() {
        return new File(this.getFile().getPath().replace("limit", "data")).exists();
    }

    private boolean readDataFile() {
        try {
            File dataFile = new File(this.getFile().getPath().replace("limit", "data"));
            List<String> lines = Files.readAllLines(dataFile.toPath());
            if (lines.isEmpty()) {
                System.out.println("Warning: there's no data in this file");
                return false;
            }
            int lineCnt = lines.size();
            int lineNo = -1;
            boolean testStart = false;
            String[] currentTestPointStr = new String[3];
            while (++lineNo < lineCnt) {
                String content = lines.get(lineNo).trim();
                if (this.isDebugMode()) {
                    System.out.printf("line %d:%s\n", lineNo, content);
                }
                if (content.equals("START SECTION WET_SITE")) {
                    testStart = true;
                } else {
                    if (content.equals("FINISH SECTION WET_SITE")) {
                        testStart = false;
                    }
                }
                if (testStart) {
                    if (this.validTestPointInfo(content)) {
                        currentTestPointStr = content.split(splitStr);
                        TestPoint testPoint = new TestPoint(
                                Integer.valueOf(currentTestPointStr[0]), // Xcoord
                                Integer.valueOf(currentTestPointStr[1]), // Ycoord
                                Integer.valueOf(currentTestPointStr[2]) // Seq
                        );
                        this.testPoints.add(testPoint);
                    }
                    if (this.validParamValContent(content)) {
                        String[] testValStr = content.split(splitStr);
                        int paramId = Integer.valueOf(testValStr[0]);
                        double testVal = Double.valueOf(testValStr[1]);
                        ParameterValue paramVal = new ParameterValue(
                                paramId, // ParamId
                                this.parameterDescs.get(paramId), //ParamDesc
                                testVal
                        );
                        getCurrentTestPoint(currentTestPointStr).setParamVals(paramId, paramVal);
                    }
                }
            }
//            for (TestPoint tp : this.testPoints) {
//                //for (int paramId : tp.getParamVals().keySet()) {
//                System.out.printf("TestPoint %d:%d:%d>>%d\n", tp.getX(), tp.getY(), tp.getSeq(), tp.getParamVals().size());
//                // }
//
//            }

        } catch (IOException ex) {
            Logger.getLogger(WatReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private TestPoint getCurrentTestPoint(String[] currentTestPoint) {
        return this.testPoints.stream()
                .filter(tp -> tp.getX() == Integer.valueOf(currentTestPoint[0])
                && tp.getY() == Integer.valueOf(currentTestPoint[1])
                && tp.getSeq() == Integer.valueOf(currentTestPoint[2]))
                .findFirst().get();
    }
}
