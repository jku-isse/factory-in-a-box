package fiab.mes.assembly;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.assembly.monitoring.actor.AssemblyMonitoringActor;
import fiab.mes.assembly.order.actor.BikeAssemblyOrderPlanningActor;
import fiab.mes.eventbus.AssemblyMonitoringEventBusWrapperActor;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.assembly.transport.actor.DummyTransportSystemCoordinatorActor;
import fiab.opcua.server.OPCUABase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class BikeAssemblyInfrastructure {

    // create machine and order event bus
    public BikeAssemblyInfrastructure(ActorSystem system) throws Exception {
        OPCUABase server = OPCUABase.createAndStartLocalServer(4840, "MonitoringServer");
        KieServices ks = KieServices.get();
        KieContainer kc = ks.getKieClasspathContainer();
        KieSession kieSession = kc.newKieSession("HelloWorldKS");

        ActorRef orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef monitorEventBus = system.actorOf(AssemblyMonitoringEventBusWrapperActor.props(), AssemblyMonitoringEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        //ActorRef coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, expectedTTs), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        //Since we don't need any transport capabilities, we will use a dummy here
        ActorRef transportActor = system.actorOf(DummyTransportSystemCoordinatorActor.props(), DummyTransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef monitoringActor = system.actorOf(AssemblyMonitoringActor.props(server, kieSession), AssemblyMonitoringActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef orderPlanningActor = system.actorOf(BikeAssemblyOrderPlanningActor.props(), BikeAssemblyOrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
        //ActorRef orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
    }
}
