/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.dom4j.Element;
import org.himalayas.filereader.kdf.Bin;

public
	class DataFormat {

	private
		boolean appendSlaveUnitId2Test = false;
	private
		ArrayList<String> logFailOnlyBaseClasses = new ArrayList();
	private
		ArrayList<String> logOnlyFailNodes = new ArrayList();
	private
		ArrayList<String> testDescFieldFilters = new ArrayList();
	private
		ArrayList<String> testDescFieldSelectors = new ArrayList();
	private
		ArrayList<String> flowContextFilters = new ArrayList();

	private
		boolean productionMode = true;

	private
		boolean generateMappingFile = true;

	private
		String kdfArchivePath = null;

	private
		String mappingPath = null;

	private
		boolean addFileName = false;

	private
		boolean logToFile = true;
	private
		boolean skipTestClass = false;

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
	private
		int sourceTypeIndex = -1;
	private
		int unitIdIndex = -1;
	private
		boolean enabledComponentHash = false;
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
	private
		ArrayList<String> fieldFiters = new ArrayList();

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
				if (fieldValue.isEmpty()) {
					continue;
				}

				switch (fieldName) {

					case "EnabledComponentHash":
						this.enabledComponentHash = fieldValue.equals("1");
						break;
					case "AppendSlaveUnitId2Test":
						this.appendSlaveUnitId2Test = fieldValue.equals("1");
						break;

					case "GenerateMappingFile":
						this.generateMappingFile = fieldValue.equals("1");
						break;

					case "ProductionMode":
						this.productionMode = fieldValue.equals("1");
						break;
					case "LogToFile":
						this.logToFile = fieldValue.equals("1");
						break;
					case "AddFileNameInUnit":
						this.addFileName = fieldValue.equals("1");
						break;
					case "LogFailOnlyNodeSelector":
						for (String name : fieldValue.split(",")) {
							this.logOnlyFailNodes.add(name.trim());
						}
						break;
					case "LogFailOnlyBaseClassSelector":
						for (String name : fieldValue.split(",")) {
							this.logFailOnlyBaseClasses.add(name.trim());
						}
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
					case "KDFArchivePath":
						this.kdfArchivePath = fieldValue;
						break;
					case "XmlPath":
						this.xmlPath = fieldValue;
						break;
					case "MappingPath":
						this.mappingPath = fieldValue;
						break;
					case "DataType":
						this.dataType = Config.DataTypes.valueOf(fieldValue);
						if (this.dataType == null) {
							System.out.println("Fatal Error: DataType value must be one of " + Arrays.toString(Config.DataTypes.values()));
						}
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
					case "UnitIdIndex":
						this.unitIdIndex = Integer.valueOf(fieldValue);
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
					case "SourceTypeIndex":
						this.sourceTypeIndex = Integer.valueOf(fieldValue);
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

					case "FieldFilter":
						for (String filter : fieldValue.split(",")) {
							this.fieldFiters.add(filter.trim());
						}
						break;
					case "SkipTestClass":
						this.skipTestClass = fieldValue.equals("1");
						break;

					case "TestDescFieldFilter":
						for (String filter : fieldValue.split(",")) {
							this.testDescFieldFilters.add(filter.trim());
						}
						break;
					case "TestDescFieldSelector":
						for (String selector : fieldValue.split(",")) {
							this.testDescFieldSelectors.add(selector.trim());
						}
						break;
					case "FlowContextFilter":
						for (String filter : fieldValue.split(",")) {
							this.flowContextFilters.add(filter.trim());
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
		if (this.getSourceType().equalsIgnoreCase(Config.DataTypes.SMAP.name())
			|| this.getSourceType().equalsIgnoreCase(Config.DataTypes.WAT.name())) {
			return true;
		}
		if (this.getCustomer() == null
			|| this.getLotEndTimeNode() == null
			|| this.getLotOpenTimeNode() == null
			|| this.getLotStartTimeNode() == null
			|| this.getFactory() == null
			|| this.getDataType() == null
			|| this.getFileOpenTimeIndex().isEmpty()
			|| this.getKdfMonthIndex() == -1
			|| (this.getKdfPath() == null || (!new File(this.getKdfPath()).exists()))
			|| this.getLotNumberIndex() == -1
			|| (this.getMfgStepIndex() == -1)
			|| this.getSourceTypeIndex() == -1
			|| (this.getTestCodeIndex() == -1 && this.dataType.equals(Config.DataTypes.SLT))
			|| (this.getWaferNumberIndex() == -1 && this.dataType.equals(Config.DataTypes.WaferSort))
			|| this.getTesterNumberIndex() == -1
			|| this.getTesterTypes().isEmpty()
			|| this.getUnderLineCnt() == -1
			|| this.getTesterTypeIndex() == -1
			|| ((this.generateMappingFile && this.mappingPath == null)
			|| (this.generateMappingFile && (!new File(this.mappingPath).exists()) && (!new File(this.mappingPath).mkdirs())))
			|| (this.xmlPath == null
			|| ((!new File(this.xmlPath).exists()) && (!new File(this.xmlPath).mkdirs())))
			|| (this.kdfArchivePath == null
			|| ((!new File(this.kdfArchivePath).exists()) && (!new File(this.kdfArchivePath).mkdirs())))
			|| this.getXmlPath() == null
			|| this.getUnit().getEndTimeNode() == null
			|| this.getUnit().getHardBinNode() == null
			|| this.getUnit().getSoftBinNode() == null
			|| this.getUnit().getStartTimeNode() == null
			|| this.getUnit().getTestTimeNode() == null
			|| this.getUnit().getUnitIdNode() == null
			|| this.getUnit().getWaferNumberNode() == null
			|| this.getUnit().getxCoordNode() == null
			|| this.getUnit().getyCoordNode() == null
			|| (this.dataType.equals(Config.DataTypes.SLT) && this.unitIdIndex == -1)
			|| ((!this.dataType.equals(Config.DataTypes.WaferSort)) && this.getUnit().getWaferLotNode() == null)) {
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
			+ "siteIndex:%s, testCodeIndex:%s, testerNumberIndex:%s,  "
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

	public
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

	public
		void clearAll() {
		for (XmlNode node : this.getLotHead().values()) {
			node.resetValue();
		}
		this.hardBinDescs.clear();
		this.softBinDescs.clear();
		unit.clear();

	}

	/**
	 * get the lot head kv string here we skip the lot start/end/open time since
	 * we use the FieldType.FileTime as the lot time timestamper
	 *
	 * @return
	 */
	public
		String getLotHeadKVString() {
		boolean firstField = false;
		String value = "";
		for (XmlNode node : this.lotHead.values()) {
			if (!node.isEnabledLog()) {
				continue;
			}
			if (node.isLotEndTime() || node.isLotOpenTime() || node.isLotStartTime()) {
				continue;
			}
			if (node.getValue() != null && (!node.getValue().isEmpty())) {
				if (!firstField) {
					value += node.toKVString();
					firstField = true;
				}
				else {
					value += "," + node.toKVString();
				}
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
		String getUnitDocKVString() {
		String value = "," + FieldType.Type + "=" + FieldType.Unit;
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
		value += "," + FieldType.SoftBinDesc + "=" + this.getUnit().getSoftBinDescValue();
		value += "," + FieldType.HardBinDesc + "=" + this.getUnit().getHardBinDescValue();
		value += "," + FieldType.BinType + "=" + this.getUnit().getFlagIntValue();
		value += "," + FieldType.DieType + "=" + FieldType.MasterDie;

		return value;

	}

	/**
	 * skip all the master die info
	 *
	 * @return
	 */
	public
		String getSlaveUnitDocKVString() {
		String value = "," + FieldType.Type + "=" + FieldType.Unit;
		for (XmlNode node : this.getUnit().getNodes().values()) {
			if (!node.isEnabledLog()) {
				continue;
			}

			if (node.isUnitIdNode() || node.isWaferNumberNode() || node.isWaferLotNode() || node.isxNode() || node.isyNode()) {
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
		value += "," + FieldType.SoftBinDesc + "=" + this.getUnit().getSoftBinDescValue();
		value += "," + FieldType.HardBinDesc + "=" + this.getUnit().getHardBinDescValue();
		value += "," + FieldType.BinType + "=" + this.getUnit().getFlagIntValue();

		return value;

	}

	public
		String getUnitHeadTestKVStr() {
		String value = "";
		for (XmlNode node : this.getUnit().getNodes().values()) {
			if (!node.isEnabledLog()) {
				continue;
			}
			if (node.getValue() != null && (!node.getValue().isEmpty())) {
				// never log the unit start time and end time into test level data
				// never log the unit tets time to test level doc
				if (node.isTimeNode() || node.isUnitTestTimeNode()) {
					continue;
				}
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
		value += "," + FieldType.SoftBinDesc + "=" + this.getUnit().getSoftBinDescValue();
		value += "," + FieldType.HardBinDesc + "=" + this.getUnit().getHardBinDescValue();
		value += "," + FieldType.BinType + "=" + this.getUnit().getFlagIntValue();

		return value;

	}

	public
		String getFileDocTimeKVStr() {
		String value = "";
		XmlNode startTimeNode = this.getLotStartTimeNode();

		if (startTimeNode.getValue() != null) {
			value += "," + startTimeNode.toKVString();
		}
		XmlNode endTimeNode = this.getLotEndTimeNode();
		if (endTimeNode.getValue() != null) {
			value += "," + endTimeNode.toKVString();
		}

		if (startTimeNode.getValue() != null && endTimeNode.getValue() != null) {
			value += ("," + FieldType.GrossTime + "=" + (Long.valueOf(endTimeNode.getTimeLongValue()) - Long.valueOf(startTimeNode.getTimeLongValue())) / 1000);
		}
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
		XmlNode getLotEndTimeNode() {
		for (XmlNode node : this.getLotHead().values()) {
			if (node.isLotEndTime()) {
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
		String getSourceType() {
		return sourceType;
	}

	public
		int getSourceTypeIndex() {
		return sourceTypeIndex;
	}

	public
		boolean isEnabledComponentHash() {
		return enabledComponentHash;
	}

	public
		ArrayList<String> getFieldFiters() {
		return fieldFiters;
	}

	public
		boolean isIgnoreEmptyValueField() {
		return ignoreEmptyValueField;
	}

	public
		boolean isAddFileName() {
		return addFileName;
	}

	public
		String getMappingPath() {
		return mappingPath;
	}

	public
		String getKdfArchivePath() {
		return kdfArchivePath;
	}

	public
		int getUnitIdIndex() {
		return unitIdIndex;
	}

	public
		boolean isGenerateMappingFile() {
		return generateMappingFile;
	}

	public
		boolean isProductionMode() {
		return productionMode;
	}

	public
		ArrayList<String> getTestDescFieldFilters() {
		return testDescFieldFilters;
	}

	public
		ArrayList<String> getTestDescFieldSelectors() {
		return testDescFieldSelectors;
	}

	public
		ArrayList<String> getFlowContextFilters() {
		return flowContextFilters;
	}

	public
		ArrayList<String> getLogOnlyFailNodes() {
		return logOnlyFailNodes;
	}

	public
		ArrayList<String> getLogFailOnlyBaseClasses() {
		return logFailOnlyBaseClasses;
	}

	public
		boolean isAppendSlaveUnitId2Test() {
		return appendSlaveUnitId2Test;
	}

}
