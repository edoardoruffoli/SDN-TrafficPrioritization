package net.floodlightcontroller.unipi;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.core.web.ControllerSummaryResource;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;
import net.floodlightcontroller.core.web.LoadedModuleLoaderResource;
import net.floodlightcontroller.linkdiscovery.web.LinksResource;
import net.floodlightcontroller.restserver.RestletRoutable;

public class TrafficPrioritizerWebRoutable implements RestletRoutable {
	/**
	 * Create the Restlet router and bind to the proper resources.
	 */
	@Override
	public Restlet getRestlet(Context context) {
		
		Router router = new Router(context);
		
		/* This resource show the topology information related to the Qos service*/
		router.attach("/switches/topology", TopologySwitchResource.class);
		
        /*
         * This resource will manage the list of switches that supports Qos.
         * @GET 	permits to retrieve the list of switches providing the service.
         * @POST 	permits to enable the Qos on a pair of switches.
         * 			@JSON:	"dpid-meter-switch","dpid-queue-switch"
         * @DELETE	permits to disable the Qos on a pair of switches.
         * 			@JSON:	"dpid-meter-switch","dpid-queue-switch"
         */
		router.attach("/switches/json", SwitchResource.class);

        /*
         * This resource will manage the list of flows.
         * @GET 	permits to retrieve the list of servers providing the service.
         * @POST 	permits to register a new flow.
         * 			@JSON:	"src_addr","dst_addr, "bandwidth"
         * @DELETE	permits to remove a registered flow.
         * 			@JSON:	"src_addr","dst_addr, "bandwidth"
         */
		router.attach("/flow/json", FlowResource.class);
		
		/* This resource will show the list of switches connected to the controller */
		router.attach("/controller/switches/json", ControllerSwitchesResource.class);
		
		// This resource will show the stats of traffic
		router.attach("/queue/stats/json", QueueResource.class);
		
		return router;
	}

	@Override
	public String basePath() {
		// The root path for the resources
		return "/qos";
	}
}
