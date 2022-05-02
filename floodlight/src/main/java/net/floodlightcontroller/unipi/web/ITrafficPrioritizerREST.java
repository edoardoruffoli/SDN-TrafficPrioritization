package net.floodlightcontroller.unipi.web;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.unipi.QosTrafficFlow;
import net.floodlightcontroller.unipi.SwitchQosDesc;

/**
 * Service interface for the Traffic Prioritizer module.
 * This interface defines all the methods that are likely used by other modules
 */
public interface ITrafficPrioritizerREST extends IFloodlightService {
	
	/**
	 * Retrieves the topology information of the switches
	 * @return 	a list of switch descriptors, identified by their DPID
	 */
	public Map<DatapathId,SwitchQosDesc> getSwitchTopology();
	
	/**
	 * Retrieves the list of switches that have been enabled to support
	 * QoS traffic prioritization
	 * @return	a list of switch DPIDs
	 */
	public List<String> getEnabledSwitches();
	
	/**
	 * Enable the traffic prioritization on a pair switches
	 * @param dpidMeterSwitch 	DPID of the switch that supports OpenFlow meters
	 * @param dpidQueueSwitch	DPID of the switch that supports OpenFlow queues
	 * @return	a number carrying information about the success of the operation
	 */
	public Integer enableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch);
	
	/**
	 * Disable the traffic prioritization on a pair switches
	 * @param dpidMeterSwitch	DPID of the switch that supports OpenFlow meters
	 * @param dpidQueueSwitch	DPID of the switch that supports OpenFlow queues
	 * @return	a number carrying information about the success of the operation
	 */
	public Integer disableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch);
	
	/**
	 * Retrieves the list of QoS traffic flows that have been registered
	 * @return	a list of QoS traffic flow objects
	 */
	public List<QosTrafficFlow> getQosTrafficFlows();
	
	/**
	 * Register a QoS traffic flow
	 * @param qosflow	object containing the QoS Traffic Flow information
	 * @return 	a number carrying information about the success of the operation
	 */
	public Integer registerQosTrafficFlow(QosTrafficFlow qosflow);
	
	/**
	 * De-register a QoS traffic flow
	 * @param qosflow	object containing the QoS Traffic Flow information
	 * @return 	a number carrying information about the success of the operation
	 */
	public boolean deregisterQosTrafficFlow(QosTrafficFlow qosflow);
	
	/**
	 * Retrieves the number of packets of each QoS classes, handled by the specified switch
	 * @param dpid		DPID of the switch
	 * @return			the number of packets handled by each class
	 */
	public Map<String, BigInteger> getNumPacketsHandledPerTrafficClass(DatapathId dpid);
	
}
