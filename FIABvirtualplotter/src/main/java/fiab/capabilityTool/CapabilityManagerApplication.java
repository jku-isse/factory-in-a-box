package fiab.capabilityTool;

import akka.actor.ActorSystem;
import fiab.capabilityTool.tool.CapabilityManagerActor;

import java.net.URI;
import java.net.URISyntaxException;

//import fiab.capabilityTool.gui.CapabilityManagerUI;

public class CapabilityManagerApplication {

    public static void main(String[] args) {
        URI resourceUri = null;
        try {
            //Path to folder containing json files
            resourceUri = CapabilityManagerApplication.class.getClassLoader().getResource("plotterCapabilities").toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ActorSystem system = ActorSystem.create("CapabilityTool");
        //system.actorOf(CapabilityManagerClient.props("opc.tcp://192.168.0.38:4840"));
        //system.actorOf(CapabilityManagerUI.props("CapabilityTool"));
        system.actorOf(CapabilityManagerActor.props(resourceUri));
    }
}
