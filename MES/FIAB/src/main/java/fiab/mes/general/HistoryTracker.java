package fiab.mes.general;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;

public class HistoryTracker {
	private int maxHistorySize = 50;	
	private ArrayDeque<MachineEvent> history; 
	private String machineId;

	public HistoryTracker(String machineId) {
		this(machineId, 50);
	}

	public HistoryTracker(String machineId, int maxEntries) {
		this.machineId = machineId;
		history = new ArrayDeque<MachineEvent>();
		maxHistorySize = maxEntries;
	}

	public void add(MachineUpdateEvent mue) {
		if (history.size() >= maxHistorySize) {
			history.removeFirst();
		}
		history.addLast(mue);
	}

	public void sendHistoryResponseTo(MachineHistoryRequest req, ActorRef recipient, ActorRef sender) {
		List<MachineEvent> events = req.shouldResponseIncludeDetails() ? history.stream().collect(Collectors.toList()) : history.stream().map(event -> event.getCloneWithoutDetails()).collect(Collectors.toList());
		MachineHistoryRequest.Response response = new MachineHistoryRequest.Response(machineId, events, req.shouldResponseIncludeDetails());
		recipient.tell(response, sender);
	}
}
