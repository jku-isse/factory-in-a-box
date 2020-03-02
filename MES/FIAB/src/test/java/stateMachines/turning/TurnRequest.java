package stateMachines.turning;

public class TurnRequest {
	TurnTableOrientation tto;

	public TurnTableOrientation getTto() {
		return tto;
	}

	public void setTto(TurnTableOrientation tto) {
		this.tto = tto;
	}

	public TurnRequest(TurnTableOrientation tto) {
		super();
		this.tto = tto;
	} 
	
	
}