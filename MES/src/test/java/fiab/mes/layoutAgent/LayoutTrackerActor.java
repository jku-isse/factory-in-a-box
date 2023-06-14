package fiab.mes.layoutAgent;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import commons.QrCodeUtils;
import commons.WiringEndpoint;
import fiab.mes.layoutAgent.msg.ChangeImageSequenceSource;
import fiab.mes.layoutAgent.msg.ClearLayoutRequest;
import fiab.mes.layoutAgent.msg.ProcessNextImage;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.participants.PositionMap;
import fiab.mes.shopfloor.utils.TurntableCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import layoutTracker.layout.LayoutTracker;
import layoutTracker.utils.ImageSequenceGenerator;
import layoutTracker.wiring.MachineCapabilityInfo;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import scala.Option;
import scala.collection.Seq;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

class LayoutTrackerActor extends AbstractActor {

    public static final String WELLKNOWN_LOOKUP_NAME = "LayoutTrackerActor";

    public static Props props(ActorSystem system, ActorRef interMachineEventBus, ImageSequenceGenerator camera) {
        return Props.create(LayoutTrackerActor.class, () -> new LayoutTrackerActor(system, interMachineEventBus, camera));
    }

    private final LayoutTracker layoutTracker;

    public LayoutTrackerActor(ActorSystem system, ActorRef interMachineEventBus, ImageSequenceGenerator camera) {
        this.layoutTracker = new LayoutTracker(camera, graph -> {
            interMachineEventBus.tell(updateShopfloorLayout(system, interMachineEventBus, graph), self());
        });
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ProcessNextImage.class, msg -> {
                    System.out.println("Processing next image...");
                    boolean hasNextImage = layoutTracker.processNextImage();
                    if (msg.isRepeat()) {
                        if (hasNextImage) {
                            self().tell(new ProcessNextImage(msg.isRepeat()), self());
                        } else {
                            System.out.println("No images available...");
                        }
                    }
                    if (!hasNextImage) {
                        System.out.println("No more images available...");
                    }
                })
                .match(ClearLayoutRequest.class, msg -> layoutTracker.clearGraph())
                .match(ChangeImageSequenceSource.class, msg -> {
                    layoutTracker.setImageSequenceGenerator(msg.getImageSequenceGenerator());
                })
                .build();
    }

    private RealTimeShopfloorLayout updateShopfloorLayout(ActorSystem system, ActorRef interMachineEventBus, Graph machineLayoutGraph) {
        final Set<Node> machineNodes = machineLayoutGraph.nodes()
                .filter(node -> String.valueOf(node.getAttribute("ui.class")).equals("machine"))
                .collect(Collectors.toSet());
        RealTimeShopfloorLayout layout = new RealTimeShopfloorLayout(system, interMachineEventBus) {

            @Override
            protected PositionMap createPositionMap() {
                int nextFreePositionId = 0;
                positionMap = new PositionMap();
                for (Node machineNode : machineNodes) {
                    Option<MachineCapabilityInfo> machineCapabilityInfo = MachineCapabilityInfo.parseToJson(machineNode.getId());
                    if (machineCapabilityInfo.isEmpty()) {
                        System.err.println("MachineNode " + machineNode + " has invalid id!");
                    }
                    String machineId = machineCapabilityInfo.get().machineId();
                    TransportRoutingInterface.Position position = parsePositionForEndpoint(machineCapabilityInfo.get().endpoint());
                    //TransportRoutingInterface.Position position = new TransportRoutingInterface.Position(String.valueOf(nextFreePositionId));
                    positionMap.addPositionMapping(machineId, position);
                    participants.add(new ParticipantInfo(machineId, position, Integer.parseInt(machineCapabilityInfo.get().endpoint().split(":")[2]), null));
                    nextFreePositionId++;
                }
                System.out.println("PositionMap: " + positionMap);
                return positionMap;
            }

            @Override
            protected HashMap<String, TurntableCapabilityToPositionMapping> createCapabilityToPositionMaps(PositionMap positionMap) {
                HashMap<String, TurntableCapabilityToPositionMapping> capabilityToPositionMappings = new HashMap<>();
                for (Node machineNode : machineNodes) {
                    Option<MachineCapabilityInfo> machineCapabilityInfoOption = MachineCapabilityInfo.parseToJson(machineNode.getId());
                    if (machineCapabilityInfoOption.isDefined()) {
                        MachineCapabilityInfo machineCapabilityInfo = machineCapabilityInfoOption.get();
                        if (machineCapabilityInfo.capabilityId().equals("DefaultTurntableCapabilityInstance")) {
                            Set<Node> endpoints = machineNode.neighborNodes().collect(Collectors.toSet());
                            TurntableCapabilityToPositionMapping mapping;
                            mapping = new TurntableCapabilityToPositionMapping(positionMap.getPositionForId(machineCapabilityInfo.machineId()));
                            for (Node endpoint : endpoints) {
                                Optional<Node> otherEndpoint = endpoint.neighborNodes()
                                        .filter(node -> String.valueOf(node.getAttribute("ui.class")).equals("wiringEndpoint"))
                                        .findFirst();
                                if (otherEndpoint.isPresent()) {
                                    Optional<Node> otherMachine = otherEndpoint.get().neighborNodes()
                                            .filter(node -> String.valueOf(node.getAttribute("ui.class")).equals("machine"))
                                            .findFirst();
                                    if (otherMachine.isPresent()) {
                                        Option<MachineCapabilityInfo> otherMachineCapabilityInfoOption = MachineCapabilityInfo.parseToJson(otherMachine.get().getId());
                                        if (otherMachineCapabilityInfoOption.isDefined()) {
                                            Option<WiringEndpoint> wiringEndpoint = QrCodeUtils.decodeWiringEndpointJson(endpoint.getId());
                                            if (wiringEndpoint.isDefined()) {
                                                TransportRoutingInterface.Position otherPosition = positionMap.getPositionForId(otherMachineCapabilityInfoOption.get().machineId());
                                                mapping.mapCapabilityToPosition(wiringEndpoint.get().localCapabilityId(), otherPosition);
                                            }
                                        }
                                    }
                                }
                            }
                            capabilityToPositionMappings.put(machineCapabilityInfo.machineId(), mapping);
                        }
                    }
                }
                return capabilityToPositionMappings;
            }

            @Override
            protected Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> createRouterConnections(PositionMap positionMap) {
                Map<TransportRoutingInterface.Position, Set<TransportRoutingInterface.Position>> routerConnections = new HashMap<>();
                for (Node machineNode : machineNodes) {
                    Option<MachineCapabilityInfo> machineCapabilityInfoOption = MachineCapabilityInfo.parseToJson(machineNode.getId());
                    if (machineCapabilityInfoOption.isDefined()) {
                        MachineCapabilityInfo machineCapabilityInfo = machineCapabilityInfoOption.get();
                        if (machineCapabilityInfo.capabilityId().equals("DefaultTurntableCapabilityInstance")) {
                            Set<Node> endpoints = machineNode.neighborNodes().collect(Collectors.toSet());
                            Set<TransportRoutingInterface.Position> connections = new HashSet<>();
                            for (Node endpoint : endpoints) {
                                Optional<Node> otherEndpoint = endpoint.neighborNodes()
                                        .filter(node -> String.valueOf(node.getAttribute("ui.class")).equals("wiringEndpoint"))
                                        .findFirst();
                                if (otherEndpoint.isPresent()) {
                                    Optional<Node> otherMachine = otherEndpoint.get().neighborNodes()
                                            .filter(node -> String.valueOf(node.getAttribute("ui.class")).equals("machine"))
                                            .findFirst();
                                    if (otherMachine.isPresent()) {
                                        Option<MachineCapabilityInfo> otherMachineCapabilityInfoOption = MachineCapabilityInfo.parseToJson(otherMachine.get().getId());
                                        if (otherMachineCapabilityInfoOption.isDefined()) {
                                            Option<WiringEndpoint> wiringEndpoint = QrCodeUtils.decodeWiringEndpointJson(endpoint.getId());
                                            if (wiringEndpoint.isDefined()) {
                                                connections.add(positionMap.getPositionForId(otherMachineCapabilityInfoOption.get().machineId()));
                                            }
                                        }
                                    }
                                }
                            }
                            routerConnections.put(positionMap.getPositionForId(machineCapabilityInfo.machineId()), connections);
                        }
                    }
                }
                return routerConnections;
            }

            private TransportRoutingInterface.Position parsePositionForEndpoint(String uriAsString) {
                try {
                    URI uri = new URI(uriAsString);
                    int port = uri.getPort();

                    /*TransportRoutingInterface.Position position = TransportRoutingInterface.UNKNOWN_POSITION;
                    for (ParticipantInfo info : positionMap.values()) {
                        if (port == info.getOpcUaPort()) {
                            position = info.getPosition();
                        }
                    }*/
                    return new TransportRoutingInterface.Position(String.valueOf(port - 4840));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return TransportRoutingInterface.UNKNOWN_POSITION;
            }

        };
        layout.init();
        return layout;
    }
}
