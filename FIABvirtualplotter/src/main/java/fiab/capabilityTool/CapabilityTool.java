package fiab.capabilityTool;

import akka.actor.ActorSystem;
import fiab.capabilityTool.gui.CapabilityManagerUI;

public class CapabilityTool {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("CapabilityTool");
        //system.actorOf(CapabilityManagerClient.props("opc.tcp://192.168.178.57:4840"));
        system.actorOf(CapabilityManagerUI.props("CapabilityTool"));
    }
}
