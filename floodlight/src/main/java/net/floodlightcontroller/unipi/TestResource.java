package net.floodlightcontroller.unipi;
import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;


public class TestResource extends ServerResource {
	@Get("json")
	public Map<String, Object> Test() {
	Map<String, Object> info = new HashMap<String, Object>();
	info.put("name", "value");
	return info;
	}
}
