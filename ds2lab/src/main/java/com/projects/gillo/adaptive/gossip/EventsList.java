package com.projects.gillo.adaptive.gossip;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EventsList {
	
	private List<Event> events;
	
	public EventsList() {
		this.events = new ArrayList<Event>();
	}
	
	public boolean add(Event event) {
		return this.events.add(event);
	}
	
	public void updateAge() {
		for (Event event : events) {
			event.setAge(event.getAge() + 1);
		}
	}
	
	public void removeOld(long maxAge) {
		Iterator<Event> eventIter = events.iterator();
		Event tempEvent = null;
		while (eventIter.hasNext()) {
			tempEvent = eventIter.next();
			if (tempEvent.getAge() > maxAge)
				eventIter.remove();
		}
	}
	
	public Event getOldestEvent() {
		long max = -1;
		Event temp = null;
		for (Event event : events) {
			if (event.getAge() > max) {
				temp = event;
				max = event.getAge();
			}
		}
		return temp;
	}
	
	public Event getOldestEventNotLost() {
		long max = -1;
		Event temp = null;
		for (Event event : events) {
			if (!event.isLost() && event.getAge() > max) {
				temp = event;
				max = event.getAge();
			}
		}
		return temp;
	}

	public List<Event> getEvents() {
		return new ArrayList<Event>(events);
	}
	
	public int getEvent(int eid, int sid) {
		Iterator<Event> eventIter = this.events.iterator();
		Event event;
		int index = 0;
		while (eventIter.hasNext()) {
			event = eventIter.next();
			if (event.getId() == eid && event.getSenderId() == sid)
				return index;
			index += 1;
		}
		return -1;
	}
	
	public Event remove(int index) {
		return this.events.remove(index);
	}
	
	public boolean remove(Event event) {
		return this.events.remove(event);
	}
	
	public int size() {
		return this.events.size();
	}
	
	public List<Event> getEventsNotLost() {
		List<Event> nlost = new ArrayList<>();
		for (Event event : events) {
			if (!event.isLost())
				nlost.add(event);
		}
		return nlost;
	}
	
}
