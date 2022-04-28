package net.floodlightcontroller.unipi;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.floodlightcontroller.linkdiscovery.web.LinkWithType;

public class SwitchQosDesc {
	
	@JsonProperty("switch-type")
	private String switchType;
	
	@JsonProperty("links")
	private Set<LinkWithType> links;
	
	public SwitchQosDesc(String switchType, Set<LinkWithType> links) {
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

	public Set<LinkWithType> getLinks() {
		return links;
	}

	public void setLinks(Set<LinkWithType> links) {
		this.links = links;
	}

	public String getSwitchType() {
		return switchType;
	}

	public void setSwitchType(String switchType) {
		this.switchType = switchType;
	}	
}