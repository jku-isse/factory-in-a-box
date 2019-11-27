package fiab.mes.handshake;

public class HandshakeProtocol {

	public enum ServerSide {
		Stopping, Stopped, Resetting, IdleLoaded, IdleEmpty, Starting, Preparing, ReadyLoaded, ReadyEmpty, Execute, Completing, Completed
	}
	
	public enum ClientSide {
		Stopping, Stopped, Resetting, Idle, Starting, Initiating, Initiated, Ready, Execute, Completing, Completed
	}
	
}
