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
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
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
	
	// Flow Table
	/* <IPSource, IPDestination, IPDSCPbits, (?) bandwidth> */	
	private FlowManager flowManager = new FlowManager(true);
	
	// Proactive flow addition
	protected StaticEntryPusher entryPusher;	// To initialize in init()
	
	// IP and MAC address for our logical load balancer
	private final static IPv4Address VIRTUAL_IP = IPv4Address.of("8.8.8.8");
	private final static MacAddress VIRTUAL_MAC = MacAddress.of("00:00:00:00:00:FE");
	
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
		
		// For every switch that implements qos
		
		log.info("Registering QoS Flow [source_addr: " + qosflow.getSourceAddress() + " dest: " + qosflow.getDestAddress());
		
		// Bofuss
        IOFSwitch sw = switchService.getSwitch(DatapathId.of(6)); /* The IOFSwitchService */
        int meterId = 1;
        
		installMeter(sw, meterId, qosflow.getBandwidth(), /*burst*/0);		
		installGoToMeterFlow(sw, meterId, qosflow);
        installSetDscpFlow(sw, qosflow);
		
		// OvS
        sw = switchService.getSwitch(DatapathId.of(7)); /* The IOFSwitchService */
        // DSCP 10
		installQoSFlow(sw, qosflow, 1, IpDscp.DSCP_10);
		// DSCP 48
		installQoSFlow(sw, qosflow, 2, IpDscp.DSCP_48);
		
		return true;	
	}
	
	@Override
	public boolean deregisterFlow(QoSFlow qosflow) {
		if (!flowManager.removeFlow(qosflow))
			return false;
		
		return true;
	}
	
	private void installSetDscpFlow(final IOFSwitch sw, QoSFlow qosflow) {
		log.info("installing set dscp flow flow instruction on switch "+ sw.getId());
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		/*
		 * OpenFlow requires the switch evaluate the goto-meter instruction in a flow prior to any apply actions instruction. 
		 * This ensures the meter can perform its prescribed task (e.g. drop packet or DSCP remark) prior to potentially sending 
		 * the packet out. If a meter drops a packet, any further instructions in the flow will not be processed for that 
		 * particular packet.
		 */
		
		/* Meters only supported in OpenFlow 1.3 and up --> need 1.3+ factory */
		OFFactory factory = sw.getOFFactory();
		
		OFActionSetField dscp = factory.actions().buildSetField().setField(factory.oxms().buildIpDscp().setValue(IpDscp.DSCP_48).build()).build();	
		actions.add(dscp);
		
		OFInstructionApplyActions setDscp = factory.instructions().buildApplyActions()
				.setActions(actions)
				.build();
		instructions.add(setDscp);
		
		OFInstructionGotoTable goToTable = factory.instructions().buildGotoTable()
				.setTableId(TableId.of(1))
				.build();
		
		instructions.add(goToTable);
		
		
		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	        .setExact(MatchField.IPV4_SRC, qosflow.getSourceAddress())
	        .setExact(MatchField.IPV4_DST, qosflow.getDestAddress());
			 
		/* Flow will send matched packets to meter ID 1 and then possibly output on port 2 */
		OFFlowAdd flowAdd = factory.buildFlowAdd()
		    .setTableId(TableId.of(0))
		    .setPriority(1)
		    .setInstructions(instructions)
		    .setMatch(matchBuilder.build())
		    .build();
		
		 /* Send meter modification message to switch */
        sw.write(flowAdd);
	}
	
	/**
	 * Create and install a meter in a switch.
	 * @param sw 			the switch in which install the meter.
	 * @param meterId		the id of the meter
	 * @param rate			the maximum bandwidth rate
	 * @param burstSize		burst
	 */
	
	private void installMeter(final IOFSwitch sw, final long meterId, final long rate, 
			final long burstSize) {
		
		log.info("Installing meter " + meterId + " on switch "+ sw.getId());
		
		OFFactory factory = sw.getOFFactory();
        
		Set<OFMeterFlags> flags = new HashSet<>(Arrays.asList(OFMeterFlags.KBPS, OFMeterFlags.BURST));
        
        /* Create and set meter band */
        Builder bandBuilder = factory.meterBands().buildDscpRemark()
                .setRate(rate)
                .setPrecLevel((short) 10)
                .setBurstSize(burstSize);
        
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
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		/*
		 * OpenFlow requires the switch evaluate the goto-meter instruction in a flow prior to any apply actions instruction. 
		 * This ensures the meter can perform its prescribed task (e.g. drop packet or DSCP remark) prior to potentially sending 
		 * the packet out. If a meter drops a packet, any further instructions in the flow will not be processed for that 
		 * particular packet.
		 */
		
		/* Meters only supported in OpenFlow 1.3 and up --> need 1.3+ factory */
		OFFactory factory = sw.getOFFactory();
		
		/*
		OFInstructionGotoTable goToTable = factory.instructions().buildGotoTable()
				.setTableId(TableId.of(1))
				.build();*/
		
		OFInstructionMeter meter = factory.instructions().buildMeter()
			.setMeterId(meterId)
			.build();
		
		// If the meter does not apply (i.e. the qos flow is respected) the packets will be enqueued in QoS priority queue
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
			 
		/* Flow will send matched packets to meter ID 1 and then possibly output on port 2 */
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
	private void installQoSFlow(final IOFSwitch sw, QoSFlow qosflow, int queueId, IpDscp dscp) {
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

		// Use OFMeterBandStats (?)
		log.info("Queue statistics requested");
		Map<String, BigInteger> queueStats = new HashMap();
		
		IOFSwitch sw = switchService.getSwitch(DatapathId.of(7));
		OFFactory factory = sw.getOFFactory();
		
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
		    System.out.println(replies);
	/*	    for (OFFlowStatsReply reply : replies) {
		        for (OFFlowStatsReply e : reply.getEntries()) {
		            long id = e.getQueueId();
		            U64 txb = e.getTxBytes();
		            queueStats.put("queue" + String.valueOf(id), txb.getBigInteger());
		        }
		    }*/
		} catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
		    e.printStackTrace();
		}
		
		// Should return Best Effort and QoS statistics
		
		return queueStats;
		

	}
}
