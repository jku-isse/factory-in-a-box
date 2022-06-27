package fiab.handshake.client;

import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.handshake.client.statemachine.ClientSideHandshakeTriggers;
import fiab.handshake.client.statemachine.ClientSideStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class TestClientSideHandshakeStateMachine {

    private RemoteServerHandshakeInfo remoteInfo;
    private ClientSideStateMachine stateMachine;

    @BeforeEach
    public void setup(){
        remoteInfo = new RemoteServerHandshakeInfo();
    }

    @Test
    public void testResetToIdleStateTransition(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.STOPPED, remoteInfo);
        stateMachine.fire(ClientSideHandshakeTriggers.RESET);
        assertEquals(ClientSideStates.RESETTING, stateMachine.getState());
    }

    @Test
    public void testIdleToInitiatingTransition(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.IDLE, remoteInfo);
        stateMachine.fire(ClientSideHandshakeTriggers.START);

        assertEquals(ClientSideStates.STARTING, stateMachine.getState());
        remoteInfo.setRemoteState(ServerSideStates.IDLE_EMPTY);
        assertEquals(ClientSideStates.INITIATING, stateMachine.getState());
    }

    @Test
    public void testIdleToInitiatedTransitionEmpty(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATING, remoteInfo);
        remoteInfo.setRemoteState(ServerSideStates.IDLE_EMPTY);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover);
        remoteInfo.setRemoteState(ServerSideStates.STARTING);
        assertEquals(ClientSideStates.INITIATED, stateMachine.getState());
    }

    @Test
    public void testInInitiatingServerInitResponseNotOk(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATING, remoteInfo);

        remoteInfo.setRemoteState(ServerSideStates.IDLE_EMPTY);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.NotOkResponseInitHandover);
        assertEquals(ClientSideStates.STOPPING, stateMachine.getState());
    }

    @Test
    public void testIdleToInitiatedTransitionFull(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATING, remoteInfo);

        remoteInfo.setRemoteState(ServerSideStates.STARTING);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover);
        assertEquals(ClientSideStates.INITIATED, stateMachine.getState());
    }

    @Test
    public void testInitiatingToReadyTransition(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATED, remoteInfo);

        stateMachine.fire(ClientSideHandshakeTriggers.READY);
        assertEquals(ClientSideStates.READY, stateMachine.getState());
    }

    @Test
    public void testReadyToExecuteTransitionEmpty(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.READY, remoteInfo);
        remoteInfo.setRemoteState(ServerSideStates.IDLE_EMPTY);    //Necessary to match Init and Ready loading state
        remoteInfo.setRemoteState(ServerSideStates.READY_EMPTY);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
        assertEquals(ClientSideStates.EXECUTE, stateMachine.getState());
    }

    @Test
    public void testReadyToExecuteTransitionFull(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.READY, remoteInfo);
        remoteInfo.setRemoteState(ServerSideStates.IDLE_LOADED);    //Necessary to match Init and Ready loading state
        remoteInfo.setRemoteState(ServerSideStates.READY_LOADED);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
        assertEquals(ClientSideStates.EXECUTE, stateMachine.getState());
    }

    @Test
    public void testInReadyServerResponseNotOk(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.READY, remoteInfo);
        remoteInfo.setRemoteState(ServerSideStates.READY_LOADED);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover);
        assertEquals(ClientSideStates.STOPPING, stateMachine.getState());
    }

    @Test
    public void testInReadyServerStateNotIdle(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.READY, remoteInfo);

        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
        assertEquals(ClientSideStates.READY, stateMachine.getState());
    }

    @Test
    public void testExecuteToCompleteTransition(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.EXECUTE, remoteInfo);
        remoteInfo.setRemoteState(ServerSideStates.COMPLETE);
        stateMachine.fire(ClientSideHandshakeTriggers.COMPLETE);
        assertEquals(ClientSideStates.COMPLETING, stateMachine.getState());

        stateMachine.fire(ClientSideHandshakeTriggers.COMPLETING_DONE);
        assertEquals(ClientSideStates.COMPLETED, stateMachine.getState());
    }

    @Test
    public void testValidTransitionsWithServerIdleAndReadyEmpty(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATING, remoteInfo);

        remoteInfo.setRemoteState(ServerSideStates.IDLE_EMPTY);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover);
        remoteInfo.setRemoteState(ServerSideStates.STARTING);
        assertEquals(ClientSideStates.INITIATED, stateMachine.getState());

        stateMachine.fire(ClientSideHandshakeTriggers.READY);
        assertEquals(ClientSideStates.READY, stateMachine.getState());
        remoteInfo.setRemoteState(ServerSideStates.READY_EMPTY);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
        assertEquals(ClientSideStates.EXECUTE, stateMachine.getState());
    }

    @Test
    public void testValidTransitionsWithServerIdleAndReadyFull(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATING, remoteInfo);

        remoteInfo.setRemoteState(ServerSideStates.IDLE_LOADED);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover);
        remoteInfo.setRemoteState(ServerSideStates.STARTING);
        assertEquals(ClientSideStates.INITIATED, stateMachine.getState());
        stateMachine.fire(ClientSideHandshakeTriggers.READY);
        assertEquals(ClientSideStates.READY, stateMachine.getState());
        remoteInfo.setRemoteState(ServerSideStates.READY_LOADED);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
        assertEquals(ClientSideStates.EXECUTE, stateMachine.getState());
    }

    @Test
    public void testInvalidTransitionsWithServerIdleEmptyAndReadyFull(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATING, remoteInfo);

        remoteInfo.setRemoteState(ServerSideStates.IDLE_EMPTY);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover);
        remoteInfo.setRemoteState(ServerSideStates.STARTING);
        assertEquals(ClientSideStates.INITIATED, stateMachine.getState());
        stateMachine.fire(ClientSideHandshakeTriggers.READY);
        assertEquals(ClientSideStates.READY, stateMachine.getState());
        remoteInfo.setRemoteState(ServerSideStates.READY_LOADED);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
        assertEquals(ClientSideStates.STOPPING, stateMachine.getState());
    }

    @Test
    public void testInvalidTransitionsWithServerIdleFullAndReadyEmpty(){
        stateMachine = new ClientSideStateMachine(ClientSideStates.INITIATING, remoteInfo);

        remoteInfo.setRemoteState(ServerSideStates.IDLE_LOADED);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover);
        remoteInfo.setRemoteState(ServerSideStates.STARTING);
        assertEquals(ClientSideStates.INITIATED, stateMachine.getState());
        stateMachine.fire(ClientSideHandshakeTriggers.READY);
        assertEquals(ClientSideStates.READY, stateMachine.getState());
        remoteInfo.setRemoteState(ServerSideStates.READY_EMPTY);
        remoteInfo.setServerResponse(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
        assertEquals(ClientSideStates.STOPPING, stateMachine.getState());
    }


}
