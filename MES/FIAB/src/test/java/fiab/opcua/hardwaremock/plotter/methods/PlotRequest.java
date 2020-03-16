package fiab.opcua.hardwaremock.plotter.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.capabilities.plotting.WellknownPlotterCapability;
import fiab.mes.machine.msg.MachineInWrongStateResponse;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.plotter.MockMachineWrapper;
import static akka.pattern.Patterns.ask;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class PlotRequest extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private ActorRef actor;
	
    public static final Argument RESPONSE = new Argument(
            "plot response",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Response indicating (new) state of Plotter whether plot request can be fulfilled.")
        );  
    
    public static final Argument IMAGE_ID = new Argument(
            WellknownPlotterCapability.PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME,
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("ImageId that should be plotted")
        );
    
    
    public PlotRequest(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode); 
        this.actor = actor;        
    }

    @Override
    public Argument[] getInputArguments() {    	
    	return new Argument[]{IMAGE_ID};    	    	
    }

    @Override
    public Argument[] getOutputArguments() {
    	return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
      
    	logger.debug("Invoked PlotRequest() method of objectId={}", invocationContext.getObjectId());    	
    	Object resp;
		try {
			String capIdFrom = (String) inputValues[0].getValue();
			// for now we ignore that we could have gotten a image id we don't support							
			resp = ask(actor, MockMachineWrapper.MessageTypes.Plot, timeout).toCompletableFuture().get();
			if (resp instanceof MachineStatusUpdateEvent) {
				 resp = ((MachineStatusUpdateEvent) resp).getStatus();
			}
			else if (resp instanceof MachineInWrongStateResponse) {
				resp = ((MachineInWrongStateResponse) resp).getStatus();
			}
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage());
			resp = MachineStatus.UNKNOWN;
		}    	        
        return new Variant[]{new Variant(resp.toString())};    	    	
    }	
    
}
