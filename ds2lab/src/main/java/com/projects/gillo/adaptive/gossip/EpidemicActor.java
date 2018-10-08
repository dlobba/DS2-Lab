package com.projects.gillo.adaptive.gossip;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.projects.gillo.adaptive.gossip.messages.*;

import akka.actor.AbstractActor;
import scala.concurrent.duration.Duration;
import scala.util.Random;
import akka.actor.ActorRef;
import akka.actor.Props;

public class EpidemicActor extends AbstractActor {
	
	public static final int RANGE = 10;
	// min events number within events
	public static final int MIN_SIZE = 5;
	
	private static int  H = 7;
	private static int  L = 5;
	private static double rH = 0.05;
	private static double rL = 0.05;
	private static double W = 0.5;
	private static double alpha = 0.8;
	private long k;
	
	private int f = 1;
	
	int id;
	final long timeoutPeriod = 100;
	final long delta = 2;
	final long period;
	
	private List<Event> events;
	private int eventIdGen;
	
	private long bufferSize;
	private long minBufferSize;
	
	List<ActorRef> actors;
	
	public EpidemicActor(int id) {
		this.id = id;
		events = new ArrayList<>();
		eventIdGen = 0;
		bufferSize = new Random().nextInt(RANGE) + MIN_SIZE;
		minBufferSize = bufferSize;
		actors = new ArrayList<>();
		this.period = this.delta;
	}
	
	static Props props(int id) {
		return Props.create(EpidemicActor.class,
				() -> new EpidemicActor(id));
	}
	
	void sendTimeoutMsg() {
		this.getContext()
        .getSystem()
        .scheduler()
        .scheduleOnce(Duration.create(this.timeoutPeriod,
                TimeUnit.MILLISECONDS),
                this.getSelf(),
                new TimeoutMsg(),
                getContext().system().dispatcher(),
                this.getSelf());
	}
	
	void onStartMessage(StartMessage msg) {
		this.actors = new ArrayList<ActorRef>(msg.getActors());
		sendTimeoutMsg();
	}
	
	void onInfectedMsg(InfectedMessage msg) {
		Event event = new Event(this.eventIdGen, 
				this.minBufferSize,
				"mex" + this.eventIdGen,
				this.id);
		this.eventIdGen += 1;
		this.events.add(event);
	}
	
	private long maxAge() {
		long max = 0;
		for (Event event : events) {
			if (event.getAge() > max)
				max = event.getAge();
		}
		return max;
	}
	
	private Event getMaxEvent() {
		long max = maxAge();
		for (Event event : events) {
			if (event.getAge() == max)
				return event;
		}
		return null;
	}
	

	void onTimeout(TimeoutMsg msg) {
		for (Event event : events) {
			event.setAge(event.getAge() + 1);
		}
		k = maxAge();
		Iterator<Event> eventIter = events.iterator();
		Event tempEvent = null;
		while (eventIter.hasNext()) {
			tempEvent = eventIter.next();
			if (tempEvent.getAge() >= k)
				eventIter.remove();
		}
		//TODO: bufferSize vs minBufferSize
		Gossip gossip = new Gossip(this.events, this.minBufferSize);
		
		List<ActorRef> tmpActors = new ArrayList<>(this.actors);
		for (int i = 0; i < this.f; i++) {
			int rand = new Random().nextInt(this.actors.size());
			ActorRef tmp = tmpActors.get(rand);
			tmpActors.remove(rand);
			// send gossip
			tmp.tell(gossip, this.getSelf());
		}
	}
	
	private void deliver(Event event) {
		System.out.printf("Delivered %s\n", event.toString());
	}
	
	private int getEvent(int eid, int sid) {
		for (Event event : events) {
			if (event.getId() == eid && event.getSenderId() == sid)
				return events.indexOf(event);
		}
		return -1;
	}
	
	void onReceive(Gossip msg) {
		for (Event event : msg.getEvents()) {
			
			// if message has same id and same senderId
			// then the message is the same
			if (!this.events.contains(event)) {
				this.events.add(event);
				deliver(event);
			}
			
			int eventIndex = getEvent(event.getId(), event.getSenderId());
			if (eventIndex > -1) throw new IllegalStateException();
			
			Event tmpEvent = this.events.remove(eventIndex);
			if (tmpEvent.getAge() < event.getAge()) {
				tmpEvent.setAge(event.getAge());
			}
			this.events.add(tmpEvent);
			
			while (this.events.size() > this.minBufferSize) {
				this.events.remove(getMaxEvent());
			}
			
		}
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartMessage.class, this::onStartMessage)
				.match(TimeoutMsg.class, this::onTimeout)
				.match(Gossip.class, this::onReceive)
				.match(InfectedMessage.class, this::onInfectedMsg)
				.build();
	}

}
