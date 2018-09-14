/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kdf.reader;

import com.amd.kdf.KDFFieldData;
import com.amd.kdf.collection.KDFCollectionFilter;
import com.amd.kdf.collection.KDFInstanceTree;
import com.amd.kdf.collection.KDFInstanceTree.Node;
import com.amd.kdf.collection.LiteKDFInstanceTree;
import com.amd.kdf.gui.KdfLoader;
import com.amd.kdf.gui.KdfTypes;
import com.amd.kdf.io.SeekableKDFInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 *
 * @author ghfan
 */
public
	class Loader extends KdfLoader {

	private
		KDFCollectionFilter kdfFilter = null;
	private
		File file;
	private
		KDFInstanceTree tree;
	private
		DataFormat format = null;
	private
		Document document = DocumentHelper.createDocument();
	private
		Element root = document.addElement("UnitData");
	private
		Config.FailureCase failType = null;
	private
		int unitCnt = 0;

	private
		String nodeContent = null;
	private
		String nodeHead = null;

	//add pinRefs
	private
		HashMap<String, String> pinRefs = new HashMap();
	private
		HashMap<String, String> patternRefs = new HashMap();
	private
		HashMap<String, String> testDescRefs = new HashMap();
	private
		HashMap<String, ComponentHash> comHashRefs = new HashMap();

	private
		String flowResultField = null;
	private
		String subBaseClassField = null;
	private
		String fileOpenTime = "";
	long jobStartTime = 0;

	public
		Loader() {
		super(null);
	}

	private
		void clearRefs() {
		this.patternRefs.clear();
		this.pinRefs.clear();
		this.testDescRefs.clear();
		this.comHashRefs.clear();

	}

	public
		boolean loadFile(File file) {
		this.clearRefs();
		jobStartTime = System.currentTimeMillis();
		System.out.printf("%s: start proceed kdf %s\n", LocalDateTime.now(), file.getName());
		this.file = file;
		if (!this.validateFile(file)) {
			this.failType = Config.FailureCase.ValidationFailure;
			return false;
		}

		try {
			this.addFile("", "");
		}
		catch (Exception ex) {
			ex.printStackTrace();
			this.failType = Config.FailureCase.OpenFailure;
			return false;
		}

		return this.readKDF();
	}

	@Override
	public
		boolean addFile(String fileName, String filter) throws Exception {
		kdfFilter = null;
		boolean useLite = true;
		boolean skipFile = false;
		if (!skipFile) {
			if (useLite) {
				SeekableKDFInputStream skis = new SeekableKDFInputStream(file);
				tree = new LiteKDFInstanceTree(skis, kdfFilter);
			}
		}

		return true;
	}

	private
		boolean validateFile(File kdfFile) {
		failType = null;

		String kdfName = kdfFile.getName().toLowerCase();
		String names[] = kdfName.split("kdf.");
		if (names.length != 2) {
			return false;
		}
		if (!kdfFile.isFile()) {
			return false;
		}
		// the timestamp when the kdf is moved to this place
		if (names[names.length - 1].length() != 14 || (!names[names.length - 1].startsWith("20"))) {
			return false;
		}
		boolean skip = false;
		for (String filter : this.getFormat().getFilters()) {
			if (kdfName.contains(filter)) {
				skip = true;
				break;
			}
		}
		if (skip) {
			return false;
		}

		for (String selector : this.getFormat().getSelectors()) {
			if (!kdfName.contains(selector)) {
				skip = true;
				break;
			}
		}
		if (skip) {
			return false;
		}

		names = kdfFile.getName().split("_");
		if (names.length != this.getFormat().getUnderLineCnt()) {
			System.out.println("Skip this kdf since underline cnt is not " + this.getFormat().getUnderLineCnt());
			return false;
		}
		return true;
	}

	/**
	 * read root
	 */
	public
		void readRoot() {
		long startTime = System.currentTimeMillis();
		Node[] roots = tree.getRoots();
		if (roots != null && roots.length > 0) {
			for (Node root : roots) {
				System.out.println("Root name is " + root.getName());
				for (XmlNode xmlNode : format.getLotHead().values()) {
					for (String fieldName : xmlNode.getFieldNames()) {
						if (root.containsKey(fieldName)) {
							xmlNode.setValue(root.get(fieldName).toString());
						}
					}
				}
			}
			System.out.println("reading root time is : " + (System.currentTimeMillis() - startTime));
		}
	}

	public
		void readRootNode(String nodeType) {
		long startTime = System.currentTimeMillis();
		Node[] nodes = tree.getNodes(nodeType);
		if (nodes != null && nodes.length > 0) {
			for (Node node : nodes) {
				for (XmlNode xmlNode : format.getLotHead().values()) {
					for (String fieldName : xmlNode.getFieldNames()) {
						if (node.containsKey(fieldName)) {
							xmlNode.setValue(node.get(fieldName).toString());
						}
					}
				}
			}
			System.out.printf("reading %s time is : %d\n", nodeType, (System.currentTimeMillis() - startTime));
		}
	}

	/**
	 * read component
	 */
	public
		void readComponement() {
		long startTime = System.currentTimeMillis();
		Node[] coms = tree.getNodes(KdfTypes.KDF_RT_COMPONENT);
		if (coms != null && coms.length > 0) {
			for (Node component : coms) {
				for (XmlNode xmlNode : format.getLotHead().values()) {
					for (String fieldName : xmlNode.getFieldNames()) {
						if (component.containsKey("componentType")
							&& component.get("componentType").toString().equals(fieldName)
							&& component.containsKey("componentId")) {
							xmlNode.setValue(component.get("componentId").toString());
						}
					}
				}
			}
			System.out.println("reading component time is : " + (System.currentTimeMillis() - startTime));
		}
	}

	/**
	 * read Omi
	 */
	private
		void readOMI() {
		long startTime = System.currentTimeMillis();
		Node[] omis = tree.getNodes(KdfTypes.KDF_RT_OMI);
		if (omis != null && omis.length > 0) {
			for (Node omi : omis) {
				for (XmlNode xmlNode : format.getLotHead().values()) {
					for (String fieldName : xmlNode.getFieldNames()) {
						if (omi.get("name") != null
							&& omi.get("name").toString().equals(fieldName)
							&& omi.containsKey("value")) {
							xmlNode.setValue(omi.get("value").toString());
						}
					}
				}
			}
			System.out.println("reading omi time is : " + (System.currentTimeMillis() - startTime));
		}
	}

	/**
	 * read the base class the sub class the class is stored in the <testDescs>
	 * the format is subClass=subClassName,baseClass=baseClassName
	 */
	private
		void readTestDesc() {
		String subClassKey = "subClass";
		String baseClassKey = "baseClass";
		String testDescIdKey = "testDescId";
		long startTime = System.currentTimeMillis();
		Node[] descs = tree.getNodes(KdfTypes.KDF_RT_TESTDESC);
		if (descs != null && descs.length > 0) {
			for (Node des : descs) {
				String value = "";
				String subClassName = des.get(subClassKey).toString().trim();
				String baseClassName = des.get(baseClassKey).toString().trim();
				if (subClassName != null && (!subClassName.isEmpty())) {
					value += Config.subClass + subClassName;
					if (baseClassName != null && (!baseClassName.isEmpty())) {
						value += "," + Config.baseClass + baseClassName;
					}
				}
				else {
					if (baseClassName != null && (!baseClassName.isEmpty())) {
						value += Config.baseClass + baseClassName;
					}
				}

				if (des.containsKey(testDescIdKey) && value.length() != 0) {
					this.testDescRefs.put(des.get(testDescIdKey).toString(), value);
				}
			}
			System.out.println("reading test desc time is : " + (System.currentTimeMillis() - startTime));
		}
	}

	private
		void readBinDesc() {
		long startTime = System.currentTimeMillis();
		Node[] descs = tree.getNodes(KdfTypes.KDF_RT_BINDESC);
		if (descs != null && descs.length > 0) {
			for (Node binDesc : descs) {

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
							binDescription = o.toString();
						}
					}
					else if (key.equalsIgnoreCase("binDescFlag")) {
						flag = binDesc.get(key).getValue().toString();
					}
				}

				if (binType.equals("83") || binType.equals("S")) {
					if (binNumber != null
						&& flag != null) {
						format.getSoftBinDescs().putIfAbsent(binNumber, new Bin(binNumber, binDescription, flag));
					}

				}
				else if (binType.equals("72") || binType.equals("H")) {
					if (binNumber != null
						&& flag != null) {
						format.getHardBinDescs().putIfAbsent(binNumber, new Bin(binNumber, binDescription, flag));
					}

				}

			}
			System.out.println("reading binDesc node time is : " + (System.currentTimeMillis() - startTime));
		}
	}

	private
		void readRooT_Refs(String kdfType, HashMap<String, String> data) {
		long startTime = System.currentTimeMillis();
		Node[] miscs = tree.getNodes(kdfType);
		if (miscs != null && miscs.length > 0) {
			for (Node misc : miscs) {
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
					data.put(key, value.trim());
				}
			}
			System.out.println("reading " + kdfType + " time is " + (System.currentTimeMillis() - startTime));
		}
	}

	private
		void readRoot_ComHash() {
		long startTime = System.currentTimeMillis();
		Node[] comHashRefs = tree.getNodes(KdfTypes.KDF_RT_COMPGROUPDESC);
		if (comHashRefs != null && comHashRefs.length > 0) {
			for (Node comHashNode : comHashRefs) {
				KDFFieldData comNameField = comHashNode.get("componentName");
				if (!comNameField.isNull()) {
					String comName = comNameField.getValue().toString();
					String comClass = comHashNode.get("componentClass").getValue().toString();
					String comHash = comHashNode.get("componentHash").getValue().toString();
					String comInst = comHashNode.get("componentInst").getValue().toString();
					this.comHashRefs.put(comHash, new ComponentHash(comClass, comName));
				}

			}
		}

	}

	private
		boolean readKDF() {
		unitCnt = 0;
		this.fileOpenTime = "";
		String waferNumber = "";

		format.clearAll();
		Node[] units = tree.getNodes(KdfTypes.KDF_RT_UNIT);
		if (units == null || units.length == 0) {
			System.out.println("Empty Uni Record: no unit record found in this kdf...");
			return true;
		}
		else {
			unitCnt = units.length;
		}

		if (format.getDataType().equals(Config.DataTypes.WaferSort)) {
			Node[] wafers = tree.getNodes(KdfTypes.KDF_RT_WAFER);
			if (wafers != null && wafers.length > 1) {
				System.err.println("Error: we expect only one wafer in one kdf file...");
				this.failType = Config.FailureCase.Exception;
				return false;
			}
			waferNumber = Integer.valueOf(this.file.getName().split("_")[this.getFormat().getWaferNumberIndex()]).toString();
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
		if (format.getDataType().equals(Config.DataTypes.WaferSort)) {
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
		format.resetHeadTimeFormat();
		if (format.isDebugMode()) {
			format.printHeadInfo();
		}
		writeHeadData();

		File testLogFile = new File(this.file.getAbsolutePath() + ".test_item.log");
		try {
			Files.deleteIfExists(testLogFile.toPath());
		}
		catch (IOException ex) {
			Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
		}
		String testItemHead = format.getLotHeadKVString() + "," + "file_date=" + this.formatFileOpenTime();

		readBinDesc();
		System.out.println("opening kdf time is : " + (System.currentTimeMillis() - jobStartTime));

		System.out.println("total unit cnt is: " + units.length);
		int unitNo = 0;
		StringBuilder dataContent = new StringBuilder();
		for (Node unit : units) {
			unitNo++;
			if (unitNo > 800) {
				break;
			}
			dataContent.setLength(0);
			format.clearUnitData();
			this.readUnit(unit);
			this.readUnitBin(unit);
			this.readUnitSerialNumber(unit);
			this.readOldKDFBinDesc(unit);

			format.calTestTime();
			format.resetUnitTimeFormat();
			format.setBinDesc();
			readFTSLTXY(waferNumber);

			/**
			 * print and log unit data
			 */
			if (format.isDebugMode()) {
				format.printUnitInfo();
			}
			writeUnitData();
			String unitDataHead = testItemHead + format.getUnitHeadKVString() + getSlaveNodeKVString();
			System.out.println(unitDataHead);

			for (Node item : unit.getChildren()) {

				String nodeName = item.getName();
				if (nodeName.equals(KdfTypes.KDF_RT_SERIAL)
					|| nodeName.equals(KdfTypes.KDF_RT_BIN)
					|| nodeName.equals(KdfTypes.KDF_RT_BINDESC)) {
					continue;
				}
				this.nodeHead = unitDataHead;
				flowResultField = "";
				subBaseClassField = "";
				String nodeKVString = printNodeInfo(item, 0);
				dataContent.append(nodeKVString);
				// log for debugging
				if (this.getFormat().isDebugMode()) {
					System.out.println(nodeKVString);
				}

			}
			if (this.getFormat().isLogToFile()) {
				try {
					Files.write(testLogFile.toPath(), dataContent.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

				}
				catch (IOException ex) {
					Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

		}

		this.closeFile(this.file.getParent());
		System.out.printf("%s: successed to proceed kdf %s\n", LocalDateTime.now(), file.getName());
		System.out.println("total kdf reading time is : " + (System.currentTimeMillis() - jobStartTime));
		this.tree = null;
		return true;
	}

	/**
	 * H800A0010041704 read waferLotNumber, waferNumber, x and y from unit id
	 *
	 * @param waferNumber
	 */
	private
		void readFTSLTXY(String waferNumber) {
		//H800A0010041704
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

	private
		void readUnit(Node unit) {
		for (XmlNode xmlNode : format.getUnit().getNodes().values()) {
			for (String fieldName : xmlNode.getFieldNames()) {
				if (unit.containsKey(fieldName)) {
					xmlNode.setValue(unit.get(fieldName).toString());
				}
			}
		}
	}

	private
		void readUnitBin(Node unit) {
		Node[] bins = unit.getChildren(KdfTypes.KDF_RT_BIN);
		if (bins != null && bins.length > 0) {
			for (Node bin : bins) {
				for (XmlNode xmlNode : format.getUnit().getNodes().values()) {
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
		void readUnitSerialNumber(Node unit) {
		int dieCnt = 0;
		Node[] serialNumbers = unit.getChildren(KdfTypes.KDF_RT_SERIAL);
		if (serialNumbers != null && serialNumbers.length > 0) {
			for (Node serialNumber : serialNumbers) {
				for (XmlNode xmlNode : format.getUnit().getNodes().values()) {
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
								if (!serialNumber.get(fieldName).isNull()) {
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
		void readOldKDFBinDesc(Node unit) {
		// very very old kdf file store bin info in unit
		Node[] binDescs = unit.getChildren(KdfTypes.KDF_RT_BINDESC);
		if (binDescs != null && binDescs.length > 0) {
			for (Node binDesc : binDescs) {
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
							binDescription = o.toString();
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
						format.getUnit().setSoftBinDesc(bin.getBinDescription());
						format.getUnit().getSoftBinNode().setValue(binNumber);
						format.getUnit().setFlag(bin.getFlag());
					} //HardBin
					else if (binType.equals("72") || binType.equals("H")) {
						format.getUnit().setHardBinDesc(binDescription);
						format.getUnit().getHardBinNode().setValue(binNumber);
						format.getUnit().setFlag(bin.getFlag());
					}
				}
			}
		}
	}

	private
		String printNodeInfo(Node node, int level) {
		String formatString = "";
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

		if (nodeName.equals(KdfTypes.KDF_RT_TEST) && node.getParentName().equals(KdfTypes.KDF_RT_FLOW)) {
			//skip all the test since test is only for the test descriptions in 93k kdf
			if (this.format.isDebugMode()) {
				System.out.println("Skip the Test Node : " + node);
			}
			String idClass = node.get("testDescId").toString();
			if (idClass != null && this.testDescRefs.containsKey(idClass)) {
				subBaseClassField = this.testDescRefs.get(idClass);

				// return immediately if validate failed
				if (!validateBaseSubClass(subBaseClassField)) {
					return formatString;
				}

			}
		}
		else if (!validateNodeType(nodeName)) {
			// skip those nodes
			if (this.format.isDebugMode()) {
				System.out.println("Skip Node: " + node);
			}
			return formatString;
		}
		else {
			/**
			 * handle the current node here
			 */
			String formatNodeString = space + formateNode(node) + "\n";

			if (validateForamtString(formatNodeString)) {
				formatString += formatNodeString;
			}
			else {
				System.out.println("FATAL Error: node format string validation failed");
				System.out.println("FATAL Error: es can not read this node string\n");
				System.out.println(node);
				System.out.printf("formatString is:%s", formatNodeString);
				System.exit(1);
			}

		}
		/**
		 * handle the child node here
		 */
		for (Node item : node.getChildren()) {
			formatString += printNodeInfo(item, level + 1);
		}
		return formatString;
	}

	/**
	 * method to check if the logging is disabled to this node
	 *
	 * @return
	 */
	private
		boolean validateNodeType(String nodeType) {
		if ((!this.getFormat().getNodeTypeFilters().isEmpty()) && this.getFormat().getNodeTypeFilters().contains(nodeType)) {
			return false;
		}
		if ((!this.getFormat().getNodeTypeSelectors().isEmpty()) && (!this.getFormat().getNodeTypeSelectors().contains(nodeType))) {
			return false;
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
		boolean validateBaseSubClass(String baseSubClass) {
		//selector and 
		String[] names = baseSubClass.split(",");
		String subClassName = null;
		String baseClassName = null;
		// subClass and baseClass 
		if (names.length == 2) {
			subClassName = names[0].split("=")[1];
			baseClassName = names[1].split("=")[1];
		}
		else {
			if (baseSubClass.startsWith(Config.baseClass)) {
				baseClassName = baseSubClass.split("=")[1];
			}
			else {
				subClassName = baseSubClass.split("=")[1];
			}
		}

		// validate the base class first
		if (!this.getFormat().getBaseClassFilters().isEmpty()) {
			// filter out
			if (baseClassName != null && this.getFormat().getBaseClassFilters().contains(baseClassName)) {
				return false;
			}
		}
		if (!this.getFormat().getBaseClassSelectors().isEmpty()) {
			// selector
			if (baseClassName != null && (!this.getFormat().getBaseClassSelectors().contains(baseClassName))) {
				return false;
			}
		}

		// validate the sub class
		if (!this.getFormat().getSubClassFilters().isEmpty()) {
			// filter out
			if (subClassName != null && this.getFormat().getSubClassFilters().contains(subClassName)) {
				return false;
			}
		}

		if (!this.getFormat().getSubClassSelectors().isEmpty()) {
			// selector
			if (subClassName != null && (!this.getFormat().getSubClassSelectors().contains(subClassName))) {
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
		boolean validateForamtString(String formatString) {
		int length = formatString.length();
		int commaCnt = 0;
		int equalityCnt = 0;
		while (length-- > 0) {
			if (formatString.charAt(length) == ',') {
				commaCnt++;
			}
			else if (formatString.charAt(length) == '=') {
				equalityCnt++;
			}
		}
		return (commaCnt + 1 == equalityCnt);

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
		String formateNode(Node node) {
		String value = "";
		if (node.getName().equals(RecordTypes.KDF_RT_ULSD)) {
			return this.formatULSDNode(node);
		}
		else {
			//add head for some nodes
			if (!this.nodeHead.isEmpty()) {
				value += this.nodeHead;
			}

			if (node.getName().equals(KdfTypes.KDF_RT_EVALUATION)) {
				value += "," + this.flowResultField;
			}
			if (!this.subBaseClassField.isEmpty()) {
				value += "," + this.subBaseClassField;
			}

			String[] fields = node.toString().split(":");
			if (fields.length > 3) {
				value += "," + fields[0] + "=" + fields[1].split(",")[0].trim();
			}

			// find the data field
			fields = node.toString().split("Data:");
			fields = fields[fields.length - 1].split("\\[");
			if (fields.length == 2) {
				fields = fields[fields.length - 1].split(",");
				int length = fields.length;
				int i = 0;
				for (String field : fields) {
					i++;

					String[] names = field.trim().split("=");
					if (names.length != 2 || names[0].equals("") || names[1].equals("")) {
						continue;
					}

					// the last field
					if (i == length) {
						field = field.substring(0, field.length() - 1);
						names = field.trim().split("=");

						if (names.length != 2) {
							System.out.println("Error field: " + field);
							continue;
						}

						// validate the field String length for text limit 32766
						if (names[1].length() > this.format.getFieldValueLengthLimit()) {
							continue;
						}
					}

					if (names[0].trim().equals(FieldType.PinRefPtr)) {
						String pinName = this.pinRefs.get(names[1].trim());
						if (pinName == null) {
							System.out.println("no pin for this pinRef:" + names[1]);
						}
						field = "pinName=" + pinName;
					}
					else if (names[0].trim().equals(FieldType.PatternId)) {
						String pattern = this.patternRefs.get(names[1].trim());
						if (pattern != null) {
							field = "patterns=" + pattern;
						}
						else {
							continue;
						}
					}

					value += "," + field.trim();

				}
				if (node.getName().equals(KdfTypes.KDF_RT_FLOW)) {
					// all the child nodes whose parent node is flow, add flow context and flow result into sub nodes
					this.nodeHead += "," + fields[2];
					flowResultField = "flowResult=" + fields[0].split("=")[1];

				}
			}
			else {
				System.out.println("\nWarning: need to improve this method for below node");
				System.out.println(node);
			}

			return value;
		}

	}

	private
		String formatField(String field) {
		int length = field.length();
		String value = "";
		int i = 0;
		while (i++ != length) {
			char chr = field.charAt(i);
			if (chr != ',' && chr != '=' && chr != 32) {
				value += chr;
			}
		}
		return value;

	}

	private
		String formatULSDNode(Node node) {
		//Type: ULSD, Parent Type: Unit, Data: [hostName=atdtsocket204.higon.com, queryTimeout=10000, totalQueryTime=2.045, recvTime=2.045, queriedTests=[[name='FUSE.LOG_MODE=SOFT'], [segmentName='Top.DIE3.DIE3FuseRam'], [segmentName='Top.DIE3.DIE3FuseRam'].bitValues]], portNumber=9999, timeout=0, queryType=3, connectTime=6.935, connectTimeout=5000, networkError=0, results=[[FT, DNXSP3XX1A0XFP0, F3, AANFNP, 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001100010110000000000000000000000000000000000000000000000000000000000000000000000000000000000000001100010110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000100000000000000000000000000000000000000000000000000000000100001000000000000000000000000000000000000000000000000000000000001000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001010011100000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001100010110000000000000000000000000000000000000000000000000000000010000010000000000000000000000000000000000000000000000000000000011100000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000010100001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001001000010100100010010010001000010010000100101101110010110111000100100111010010011001001001001110111111100110111110101110101111110101111110001110110111010110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001101000111010000000000000000000000000000000000000000000000000000001001100101011011010101011000100010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000101000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001110110110000000000000000000000000101011110000000000000000000000000000011000101001000000000000000000000000011001001000000000000000000000000000011000100101000000000000000000000000010110010100110010000001100100000110001101010001111111000101111101010101111001110101110101101101101110111101101001111101100010010101001010000101000110110100111111110000000000000000000000000000000000000000000000000000000000000000000000000101111010110101111001010101111010000101111010010101110100110101110100101100000001111100000000101100000001001100000001011011110110111011110110100001110100000001110011000001110011010001110011010001101010010001101010010110111110101101111111001101110100010111100000110111101100010111100101100111101001100111101011110111101011010111100111110111101011000111100011000111101011110111100100110111101000000111101010110111100110000111100011000111100101010111100100110111100100100111100011010111100010110111100100000111100010110111100101100111100011100111000000011111000000110110010001111110110010101110111110011110111011001110011101001110111000000110110001001111000000000110010111101110101001111110111010011101100110100110110001000110001010000110100000010110011111100110101111010110110001010110100101100110110010100110010111010110101110110110100001100110110011100110101010110110110001000110110010110110110010010110101110010110110001100110100111100110110011100110101011000110110000100110110010100110101100110110100111010110101011010110101001000110101001000110100110100110100110000110101000000110100101100110101001010110100110010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100110010101001010100101110010000000000000000000000000000000000001111000000100001110010001001010100000000000000000000000001000011000010100000000000000000000000000000000000010010101101100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000111111111111111111111110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010101011011001100100111010110100111101000000110001110010000000000000000000000000000000111]], status=Success]
		String value = "Type=" + RecordTypes.KDF_RT_ULSD;
		String names[] = node.toString().split(",");
		int size = names.length;
		while (size-- > 0) {
			if (!names[size].contains("[")) {
				if (names[size].contains("=")) {
					value += "," + names[size].trim().replaceAll("]", "");
				}
			}
			else {
//				String[] temp = names[size].split("\\[");
//				if(temp.length > 1) {
//					if(temp[temp.length - 1].contains("=")) {
//						value += "," + (temp[temp.length - 1].replaceAll("]","")).replace(',', '-').trim();
//					}
//				}
			}
		}
		return value;

	}

	private
		String genericFormat(Node node) {
		String value = "";
		String names[] = node.toString().split(",");
		int size = names.length;
		while (size-- > 0) {
			String content = names[size];
			if (!content.contains("=")) {
				String[] field = content.split(":");
				if (field.length == 2) {
					value += field[0].replace('[', ' ').replace(']', ' ').trim() + "=" + field[1].replace('[', ' ').replace(']', ' ').trim();
				}
			}
			else {
				//

			}

		}
		return value;

	}

	/**
	 * this method is to generate the kv string for the slave die format:
	 * ,componentName=unitID
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
		void writeHeadData() {
		this.fileOpenTime = "";
		String[] names = this.file.getName().split("_");
		// set file open time
		for (int index : format.getFileOpenTimeIndex()) {
			if (names[index].contains(".")) {
				names[index] = names[index].substring(0, names[index].indexOf('.'));
			}
			fileOpenTime += names[index];
		}
		if (fileOpenTime.length() == 12) {
			fileOpenTime = "20" + fileOpenTime;
		}
		if (fileOpenTime.length() != 14) {
			System.out.println("FATAL Error: failed to get file open time from the file name");
			System.exit(1);
		}

		// set kdf month
		XmlNode lotStartTimeNode = format.getLotStartTimeNode();
		XmlNode lotOpenTimeNode = format.getLotOpenTimeNode();
		System.out.printf("fileOpenTime = %s, lotStartTime = %s, lotOpenTime = %s\n", fileOpenTime, lotStartTimeNode.getValue(), lotOpenTimeNode.getValue());

		if (lotStartTimeNode != null && lotStartTimeNode.getValue() != null) {
			if (lotOpenTimeNode != null && lotOpenTimeNode.getValue() != null) {
				// this is the best case
			}
			else {
				// set lotOpenTimeNode = lotStartTime
				lotOpenTimeNode.forceValueTo(lotStartTimeNode.getValue());
			}
		}
		else {

			if (lotOpenTimeNode != null && lotOpenTimeNode.getValue() != null) {
				lotStartTimeNode.forceValueTo(lotOpenTimeNode.getValue());
			}
			else {
				lotOpenTimeNode.forceValueTo(fileOpenTime);
				lotStartTimeNode.forceValueTo(fileOpenTime);
			}
		}

		if (lotStartTimeNode.getValue().compareTo(fileOpenTime) < 0) {
			lotStartTimeNode.forceValueTo(fileOpenTime);
		}

		root.clearContent();
		for (XmlNode node : format.getLotHead().values()) {
			root.addElement(node.getName()).setText(node.getXmlValue());

		}
		root.addElement("src_to_xml_time").setText("1");
		root.addElement("xml_file_generated_time").setText(LocalDateTime.now().toString());
		root.addElement("kdf_file_name").setText(this.file.getName());

	}

	private
		void writeUnitData() {
		Element unit = root.addElement("Unit");
		for (XmlNode node : format.getUnit().getNodes().values()) {
			unit.addElement(node.getName()).setText(node.getXmlValue());
		}
		//add binDesc
		unit.addElement("soft_bin_desc").setText(format.getUnit().getSoftBinDescValue());
		unit.addElement("hard_bin_desc").setText(format.getUnit().getHardBinDescValue());
		unit.addElement("test_result").setText(format.getUnit().getFlagValue());

	}

	private
		String getLotDate() {
		String[] names = this.file.getName().split("_");
		//String factoryName = names[0];
		String lotDate = "20" + names[7].substring(0, 2) + "-" + names[7].substring(2, 4) + "-" + names[7].substring(4, 6);
		return lotDate;
	}

	private
		void closeFile(String xmlFilePath) {
		xmlFilePath = format.getXmlPath();

		String[] names = this.file.getName().split("_");
		String factoryName = format.getFactory();
		String testerNumber = names[format.getTesterNumberIndex()];
		String lotNumber = names[format.getLotNumberIndex()];
		String mfgStep = names[format.getMfgStepIndex()];
		String kdfMonth = names[format.getKdfMonthIndex()];
		if (kdfMonth.length() == 4) {
			//0604
			kdfMonth = "20" + kdfMonth;
		}
		else if (kdfMonth.length() == 6) {
			//180604
			kdfMonth = "20" + kdfMonth.substring(0, 4);
		}
		String xmlDate = LocalDate.now().format(DateTimeFormatter.ofPattern("uuuuMMdd"));
		String platform = format.getTesterTypes().get(names[format.getTesterTypeIndex()]);
		/**
		 * XmlNode lotStartTimeNode = format.getLotStartTimeNode(); if
		 * (lotStartTimeNode != null && lotStartTimeNode.getValue() != null) {
		 * if (lotStartTimeNode.getValue().length() == 14 &&
		 * lotStartTimeNode.getValue().startsWith("20")) { lotOpenTime =
		 * lotStartTimeNode.getValue(); } }
		 *
		 * XmlNode lotOpenTimeNode = format.getLotOpenTimeNode(); if
		 * (lotOpenTimeNode != null && lotOpenTimeNode.getValue() != null) { if
		 * (lotOpenTimeNode.getValue().length() == 14 &&
		 * lotOpenTimeNode.getValue().startsWith("20")) { lotOpenTime =
		 * lotOpenTimeNode.getValue(); } }
		 *
		 */

		String filePath = format.getXmlPath()
			+ "/" + factoryName
			+ "/" + format.getCustomer()
			+ "/" + format.getDataType()
			+ "/" + platform
			+ "/kdf"
			+ "/" + xmlDate
			+ "/" + testerNumber
			+ "/" + kdfMonth
			+ "/" + lotNumber
			+ "/" + mfgStep
			+ "/" + format.getLotOpenTimeNode().getValue();;

		File unitFile = new File(filePath + "/" + "UnitData_" + this.file.getName() + ".xml");
		File tempUunitFile = new File(filePath + "/" + "UnitData_" + this.file.getName() + ".xml.tmp");

		try {
			Files.deleteIfExists(tempUunitFile.toPath());
			if ((!tempUunitFile.getParentFile().exists())) {
				if (tempUunitFile.getParentFile().mkdirs()) {
					//System.out.println("successed to generate file: " + tempUunitFile.getParentFile().getAbsolutePath());
				}
				else {
					System.out.println("failed to generate file: " + tempUunitFile.getParentFile().getAbsolutePath());
				}
			}
			if (tempUunitFile.getParentFile().exists()) {
				if (tempUunitFile.createNewFile()) {
					OutputFormat goodFormat = OutputFormat.createPrettyPrint();
					goodFormat.setIndent(true);
					goodFormat.setIndentSize(2);
					XMLWriter writer = new XMLWriter(new FileWriter(tempUunitFile.getAbsolutePath()), goodFormat);
					writer.write(document);
					writer.close();
					writer = null;
					if (tempUunitFile.renameTo(unitFile)) {
						System.out.println("Successed to generate xml file");
						if (Config.renameKDF) {
							File kdfedFile = new File(this.file.getAbsolutePath() + "." + Config.KdfRename.done);
							if (this.file.renameTo(kdfedFile)) {
								System.out.println("Successed to rename the kdf file");
								System.out.printf("EventType=%s,DoneTime=%s,KDFName=%s,UnitCnt=%d,LotNumber=%s,MFGStep=%s,KDFMonth=%s,xmlDate=%s,TransferTime=%s\n",
									"KDFDone",
									ZonedDateTime.now().toOffsetDateTime(),
									file.getName().split("kdf.")[0] + "kdf",
									unitCnt,
									lotNumber,
									mfgStep,
									kdfMonth,
									xmlDate,
									file.getName().split("kdf.")[1]
								);
							}
							else {
								System.out.println("Failed to rename the kdf file");
							}
						}

					}
					else {
						System.out.println("failed to generate xml file");
					}
				}
			}
			else {
				System.out.println("failed to generate the xml file since failed to make dirs");
			}

		}
		catch (IOException ex) {
			Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
		}
		this.root.clearContent();

	}

	private
		String formatFileOpenTime() {
		return this.fileOpenTime.substring(0, 4) + "-" + this.fileOpenTime.substring(4, 6) + "-" + this.fileOpenTime.substring(6, 8)
			+ "T" + this.fileOpenTime.substring(8, 10) + ":" + this.fileOpenTime.substring(10, 12) + ":"
			+ this.fileOpenTime.substring(12, 14) + ".00+08:00";
	}

	/**
	 * judge the kdf type never change the judge order!!!
	 */
	/**
	 * public boolean getKDFType() { xmlDate =
	 * LocalDate.now().format(DateTimeFormatter.ofPattern("uuuuMMdd")); String[]
	 * names = this.file.getName().split("_"); if
	 * (names[0].equals(KDFType.KDF_GFDRS) && names.length == 11) { factoryName
	 * = names[0]; testerNumber = names[3].toLowerCase(); lotNumber = names[4];
	 * mfgStep = names[5]; kdfMonth = "20" + names[9].substring(0, 4);
	 * lotOpenTime = "20" + names[9] + names[10]; fileOpenTime = lotOpenTime;
	 *
	 * platform = names[2]; if (platform.equals("P")) { platform = "93k_data"; }
	 * else if (platform.equals("S")) { platform = "sapphire_data"; } } else if
	 * (names[0].equals(KDFType.KDF_SUZ) && names.length == KDFType.KDF_93K_FT)
	 * { factoryName = names[0]; testerNumber = names[3].toLowerCase();
	 * lotNumber = names[4]; mfgStep = names[5]; kdfMonth = "20" +
	 * names[7].substring(0, 4); lotOpenTime = "20" + names[7] +
	 * names[8].substring(0, 6); fileOpenTime = lotOpenTime;
	 *
	 * platform = names[2]; if (platform.equals("P")) { platform = "93k_data"; }
	 * else if (platform.equals("S")) { platform = "sapphire_data"; } } else if
	 * (names.length == KDFType.KDF_ASLT && names[KDFType.KDF_ASLT -
	 * 3].toLowerCase().startsWith(KDFType.KDF_SUZASLT_Tester)) { factoryName =
	 * KDFType.KDF_SUZ; testerNumber = names[KDFType.KDF_ASLT -
	 * 3].toLowerCase(); lotNumber = names[0]; mfgStep = names[1]; kdfMonth =
	 * names[6].substring(0, 4); lotOpenTime = names[6] + names[7].substring(0,
	 * 6); fileOpenTime = lotOpenTime;
	 *
	 * platform = KDFType.KDF_SUZASLT_Tester; } else {
	 * System.out.println("unsupportted kdf type found"); return false; } return
	 * true; }
	 *
	 */
	public
		DataFormat getFormat() {
		return format;
	}

	public
		void setTree(KDFInstanceTree tree) {
		this.tree = tree;
	}

	public
		void setFormat(DataFormat format) {
		this.format = format;
	}

	public
		Config.FailureCase getFailType() {
		return failType;
	}

	public
		boolean chooseFormat(File file) {
		for (DataFormat format : Config.dataFormats.values()) {
			if (!format.isEnabled()) {
				continue;
			}
			String sourceType = file.getName().split("_")[format.getSourceTypeIndex()];
			if (sourceType.equals(format.getSourceType()) || sourceType.toLowerCase().startsWith(format.getSourceType().toLowerCase())) {
				this.setFormat(format);
				return true;
			}
		}
		return false;
	}

	public static
		void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		new Config("config/dataformat.xml");
		Loader loader = new Loader();

		File testDataFile = new File("./testdata/KDF");
		for (File stageFile : testDataFile.listFiles()) {
			if (stageFile.isDirectory()) {
				for (File file : stageFile.listFiles()) {
					if (loader.chooseFormat(file)) {
						loader.loadFile(file);
					}
				}
			}
		}

		System.out.println("total time = " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
	}

}
