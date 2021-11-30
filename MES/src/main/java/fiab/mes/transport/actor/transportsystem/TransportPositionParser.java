package fiab.mes.transport.actor.transportsystem;

public interface TransportPositionParser {

    public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString);

    public TransportRoutingInterface.Position parseLastIPPos(String uriAsString);
}
