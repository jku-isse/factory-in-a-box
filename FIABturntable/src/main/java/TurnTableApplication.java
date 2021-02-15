import akka.actor.ActorSystem;

import fiab.turntable.opcua.OPCUATurntableRootActor;

public class TurnTableApplication {

    public static void main(String[] args) {
        if(args.length < 1 || args.length > 2){
             System.out.println("Turntable needs to be instantiated using a name that matches a wiring info" +
                     " and optionally provide the name for the json file (without wiringInfo suffix)");
             System.exit(-1);
        }
        ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA");
        int portOffset = 2;
        boolean exposeInternalControls = false;
        if(args.length == 1){
            system.actorOf(OPCUATurntableRootActor.props(args[0], portOffset, exposeInternalControls), "TurntableRoot");
        }
        if(args.length == 2){
            system.actorOf(OPCUATurntableRootActor.props(args[0], args[1], portOffset, exposeInternalControls), "TurntableRoot");
        }

        /*
        InterMachineEventBus interMachineEventBus = new InterMachineEventBus();
        StatePublisher statePublisher = newStatus -> System.out.println("STATE_PUBLISHER - NEW STATUS: " + newStatus);

        ActorSystem system = ActorSystem.create("tt_root");
        ActorRef ttActor = system.actorOf(TurntableActor.props(interMachineEventBus, statePublisher));
        ActorRef convActor = system.actorOf(ConveyorActor.props(interMachineEventBus, statePublisher));
        */

    }
}
