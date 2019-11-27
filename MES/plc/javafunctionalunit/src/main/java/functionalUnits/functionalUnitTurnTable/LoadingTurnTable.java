package functionalUnits.functionalUnitTurnTable;

import communication.utils.RequestedNodePair;
import functionalUnits.LoadingProtocolBase;


/**
 * TurnTable implementation of the Loading Protocol FU.
 */
public class LoadingTurnTable extends LoadingProtocolBase {

    private Object statusNodeId;

    @Override
    public void request_init_handover() {
        System.out.println("Request init handover");
    }

    @Override
    public void request_start_handover() {
        System.out.println("Start handover");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete() {
        System.out.println("Complete was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        System.out.println("Loading: Reset was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        System.out.println("Loading: Stop was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(),
                new RequestedNodePair<>(1, 55), "LoadingProtocolStatus");
        addStringMethodToServer(new RequestedNodePair<>(1, 11), "CompleteLoadingMethod", x -> {
            complete();
            return "Complete Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 12), "InitiateLoadingMethod", x -> {
            request_init_handover();
            return "Initiate Loading Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 13), "InitiateUnloadingMethod", x -> {
            request_start_handover();
            return "Initiate Unloading Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 14), "ResetLoadingMethod", x -> {
            reset();
            return "Resetting Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 15), "StopLoadingMethod", x -> {
            stop();
            return "Stopping Successful";
        });
    }
}
