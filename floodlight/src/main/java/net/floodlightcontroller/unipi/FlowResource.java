package net.floodlightcontroller.unipi;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class FlowResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(FlowResource.class);
	
	
	/**
	 * Retrieves the list of the registered QoS flows.
	 * @return  the list of flows.
	 */
	@Get("json")
    public List<QosFlow> show() {
		ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
				.get(ITrafficPrioritizerREST.class.getCanonicalName());
    	return (List<QosFlow>) tp.getFlows();
    }
	
	@Post("json")
	public String register(String fmJson) {

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
			JsonNode sourceAddrNode = root.get("src_addr");
			JsonNode destAddrNode = root.get("dst_addr");
			JsonNode bandwidthNode = root.get("bandwidth");
			
			if (dpidMeterSwitchNode == null || dpidQueueSwitchNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'dpid-meter-switch' or 'dpid-queue-switch' provided");
			}

			if (sourceAddrNode == null || destAddrNode == null || bandwidthNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'src_addr', 'dst_addr' or 'bandwidth' provided");
			}
			
			DatapathId dpidMeterSwitch = DatapathId.of(dpidMeterSwitchNode.asText());
			DatapathId dpidQueueSwitch = DatapathId.of(dpidQueueSwitchNode.asText());
			IPv4Address sourceAddr = IPv4Address.of(sourceAddrNode.asText());
			IPv4Address destAddr = IPv4Address.of(destAddrNode.asText());
			int bandwidth = bandwidthNode.asInt();
			
			QosFlow qosflow = new QosFlow(sourceAddr, destAddr, bandwidth);
			
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			if (!tp.registerFlow(dpidMeterSwitch, dpidQueueSwitch, qosflow)) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("This flow was already registered");
			}
			
			return new String("OK");

		} catch (IOException e) {
			e.printStackTrace();
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return new String("Unknown error occurred");
		}
	}
	
	@Delete("json")
	public String deregister(String fmJson) {
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
			JsonNode sourceAddrNode = root.get("src_addr");
			JsonNode destAddrNode = root.get("dst_addr");
			JsonNode bandwidthNode = root.get("bandwidth");
			
			if (dpidMeterSwitchNode == null || dpidQueueSwitchNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'dpid-meter-switch' or 'dpid-queue-switch' provided");
			}

			if (sourceAddrNode == null || destAddrNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'src_addr', 'dst_addr' or 'bandwidth' provided");
			}
			
			DatapathId dpidMeterSwitch = DatapathId.of(dpidMeterSwitchNode.asText());
			DatapathId dpidQueueSwitch = DatapathId.of(dpidQueueSwitchNode.asText());
			IPv4Address sourceAddr = IPv4Address.of(sourceAddrNode.asText());
			IPv4Address destAddr = IPv4Address.of(destAddrNode.asText());
			int bandwidth = bandwidthNode.asInt();
			
			QosFlow qosflow = new QosFlow(sourceAddr, destAddr, bandwidth);
			
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			if (!tp.deregisterFlow(dpidMeterSwitch, dpidQueueSwitch, qosflow)) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("This flow is not registered");
			}
			
			return new String("OK");

		} catch (IOException e) {
			e.printStackTrace();
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return new String("Unknown error occurred");
		}
	}
}
