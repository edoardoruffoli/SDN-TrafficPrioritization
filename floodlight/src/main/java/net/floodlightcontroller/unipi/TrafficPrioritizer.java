package net.floodlightcontroller.unipi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFlags;
import org.projectfloodlight.openflow.protocol.OFMeterMod;
import org.projectfloodlight.openflow.protocol.OFMeterModCommand;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFQueueStatsEntry;
import org.projectfloodlight.openflow.protocol.OFQueueStatsReply;
import org.projectfloodlight.openflow.protocol.OFQueueStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionMeter;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBand;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDscpRemark.Builder;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

import javafx.util.Pair;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.unipi.web.ITrafficPrioritizerREST;
import net.floodlightcontroller.unipi.web.TrafficPrioritizerWebRoutable;

public class TrafficPrioritizer implements IFloodlightModule, IOFMessageListener, ITrafficPrioritizerREST {
	
	// Floodlight services used by the module
	protected IFloodlightProviderService floodlightProvider; 	// Reference to the provider
	protected IRestApiService restApiService; 					// Reference to the Rest API service
	protected IOFSwitchService switchService;
	protected ILinkDiscoveryService linkService;
	
	// Logger
	protected static Logger log;
	
	// Qos flows
	private QosTrafficManager qosTrafficManager = new QosTrafficManager(true);
	
	// Qos Queues IDs
	private final int QOS_SWITCH_BEST_EFFORT_QUEUE = 0;
	private final int QOS_SWITCH_LESS_EFFORT_QUEUE = 1;
	private final int QOS_SWITCH_QOS_QUEUE = 2;
	
    /*
     * The default rule of a switch is to forward a packet to the controller.
     * The rules of a switch that implements QoS must have a priority higher 
     * than one, so that the rules installed by the Forwarding module are ignored.
     */
    private final int QOS_SWITCH_DEFAULT_RULE_PRIORITY = 10;
	
	@Override
	public String getName() {
		return TrafficPrioritizer.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}	

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	    Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(ITrafficPrioritizerREST.class);
	    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	    Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(ITrafficPrioritizerREST.class, this);
	    return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
	    l.add(IRestApiService.class);
	    l.add(IOFSwitchService.class);
	    l.add(ILinkDiscoveryService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		linkService = context.getServiceImpl(ILinkDiscoveryService.class);
		
		log = LoggerFactory.getLogger(TrafficPrioritizer.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		// Add as REST interface
		restApiService.addRestletRoutable(new TrafficPrioritizerWebRoutable());
	}


	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
		case PACKET_IN:
			// Cast Packet
			OFPacketIn pi = (OFPacketIn) msg;
			
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
	                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			
			// Print the source MAC address
			Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress().getBytes());
			/* System.out.printf("TRAFFIC PRIORITIZER MAC Address: {%s} seen on switch: {%s}\n",
			HexString.toHexString(sourceMACHash),
			sw.getId());*/
						
			// Dissect Packet included in Packet-In
			IPacket pkt = eth.getPayload();

