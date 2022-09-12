package fiab.mes.mockactors.transport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.RoutingException;

@Tag("UnitTest")
class TestTransportRouting {

	private Position pos31 = new Position("31");
	private Position pos32 = new Position("32");
	private Position pos34 = new Position("34");
	private Position pos35 = new Position("35");
	private Position pos37 = new Position("37");
	private Position pos38 = new Position("38");
	private Position pos20 = new Position("20");
	private Position pos21 = new Position("21");
	
	@Test
	void test1FIABStandardLayout() throws RoutingException {
		HardcodedDefaultTransportRoutingAndMapping tr = new HardcodedDefaultTransportRoutingAndMapping();
		List<Position> route = tr.calculateRoute(pos31,pos20);
		System.out.println(route);
		assertTrue(route.size()==2);
	}

	@Test
	void test2FIABStandardLayout() throws RoutingException {
		HardcodedDefaultTransportRoutingAndMapping tr = new HardcodedDefaultTransportRoutingAndMapping();
		List<Position> route = tr.calculateRoute(pos31,pos37);
		System.out.println(route);
		assertTrue(route.size()==3);
	}
	
	@Test
	void test3FIABStandardLayout() throws RoutingException {
		HardcodedDefaultTransportRoutingAndMapping tr = new HardcodedDefaultTransportRoutingAndMapping();
		List<Position> route = tr.calculateRoute(pos34,pos35);
		System.out.println(route);
		assertTrue(route.size()==4);
	}
	
	@Test
	void test4FIABStandardLayout() throws RoutingException {
		HardcodedDefaultTransportRoutingAndMapping tr = new HardcodedDefaultTransportRoutingAndMapping();
		List<Position> route = tr.calculateRoute(pos21,pos20);
		System.out.println(route);
		assertTrue(route.size()==2);
	}
}
