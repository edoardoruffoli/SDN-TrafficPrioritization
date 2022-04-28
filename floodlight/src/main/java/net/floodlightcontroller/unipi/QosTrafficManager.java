package net.floodlightcontroller.unipi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Pair;

public class QosTrafficManager {

	private static boolean verboseMode = true;
	
	protected static final Logger log = LoggerFactory.getLogger(QosTrafficManager.class);
	
	// List of pair of switches that have been enabled to support Traffic Prioritization Qos 
	private List<Pair<DatapathId, DatapathId>> qosEnabledSwitches;
	
	// List of registered Qos traffic flows
	private final List<QosTrafficFlow> qosTrafficFlows;
	
	public QosTrafficManager(boolean verbose) {
		this.qosEnabledSwitches = new ArrayList<>();
		this.qosTrafficFlows = new ArrayList<>();
		this.verboseMode = verbose;
	}
	
	public List<Pair<DatapathId, DatapathId>> getQosEnabledSwitches() {
		return qosEnabledSwitches;
	}
	
	public boolean addQosEnabledSwitches(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {
		if (verboseMode) {
			log.debug("A new pair of switches have been enabled");
		}
		qosEnabledSwitches.add(new Pair(dpidMeterSwitch, dpidQueueSwitch));
		
		return true;
	}
	
	public boolean removeQosEnabledSwitches(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {
		if (verboseMode) {
			log.debug("A pair of switches have been disabled");
		}
		qosEnabledSwitches.remove(new Pair(dpidMeterSwitch, dpidQueueSwitch));
		
		return true;
	}
		
	public List<QosTrafficFlow> getQosTrafficFlows() {
		return qosTrafficFlows;
	}
	
	public boolean addQosTrafficFlow(QosTrafficFlow qosflow) {
		if (verboseMode) {
			log.debug("A new Qos Traffic Flow has been added");
		}

		qosTrafficFlows.add(qosflow);
		
		return true;
	}
	
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
