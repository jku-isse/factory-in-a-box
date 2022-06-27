package fiab.mes.proxy.turntable.testutils;

import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

public class TurntableStationPositionParser implements TransportPositionParser {

    @Override
    public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
        return new TransportRoutingInterface.Position("20");
    }

    @Override
    public TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
        return new TransportRoutingInterface.Position("20");
    }
}
