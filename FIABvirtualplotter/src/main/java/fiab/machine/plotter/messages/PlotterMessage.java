package fiab.machine.plotter.messages;

import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class PlotterMessage extends ExtensibleMessage<PlotterMessageTypes> {
	private final PlotterMessageTypes body;

	public PlotterMessage(String header, PlotterMessageTypes body) {
		super(header);
		this.body = body;
	}

	@Override
	public PlotterMessageTypes getBody() {
		return body;
	}

}
