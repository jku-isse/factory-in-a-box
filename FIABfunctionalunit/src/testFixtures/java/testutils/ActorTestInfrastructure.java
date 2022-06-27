package testutils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;

import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.functionalunit.connector.FUSubscriptionClassifier;

import java.util.concurrent.atomic.AtomicInteger;

public class ActorTestInfrastructure {

    public final String eventSourceId = "TestClass";
    protected ActorSystem system;
    protected final FUSubscriptionClassifier testClassifier;
    protected TestKit probe;
    protected IntraMachineEventBus intraMachineEventBus;
    protected MachineEventBus machineEventBus;
    protected ActorRef actorRef;
    protected static AtomicInteger runCount;

    /**
     * This class holds the necessary infrastructure for Actor Tests
     * Use this class in the BeforeAll method to initialize all common
     * elements of such a test.
     * <p>
     * IMPORTANT: The actor under Test must be instantiated using the initializeActor method
     * <p>
     * Please note that FU tests require additional information and can be found
     * in the FUTestInfrastructure class
     */
    public ActorTestInfrastructure() {
        system = ActorSystem.create();
        testClassifier = new FUSubscriptionClassifier(eventSourceId, "*");
        intraMachineEventBus = new IntraMachineEventBus();
        machineEventBus = new MachineEventBus();
        probe = new TestKit(system);
        runCount = new AtomicInteger(0);
    }

    public void subscribeToMachineEventBus() {
        machineEventBus.subscribe(probe.getRef(), testClassifier);
    }

    public void subscribeToIntraMachineEventBus() {
        intraMachineEventBus.subscribe(probe.getRef(), testClassifier);
    }

    /**
     * Method to initialize an actor using its props and assigning it a name for easier debugging
     * Use this method in the BeforeEach method of a Test
     *
     * @param props     Props of the actor to test
     * @param actorName The name for the actor
     */
    public void initializeActor(Props props, String actorName) {
        actorRef = system.actorOf(props, actorName);
    }

    /**
     * This method tells an actor to shut down and makes sure it does so successfully
     * It removes its reference, so that a new test won't be affected by an old instance
     * <p>
     * Use this in the AfterEach method
     */
    public void destroyActor() {
        probe.watch(actorRef);
        actorRef.tell(PoisonPill.getInstance(), probe.getRef());
        probe.expectTerminated(actorRef);
        probe.unwatch(actorRef);
        actorRef = null;
    }

    /**
     * Returns the ref of the actor under test
     *
     * @return actor under test
     */
    public ActorRef getActorRef() {
        return actorRef;
    }

    /**
     * This code must be called in the AfterAll method to avoid errors in other tests
     * and to free up memory
     */
    public void shutdownInfrastructure() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Returns the default classifier ("*") that can be used to subscribe the connector to an eventbus
     * @return subscription classifier
     */
    public FUSubscriptionClassifier getTestClassifier() {
        return testClassifier;
    }

    /**
     * A thread safe Integer representing the run count
     * This is used for instantiating Actors which might run in parallel to avoid naming conflicts
     *
     * @return runCount
     */
    public int getAndIncrementRunCount() {
        return runCount.getAndIncrement();
    }

    /**
     * The Actor system for the test
     *
     * @return ActorSystem
     */
    public ActorSystem getSystem() {
        return system;
    }

    /**
     * Returns the TestKit. It acts as a probe that provides assertions and other
     * useful test functionality
     *
     * @return TestKit
     */
    public TestKit getProbe() {
        return probe;
    }

    public IntraMachineEventBus getIntraMachineEventBus() {
        return intraMachineEventBus;
    }

    public MachineEventBus getMachineEventBus() {
        return machineEventBus;
    }
}
