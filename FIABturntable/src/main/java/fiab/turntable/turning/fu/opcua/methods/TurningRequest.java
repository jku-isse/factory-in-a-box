package fiab.opcua.hardwaremock.turntable.methods;

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
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.mes.machine.msg.MachineInWrongStateResponse;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.plotter.MockMachineWrapper;
import stateMachines.turning.TurnRequest;
import stateMachines.turning.TurnTableOrientation;
import stateMachines.turning.TurningTriggers;

import static akka.pattern.Patterns.ask;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class TurningRequest extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private ActorRef actor;
	
    
    public static final Argument TURN_POS = new Argument(
            "TurnToPos",
            Identifiers.Integer,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Position the Turntable should turn to: on of 0,1,2,3")
        );
    
    
    public TurningRequest(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode); 
        this.actor = actor;        
    }

    @Override
    public Argument[] getInputArguments() {    	
    	return new Argument[]{TURN_POS};    	    	
    }

    @Override
    public Argument[] getOutputArguments() {
    	return new Argument[0];
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
      
    	logger.debug("Invoked TurnRequest() method of objectId={}", invocationContext.getObjectId());    	
		try {
			int pos = (Integer) inputValues[0].getValue();
			// for now we ignore that we could have gotten a image id we don't support							
			actor.tell(new TurnRequest(TurnTableOrientation.createFromInt(pos)), ActorRef.noSender());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}    	        
    	return new Variant[0]; 	    	    	    	
    }	
    
}
