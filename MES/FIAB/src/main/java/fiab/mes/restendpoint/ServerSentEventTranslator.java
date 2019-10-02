package fiab.mes.restendpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.http.javadsl.model.sse.ServerSentEvent;
import fiab.mes.order.OrderProcessWrapper;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEventWrapper;
import fiab.mes.restendpoint.requests.OrderStatusRequest;


public class ServerSentEventTranslator{

	protected static ObjectMapper om = new ObjectMapper();

	//public static String ORDER_STATUS_UPDATE = "ORDER_STATUS_UPDATE";
	private static final Logger logger = LoggerFactory.getLogger(ServerSentEventTranslator.class);
	
	public static ServerSentEvent toServerSentEvent(OrderEvent orderEvent) {				
		
		try {
			String json = om.writeValueAsString(new OrderEventWrapper(orderEvent));
			//return ServerSentEvent.create(json, orderEvent.getType().toString());
			return ServerSentEvent.create(json, "message"); // in the angular frontend also implement addEventListener, as onmessage expects the type to be 'message'
		} catch (JsonProcessingException e) {
			logger.warn("Error marshalling OrderEvent", e);
			return ServerSentEvent.create("", "message");
		}		
	}
	
	public static ServerSentEvent toServerSentEvent(String orderId, OrderStatusRequest.Response response) {				
		
		try {
			String json = om.writeValueAsString(new OrderProcessWrapper(orderId, response));
			//return ServerSentEvent.create(json, orderEvent.getType().toString());
			return ServerSentEvent.create(json, "message"); // in the angular frontend also implement addEventListener, as onmessage expects the type to be 'message'
		} catch (JsonProcessingException e) {
			logger.warn("Error marshalling OrderStatusRequest.Response", e);
			return ServerSentEvent.create("", "message");
		}		
	}
}
