package org.eclipse.californium.relay;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class RegisterResource extends CoapResource {
	Relay relay;
	
	public RegisterResource(String name, Relay relay) {
		super(name);
		getAttributes().setTitle("Resource for registration to the Relay");
		
		this.relay = relay;
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
		String address = exchange.getSourceAddress().toString();
		address = address.substring(1);
		String room = exchange.getRequestText();
		Node newNode = new Node(address, room);
		System.out.println("New node: address("+address+") room("+room+")");
		if(!relay.inactiveNodes.contains(newNode)){
			relay.inactiveNodes.add(newNode);
			System.out.println("Node "+newNode+" added to inactiveNodes list");
		}
		
		exchange.respond(ResponseCode.CREATED);
	}
}
