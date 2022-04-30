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

/*
 * Class that implements the TrafficPrioritizer module exposed by the controller
 */
public class TrafficPrioritizer implements IFloodlightModule, IOFMessageListener, ITrafficPrioritizerREST {
	
	// Floodlight services used by the module
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApiService; 
	protected IOFSwitchService switchService;					
	protected ILinkDiscoveryService linkService;
	
	// Logger
	protected static Logger log;
	
	// Manager
	private QosTrafficManager qosTrafficManager = new QosTrafficManager(true);
	
	// Switch Queues IDs used for implementing Qos
	private final int QOS_SWITCH_BEST_EFFORT_QUEUE = 0;
	private final int QOS_SWITCH_LESS_EFFORT_QUEUE = 1;
	private final int QOS_SWITCH_QOS_QUEUE = 2;
	
	/**
	 * Installs a flow that sets the default Type of Service for the traffic flow that has been registered 
	 * as Quality of Service Traffic flows.
	 * OfSoftSwitch BOFUSS meters apply the DSCP remark only to packets with low value of ToS,
	 * for this reason we should first set an appropriate value of ToS to the packets belonging to the registered
	 * QoS traffic flow so that the meter applies the DSCP remark.
	 * @param sw		The switch in which the flow will be installed
	 * @param srcAddr	The IPv4 Address of the QoS traffic source
	 * @param dstAddr	The IPv4 Address of the QoS traffic destination
	 */
	private void installSetTosFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("Installing flow that sets the default ToS for the registered QoS traffic flow on switch "+ sw.getId());
		
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		// Setting the Type of Service value
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
		
		// Send the matched packets to table 1 to match the GotoMeter flow
		OFInstructionGotoTable goToTable = factory.instructions().buildGotoTable()
				.setTableId(TableId.of(1))
				.build();
		
		instructions.add(goToTable);
		
		// Matching registered QoS traffic end points
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
			 
		OFFlowAdd flowAdd = factory.buildFlowAdd()
		    .setTableId(TableId.of(0))	
		    .setPriority(1)					// Higher priority that default rule
		    .setInstructions(instructions)
		    .setMatch(matchBuilder.build())
		    .build();
		
