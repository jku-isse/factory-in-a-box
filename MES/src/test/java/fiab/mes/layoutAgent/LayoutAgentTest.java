package fiab.mes.layoutAgent;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.DateTime;
import akka.pattern.Patterns;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.layoutAgent.msg.*;
import fiab.mes.machine.MachineEntryActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.layout.ShopfloorLayout;
import fiab.mes.shopfloor.utils.TransportPositionLookupAndParser;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.transport.actor.transportmodule.InternalCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.opcua.CapabilityImplementationMetadata;
import layoutTracker.utils.ImageSequenceGenerator;
import layoutTracker.utils.MockCamera;
import layoutTracker.utils.MockMJPEGVideoCamera;
import layoutTracker.utils.MockMP4VideoCamera;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LayoutAgentTest {
    //static String imageSource = new File(LayoutAgentTest.class.getClassLoader().getResource("testImages/lab").getFile()).toPath().toString();
    private final static String imageSource = "MES/build/resources/test/testImages/lab/";
    private final static String videoSource = "MES/build/resources/test/testVideos/fiab/";
    private final static int expectedTTs = 2;

    private static final AtomicInteger discoveryCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        //Set up camera and graphstream ui
        System.setProperty("org.graphstream.ui", "swing");
        ImageSequenceGenerator camera = new MockCamera(imageSource, "");
        //ImageSequenceGenerator camera = new MockMJPEGVideoCamera(videoSource + "FIAB_9.mp4");   //7 is reduced, 4 is original layout
        //Create shopfloor
        ActorSystem system = ActorSystem.create("LayoutTrackerSystem");
        ActorRef intraMachineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ShopfloorLayout layout = new DefaultTestLayout(system, intraMachineEventBus);
        layout.initializeParticipants();
        LookupAndRouting lookupAndRouting = new LookupAndRouting(layout.getTransportPositionLookup(), layout.getTransportRoutingAndMapping());
        //Create MES
        ActorRef orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef coordActor = system.actorOf(AdaptedTransportSystemCoordinatorActor.props(lookupAndRouting.transportRoutingAndMapping, lookupAndRouting.transportPositionLookupAndParser, expectedTTs), AdaptedTransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef orderPlanningActor = system.actorOf(AdaptedOrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef orderEntryActor = system.actorOf(OrderEntryActor.props(), OrderEntryActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef machineEntryActor = system.actorOf(AdaptedMachineEntryActor.props(), MachineEntryActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef layoutRepositoryActor = system.actorOf(LayoutRepositoryActor.props(lookupAndRouting.getTransportPositionLookupAndParser(), lookupAndRouting.getTransportRoutingAndMapping()), LayoutRepositoryActor.WELLKNOWN_LOOKUP_NAME);
        //Create LayoutTracker
        ActorRef layoutTracker = system.actorOf(LayoutTrackerActor.props(system, intraMachineEventBus, camera));
        //Create Proxy Spawners
        Set<ActorRef> proxySpawners = new HashSet<>();
        //Create UI
        JFrame frame = new JFrame("Commander in a box");
        frame.setLayout(new BorderLayout());
        JButton nextButton = new JButton("NextImage");
        nextButton.addActionListener(e -> layoutTracker.tell(new ProcessNextImage(false), ActorRef.noSender()));
        JButton allButton = new JButton("PlayAll");
        allButton.addActionListener(e -> layoutTracker.tell(new ProcessNextImage(true), ActorRef.noSender()));
        JButton clearButton = new JButton("Clear Layout");
        clearButton.addActionListener(e -> layoutTracker.tell(new ClearLayoutRequest(), ActorRef.noSender()));
        JButton orderButton = new JButton("Place Order");
        orderButton.addActionListener(e -> orderEntryActor.tell(createOrderRequest(DateTime.now().toIsoDateTimeString()), ActorRef.noSender()));
        JPanel machinePanel = new JPanel(new BorderLayout());
        JPanel layoutPanel = new JPanel(new BorderLayout());
        JButton resetButton = new JButton("Reset Machines");
        resetButton.addActionListener(e -> layout.getParticipants().forEach(participantInfo -> {
            String machineId = participantInfo.getDiscoveryEndpoint() + "/" + participantInfo.getMachineId();
            machineEntryActor.tell(new GenericMachineRequests.Reset(machineId), ActorRef.noSender());
        }));
        JButton stopButton = new JButton("Stop Machines");
        stopButton.addActionListener(e -> layout.getParticipants().forEach(participantInfo -> {
            String machineId = participantInfo.getDiscoveryEndpoint() + "/" + participantInfo.getMachineId();
            machineEntryActor.tell(new GenericMachineRequests.Stop(machineId), ActorRef.noSender());
        }));
        JButton runDiscoveryButton = new JButton("Discover Machines");
        runDiscoveryButton.addActionListener(e -> {
            proxySpawners.forEach(proxy -> system.stop(proxy));
            proxySpawners.clear();
            Patterns.ask(layoutRepositoryActor, new GetLayoutRequest(), Duration.ofSeconds(3)).thenApply(resp -> {
                GetLayoutResponse response = (GetLayoutResponse) resp;
                proxySpawners.addAll(triggerDiscoveryMechanism(system, response.getTransportPositionLookup(), response.getTransportRouting()));
                return response;
            });
        });
        machinePanel.add(resetButton, BorderLayout.EAST);
        machinePanel.add(runDiscoveryButton, BorderLayout.CENTER);
        machinePanel.add(stopButton, BorderLayout.WEST);
        layoutPanel.add(nextButton, BorderLayout.EAST);
        layoutPanel.add(clearButton, BorderLayout.CENTER);
        layoutPanel.add(allButton, BorderLayout.WEST);
        frame.add(orderButton, BorderLayout.NORTH);
        frame.add(layoutPanel, BorderLayout.CENTER);
        frame.add(machinePanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("ImageSource");
        JMenuItem loadImageMenuItem = new JMenuItem("Load Images");
        JMenuItem loadVideoMenuItem = new JMenuItem("Load Video");
        loadImageMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir") + "\\MES\\src\\test\\resources");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if(fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
                layoutTracker.tell(new ChangeImageSequenceSource(new MockCamera(fileChooser.getSelectedFile().getAbsolutePath(), "")), ActorRef.noSender());
            }
        });
        loadVideoMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir") + "\\MES\\src\\test\\resources");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if(fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
                layoutTracker.tell(new ChangeImageSequenceSource(new MockMJPEGVideoCamera(fileChooser.getSelectedFile().getAbsolutePath())), ActorRef.noSender());
            }
        });
        menu.add(loadImageMenuItem);
        menu.add(loadVideoMenuItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        //layoutTracker.tell(new ProcessNextImage(false), ActorRef.noSender());
    }

    public static Set<ActorRef> triggerDiscoveryMechanism(ActorSystem system, TransportPositionParser transportPositionParser, InternalCapabilityToPositionMapping icmp) {
        Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
        ShopfloorConfigurations.addSpawners(capURI2Spawning, transportPositionParser, icmp);
        List<String> endpoints = new ArrayList<>();
        endpoints.add("opc.tcp://127.0.0.1:4840");
        endpoints.add("opc.tcp://127.0.0.1:4841");
        endpoints.add("opc.tcp://127.0.0.1:4842");
        endpoints.add("opc.tcp://127.0.0.1:4843");

        endpoints.add("opc.tcp://127.0.0.1:4844");
        endpoints.add("opc.tcp://127.0.0.1:4845");
        endpoints.add("opc.tcp://127.0.0.1:4846");
        endpoints.add("opc.tcp://127.0.0.1:4847");

        int currentDiscoveryCount = discoveryCounter.incrementAndGet();
        return endpoints.stream().map(ep -> createDiscoveryActor(ep, capURI2Spawning, system, currentDiscoveryCount)).collect(Collectors.toSet());
    }

    private static ActorRef createDiscoveryActor(String endpointURL, Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning, ActorSystem system, int discoveryCount) {
        ActorRef discoveryActor = system.actorOf(AdaptedCapabilityDiscoveryActor.props(), "Spawner" + endpointURL.split(":")[2] + "-" + discoveryCount);
        discoveryActor.tell(new AdaptedCapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), ActorRef.noSender());
        return discoveryActor;
    }

    private static RegisterProcessRequest createOrderRequest(String orderId) {
        OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleRedStepProcess(orderId));
        return new RegisterProcessRequest(orderId, op1, ActorRef.noSender());
    }

    static class LookupAndRouting {
        private TransportPositionLookupAndParser transportPositionLookupAndParser;
        private TransportRoutingAndMappingInterface transportRoutingAndMapping;

        public LookupAndRouting(TransportPositionLookupAndParser transportPositionLookupAndParser, TransportRoutingAndMappingInterface transportRoutingAndMapping) {
            this.transportPositionLookupAndParser = transportPositionLookupAndParser;
            this.transportRoutingAndMapping = transportRoutingAndMapping;
        }

        public TransportPositionLookupAndParser getTransportPositionLookupAndParser() {
            return transportPositionLookupAndParser;
        }

        public void setTransportPositionLookupAndParser(TransportPositionLookupAndParser transportPositionLookupAndParser) {
            this.transportPositionLookupAndParser = transportPositionLookupAndParser;
        }

        public TransportRoutingAndMappingInterface getTransportRoutingAndMapping() {
            return transportRoutingAndMapping;
        }

        public void setTransportRoutingAndMapping(TransportRoutingAndMappingInterface transportRoutingAndMapping) {
            this.transportRoutingAndMapping = transportRoutingAndMapping;
        }
    }

    static class LayoutRepositoryActor extends AbstractActor {

        public static final String WELLKNOWN_LOOKUP_NAME = "LayoutRepositoryActor";

        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

        public static Props props(TransportPositionLookupAndParser transportPositionLookupAndParser, TransportRoutingAndMappingInterface transportRoutingAndMapping) {
            return Props.create(LayoutRepositoryActor.class, () -> new LayoutRepositoryActor(transportPositionLookupAndParser, transportRoutingAndMapping));
        }

        protected ActorSelection machineEventBus;
        private TransportPositionLookupAndParser transportPositionLookupAndParser;
        private TransportRoutingAndMappingInterface transportRoutingAndMapping;

        public LayoutRepositoryActor(TransportPositionLookupAndParser transportPositionLookupAndParser, TransportRoutingAndMappingInterface transportRoutingAndMapping) {
            this.transportPositionLookupAndParser = transportPositionLookupAndParser;
            this.transportRoutingAndMapping = transportRoutingAndMapping;
            getEventBusAndSubscribe();
        }

        private void getEventBusAndSubscribe() {
            SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(self().path().name(), "*"));
            machineEventBus = this.context().actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
            machineEventBus.tell(machineSub, getSelf());
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(RealTimeShopfloorLayout.class, msg -> {
                        log.info("Received new layout");
                        this.transportPositionLookupAndParser = msg.getTransportPositionLookup();
                        this.transportRoutingAndMapping = msg.getTransportRoutingAndMapping();
                    })
                    .match(GetLayoutRequest.class, req -> {
                        log.info("Layout info requested");
                        sender().tell(new GetLayoutResponse(transportPositionLookupAndParser, transportRoutingAndMapping), self());
                    })
                    .matchAny(msg -> log.debug("LayoutRepositoryActor received unknown message {}", msg))
                    .build();
        }
    }

}
