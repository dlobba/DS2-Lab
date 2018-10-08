package com.projects.gillo.adaptive.gossip.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.projects.gillo.adaptive.gossip.Event;

public class Gossip implements Serializable {
	
	private List<Event> events;
	private long bufferSize;
	
	public Gossip(List<Event> events, long bufferSize) {
		this.events = Collections.unmodifiableList(events);
		this.bufferSize = bufferSize;
	}

	public List<Event> getEvents() {
		return events;
	}

	public long getBufferSize() {
		return bufferSize;
	}
	
	

}
