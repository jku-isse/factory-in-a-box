package fiab.mes.assembly.order.message;

import ProcessCore.XmlRoot;
import akka.actor.ActorRef;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.msg.RegisterProcessRequest;

public class ExtendedRegisterProcessRequest extends RegisterProcessRequest {

    protected final XmlRoot xmlRoot;

    public ExtendedRegisterProcessRequest(String rootOrderId, OrderProcess process, XmlRoot xmlRoot, ActorRef requestor) {
        super(rootOrderId, process, requestor);
        this.xmlRoot = xmlRoot;
    }

//    public ExtendedRegisterProcessRequest(String rootOrderId, XmlRoot xmlRoot, ActorRef requestor) {
//        super();
//        this.rootOrderId = rootOrderId;
//        this.xmlRoot = xmlRoot;
//        this.orderActor = requestor;
//    }

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
