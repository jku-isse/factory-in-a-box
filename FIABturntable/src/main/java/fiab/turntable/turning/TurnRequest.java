package fiab.turntable.turning;

import fiab.tracing.actor.messages.TracingHeader;

public class TurnRequest implements TracingHeader {
	TurnTableOrientation tto;
	private String header;

	public TurnTableOrientation getTto() {
		return tto;
	}

	public void setTto(TurnTableOrientation tto) {
		this.tto = tto;
	}

	public TurnRequest(TurnTableOrientation tto) {
		this(tto, "");
	}

	public TurnRequest(TurnTableOrientation tto, String header) {
		super();
		this.tto = tto;
		this.header = header;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}

}