			break;
		default:
			break;
		}
		return Command.CONTINUE;
	}
	
	@Override
	public HashMap<DatapathId,SwitchQosDesc> getSwitchTopology() {
		HashMap<DatapathId,SwitchQosDesc> topoInfo = new HashMap<DatapathId,SwitchQosDesc>();	
		Map<DatapathId,Set<Link>> tmp = linkService.getSwitchLinks();

		for (DatapathId dpid: tmp.keySet()) {					
			String typeSw = switchService.getSwitch(dpid).getSwitchDescription().getHardwareDescription();
			
			Set<Link> links = tmp.get(dpid);
			
			SwitchQosDesc swDesc = new SwitchQosDesc(typeSw, links);
			topoInfo.put(dpid, swDesc);
		}
		
		return topoInfo;
	}
	
	@Override
	public List<String> getEnabledSwitches() {
		List<String> returnList = new ArrayList<>();
		
		for (Pair<?, ?> p : qosTrafficManager.getQosEnabledSwitches()) {
			String s1 = "Meter Switch: " + p.getKey().toString();
			String s2 = "Queue Switch: " + p.getValue().toString();
			returnList.add(s1 + " - " + s2);
		}
		return returnList;
	}
	
	@Override
	/**
	 * 
	 */
	public Integer enableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {		
		// Check if the switches are connected to the Controller
		if (switchService.getActiveSwitch(dpidMeterSwitch) == null || switchService.getActiveSwitch(dpidQueueSwitch) == null)
			return -1;	
		
		// Check if the switches are already enabled
		if (qosTrafficManager.getQosEnabledSwitches().contains(new Pair<DatapathId, DatapathId>(dpidMeterSwitch, dpidQueueSwitch)))
			return -2;
				
		qosTrafficManager.addQosEnabledSwitches(dpidMeterSwitch, dpidQueueSwitch);
		return 0;
	}
	
	@Override
	public Integer disableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {
		// Check if the switches are connected to the Controller
		if (switchService.getActiveSwitch(dpidMeterSwitch) == null || switchService.getActiveSwitch(dpidQueueSwitch) == null)
			return -1;	

		if (!qosTrafficManager.removeQosEnabledSwitches(dpidMeterSwitch, dpidQueueSwitch))
			return -2;
		
		return 0;
	}
	
	
	@Override
	public List<QosTrafficFlow> getQosTrafficFlows(){
    	List<QosTrafficFlow> info = new ArrayList<>();

        for (QosTrafficFlow flow : qosTrafficManager.getQosTrafficFlows()) {
            info.add(flow);
        }

        log.info("The list of flows has been provided.");
        
    	return info;
	} 
	
	@Override
	public Integer registerQosTrafficFlow(QosTrafficFlow qosTrafficFlow) {
		log.info("Registering Qos Flow [source_addr: " + qosTrafficFlow.getSourceAddr() + " dest: " + qosTrafficFlow.getDestAddr() + "]");
		
		// Check if the switches are enabled
		if (!qosTrafficManager.getQosEnabledSwitches().contains(new Pair<DatapathId, DatapathId>(qosTrafficFlow.getDpidMeterSwitch(), qosTrafficFlow.getDpidQueueSwitch())))
			return -1;
		
        /* The next non used meter id */
        qosTrafficFlow.setMeterId(qosTrafficManager.getNextMeterId());
        
		// Add the Qos Traffic Flow 
		if (!qosTrafficManager.addQosTrafficFlow(qosTrafficFlow))
			return -2;
		
		// BOFUSS (Meter Enabled Switch)
        IOFSwitch sw = switchService.getSwitch(qosTrafficFlow.getDpidMeterSwitch()); /* The IOFSwitchService */
        
		createMeter(sw, qosTrafficFlow.getMeterId(), qosTrafficFlow.getBandwidth(), /*burst*/0);		
		installGoToMeterFlow(sw, qosTrafficFlow.getMeterId(), qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());
        installSetTosFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());
	
        /*
         * packets remarked by the meter band with prec_level=1 will have ip_dscp=4, while non remarked packet will have ip_dscp=2.
         */
		// OvS (Queue Enabled Switch)
        sw = switchService.getSwitch(qosTrafficFlow.getDpidQueueSwitch()); /* The IOFSwitchService */

        /* Flow that are not conforming to the registered QoS traffic bandwidth are enqueued in the lowest priority queue */ 
		installEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_LESS_EFFORT_QUEUE, IpDscp.DSCP_4);
		
		/* Flow that are conforming to the registered traffic parameters are enqueued in the highest priority queue */
		installEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_QOS_QUEUE, IpDscp.DSCP_2);
		
		return 0;	
	}
	
	@Override
	public boolean deregisterQosTrafficFlow(QosTrafficFlow q) {
		QosTrafficFlow qosTrafficFlow = qosTrafficManager.removeQosTrafficFlow(q);
		
		if (qosTrafficFlow == null)
			return false;
		
		log.info("De-registering Qos Flow [source_addr: " + qosTrafficFlow.getSourceAddr() + " dest: " + qosTrafficFlow.getDestAddr() + "]");
		
		// BOFUSS (Meter Enabled Switch)
        IOFSwitch sw = switchService.getSwitch(qosTrafficFlow.getDpidMeterSwitch()); /* The IOFSwitchService */
        
        removeMeter(sw, qosTrafficFlow.getMeterId());		
		removeGoToMeterFlow(sw, qosTrafficFlow.getMeterId(), qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());
        removeSetTosFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());

		// OvS (Queue Enabled Switch)
        sw = switchService.getSwitch(qosTrafficFlow.getDpidQueueSwitch()); /* The IOFSwitchService */

        /* Flow that are not conforming to the registered QoS traffic bandwidth are enqueued in the lowest priority queue */ 
		removeEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_LESS_EFFORT_QUEUE, IpDscp.DSCP_4);
		
		/* Flow that are conforming to the registered traffic parameters are enqueued in the highest priority queue */
		removeEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_QOS_QUEUE, IpDscp.DSCP_2);
		
		return true;
	}
	
	/**
	 * Set default ToS for traffic registered to QualityOfService
	 * OpenFlow requires the switch evaluate the goto-meter instruction in a flow prior to any apply actions instruction. 
	 * This ensures the meter can perform its prescribed task (e.g. drop packet or DSCP remark) prior to potentially sending 
	 * the packet out. If a meter drops a packet, any further instructions in the flow will not be processed for that 
	 * particular packet.
	 * Only ToS with low value
	 * For this reason we should first set the tos and then it will be remarked by the meter
	 * @param sw
	 * @param qosflow
	 */
	private void installSetTosFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("Installing flow default ToS for registered QoS traffic instruction on switch "+ sw.getId());
		
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		/* Setting default Type of Service value */
		OFActionSetField dscp = factory.actions().buildSetField()
				.setField(factory.oxms().buildIpDscp()
						.setValue(IpDscp.DSCP_2)		// Corresponding to ToS 0x08
						.build())
				.build();	
		actions.add(dscp);
		
		OFInstructionApplyActions setTos = factory.instructions().buildApplyActions()
				.setActions(actions)
				.build();
		instructions.add(setTos);
		
		OFInstructionGotoTable goToTable = factory.instructions().buildGotoTable()
				.setTableId(TableId.of(1))
				.build();
		
		instructions.add(goToTable);
		
		/* Matching registered QoS traffic parameters */
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
			 
		/* Flow will send matched packets to table 1 to match the GotoMeter flow */
		OFFlowAdd flowAdd = factory.buildFlowAdd()
		    .setTableId(TableId.of(0))
		    .setPriority(1)
		    .setInstructions(instructions)
		    .setMatch(matchBuilder.build())
		    .build();
		
		 /* Send flow modification message to switch */
        sw.write(flowAdd);
	}
	
	/**
	 * 
	 * @param sw
	 * @param qosflow
	 */
	private void removeSetTosFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("removing flow default ToS for registered QoS traffic instruction on switch "+ sw.getId());
		
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		/* Setting default Type of Service value */
		OFActionSetField dscp = factory.actions().buildSetField()
				.setField(factory.oxms().buildIpDscp()
						.setValue(IpDscp.DSCP_2)		// Corresponding to ToS 0x08
						.build())
				.build();	
		actions.add(dscp);
		
		OFInstructionApplyActions setTos = factory.instructions().buildApplyActions()
				.setActions(actions)
				.build();
		instructions.add(setTos);
		
		OFInstructionGotoTable goToTable = factory.instructions().buildGotoTable()
				.setTableId(TableId.of(1))
				.build();
		
		instructions.add(goToTable);
		
		/* Matching registered QoS traffic parameters */
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
			 
		/* Flow will send matched packets to table 1 to match the GotoMeter flow */
		OFFlowDelete flowDelete = factory.buildFlowDelete()
		    .setTableId(TableId.of(0))
		    .setInstructions(instructions)
		    .setMatch(matchBuilder.build())
		    .build();
		
		 /* Send flow modification message to switch */
        sw.write(flowDelete);
	}
	
	/**
	 * Create and install a meter of type dscp_remark in a switch.
	 * 
	 * @param sw 			the switch in which install the meter.
	 * @param meterId		the id of the meter
	 * @param rate			the maximum bandwidth rate
	 * @param burstSize		burst
	 */
	
	private void createMeter(final IOFSwitch sw, final long meterId, final long rate, final long burstSize) {
		log.info("Creating meter " + meterId + " on switch "+ sw.getId());
		
		OFFactory factory = sw.getOFFactory();
        
		/* Specify meter flags */
		Set<OFMeterFlags> flags = new HashSet<>(Arrays.asList(OFMeterFlags.KBPS));//, OFMeterFlags.BURST));
        
        /* Create and set meter band */
        Builder bandBuilder = factory.meterBands().buildDscpRemark()
                .setRate(rate)
                .setPrecLevel((short) 1);
                //.setBurstSize(burstSize);
        
        /* Create meter modification message */
        OFMeterMod.Builder meterModBuilder = factory.buildMeterMod()
            .setMeterId(meterId)
            .setCommand(OFMeterModCommand.ADD)
            .setFlags(flags)
            .setMeters(Collections.singletonList((OFMeterBand) bandBuilder.build()));
        	
        /* Send meter modification message to switch */
        sw.write(meterModBuilder.build());
        
        return;
	}
	
	/**
	 * Remove a meter of type dscp_remark from a switch.
	 * 
	 * @param sw 			the switch in which install the meter.
	 * @param meterId		the id of the meter
	 * @param rate			the maximum bandwidth rate
	 * @param burstSize		burst
	 */
	
	private void removeMeter(final IOFSwitch sw, final long meterId) {
		log.info("Removing meter " + meterId + " on switch "+ sw.getId());
		
		OFFactory factory = sw.getOFFactory();
        
        /* Create meter delete message */
        OFMeterMod.Builder meterModBuilder = factory.buildMeterMod()
            .setMeterId(meterId)
            .setCommand(OFMeterModCommand.DELETE);
        	
        /* Send meter modification message to switch */
        sw.write(meterModBuilder.build());
        
        return;
	}
	
	/**
	 * 
	 * @param sw
	 * @param meterId
	 * @param qosTrafficFlow
	 * @return
	 */
	private void installGoToMeterFlow(final IOFSwitch sw, int meterId, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("installing go-to-meter flow instruction " + meterId + " on switch "+ sw.getId());
				
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		/* Add Goto Meter instructions */
		OFInstructionMeter meter = factory.instructions().buildMeter()
			.setMeterId(meterId)
			.build();
		
		OFActionOutput port = factory.actions().buildOutput().setPort(OFPort.ALL).build();
		actions.add(port);

		OFInstructionApplyActions output = factory.instructions().buildApplyActions()
				.setActions(actions)
				.build();
		 
		/*
		 * Regardless of the instruction order in the flow, the switch is required 
		 * to process the meter instruction prior to any apply actions instruction.
		 */
		instructions.add(meter);
		instructions.add(output);
		
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
			 
		/* Flow will send matched packets to meter ID 1 and then output */
		OFFlowAdd flowAdd = factory.buildFlowAdd()
		    .setInstructions(instructions)
		    .setTableId(TableId.of(1))
		    .setPriority(1)
		    .setMatch(matchBuilder.build())
		    .build();
		
		 /* Send meter modification message to switch */
        sw.write(flowAdd);
	}
	
	/**
	 * 
	 * @param sw
	 * @param meterId
	 * @param qosTrafficFlow
	 * @return
	 */
	private void removeGoToMeterFlow(final IOFSwitch sw, int meterId, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("removing go-to-meter flow instruction " + meterId + " on switch "+ sw.getId());
				
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		/* Add Goto Meter instructions */
		OFInstructionMeter meter = factory.instructions().buildMeter()
			.setMeterId(meterId)
			.build();
		
		OFActionOutput port = factory.actions().buildOutput().setPort(OFPort.ALL).build();
		actions.add(port);

		OFInstructionApplyActions output = factory.instructions().buildApplyActions()
				.setActions(actions)
				.build();
		 
		/*
		 * Regardless of the instruction order in the flow, the switch is required 
		 * to process the meter instruction prior to any apply actions instruction.
		 */
		instructions.add(meter);
		instructions.add(output);
		
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
		
		OFFlowDelete flowDelete = factory.buildFlowDelete()
				.setInstructions(instructions)
				.setTableId(TableId.of(1))
				.setMatch(matchBuilder.build())
				.build();
				
		 /* Send flow delete message to switch */
        sw.write(flowDelete);
	}
	
	/**
	 * 
	 * @param sw
	 * @param meterId
	 * @param qosflow
	 * @return
	 */
	private void installEnqueueBasedOnDscpFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr, int queueId, IpDscp dscp) {
		log.info("installing QoS flow instruction on switch "+ sw.getId());
		
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFFactory factory = sw.getOFFactory();

		OFActionSetQueue setQueue = factory.actions().buildSetQueue()
		        .setQueueId(queueId)
		        .build();
		actions.add(setQueue);
		
		OFActionOutput output = factory.actions().buildOutput()
				.setPort(OFPort.ALL)
				.build();
		actions.add(output);
		
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr)
	        .setExact(MatchField.IP_DSCP, dscp);
			 
		OFFlowAdd flowAdd = factory.buildFlowAdd()
			.setMatch(matchBuilder.build())
		    .setActions(actions)
		    .setPriority(1)
		    .build();
		
		 /* Send meter modification message to switch */
        sw.write(flowAdd);
	}
	
	/**
	 * 
	 * @param sw
	 * @param meterId
	 * @param qosflow
	 * @return
	 */
	private void removeEnqueueBasedOnDscpFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr, int queueId, IpDscp dscp) {
		log.info("removing QoS enqueue flow instruction on switch "+ sw.getId());
		
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFFactory factory = sw.getOFFactory();

		OFActionSetQueue setQueue = factory.actions().buildSetQueue()
		        .setQueueId(queueId)
		        .build();
		actions.add(setQueue);
		
		OFActionOutput output = factory.actions().buildOutput()
				.setPort(OFPort.ALL)
				.build();
		actions.add(output);
		
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr)
	        .setExact(MatchField.IP_DSCP, dscp);
			 
		OFFlowDelete flowDelete = factory.buildFlowDelete()
				.setMatch(matchBuilder.build())
			    .setPriority(1)
			    .build();
		
		 /* Send meter modification message to switch */
        sw.write(flowDelete);
	}
	
	@Override
	public Map<String, BigInteger> getNumPacketsHandledPerTrafficClass(DatapathId dpid) {		
		log.info("Class statistics requested");
		
		Map<String, BigInteger> classStats = new HashMap<String, BigInteger>();
		IOFSwitch sw = switchService.getSwitch(dpid);		
		OFFactory factory = sw.getOFFactory();
		
		OFQueueStatsRequest sr = factory.buildQueueStatsRequest()
				.setQueueId(-1)
				.build();
		ListenableFuture<List<OFQueueStatsReply>> future = sw.writeStatsRequest(sr);
		
		try {
			// Wait up to 10s for a reply; return when received; else exception thrown
		    List<OFQueueStatsReply> replies = future.get(10, TimeUnit.SECONDS);
		    
		    for (OFQueueStatsReply reply : replies) {
		        for (OFQueueStatsEntry e : reply.getEntries()) {		 
		        	if (e.getQueueId() == 0)
		        		classStats.merge("Best Effort Traffic", e.getTxPackets().getBigInteger(), (a, b) -> a.add(b));
		        	else if (e.getQueueId() == 2)
		        		classStats.merge("Conformant QoS Traffic", e.getTxPackets().getBigInteger(), (a, b) -> a.add(b));
		        	else if(e.getQueueId() == 1)
			        	classStats.merge("Non-conformant QoS Traffic", e.getTxPackets().getBigInteger(), (a, b) -> a.add(b));
		        }
		    }
		} catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
		    e.printStackTrace();
		}
				
		return classStats;
	}
}
