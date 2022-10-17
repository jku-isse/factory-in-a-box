package fiab.mes.shopfloor.layout;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.participants.PositionMap;
import fiab.mes.shopfloor.participants.virtual.VirtualInputStationFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualOutputStationFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualPlotterFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualTurntableFactory;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import fiab.mes.shopfloor.utils.TransportPositionLookupAndParser;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.shopfloor.utils.TurntableCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

import java.util.*;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.TURNTABLE_2;

//For now, we assume the maximum number of turntables on the shopfloor is two
public abstract class ShopfloorLayout {

    protected final ActorSystem system;
    protected final List<ParticipantInfo> participants;
    protected final TransportRoutingAndMappingInterface transportRouting;
    protected final TransportPositionLookupAndParser positionLookup;
    protected final ActorRef interMachineEventBus;
    protected final PositionMap positionMap;
    protected final TurntableCapabilityToPositionMapping tt1mapping;
    protected final TurntableCapabilityToPositionMapping tt2mapping;

    public ShopfloorLayout(ActorSystem system, ActorRef interMachineEventBus) {
        this.system = system;
        this.participants = new ArrayList<>();
        this.interMachineEventBus = interMachineEventBus;

        positionMap = createPositionMap();
        positionLookup = ShopfloorUtils.createPositionToPortMapping(positionMap);

        tt1mapping = createCapabilityToPositionMapTT1(positionMap);
        tt2mapping = createCapabilityToPositionMapTT2(positionMap);

        Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections = createRouterConnections(positionMap);
        Map<TransportRoutingInterface.Position, TransportRoutingInterface.Position> edgeNodeMap = ShopfloorUtils.createEdgeNodeMappingFromRouterConnections(routerConnections);

        transportRouting = ShopfloorUtils.createRoutesAndCapabilityMapping(positionMap, edgeNodeMap, routerConnections, tt1mapping, tt2mapping);
    }

    /**
     * Starts all mock machines in the current layout. No proxy will be created.
     * Machines can be discovered via opc ua using the discovery actor for example (default: {@link CapabilityDiscoveryActor})
     */
    public abstract void initializeParticipants();

    /**
     * Starts all mock machines in the current layout and runs a discovery service to register proxies
     * All discovered proxies will send a MachineConnectedEvent via the interMachineEventBus to the testKit
     *
     * @param testKit ref to the testKit
     */
    public void initializeAndDiscoverParticipants(ActorRef testKit) {
        initializeParticipants();
        runDiscovery(testKit);
    }

