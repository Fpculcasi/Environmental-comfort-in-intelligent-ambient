package org.eclipse.californium.relay;

public class Node {
	private String address;
	private String room;
	
	public Node(String address, String room){
		this.address = address;
		this.room = room;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getRoom() {
		return room;
	}
	public void setRoom(String room) {
		this.room = room;
	}
	@Override
	public String toString(){
		return "addr:"+address+", room:"+room;
	}
}
