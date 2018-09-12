/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kdf.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.dom4j.Element;

/**
 *
 * @author ghfan
 */
public
	class DataFormat {

	private
		boolean logToFile = true;

	private
		int fieldValueLengthLimit = 8192;

	private
		String factory = null;
	private
		String sourceType = null;
	private
		boolean enabled = false;
	private
		String customer = null;
	private
		String xmlPath = null;
	private
		String kdfPath = null;
	private
		Config.DataTypes dataType = null;
	private
		boolean debugMode = false;

	private final
		ArrayList<String> filters = new ArrayList();
	private final
		ArrayList<String> selectors = new ArrayList();

	private
		boolean ignoreEmptyValueField = true;
	private
		int underLineCnt = -1;
	private
		int lotNumberIndex = -1;
	private
		int testerNumberIndex = -1;
	private
		int mfgStepIndex = -1;
	private
		int waferNumberIndex = -1;
	private
		int testCodeIndex = -1;
	private
		int siteIndex = -1;
	private
		int kdfMonthIndex = -1;
	private
		int testerTypeIndex = -1;
	private final
		ArrayList<Integer> fileOpenTimeIndex = new ArrayList();

	private final
		LinkedHashMap<String, XmlNode> lotHead = new LinkedHashMap();
	private final
		Unit unit = new Unit();

	private final
		HashMap<String, Bin> softBinDescs = new HashMap();
	private final
		HashMap<String, Bin> hardBinDescs = new HashMap();

	private final
		HashMap<String, String> testerTypes = new HashMap();

	// base class and sub class filters and selectors
	private final
		ArrayList<String> baseClassFilters = new ArrayList();
	private final
		ArrayList<String> baseClassSelectors = new ArrayList();
	private final
		ArrayList<String> subClassFilters = new ArrayList();
	private final
		ArrayList<String> subClassSelectors = new ArrayList();

	private final
		ArrayList<String> nodeTypeFilters = new ArrayList();
	private final
		ArrayList<String> nodeTypeSelectors = new ArrayList();

	public
		DataFormat(Element sourceData) {
		readData(sourceData);
	}

	private
		void readData(Element sourceData) {
		@SuppressWarnings("unused")

		List<Element> nodes = sourceData.elements();
		sourceType = sourceData.attributeValue("name");

		String fieldName = null;
		String fieldValue = null;

		if (!nodes.isEmpty()) {
			this.enabled = sourceData.attributeValue("enabled").trim().equals("1");
			for (Element node : nodes) {
				fieldName = node.getName().trim();
				fieldValue = node.getTextTrim();

				if ((!fieldName.equals("Head")) && (!fieldName.equals("Unit")) && fieldValue.isEmpty()) {
					continue;
				}

				switch (fieldName) {
					case "LogToFile":
						this.logToFile = fieldValue.equals("1");
						break;
					case "Factory":
						this.factory = fieldValue;
						break;
					case "Customer":
						this.customer = fieldValue;
						break;
					case "KDFPath":
						this.kdfPath = fieldValue;
						break;
					case "XmlPath":
						this.xmlPath = fieldValue;
						break;
					case "DataType":
						this.dataType = Config.DataTypes.valueOf(fieldValue);
						break;
					case "DebugMode":
						this.debugMode = fieldValue.equals("1");
						break;
					case "FilterOut":
						for (String filter : fieldValue.split(",")) {
							this.filters.add(filter.trim());
						}
						break;
					case "Selector":
						for (String filter : fieldValue.split(",")) {
							this.selectors.add(filter.trim());
						}
						break;
					case "TesterType":
						for (String filter : fieldValue.split(",")) {
							String[] temp = filter.split(":");
							if (temp.length != 2) {
								System.out.println("Error: incorrect selector found " + fieldValue);
								System.exit(1);
							}
							testerTypes.put(temp[0], temp[1]);
						}
						break;
					case "TesterTypeIndex":
						this.testerTypeIndex = Integer.valueOf(fieldValue);
						break;

					case "UnderLineCnt":
						this.underLineCnt = Integer.valueOf(fieldValue);
						break;
					case "LotNumberIndex":
						this.lotNumberIndex = Integer.valueOf(fieldValue);
						break;
					case "TesterNumberIndex":
						this.testerNumberIndex = Integer.valueOf(fieldValue);
						break;
					case "MfgStepIndex":
						this.mfgStepIndex = Integer.valueOf(fieldValue);
						break;
					case "TestCodeIndex":
						this.testCodeIndex = Integer.valueOf(fieldValue);
						break;
					case "WaferNumberIndex":
						this.waferNumberIndex = Integer.valueOf(fieldValue);
						break;
					case "SiteIndex":
						this.siteIndex = Integer.valueOf(fieldValue);
						break;
					case "KDFMonthIndex":
						this.kdfMonthIndex = Integer.valueOf(fieldValue);
						break;
					case "FieldValueLength":
						this.fieldValueLengthLimit = Integer.valueOf(fieldValue);
						break;
					case "FileOpenTimeIndex":
						int[] indices = new int[]{-1, -1, -1, -1};
						int i = 0;
						for (String var : fieldValue.split(",")) {
							indices[i++] = Integer.valueOf(var.trim());
						}
						Arrays.sort(indices);
						for (int j = 0; j != indices.length; j++) {
							if (indices[j] == -1) {
								continue;
							}
							this.fileOpenTimeIndex.add(indices[j]);
						}

						break;
					case "BaseClasFilter":
						for (String filter : fieldValue.split(",")) {
							this.baseClassFilters.add(filter.trim());
						}
						break;
					case "BaseClasSelector":
						for (String selector : fieldValue.split(",")) {
							this.baseClassSelectors.add(selector.trim());
						}
						break;
					case "SubClasFilter":
						for (String filter : fieldValue.split(",")) {
							this.subClassFilters.add(filter.trim());
						}
						break;
					case "SubClasSelector":
						for (String selector : fieldValue.split(",")) {
							this.subClassSelectors.add(selector.trim());
						}
						break;
					case "NodeTypeFilter":
						for (String filter : fieldValue.split(",")) {
							this.nodeTypeFilters.add(filter.trim());
						}
						break;
					case "NodeTypeSelector":
						for (String selector : fieldValue.split(",")) {
							this.nodeTypeSelectors.add(selector.trim());
						}
						break;
					default:
						System.out.printf("Error: this filed: %s is not supportted!\n", fieldName);
						System.exit(1);
						break;

				}
			}
		}
	}

	public
		boolean validate() {
		if (this.getCustomer() == null
			|| this.getFactory() == null
			|| this.getDataType() == null
			|| this.getFileOpenTimeIndex().isEmpty()
			|| this.getKdfMonthIndex() == -1
			|| this.getKdfPath() == null
			|| this.getLotNumberIndex() == -1
			|| this.getMfgStepIndex() == -1
			//			|| this.getTestCodeIndex() == -1
			|| this.getTesterNumberIndex() == -1
			|| this.getTesterTypes().isEmpty()
			|| this.getUnderLineCnt() == -1
			|| this.getTesterTypeIndex() == -1
			|| (this.getDataType().equals(Config.DataTypes.WaferSort) && this.getWaferNumberIndex() == -1)
			|| this.getXmlPath() == null) {
			this.printConfig();
			System.out.printf("SourceData %s validation result: failed\n", this.sourceType);
			return false;
		}
		else {
			if (this.isDebugMode()) {
				this.printConfig();
			}

			return true;
		}
	}

	private
		void printConfig() {
		System.out.printf("\nsourceData:%s, customer:%s, dataType:%s\n"
			+ "debugMode:%s, enabled:%s, xmlPath:%s, kdfPath:%s\n"
			+ "testerTypes:%s,"
			+ "filters:%s, selectors:%s\n"
			+ "fileOpenTimeIndex:%s\n"
			+ "kdfMonthIndex:%s, lotNumberIndex:%s, mfgStepIndex:%s\n"
			+ "siteIndex%s, testCodeIndex:%s, testerNumberIndex:%s,  "
			+ "underLineCnt:%s, waferNumberIndex:%s\n\n",
			this.sourceType, this.customer, this.dataType,
			this.debugMode, this.enabled, this.xmlPath, this.kdfPath,
			this.testerTypes,
			Arrays.toString(this.filters.toArray()), Arrays.toString(this.selectors.toArray()),
			Arrays.toString(this.fileOpenTimeIndex.toArray()),
			this.kdfMonthIndex, this.lotNumberIndex, this.mfgStepIndex,
			this.siteIndex, this.testCodeIndex, this.testerNumberIndex,
			this.underLineCnt, this.waferNumberIndex);

	}

	@Override
	public
		String toString() {
		String value = "";
		for (XmlNode node : this.lotHead.values()) {
			value += node.toString();
		}
		value += "\n\n";

		for (XmlNode node : this.unit.getNodes().values()) {
			value += node.toString();
		}
		return value;
	}

	public
		void printHeadInfo() {
		String value = "";
		for (XmlNode node : this.lotHead.values()) {
			if (node.getValue() != null) {
				value += node.toString();
			}
		}
		value += "\n";
		for (XmlNode node : this.lotHead.values()) {
			if (node.getValue() == null) {
				value += node.toString();
			}
		}

		System.out.println(value);
	}

	public
		void printUnitInfo() {
		unit.printinfo();
	}

	public
		void clearUnitData() {
		unit.clear();

	}

	public
		void resetHeadTimeFormat() {
		for (XmlNode node : this.lotHead.values()) {
			node.resetTime();
		}
	}

	public
		void resetUnitTimeFormat() {
		for (XmlNode node : this.unit.getNodes().values()) {
			node.resetTime();
		}
	}

	void calTestTime() {
		String startTime = null;
		String endTime = null;
		double testTime = 0;

		for (XmlNode node : this.unit.getNodes().values()) {
			if (node.isUnitTestTimeNode()
				&& node.getValue() != null) {
				return;
			}
			else if (node.isStartTime()) {
				if (node.getValue() == null) {
					return;
				}
				else {
					startTime = node.getValue();
				}
			}
			else if (node.isEndTime()) {
				if (node.getValue() == null) {
					return;
				}
				else {
					endTime = node.getValue();
				}
			}
		}
		if (startTime == null) {
			return;
		}
		if (startTime.startsWith("1")
			&& startTime.length() == 13
			&& endTime.startsWith("1")
			&& endTime.length() == 13) {
			testTime = Double.valueOf(Long.valueOf(endTime) - Long.valueOf(startTime)) / 1000.0;
			for (XmlNode node : this.unit.getNodes().values()) {
				if (node.isUnitTestTimeNode()) {
					node.setValue(String.valueOf(testTime));
				}
			}
		}
	}

	public
		HashMap<String, Bin> getSoftBinDescs() {
		return softBinDescs;
	}

	public
		HashMap<String, Bin> getHardBinDescs() {
		return hardBinDescs;
	}

	public
		void setBinDesc() {
		setHardBinDesc();
		setSoftBinDesc();
	}

	private
		void setHardBinDesc() {
		XmlNode hardBinNode = this.getUnit().getHardBinNode();
		if (hardBinNode.getValue() != null
			&& this.getUnit().getHardBinDesc() == null) {
			if (this.hardBinDescs.containsKey(hardBinNode.getValue())) {
				this.getUnit().setHardBinDesc(this.hardBinDescs.get(hardBinNode.getValue()).getBinDescription());
				this.getUnit().setFlag(this.hardBinDescs.get(hardBinNode.getValue()).getFlag());
			}
		}
	}

	private
		void setSoftBinDesc() {
		XmlNode softBinNode = this.getUnit().getSoftBinNode();
		if (softBinNode.getValue() != null
			&& this.getUnit().getSoftBinDesc() == null) {
			if (this.softBinDescs.containsKey(softBinNode.getValue())) {
				this.getUnit().setSoftBinDesc(this.softBinDescs.get(softBinNode.getValue()).getBinDescription());
				this.getUnit().setFlag(this.softBinDescs.get(softBinNode.getValue()).getFlag());
			}
		}
	}

	void clearAll() {
		for (XmlNode node : this.getLotHead().values()) {
			node.resetValue();
		}
		this.hardBinDescs.clear();
		this.softBinDescs.clear();
		unit.clear();

	}

	public
		String getLotHeadKVString() {
		String value = "\nEventType=test-item";
		for (XmlNode node : this.lotHead.values()) {
			if (!node.isEnabledLog()) {
				continue;
			}
			if (node.getValue() != null && (!node.getValue().isEmpty())) {
				value += "," + node.toKVString();
			}
		}
		if (!this.ignoreEmptyValueField) {
			for (XmlNode node : this.lotHead.values()) {
				if (!node.isEnabledLog()) {
					continue;
				}
				if (node.getValue() == null || node.getValue().isEmpty()) {
					value += "," + node.toKVString();
				}
			}
		}

		return value;
	}

	public
		String getUnitHeadKVString() {
		String value = "";
		for (XmlNode node : this.getUnit().getNodes().values()) {
			if (!node.isEnabledLog()) {
				continue;
			}
			if (node.getValue() != null && (!node.getValue().isEmpty())) {
				value += "," + node.toKVString();
			}
		}
		if (!this.ignoreEmptyValueField) {
			for (XmlNode node : this.lotHead.values()) {
				if (!node.isEnabledLog()) {
					continue;
				}
				if (node.getValue() == null || node.getValue().isEmpty()) {
					value += "," + node.toKVString();
				}
			}
		}
		value += "," + "softBinDesc=" + this.getUnit().getSoftBinDescValue();
		value += "," + "hardBinDesc=" + this.getUnit().getHardBinDescValue();
		value += "," + "binType=" + this.getUnit().getFlagIntValue();

		return value;

	}

	public
		XmlNode getLotStartTimeNode() {
		for (XmlNode node : this.getLotHead().values()) {
			if (node.isLotStartTime()) {
				return node;
			}
		}
		return null;
	}

	public
		XmlNode getLotOpenTimeNode() {
		for (XmlNode node : this.getLotHead().values()) {
			if (node.isLotOpenTime()) {
				return node;
			}
		}
		return null;

	}

	public
		int getUnderLineCnt() {
		return underLineCnt;
	}

	public
		String getCustomer() {
		return customer;
	}

	public
		ArrayList<String> getSelectors() {
		return selectors;
	}

	public
		ArrayList<String> getFilters() {
		return filters;
	}

	public
		boolean isDebugMode() {
		return debugMode;
	}

	public
		String getXmlPath() {
		return xmlPath;
	}

	public
		void print() {
		System.out.println(this.toString());
	}

	public
		Unit getUnit() {
		return unit;
	}

	public
		LinkedHashMap<String, XmlNode> getLotHead() {
		return lotHead;
	}

	public
		boolean isEnabled() {
		return enabled;
	}

	public
		String getKdfPath() {
		return kdfPath;
	}

	public
		Config.DataTypes getDataType() {
		return dataType;
	}

	public
		int getLotNumberIndex() {
		return lotNumberIndex;
	}

	public
		int getTesterNumberIndex() {
		return testerNumberIndex;
	}

	public
		int getMfgStepIndex() {
		return mfgStepIndex;
	}

	public
		int getWaferNumberIndex() {
		return waferNumberIndex;
	}

	public
		int getTestCodeIndex() {
		return testCodeIndex;
	}

	public
		int getSiteIndex() {
		return siteIndex;
	}

	public
		int getKdfMonthIndex() {
		return kdfMonthIndex;
	}

	public
		ArrayList<Integer> getFileOpenTimeIndex() {
		return fileOpenTimeIndex;
	}

	public
		HashMap<String, String> getTesterTypes() {
		return testerTypes;
	}

	public
		String getFactory() {
		return factory;
	}

	public
		int getTesterTypeIndex() {
		return testerTypeIndex;
	}

	public
		ArrayList<String> getBaseClassFilters() {
		return baseClassFilters;
	}

	public
		ArrayList<String> getBaseClassSelectors() {
		return baseClassSelectors;
	}

	public
		ArrayList<String> getSubClassFilters() {
		return subClassFilters;
	}

	public
		ArrayList<String> getSubClassSelectors() {
		return subClassSelectors;
	}

	public
		ArrayList<String> getNodeTypeFilters() {
		return nodeTypeFilters;
	}

	public
		ArrayList<String> getNodeTypeSelectors() {
		return nodeTypeSelectors;
	}

	public
		int getFieldValueLengthLimit() {
		return fieldValueLengthLimit;
	}

	public
		boolean isLogToFile() {
		return logToFile;
	}

	public
		String getSourceTypeName() {
		return sourceType;
	}

}
