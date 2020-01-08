package fiab.mes.machine.actor.iostation.wrapper;

import fiab.opcua.hardwaremock.InputStationMock;

public class MockIOStationWrapper {
	
	InputStationMock ism;
	
	
	public MockIOStationWrapper(InputStationMock ism) {
		this.ism = ism;
	}
	//This wrapper will invoke the OPC-Server Methods. 
	//This can either be achieved by a client connected to the server, or internally in java
	
	public void stop() {
		ism.stopMethod();
	}
	public void reset() {
		ism.resetMethod();
	}
	public void complete() {
		ism.completeMethod();
	}
	public void readyEmpty() {
		ism.readyEmptyMethod();
	}
}
