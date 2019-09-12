package functionalUnits.functionalUnitTurnTable;

import communication.utils.RequestedNodePair;
import functionalUnits.ProcessEngineBase;

/**
 * TurnTable implementation of the Process Engine
 */
public class ProcessTurnTable extends ProcessEngineBase {

    private Object statusNodeId;

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProcess() {
        //TODO create mock process
        System.out.println("Loaded Process");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        System.out.println("Reset Process was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        System.out.println("Stop Process was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
          /*
        statusNodeId = ServerCommunication.addIntegerVariableNode(getServer(), getConveyorFolder(), new Pair<>(1, 56),
                "ConveyorStatus");
         */
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(),new RequestedNodePair<>(1, 58),
                "ProcessEngineStatus");
        /*new LoadProcessMethod(this).addMethod();
        new ResetProcessMethod(this).addMethod();
        new StopProcessMethod(this).addMethod();*/
    }
}
