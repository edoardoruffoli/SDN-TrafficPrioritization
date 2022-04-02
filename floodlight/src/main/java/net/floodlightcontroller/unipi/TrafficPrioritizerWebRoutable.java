package net.floodlightcontroller.unipi;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.core.web.ControllerSummaryResource;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;
import net.floodlightcontroller.core.web.LoadedModuleLoaderResource;
import net.floodlightcontroller.restserver.RestletRoutable;

public class TrafficPrioritizerWebRoutable implements RestletRoutable {
	/**
	 * Create the Restlet router and bind to the proper resources.
	 */
	@Override
	public Restlet getRestlet(Context context) {
		
		Router router = new Router(context);

        /*
         * This resource will manage the list of flows.
         * @GET 	permits to retrieve the list of servers providing the service.
         * @POST 	permits to register a new flow.
         * 			@JSON:	"source_address","dest_address, "bandwidth"
         * @DELETE	permits to remove a registered flow.
         * 			@JSON:	"source_address","dest_address, "bandwidth"
         */
		router.attach("/flow/json", FlowResource.class);
		
		// This resource will show the list of modules loaded in the controller
		//router.attach("/module/loaded/json", UnregisterFlow.class);
		
		// This resource will show the list of switches connected to the controller
		router.attach("/switches/json", ControllerSwitchesResource.class);
		
		router.attach("/switches/stats/json", SwitchResource.class);
		return router;
	}

	@Override
	public String basePath() {
		// The root path for the resources
		return "/qos";
	}
}
