package fiab.mes.restendpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.http.javadsl.model.sse.ServerSentEvent;
import fiab.mes.order.msg.OrderEvent;


public class ServerSentEventTranslator{

	protected static ObjectMapper om = new ObjectMapper();

	//public static String ORDER_STATUS_UPDATE = "ORDER_STATUS_UPDATE";
	private static final Logger logger = LoggerFactory.getLogger(ServerSentEventTranslator.class);
	
	public static ServerSentEvent toServerSentEvent(OrderEvent orderEvent) {				
		
		try {
			String json = om.writeValueAsString(orderEvent);
			//return ServerSentEvent.create(json, orderEvent.getType().toString());
			return ServerSentEvent.create(json, "message"); // in the angular frontend also implement addEventListener, as onmessage expects the type to be 'message'
		} catch (JsonProcessingException e) {
			logger.warn("Error marshalling OrderEvent", e);
			return ServerSentEvent.create("", orderEvent.getEventType().toString());
		}		
	}
}
