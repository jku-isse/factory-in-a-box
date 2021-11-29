package fiab.mes.transport.actor.transportsystem;

import com.google.common.collect.Lists;
import fiab.mes.transport.actor.transportmodule.InternalCapabilityToPositionMapping;

import java.util.*;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;

// FACTORY IN A BOX - SHOPFLOOR LAYOUT:
// Turntable TT1, TT2 and TT3 in the middle, 20,21,22
// Input(In) Outputstation(O) left and right + one for plot only: 37,43, 32
// Plotters(Pl) top left, bottom left*2, 31,37,38
// Folding(Fo) stations middle, 41*3
// Note: This will allow the handshake to address multiple foldingStations at different positions,
//      the actual transport is performed via the FoldingCellCoordinator
// Transit station(Tr), 42 - It is not connected to Folding Stations, but the fs will relocate the pallet here
// IP Addresses encode locations and remain fixed (USB lan adapter stay with their locations)
// 30(#)  | 31(Pl)   | 32(O)    | 33(#)    | 34(#)  | 35(#)  | 36(#)
// 37(In) | 38/2(TT) | 39/3(TT) | 40(Fo*3) | 41(Tr) | 42(TT) | 43(O)
// 44(#)  | 45(Pl)   | 46(Pl)   | 47(#)    | 48(#)  | 49(#)  | 50(#)

public class HardcodedFoldingTransportRoutingAndMapping implements TransportRoutingInterface, InternalCapabilityToPositionMapping {

    //TODO implement layout from above
    private final Map<Position, Position> edgeNodeMapping = new HashMap<>();
    private final Map<Position, Set<Position>> routerConnections = new HashMap<>();

    private final Position pos30 = new Position("30");  //Empty
    private final Position pos31 = new Position("31");
    private final Position pos32 = new Position("32");
    private final Position pos33 = new Position("33");  //Empty
    private final Position pos34 = new Position("34");  //Empty
    private final Position pos35 = new Position("35");  //Empty
    private final Position pos36 = new Position("36");  //Empty
    private final Position pos37 = new Position("37");
    private final Position pos38 = new Position("38");
    private final Position pos39 = new Position("39");
    private final Position pos40 = new Position("40");
    private final Position pos41 = new Position("41");
    private final Position pos42 = new Position("42");
    private final Position pos43 = new Position("43");
    private final Position pos44 = new Position("44");  //Empty
    private final Position pos45 = new Position("45");
    private final Position pos46 = new Position("46");
    private final Position pos47 = new Position("47");  //Empty
    private final Position pos48 = new Position("48");  //Empty
    private final Position pos49 = new Position("49");  //Empty
    private final Position pos50 = new Position("50");  //Empty
    private final Position pos51 = new Position("51");  //Empty

    public HardcodedFoldingTransportRoutingAndMapping() {
        setupHardCodedLayout();
        setupHardcodedCapabilityToPositionMapping();
    }

    private void setupHardCodedLayout() {
        edgeNodeMapping.put(pos31, pos38);  //Plotter to TT1
        edgeNodeMapping.put(pos37, pos38);  //Input to TT1
        edgeNodeMapping.put(pos45, pos38);  //Plotter to TT1
        edgeNodeMapping.put(pos38, pos39);  //TT1 to TT2

        edgeNodeMapping.put(pos32, pos39);  //Output1 to TT2
        edgeNodeMapping.put(pos46, pos39);  //Plotter to TT2
        edgeNodeMapping.put(pos40, pos39);  //Folding to TT2
        edgeNodeMapping.put(pos39, pos38);  //TT2 to TT1

        edgeNodeMapping.put(pos41, pos40);  //Tr to Folding
        edgeNodeMapping.put(pos42, pos41);  //TT3 to Tr
        edgeNodeMapping.put(pos43, pos42);  //Out to TT3

        routerConnections.put(pos38, Set.of(pos31, pos37, pos45, pos39));   //TT1 transport connections
        routerConnections.put(pos39, Set.of(pos32, pos46, pos40, pos38));   //TT2 transport connections
        routerConnections.put(pos42, Set.of(pos41, pos43));                 //TT3 transport connections
    }

    private final Map<String, Position> tt38map = new HashMap<>();
    private final Map<String, Position> tt39map = new HashMap<>();
    private final Map<String, Position> tt42map = new HashMap<>();
    private final Map<Position, String> pos38cap = new HashMap<>();
    private final Map<Position, String> pos39cap = new HashMap<>();
    private final Map<Position, String> pos42cap = new HashMap<>();

    private void setupHardcodedCapabilityToPositionMapping() {
        setupTT38CapabilityToPositionMapping();
        setupTT39CapabilityToPositionMapping();
        setupTT42CapabilityToPositionMapping();
    }

    private void setupTT38CapabilityToPositionMapping() {
        tt38map.put(TRANSPORT_MODULE_NORTH_CLIENT, pos31);
        tt38map.put(TRANSPORT_MODULE_SOUTH_CLIENT, pos45);
        tt38map.put(TRANSPORT_MODULE_WEST_CLIENT, pos37);
        tt38map.put(TRANSPORT_MODULE_EAST_SERVER, pos39);
        tt38map.put(TRANSPORT_MODULE_SELF, pos38);

        pos38cap.put(pos31, TRANSPORT_MODULE_NORTH_CLIENT);
        pos38cap.put(pos39, TRANSPORT_MODULE_EAST_SERVER);
        pos38cap.put(pos45, TRANSPORT_MODULE_SOUTH_CLIENT);
        pos38cap.put(pos37, TRANSPORT_MODULE_WEST_CLIENT);
        pos38cap.put(pos38, TRANSPORT_MODULE_SELF);
    }

