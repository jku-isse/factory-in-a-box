import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.turntable.TurntableFactory;

public class TurntableApplication {

    public static void main(String[] args) {
        if(args.length != 1){
             System.out.println("Turntable needs to be instantiated using a name that preferably matches a wiring info");
             System.exit(-1);
        }
        String ttName = args[0];

        ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPC_UA");
        ActorRef actor = TurntableFactory.startTurntable(system, new MachineEventBus(), 4840, ttName);
    }
}
