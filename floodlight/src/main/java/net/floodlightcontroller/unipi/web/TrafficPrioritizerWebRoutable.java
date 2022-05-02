package net.floodlightcontroller.unipi.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

/**
 * Class that defines the REST interface of the TrafficPrioritizer module.
 */
public class TrafficPrioritizerWebRoutable implements RestletRoutable {
	
	/**
	 * Creates the Restlet router and bind to the proper resources.
	 * @param context the context for constructing the restlet.
     * @return        the Restlet router.
     */
	@Override
	public Restlet getRestlet(Context context) {
		
		Router router = new Router(context);
		
		// This resource show the topology information related to the Qos service
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
		router.attach("/flows/json", QosTrafficFlowResource.class);
		
		// This resource will show the stats of traffic
		router.attach("/stats/json", StatsResource.class);
		
		return router;
	}

    /**
     * Sets the base path for the endpoints of the REST interface
     * @return  the base path.
     */
	@Override
	public String basePath() {
		return "/qos";
	}
}
