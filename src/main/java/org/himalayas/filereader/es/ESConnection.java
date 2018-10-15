/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.es;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 *
 * @author ghfan
 */
public
	class ESConnection {

	public
		ESConnection() {
	}

	public
		boolean init() {
		StatusLogger.getLogger().setLevel(Level.INFO);
		Settings settings = Settings.builder()
			.put("cluster.name", "DataSystem")
			.put("client.transport.sniff", true)
			.build();
		TransportClient client = null;
		try {
			client = new PreBuiltTransportClient(settings).addTransportAddress(new TransportAddress(InetAddress.getByName("suzgpu01.jv.tfme.com"), 9300))
				//                    .addTransportAddress(new TransportAddress(InetAddress.getByName("bmw001"), 9300))
				.addTransportAddress(new TransportAddress(InetAddress.getByName("vpngtdweb01"), 9300))
				.addTransportAddress(new TransportAddress(InetAddress.getByName("vpngdataparse01"), 9300));
		}
		catch (UnknownHostException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

}
