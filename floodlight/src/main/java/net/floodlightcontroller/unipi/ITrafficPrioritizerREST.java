package net.floodlightcontroller.unipi;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.module.IFloodlightService;

//Service interface for the module
//This interface will be use to interact with other modules
//Export here all the methods of the class that are likely used by other modules

public interface ITrafficPrioritizerREST extends IFloodlightService {
	
	public boolean enableTrafficPrioritization(String sw);
	
	public boolean disableTrafficPrioritization(String sw);
	
	public List<QoSFlow> getFlows();
	
	/* <IPSource, IPDestination, IPDSCPbits, (?) bandwidth> */
	public boolean registerFlow(QoSFlow qosflow);
	
	public boolean deregisterFlow(QoSFlow qosflow);
	
	public Map<String, BigInteger> getNumPacketsHandled();
	
}
