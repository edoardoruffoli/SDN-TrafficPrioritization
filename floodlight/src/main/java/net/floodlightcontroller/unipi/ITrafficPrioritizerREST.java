package net.floodlightcontroller.unipi;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;

import javafx.util.Pair;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.Link;

//Service interface for the module
//This interface will be use to interact with other modules
//Export here all the methods of the class that are likely used by other modules

public interface ITrafficPrioritizerREST extends IFloodlightService {
	
	public HashMap<DatapathId,SwitchQosDesc> getSwitchTopology();
	
	public List<String> getEnabledSwitches();
	
	public Integer enableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch);
	
	public Integer disableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch);
	
	public List<QosFlow> getFlows();
	
	public boolean registerFlow(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch, QosFlow qosflow);
	
	public boolean deregisterFlow(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch, QosFlow qosflow);
	
	public Map<String, BigInteger> getNumPacketsHandled();
	
}
