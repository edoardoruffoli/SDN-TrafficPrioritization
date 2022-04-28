package net.floodlightcontroller.unipi;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;

public class QosTrafficFlow {
	
	/* Pair of switches that implement the QosTrafficFlow */
	private final DatapathId dpidMeterSwitch; 
	private final DatapathId dpidQueueSwitch;
	
	/* QosTrafficFlow fields */
	private final IPv4Address sourceAddr;
	private final IPv4Address destAddr;
	private final Integer bandwidth;
	
	/* Meter associated to the QosTrafficFlow */
	private Integer meterId;
	
	/**
	 * Initialize a flow descriptor
	 * @param sourceAddr
	 * @param destAddr
	 * @param bandwidth
	 */
	public QosTrafficFlow(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch, IPv4Address sourceAddr, IPv4Address destAddr, Integer bandwidth) {
		this.dpidMeterSwitch = dpidMeterSwitch;
		this.dpidQueueSwitch = dpidQueueSwitch;
		this.sourceAddr = sourceAddr;
		this.destAddr = destAddr;
		this.bandwidth = bandwidth;
	}

	public DatapathId getDpidMeterSwitch() {
		return dpidMeterSwitch;
	}

	public DatapathId getDpidQueueSwitch() {
		return dpidQueueSwitch;
	}

	public IPv4Address getSourceAddr() {
		return sourceAddr;
	}

	public IPv4Address getDestAddr() {
		return destAddr;
	}
	
	public Integer getBandwidth() {
		return bandwidth;
	}
	
	public Integer getMeterId() {
		return meterId;
	}

	public void setMeterId(Integer meterId) {
		this.meterId = meterId;
	}
	
	public boolean equals(QosTrafficFlow qosflow) {
		return  qosflow.getDpidMeterSwitch().equals(this.getDpidMeterSwitch()) ||
				qosflow.getDpidQueueSwitch().equals(this.getDpidQueueSwitch()) ||
				qosflow.getSourceAddr().equals(this.getSourceAddr()) || 
				qosflow.getDestAddr().equals(this.getDestAddr()) ||
				qosflow.getBandwidth().equals(this.getBandwidth());
	}
}