    /**
     * Triggers a discovery mechanism for all machines using the discoveryUtil
     * All machines will be spawned as a child actor to the testKit ref
     * When a machine is discovered, it will send a MachineConnectedEvent the tester can process
     *
     * @param testKit ref to the TestKit
     */
    public void runDiscovery(ActorRef testKit) {
        DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, testKit, positionLookup, transportRouting);
        for (ParticipantInfo participant : participants) {
            discoveryUtil.discoverCapabilityForEndpoint(participant.getDiscoveryEndpoint());
        }
    }

    /**
     * Run discovery of remote machines using their opcua endpoint (e.g. 192.168.0.1:4840)
     * This can be used for system tests where no virtual participants have been started
     * Another use case includes additional discovery of remote machines for hybrid tests (virtual + physical machine)
     * @param testKit ref to the TestKit
     * @param remoteEndpoints opcua endpoints for remote machines
     */
    public void runRemoteDiscovery(ActorRef testKit, Set<String> remoteEndpoints){
        DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, testKit, positionLookup, transportRouting);
        for (String endpoint : remoteEndpoints) {
            discoveryUtil.discoverCapabilityForEndpoint(endpoint);
        }
    }

    /**
     * Starts a set of machines for a given id from {@link ShopfloorUtils}
     *
     * @param machineIds unique Ids for machines
     */
    public void initializeParticipantsForId(Set<String> machineIds) {   //Change constructor to String ... machineIds?
        Set<String> transportUnits = new HashSet<>();   //Turntable needs to be started last to wire correctly
        for (String machineId : machineIds) {
            switch (machineId) {
                case INPUT_STATION:
                    participants.add(VirtualInputStationFactory.initInputStationAndReturnInfo(system, INPUT_STATION, positionMap));
                    break;
                case OUTPUT_STATION:
                    participants.add(VirtualOutputStationFactory.initOutputStationAndReturnInfo(system, OUTPUT_STATION, positionMap));
                    break;
                case PLOTTER_BLACK:
                    participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLACK, WellknownPlotterCapability.SupportedColors.BLACK, positionMap));
                    break;
                case PLOTTER_BLUE:
                    participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLUE, WellknownPlotterCapability.SupportedColors.BLUE, positionMap));
                    break;
                case PLOTTER_GREEN:
                    participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_GREEN, WellknownPlotterCapability.SupportedColors.GREEN, positionMap));
                    break;
                case PLOTTER_RED:
                    participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_RED, WellknownPlotterCapability.SupportedColors.RED, positionMap));
                    break;
                case TURNTABLE_1:
                    transportUnits.add(TURNTABLE_1);
                    break;
                case TURNTABLE_2:
                    transportUnits.add(TURNTABLE_2);
                    break;
            }
        }
        //Now we start the turntables
        if (transportUnits.contains(TURNTABLE_1))
            participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_1, tt1mapping, positionMap, transportRouting));
        if (transportUnits.contains(TURNTABLE_2))
            participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_2, tt2mapping, positionMap, transportRouting));
    }

    /**
     * Starts a set of machines for a given id from {@link ShopfloorUtils}
     * In addition, discovery will be run for each individual machine and proxies will be registered
     * The testKit will receive a MachineConnectedEvent for each machine via the interMachineEventBus
     *
     * @param testKit    ref to the testKit
     * @param machineIds unique Ids for machines
     */
    public void initializeAndDiscoverParticipantsForId(ActorRef testKit, String... machineIds) {
        initializeParticipantsForId(Set.of(machineIds));
        runDiscovery(testKit);
    }

    /**
     * Retrieves all active participants
     *
     * @return active participants
     */
    public List<ParticipantInfo> getParticipants() {
        return participants;
    }

    /**
     * Retrieves the participantInfo for a given id
     * @param id machine id
     * @return participantInfo
     */
    public ParticipantInfo getParticipantForId(String id) {
        return participants.stream().filter(p -> p.getMachineId().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No participant for id " + id + " found"));
    }

    /**
     * Returns the number of available transport units
     * @return number of transport units
     */
    public int getAmountOfTransportUnits(){
        return Math.toIntExact(participants.stream()
                .filter(p -> p.getMachineId().equals(TURNTABLE_1) || p.getMachineId().equals(TURNTABLE_2))
                .count());
    }

    /**
     * Retrieves an actorRef of a machine for a given machineId
     * ActorRef.noSender() is returned if machine does not exist in the current layout
     *
     * @param machineId unique machineId (See {@link ShopfloorUtils})
     * @return actorRef for machine
     */
    public ActorRef getMachineById(String machineId) {
        for (ParticipantInfo info : participants) {
            if (info.getMachineId().equals(machineId)) {
                return info.getRemoteMachine().orElseThrow(() -> new RuntimeException("No remote machine registered under this id"));
            }
        }
        return ActorRef.noSender();
    }

    public String getMachineEndpoint(String machineId) {
        return participants.stream()
                .filter(p -> p.getMachineId().equals(machineId))
                .map(m -> m.getOpcUaPort())
                .map(p -> ParticipantInfo.localhostOpcUaPrefix.concat(String.valueOf(p)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException());
    }

    /**
     * Resets all machines
     */
    public void resetParticipants() {
        for (ParticipantInfo info : participants) {
            info.getRemoteMachine().ifPresent(machine -> machine.tell(new ResetRequest(info.getMachineId()), ActorRef.noSender()));
        }
    }

    /**
     * Prints all active participants
     */
    public void printParticipantInfos() {
        for (ParticipantInfo info : participants) {
            System.out.println(info);
        }
    }

    public TransportPositionLookupAndParser getTransportPositionLookup() {
        return positionLookup;
    }

    public TransportRoutingAndMappingInterface getTransportRoutingAndMapping() {
        return transportRouting;
    }

    /**
     * Subscribes an actor to the interMachineEventBus. Useful for testing
     *
     * @param subscriber     actor to be subscribed (usually instance of {@link akka.testkit.javadsl.TestKit})
     * @param subscriberName unique name for subscriber (use path().name() to guarantee unique name)
     */
    public void subscribeToInterMachineEventBus(ActorRef subscriber, String subscriberName) {
        interMachineEventBus.tell(new SubscribeMessage(subscriber, new MESSubscriptionClassifier(subscriberName, "*")), subscriber);
    }

    protected abstract PositionMap createPositionMap();

    protected abstract TurntableCapabilityToPositionMapping createCapabilityToPositionMapTT1(PositionMap positionMap);

    protected abstract TurntableCapabilityToPositionMapping createCapabilityToPositionMapTT2(PositionMap positionMap);

    protected abstract Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> createRouterConnections(PositionMap positionMap);


}
