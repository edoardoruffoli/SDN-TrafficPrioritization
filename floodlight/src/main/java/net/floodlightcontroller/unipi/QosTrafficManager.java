package net.floodlightcontroller.unipi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Pair;

/**
 * Class that implements a manager that provides a subscription service to the
 * other modules. In particular it accepts clients subscriptions and
 */
public class QosTrafficManager {

	private static boolean verboseMode = true;
	
	protected static final Logger log = LoggerFactory.getLogger(QosTrafficManager.class);
	
	// List of pair of switches that have been enabled to support Traffic Prioritization 
	private List<Pair<DatapathId, DatapathId>> qosEnabledSwitches;
	
	// List of registered Qos traffic flows
	private final List<QosTrafficFlow> qosTrafficFlows;
	
	public QosTrafficManager(boolean verbose) {
		this.qosEnabledSwitches = new ArrayList<>();
		this.qosTrafficFlows = new ArrayList<>();
		QosTrafficManager.verboseMode = verbose;
	}
	
	/**
	 * Retrieves the switches that have been enabled to support QoS traffic prioritization
	 * @return the list of enabled switch pairs
	 */
	public List<Pair<DatapathId, DatapathId>> getQosEnabledSwitches() {
		return qosEnabledSwitches;
	}
	
	/**
	 * Adds a pair switches to the list of the switches enabled to support QoS traffic prioritization
	 * @param dpidMeterSwitch 	DPID of the switch that supports OpenFlow meters
	 * @param dpidQueueSwitch	DPID of the switch that supports OpenFlow queues
	 * @return	a boolean carrying information about the success of the operation
	 */
	public boolean addQosEnabledSwitches(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {
		if (verboseMode) {
			log.debug("A new pair of switches have been enabled");
		}
		qosEnabledSwitches.add(new Pair<DatapathId, DatapathId>(dpidMeterSwitch, dpidQueueSwitch));
		
		return true;
	}
	
	/**
	 * Removes a pair switches from the list of the switches enabled to support QoS traffic prioritization
	 * @param dpidMeterSwitch 	DPID of the switch that supports OpenFlow meters
	 * @param dpidQueueSwitch	DPID of the switch that supports OpenFlow queues
	 * @return	a boolean carrying information about the success of the operation
	 */
	public boolean removeQosEnabledSwitches(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {
		if (verboseMode) {
			log.debug("A pair of switches have been disabled");
		}
		qosEnabledSwitches.remove(new Pair<DatapathId, DatapathId>(dpidMeterSwitch, dpidQueueSwitch));
		
		return true;
	}
	
	/**
	 * Retrieves the QoS traffic flows that have been registered
	 * @return the list of registered QoS traffic flows
	 */
	public List<QosTrafficFlow> getQosTrafficFlows() {
		return qosTrafficFlows;
	}
	
	/**
	 * Adds a QoS traffic flow to the list of registered QoS traffic flows
	 * @param qosflow	object containing the QoS Traffic Flow information
	 * @return 	a boolean carrying information about the success of the operation
	 */
	public boolean addQosTrafficFlow(QosTrafficFlow qosflow) {
		if (verboseMode) {
			log.debug("A new Qos Traffic Flow has been added");
		}
		
		// Check if there is already a registered traffic flow for the specified source and destination
		Iterator<QosTrafficFlow> i = qosTrafficFlows.iterator();
				
		while (i.hasNext()) {
			QosTrafficFlow cur = i.next();
			if (cur.equals(qosflow)) {
				if (verboseMode) {
					log.debug("A Qos Traffic Flow is already present for the specified end points");
				}
				return false;
			}
		}

		qosTrafficFlows.add(qosflow);
		
		return true;
	}
	
	/**
	 * Removes a QoS traffic flow from the list of registered QoS traffic flows
	 * @param qosflow	object containing the QoS Traffic Flow information
	 * @return 	a boolean carrying information about the success of the operation
	 */
	public QosTrafficFlow removeQosTrafficFlow(QosTrafficFlow qosflow) {
		Iterator<QosTrafficFlow> i = qosTrafficFlows.iterator();
		
		while (i.hasNext()) {
			QosTrafficFlow cur = i.next();
			if (cur.equals(qosflow)) {
				if (verboseMode) {
					log.debug("A Qos Traffic Flow has been removed");
				}
				i.remove();
				return cur;
			}
		}
		return null;
	}
	
	/**
	 * Returns the first unused identifier of the meters.
	 * @return	next free ID
	 */
	public Integer getNextMeterId() {
		if (verboseMode) {
			log.debug("Searching for next meter id");
		}
		
		int nextMeterId = qosTrafficFlows.size() + 1;
		List<Integer> idsList = new ArrayList<>();
		
		Iterator<QosTrafficFlow> it = qosTrafficFlows.iterator();
		while (it.hasNext())
			idsList.add(it.next().getMeterId());
		
		Collections.sort(idsList);
		
		for (int i = 1; i <= idsList.size(); i++) {
		      if (i < idsList.get(i - 1)) {
		         return i;
		      }
		}
		
		return nextMeterId;
	}	
}
