package fiab.mes.mockactors.bufferStation;

import akka.actor.ActorRef;
import fiab.core.capabilities.buffer.BufferStationWellKnownCapabilityIdentifiers;
import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.mockactors.plotter.MockPlottingMachineWrapperDelegate;
import fiab.mes.transport.actor.bufferstation.wrapper.BufferStationWrapperInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockBufferStationWrapperDelegate implements BufferStationWrapperInterface {

    protected ActorRef wrapper;
    private static final Logger logger = LoggerFactory.getLogger(MockBufferStationWrapperDelegate.class);

    public MockBufferStationWrapperDelegate(ActorRef wrapper){
        this.wrapper = wrapper;
    }

    @Override
    public void load(String orderId) {
        logger.info("load called");
        wrapper.tell(BufferStationWellKnownCapabilityIdentifiers.OPCUA_LOAD_REQUEST, ActorRef.noSender());
    }

    @Override
    public void unload(String orderId) {
        logger.info("unload called");
        wrapper.tell(BufferStationWellKnownCapabilityIdentifiers.OPCUA_UNLOAD_REQUEST, ActorRef.noSender());
    }

    @Override
    public void stop() {
        logger.info("stop called");
        wrapper.tell(BufferStationWellKnownCapabilityIdentifiers.STOP_REQUEST, ActorRef.noSender());
    }

    @Override
    public void reset() {
        logger.info("reset called");
        wrapper.tell(BufferStationWellKnownCapabilityIdentifiers.RESET_REQUEST, ActorRef.noSender());
    }

    @Override
    public void subscribeToStatus() {
        logger.info("subscribeToStatus called");
        wrapper.tell(BufferStationWellKnownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
    }

    @Override
    public void unsubscribeFromStatus() {
        //TODO?
    }
}
