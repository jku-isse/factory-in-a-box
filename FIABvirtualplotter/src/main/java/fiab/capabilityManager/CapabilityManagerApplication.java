package fiab.capabilityManager;

import akka.actor.ActorSystem;
import fiab.capabilityManager.tool.CapabilityManagerActor;

import java.net.URI;
import java.net.URISyntaxException;

//import fiab.capabilityTool.gui.CapabilityManagerUI;

//http://factory-in-a-box.fiab/capabilities/plot/color/BLACK -> for example Black capability
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
        system.actorOf(CapabilityManagerActor.props(resourceUri));
    }
}
