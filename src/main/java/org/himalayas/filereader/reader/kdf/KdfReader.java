/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template kdfFile, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.kdf;

import com.amd.kdf.KDFFieldData;
import com.amd.kdf.collection.KDFCollectionFilter;
import com.amd.kdf.collection.KDFInstanceTree;
import com.amd.kdf.collection.LiteKDFInstanceTree;
import com.amd.kdf.gui.KdfLoader;
import com.amd.kdf.gui.KdfTypes;
import com.amd.kdf.io.SeekableKDFInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.FileReader;
import org.himalayas.filereader.es.LotInfo;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;
import org.himalayas.filereader.util.RecordTypes;
import org.himalayas.filereader.util.XmlNode;
import org.himalayas.filereader.util.Bin;
import org.himalayas.filereader.util.SlaveUnit;

/**
 *
 * @author guanghao
 */
final public
    class KdfReader extends Reader {

    private static
        KdfReader instance = null;
    private final
        KdfParser kdfParser = new KdfParser();
    private
        KDFInstanceTree.Node[] unitsNode = null;
    private
        String nodeContent = null;
    private
        String nodeHead = null;

    //add pinRefs
    private final
        HashMap<String, String> pinRefs = new HashMap();
    private final
        HashMap<String, String> patternRefs = new HashMap();
    private final
        HashMap<String, TestDesc> testDescRefs = new HashMap();
    private final
        HashMap<String, ComponentHash> comHashRefs = new HashMap();

    private
        String flowContextField = null;
    private
        String testResultFieldValue = null;
    private
        String subBaseClassField = null;
    private
        String comHashValue = null;
    private
        int testCntInFlow = 0;
    private
        String mfgStp = null;
    private
        int unitLevelDocCnt = 0;
    private
        int fileLevelDocCnt = 0;
    private
        String baseClass = null;
    private
        int slaveUnitCnt = 0;

    private
        String waferNumber = null;
    private final
        StringBuilder dataContent = new StringBuilder();

    private
        KdfReader(DataFormat format) {
        super(format);
    }

    public static
        KdfReader getInstance(DataFormat dataFormat) {
        if ((dataFormat == null) || (!dataFormat.isKdfData())) {
            System.out.println("Fatal Error: KdfReader only should be initilized with KDF Data Type DataFormat");
            return null;
        }
        if (instance == null && dataFormat.isEnabled()) {
            instance = new KdfReader(dataFormat);
        }
        else if (instance != null && instance.getFormat() != dataFormat) {
            instance.setFormat(dataFormat);
        }
        return instance;
    }

    private
        void clearRefs() {
        this.patternRefs.clear();
        this.pinRefs.clear();
        this.testDescRefs.clear();
        this.comHashRefs.clear();
    }

    @Override
    protected
        void init() {
        //reset all the variables
        this.clearRefs();
        this.mfgStp = null;
        this.fileLevelDocCnt = 0;
        this.unitLevelDocCnt = 0;
        this.slaveUnitCnt = 0;
        this.baseClass = null;
        this.testCntInFlow = 0;
        this.comHashValue = null;
        this.subBaseClassField = null;
        this.testResultFieldValue = null;
        this.flowContextField = null;
        this.nodeContent = null;
        this.nodeHead = null;
        this.unitsNode = null;
        this.waferNumber = null;
    }

    @Override
    protected
        boolean postValidate() {
        this.mfgStp = this.getFile().getName().split("_")[this.getFormat().getMfgStepIndex()];
        if (this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            this.waferNumber = Integer.valueOf(this.getFile().getName().split("_")[this.getFormat().getWaferNumberIndex()]).toString();
        }
        return true;
    }

    /**
     * this method is to parse KDF file
     *
     * @return false if failed to open this file, and will generate a open file
     * error log
     */
    @Override
    protected
        boolean readFile() {
        try {
            this.kdfParser.setKdfFile(this.getFile()).addFile("", "");

        }
        catch (Exception ex) {
            Logger.getLogger(KdfReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    @Override
    protected
        boolean writeLogFile() {

        this.unitsNode = kdfParser.getTree().getNodes(KdfTypes.KDF_RT_UNIT);
        if (this.isEmpthData()) {
            return true;
        }

        this.readRoot();

        /**
         * read info
         */
        this.readRootNode(KdfTypes.KDF_RT_INFO);

        /**
         * read lot
         */
        this.readRootNode(KdfTypes.KDF_RT_LOT);

        /**
         * read wafer
         */
        if (this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            this.readRootNode(KdfTypes.KDF_RT_WAFER);
        }

        /**
         * read mfg
         */
        this.readRootNode(RecordTypes.KDF_RT_MFG);

        this.readComponement();
        this.readOMI();
        this.readRooT_Refs(KdfTypes.KDF_RT_PINREF, this.pinRefs);
        this.readRooT_Refs(KdfTypes.KDF_RT_PATTERN, this.patternRefs);
        readTestDesc();
        readRoot_ComHash();
        /**
         * print and log head Data
         */
        this.getFormat().resetHeadTimeFormat();
        if (this.isDebugMode()) {
            this.getFormat().printHeadInfo();
        }

        /**
         * here there are 3 types doc need to send to es A: Type=File, only
         * exist for ate since slt file is just unit B: Type=Unit, C:
         * Type=others for the details test items.
         *
         */
        // the lotHeadStr does't contains lot open/start/end time, we use the FieldType.FileTime as the timestamper
        String lotHeadStr = this.getFormat().getLotHeadKVString() + "," + FieldType.FileTime + "=" + this.formatTimeStr(this.getFileOpenTime());

        readBinDesc();
        System.out.printf("%s: read all kdf nodes time is  %d\n", LocalDateTime.now(), (System.currentTimeMillis() - this.getJobStartTime()));

        int unitNo = 0;

        for (KDFInstanceTree.Node unit : unitsNode) {
            unitNo++;
            this.slaveUnitCnt = 0;

            this.dataContent.setLength(0);
            this.getFormat().clearUnitData();
            this.readUnit(unit);
            this.readUnitBin(unit);
            this.readUnitSerialNumber(unit);
            this.readOldKDFBinDesc(unit);

            this.getFormat().calTestTime();
            this.getFormat().resetUnitTimeFormat();
            this.getFormat().setBinDesc();
            readFTSLTXY(this.waferNumber);

            /**
             * here we create the unit level doc <unitKVStr>
             * unitDocKVString contains the unit start and end timestamper, add
             * kdf file name in the unit level doc for ate only.
             */
            String unitKVStr = lotHeadStr + this.getFormat().getUnitDocKVString() + getSlaveNodeKVString() + this.getFormat().getX0Y0KVString();
            String slaveKVStr = this.getSlaveUnitKVString(lotHeadStr + this.getFormat().getSlaveUnitDocKVString());

            if (this.getFormat().isAddFileName()) {
                unitKVStr += this.getFileNameKVStr();
            }
            unitKVStr += "\n";

            if (validateFullForamtString(unitKVStr)) {
                this.dataContent.append(slaveKVStr).append(unitKVStr);

            }
            else {
                System.err.println("Error Unit Doc: \n" + slaveKVStr + unitKVStr);
                System.err.flush();
                return false;
            }

            /**
             * here we create the head for each test time it combines the:
             * <lotHeadStr> which contains the lot info
             * <UnitHeadTestKVStr> which contains the unit level info and has no
             * the unit start/end time
             * <SlaveNodeKVString> which contains the slave die info for the
             * whole part
             */
            String testItemHeadStr = lotHeadStr + this.getFormat().getUnitHeadTestKVStr() + this.getFormat().getX0Y0KVString();
            if (this.getFormat().isAppendSlaveUnitId2Test()) {
                testItemHeadStr += getSlaveNodeKVString();
            }

            /**
             * print and log unit data
             */
            if (this.isDebugMode()) {
                System.out.printf("Unit#%d KV String:\n%s\n", unitNo, unitKVStr);
            }
            this.unitLevelDocCnt = this.slaveUnitCnt + 1;

            for (KDFInstanceTree.Node item : unit.getChildren()) {

                String nodeName = item.getName();
                if (nodeName.equals(KdfTypes.KDF_RT_SERIAL)
                    || nodeName.equals(KdfTypes.KDF_RT_BIN)
                    || nodeName.equals(KdfTypes.KDF_RT_BINDESC)) {
                    continue;
                }
                this.nodeHead = testItemHeadStr;
                this.flowContextField = "";
                this.testResultFieldValue = "";
                this.subBaseClassField = "";
                this.comHashValue = "";
                String nodeKVString = printNodeInfo(item, 0);
                this.dataContent.append(nodeKVString);
                // log for debugging
                if (this.isDebugMode()) {
                    System.out.println(nodeKVString);
                }

            }
            this.fileLevelDocCnt += this.unitLevelDocCnt;

            // IO error and exit for this file
            if (!this.writeKVString(dataContent.toString())) {
                return false;
            }
        }
        // add the file level doc to es
        if (!this.writeFileKVString()) {
            return false;
        }

        LotInfo lotInfo = new LotInfo(this.getLotNumber(), this.mfgStp);
        this.getFormat().getLotList().putIfAbsent(lotInfo.getDoc_Id(), lotInfo);

        kdfDoneCnt++;
        return true;
    }

    private
        boolean isEmpthData() {
        if (unitsNode == null || unitsNode.length == 0) {
            System.out.println("Empty Uni Record: no unit record found in this kdf...");
            return true;
        }
        else {
            this.setUnitCnt(unitsNode.length);
            if (this.isDebugMode()) {
                System.out.println("total unit cnt is: " + this.getUnitCnt());
            }
        }

//        if (this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
//            KDFInstanceTree.Node[] wafers = kdfParser.getTree().getNodes(KdfTypes.KDF_RT_WAFER);
//            if (wafers != null) {
//                if (wafers.length > 1) {
//                    System.out.println("Error: we expect only one wafer in one kdf file...");
//                    return true;
//                }
//                else if (wafers.length == 0) {
//                    System.out.println("Empty Uni Record: no unit record found in this kdf...");
//                    return true;
//                }
//            }
//        }
        return false;

    }

    /**
     * read root info
     */
    public
        void readRoot() {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] roots = this.kdfParser.getTree().getRoots();
        if (roots != null && roots.length > 0) {
            for (KDFInstanceTree.Node rootNode : roots) {
                if (this.isDebugMode()) {
                    System.out.println("Root name is " + rootNode.getName());
                }
                for (XmlNode xmlNode : this.getFormat().getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (rootNode.containsKey(fieldName)) {
                            xmlNode.setValue(rootNode.get(fieldName).toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.printf("reading %s time is : %d\n", "roots", (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * read a node type from kdf tree
     *
     * @param nodeType
     */
    public
        void readRootNode(String nodeType) {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] nodes = this.kdfParser.getTree().getNodes(nodeType);
        if (nodes != null && nodes.length > 0) {
            for (KDFInstanceTree.Node node : nodes) {
                for (XmlNode xmlNode : this.getFormat().getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (node.containsKey(fieldName)) {
                            xmlNode.setValue(node.get(fieldName).toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.printf("reading %s time is : %d\n", nodeType, (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * read component
     */
    public
        void readComponement() {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] coms = this.kdfParser.getTree().getNodes(KdfTypes.KDF_RT_COMPONENT);
        if (coms != null && coms.length > 0) {
            for (KDFInstanceTree.Node component : coms) {
                for (XmlNode xmlNode : this.getFormat().getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (component.containsKey("componentType")
                            && component.get("componentType").toString().equals(fieldName)
                            && component.containsKey("componentId")) {
                            xmlNode.setValue(component.get("componentId").toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading component time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * read Omi
     */
    private
        void readOMI() {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] omis = this.kdfParser.getTree().getNodes(KdfTypes.KDF_RT_OMI);
        if (omis != null && omis.length > 0) {
            for (KDFInstanceTree.Node omi : omis) {
                for (XmlNode xmlNode : this.getFormat().getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (omi.get("name") != null
                            && omi.get("name").toString().equals(fieldName)
                            && omi.containsKey("value")) {
                            xmlNode.setValue(omi.get("value").toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading omi time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * read the base class the sub class the class is stored in the <testDescs>
     * the format is subClassName=subClassName,baseClass=baseClassName
     */
    private
        void readTestDesc() {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] descs = this.kdfParser.getTree().getNodes(KdfTypes.KDF_RT_TESTDESC);
        if (descs != null && descs.length > 0) {
            for (KDFInstanceTree.Node des : descs) {
                String value = "";
                String testDescId = null;
                String baseClassName = null;
                String subClassName = null;
                for (String fieldName : des.keySet()) {
                    String fieldValue = des.get(fieldName).toString().trim();

                    if (fieldValue.isEmpty() || fieldName.isEmpty()) {
                        continue;
                    }
                    if (fieldName.endsWith(Config.subClass)) {
                        subClassName = fieldValue;
                        // base class and sub class filters need to apply them here since it needs the base and sub class
                        // to generate the generated test name
                        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT)
                            && this.getFormat().getSubClassFilters().contains(fieldValue)) {
                            continue;
                        }
                    }
                    if (fieldName.endsWith(Config.baseClass)) {
                        baseClassName = fieldValue;
                        // base class and sub class filters need to apply them here since it needs the base and sub class
                        // to generate the generated test name
                        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT)
                            && this.getFormat().getBaseClassFilters().contains(fieldValue)) {
                            continue;
                        }
                    }
                    if (fieldName.equals(Config.testDescId)) {
                        testDescId = fieldValue;
                        continue;
                    }
                    // apply only after the base and sub class and also the testDescId
                    if (this.getFormat().getTestDescFieldFilters().contains(fieldName)) {
                        continue;
                    }
                    if ((!this.getFormat().getTestDescFieldSelectors().isEmpty())
                        && (!this.getFormat().getTestDescFieldSelectors().contains(fieldName))) {
                        continue;
                    }

                    value += "," + fieldName.replace('.', '_') + "=" + fieldValue.replace(',', ' ').replace('=', ' ');

                }
                if (testDescId != null && (!value.isEmpty())) {
                    this.testDescRefs.put(testDescId, new TestDesc(baseClassName, subClassName, value));
                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading test desc time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    private
        void readBinDesc() {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] descs = this.kdfParser.getTree().getNodes(KdfTypes.KDF_RT_BINDESC);
        if (descs != null && descs.length > 0) {
            for (KDFInstanceTree.Node binDesc : descs) {

                String binType = "";
                String binNumber = null;
                String binDescription = null;
                String flag = null;

                for (String key : binDesc.keySet()) {
                    if (key.equalsIgnoreCase("bintype")) {
                        binType = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binNumber")) {
                        binNumber = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binDescription")) {
                        Object o = binDesc.get(key).getValue();
                        if (o != null) {
                            binDescription = this.formatBinDesc(o.toString());
                        }
                    }
                    else if (key.equalsIgnoreCase("binDescFlag")) {
                        flag = binDesc.get(key).getValue().toString();
                    }
                }

                if (binType.equals("83") || binType.equals("S")) {
                    if (binNumber != null
                        && flag != null) {
                        this.getFormat().getSoftBinDescs().putIfAbsent(binNumber, new Bin(binNumber, binDescription, flag));
                    }

                }
                else if (binType.equals("72") || binType.equals("H")) {
                    if (binNumber != null
                        && flag != null) {
                        this.getFormat().getHardBinDescs().putIfAbsent(binNumber, new Bin(binNumber, binDescription, flag));
                    }

                }

            }
            if (this.isDebugMode()) {
                System.out.println("reading binDesc node time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    private
        void readRooT_Refs(String kdfType, HashMap<String, String> data) {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] miscs = this.kdfParser.getTree().getNodes(kdfType);
        if (miscs != null && miscs.length > 0) {
            for (KDFInstanceTree.Node misc : miscs) {
                String key = null;
                String value = null;
                for (String fieldName : misc.keySet()) {
                    if (misc.get(fieldName).getType() == 5) {
                        key = misc.get(fieldName).getValue().toString();
                    }
                    else if (misc.get(fieldName).getType() == 11) {
                        value = misc.get(fieldName).getValue().toString();
                    }
                }
                if (key != null && (!key.isEmpty()) && value != null && (!value.isEmpty())) {
                    data.put(key, value.trim().replace(',', ' ').replace('=', ' '));

                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading " + kdfType + " time is " + (System.currentTimeMillis() - startTime));
            }

        }
    }

    private
        void readRoot_ComHash() {
        long startTime = System.currentTimeMillis();
        KDFInstanceTree.Node[] rawComHashRefs = this.kdfParser.getTree().getNodes(KdfTypes.KDF_RT_COMPGROUPDESC);
        if (rawComHashRefs != null && rawComHashRefs.length > 0) {
            for (KDFInstanceTree.Node comHashNode : rawComHashRefs) {
                KDFFieldData comNameField = comHashNode.get("componentName");
                if (comNameField != null) {
                    String comName = comNameField.getValue().toString().replace('.', '_');
                    String comClass = comHashNode.get("componentClass").getValue().toString().replace('.', '_');

                    String comHash = comHashNode.get("componentHash").getValue().toString();
                    String comInst = comHashNode.get("componentInst").getValue().toString();
                    this.comHashRefs.put(comHash, new ComponentHash(comClass, comName));
                }

            }
        }
        if (this.isDebugMode()) {
            System.out.println("reading component hash time is " + (System.currentTimeMillis() - startTime));
        }

    }

    private
        String formatBinDesc(String binDesc) {
        int size = binDesc.length();
        String value = "";
        int i = 0;
        while (i < size && i < 128) {
            char chr = binDesc.charAt(i);
//			if(chr == ',' || chr == '=' || chr == 13 || chr == 10) {
//
//			}
            if ((chr >= 48 && chr <= 57)
                || (chr >= 65 && chr <= 90)
                || (chr >= 97 && chr <= 122)) {

            }
            else {
                chr = 32;
            }
            value += chr;
            i++;
        }
        return value;
    }

    private
        void readUnit(KDFInstanceTree.Node unit) {
        for (XmlNode xmlNode : this.getFormat().getUnit().getNodes().values()) {
            for (String fieldName : xmlNode.getFieldNames()) {
                if (unit.containsKey(fieldName)) {
                    xmlNode.setValue(unit.get(fieldName).toString());
                }
            }
        }
    }

    private
        void readUnitBin(KDFInstanceTree.Node unit) {
        KDFInstanceTree.Node[] bins = unit.getChildren(KdfTypes.KDF_RT_BIN);
        if (bins != null && bins.length > 0) {
            for (KDFInstanceTree.Node bin : bins) {
                for (XmlNode xmlNode : this.getFormat().getUnit().getNodes().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (bin.containsKey(fieldName)) {
                            xmlNode.setValue(bin.get(fieldName).toString());
                        }
                    }
                }
            }
        }
    }

    private
        void readUnitSerialNumber(KDFInstanceTree.Node unit) {
        int dieCnt = 0;
        KDFInstanceTree.Node[] serialNumbers = unit.getChildren(KdfTypes.KDF_RT_SERIAL);
        if (serialNumbers != null && serialNumbers.length > 0) {
            for (KDFInstanceTree.Node serialNumber : serialNumbers) {
                for (XmlNode xmlNode : this.getFormat().getUnit().getNodes().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (serialNumber.containsKey(fieldName)) {

                            /**
                             * for slt and ate only slt and ate contains 4 die
                             * per device one master die and 3 slave die
                             */
                            if (xmlNode.isUnitIdNode()
                                && (!this.getFormat().getDataType().equals(Config.DataTypes.WaferSort))
                                && (!serialNumber.get("master").getValue().toString().equals("1"))) {
                                //add for slave unit id if unit id is not null
                                if (serialNumber.get(fieldName) != null) {
                                    this.getFormat().getUnit().getSlaveUnits().add(
                                        new SlaveUnit(serialNumber.get(fieldName).toString(), serialNumber.get("componentHash").toString())
                                    );
                                }
                                continue;

                            }

                            xmlNode.setValue(serialNumber.get(fieldName).toString());
                        }
                    }
                }
            }
        }
        /**
         *
         * here we have to generate a dummy unit id for the master die only
         * format:
         */
        // TODO
        if (this.getFormat().getUnit().getUnitIdNode().getXmlValue().isEmpty()) {

        }
    }

    private
        void readOldKDFBinDesc(KDFInstanceTree.Node unit) {
        // very very old kdf file store bin info in unit
        KDFInstanceTree.Node[] binDescs = unit.getChildren(KdfTypes.KDF_RT_BINDESC);
        if (binDescs != null && binDescs.length > 0) {
            for (KDFInstanceTree.Node binDesc : binDescs) {
                String binType = "";
                String binNumber = null;
                String binDescription = null;
                String flag = null;
                for (String key : binDesc.keySet()) {
                    if (key.equalsIgnoreCase("bintype")) {
                        binType = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binNumber")) {
                        binNumber = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binDescription")) {
                        Object o = binDesc.get(key).getValue();
                        if (o != null) {
                            binDescription = this.formatBinDesc(o.toString());
                        }
                    }
                    else if (key.equalsIgnoreCase("binDescFlag")) {
                        flag = binDesc.get(key).getValue().toString();
                    }
                }
                //SoftBin
                if (binNumber != null
                    && flag != null) {
                    Bin bin = new Bin(binNumber, binDescription, flag);
                    if (binType.equals("83") || binType.equals("S")) {
                        this.getFormat().getUnit().setSoftBinDesc(bin.getBinDescription());
                        this.getFormat().getUnit().getSoftBinNode().setValue(binNumber);
                        this.getFormat().getUnit().setFlag(bin.getFlag());
                    } //HardBin
                    else if (binType.equals("72") || binType.equals("H")) {
                        this.getFormat().getUnit().setHardBinDesc(binDescription);
                        this.getFormat().getUnit().getHardBinNode().setValue(binNumber);
                        this.getFormat().getUnit().setFlag(bin.getFlag());
                    }
                }
            }
        }
    }

    /**
     * H800A0010041704 read waferLotNumber, waferNumber, x and y from unit id
     *
     * @param waferNumber
     */
    private
        void readFTSLTXY(String waferNumber) {
        if (!this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            XmlNode waferNumberNode = this.getFormat().getUnit().getWaferNumberNode();
            XmlNode unitIdNode = this.getFormat().getUnit().getUnitIdNode();
            XmlNode xNode = this.getFormat().getUnit().getxCoordNode();
            XmlNode yNode = this.getFormat().getUnit().getyCoordNode();
            XmlNode waferLotNode = this.getFormat().getUnit().getWaferLotNode();

            if (unitIdNode.getXmlValue().length() > yNode.getEndIndex()) {
                waferNumberNode.setValue(Integer.valueOf(unitIdNode.getValue().substring(waferNumberNode.getStartIndex(), waferNumberNode.getEndIndex())).toString());
                xNode.setValue(Integer.valueOf(unitIdNode.getValue().substring(xNode.getStartIndex(), xNode.getEndIndex())).toString());
                yNode.setValue(Integer.valueOf(unitIdNode.getValue().substring(yNode.getStartIndex(), yNode.getEndIndex())).toString());
                waferLotNode.setValue(unitIdNode.getValue().substring(waferLotNode.getStartIndex(), waferLotNode.getEndIndex())
                    + "."
                    + unitIdNode.getValue().substring(waferLotNode.getEndIndex(), waferLotNode.getEndIndex() + 2));
            }
        }
        else {
            if (this.getFormat().getUnit().getWaferNumberNode().getValue() == null) {
                this.getFormat().getUnit().getWaferNumberNode().setValue(waferNumber);
            }
        }
    }

    /**
     * this method is to generate the kv string for the slave die format:
     * ,DIE.1=unitID_1,DIE_2=unitID_2
     *
     *
     * @return
     */
    private
        String getSlaveNodeKVString() {
        String value = "";
        if (this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            return value;
        }
        for (SlaveUnit slaveUnit : this.getFormat().getUnit().getSlaveUnits()) {
            if (slaveUnit.getComHash() != null) {
                ComponentHash componentHash = this.comHashRefs.get(slaveUnit.getComHash());

                if (componentHash != null && componentHash.getComName() != null && (!componentHash.getComName().isEmpty())) {
                    value += "," + componentHash.getComName() + "=" + slaveUnit.getUnitId();
                }
            }
        }
        return value;
    }

    private
        String getSlaveUnitKVString(String lotHeadStr) {
        String value = "";
        if (this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            return value;
        }
        for (SlaveUnit slaveUnit : this.getFormat().getUnit().getSlaveUnits()) {
            if (slaveUnit.getComHash() != null) {
                ComponentHash componentHash = this.comHashRefs.get(slaveUnit.getComHash());

                if (componentHash != null && componentHash.getComName() != null && (!componentHash.getComName().isEmpty())) {
                    //value += "," + componentHash.getComName() + "=" + slaveUnit.getUnitId();
                    String tempStr = "";
                    XmlNode waferNumberNode = this.getFormat().getUnit().getWaferNumberNode();
                    XmlNode waferLotNode = this.getFormat().getUnit().getWaferLotNode();
                    XmlNode xNode = this.getFormat().getUnit().getxCoordNode();
                    XmlNode yNode = this.getFormat().getUnit().getyCoordNode();
//					tempStr += "," + FieldType.Type + "=" + FieldType.Unit;
                    tempStr += "," + this.getFormat().getUnit().getUnitIdNode().getName() + "=" + slaveUnit.getUnitId();
                    tempStr += this.getFileNameKVStr();
                    tempStr += "," + FieldType.DieType + "=" + componentHash.getComName();
                    String masterDieId = this.getFormat().getUnit().getUnitIdNode().getXmlValue();
//                    if (!masterDieId.isEmpty()) {
//                        tempStr += "," + FieldType.MasterDieId + "=" + this.getFormat().getUnit().getUnitIdNode().getValue();
//                    }

                    if (slaveUnit.getUnitId().length() > this.getFormat().getUnit().getyCoordNode().getEndIndex()) {
                        tempStr += "," + waferNumberNode.getName() + "="
                            + (Integer.valueOf(slaveUnit.getUnitId().substring(waferNumberNode.getStartIndex(), waferNumberNode.getEndIndex())).toString());
                        tempStr += "," + xNode.getName() + "="
                            + (Integer.valueOf(slaveUnit.getUnitId().substring(xNode.getStartIndex(), xNode.getEndIndex())).toString());
                        tempStr += "," + yNode.getName() + "="
                            + (Integer.valueOf(slaveUnit.getUnitId().substring(yNode.getStartIndex(), yNode.getEndIndex())).toString());
                        tempStr += "," + waferLotNode.getName() + "="
                            + slaveUnit.getUnitId().substring(waferLotNode.getStartIndex(), waferLotNode.getEndIndex())
                            + "."
                            + slaveUnit.getUnitId().substring(waferLotNode.getEndIndex(), waferLotNode.getEndIndex() + 2);
                    }
                    tempStr += "\n";
////					if(validatePartForamtString(tempStr)) {
                    value += lotHeadStr + tempStr;
                    slaveUnitCnt++;
//					}

                }
            }
        }
        return value;
    }

    private
        String getFileNameKVStr() {
        String value = "";
        if (this.getFormat().isAddFileName()) {
            value = "," + "FileName=" + this.getFileName();
        }
        return value;
    }

    private
        String printNodeInfo(KDFInstanceTree.Node node, int level) {

        String formatString = "";
        if (node == null) {
            return formatString;
        }
        String space = "";
        switch (level) {
            case 0:
                space = "--";
                break;
            case 1:
                space = "  |--";
                break;
            case 2:
                space = "    |--";
                break;
            case 3:
                space = "      |--";
                break;
            case 4:
                space = "        |--";
                break;
            case 5:
                space = "          |--";
                break;
        }

        space = "";
        String nodeName = node.getName();
        if (nodeName == null) {
            System.out.println("Warnning: empty node name node found: " + node.toString());
            return formatString;
        }
        boolean isFlow = false;
        if (nodeName.equals(KdfTypes.KDF_RT_FLOW)) {
            this.flowContextField = "";
            isFlow = true;
            baseClass = null;
        }

        if (nodeName.equals(KdfTypes.KDF_RT_TEST)) {
            // reset all the test related items here
            this.baseClass = null;
            this.testCntInFlow++;
            this.subBaseClassField = "";
            this.comHashValue = "";
            this.testResultFieldValue = "";

            String idClass = null;
            if (node.get("testDescId") != null) {
                idClass = node.get("testDescId").toString();
            }

            if (idClass != null && this.testDescRefs.containsKey(idClass)) {
                this.subBaseClassField = this.testDescRefs.get(idClass).getValue();

                // return immediately if validate failed
                if (!validateBaseSubClass(idClass)) {
                    return formatString;
                }
            }
            if (this.getFormat().isEnabledComponentHash()) {
                String comHash = null;
                if (node.get("componentHash") != null) {
                    comHash = node.get("componentHash").toString();
                }
                if (comHash != null && this.comHashRefs.containsKey(comHash)) {
                    this.comHashValue = this.comHashRefs.get(comHash).getKVString();
                }
            }

        }

        if (!validateNodeType(nodeName, node)) {
//			 skip those nodes
            if (this.isDebugMode()) {
                System.out.println("Skip this Node Type: " + node);
            }
            return formatString;
        }
        else {
            /**
             * handle the current node here
             */

            String formatNodeString = formateNode(node) + "\n";
            if (validatePartForamtString(formatNodeString)) {
                formatString += space + this.nodeHead + formatNodeString;
                this.unitLevelDocCnt++;
            }
            else {
                System.out.println("--------------------------------------------------------------------");
                System.out.println("FATAL Error: node format string validation failed");
                System.out.println("FATAL Error: es can not read this node string");
                System.out.println("Node=" + node);
                System.out.printf("formatString is:%s", formatNodeString);

                //System.exit(1);
            }

        }
        if (isFlow) {
            KDFFieldData flowContextData = node.get("context");
            if (flowContextData != null && (!flowContextData.isNull())) {
                String contextData = flowContextData.getValue().toString();
                if ((contextData != null) && (!contextData.isEmpty()) && (this.getFormat().getFlowContextFilters().contains(contextData))) {
                    return formatString;
                }
            }
        }

        // return immediately after slt test
        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT) && nodeName.equals(KdfTypes.KDF_RT_TEST)) {
            return formatString;
        }

        /**
         * handle the child node here
         */
        for (KDFInstanceTree.Node item : node.getChildren()) {
            if (item != null) {
                formatString += printNodeInfo(item, level + 1);
            }
        }
        return formatString;
    }

    /**
     * method to check if the logging is disabled to this node validate the node
     * name filter validate the log fail filter
     *
     * @return
     */
    private
        boolean validateNodeType(String nodeType, KDFInstanceTree.Node node) {
        if ((!this.getFormat().getNodeTypeFilters().isEmpty()) && this.getFormat().getNodeTypeFilters().contains(nodeType)) {
            return false;
        }
        if ((!this.getFormat().getNodeTypeSelectors().isEmpty()) && (!this.getFormat().getNodeTypeSelectors().contains(nodeType))) {
            return false;
        }

        /**
         * log fail filters A: fail node type selector B: base calss selector
         */
        if (this.getFormat().getLogOnlyFailNodes().contains(nodeType)
            && this.getFormat().getLogFailOnlyBaseClasses().contains(this.baseClass)) {
            KDFFieldData fieldData = node.get("result");
            if (fieldData == null) {
                return true;
            }
            String fieldValue = fieldData.toString().trim();
            if (this.isDebugMode()) {
                System.out.println();
            }
            return fieldValue.equals("0");
        }

        return true;
    }

    /**
     * this method is to filter and selector the base and sub class
     *
     * @param baseSubClass
     * @return
     */
    private
        boolean validateBaseSubClass(String idClass) {

        // slt class filters is in the readTestDesc method, slt TestDesc is different from ate
        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT)) {
            return true;
        }
        //selector and filters

        String subClassName = this.testDescRefs.get(idClass).getSubClass();
        String baseClassName = this.testDescRefs.get(idClass).getBaseClass();
        this.baseClass = baseClassName;

        // validate the base class first
        if (!this.getFormat().getBaseClassFilters().isEmpty()) {
            // filter out
            if (this.getFormat().getBaseClassFilters().contains(baseClassName)) {
                return false;
            }
        }
        if (!this.getFormat().getBaseClassSelectors().isEmpty()) {
            // selector
            if (!this.getFormat().getBaseClassSelectors().contains(baseClassName)) {
                return false;
            }
        }

        // validate the sub class
        if (!this.getFormat().getSubClassFilters().isEmpty()) {
            // filter out
            if (this.getFormat().getSubClassFilters().contains(subClassName)) {
                return false;
            }
        }

        if (!this.getFormat().getSubClassSelectors().isEmpty()) {
            // selector
            if (!this.getFormat().getSubClassSelectors().contains(subClassName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * validate a string return false if this string contains char ',' or '='
     * else return true
     *
     * @param formatString
     * @return
     */
    private
        boolean validatePartForamtString(String formatString) {
        int length = formatString.length();
        int commaCnt = 0;
        int equalityCnt = 0;
        while (length-- > 0) {
            char chr = formatString.charAt(length);
//			if(chr == '\n') {
//				System.out.println("Error: bad format string which contains LF," + formatString);
//				return false;
//			}
            if (chr == ',') {
                commaCnt++;
            }
            else if (chr == '=') {
                equalityCnt++;
            }
        }
        return (commaCnt == equalityCnt);

    }

    private
        String formatTestNode(KDFInstanceTree.Node node) {
        String value = "";
        String testResultField = this.getFieldKVStr(node, "result", "result");
        String startTimeField = this.getFieldKVStr(node, "startTimestamp", "startTimestamp");
        String endTimeField = this.getFieldKVStr(node, "endTimestamp", "endTimestamp");
        String alarmField = this.getFieldKVStr(node, "alarm", "alarm");

        if (!testResultField.isEmpty()) {
            value += "," + testResultField;
            this.testResultFieldValue = testResultField;
        }
        if (!this.getFormat().getFieldFiters().contains("startTimestamp")) {
            if (!startTimeField.isEmpty()) {
                value += "," + startTimeField;
            }
        }
        if (!this.getFormat().getFieldFiters().contains("endTimestamp")) {
            if (!endTimeField.isEmpty()) {
                value += "," + endTimeField;
            }
        }
        if (!this.getFormat().getFieldFiters().contains("alarm")) {
            if (!alarmField.isEmpty()) {
                value += "," + alarmField;
            }
        }

        // here add the test type node test time
        value += this.getTestTimeValue(node);

        return value;

    }

    private
        String getTestTimeValue(KDFInstanceTree.Node node) {
        String value = "";
        if (this.getFormat().getFieldFiters().contains("TestTime")) {
            return value;
        }
        // here add the test type node test time
        if (node.containsKey("startTimestamp")) {
            String startTimeValue = node.get("startTimestamp").toString().trim();
            if (node.containsKey("endTimestamp")) {
                String endTimeValue = node.get("endTimestamp").toString().trim();
                if (startTimeValue.length() == 13 && endTimeValue.length() == 13) {
                    value = ",TestTime=" + (Long.valueOf(endTimeValue) - Long.valueOf(startTimeValue)) / 1000.0;
                }
            }
        }
        return value;
    }

    /**
     * bug1: Type: StructureMeasure, Parent Type: Test, Data: [result=1,
     * highSpecLimit=20.0, structureName=RDIL0:Temp, paramRefPtr=,
     * componentHash=, lowSpecLimit=-20.0, units=, value=1.25] bug2: Type:
     * BitString, Parent Type: Test, Data: [bitValues=0000, paramRefPtr=,
     * componentHash=, segmentName=VAR:Rev_ID;EXPR:''0000'']
     *
     * @param node
     * @return
     */
    private
        String formateNode(KDFInstanceTree.Node node) {
        String value = "";
        String nodeType = node.getName().trim();

        if (!this.flowContextField.isEmpty()) {
            value += "," + this.flowContextField;
        }

        if (nodeType.equals(KdfTypes.KDF_RT_EVALUATION)) {
            value += "," + this.testResultFieldValue;
        }
        if (!this.subBaseClassField.isEmpty()) {
            value += this.subBaseClassField;
        }
        value += this.comHashValue;

        // add the node type
        if (!nodeType.isEmpty()) {
            value += "," + FieldType.Type + "=" + node.getName().trim();
        }

        if (nodeType.equals("Test")) {
            value += this.formatTestNode(node);
            return value;
        }

//			if (nodeType.equals(RecordTypes.KDF_RT_ULSD)) {
//				value += this.formatULSDNode(node);
//				return value;
//			}
        boolean isLog = true;
        if (nodeType.equals(RecordTypes.KDF_RT_ULSD)) {
            isLog = false;
        }

        for (String fieldName : node.keySet()) {
            Byte fieldType = node.get(fieldName).getType();

//			if ((!this.allFields.contains(fieldName)) && (fieldType == 11 || fieldType == 22 || fieldType == 33)) {
//				this.allFields.add(fieldName);
//			}
            String fieldValue = node.get(fieldName).toString().trim();

            if (fieldName == null || fieldName.trim().isEmpty()) {
                continue;
            }
            fieldName = fieldName.trim();
            if (this.getFormat().getFieldFiters().contains(fieldName)) {
                continue;
            }

            if (!fieldValue.isEmpty()) {
//                if (!this.isFormatField(fieldValue, fieldName, isLog)) {
//                    continue;
//
//                }
                if (fieldName.equals(FieldType.PinRefPtr)) {
                    String pinName = this.pinRefs.get(fieldValue);
                    if (fieldValue.contains(",")) {
                        System.out.println("Multip Pin found: " + fieldValue);
                    }
                    if (pinName != null) {
                        value += "," + FieldType.Pin + "=" + pinName;
                    }
                }
                else if (fieldName.equals(FieldType.PatternId)) {
                    String pattern = this.patternRefs.get(fieldValue);
                    if (fieldValue.contains(",")) {
                        System.out.println("Multip Patterns found: " + fieldValue);
                    }
                    if (pattern != null) {
                        value += "," + FieldType.Pattern + "=" + pattern;
                    }
                }
                else {
                    value += this.getFieldKVStr(fieldName, fieldValue);
//                    value += "," + fieldName.replace('.', '_') + "=" + fieldValue;

                }
            }
        }
        if (node.getName().equals(KdfTypes.KDF_RT_FLOW)) {
            // all the child nodes whose parent node is flow, add flow context and flow result into sub nodes
            this.flowContextField = this.getFieldKVStr(node, "context", "context");
//				this.testResultField += this.getFieldKVStr(node, "result", "flowResult");
            testCntInFlow = 0;
            value += this.getTestTimeValue(node);
        }
        return value;

    }

    /**
     * get the filed kv value return empty if key or value is empty
     *
     * @param node
     * @param fieldName
     * @return
     */
    private
        String getFieldKVStr(KDFInstanceTree.Node node, String fieldName, String aliasName) {
        String value = "";

        KDFFieldData fieldData = node.get(fieldName);
        if (fieldData == null) {
            return value;
        }
        String fieldValue = fieldData.toString().trim();
        if ((!fieldValue.isEmpty()) && this.isFormatField(fieldValue, fieldName, true)) {
            if (aliasName != null) {
                fieldName = aliasName;
            }
            value = fieldName + "=" + fieldValue;
        }

        return value;
    }

    /**
     *
     * @param fieldName
     * @param fieldValue
     * @return
     */
    private
        String getFieldKVStr(String fieldName, String fieldValue) {
        String value = "";
        if (fieldName.isEmpty() || fieldValue.isEmpty()) {
            return value;
        }
        int length = fieldValue.length();
        if (!fieldName.equals("bitValues")) {
            if (length > this.getFormat().getFieldValueLengthLimit()) {
                if (this.isDebugMode()) {
                    System.out.printf("Too long Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                }
                return value;
            }
        }
        if (fieldName.equals("value")) {
            try {
                float numberValue = Float.valueOf(fieldValue);
                if (Float.isInfinite(numberValue)) {
                    System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                    return value;
                }
            }
            catch (NumberFormatException e) {
                System.out.printf("Bad Format Field: value should be float, fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                return value;
            }
        }
        String formatValue = this.formatFieldValue(fieldValue);
        if (formatValue.isEmpty()) {
            return value;
        }
        value = "," + fieldName.replace('.', '_').replace('=', ':').replace(',', ';') + "=" + formatValue;
        return value;

    }

    private
        String formatFieldValue(String fieldValue) {
        int length = fieldValue.length();
        String value = "";
        int i = 0;
        while (i != length) {
            char chr = fieldValue.charAt(i);
            if (chr == ',') {
                value += ';';
            }
            else if (chr == '=') {
                value += ':';
            }
            else if (chr != 13 && chr != 10) {
                value += chr;
            }
            i++;
        }
        return value;

    }

    private
        boolean isFormatField(String fieldValue, String fieldName, boolean isLog) {
        int length = fieldValue.length();
        if (length > this.getFormat().getFieldValueLengthLimit()) {
            if (this.isDebugMode()) {
                System.out.printf("Too long Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
            }
            return false;
        }

        while (length-- > 0) {
            char chr = fieldValue.charAt(length);
            if (chr == ',' || chr == '=' || chr == 13 || chr == 10) {
                if (isLog) {
                    System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                }
                return false;
            }
        }

        if (fieldName.equals("value")) {
            try {
                float value = Float.valueOf(fieldValue);
                if (Float.isInfinite(value)) {
                    System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                    return false;
                }
            }
            catch (NumberFormatException e) {
                System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                return false;
            }
        }
        return true;

    }

    /**
     * append the file level doc to the data log file
     *
     * @return
     */
    private
        boolean writeFileKVString() {
        return this.writeKVString(this.getAteFileKVStr());
    }

    private
        String getAteFileKVStr() {

        String value = FieldType.Type + "=" + FieldType.File
            + "," + FieldType.DoneTime + "=" + ZonedDateTime.now().toOffsetDateTime()
            + "," + FieldType.KdfName + "=" + this.getFileName()
            + "," + FieldType.UnitCnt + "=" + this.getUnitCnt()
            + "," + this.getFormat().getLotNumberNode().getName() + "=" + this.getLotNumber()
            + "," + this.getFormat().getOperationNode().getName() + "=" + this.mfgStp
            + "," + FieldType.KdfMonth + "=" + this.getFileMonth()
            + "," + FieldType.KdfDate + "=" + this.getFileDate()
            + "," + FieldType.TransferTime + "=" + this.getTransferTime()
            //			+ "," + FieldType.DataType + "=" + this.getFormat().getDataType()
            + "," + FieldType.DocCnt + "=" + this.fileLevelDocCnt
            + "," + FieldType.FileTime + "=" + this.formatTimeStr(this.getFileOpenTime());
        value += this.getFormat().getFileDocTimeKVStr() + "\n";
        if (this.isDebugMode()) {
            System.out.println(value);
        }
        return value;
    }

    @Override
    protected
        void logOpenFailureToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFOpenFailure,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            this.getFormat().getLotNumberNode().getName(), this.getLotNumber(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.KdfName, this.getFileName(),
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp,
            FieldType.SourceType, this.getFormat().getSourceType(),
            FieldType.CATEGORY, FieldType.CATEGORY_READER
        );
    }

    @Override
    protected
        void logRepeatFileToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFRepeat,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            this.getFormat().getLotNumberNode().getName(), this.getLotNumber(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.KdfName, this.getFileName(),
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp,
            FieldType.SourceType, this.getFormat().getSourceType(),
            FieldType.CATEGORY, FieldType.CATEGORY_READER
        );
    }

    @Override
    protected
        void logIoErrorToES(String func) {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.IOError,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            FieldType.Failure, func,
            this.getFormat().getLotNumberNode().getName(), this.getLotNumber(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.KdfName, this.getFileName(),
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp,
            FieldType.SourceType, this.getFormat().getSourceType(),
            FieldType.CATEGORY, FieldType.CATEGORY_READER
        );
    }

    /**
     *
     */
    @Override
    protected
        void logExceptionToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFException,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            this.getFormat().getLotNumberNode().getName(), this.getLotNumber(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.KdfName, this.getFileName(),
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp,
            FieldType.SourceType, this.getFormat().getSourceType(),
            FieldType.CATEGORY, FieldType.CATEGORY_READER
        );
    }

    private
        void logKDFDoneToES() {
        if (!this.getFormat().getDataType().equals(Config.DataTypes.SLT)) {
            System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%d,%s=%d,\n",
                FieldType.EventType, Config.EventType.KDFDone,
                FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
                FieldType.KdfName, this.getFileName(),
                this.getFormat().getLotNumberNode().getName(), this.getLotNumber(),
                this.getFormat().getOperationNode().getName(), this.mfgStp,
                FieldType.KdfMonth, this.getFileMonth(),
                FieldType.KdfDate, this.getFileDate(),
                FieldType.TransferTime, this.getTransferTime(),
                FieldType.DataType, this.getFormat().getDataType(),
                FieldType.SourceType, this.getFormat().getSourceType(),
                FieldType.CATEGORY, FieldType.CATEGORY_READER,
                FieldType.DocCnt, this.fileLevelDocCnt,
                FieldType.UnitCnt, this.getUnitCnt()
            );
        }
        else {
            System.out.printf("%s=%s,%s=%s,%s=%s,%s=%d,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%d,%s=%s,%s=%s,%s=%s\n",
                FieldType.EventType, Config.EventType.KDFDone,
                FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
                this.getFormat().getUnit().getUnitIdNode().getName(), this.getFormat().getUnit().getUnitIdNode().getXmlValue(),
                FieldType.UnitCnt, this.getUnitCnt(),
                this.getFormat().getLotNumberNode().getName(), this.getLotNumber(),
                this.getFormat().getOperationNode().getName(), this.mfgStp,
                FieldType.KdfMonth, this.getFileMonth(),
                FieldType.KdfDate, this.getFileDate(),
                FieldType.TransferTime, this.getTransferTime(),
                FieldType.DataType, this.getFormat().getDataType(),
                FieldType.DocCnt, this.fileLevelDocCnt,
                FieldType.KdfName, this.getFileName(),
                FieldType.SourceType, this.getFormat().getSourceType(),
                FieldType.CATEGORY, FieldType.CATEGORY_READER
            );
        }

    }

    /**
     *
     * @author guanghao
     */
    private
        class KdfParser extends KdfLoader {

        private final
            KDFCollectionFilter kdfFilter = null;
        private
            KDFInstanceTree tree = null;
        private
            File kdfFile = null;

        private
            KdfParser() {
            super(null);
        }

        @Override
        public
            boolean addFile(String fileName, String filter) throws Exception {
            boolean useLite = true;
            boolean skipFile = false;
            if (!skipFile) {
                if (useLite) {
                    SeekableKDFInputStream skis = new SeekableKDFInputStream(kdfFile);
                    tree = new LiteKDFInstanceTree(skis, kdfFilter);
                }
            }
            return true;
        }

        private
            KdfParser setKdfFile(File kdfFile) {
            this.kdfFile = kdfFile;
            this.tree = null;
            return this;
        }

        private
            KDFInstanceTree getTree() {
            return tree;
        }

    }

    private static
        void startTest(DataFormat dataFormat, Path path) {
        KdfReader reader = KdfReader.getInstance(dataFormat);
        if (path.toFile().length() < 100) {

            System.out.println("Error: " + path.toString());
            System.out.printf("Error: file size = %d error, less than 100 byte\n", path.toFile().length());
        }
        else {
            reader.loadFile(path.toFile());
        }

    }

    public static
        void main(String[] args) {
        new Config("config/dataformat.xml");
        for (DataFormat dataFormat : Config.dataFormats.values()) {
            if (dataFormat.isEnabled() && dataFormat.isKdfData()) {
                File rootFile = new File(dataFormat.getKdfPath());
                for (File dateFile : rootFile.listFiles()) {
                    System.out.println("*****************************************************************");
                    System.out.println("**********                                     ******************");
                    System.out.printf("**********        start to proceed %s        ******************\n", dataFormat.getDataType());
                    System.out.println("**********                                     ******************");
                    System.out.println("*****************************************************************");

                    try {
                        Files.walk(rootFile.toPath(), 3)
                            .filter(path -> path.toFile().isFile() && path.toFile().canRead())
                            .forEach(path -> startTest(dataFormat, path));
                    }
                    catch (IOException ex) {
                        Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    System.out.println("*****************************************************************");
                    System.out.println("**********                                     ******************");
                    System.out.printf("**********      complete to proceed %s       ******************\n", dataFormat.getDataType());
                    System.out.println("**********                                     ******************");
                    System.out.println("*****************************************************************");
                }
            }
        }

    }

}
