package net.floodlightcontroller.unipi;

import java.io.IOException;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using=QosTrafficFlow.class)
/**
 * Class that is used to serialize the information of the QoS traffic flows
 */
public class QosTrafficFlow extends JsonSerializer<QosTrafficFlow> {
	
	// Pair of switches that implement the QosTrafficFlow
	private DatapathId dpidMeterSwitch; 
	private DatapathId dpidQueueSwitch;
	
	// QosTrafficFlow fields
	private IPv4Address sourceAddr;
	private IPv4Address destAddr;
	private Integer bandwidth;
	
	/* Meter associated to the QosTrafficFlow */
	private Integer meterId;
	
    /*
     * Do not use this constructor. Used primarily for JSON
     * Serialization/Deserialization
     */
	public QosTrafficFlow() {}
	
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
				qosflow.getDestAddr().equals(this.getDestAddr());
	}

	@Override
	public void serialize(QosTrafficFlow qtf, JsonGenerator jgen, SerializerProvider arg2)
			throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeStringField("dpid-meter-switch", qtf.getDpidMeterSwitch().toString());
        jgen.writeStringField("dpid-queue-switch", qtf.getDpidQueueSwitch().toString());
        jgen.writeStringField("src-addr", qtf.getSourceAddr().toString());
        jgen.writeStringField("dst-addr", qtf.getDestAddr().toString());
        jgen.writeNumberField("bandwidth", qtf.getBandwidth());
        jgen.writeEndObject();
		
	}
}
