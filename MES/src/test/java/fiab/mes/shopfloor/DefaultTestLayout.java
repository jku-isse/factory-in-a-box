package fiab.mes.shopfloor;

import akka.actor.ActorSystem;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.participants.virtual.VirtualInputStationFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualOutputStationFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualPlotterFactory;
import fiab.mes.shopfloor.participants.virtual.VirtualTurntableFactory;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import fiab.mes.shopfloor.utils.TransportPositionLookupAndParser;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.shopfloor.utils.TurntableCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

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
        layout.printParticipantInfos();
    }

    private final ActorSystem system;
    private final List<ParticipantInfo> participants;
    private TransportRoutingAndMappingInterface transportRouting;
    private TransportPositionLookupAndParser positionLookup;

    //Don't forget to call initialize!!!
    public DefaultTestLayout(ActorSystem system) {
        this.system = system;
        this.participants = new ArrayList<>();
    }

    public void initializeDefaultLayout() {
        //TODO first create all ports, then create the positionToPortMapping to parse ip address correctly!
        Map<String, Position> positionMap = createPositionMapForDefaultLayout();
        positionLookup = ShopfloorUtils.createPositionToPortMapping(positionMap);
        TurntableCapabilityToPositionMapping tt1mapping = createDefaultCapabilityToPositionMapTT1(positionMap);
        TurntableCapabilityToPositionMapping tt2mapping = createDefaultCapabilityToPositionMapTT2(positionMap);
        Map<Position, Set<Position>> routerConnections = ShopfloorUtils.createRouterConnections(positionMap);
        Map<Position, Position> edgeNodeMap = ShopfloorUtils.createEdgeNodeMappingFromRouterConnections(routerConnections);

        transportRouting = ShopfloorUtils.createRoutesAndCapabilityMapping(positionMap, edgeNodeMap, routerConnections, tt1mapping, tt2mapping);

        participants.add(VirtualInputStationFactory.initInputStationAndReturnInfo(system, INPUT_STATION, positionMap));
        participants.add(VirtualOutputStationFactory.initOutputStationAndReturnInfo(system, OUTPUT_STATION, positionMap));
        participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_1, positionMap, positionLookup, transportRouting));
        participants.add(VirtualTurntableFactory.initTurntableAndReturnInfo(system, TURNTABLE_2, positionMap, positionLookup, transportRouting));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLACK, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_BLUE, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_GREEN, positionMap));
        participants.add(VirtualPlotterFactory.initPlotterAndReturnInfo(system, PLOTTER_RED, positionMap));
    }

    public List<ParticipantInfo> getParticipants() {
        return participants;
    }

    public void printParticipantInfos() {
        for (ParticipantInfo info : participants) {
            System.out.println(info);
        }
    }

    public TransportPositionLookupAndParser getTransportPositionLookup(){
        return positionLookup;
    }

    public TransportRoutingAndMappingInterface getTransportRoutingAndMapping(){
        return transportRouting;
    }

    private Map<String, Position> createPositionMapForDefaultLayout() {
        Map<String, Position> positionMap = new HashMap<>();
        positionMap.put(INPUT_STATION, new Position("34"));
        positionMap.put(OUTPUT_STATION, new Position("35"));
        positionMap.put(TURNTABLE_1, new Position("20"));
        positionMap.put(TURNTABLE_2, new Position("21"));
        positionMap.put(PLOTTER_BLACK, new Position("31"));
        positionMap.put(PLOTTER_BLUE, new Position("32"));
        positionMap.put(PLOTTER_GREEN, new Position("37"));
        positionMap.put(PLOTTER_RED, new Position("38"));
        return positionMap;
    }

    private TurntableCapabilityToPositionMapping createDefaultCapabilityToPositionMapTT1(Map<String, Position> positionMap) {
        TurntableCapabilityToPositionMapping mapping = new TurntableCapabilityToPositionMapping(positionMap.get(TURNTABLE_1));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_EAST_SERVER, positionMap.get(TURNTABLE_2));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_WEST_CLIENT, positionMap.get(INPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SOUTH_CLIENT, positionMap.get(PLOTTER_GREEN));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_NORTH_CLIENT, positionMap.get(PLOTTER_BLACK));
        return mapping;
    }

    private TurntableCapabilityToPositionMapping createDefaultCapabilityToPositionMapTT2(Map<String, Position> positionMap) {
        TurntableCapabilityToPositionMapping mapping = new TurntableCapabilityToPositionMapping(positionMap.get(TURNTABLE_2));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_EAST_CLIENT, positionMap.get(OUTPUT_STATION));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_WEST_CLIENT, positionMap.get(TURNTABLE_1));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_SOUTH_CLIENT, positionMap.get(PLOTTER_RED));
        mapping.mapCapabilityToPosition(TRANSPORT_MODULE_NORTH_CLIENT, positionMap.get(PLOTTER_BLUE));
        return mapping;
    }


}
