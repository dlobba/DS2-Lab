package com.projects.gillo.adaptive.gossip.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.projects.gillo.adaptive.gossip.Event;

public class Gossip implements Serializable {
	
	private List<Event> events;
	private long bufferSize;
	private long period;
	private int senderId;


	public Gossip(List<Event> events, long bufferSize, long period, int senderId) {
		this.events = Collections.unmodifiableList(events);
		this.bufferSize = bufferSize;
		this.period = period;
		this.senderId = senderId;
	}

	public List<Event> getEvents() {
		return events;
	}

	public long getBufferSize() {
		return bufferSize;
	}

	public long getPeriod() {
		return period;
	}
	
	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
}
