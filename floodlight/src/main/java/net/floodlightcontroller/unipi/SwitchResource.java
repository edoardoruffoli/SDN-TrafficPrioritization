package net.floodlightcontroller.unipi;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class SwitchResource extends ServerResource {
	/**
	 * Retrieves the switch statistics
	 * @return  the list of flows.
	 */
	@Get("json")
    public Map<String, BigInteger> showStats(String fmJson) {
		
		ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
				.get(ITrafficPrioritizerREST.class.getCanonicalName());
    	return  tp.getNumPacketsHandled();
    }
	
}
