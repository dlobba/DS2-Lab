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
	
	// not used yet TODO
	private static int  H = 7;
	private static int  L = 5;
	private static double rH = 0.05;
	private static double rL = 0.05;
	private static double W = 0.5;
	private static double alpha = 0.8;
	// end not used yet TODO: DO IT
	
	
	private static long maxAge = 5; // age after which an event becomes old
									// and must be discarded, it's the **k** in the alg
	private static int numberInformed = 1; // it's the f in the algorithm, the amount of
										   // participants contacted in the multicast
	
	final long T = 1000; // gossip update timeout (ms)
	final long S = 1000; // minBuff update timeout (ms)
	
	
	private List<Event> events;
	private int eventIdGen;
	private static final int UPPER_MAX_EVENTS = 10;
	private final int maxEvents; // this is |events|_m in the paper
	
	final long delta = 2; // periods window (in periods)
	private long period; // current period, it's the **s** in the paper
	private long minBuff;
	private MinBuffList minBuffSerie;	

	int id;
	List<ActorRef> actors;

	public EpidemicActor(int id) {
		this.id = id;
		actors = new ArrayList<>();
		eventIdGen = 0;
		
		this.maxEvents = new Random().nextInt(UPPER_MAX_EVENTS) + 1;
		
		this.events = new ArrayList<>();
		this.period = this.delta - 1;
		this.minBuff = this.maxEvents; // default value;
		this.minBuffSerie = new MinBuffList(this.delta);
		for (long i = 0; i < this.delta; i++) {
			this.minBuffSerie.add(new MinBuff(i, this.maxEvents));
		}
	}
	
	static Props props(int id) {
		return Props.create(EpidemicActor.class,
				() -> new EpidemicActor(id));
	}
	
	void onStartMessage(StartMessage msg) {
		this.actors = new ArrayList<ActorRef>(msg.getActors());
		sendGossipTimeoutMsg();
		sendMinBuffTimeoutMsg();
	}
	
	void onBroadcast(BroadcastMsg msg) {
		// TODO: missing implementation
		Event event = new Event(this.eventIdGen, 
				"mex" + this.eventIdGen,
				this.id);
		this.eventIdGen += 1;
		this.events.add(event);
		System.out.printf("P%d added message %s\n", this.id, event.toString());
	}
	
	void onGossipTimeout(GossipTimeoutMsg msg) {
		for (Event event : events) {
			event.setAge(event.getAge() + 1);
		}
		Iterator<Event> eventIter = events.iterator();
		Event tempEvent = null;
		while (eventIter.hasNext()) {
			tempEvent = eventIter.next();
			if (tempEvent.getAge() > maxAge)
				eventIter.remove();
		}
		// at least the current period is in the serie
		MinBuff currentMinBuff = this.minBuffSerie.getMinBuff(this.period);
		Gossip gossip = new Gossip(this.events,
				currentMinBuff.bufferSize,
				this.period);
		
		List<ActorRef> tmpActors = new ArrayList<>(this.actors);
		for (int i = 0; i < numberInformed; i++) {
			int rand = new Random().nextInt(this.actors.size());
			ActorRef tmp = tmpActors.get(rand);
			tmpActors.remove(rand);
			// send gossip
			tmp.tell(gossip, this.getSelf());
		}
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
			if (eventIndex < 0) throw new IllegalStateException();
			
			Event tmpEvent = this.events.remove(eventIndex);
			if (tmpEvent.getAge() < event.getAge()) {
				tmpEvent.setAge(event.getAge());
			}
			this.events.add(tmpEvent);
			
			while (this.events.size() > this.maxEvents) {
				this.events.remove(getMaxEvent());
			}
		}
		
		// update the minBuffer
		MinBuff currentMinBuff = this.minBuffSerie.getMinBuff(this.period);
		if (this.period == msg.getPeriod() &&
				msg.getBufferSize() < currentMinBuff.bufferSize)
			currentMinBuff.bufferSize = msg.getBufferSize();
	}
	
	private void onMinBuffTimeout(MinBuffTimeoutMsg msg) {
		synchronized (msg) {			
			this.period += 1;
			MinBuff mb = new MinBuff(this.period, this.maxEvents);
			this.minBuffSerie.add(mb);
			this.minBuff = this.minBuffSerie.getMinBufferSize();
		}
		System.out.printf("P%d %s\n", this.id, this.minBuffSerie.toString());
		sendMinBuffTimeoutMsg();
	}
	
	private void deliver(Event event) {
		System.out.printf("P%d delivered %s\n", this.id, event.toString());
	}
	
	private int getEvent(int eid, int sid) {
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

	private Event getMaxEvent() {
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
	
	void sendGossipTimeoutMsg() {
		this.getContext()
        .getSystem()
        .scheduler()
        .scheduleOnce(Duration.create(this.T,
                TimeUnit.MILLISECONDS),
                this.getSelf(),
                new GossipTimeoutMsg(),
                getContext().system().dispatcher(),
                this.getSelf());
	}
	
	void sendMinBuffTimeoutMsg() {
//		System.out.printf("P%d started minBuff timeout\n", this.id);
		this.getContext()
        .getSystem()
        .scheduler()
        .scheduleOnce(Duration.create(this.T,
                TimeUnit.MILLISECONDS),
                this.getSelf(),
                new MinBuffTimeoutMsg(),
                getContext().system().dispatcher(),
                this.getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartMessage.class, this::onStartMessage)
				.match(GossipTimeoutMsg.class, this::onGossipTimeout)
				.match(MinBuffTimeoutMsg.class, this::onMinBuffTimeout)
				.match(Gossip.class, this::onReceive)
				.match(BroadcastMsg.class, this::onBroadcast)
				.build();
	}

}
