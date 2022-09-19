package fiab.mes.shopfloor.utils;

import com.google.common.collect.Lists;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

import java.util.*;

public class ShopfloorUtils {

    public static String OUTPUT_STATION = "OUTPUT_STATION";
    public static String INPUT_STATION = "INPUT_STATION";

    public static String TURNTABLE_1 = "TURNTABLE_1";
    public static String TURNTABLE_2 = "TURNTABLE_2";

    public static String PLOTTER_BLACK = "PLOTTER_BLACK";
    public static String PLOTTER_BLUE = "PLOTTER_BLUE";
    public static String PLOTTER_GREEN = "PLOTTER_GREEN";
    public static String PLOTTER_RED = "PLOTTER_RED";

    public static Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> createRouterConnections(Map<String, TransportRoutingInterface.Position> positionMap) {
        Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections = new HashMap<>();
        routerConnections.put(positionMap.get(TURNTABLE_1),
                Set.of(positionMap.get(TURNTABLE_2),
                        positionMap.get(INPUT_STATION),
                        positionMap.get(PLOTTER_GREEN),
                        positionMap.get(PLOTTER_BLACK)));
        routerConnections.put(positionMap.get(TURNTABLE_2),
                Set.of(positionMap.get(OUTPUT_STATION),
                        positionMap.get(TURNTABLE_1),
                        positionMap.get(PLOTTER_RED),
                        positionMap.get(PLOTTER_BLUE)));

        return routerConnections;
    }

    public static Map<TransportRoutingInterface.Position, TransportRoutingInterface.Position> createEdgeNodeMappingFromRouterConnections(Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections) {
        Map<TransportRoutingInterface.Position, TransportRoutingInterface.Position> edgeNodeMapping = new HashMap<>();
        Set<TransportRoutingInterface.Position> destinations = routerConnections.keySet();
        for (TransportRoutingInterface.Position destination : destinations) { //For each destination (router)
            for (TransportRoutingInterface.Position source : routerConnections.get(destination)) {
                //get all source positions and store the connection as a directed edge from node source to dest
                edgeNodeMapping.put(source, destination);
            }
        }
        return edgeNodeMapping;
    }

    public static TransportPositionLookupAndParser createPositionToPortMapping(final Map<String, TransportRoutingInterface.Position> positionMap) {
        //We create a new LookupInterface that returns the predefined positions in positionMap for a given actorId
        return new TransportPositionLookupAndParser() {
            @Override
            public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
                //FIXME Very ugly workaround. Would not work if there exist both TT1 and TT11 for example
                for(String machineId : positionMap.keySet()){
                    if(machineId.toLowerCase(Locale.ROOT).contains(machineId)) return positionMap.get(machineId);
                }
                return TransportRoutingInterface.UNKNOWN_POSITION;
            }

            @Override
            public TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
                for(String machineId : positionMap.keySet()){
                    if(machineId.toLowerCase(Locale.ROOT).contains(machineId)) return positionMap.get(machineId);
                }
                return TransportRoutingInterface.UNKNOWN_POSITION;
            }

            private final HashMap<TransportRoutingInterface.Position, AkkaActorBackedCoreModelAbstractActor> lookupTable = new HashMap<>();

            @Override
            public TransportRoutingInterface.Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor) {
                //Here we get the position for a known machine id defined in positionMap and store it in lookuptable
                TransportRoutingInterface.Position pos = positionMap.get(actor.getId());
                if (pos != TransportRoutingInterface.UNKNOWN_POSITION)
                    lookupTable.put(pos, actor);
                return pos;
            }

            @Override
            public Optional<AkkaActorBackedCoreModelAbstractActor> getActorForPosition(TransportRoutingInterface.Position pos) {
                return Optional.ofNullable(lookupTable.get(pos));
            }
        };
    }

    public static TransportRoutingAndMappingInterface createRoutesAndCapabilityMapping(Map<String, TransportRoutingInterface.Position> positionMap,
                                                                                 Map<TransportRoutingInterface.Position, TransportRoutingInterface.Position> edgeNodeMapping,
                                                                                 Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections,
                                                                                 TurntableCapabilityToPositionMapping tt1mapping,
                                                                                 TurntableCapabilityToPositionMapping tt2mapping) {
        return new TransportRoutingAndMappingInterface() {
            @Override
            public Position getPositionForCapability(String capabilityId, Position selfPos) {
                if (selfPos.equals(positionMap.get(TURNTABLE_1))) {    //In case we are tt1
                    return tt1mapping.getPositionForCapability(capabilityId);
                } else if (selfPos.equals(positionMap.get(TURNTABLE_2))) {  //In case we are tt2
                    return tt2mapping.getPositionForCapability(capabilityId);
                }
                return TransportRoutingAndMappingInterface.UNKNOWN_POSITION;
            }

            @Override
            public Optional<String> getCapabilityIdForPosition(Position pos, Position selfPos) {
                if (selfPos.equals(positionMap.get(TURNTABLE_1))) {    //In case we are tt1
                    return tt1mapping.getCapabilityForPosition(pos);
                } else if (selfPos.equals(positionMap.get(TURNTABLE_2))) {  //In case we are tt2
                    return tt2mapping.getCapabilityForPosition(pos);
                }
                return Optional.empty();
            }

            @Override
            public List<Position> calculateRoute(Position fromMachine, Position toMachine) throws RoutingException {
                if (!routerConnections.containsKey(fromMachine) || !routerConnections.getOrDefault(fromMachine, new HashSet<>()).contains(fromMachine))
                    throw new RoutingException("Source position not known", RoutingException.Error.UNKNOWN_POSITION);
                if (!edgeNodeMapping.containsKey(toMachine))
                    throw new RoutingException("Destination position not known", RoutingException.Error.UNKNOWN_POSITION);

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
        };
    }
}
