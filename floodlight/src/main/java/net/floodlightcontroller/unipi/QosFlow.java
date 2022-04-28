package net.floodlightcontroller.unipi;

import org.projectfloodlight.openflow.types.IPv4Address;

public class QoSFlow {
	private final IPv4Address sourceAddr;
	private final IPv4Address destAddr;
	// IPv4Address bits
	private final Integer bandwidth;
	
	/**
	 * Initialize a flow descriptor
	 * @param sourceAddr
	 * @param destAddr
	 * @param bandwidth
	 */
	public QoSFlow(IPv4Address sourceAddr, IPv4Address destAddr, Integer bandwidth) {
		this.sourceAddr = sourceAddr;
		this.destAddr = destAddr;
		this.bandwidth = bandwidth;
	}
	
	public IPv4Address getSourceAddress() {
		return sourceAddr;
	}
	
	public IPv4Address getDestAddress() {
		return destAddr;
	}
	
	public Integer getBandwidth() {
		return bandwidth;
	}
	
	public boolean equals(QoSFlow qosflow) {
		return  qosflow.getSourceAddress().equals(this.getSourceAddress()) || 
				qosflow.getDestAddress().equals(this.getDestAddress()) ||
				qosflow.getBandwidth().equals(this.getBandwidth());
	}
}
