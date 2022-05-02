package net.floodlightcontroller.unipi.web;

import java.io.IOException;
import java.util.List;
import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that manages the resource "/switches/json"
 */
public class SwitchResource extends ServerResource {
	/**
	 * Retrieves the switches that have been enabled to support traffic prioritization
	 * @return  the list of switches.
	 */
	@Get("json")
    public List<String> showEnabledSwitches(String fmJson) {	
		ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
				.get(ITrafficPrioritizerREST.class.getCanonicalName());
    	return  tp.getEnabledSwitches();
    }
	
	/**
	 * Adds a pair of switches to the list of switches implementing the Qos traffic prioritization service.
	 * @param fmJson  the JSON message.
	 * @return        a string carrying information about the success of the operation.
	 */
	@Post("json")
	public String enableQos(String fmJson) {

		// Check if the payload is provided
		if (fmJson == null) {
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new String("Empty payload");
		}

		// Parse the JSON input
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(fmJson);
			
			JsonNode dpidMeterSwitchNode = root.get("dpid-meter-switch");
			JsonNode dpidQueueSwitchNode = root.get("dpid-queue-switch");
			
			if (dpidMeterSwitchNode == null || dpidQueueSwitchNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'dpid-meter-switch' or 'dpid-queue-switch' provided");
			}
			
			DatapathId dpidMeterSwitch = DatapathId.of(dpidMeterSwitchNode.asText());
			DatapathId dpidQueueSwitch = DatapathId.of(dpidQueueSwitchNode.asText());

			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			int ret = tp.enableTrafficPrioritization(dpidMeterSwitch, dpidQueueSwitch);
			
			if (ret == -1) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("Switches not connected to the Controller");
			}
			else if (ret == -2) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("Switches already enabled");
			}
			else
				return new String("OK");
			
		} catch (IOException e) {
			e.printStackTrace();
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return new String("Unknown error occurred");
		}
	}

	/**
	 * Removes a pair of switches from the list of switches implementing the Qos traffic prioritization service..
	 * @param fmJson  the JSON message.
	 * @return        a string carrying information about the success of the operation.
	 */
	@Delete("json")
	public String disableQos(String fmJson) {

		// Check if the payload is provided
		if (fmJson == null) {
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new String("Empty payload");
		}

		// Parse the JSON input
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(fmJson);
			
			JsonNode dpidMeterSwitchNode = root.get("dpid-meter-switch");
			JsonNode dpidQueueSwitchNode = root.get("dpid-queue-switch");
			
			if (dpidMeterSwitchNode == null || dpidQueueSwitchNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'dpid-meter-switch' or 'dpid-queue-switch' provided");
			}
			
			// Get the dpid of the meter switch
			DatapathId dpidMeterSwitch;
			try {
				dpidMeterSwitch = DatapathId.of(dpidMeterSwitchNode.asText());
			} catch (IllegalArgumentException e) {
				return new String("Invalid DatapathId of the meter switch");
			}

			// Get the dpid of the queue switch
			DatapathId dpidQueueSwitch;
			try {
				dpidQueueSwitch = DatapathId.of(dpidQueueSwitchNode.asText());
			} catch (IllegalArgumentException e) {
				return new String("Invalid DatapathId of the queue switch");
			}
			
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			int ret = tp.disableTrafficPrioritization(dpidMeterSwitch, dpidQueueSwitch);
			
			if (ret == -1) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("Switches not connected to the Controller");
			}
			else if (ret == -2) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("Switches were not enabled");
			}
			else
				return new String("OK");
			
		} catch (IOException e) {
			e.printStackTrace();
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return new String("Unknown error occurred");
		}
	}
}
