package net.floodlightcontroller.unipi;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.floodlightcontroller.linkdiscovery.web.LinkWithType;

public class SwitchQosDesc {
	@JsonProperty("qos-enabled")
	private Boolean qosEnabled;
	
	@JsonProperty("links")
	private Set<LinkWithType> links;
	
	@JsonProperty("switch-type")
	private String type;

	public SwitchQosDesc(Boolean qosEnabled, Set<LinkWithType> links, String type) {
		this.qosEnabled = qosEnabled;
		this.links = links;
		this.type = type;
	}

    /*
     * Do not use this constructor. Used primarily for JSON
     * Serialization/Deserialization
     */
    public SwitchQosDesc() {
        super();
    }

	public Boolean getQosEnabled() {
		return qosEnabled;
	}

	public void setQosEnabled(Boolean qosEnabled) {
		this.qosEnabled = qosEnabled;
	}

	public Set<LinkWithType> getLinks() {
		return links;
	}

	public void setLinks(Set<LinkWithType> links) {
		this.links = links;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}	
}