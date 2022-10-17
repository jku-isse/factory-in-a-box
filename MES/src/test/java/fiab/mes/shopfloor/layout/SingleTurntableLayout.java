package fiab.mes.shopfloor.layout;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.shopfloor.participants.PositionMap;
import fiab.mes.shopfloor.participants.virtual.VirtualInputStationFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualOutputStationFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualPlotterFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualTurntableFactory;
import fiab.mes.shopfloor.utils.TurntableCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.TURNTABLE_2;

public class SingleTurntableLayout extends ShopfloorLayout{

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("Playground");
        ShopfloorLayout layout = new SingleTurntableLayout(system, system.actorOf(InterMachineEventBusWrapperActor.props(),
                InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME));
        layout.initializeParticipants();
        layout.printParticipantInfos();
    }

    public SingleTurntableLayout(ActorSystem system, ActorRef interMachineEventBus) {
        super(system, interMachineEventBus);
    }

    @Override
    public void initializeParticipants() {
        participants.add(VirtualInputStationFactory.initInputStationAndReturnInfo(system, INPUT_STATION, positionMap));
        participants.add(VirtualOutputStationFactory.initOutputStationAndReturnInfo(system, OUTPUT_STATION, positionMap));

        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_GREEN, WellknownPlotterCapability.SupportedColors.GREEN, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_RED, WellknownPlotterCapability.SupportedColors.RED, positionMap));

        participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_1, tt1mapping, positionMap, transportRouting));
    }

    @Override
    protected PositionMap createPositionMap() {
        PositionMap positionMap = new PositionMap();
        positionMap.addPositionMapping(INPUT_STATION, new TransportRoutingInterface.Position("34"));
        positionMap.addPositionMapping(OUTPUT_STATION, new TransportRoutingInterface.Position("21"));
        positionMap.addPositionMapping(TURNTABLE_1, new TransportRoutingInterface.Position("20"));
        positionMap.addPositionMapping(PLOTTER_GREEN, new TransportRoutingInterface.Position("31"));
        positionMap.addPositionMapping(PLOTTER_RED, new TransportRoutingInterface.Position("37"));
        return positionMap;
    }

    @Override
    protected TurntableCapabilityToPositionMapping createCapabilityToPositionMapTT1(PositionMap positionMap) {
        TurntableCapabilityToPositionMapping mapping = new TurntableCapabilityToPositionMapping(positionMap.get(TURNTABLE_1).getPosition());
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_EAST_CLIENT, positionMap.getPositionForId(OUTPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_WEST_CLIENT, positionMap.getPositionForId(INPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SOUTH_CLIENT, positionMap.getPositionForId(PLOTTER_RED));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_NORTH_CLIENT, positionMap.getPositionForId(PLOTTER_GREEN));
        return mapping;
    }

    @Override
    protected TurntableCapabilityToPositionMapping createCapabilityToPositionMapTT2(PositionMap positionMap) {
        //We don't use the second turntable here
        return new TurntableCapabilityToPositionMapping(TransportRoutingInterface.UNKNOWN_POSITION);
    }

    @Override
    protected Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> createRouterConnections(PositionMap positionMap) {
        Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections = new HashMap<>();
        routerConnections.put(positionMap.getPositionForId(TURNTABLE_1),
                Set.of(positionMap.getPositionForId(OUTPUT_STATION),
                        positionMap.getPositionForId(INPUT_STATION),
                        positionMap.getPositionForId(PLOTTER_GREEN),
                        positionMap.getPositionForId(PLOTTER_RED)));
        return routerConnections;
    }
}
