package org.eclipse.californium.relay;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class UpdateResource extends CoapResource {

	public UpdateResource(String name) {
		super(name);
	}

	@Override
	public void handlePOST(CoapExchange exchange) {
		String address = exchange.getSourceAddress().toString();
		//TODO check if the request comes from an anctiveNodes Node and convert to room
		
		String payload = exchange.getRequestText();
		System.out.println("payload: " + payload);
		
		//values[0] = temp; values[1] = lum
		String[] values = payload.split(";");
		
		Map<String,String> nameValuePairs = new HashMap<String, String>();
		//nameValuePairs.put("addr", address);
		nameValuePairs.put("temp", values[0]);
		nameValuePairs.put("lum", values[1]);
		nameValuePairs.put("room", values[2]);
		System.out.println("Update from "+address+"("+values[2]+"): temp="+values[0]+" lum="+values[1]);
		
		ServiceHandler jsonParser = new ServiceHandler();
		String result = jsonParser.makeServiceCall("update.php", nameValuePairs);
		
		exchange.respond(ResponseCode.CREATED);
	}
}
