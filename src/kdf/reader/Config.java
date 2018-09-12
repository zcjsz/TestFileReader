/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kdf.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;

/**
 *
 * @author ghfan
 */
public
	class Config {

	public static
		boolean renameKDF = false;

	public static
		boolean convertTime = true;

	public static
		String lockFilePath = null;
	public static
		HashMap<String, DataFormat> dataFormats = new HashMap();

	static
		enum DataTypes {
		WaferSort, ATE, SLT
	};

	static
		enum FailureCase {
		ValidationFailure, OpenFailure, Exception
	};

	static
		enum KdfRename {
		exception, done, skip, openErr
	}
	static
		String subClass = "subClass=";

	static
		String baseClass = "baseClass=";

	public
		Config(String configFile) {
		if (!readDataFormat(configFile)) {
			System.out.println("please setup correct config file");
		}
		for (DataFormat dataFormat : dataFormats.values()) {
			if (!dataFormat.validate()) {
				System.exit(1);
			}
		}
	}

	private
		boolean readDataFormat(String configFile) {

		SAXReader reader = new SAXReader();
		Document document = null;

		reader.addHandler("/root/LockFilePath", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				lockFilePath = row.getTextTrim();
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/ConvertTime", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				convertTime = row.getTextTrim().equals("1");
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/RenameKDF", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				renameKDF = row.getTextTrim().equals("1");
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});

		reader.addHandler("/root/SourceType", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element sourceTypeNode = path.getCurrent();
				DataFormat dataFormat = new DataFormat(sourceTypeNode);
				sourceTypeNode.detach();
				if (Config.dataFormats.containsKey(dataFormat.getSourceTypeName())) {
					System.out.println("Fatal Error: duplicate source data found " + dataFormat.getSourceTypeName());
					System.exit(1);
				}
				Config.dataFormats.put(dataFormat.getSourceTypeName(), dataFormat);
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/Head", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element head = path.getCurrent();
				List<Element> xmlHeadNodes = head.elements();
				for (Element xmlNode : xmlHeadNodes) {
					readHeadNode(xmlNode);
				}
				head.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/Unit", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element unit = path.getCurrent();
				List<Element> xmlHeadNodes = unit.elements();
				for (Element xmlNode : xmlHeadNodes) {
					readUnitNode(xmlNode);
				}
				unit.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		try {
			document = reader.read(configFile);
			if (lockFilePath == null || (!new File(lockFilePath).exists())) {
				System.err.println("please setup correct lockFilePath");
				return false;
			}
		}
		catch (DocumentException ex) {
			Logger.getLogger(DataFormat.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		return true;
	}

	private
		void readHeadNode(Element element) {
		@SuppressWarnings("unused")
		List<Element> nodes = element.elements();
		String xmlNodeName = null;
		boolean nodeEnabled = false;
		boolean logEnabled = false;
		if (!nodes.isEmpty()) {
			xmlNodeName = element.attributeValue("name").trim();

			ArrayList<String> fieldNames = new ArrayList();

			boolean isTime = false;
			boolean isLowerCase = false;

			boolean isLotStartTime = false;
			boolean isLotOpenTime = false;
			boolean enabledLog = true;

			for (Element node : nodes) {
				String nodeName = node.getName().trim().toLowerCase();
				String value = node.getText().trim();
				if (value.isEmpty() && nodeName.equals("SourceType")) {
					System.out.printf("Fatal Error: please setup value for this tag %s in xml node: %s\n", nodeName, xmlNodeName);
					System.exit(1);
				}
				switch (nodeName) {
					case "time":
						isTime = value.equals("1");
						break;
					case "lowercase":
						isLowerCase = value.equals("1");
						break;
					case "lotstarttime":
						isLotStartTime = value.equals("1");
						break;
					case "lotopentime":
						isLotOpenTime = value.equals("1");
						break;
					case "field":
						if (!fieldNames.contains(value)) {
							fieldNames.add(value);
						}
						break;
					case "sourcetype":
						String sourceType = node.attributeValue("name").trim();
						if (!Config.dataFormats.containsKey(sourceType)) {
							System.out.printf("Fatal Error: please setup this source type: %s first\n", sourceType);
							System.exit(1);
						}

						XmlNode xmlNode = new XmlNode(xmlNodeName);
						nodeEnabled = node.attributeValue("enabled").trim().equals("1");
						enabledLog = node.elementTextTrim("EnabledLog").equals("1");

						if (Config.dataFormats.get(sourceType).getLotHead().containsKey(xmlNodeName)) {
							System.out.printf("Fatal Error: duplicate head xml node found %s\n", xmlNodeName);
							System.exit(1);
						}
						Config.dataFormats.get(sourceType).getLotHead().put(xmlNodeName, xmlNode);
						xmlNode.setEnabledLog(enabledLog);
						xmlNode.setEnabled(nodeEnabled);

						break;

					default:
						System.out.printf("FATAL ERROR: unsupportted head xml node property found: %s in head xml node: %s\n", nodeName, xmlNodeName);
						System.exit(1);
				}
			}

			for (DataFormat dataFormat : Config.dataFormats.values()) {
				XmlNode xmlNode = dataFormat.getLotHead().get(xmlNodeName);
				if (xmlNode == null) {
					System.out.printf("Fatal Error: please setup the head xml node: %s property for source type: %s\n", xmlNodeName, dataFormat.getSourceTypeName());
					System.exit(1);
				}
				if (xmlNode.isEnabled()) {
					if (fieldNames.isEmpty()) {
						System.out.printf("Fatal Error: please add field for this head xml node %s\n", xmlNodeName);
						System.exit(1);
					}
					xmlNode.setFieldNames(fieldNames);
					xmlNode.setTimeNode(isTime);
					xmlNode.setToLowerCase(isLowerCase);
					xmlNode.setLotOpenTime(isLotOpenTime);
					xmlNode.setLotStartTime(isLotStartTime);
				}
				else {
					dataFormat.getLotHead().remove(xmlNodeName);
				}

			}
		}
	}

	private
		void readUnitNode(Element element) {
		@SuppressWarnings("unused")
		List<Element> nodes = element.elements();
		String xmlNodeName = null;

		if (!nodes.isEmpty()) {
			xmlNodeName = element.attributeValue("name").trim();
			ArrayList<String> fieldNames = new ArrayList();

			boolean isTime = false;
			boolean isLowerCase = false;
			boolean isEndTime = false;
			boolean isStartTime = false;
			boolean isTestTime = false;

			boolean nodeEnabled = false;
			boolean enabledLog = true;

			for (Element node : nodes) {
				String nodeName = node.getName().trim().toLowerCase();
				String value = node.getText().trim();
				if (value.isEmpty() && nodeName.equals("SourceType")) {
					System.out.printf("Fatal Error: please setup value for this tag %s in xml node: %s\n", nodeName, xmlNodeName);
					System.exit(1);
				}
				switch (nodeName) {
					case "time":
						isTime = value.equals("1");
						break;
					case "lowercase":
						isLowerCase = value.equals("1");
						break;
					case "starttime":
						isStartTime = value.equals("1");
						break;
					case "endtime":
						isEndTime = value.equals("1");
						break;
					case "testtime":
						isTestTime = value.equals("1");
						break;
					case "field":
						if (!fieldNames.contains(value)) {
							fieldNames.add(value);
						}
						break;
					case "sourcetype":
						String sourceType = node.attributeValue("name").trim();
						if (!Config.dataFormats.containsKey(sourceType)) {
							System.out.printf("Fatal Error: please setup this source type: %s first\n", sourceType);
							System.exit(1);
						}

						XmlNode xmlNode = new XmlNode(xmlNodeName);
						nodeEnabled = node.attributeValue("enabled").trim().equals("1");
						enabledLog = node.elementTextTrim("EnabledLog").equals("1");
						if (Config.dataFormats.get(sourceType).getUnit().getNodes().containsKey(xmlNodeName)) {
							System.out.printf("Fatal Error: duplicate unit xml node found %s\n", xmlNodeName);
							System.exit(1);
						}
						Config.dataFormats.get(sourceType).getUnit().getNodes().put(xmlNodeName, xmlNode);
						xmlNode.setEnabledLog(enabledLog);
						xmlNode.setEnabled(nodeEnabled);

						break;
					default:
						System.out.printf("FATAL ERROR: unsupportted unit xml node property found: %s in unit xml node: %s\n", nodeName, xmlNodeName);
						System.exit(1);
				}
			}

			for (DataFormat dataFormat : Config.dataFormats.values()) {
				XmlNode xmlNode = dataFormat.getUnit().getNodes().get(xmlNodeName);
				if (xmlNode == null) {
					System.out.printf("Fatal Error: please setup the unit xml node: %s property for source type: %s\n", xmlNodeName, dataFormat.getSourceTypeName());
					System.exit(1);
				}
				if (xmlNode.isEnabled()) {
					if (fieldNames.isEmpty()) {
						System.out.printf("Fatal Error: please add field for this unit xml node %s\n", xmlNodeName);
						System.exit(1);
					}
					xmlNode.setFieldNames(fieldNames);
					xmlNode.setTimeNode(isTime);
					xmlNode.setToLowerCase(isLowerCase);
					xmlNode.setStartTime(isStartTime);
					xmlNode.setEndTime(isEndTime);
					xmlNode.setUnitTestTimeNode(isTestTime);
				}
				else {
					dataFormat.getUnit().getNodes().remove(xmlNodeName);
				}

			}

		}

	}

	public static
		void main(String[] args) {
		new Config("./config/dataformat.xml");
		System.out.println("");

	}
}
