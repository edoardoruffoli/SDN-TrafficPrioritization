package net.floodlightcontroller.trafficprioritization;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.linkdiscovery.Link;

@JsonSerialize(using=SwitchQosDesc.class)
/**
 * Class that is used to serialize the topology information of the switches
 */
public class SwitchQosDesc extends JsonSerializer<SwitchQosDesc> {
	
	@JsonProperty("switch-type")
	private String switchType;
	
	@JsonProperty("links")
	private Set<Link> links;
	
	public SwitchQosDesc(String switchType, Set<Link> links) {
		this.links = links;
		this.switchType = switchType;
	}

    /*
     * Do not use this constructor. Used primarily for JSON
     * Serialization/Deserialization
     */
    public SwitchQosDesc() {
        super();
    }

	public Set<Link> getLinks() {
		return links;
	}

	public void setLinks(Set<Link> links) {
		this.links = links;
	}

	public String getSwitchType() {
		return switchType;
	}

	public void setSwitchType(String switchType) {
		this.switchType = switchType;
	}

	@Override
	public void serialize(SwitchQosDesc sqd, JsonGenerator jgen, SerializerProvider arg2)
			throws IOException, JsonProcessingException {
		
        jgen.writeStartObject();
        jgen.writeStringField("switch-type", sqd.getSwitchType().toString());
        jgen.writeArrayFieldStart("switch-links");
        
        for (Link l : sqd.getLinks()) {
            jgen.writeStartObject();
        	jgen.writeStringField("src-switch", l.getSrc().toString());
        	jgen.writeNumberField("src-port", l.getDstPort().getPortNumber());
        	jgen.writeStringField("dst-switch", l.getDst().toString());
        	jgen.writeNumberField("dst-port", l.getDstPort().getPortNumber());
        	jgen.writeNumberField("latency", l.getLatency().getValue());
        	jgen.writeEndObject();
        }
        jgen.writeEndArray();
        jgen.writeEndObject();	
	}	
}