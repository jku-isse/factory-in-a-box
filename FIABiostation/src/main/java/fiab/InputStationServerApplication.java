package fiab;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.iostation.InputStationFactory;

public class InputStationServerApplication {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        ActorRef actor = InputStationFactory.startStandaloneInputStation(system, 4840, "InputStation");
    }
}
