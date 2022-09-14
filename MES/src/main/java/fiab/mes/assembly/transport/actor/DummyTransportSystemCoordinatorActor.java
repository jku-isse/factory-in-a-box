package fiab.mes.assembly.transport.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.transport.msg.CancelTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;

/**
 * This class acts as a TransportSystemCoordinator, but will always tell that the transport has been successful
 * Currently will be used for a factory that has no necessary transport capabilities
 */
public class DummyTransportSystemCoordinatorActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    public static final String WELLKNOWN_LOOKUP_NAME = "TransportSystemCoordinatorActor";

    public static Props props() {
        return Props.create(DummyTransportSystemCoordinatorActor.class, () -> new DummyTransportSystemCoordinatorActor());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegisterTransportRequest.class, req -> {
                    //log.info(String.format("Received Transport Request %s -> %s", req.getSource().getId(), req.getDestination().getId()));
                    //handleNewIncomingRequest(req); // Message to register transport among machines (including input and output station)
                    String msg = String.format("Transport for OrderId %s complete", req.getOrderId());
                    log.info(msg);
                    sender().tell(new RegisterTransportRequestStatusResponse(req, RegisterTransportRequestStatusResponse.ResponseType.COMPLETED, msg), self());
                })
                // available transport systems
                .match(MachineConnectedEvent.class, machineEvent -> {
                    //handleNewlyAvailableMachine(machineEvent);
                })
                .match(MachineDisconnectedEvent.class, machineEvent -> {
                    //handleNoLongerAvailableMachine(machineEvent);
                })
                // message from turntable (actually all machines) on their state --> LATER: filter to only receive events from transport modules
                .match(MachineStatusUpdateEvent.class, machineEvent -> {
                    //handleMachineUpdateEvent(machineEvent);
                })
                .match(CancelTransportRequest.class, req -> {
                    //handleCancelTransportRequest(req);
                })
                .match(MachineHistoryRequest.class, req -> {
                    //log.info(String.format("Machine %s received MachineHistoryRequest", WELLKNOWN_LOOKUP_NAME));
                    //externalHistory.sendHistoryResponseTo(req, getSender(), self);
                })
                // TODO: include transportModuleErrorEvent/Message
                .build();
    }
}
