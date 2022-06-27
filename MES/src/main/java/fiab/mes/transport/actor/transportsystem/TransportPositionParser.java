package fiab.mes.transport.actor.transportsystem;

public interface TransportPositionParser {

    default String getLookupPrefix(){return "";}

    TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString);

    TransportRoutingInterface.Position parseLastIPPos(String uriAsString);
}
