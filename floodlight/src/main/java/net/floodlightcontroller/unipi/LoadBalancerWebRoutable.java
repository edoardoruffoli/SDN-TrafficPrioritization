package net.floodlightcontroller.unipi;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.core.web.ControllerSummaryResource;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;
import net.floodlightcontroller.core.web.LoadedModuleLoaderResource;
import net.floodlightcontroller.restserver.RestletRoutable;

public class LoadBalancerWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		
		Router router = new Router(context);
		
		// Add some pre-defined REST resources available in the floodlight framework
		// This resource will show some summary stats on the controller
		router.attach("/controller/summary/json", ControllerSummaryResource.class);
		
		// This resource will show the list of modules loaded in the controller
		router.attach("/module/loaded/json", LoadedModuleLoaderResource.class);
		
		// This resource will show the list of switches connected to the controller
		router.attach("/controller/switches/json", ControllerSwitchesResource.class);
		return router;
	}

	@Override
	public String basePath() {
		// The root path for the resources
		return "/lb";
	}

}
