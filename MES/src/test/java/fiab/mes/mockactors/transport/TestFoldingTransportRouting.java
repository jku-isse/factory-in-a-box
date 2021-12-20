package fiab.mes.mockactors.transport;

import fiab.mes.transport.actor.transportsystem.HardcodedFoldingTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestFoldingTransportRouting {

    @Test
    public void testConnectionBetween41And43() throws TransportRoutingInterface.RoutingException {
        HardcodedFoldingTransportRoutingAndMapping mapping = new HardcodedFoldingTransportRoutingAndMapping();
        List<Position> result = mapping.calculateRoute(new Position("41"), new Position("43"));
        Assertions.assertTrue(result.contains(new Position("42")));
    }
}
