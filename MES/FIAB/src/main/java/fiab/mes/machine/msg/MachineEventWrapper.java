package fiab.mes.machine.msg;

import main.java.fiab.core.capabilities.basicmachine.events.MachineEvent;
import main.java.fiab.core.capabilities.basicmachine.events.MachineEvent.MachineEventType;
import main.java.fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.transport.msg.TransportSystemStatusMessage;

public class MachineEventWrapper {

	private String message;
	private String machineId;
	private MachineEventType eventType;
	private String timestamp;
	
	//MachineUpdateEvent additional fields
	private String nodeId = "";
	private String parameterName = "";
	private String newValue = "";
	
	public MachineEventWrapper(MachineEvent e) {
		this.message = e.getMessage();
		this.machineId = e.getMachineId();
		this.eventType = e.getEventType();
		this.timestamp = e.getTimestamp().toString();
		if (e instanceof MachineUpdateEvent) {
			MachineUpdateEvent mue = (MachineUpdateEvent)e;			
			this.parameterName = mue.getParameterName();
			this.newValue = mue.getValue().toString();
		} else if (e instanceof PlanerStatusMessage) {
			PlanerStatusMessage se = (PlanerStatusMessage)e;
			this.parameterName = se.getEventType().toString();
			this.newValue = se.getState().toString();
		} else if (e instanceof TransportSystemStatusMessage) {
			TransportSystemStatusMessage se = (TransportSystemStatusMessage)e;
			this.parameterName = se.getEventType().toString();
			this.newValue = se.getState().toString();
		}
		
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getMachineId() {
		return machineId;
	}
	
	public MachineEventType getEventType() {
		return eventType;
	}
	
	public String getTimestamp() {
		return timestamp;
	}

	public String getNodeId() {
		return nodeId;
	}

	public String getParameterName() {
		return parameterName;
	}

	public String getNewValue() {
		return newValue;
	}
	
	
}
