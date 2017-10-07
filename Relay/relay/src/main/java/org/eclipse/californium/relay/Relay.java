package org.eclipse.californium.relay;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Relay extends CoapServer {
	private final long DELAY = 20*1000;
	
	class MyTimerTask extends TimerTask{
		URI uri = null;

		@Override
		public void run() {

			//compute the preferred values for temp and lum
			if(!activeNodes.isEmpty()){
				ServiceHandler jsonParser = new ServiceHandler();
				String result = jsonParser.makeServiceCall("getPref.php", null);
				try {
                    JSONObject jsonObj = new JSONObject(result);
                    JSONArray roomsArray = jsonObj.getJSONArray("measures");

                    for (int i = 0; i < roomsArray.length();i++) {
                        JSONObject roomObj = (JSONObject) roomsArray.get(i);
                        String room = roomObj.getString("room");
                        String lum = roomObj.getString("lum");
                        String temp = roomObj.getString("temp");
                        
                        for(Node n : activeNodes){
                        	if(room.equals(n.getRoom())){
                        		try{
            						uri = new URI("coap://[" + n.getAddress() + "]:5683/temp");
            					} catch (Exception e) {
            			 			System.err.println("Caught Exception: " + e.getMessage());
            					}
            					CoapClient client = new CoapClient(uri);
            					client.post("temp="+temp, MediaTypeRegistry.TEXT_PLAIN);
            					
                        		try{
            						uri = new URI("coap://[" + n.getAddress() + "]:5683/lum");
            					} catch (Exception e) {
            			 			System.err.println("Caught Exception: " + e.getMessage());
            					}
                        		client = new CoapClient(uri);
            					client.post("lum="+lum, MediaTypeRegistry.TEXT_PLAIN);
                        	}
                        }
                    }

                } catch (JSONException e) { e.printStackTrace(); }
			}
			
			//update the list of active/inactive nodes
			if(!inactiveNodes.isEmpty() || !activeNodes.isEmpty()){
				List<String> rooms = new ArrayList<String>();
				List<Node> activeNodesNew = new ArrayList<Node>();
				
				//asks if for some of them a request of activation has been issued
				ServiceHandler jsonParser = new ServiceHandler();
				String result = jsonParser.makeServiceCall("whoIsActive.php", null);
				
				// Parse the JSON input
                try {
                    JSONObject jsonObj = new JSONObject(result);
                    JSONArray roomsArray = jsonObj.getJSONArray("rooms");

                    for (int i = 0; i < roomsArray.length();i++) {
                        JSONObject roomObj = (JSONObject) roomsArray.get(i);
                        rooms.add(roomObj.getString("room"));
                    }

                } catch (JSONException e) { e.printStackTrace(); }
				
				for(String room: rooms){
					//nodes that become active
					for(Node n : inactiveNodes){
						if(n.getRoom().equals(room)){
							activeNodesNew.add(n);
							try{
								uri = new URI("coap://[" + n.getAddress() + "]:5683/activate");
							} catch (Exception e) {
					 			System.err.println("Caught Exception: " + e.getMessage());
							}
							CoapClient client = new CoapClient(uri);
							client.post("mode=on", MediaTypeRegistry.TEXT_PLAIN);
						}
					}
					
					for(Node n : activeNodes){
						//nodes that remain active
						if(n.getRoom().equals(room)){
							//activeNodes.remove(n);
							activeNodesNew.add(n);
						}
					}
				}
				inactiveNodes.removeAll(activeNodesNew);
				activeNodes.removeAll(activeNodesNew);
				
				//nodes that become inactive
				for(Node n : activeNodes){
					inactiveNodes.add(n);
					try{
						uri = new URI("coap://[" + n.getAddress() + "]:5683/activate");
					} catch (Exception e) {
			 			System.err.println("Caught Exception: " + e.getMessage());
					}
					CoapClient client = new CoapClient(uri);
					client.post("mode=off", MediaTypeRegistry.TEXT_PLAIN);
				}
				activeNodes = activeNodesNew;
			}
			
			System.out.println("Inactive list: " + inactiveNodes);
			System.out.println("Active list: " + activeNodes);
			
			//restart the timer
			timer.schedule(new MyTimerTask(), DELAY);
		}
		
	}
	
	public List<Node> inactiveNodes, activeNodes;
	private Timer timer;
	
	public Relay() {
		super();

		inactiveNodes = new ArrayList<Node>();
		activeNodes = new ArrayList<Node>();

		/*TODO: add resources*/
		add(new RegisterResource("register", this));
		add(new UpdateResource("update"));
		
		//every minute
		timer=new Timer();
		timer.schedule(new MyTimerTask(), DELAY);
	}
	
	public static void main(String[] args) {
		Relay proxy = new Relay();
		/*
		 * An end-point is used by the server to expose resources to clients. I-s
		 * bound to a particular IP address and port
		 */
		proxy.addEndpoint(
				new CoapEndpoint(new InetSocketAddress("aaaa::1", 5683)));

		proxy.start();
	}
	
	@Override
	public void start() {
		super.start();
	}
}
