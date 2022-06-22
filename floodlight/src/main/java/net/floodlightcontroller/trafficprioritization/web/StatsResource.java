package net.floodlightcontroller.trafficprioritization.web;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that manages the resource "/stats/json"
 */
public class StatsResource extends ServerResource{
	/**
	 * Retrieves the QoS class statistics
	 * @return  the number of packets handled by each class
	 */
	@Post("json")
    public Map<String, BigInteger> showStats(String fmJson) {
		// Check if the payload is provided
		if (fmJson == null) {
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return null;
		}

		// Parse the JSON input
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(fmJson);
			
			JsonNode dpidSwitchNode = root.get("dpid-switch");
			
			if (dpidSwitchNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return null;
			}
			
			DatapathId dpidSwitch = DatapathId.of(dpidSwitchNode.asText());
						
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			Map<String, BigInteger> ret = tp.getNumPacketsHandledPerTrafficClass(dpidSwitch);
			
			if (ret == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				ret = new HashMap<>();
				ret.put("The switch specified is not enable to support QoS or it is not a queue supporting switch", BigInteger.valueOf(-1));
				return ret;
			}
			else
				return ret;
    	
		} catch (IOException e) {
			e.printStackTrace();
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return null;
		}
    }
}
