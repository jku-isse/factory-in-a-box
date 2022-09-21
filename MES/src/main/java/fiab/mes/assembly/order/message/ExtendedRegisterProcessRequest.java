package fiab.mes.assembly.order.message;

import ProcessCore.XmlRoot;
import akka.actor.ActorRef;
import fiab.mes.order.OrderProcess;

public class ExtendedRegisterProcessRequest {

    protected final String rootOrderId;
    protected final XmlRoot xmlRoot;
    protected final ActorRef orderActor;

    public ExtendedRegisterProcessRequest(String rootOrderId, XmlRoot xmlRoot, ActorRef requestor) {
        this.rootOrderId = rootOrderId;
        this.xmlRoot = xmlRoot;
        this.orderActor = requestor;
    }

    public String getRootOrderId() {
        return rootOrderId;
    }

    public XmlRoot getXmlRoot() {
        return xmlRoot;
    }

    public ActorRef getRequestor() {
        return orderActor;
    }
}
