import actors.OPCUATurntableRootActor;
import akka.actor.ActorSystem;

public class TurnTableApplication /*extends JFrame*/ {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA");
        system.actorOf(OPCUATurntableRootActor.props("Turntable1"));
        /*
        InterMachineEventBus interMachineEventBus = new InterMachineEventBus();
        StatePublisher statePublisher = newStatus -> System.out.println("STATE_PUBLISHER - NEW STATUS: " + newStatus);

        ActorSystem system = ActorSystem.create("tt_root");
        ActorRef ttActor = system.actorOf(TurntableActor.props(interMachineEventBus, statePublisher));
        ActorRef convActor = system.actorOf(ConveyorActor.props(interMachineEventBus, statePublisher));

        JFrame frame = new JFrame("Robot Control");

        //Turntable buttons with events
        JButton resetTTButton = new JButton("RESET_TURNING");
        resetTTButton.addActionListener(e -> {
            ttActor.tell(new GenericMachineRequests.Reset("1"), ActorRef.noSender());
        });

        JButton turnTTButton = new JButton("TURN_TURNING");
        turnTTButton.addActionListener(e -> {
            ttActor.tell(new TurnRequest(TurnTableOrientation.EAST), ActorRef.noSender());
        });

        JButton stopTTButton = new JButton("STOP_TURNING");
        stopTTButton.addActionListener(e -> {
            ttActor.tell(new GenericMachineRequests.Stop("1"), ActorRef.noSender());
        });

        //Conveyor buttons and events
        JButton resetCButton = new JButton("RESET_CONVEYOR");
        resetCButton.addActionListener(e -> {
            convActor.tell(ConveyorTriggers.RESET, ActorRef.noSender());
        });

        JButton loadCButton = new JButton("LOAD_CONVEYOR");
        loadCButton.addActionListener(e -> {
            convActor.tell(ConveyorTriggers.LOAD, ActorRef.noSender());
        });

        JButton unloadCButton = new JButton("UNLOAD_CONVEYOR");
        unloadCButton.addActionListener(e -> {
            convActor.tell(ConveyorTriggers.UNLOAD, ActorRef.noSender());
        });

        JButton stopCButton = new JButton("STOP_CONVEYOR");
        stopCButton.addActionListener(e -> {
            convActor.tell(ConveyorTriggers.STOP, ActorRef.noSender());
        });

        //Set layout and close op, add buttons, pack frame and show
        frame.getContentPane().setLayout(new GridLayout(7, 1));
        frame.getContentPane().add(resetTTButton);
        frame.getContentPane().add(turnTTButton);
        frame.getContentPane().add(stopTTButton);

        frame.getContentPane().add(resetCButton);
        frame.getContentPane().add(loadCButton);
        frame.getContentPane().add(unloadCButton);
        frame.getContentPane().add(stopCButton);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);*/

    }
}
