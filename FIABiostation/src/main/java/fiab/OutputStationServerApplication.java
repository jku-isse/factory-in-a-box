package fiab;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.iostation.OutputStationFactory;

public class OutputStationServerApplication {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("OUTPUT_STATION_ROOT_OPC_UA");
        ActorRef actor = OutputStationFactory.startStandaloneOutputStation(system, 4840, "OutputStation");
    }
}
