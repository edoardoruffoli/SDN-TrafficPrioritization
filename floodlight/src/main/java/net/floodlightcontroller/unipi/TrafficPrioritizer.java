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
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFlags;
import org.projectfloodlight.openflow.protocol.OFMeterMod;
import org.projectfloodlight.openflow.protocol.OFMeterModCommand;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPacketQueue;
import org.projectfloodlight.openflow.protocol.OFQueueGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFQueueGetConfigRequest;
import org.projectfloodlight.openflow.protocol.OFQueueStatsEntry;
import org.projectfloodlight.openflow.protocol.OFQueueStatsReply;
import org.projectfloodlight.openflow.protocol.OFQueueStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwTos;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.actionid.OFActionIdSetField;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionMeter;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBand;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDrop;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDscpRemark;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDscpRemark.Builder;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandExperimenter;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.queueprop.OFQueueProp;
import org.projectfloodlight.openflow.protocol.queueprop.OFQueuePropMaxRate;
import org.projectfloodlight.openflow.protocol.queueprop.OFQueuePropMinRate;
import org.projectfloodlight.openflow.protocol.ver13.OFQueuePropertiesSerializerVer13;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.staticentry.StaticEntryPusher;

public class TrafficPrioritizer implements IFloodlightModule, IOFMessageListener, ITrafficPrioritizerREST {
	
	// Floodlight services used by the module
	protected IFloodlightProviderService floodlightProvider; // Reference to the provider
	protected IRestApiService restApiService; // Reference to the Rest API service
	protected IOFSwitchService switchService;
	
	// Logger
	protected static Logger log;
	
	//private switchEnabled;
	
	// Flow Table
	/* <IPSource, IPDestination, bandwidth> */	
	private FlowManager flowManager = new FlowManager(true);
	
	
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
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		
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
			System.out.printf("TRAFFIC PRIORITIZER MAC Address: {%s} seen on switch: {%s}\n",
			HexString.toHexString(sourceMACHash),
			sw.getId());
						
			// Dissect Packet included in Packet-In
			IPacket pkt = eth.getPayload();

			break;
		default:
			break;
		}
		return Command.CONTINUE;
	}
	
	@Override
	public boolean enableTrafficPrioritization(String dpid) {
		
		return true;
		
	}
	
	public boolean disableTrafficPrioritization(String dpid) {
		return true;
	}
	
	
	@Override
	public List<QoSFlow> getFlows(){
    	List<QoSFlow> info = new ArrayList<>();

        for (QoSFlow flow : flowManager.getFlows()) {
            info.add(flow);
        }

        log.info("The list of flows has been provided.");
    	return info;
	} 
	
	
	@Override
	public boolean registerFlow(QoSFlow qosflow) {
		if (!flowManager.addFlow(qosflow))
			return false;
		
		// Check if switch implements qos
		
		log.info("Registering QoS Flow [source_addr: " + qosflow.getSourceAddress() + " dest: " + qosflow.getDestAddress());
		
		// Bofuss (Meter Enabled Switch)
        IOFSwitch sw = switchService.getSwitch(DatapathId.of(6)); /* The IOFSwitchService */
        int meterId = 1;
        
		createMeter(sw, meterId, qosflow.getBandwidth(), /*burst*/0);		
		installGoToMeterFlow(sw, meterId, qosflow);
        installSetTosFlow(sw, qosflow);
	
        /*
         * packets remarked by the meter band with prec_level=1 will have ip_dscp=4, while non remarked packet will have ip_dscp=2.
         */
		// OvS (Queue Enabled Switch)
        sw = switchService.getSwitch(DatapathId.of(7)); /* The IOFSwitchService */
        
        /* Flow that are not conforming to the registered QoS traffic bandwidth are enqueued in the lowest priority queue */ 
		installEnqueueBasedOnDscpFlow(sw, qosflow, 1, IpDscp.DSCP_4);
		
		/* Flow that are conforming to the registered traffic parameters are enqueued in the highest priority queue */
		installEnqueueBasedOnDscpFlow(sw, qosflow, 2, IpDscp.DSCP_2);
		
		return true;	
	}
	
	@Override
	public boolean deregisterFlow(QoSFlow qosflow) {
		if (!flowManager.removeFlow(qosflow))
			return false;
		
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
	private void installSetTosFlow(final IOFSwitch sw, QoSFlow qosflow) {
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
	        .setExact(MatchField.IPV4_SRC, qosflow.getSourceAddress())
	        .setExact(MatchField.IPV4_DST, qosflow.getDestAddress());
			 
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
	 * 
	 * @param sw
	 * @param meterId
	 * @param qosflow
	 * @return
	 */
	private void installGoToMeterFlow(final IOFSwitch sw, final long meterId, QoSFlow qosflow) {
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
	        .setExact(MatchField.IPV4_SRC, qosflow.getSourceAddress())
	        .setExact(MatchField.IPV4_DST, qosflow.getDestAddress());
			 
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
	 * @param qosflow
	 * @return
	 */
	private void installEnqueueBasedOnDscpFlow(final IOFSwitch sw, QoSFlow qosflow, int queueId, IpDscp dscp) {
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
	        .setExact(MatchField.IPV4_SRC, qosflow.getSourceAddress())
	        .setExact(MatchField.IPV4_DST, qosflow.getDestAddress())
	        .setExact(MatchField.IP_DSCP, dscp);
			 
		OFFlowAdd flowAdd = factory.buildFlowAdd()
			.setMatch(matchBuilder.build())
		    .setActions(actions)
		    .setPriority(1)
		    .build();
		
		 /* Send meter modification message to switch */
        sw.write(flowAdd);
	}
	
	@Override
	public Map<String, BigInteger> getNumPacketsHandled() {		
		/*
		 * OFQueueStats has a bug with TCLink of mininet issue, so we used FLow stats , PROBLEM
		 */
		log.info("Class statistics requested");
		
		IOFSwitch sw = switchService.getSwitch(DatapathId.of(7));
		OFFactory factory = sw.getOFFactory();
		
		Map<String, BigInteger> classStats = new HashMap();
		
		Match match = sw.getOFFactory().buildMatch().build();
		OFFlowStatsRequest sr = factory.buildFlowStatsRequest()
                .setMatch(match)
                .setOutPort(OFPort.ANY)
                .setTableId(TableId.ALL)
                .setOutGroup(OFGroup.ANY)
				.build();
		ListenableFuture<List<OFFlowStatsReply>> future = sw.writeStatsRequest(sr);
		
		try {
			// Wait up to 10s for a reply; return when received; else exception thrown
		    List<OFFlowStatsReply> replies = future.get(10, TimeUnit.SECONDS);
		    
		    for (OFFlowStatsReply reply : replies) {
		        for (OFFlowStatsEntry e : reply.getEntries()) {
		        	IpDscp c = e.getMatch().get(MatchField.IP_DSCP);  
		 
		        	if (c == null)
		        		classStats.merge("Best Effort Traffic", e.getPacketCount().getBigInteger(), (a, b) -> a.add(b));
		        	else if (c.equals(IpDscp.DSCP_2))
		        		classStats.merge("Conformant QoS Traffic", e.getPacketCount().getBigInteger(), (a, b) -> a.add(b));
		        	else if(c.equals(IpDscp.DSCP_4))
			        	classStats.merge("Non-conformant QoS Traffic", e.getPacketCount().getBigInteger(), (a, b) -> a.add(b));
		        }
		    }
		} catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
		    e.printStackTrace();
		}
		
		return classStats;
	}
}
