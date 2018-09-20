/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kdf.reader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 *
 * @author ghfan
 */
public
	class XmlNode {

	private
		String name = null;
	private
		String value = null;

	private
		ArrayList<String> fieldNames = new ArrayList();
	private
		boolean timeNode = false;
	private
		boolean toLowerCase = false;
	private
		boolean endTime = false;
	private
		boolean startTime = false;
	private
		boolean unitTestTimeNode = false;

	private
		boolean lotStartTime = false;
	private
		boolean lotOpenTime = false;
	private
		boolean lotEndTime = false;
	private
		String timeLongValue = null;
	private
		boolean enabledLog = true;
	private
		boolean enabled = false;
	private
		int startIndex = -1;
	private
		int endIndex = -1;
	private
		boolean unitIdNode = false;

	/**
	 *
	 * @param fileName
	 */
	public
		XmlNode(String fieldName) {
		this.name = fieldName;
	}

	public
		ArrayList<String> getFieldNames() {
		return fieldNames;
	}

	public
		void setFieldNames(ArrayList<String> fieldNames) {
		this.fieldNames = fieldNames;
	}

	public
		String getName() {
		return name;
	}

	public
		String getValue() {
		return value;
	}

	public
		String getXmlValue() {
		if (this.value == null) {
			return "";
		}
		else {
			return this.value;
		}
	}

	/**
	 * Empty content is not valid
	 *
	 * @param aValue
	 */
	public
		void setValue(String aValue) {
		if (this.value != null && (!this.value.isEmpty())) {

//            System.out.printf("skip since has value %s = %s \n" , this.name, this.value);
			return;
		}
		if ((!aValue.trim().equals(""))
			&& (!aValue.equalsIgnoreCase("null"))) {
			this.value = aValue.trim();
		}
		else {
			return;
		}

	}

	public
		void resetTime() {
		if (this.value == null) {
			return;
		}

		if (this.isTimeNode()) {
			this.value = this.toNumber(this.value);
			this.timeLongValue = this.value;
			if (this.value.startsWith("1") && this.value.length() == 13) {
				this.value = toTimeStr(this.value);
			}
			else {
				this.timeLongValue = null;
				this.value = null;
			}

		}
		if (this.isToLowerCase()) {
			this.value = this.value.toLowerCase();
		}
	}

	@Override
	public
		String toString() {
		return "XmlNode = " + this.name
			+ " , Value = " + this.value
			+ " , Fields = " + this.fieldNames.toString()
			+ ", enabledLoged = " + this.enabledLog
			+ ", startIndex = " + this.startIndex
			+ ", endIndex = " + this.endIndex
			+ "\n";
	}

	public
		String toKVString() {

		if (this.isTimeNode()) {
			if (Config.convertTime) {
				if (this.value.length() != 14) {
					this.value = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuuMMddHHmmss"));
				}
				return this.name + "=" + this.value.substring(0, 4) + "-" + this.value.substring(4, 6) + "-" + this.value.substring(6, 8)
					+ "T" + this.value.substring(8, 10) + ":" + this.value.substring(10, 12) + ":"
					+ this.value.substring(12, 14) + ".00+08:00";
			}
			else {
				return this.name + "=" + this.timeLongValue;
			}
		}
		else {
			return this.name + "=" + this.value;
		}

	}

	void resetValue() {
		this.value = null;
	}

	public
		boolean isTimeNode() {
		return timeNode;
	}

	public
		boolean isToLowerCase() {
		return toLowerCase;
	}

	public
		void setTimeNode(boolean timeNode) {
		this.timeNode = timeNode;
	}

	public
		void setToLowerCase(boolean toLowerCase) {
		this.toLowerCase = toLowerCase;
	}

	public
		boolean isStartTime() {
		return startTime;
	}

	public
		boolean isEndTime() {
		return endTime;
	}

	public
		void setName(String name) {
		this.name = name;
	}

	public
		void setEndTime(boolean endTime) {
		this.endTime = endTime;
	}

	public
		void setLotStartTime(boolean lotStartTime) {
		this.lotStartTime = lotStartTime;
	}

	public
		void setLotOpenTime(boolean lotOpenTime) {
		this.lotOpenTime = lotOpenTime;
	}

	public
		void setUnitTestTimeNode(boolean unitTestTimeNode) {
		this.unitTestTimeNode = unitTestTimeNode;
	}

	public
		boolean isUnitTestTimeNode() {
		return unitTestTimeNode;
	}

	public
		void setStartTime(boolean startTime) {
		this.startTime = startTime;
	}

	public
		boolean isLotStartTime() {
		return lotStartTime;
	}

	public
		boolean isLotOpenTime() {
		return lotOpenTime;
	}

	private
		String toNumber(String value) {
		String number = "";
		for (int i = 0; i != value.length(); i++) {
			char chr = value.charAt(i);
			if (chr >= '0' && chr <= '9') {
				number += chr;
			}
		}
		return number;
	}

	public static
		String toTimeStr(String time) {
		Calendar c = Calendar.getInstance();
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		c.setTimeInMillis(Long.valueOf(time));
		return String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", c);
	}

	public
		void forceValueTo(String value) {
		this.value = value;
	}

	public
		boolean isEnabledLog() {
		return enabledLog;
	}

	public
		void setEnabledLog(boolean enabledLog) {
		this.enabledLog = enabledLog;
	}

	public
		boolean isEnabled() {
		return enabled;
	}

	public
		void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public
		int getStartIndex() {
		return startIndex;
	}

	public
		int getEndIndex() {
		return endIndex;
	}

	public
		void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public
		void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public
		boolean isUnitIdNode() {
		return unitIdNode;
	}

	public
		void setUnitIdNode(boolean unitIdNode) {
		this.unitIdNode = unitIdNode;
	}

	public
	boolean isLotEndTime() {
		return lotEndTime;
	}

	public
	void setLotEndTime(boolean lotEndTime) {
		this.lotEndTime = lotEndTime;
	}
	

	public
	String getTimeLongValue() {
		return timeLongValue;
	}
	public static
		void main(String[] args) {
		String value = "1494200055407";
		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuuMMddHHmmss"));
		System.out.println(date);

	}

}
