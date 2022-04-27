package net.floodlightcontroller.unipi;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;

public class TopologySwitchResource  extends ServerResource{
	@Get("json")
    public HashMap<DatapathId,SwitchQosDesc> showTopology(String fmJson) {
		
		ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
				.get(ITrafficPrioritizerREST.class.getCanonicalName());
    	return  tp.getSwitchTopology();
    }
	
}
