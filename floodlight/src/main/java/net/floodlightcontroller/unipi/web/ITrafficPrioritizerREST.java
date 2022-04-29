package net.floodlightcontroller.unipi.web;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;

import javafx.util.Pair;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.unipi.QosTrafficFlow;
import net.floodlightcontroller.unipi.SwitchQosDesc;

//Service interface for the module
//This interface will be use to interact with other modules
//Export here all the methods of the class that are likely used by other modules

public interface ITrafficPrioritizerREST extends IFloodlightService {
	
	public HashMap<DatapathId,SwitchQosDesc> getSwitchTopology();
	
	public List<String> getEnabledSwitches();
	
	public Integer enableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch);
	
	public Integer disableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch);
	
	public List<QosTrafficFlow> getQosTrafficFlows();
	
	public Integer registerQosTrafficFlow(QosTrafficFlow qosflow);
	
	public boolean deregisterQosTrafficFlow(QosTrafficFlow qosflow);
	
	public Map<String, BigInteger> getNumPacketsHandledPerTrafficClass(DatapathId dpid);
	
}
