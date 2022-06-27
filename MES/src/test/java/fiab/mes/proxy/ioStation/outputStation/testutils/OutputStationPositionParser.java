package fiab.mes.proxy.ioStation.outputStation.testutils;

import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

public class OutputStationPositionParser implements TransportPositionParser {

    @Override
    public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
        return new TransportRoutingInterface.Position("35");
    }

    @Override
    public TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
        return new TransportRoutingInterface.Position("35");
    }
}
