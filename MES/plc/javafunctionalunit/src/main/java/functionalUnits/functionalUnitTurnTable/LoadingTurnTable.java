package functionalUnits.functionalUnitTurnTable;

import communication.utils.RequestedNodePair;
import functionalUnits.LoadingProtocolBase;
import robot.turnTable.TurnTableOrientation;


/**
 * TurnTable implementation of the Loading Protocol FU.
 */
public class LoadingTurnTable extends LoadingProtocolBase {

    private Object statusNodeId;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initiateLoading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiate loading was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initiateUnloading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiate unloading was called");
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
            initiateLoading(TurnTableOrientation.createFromInt(Integer.parseInt(x.substring(0, 1))),
                    Integer.parseInt(x.substring(1)));
            return "Initiate Loading Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 13), "InitiateUnloadingMethod", x -> {
            initiateUnloading(TurnTableOrientation.createFromInt(Integer.parseInt(x.substring(0, 1))),
                    Integer.parseInt(x.substring(1)));
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
