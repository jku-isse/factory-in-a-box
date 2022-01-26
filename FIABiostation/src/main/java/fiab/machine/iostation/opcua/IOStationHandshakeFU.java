package fiab.machine.iostation.opcua;

import ev3dev.sensors.ev3.EV3ColorSensor;
import fiab.machine.iostation.EV3IOStationServerHandshakeActor;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.machine.iostation.IOStationServerHandshakeActor;
import fiab.opcua.server.OPCUABase;

public class IOStationHandshakeFU {

    static EV3ColorSensor colorSensor;

    public static void setColorSensor(EV3ColorSensor colorSensor){
        IOStationHandshakeFU.colorSensor = colorSensor;
    }

    public static class InputStationHandshakeFU extends ServerSideHandshakeFU {

        public InputStationHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef parentActor,
                                       ActorContext context, String capInstId, boolean exposeInternalControl, boolean isInputStation) {
            super(base, root, fuPrefix, parentActor, context, capInstId, OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, exposeInternalControl);
        }

        @Override
        protected void setupActor() {
            localClient = context.actorOf(IOStationServerHandshakeActor.propsForInputstation(parentActor, true, this), capInstId);
        }
    }

    public static class EV3InputStationHandshakeFU extends ServerSideHandshakeFU {

        public EV3InputStationHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef parentActor,
                                          ActorContext context, String capInstId, boolean exposeInternalControl, boolean isInputStation) {
            super(base, root, fuPrefix, parentActor, context, capInstId, OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, exposeInternalControl);
        }

        @Override
        protected void setupActor() {
            System.out.println("In FU: ColorSensor=" + colorSensor);
            localClient = context.actorOf(EV3IOStationServerHandshakeActor.propsForInputstation(parentActor, true, this, colorSensor), capInstId);
        }
    }

    public static class OutputStationHandshakeFU extends ServerSideHandshakeFU {

        public OutputStationHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef parentActor,
                                        ActorContext context, String capInstId, boolean exposeInternalControl, boolean isInputStation) {
            super(base, root, fuPrefix, parentActor, context, capInstId, OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, exposeInternalControl);
        }

        @Override
        protected void setupActor() {
            localClient = context.actorOf(IOStationServerHandshakeActor.propsForOutputstation(parentActor, true, this), capInstId);
        }
    }

    public static class EV3OutputStationHandshakeFU extends ServerSideHandshakeFU {

        public EV3OutputStationHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef parentActor,
                                           ActorContext context, String capInstId, boolean exposeInternalControl, boolean isInputStation) {
            super(base, root, fuPrefix, parentActor, context, capInstId, OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, exposeInternalControl);
        }

        @Override
        protected void setupActor() {
            System.out.println("In FU: ColorSensor=" + colorSensor);
            localClient = context.actorOf(EV3IOStationServerHandshakeActor.propsForOutputstation(parentActor, true, this, colorSensor), capInstId);
        }
    }

}