		 // Send flow add message to switch
        sw.write(flowAdd);
	}
	
	/**
	 * Removes a flow that sets the default Type of Service for the traffic flow that has been registered 
	 * as Quality of Service Traffic flows.
	 * @param sw		The switch on which the flow will be removed
	 * @param srcAddr	The IPv4 Address of the QoS traffic source
	 * @param dstAddr	The IPv4 Address of the QoS traffic destination
	 */
	private void removeSetTosFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("Removing flow default ToS for registered QoS traffic instruction on switch "+ sw.getId());
		
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		// Setting the Type of Service value
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
		
		// Send the matched packets to table 1 to match the GotoMeter flow
		OFInstructionGotoTable goToTable = factory.instructions().buildGotoTable()
				.setTableId(TableId.of(1))
				.build();
		
		instructions.add(goToTable);
		
		// Matching registered QoS traffic parameters
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
			 
		OFFlowDelete flowDelete = factory.buildFlowDelete()
		    .setTableId(TableId.of(0))
		    .setInstructions(instructions)
		    .setMatch(matchBuilder.build())
		    .build();
		
		// Send flow delete message to switch
        sw.write(flowDelete);
	}
	
	/**
	 * Creates a meter of type dscp_remark in a switch.
	 * 
	 * @param sw 			the switch in which install the meter
	 * @param meterId		the ID of the meter
	 * @param rate			the maximum guaranteed bandwidth
	 * @param burstSize		burst
	 */
	
	private void createMeter(final IOFSwitch sw, final long meterId, final long rate, final long burstSize) {
		log.info("Creating DSCP remark meter " + meterId + " with guaranteed bandwidth " + rate + " on switch " + sw.getId());
		
		OFFactory factory = sw.getOFFactory();
        
		// Specify meter flags
		Set<OFMeterFlags> flags = new HashSet<>(Arrays.asList(OFMeterFlags.KBPS));//, OFMeterFlags.BURST));
        
        // Create and set meter band
        Builder bandBuilder = factory.meterBands().buildDscpRemark()
                .setRate(rate)
                .setPrecLevel((short) 1);	// Needed in order to match the ToS for the Remark
                //.setBurstSize(burstSize);
        
        // Create meter modification message
        OFMeterMod.Builder meterModBuilder = factory.buildMeterMod()
            .setMeterId(meterId)
            .setCommand(OFMeterModCommand.ADD)
            .setFlags(flags)
            .setMeters(Collections.singletonList((OFMeterBand) bandBuilder.build()));
        	
        // Send meter modification message to switch
        sw.write(meterModBuilder.build());
        
        return;
	}
	
	/**
	 * Removes a meter of type dscp_remark from a switch.
	 * 
	 * @param sw 			the switch in which the meter will be removed
	 * @param meterId		the ID of the meter
	 * @param rate			the maximum guaranteed bandwidth
	 * @param burstSize		burst
	 */
	
	private void removeMeter(final IOFSwitch sw, final long meterId) {
		log.info("Removing DSCP remark meter " + meterId + " on switch " + sw.getId());
		
		OFFactory factory = sw.getOFFactory();
        
        // Create meter delete message 
        OFMeterMod.Builder meterModBuilder = factory.buildMeterMod()
            .setMeterId(meterId)
            .setCommand(OFMeterModCommand.DELETE);
        	
        // Send meter modification message to switch
        sw.write(meterModBuilder.build());
        
        return;
	}
	
	/**
	 * Installs a flow that makes the packets, belonging to a registered QoS traffic flow, go through the specified meter. 
	 * The packets exceeding the bandwidth of the meter, will be DSCP remarked.
	 * @param sw		The switch in which the flow will be installed
	 * @param meterId	ID of the meter that will perform the DSCP remark.
	 * @param srcAddr	The IPv4 Address of the QoS traffic source
	 * @param dstAddr	The IPv4 Address of the QoS traffic destination
	 * @return
	 */
	private void installGoToMeterFlow(final IOFSwitch sw, int meterId, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("Installing go-to-meter flow instruction " + meterId + " on switch "+ sw.getId());
				
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		// Add Goto Meter instruction
		OFInstructionMeter meter = factory.instructions().buildMeter()
			.setMeterId(meterId)
			.build();
		
		// Set output port
		OFActionOutput port = factory.actions().buildOutput().setPort(OFPort.ALL).build();
		actions.add(port);

		OFInstructionApplyActions output = factory.instructions().buildApplyActions()
				.setActions(actions)
				.build();
		 
		// Regardless of the instruction order in the flow, the switch is required 
		// to process the meter instruction prior to any apply actions instruction.
		// Flow will send matched packets to the meter and then output
		instructions.add(meter);
		instructions.add(output);
		
		// Matching registered QoS traffic end points
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
			 
		OFFlowAdd flowAdd = factory.buildFlowAdd()
		    .setInstructions(instructions)
		    .setTableId(TableId.of(1))
		    .setPriority(1)
		    .setMatch(matchBuilder.build())
		    .build();
		
		// Send meter add message to switch
        sw.write(flowAdd);
	}
	
	/**
	 * Remove a flow that makes the packets, belonging to a registered QoS traffic flow, go through the specified meter. 
	 * @param sw		The switch in which the flow will be installed
	 * @param meterId	ID of the meter that will perform the DSCP remark.
	 * @param srcAddr	The IPv4 Address of the QoS traffic source
	 * @param dstAddr	The IPv4 Address of the QoS traffic destination
	 * @return
	 */
	private void removeGoToMeterFlow(final IOFSwitch sw, int meterId, IPv4Address srcAddr, IPv4Address dstAddr) {
		log.info("Removing go-to-meter flow instruction " + meterId + " on switch "+ sw.getId());
				
		OFFactory factory = sw.getOFFactory();
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		// Add Goto Meter instructions
		OFInstructionMeter meter = factory.instructions().buildMeter()
			.setMeterId(meterId)
			.build();
		
		OFActionOutput port = factory.actions().buildOutput().setPort(OFPort.ALL).build();
		actions.add(port);

		OFInstructionApplyActions output = factory.instructions().buildApplyActions()
				.setActions(actions)
				.build();
		
		instructions.add(meter);
		instructions.add(output);
		
		// Matching registered QoS traffic end points
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr);
		
		OFFlowDelete flowDelete = factory.buildFlowDelete()
				.setInstructions(instructions)
				.setTableId(TableId.of(1))
				.setMatch(matchBuilder.build())
				.build();
				
		// Send flow delete message to switch
        sw.write(flowDelete);
	}

	 /**
	  * Installs a flow that sends to the specified queue the packets that has the specified DSCP value.
	  * @param sw		The switch in which the flow will be installed
	  * @param srcAddr	The IPv4 Address of the QoS traffic source
	  * @param dstAddr	The IPv4 Address of the QoS traffic destination
	  * @param queueId	ID of the queue to which the packets will be sent
	  * @param dscp		DSCP value
	  */
	private void installEnqueueBasedOnDscpFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr, int queueId, IpDscp dscp) {
		log.info("Installing flow that sends to queue " + queueId + " packets with DSCP= " + dscp + " on switch " + sw.getId());
		
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFFactory factory = sw.getOFFactory();

		// Add Enqueue action
		OFActionSetQueue setQueue = factory.actions().buildSetQueue()
		        .setQueueId(queueId)
		        .build();
		actions.add(setQueue);
		
		OFActionOutput output = factory.actions().buildOutput()
				.setPort(OFPort.ALL)
				.build();
		actions.add(output);
		
		// Matching registered QoS traffic end points and DSCP value
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
		
		// Send meter add message to switch
        sw.write(flowAdd);
	}
	
	/**
	 * Removes a flow that sends to the specified queue the packets that has the specified DSCP value.
	 * @param sw		The switch in which the flow will be installed
	 * @param srcAddr	The IPv4 Address of the QoS traffic source
	 * @param dstAddr	The IPv4 Address of the QoS traffic destination
	 * @param queueId	ID of the queue to which the packets will be sent
	 * @param dscp		DSCP value
	 */
	private void removeEnqueueBasedOnDscpFlow(final IOFSwitch sw, IPv4Address srcAddr, IPv4Address dstAddr, int queueId, IpDscp dscp) {
		log.info("Removing flow that sends to queue " + queueId + " packets with DSCP= " + dscp + " on switch " + sw.getId());
		
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFFactory factory = sw.getOFFactory();

		// Add Enqueue action
		OFActionSetQueue setQueue = factory.actions().buildSetQueue()
		        .setQueueId(queueId)
		        .build();
		actions.add(setQueue);
		
		OFActionOutput output = factory.actions().buildOutput()
				.setPort(OFPort.ALL)
				.build();
		actions.add(output);
		
		// Matching registered QoS traffic end points and DSCP value
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, srcAddr)
	        .setExact(MatchField.IPV4_DST, dstAddr)
	        .setExact(MatchField.IP_DSCP, dscp);
			 
		OFFlowDelete flowDelete = factory.buildFlowDelete()
				.setMatch(matchBuilder.build())
			    .setPriority(1)
			    .build();
		
		// Send meter modification message to switch
        sw.write(flowDelete);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.IListener#getName()
	 */
	@Override
	public String getName() {
		return TrafficPrioritizer.class.getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.IListener#isCallbackOrderingPrereq(java.lang.
	 * Object, java.lang.String)
	 */
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.IListener#isCallbackOrderingPostreq(java.lang.
	 * Object, java.lang.String)
	 */
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.module.IFloodlightModule#getModuleServices()
	 */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	    Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(ITrafficPrioritizerREST.class);
	    return l;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#getServiceImpls()
	 */
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	    Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(ITrafficPrioritizerREST.class, this);
	    return m;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.module.IFloodlightModule#getModuleDependencies(
	 * )
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#init(net.
	 * floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		linkService = context.getServiceImpl(ILinkDiscoveryService.class);
		
		log = LoggerFactory.getLogger(TrafficPrioritizer.class);
        log.info("Initializing Traffic Prioritizer module");
        log.info("Best Effort Queue ID: {}", QOS_SWITCH_BEST_EFFORT_QUEUE);
        log.info("Less Effort Queue ID: {}", QOS_SWITCH_LESS_EFFORT_QUEUE);
        log.info("Qos Queue ID: {}: {}", QOS_SWITCH_QOS_QUEUE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#startUp(net.
	 * floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		// REST interface
		restApiService.addRestletRoutable(new TrafficPrioritizerWebRoutable());
	}


	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
		case PACKET_IN:		
	     	//log.debug("PACKET_IN received");
			OFPacketIn pi = (OFPacketIn) msg;
			
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
	                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
						
			IPacket pkt = eth.getPayload();

			Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress().getBytes());
			/* log.debug("MAC Address: {%s} seen on switch: {%s}\n",
				HexString.toHexString(sourceMACHash),
				sw.getId());*/
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
		
        log.info("The topology of switches has been provided");
        
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
        log.info("The list of enabled switches has been provided");
        
		return returnList;
	}
	
	@Override
	public Integer enableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {	
        log.info("Enabling the pair of switches <{} - {}>", dpidMeterSwitch, dpidQueueSwitch);
        
		// Check if the switches are connected to the Controller
		if (switchService.getActiveSwitch(dpidMeterSwitch) == null || switchService.getActiveSwitch(dpidQueueSwitch) == null)
			return -1;	
		
		// Check if the switches are already enabled to support QoS Traffic Prioritization
		if (qosTrafficManager.getQosEnabledSwitches().contains(new Pair<DatapathId, DatapathId>(dpidMeterSwitch, dpidQueueSwitch)))
			return -2;
				
		qosTrafficManager.addQosEnabledSwitches(dpidMeterSwitch, dpidQueueSwitch);
		return 0;
	}
	
	@Override
	public Integer disableTrafficPrioritization(DatapathId dpidMeterSwitch, DatapathId dpidQueueSwitch) {
        log.info("Disabling the pair of switches <{} - {}>", dpidMeterSwitch, dpidQueueSwitch);
        
		// Check if the switches are connected to the Controller
		if (switchService.getActiveSwitch(dpidMeterSwitch) == null || switchService.getActiveSwitch(dpidQueueSwitch) == null)
			return -1;	

		if (!qosTrafficManager.removeQosEnabledSwitches(dpidMeterSwitch, dpidQueueSwitch))
			return -2;	// Switch are not enabled to support QoS Traffic Prioritization
		
		return 0;
	}
	
	
	@Override
	public List<QosTrafficFlow> getQosTrafficFlows(){
    	List<QosTrafficFlow> info = new ArrayList<>();

        for (QosTrafficFlow flow : qosTrafficManager.getQosTrafficFlows()) {
            info.add(flow);
        }
        log.info("The list of flows has been provided");
        
    	return info;
	} 
	
	@Override
	public Integer registerQosTrafficFlow(QosTrafficFlow qosTrafficFlow) {
		log.info("Registering Qos Flow [src_addr: " + qosTrafficFlow.getSourceAddr() + 
				" dst_addr: " + qosTrafficFlow.getDestAddr() + 
				" bandwidth: " + qosTrafficFlow.getBandwidth() +
				"]");
		
		// Check if the switches are enabled
		if (!qosTrafficManager.getQosEnabledSwitches().contains(new Pair<DatapathId, DatapathId>(qosTrafficFlow.getDpidMeterSwitch(), qosTrafficFlow.getDpidQueueSwitch())))
			return -1;
		
        // Get the first non used meter id
        qosTrafficFlow.setMeterId(qosTrafficManager.getNextMeterId());
        
		// Add the Qos Traffic Flow 
		if (!qosTrafficManager.addQosTrafficFlow(qosTrafficFlow))
			return -2;
		
		/* BOFUSS (Meter Enabled Switch) */
        IOFSwitch sw = switchService.getSwitch(qosTrafficFlow.getDpidMeterSwitch());
        
		createMeter(sw, qosTrafficFlow.getMeterId(), qosTrafficFlow.getBandwidth(), /*burst*/0);		
		installGoToMeterFlow(sw, qosTrafficFlow.getMeterId(), qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());
        installSetTosFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());
	
		/* OvS (Queue Enabled Switch) */
        sw = switchService.getSwitch(qosTrafficFlow.getDpidQueueSwitch()); /* The IOFSwitchService */

        // Packets remarked by the meter band with prec_level=1 will have ip_dscp=4, while non remarked packet will have ip_dscp=2
		installEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_LESS_EFFORT_QUEUE, IpDscp.DSCP_4);
		installEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_QOS_QUEUE, IpDscp.DSCP_2);
		
		return 0;	
	}
	
	@Override
	public boolean deregisterQosTrafficFlow(QosTrafficFlow q) {
		QosTrafficFlow qosTrafficFlow = qosTrafficManager.removeQosTrafficFlow(q);
		
		if (qosTrafficFlow == null)
			return false;	// The QoS Traffic Flow has not been registered
		
		log.info("De-registering Qos Flow [src_addr: " + qosTrafficFlow.getSourceAddr() + 
				" dst_addr: " + qosTrafficFlow.getDestAddr() + 
				" bandwidth: " + qosTrafficFlow.getBandwidth() +
				"]");
		
		/* BOFUSS (Meter Enabled Switch) */
        IOFSwitch sw = switchService.getSwitch(qosTrafficFlow.getDpidMeterSwitch()); /* The IOFSwitchService */
        
        removeMeter(sw, qosTrafficFlow.getMeterId());		
		removeGoToMeterFlow(sw, qosTrafficFlow.getMeterId(), qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());
        removeSetTosFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr());

		/* OvS (Queue Enabled Switch) */
        sw = switchService.getSwitch(qosTrafficFlow.getDpidQueueSwitch()); /* The IOFSwitchService */

        // Packets remarked by the meter band with prec_level=1 will have ip_dscp=4, while non remarked packet will have ip_dscp=2
		removeEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_LESS_EFFORT_QUEUE, IpDscp.DSCP_4);
		removeEnqueueBasedOnDscpFlow(sw, qosTrafficFlow.getSourceAddr(), qosTrafficFlow.getDestAddr(), QOS_SWITCH_QOS_QUEUE, IpDscp.DSCP_2);
		
		return true;
	}
	
	@Override
	public Map<String, BigInteger> getNumPacketsHandledPerTrafficClass(DatapathId dpid) {		
		Map<String, BigInteger> classStats = new HashMap<String, BigInteger>();
		IOFSwitch sw = switchService.getSwitch(dpid);		
		OFFactory factory = sw.getOFFactory();
		
		OFQueueStatsRequest sr = factory.buildQueueStatsRequest()
				.setQueueId(-1)	// Any queues
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
				
        log.info("The QoS classes statistics has been provided");
        
		return classStats;
	}
}
