package net.floodlightcontroller.unipi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowManager {

	private static boolean verboseMode = true;
	
	protected static final Logger log = LoggerFactory.getLogger(FlowManager.class);
	
	private final List<QosFlow> flows;
	
	public FlowManager(boolean verbose) {
		this.flows = new ArrayList<>();
		this.verboseMode = verbose;
	}
	
	public List<QosFlow> getFlows() {
		return flows;
	}
	
	public boolean addFlow(QosFlow qosflow) {
		if (verboseMode) {
			log.debug("A server has been added to the pool");
		}

		flows.add(qosflow);
		
		return true;
	}
	
	public boolean removeFlow(QosFlow qosflow) {
		Iterator<QosFlow> i = flows.iterator();
		boolean foundIt = false;
		
		while (i.hasNext()) {
			if (i.next().equals(qosflow)) {

				i.remove();

				if (verboseMode) {
					log.debug("A flow has been removed from the list:");
				}

				foundIt = true;
				break;
			}
		}
		return foundIt;
	}
	
	
}
