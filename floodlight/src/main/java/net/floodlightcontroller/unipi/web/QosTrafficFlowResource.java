package net.floodlightcontroller.unipi.web;
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

import net.floodlightcontroller.unipi.QosTrafficFlow;


public class QosTrafficFlowResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(QosTrafficFlowResource.class);
	
	
	/**
	 * Retrieves the list of the registered QoS flows.
	 * @return  the list of flows.
	 */
	@Get("json")
    public List<QosTrafficFlow> show() {
		ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
				.get(ITrafficPrioritizerREST.class.getCanonicalName());
    	return (List<QosTrafficFlow>) tp.getQosTrafficFlows();
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
			
			QosTrafficFlow qosTrafficFlow = new QosTrafficFlow(dpidMeterSwitch, dpidQueueSwitch, sourceAddr, destAddr, bandwidth);
			
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			int ret = tp.registerQosTrafficFlow(qosTrafficFlow);
			if (ret == -1) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("The speicifed pair of switches is not enabled to support Qos");
			}
			else if (ret == -2) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new String("This Qos Traffic Flow has been already registered");
			}
			else 
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
			
			QosTrafficFlow qosTrafficFlow = new QosTrafficFlow(dpidMeterSwitch, dpidQueueSwitch, sourceAddr, destAddr, bandwidth);
			
			ITrafficPrioritizerREST tp = (ITrafficPrioritizerREST) getContext().getAttributes()
					.get(ITrafficPrioritizerREST.class.getCanonicalName());
			
			if (!tp.deregisterQosTrafficFlow(qosTrafficFlow)) {
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
