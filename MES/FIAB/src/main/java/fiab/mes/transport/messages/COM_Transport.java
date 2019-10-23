
package fiab.mes.transport.messages;

import fiab.mes.transport.mockClasses.Direction;

public class COM_Transport {
	private Direction from;
	private Direction to;
	private String id;
	

	public COM_Transport(Direction from, Direction to, String id) {
		this.from = from;
		this.to = to;
		this.id = id;
	}

	public Direction getFrom() {
		return from;
	}

	public void setFrom(Direction from) {
		this.from = from;
	}

	public Direction getTo() {
		return to;
	}

	public void setTo(Direction to) {
		this.to = to;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
}
