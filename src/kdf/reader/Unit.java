/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kdf.reader;

import java.util.LinkedHashMap;

/**
 *
 * @author ghfan
 */
public
	class Unit {

	private final
		LinkedHashMap<String, XmlNode> nodes = new LinkedHashMap();
	private
		String softBinDesc = null;
	private
		String hardBinDesc = null;
	private
		String flag = null;

	private
		XmlNode softBinNode = null;
	private
		XmlNode hardBinNode = null;

	// must be having those node in each unit
	private
		XmlNode unitIdNode = null;
	private
		XmlNode waferNumberNode = null;
	private
		XmlNode xCoordNode = null;
	private
		XmlNode yCoordNode = null;
	private
		XmlNode startTimeNode = null;
	private
		XmlNode endTimeNode = null;
	private
		XmlNode testTimeNode = null;
	private
		XmlNode waferLotNode = null;

	public
		Unit() {
	}

	public
		LinkedHashMap<String, XmlNode> getNodes() {
		return nodes;
	}

	public
		String getFlag() {
		return flag;
	}

	public
		String getFlagValue() {
		if (this.flag == null) {
			return "F";
		}
		return flag;
	}

	public
		String getFlagIntValue() {
		if (this.flag == null) {
			return "0";
		}
		return (this.flag == "P" ? "1" : "0");
	}

	public
		String getHardBinDesc() {
		return hardBinDesc;
	}

	public
		String getHardBinDescValue() {
		if (this.getHardBinDesc() == null) {
			return "unknow";
		}

		return hardBinDesc;
	}

	public
		String getSoftBinDesc() {
		return softBinDesc;
	}

	public
		String getSoftBinDescValue() {
		if (this.getSoftBinDesc() == null) {
			return "unknow";
		}
		return softBinDesc;
	}

	public
		void setFlag(String flag) {
		if (this.flag == null) {
			this.flag = flag;
		}
	}

	public
		void setHardBinDesc(String hardBinDesc) {
		this.hardBinDesc = hardBinDesc;
	}

	public
		void setSoftBinDesc(String softBinDesc) {
		this.softBinDesc = softBinDesc;
	}

	void clear() {
		for (XmlNode node : this.nodes.values()) {
			node.resetValue();
		}
		this.hardBinDesc = null;
		this.softBinDesc = null;
		this.flag = null;
	}

	public
		XmlNode getHardBinNode() {
//		if (this.hardBinNode == null) {
//			for (XmlNode node : this.nodes.values()) {
//				for (String fieldName : node.getFieldNames()) {
//					if (fieldName.toLowerCase().contains("hard")) {
//						this.hardBinNode = node;
//						return this.hardBinNode;
//					}
//				}
//			}
//		}
		return hardBinNode;
	}

	public
		XmlNode getSoftBinNode() {
//		if (this.softBinNode == null) {
//			for (XmlNode node : this.nodes.values()) {
//				for (String fieldName : node.getFieldNames()) {
//					if (fieldName.toLowerCase().contains("soft")) {
//						this.softBinNode = node;
//						return this.softBinNode;
//					}
//				}
//			}
//		}
		return softBinNode;
	}

	void printinfo() {
		String value = "|----------------------------------------------------------------------------------------|\n";

		for (XmlNode noode : this.getNodes().values()) {
			if (noode.getValue() != null) {
				value += noode.toString();
			}
		}
		value += "\n";
		for (XmlNode noode : this.getNodes().values()) {
			if (noode.getValue() == null) {
				value += noode.toString();
			}
		}
		value += "XmlNode = SoftBinDesc , Value = " + this.softBinDesc + " , Fields = [SoftBinDesc] \n";
		value += "XmlNode = HardBinDesc , Value = " + this.hardBinDesc + " , Fields = [HardBinDesc] \n";
		value += "XmlNode = Result , Value = " + this.flag + " , Fields = [binDescFlag] \n";
		System.out.println(value);

	}

	public
		XmlNode getUnitIdNode() {
		return unitIdNode;
	}

	public
		XmlNode getWaferNumberNode() {
		return waferNumberNode;
	}

	public
		XmlNode getxCoordNode() {
		return xCoordNode;
	}

	public
		XmlNode getyCoordNode() {
		return yCoordNode;
	}

	public
		void setHardBinNode(XmlNode hardBinNode) {
		this.hardBinNode = hardBinNode;
	}

	public
		void setSoftBinNode(XmlNode softBinNode) {
		this.softBinNode = softBinNode;
	}

	public
		void setUnitIdNode(XmlNode unitIdNode) {
		this.unitIdNode = unitIdNode;
	}

	public
		void setWaferNumberNode(XmlNode waferNumberNode) {
		this.waferNumberNode = waferNumberNode;
	}

	public
		void setxCoordNode(XmlNode xCoordNode) {
		this.xCoordNode = xCoordNode;
	}

	public
		void setyCoordNode(XmlNode yCoordNode) {
		this.yCoordNode = yCoordNode;
	}

	public
		XmlNode getStartTimeNode() {
		return startTimeNode;
	}

	public
		void setStartTimeNode(XmlNode startTimeNode) {
		this.startTimeNode = startTimeNode;
	}

	public
		XmlNode getEndTimeNode() {
		return endTimeNode;
	}

	public
		void setEndTimeNode(XmlNode endTimeNode) {
		this.endTimeNode = endTimeNode;
	}

	public
		XmlNode getTestTimeNode() {
		return testTimeNode;
	}

	public
		void setTestTimeNode(XmlNode testTimeNode) {
		this.testTimeNode = testTimeNode;
	}

	public
		XmlNode getWaferLotNode() {
		return waferLotNode;
	}

	public
		void setWaferLotNode(XmlNode waferLotNode) {
		this.waferLotNode = waferLotNode;
	}

}