    private void setupTT39CapabilityToPositionMapping() {
        tt39map.put(TRANSPORT_MODULE_NORTH_CLIENT, pos32);
        tt39map.put(TRANSPORT_MODULE_SOUTH_CLIENT, pos46);
        tt39map.put(TRANSPORT_MODULE_WEST_CLIENT, pos38);
        tt39map.put(TRANSPORT_MODULE_EAST_SERVER, pos40);
        tt39map.put(TRANSPORT_MODULE_SELF, pos39);

        pos39cap.put(pos32, TRANSPORT_MODULE_NORTH_CLIENT);
        pos39cap.put(pos40, TRANSPORT_MODULE_EAST_CLIENT);
        pos39cap.put(pos46, TRANSPORT_MODULE_SOUTH_CLIENT);
        pos39cap.put(pos38, TRANSPORT_MODULE_WEST_CLIENT);
        pos39cap.put(pos39, TRANSPORT_MODULE_SELF);
    }

    private void setupTT42CapabilityToPositionMapping() {
        tt42map.put(TRANSPORT_MODULE_EAST_CLIENT, pos43);
        tt42map.put(TRANSPORT_MODULE_WEST_CLIENT, pos41);
        tt42map.put(TRANSPORT_MODULE_SELF, pos42);

        pos42cap.put(pos43, TRANSPORT_MODULE_EAST_CLIENT);
        pos42cap.put(pos41, TRANSPORT_MODULE_WEST_CLIENT);
        pos42cap.put(pos42, TRANSPORT_MODULE_SELF);
    }

    private boolean isDirectlyConnected(Position pos1, Position pos2) {
        return (edgeNodeMapping.get(pos1).equals(pos2) || edgeNodeMapping.get(pos2).equals(pos1));
    }

    private boolean isSameRouter(Position pos1, Position pos2) {
        return edgeNodeMapping.get(pos1).equals(edgeNodeMapping.get(pos2));
    }

    // we limit ourselves to two turntables for now
    private List<Position> collectRouterConnections(Position pos1, Position pos2) throws RoutingException {
        //FIXME: only supports two hops thus distance 3, beyond that Routing exception throws, would need recursive search
        //collect for any of these positions their neighbors (need to be routers
        if (!routerConnections.containsKey(pos1))
            throw new RoutingException("Router not known:" + pos1, RoutingException.Error.UNKNOWN_POSITION);
        if (!routerConnections.containsKey(pos2))
            throw new RoutingException("Router not known:" + pos2, RoutingException.Error.UNKNOWN_POSITION);
        if (routerConnections.get(pos1).contains(pos2)) {
            return Lists.newArrayList(pos1, pos2);
        } else {
            HashSet<Position> copy1 = new HashSet<>(routerConnections.get(pos1));
            HashSet<Position> copy2 = new HashSet<>(routerConnections.get(pos2));
            copy1.retainAll(copy2); // copy1 now is the intersect/overlap
            if (copy1.isEmpty())
                throw new RoutingException(String.format("Route not found between default gateways %s and %s ", pos1, pos2), RoutingException.Error.NO_ROUTE);
            else { // return first option
                return Lists.newArrayList(pos1, copy1.stream().findAny().get(), pos2);
            }
        }
    }

    @Override
    public Position getPositionForCapability(String capabilityId, Position selfPos) {
        if (selfPos.equals(pos38)) {
            return tt38map.getOrDefault(capabilityId, TransportRoutingInterface.UNKNOWN_POSITION);
        } else if (selfPos.equals(pos39)) {
            return tt39map.getOrDefault(capabilityId, TransportRoutingInterface.UNKNOWN_POSITION);
        } else if (selfPos.equals(pos42)) {
            return tt42map.getOrDefault(capabilityId, TransportRoutingInterface.UNKNOWN_POSITION);
        } else return TransportRoutingInterface.UNKNOWN_POSITION;
    }

    @Override
    public Optional<String> getCapabilityIdForPosition(Position pos, Position selfPos) {
        if (selfPos.equals(pos38)) {
            return Optional.ofNullable(pos38cap.get(pos));
        } else if (selfPos.equals(pos39)) {
            return Optional.ofNullable(pos39cap.get(pos));
        } else if (selfPos.equals(pos42)) {
            return Optional.ofNullable(pos42cap.get(pos));
        } else return Optional.empty();
    }

    @Override
    public List<Position> calculateRoute(Position fromMachine, Position toMachine) throws RoutingException {
        // a route consists of a list of stations, starting with the fromMachineId, and ending with toMachineId, and any transportsystem hops inbetween
        if (!edgeNodeMapping.containsKey(fromMachine))
            throw new RoutingException("Source position not known", RoutingException.Error.UNKNOWN_POSITION);
        if (!edgeNodeMapping.containsKey(toMachine))
            throw new RoutingException("Source position not known", RoutingException.Error.UNKNOWN_POSITION);

        List<Position> route = new ArrayList<Position>();
        route.add(fromMachine);
        if (isDirectlyConnected(fromMachine, toMachine)) {
            //done
        } else if (isSameRouter(fromMachine, toMachine)) {
            route.add(edgeNodeMapping.get(fromMachine));
        } else { // Intermediary routers needed,
            route.addAll(collectRouterConnections(edgeNodeMapping.get(fromMachine), edgeNodeMapping.get(toMachine)));
        }
        route.add(toMachine);
        return route;
    }
}
