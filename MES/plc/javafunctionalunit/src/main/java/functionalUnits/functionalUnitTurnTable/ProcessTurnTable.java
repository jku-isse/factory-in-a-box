package functionalUnits.functionalUnitTurnTable;

import communication.utils.RequestedNodePair;
import functionalUnits.ProcessEngineBase;
import io.vertx.core.Vertx;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TurnTable implementation of the Process Engine
 */
public class ProcessTurnTable extends ProcessEngineBase {

    private Object statusNodeId;
    private AtomicBoolean stopped;

    public ProcessTurnTable() {
        stopped = new AtomicBoolean(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProcess() {
        //TODO refactor once it works with multiple Functional Units
        System.out.println("Conveyor status: " + getClientCommunication().getConveyorStatus());
        if (getClientCommunication().getConveyorStatus() != 9) {
            System.out.println("Busy");
            return;
        } else if (getClientCommunication().getConveyorStatus() == -1) {
            //Stop conveyor to update state to stopped if not initialised
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 24), "");
        }
        System.out.println("Entered load process");
        Vertx vertx = Vertx.vertx();
        vertx.executeBlocking(promise -> {
                    int conveyorState = getClientCommunication().getConveyorStatus();
                    //int turningState = getClientCommunication().getTurningStatus();
                    System.out.println("Loading Process");
                    //Reset conveyor
                    System.out.println("Resetting conveyor");
                    getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                            new RequestedNodePair<>(1, 23), "");
                    System.out.println("Reset conveyor done");

                    while (conveyorState != 0) {
                        if (stopped.get()) {
                            return;
                        }
                        conveyorState = getClientCommunication().getConveyorStatus();
                    }

                    //Load conveyor
                    System.out.println("Loading conveyor");
                    getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                            new RequestedNodePair<>(1, 21), "");
                    System.out.println("Loading done");

                    while (conveyorState != 6) {
                        if (stopped.get()) {
                            return;
                        }
                        conveyorState = getClientCommunication().getConveyorStatus();
                    }
                    //Unload conveyor
                    System.out.println("Unloading conveyor");
                    getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                            new RequestedNodePair<>(1, 25), "");
                    System.out.println("Unloading done");

                    while (conveyorState != 0) {
                        if (stopped.get()) {
                            return;
                        }
                        conveyorState = getClientCommunication().getConveyorStatus();
                    }
                    //Stop conveyor
                    System.out.println("Stopping conveyor");
                    getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                            new RequestedNodePair<>(1, 24), "");
                    System.out.println("Stop done");

                    System.out.println("Loaded Process");

                },
                res -> {
                });

        vertx.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        stopped.set(false);
        System.out.println("Reset Process was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        stopped.set(true);
        System.out.println("Stop Process was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(),
                new RequestedNodePair<>(1, 58), "ProcessEngineStatus");
        addStringMethodToServer(new RequestedNodePair<>(1, 41), "LoadProcessMethod", x -> {
            loadProcess();
            return "Load Process Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 42), "ResetProcessMethod", x -> {
            reset();
            return "Resetting Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 43), "StopProcessMethod", x -> {
            stop();
            return "Stopping Successful";
        });
    }
}
