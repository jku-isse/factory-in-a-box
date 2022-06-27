package fiab.mes.proxy.ioStation.inputStation.testutils;

import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

public class InputStationPositionParser implements TransportPositionParser {

    @Override
    public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
        return new TransportRoutingInterface.Position("34");
    }

    @Override
    public TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
        return new TransportRoutingInterface.Position("34");
    }
}
