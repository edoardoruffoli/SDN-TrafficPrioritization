package net.floodlightcontroller.unipi;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<QoSFlow> show() {
		ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
				.get(ITrafficPrioritizerREST.class.getCanonicalName());
    	return (List<QoSFlow>) tp.getFlows();
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

			JsonNode sourceAddrNode = root.get("source_address");
			JsonNode destAddrNode = root.get("dest_address");
			JsonNode bandwidthNode = root.get("bandwidth");

			if (sourceAddrNode == null || destAddrNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'source_address', 'dest_address' or 'bandwidth' provided");
			}

			IPv4Address sourceAddr = IPv4Address.of(sourceAddrNode.asText());
			IPv4Address destAddr = IPv4Address.of(destAddrNode.asText());
			int bandwidth = bandwidthNode.asInt();
			
			QoSFlow qosflow = new QoSFlow(sourceAddr, destAddr, bandwidth);
			
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			if (!tp.registerFlow(qosflow)) {
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

			JsonNode sourceAddrNode = root.get("source_address");
			JsonNode destAddrNode = root.get("dest_address");
			JsonNode bandwidthNode = root.get("bandwidth");

			if (sourceAddrNode == null || destAddrNode == null) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("No 'source_address', 'dest_address' or 'bandwidth' provided");
			}
			
			IPv4Address sourceAddr = IPv4Address.of(sourceAddrNode.asText());
			IPv4Address destAddr = IPv4Address.of(destAddrNode.asText());
			int bandwidth = bandwidthNode.asInt();
			
			QoSFlow qosflow = new QoSFlow(sourceAddr, destAddr, bandwidth);
			
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			if (!tp.deregisterFlow(qosflow)) {
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
