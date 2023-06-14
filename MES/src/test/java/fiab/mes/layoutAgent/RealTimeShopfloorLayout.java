package fiab.mes.layoutAgent;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.collect.Lists;
import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.shopfloor.layout.ShopfloorLayout;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.participants.PositionMap;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import fiab.mes.shopfloor.utils.TransportPositionLookupAndParser;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.shopfloor.utils.TurntableCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.TURNTABLE_2;

public abstract class RealTimeShopfloorLayout extends MachineEvent {

    protected final ActorSystem system;
    protected final ActorRef interMachineEventBus;
    protected final List<ParticipantInfo> participants;

    protected TransportRoutingAndMappingInterface transportRouting;
    protected TransportPositionLookupAndParser positionLookup;

    protected PositionMap positionMap;
    protected HashMap<String, TurntableCapabilityToPositionMapping> ttMappings;

    protected int transportUnitCount;

    public RealTimeShopfloorLayout(ActorSystem system, ActorRef interMachineEventBus) {
        super("None", MachineEventType.UPDATED);
        this.system = system;
        this.interMachineEventBus = interMachineEventBus;
        this.participants = new ArrayList<>();
    }

    public void init() {
        positionMap = createPositionMap();
        positionLookup = createPositionToPortMapping(positionMap, participants);

        ttMappings = createCapabilityToPositionMaps(positionMap);
        transportUnitCount = ttMappings.size();

        Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections = createRouterConnections(positionMap);
        Map<TransportRoutingInterface.Position, TransportRoutingInterface.Position> edgeNodeMap = ShopfloorUtils.createEdgeNodeMappingFromRouterConnections(routerConnections);

        transportRouting = createRoutesAndCapabilityMapping(positionMap, edgeNodeMap, routerConnections, ttMappings);
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
     *
     * @param testKit         ref to the TestKit
     * @param remoteEndpoints opcua endpoints for remote machines
     */
    public void runRemoteDiscovery(ActorRef testKit, Set<String> remoteEndpoints) {
        DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, testKit, positionLookup, transportRouting);
        for (String endpoint : remoteEndpoints) {
            discoveryUtil.discoverCapabilityForEndpoint(endpoint);
        }
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
     *
     * @param id machine id
     * @return participantInfo
     */
    public ParticipantInfo getParticipantForId(String id) {
        return participants.stream().filter(p -> p.getMachineId().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No participant for id " + id + " found"));
    }

    /**
     * Returns the number of available transport units
     *
     * @return number of transport units
     */
    public int getAmountOfTransportUnits() {
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

    protected abstract HashMap<String, TurntableCapabilityToPositionMapping> createCapabilityToPositionMaps(PositionMap positionMap);

    protected abstract Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> createRouterConnections(PositionMap positionMap);

    private TransportRoutingAndMappingInterface createRoutesAndCapabilityMapping(PositionMap positionMap,
                                                                                 Map<TransportRoutingInterface.Position, TransportRoutingInterface.Position> edgeNodeMapping,
                                                                                 Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections,
                                                                                 HashMap<String, TurntableCapabilityToPositionMapping> ttMappings) {
        return new TransportRoutingAndMappingInterface() {
            @Override
            public Position getPositionForCapability(String capabilityId, Position selfPos) {
                /*if (selfPos.equals(positionMap.getPositionForId(TURNTABLE_1))) {    //In case we are tt1
                    return tt1mapping.getPositionForCapability(capabilityId);
                } else if (selfPos.equals(positionMap.getPositionForId(TURNTABLE_2))) {  //In case we are tt2
                    return tt2mapping.getPositionForCapability(capabilityId);
                }*/

                //return TransportRoutingAndMappingInterface.UNKNOWN_POSITION;
                if ((selfPos.equals(positionMap.getPositionForId(TURNTABLE_1))
                        || selfPos.equals(positionMap.getPositionForId(TURNTABLE_2))) &&
                        positionMap.getMachineIdForPosition(selfPos).isPresent()) {
                    if (ttMappings.containsKey(positionMap.getMachineIdForPosition(selfPos).get())) {
                        TurntableCapabilityToPositionMapping ttMapping = ttMappings.get(positionMap.getMachineIdForPosition(selfPos).get());
                        return ttMapping.getPositionForCapability(capabilityId);
                    }
                }
                return TransportRoutingAndMappingInterface.UNKNOWN_POSITION;
            }

            @Override
            public Optional<String> getCapabilityIdForPosition(Position pos, Position selfPos) {
                /*if (selfPos.equals(positionMap.getPositionForId(TURNTABLE_1))) {    //In case we are tt1
                    return tt1mapping.getCapabilityForPosition(pos);
                } else if (selfPos.equals(positionMap.getPositionForId(TURNTABLE_2))) {  //In case we are tt2
                    return tt2mapping.getCapabilityForPosition(pos);
                }*/
                if ((selfPos.equals(positionMap.getPositionForId(TURNTABLE_1))
                        || selfPos.equals(positionMap.getPositionForId(TURNTABLE_2))) &&
                        positionMap.getMachineIdForPosition(selfPos).isPresent()) {
                    if (ttMappings.containsKey(positionMap.getMachineIdForPosition(selfPos).get())) {
                        TurntableCapabilityToPositionMapping ttMapping = ttMappings.get(positionMap.getMachineIdForPosition(selfPos).get());
                        return ttMapping.getCapabilityForPosition(pos);
                    }
                }
                return Optional.empty();
            }

            @Override
            public List<Position> calculateRoute(Position fromMachine, Position toMachine) throws RoutingException {
                if (!edgeNodeMapping.containsKey(fromMachine))
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

    public static TransportPositionLookupAndParser createPositionToPortMapping(final PositionMap positionMap, final List<ParticipantInfo> participants) {
        //We create a new LookupInterface that returns the predefined positions in positionMap for a given actorId

        return new TransportPositionLookupAndParser() {
            private final HashMap<Integer, TransportRoutingInterface.Position> parserLookupTable = new HashMap<>();

            @Override
            public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
                TransportRoutingInterface.Position position = positionMap.getPositionForId(participants.stream()
                        .filter(participantInfo -> participantInfo.getDiscoveryEndpoint().equals(uriAsString))
                        .findFirst().get().getMachineId());
                return Objects.requireNonNullElse(position, TransportRoutingAndMappingInterface.UNKNOWN_POSITION);

                /*try {
                    URI uri = new URI(uriAsString);
                    int port = uri.getPort();
                    if (parserLookupTable.containsKey(port)) {
                        return parserLookupTable.get(port);
                    } else {
                        TransportRoutingInterface.Position position = TransportRoutingInterface.UNKNOWN_POSITION;
                        for (ParticipantInfo info : positionMap.values()) {
                            if (port == info.getOpcUaPort()) {
                                position = info.getPosition();
                                parserLookupTable.put(port, position);
                            }
                        }
                        return position;
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return TransportRoutingInterface.UNKNOWN_POSITION;*/
            }

            @Override
            public TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
                //return parsePosViaPortNr(uriAsString);
                TransportRoutingInterface.Position position = positionMap.getPositionForId(participants.stream()
                        .filter(participantInfo -> participantInfo.getDiscoveryEndpoint().equals(uriAsString))
                        .findFirst().get().getMachineId());
                return Objects.requireNonNullElse(position, TransportRoutingAndMappingInterface.UNKNOWN_POSITION);
            }

            private final HashMap<TransportRoutingInterface.Position, AkkaActorBackedCoreModelAbstractActor> lookupTable = new HashMap<>();

            @Override
            public TransportRoutingInterface.Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor) {
                //Here we get the position for a known machine id defined in positionMap and store it in lookuptable
                TransportRoutingInterface.Position pos = positionMap.get(actor.getModelActor().getActorName()).getPosition();
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
}
