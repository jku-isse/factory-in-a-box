package fiab.mes.transport.actor.transportsystem;

public interface TransportPositionParser {

    default public String getLookupPrefix(){return "";}

    public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString);

    public TransportRoutingInterface.Position parseLastIPPos(String uriAsString);
}
