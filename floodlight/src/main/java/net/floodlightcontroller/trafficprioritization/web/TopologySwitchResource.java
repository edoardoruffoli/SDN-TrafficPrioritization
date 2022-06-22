package net.floodlightcontroller.trafficprioritization.web;

import java.util.Map;
import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.trafficprioritization.SwitchQosDesc;

/**
 * Class that manages the resource "/switches/topology/json"
 */
public class TopologySwitchResource  extends ServerResource{
	@Get("json")
    public Map<DatapathId,SwitchQosDesc> showTopology(String fmJson) {
		
		ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
				.get(ITrafficPrioritizerREST.class.getCanonicalName());
    	return  tp.getSwitchTopology();
    }
	
}
