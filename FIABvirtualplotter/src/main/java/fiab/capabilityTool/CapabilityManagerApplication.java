package fiab.capabilityTool;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigException;
import fiab.capabilityTool.tool.CapabilityManagerActor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

//import fiab.capabilityTool.gui.CapabilityManagerUI;

//http://factory-in-a-box.fiab/capabilities/plot/color/BLACK -> Black capability
public class CapabilityManagerApplication {

    public static void main(String[] args) {
        URI resourceUri = null;
        try {
            //Path to folder containing json files
            resourceUri = Objects.requireNonNull(CapabilityManagerApplication.class.getClassLoader().getResource("plotterCapabilities")).toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NullPointerException ne){
            System.out.println("Could not find resource file");
            ne.printStackTrace();
            System.exit(1);
        }
        ActorSystem system = ActorSystem.create("CapabilityTool");
        system.actorOf(CapabilityManagerActor.props(resourceUri));
    }
}
