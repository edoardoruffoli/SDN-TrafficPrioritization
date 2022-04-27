package net.floodlightcontroller.unipi;

import java.util.Set;

import net.floodlightcontroller.linkdiscovery.Link;

public class SwitchQosDesc {
	private Boolean qosEnabled;
	private Set<Link> links;
	private String type;

	public SwitchQosDesc(Boolean qosEnabled, Set<Link> links, String type) {
		this.qosEnabled = qosEnabled;
		this.links = links;
		this.type = type;
	}

	public Boolean getQosEnabled() {
		return qosEnabled;
	}

	public void setQosEnabled(Boolean qosEnabled) {
		this.qosEnabled = qosEnabled;
	}

	public Set<Link> getLinks() {
		return links;
	}

	public void setLinks(Set<Link> links) {
		this.links = links;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
