package fiab.mes.shopfloor.layout;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
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

import java.util.*;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;

/**
 * This class is meant to replace DefaultLayout as it provides more flexible integration
 */
public class DefaultTestLayout extends ShopfloorLayout{

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("Playground");
        ShopfloorLayout layout = new DefaultTestLayout(system, system.actorOf(InterMachineEventBusWrapperActor.props(),
                InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME));
        layout.initializeParticipants();
        layout.printParticipantInfos();
    }

    /**
     * Creates the default layout used for the factory in a box
     * @param system actor system
     * @param interMachineEventBus inter machine event bus for sending messages to
     */
    public DefaultTestLayout(ActorSystem system, ActorRef interMachineEventBus) {
        super(system, interMachineEventBus);
    }

    @Override
    public void initializeParticipants() {
        participants.add(VirtualInputStationFactory.initInputStationAndReturnInfo(system, INPUT_STATION, positionMap));
        participants.add(VirtualOutputStationFactory.initOutputStationAndReturnInfo(system, OUTPUT_STATION, positionMap));

        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLACK, SupportedColors.BLACK, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLUE, SupportedColors.BLUE, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_GREEN, SupportedColors.GREEN, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_RED, SupportedColors.RED, positionMap));

        participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_1, tt1mapping, positionMap, transportRouting));
        participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_2, tt2mapping, positionMap, transportRouting));
    }

    @Override
    protected PositionMap createPositionMap() {
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

    @Override
    protected TurntableCapabilityToPositionMapping createCapabilityToPositionMapTT1(PositionMap positionMap) {
        TurntableCapabilityToPositionMapping mapping = new TurntableCapabilityToPositionMapping(positionMap.get(TURNTABLE_1).getPosition());
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_EAST_SERVER, positionMap.getPositionForId(TURNTABLE_2));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_WEST_CLIENT, positionMap.getPositionForId(INPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SOUTH_CLIENT, positionMap.getPositionForId(PLOTTER_GREEN));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_NORTH_CLIENT, positionMap.getPositionForId(PLOTTER_BLACK));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SELF, positionMap.getPositionForId(TURNTABLE_1));
        return mapping;
    }

    @Override
    protected TurntableCapabilityToPositionMapping createCapabilityToPositionMapTT2(PositionMap positionMap) {
        TurntableCapabilityToPositionMapping mapping = new TurntableCapabilityToPositionMapping(positionMap.getPositionForId(TURNTABLE_2));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_EAST_CLIENT, positionMap.getPositionForId(OUTPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_WEST_CLIENT, positionMap.getPositionForId(TURNTABLE_1));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SOUTH_CLIENT, positionMap.getPositionForId(PLOTTER_RED));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_NORTH_CLIENT, positionMap.getPositionForId(PLOTTER_BLUE));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SELF, positionMap.getPositionForId(TURNTABLE_2));
        return mapping;
    }

    protected Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> createRouterConnections(PositionMap positionMap) {
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
