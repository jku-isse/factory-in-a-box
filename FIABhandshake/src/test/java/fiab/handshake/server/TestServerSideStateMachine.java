package fiab.handshake.server;

import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.handshake.server.statemachine.ServerSideHandshakeTriggers;
import fiab.handshake.server.statemachine.ServerSideStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
public class TestServerSideStateMachine {

    private TransportAreaStatusInfo transportAreaStatusInfo;

    @BeforeEach
    public void setup(){
        transportAreaStatusInfo = new TransportAreaStatusInfo();    //Default value is empty!
    }

    @Test
    public void testResetEmptyFromStoppedSuccess(){
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo);

        stateMachine.fire(ServerSideHandshakeTriggers.RESET);
        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_EMPTY, stateMachine.getState());
    }

    @Test
    public void testResetLoadedFromStoppedSuccess(){
        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetLoaded);
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo);

        stateMachine.fire(ServerSideHandshakeTriggers.RESET);
        assertEquals(ServerSideStates.RESETTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_LOADED, stateMachine.getState());
    }

    @Test
    public void testIdleEmptyToReadyEmptySuccess(){
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.RESETTING);
        //We need to start in resetting, since the idle state does not trigger onEntry when it is the starting state
        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_EMPTY, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.START);
        assertEquals(ServerSideStates.STARTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.PREPARE);
        assertEquals(ServerSideStates.PREPARING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.READY);
        assertEquals(ServerSideStates.READY_EMPTY, stateMachine.getState());
    }

    @Test
    public void testIdleLoadedToReadyLoadedSuccess(){
        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetLoaded);
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.RESETTING);

        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_LOADED, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.START);
        assertEquals(ServerSideStates.STARTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.PREPARE);
        assertEquals(ServerSideStates.PREPARING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.READY);
        assertEquals(ServerSideStates.READY_LOADED, stateMachine.getState());
    }

    @Test
    public void testIdleEmptyToReadyLoadedFails(){
        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetLoaded);
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.IDLE_LOADED);

        stateMachine.fire(ServerSideHandshakeTriggers.START);
        assertEquals(ServerSideStates.STARTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.PREPARE);
        assertEquals(ServerSideStates.PREPARING, stateMachine.getState());

        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetEmpty);
        stateMachine.fire(ServerSideHandshakeTriggers.READY);
        assertEquals(ServerSideStates.STOPPING, stateMachine.getState());
    }

    @Test
    public void testIdleLoadedToReadyEmptyFails(){
        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetLoaded);
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.IDLE_LOADED);

        stateMachine.fire(ServerSideHandshakeTriggers.START);
        assertEquals(ServerSideStates.STARTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.PREPARE);
        assertEquals(ServerSideStates.PREPARING, stateMachine.getState());

        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetEmpty);
        stateMachine.fire(ServerSideHandshakeTriggers.READY);
        assertEquals(ServerSideStates.STOPPING, stateMachine.getState());
    }

    @Test
    public void testReadyEmptyToCompleteSuccess(){
        //We need to enter idle empty from resetting, since otherwise the state machine does not trigger the action
        //It checks whether the idle and ready state are both empty before execute can be entered
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.RESETTING);
        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_EMPTY, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.START);
        assertEquals(ServerSideStates.STARTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.PREPARE);
        assertEquals(ServerSideStates.PREPARING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.READY);
        assertEquals(ServerSideStates.READY_EMPTY, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.EXECUTE);
        assertEquals(ServerSideStates.EXECUTE, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.COMPLETE);
        assertEquals(ServerSideStates.COMPLETING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.COMPLETING_DONE);
        assertEquals(ServerSideStates.COMPLETE, stateMachine.getState());
    }

    @Test
    public void testReadyLoadedToCompleteSuccess(){
        //We need to enter idle from resetting, since otherwise the state machine does not trigger the action
        //It checks whether the idle and ready state are both loaded before execute can be entered
        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetLoaded);
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.RESETTING);
        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_LOADED, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.START);
        assertEquals(ServerSideStates.STARTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.PREPARE);
        assertEquals(ServerSideStates.PREPARING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.READY);
        assertEquals(ServerSideStates.READY_LOADED, stateMachine.getState());

        stateMachine.fire(ServerSideHandshakeTriggers.EXECUTE);
        assertEquals(ServerSideStates.EXECUTE, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.COMPLETE);
        assertEquals(ServerSideStates.COMPLETING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.COMPLETING_DONE);
        assertEquals(ServerSideStates.COMPLETE, stateMachine.getState());
    }

    @Test
    public void testCompleteToIdleEmptySuccess(){
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.COMPLETE);

        stateMachine.fire(ServerSideHandshakeTriggers.RESET);
        assertEquals(ServerSideStates.RESETTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_EMPTY, stateMachine.getState());
    }

    @Test
    public void testCompleteToIdleLoadedSuccess(){
        transportAreaStatusInfo.updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests.SetLoaded);
        ServerSideStateMachine stateMachine = new ServerSideStateMachine(transportAreaStatusInfo, ServerSideStates.COMPLETE);

        stateMachine.fire(ServerSideHandshakeTriggers.RESET);
        assertEquals(ServerSideStates.RESETTING, stateMachine.getState());
        stateMachine.fire(ServerSideHandshakeTriggers.RESETTING_DONE);
        assertEquals(ServerSideStates.IDLE_LOADED, stateMachine.getState());
    }
}
