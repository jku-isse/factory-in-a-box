package fiab.mes.shopfloor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.msg.GenericMachineRequests;
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
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import org.apache.commons.httpclient.methods.multipart.Part;

import java.util.*;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;

/**
 * This class is meant to replace DefaultLayout as it provides more flexible integration
 */
public class DefaultTestLayout {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("Playground");
        DefaultTestLayout layout = new DefaultTestLayout(system);
        layout.initializeDefaultLayout();
        //layout.initializeDefaultLayoutWithProxies();
        layout.printParticipantInfos();
    }

    private final ActorSystem system;
    private final List<ParticipantInfo> participants;
    private final TransportRoutingAndMappingInterface transportRouting;
    private final TransportPositionLookupAndParser positionLookup;
    private final ActorRef interMachineEventBus;
    private final PositionMap positionMap;
    private final TurntableCapabilityToPositionMapping tt1mapping;
    private final TurntableCapabilityToPositionMapping tt2mapping;


    //Don't forget to call initialize!!!
    public DefaultTestLayout(ActorSystem system) {
        this.system = system;
        this.participants = new ArrayList<>();
        interMachineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);

        positionMap = createPositionMapForDefaultLayout();
        positionLookup = ShopfloorUtils.createPositionToPortMapping(positionMap);

        tt1mapping = createDefaultCapabilityToPositionMapTT1(positionMap);
        tt2mapping = createDefaultCapabilityToPositionMapTT2(positionMap);
        Map<Position, Set<Position>> routerConnections = createRouterConnections(positionMap);
        Map<Position, Position> edgeNodeMap = ShopfloorUtils.createEdgeNodeMappingFromRouterConnections(routerConnections);

        transportRouting = ShopfloorUtils.createRoutesAndCapabilityMapping(positionMap, edgeNodeMap, routerConnections, tt1mapping, tt2mapping);
    }

    public void initializeDefaultLayout() {
        participants.add(VirtualInputStationFactory.initInputStationAndReturnInfo(system, INPUT_STATION, positionMap));
        participants.add(VirtualOutputStationFactory.initOutputStationAndReturnInfo(system, OUTPUT_STATION, positionMap));
        participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_1, tt1mapping, positionMap, transportRouting));
        participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_2, tt2mapping, positionMap, transportRouting));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLACK, SupportedColors.BLACK, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLUE, SupportedColors.BLUE, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_GREEN, SupportedColors.GREEN, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_RED, SupportedColors.RED, positionMap));
    }

    public void initializeDefaultLayoutWithProxies() {
        //Start IO Stations
        participants.add(VirtualInputStationFactory.initInputStationWithProxyAndReturnInfo(system, INPUT_STATION, positionMap));
        participants.add(VirtualOutputStationFactory.initOutputStationWithProxyAndReturnInfo(system, OUTPUT_STATION, positionMap));
        //Start Machines
        participants.add(VirtualPlotterFactory.initPlotterWithProxyAndReturnInfo(system, PLOTTER_BLACK, SupportedColors.BLACK, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterWithProxyAndReturnInfo(system, PLOTTER_BLUE, SupportedColors.BLUE, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterWithProxyAndReturnInfo(system, PLOTTER_GREEN, SupportedColors.GREEN, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterWithProxyAndReturnInfo(system, PLOTTER_RED, SupportedColors.RED, positionMap));
        //Start Transport units. These should always be started last, since they wire via handshake to the other machines
        participants.add(VirtualTurntableFactory.initTurntableWithProxyAndReturnInfo(system, TURNTABLE_1, tt1mapping, positionMap, positionLookup, transportRouting));
        participants.add(VirtualTurntableFactory.initTurntableWithProxyAndReturnInfo(system, TURNTABLE_2, tt2mapping, positionMap, positionLookup, transportRouting));
    }

    public List<ParticipantInfo> getParticipants() {
        return participants;
    }

    public ActorRef getMachineById(String machineId) {
        for (ParticipantInfo info : participants) {
            if (info.getMachineId().equals(machineId)) {
                return info.getRemoteMachine().orElseThrow(() -> new RuntimeException("No remote machine registered under this id"));
            }
        }
        return ActorRef.noSender();
    }

    public ActorRef getMachineProxyById(String machineId) {
        for (ParticipantInfo info : participants) {
            if (info.getMachineId().equals(machineId)) {
                return info.getProxy().orElseThrow(() -> new RuntimeException("No proxy registered under this id"));
            }
        }
        return ActorRef.noSender();
    }

    public void resetParticipants() {
        for (ParticipantInfo info : participants) {
            info.getRemoteMachine().ifPresent(machine -> machine.tell(new ResetRequest(info.getMachineId()), ActorRef.noSender()));
        }
    }

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

    public ActorRef getInterMachineEventBus() {
        return interMachineEventBus;
    }

    public void subscribeToMachineEventBus(ActorRef subscriber, String subscriberName) {
        interMachineEventBus.tell(new SubscribeMessage(subscriber, new MESSubscriptionClassifier(subscriberName, "*")), subscriber);
    }

    private PositionMap createPositionMapForDefaultLayout() {
        //Use ParticipantInfo instead
        PositionMap positionMap = new PositionMap();
        positionMap.addPositionMapping(INPUT_STATION, new Position("34"));
        positionMap.addPositionMapping(OUTPUT_STATION, new Position("35"));
        positionMap.addPositionMapping(TURNTABLE_1, new Position("20"));
        positionMap.addPositionMapping(TURNTABLE_2, new Position("21"));
        positionMap.addPositionMapping(PLOTTER_BLACK, new Position("31"));
        positionMap.addPositionMapping(PLOTTER_BLUE, new Position("32"));
        positionMap.addPositionMapping(PLOTTER_GREEN, new Position("37"));
        positionMap.addPositionMapping(PLOTTER_RED, new Position("38"));
        return positionMap;
    }

    private TurntableCapabilityToPositionMapping createDefaultCapabilityToPositionMapTT1(PositionMap positionMap) {
        TurntableCapabilityToPositionMapping mapping = new TurntableCapabilityToPositionMapping(positionMap.get(TURNTABLE_1).getPosition());
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_EAST_SERVER, positionMap.getPositionForId(TURNTABLE_2));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_WEST_CLIENT, positionMap.getPositionForId(INPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SOUTH_CLIENT, positionMap.getPositionForId(PLOTTER_GREEN));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_NORTH_CLIENT, positionMap.getPositionForId(PLOTTER_BLACK));
        return mapping;
    }

    private TurntableCapabilityToPositionMapping createDefaultCapabilityToPositionMapTT2(PositionMap positionMap) {
        TurntableCapabilityToPositionMapping mapping = new TurntableCapabilityToPositionMapping(positionMap.getPositionForId(TURNTABLE_2));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_EAST_CLIENT, positionMap.getPositionForId(OUTPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_WEST_CLIENT, positionMap.getPositionForId(TURNTABLE_1));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SOUTH_CLIENT, positionMap.getPositionForId(PLOTTER_RED));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_NORTH_CLIENT, positionMap.getPositionForId(PLOTTER_BLUE));
        return mapping;
    }

    public static Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> createRouterConnections(PositionMap positionMap) {
        Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections = new HashMap<>();
        routerConnections.put(positionMap.getPositionForId(TURNTABLE_1),
                Set.of(positionMap.getPositionForId(TURNTABLE_2),
                        positionMap.getPositionForId(INPUT_STATION),
                        positionMap.getPositionForId(PLOTTER_GREEN),
                        positionMap.getPositionForId(PLOTTER_BLACK)));

        routerConnections.put(positionMap.getPositionForId(TURNTABLE_2),
                Set.of(positionMap.getPositionForId(OUTPUT_STATION),
                        positionMap.getPositionForId(TURNTABLE_1),
                        positionMap.getPositionForId(PLOTTER_RED),
                        positionMap.getPositionForId(PLOTTER_BLUE)));

        return routerConnections;
    }


}
