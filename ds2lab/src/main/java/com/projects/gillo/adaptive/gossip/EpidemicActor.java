package com.projects.gillo.adaptive.gossip;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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

	private static long maxAge = 5; // age after which an event becomes old
	// and must be discarded, it's the **k** in the alg
	private static int numberInformed = 3; // it's the f in the algorithm, the amount of
	// participants contacted in the multicast

	final long T = 1000; // gossip update timeout (ms)
	final long S = 2 * T; // minBuff update timeout (ms)


	private EventsList events;
	private int eventIdGen;
	private static final int UPPER_MAX_EVENTS = 50;
	private final int maxEvents; // this is |events|_m in the paper

	final long delta = 3; // periods window (in periods)
	private long period; // current period, it's the **s** in the paper
	private long minBuff;
	private MinBuffList minBuffSerie;	

	private long avgAge;
	private static final long TOKEN_MAX = 10L;
	private List<Long> tokensLog; // TODO: check for this
	private long tokens;
	private long tokenPeriod = 50;

	private int id;

	List<ActorRef> actors;

	public EpidemicActor(int id) {
		this.id = id;
		actors = new ArrayList<>();
		eventIdGen = 0;

		this.maxEvents = new Random().nextInt(UPPER_MAX_EVENTS) + 1;

		this.events = new EventsList();
		this.period = this.delta - 1;
		this.minBuff = this.maxEvents; // default value;
		this.minBuffSerie = new MinBuffList(this.delta);
		for (long i = 0; i < this.delta; i++) {
			this.minBuffSerie.add(new MinBuff(i, this.maxEvents));
		}
		this.avgAge = (H + L) / 2;
		this.tokens = TOKEN_MAX;
		this.tokensLog = new LinkedList<Long>();
		for(long i = 0; i < this.delta; i++) {
			this.tokensLog.add(TOKEN_MAX);
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
		//System.err.println("onBroadcast: " + this.events.size());
		while (this.tokens <= 0) {}
		this.tokens -= 1;
		this.updateTokensLog(this.tokens);
		Event event = new Event(this.eventIdGen, 
				"mex" + this.eventIdGen,
				this.id);
		this.eventIdGen += 1;
		this.events.add(event);
		System.out.printf("%d P%d P%d added message %s to the event list\n",
				System.currentTimeMillis(),
				this.id,
				this.id,
				event.toString());
	}

	void onGossipTimeout(GossipTimeoutMsg msg) {
//		System.out.printf("%d P%d P%d Received a GossipTimeout message\n",
//				System.currentTimeMillis(),
//				this.id,
//				this.id);
		//System.err.println("onGossipTimeout: " + this.events.size());
		this.events.updateAge();
		this.events.removeOld(maxAge);
		// at least the current period is in the serie
		MinBuff currentMinBuff = this.minBuffSerie.getMinBuff(this.period);
		Gossip gossip = new Gossip(this.events.getEvents(),
				currentMinBuff.bufferSize,
				this.period,
				this.id);
		System.out.printf("%d P%d P%d Broadcasted gossip P%d-%d\n",
				System.currentTimeMillis(),
				this.id,
				this.id,
				this.id,
				this.period);

		List<ActorRef> tmpActors = new ArrayList<>(this.actors);
		for (int i = 0; i < numberInformed; i++) {
			int rand = new Random().nextInt(tmpActors.size());
			ActorRef tmp = tmpActors.get(rand);
			tmpActors.remove(rand);
			// send gossip
			tmp.tell(gossip, this.getSelf());
		}

		long avgTokens = this.tokensLog.stream().mapToLong(token -> token).sum() / this.tokensLog.size();
		if (this.avgAge > H && avgTokens < TOKEN_MAX / 2 &&
				new Random().nextDouble() > W) {
			this.tokenPeriod *= (1 + rH);
			System.out.printf("%d P%d P%d Increased the token period to %d\n",
					System.currentTimeMillis(),
					this.id,
					this.id,
					this.tokenPeriod);
		}
		if (this.avgAge < L && avgTokens > TOKEN_MAX / 2) {
			this.tokenPeriod *= (1 - rL);
			System.out.printf("%d P%d P%d Decreased the token period to %d\n",
					System.currentTimeMillis(),
					this.id,
					this.id,
					this.tokenPeriod);
		}

		sendGossipTimeoutMsg();
	}

	void onReceive(Gossip msg) {
		System.out.printf("%d P%d P%d Received gossip P%d-%d\n",
				System.currentTimeMillis(),
				this.id,
				msg.getSenderId(),
				msg.getSenderId(),
				msg.getPeriod());
		//System.err.println("onReceive: " + this.events.size());
		for (Event event : msg.getEvents()) {

			// if message has same id and same senderId
			// then the message is the same
			if (!this.events.getEvents().contains(event)) {
				this.events.add(event);
				deliver(event);
			}

			int eventIndex = this.events.getEvent(event.getId(), event.getSenderId());
			if (eventIndex < 0) throw new IllegalStateException();

			Event tmpEvent = this.events.remove(eventIndex);
			if (tmpEvent.getAge() < event.getAge()) {
				tmpEvent.setAge(event.getAge());
			}
			this.events.add(tmpEvent);
		}

		List<Event> notLost;
		int sizeNotLost;
		Event oldest;
		/*do {
			notLost = this.events.getEventsNotLost();
			sizeNotLost = notLost.size();
			oldest = this.events.getOldestEventNotLost();
			this.avgAge = (long)(alpha * avgAge +
					(1 - alpha) * oldest.getAge());
			oldest.setLost();
		}while (!(sizeNotLost > this.minBuff));*/
		notLost = this.events.getEventsNotLost();
		sizeNotLost = notLost.size();
		while (sizeNotLost > this.minBuff) {
			oldest = this.events.getOldestEventNotLost();
			this.avgAge = (long)(alpha * avgAge +
					(1 - alpha) * oldest.getAge());
			oldest.setLost();
			notLost = this.events.getEventsNotLost();
			sizeNotLost = notLost.size();
		}

		while (this.events.size() > this.maxEvents) {
			this.events.removeOld(maxAge); 
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
			/*System.out.printf("%d P%d P%d %s\n",
					System.currentTimeMillis(),
					this.id,
					this.id,
					this.minBuffSerie.toString());*/
		}
		sendMinBuffTimeoutMsg();
	}

	private void updateTokensLog(long newTokenValue) {
		this.tokensLog.add(this.tokens);
		this.tokensLog.remove(0);
	}

	private void onTokenTimeout(TokenTimeoutMsg msg) {
		if (this.tokens < TOKEN_MAX)
			this.tokens += 1;
		this.updateTokensLog(this.tokens);
		this.sendTokenTimeoutMsg();
	}

	public int getId() {
		return id;
	}

	private void deliver(Event event) {
		System.out.printf("%d P%d P%d delivered %s\n",
				System.currentTimeMillis(),
				this.id,
				event.getSenderId(),
				event.toString());
	}

	void sendTokenTimeoutMsg() {
		this.getContext()
		.getSystem()
		.scheduler()
		.scheduleOnce(Duration.create(this.tokenPeriod,
				TimeUnit.MILLISECONDS),
				this.getSelf(),
				new TokenTimeoutMsg(),
				getContext().system().dispatcher(),
				this.getSelf());
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
		this.getContext()
		.getSystem()
		.scheduler()
		.scheduleOnce(Duration.create(this.S,
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
				.match(TokenTimeoutMsg.class, this::onTokenTimeout)
				.match(MinBuffTimeoutMsg.class, this::onMinBuffTimeout)
				.match(Gossip.class, this::onReceive)
				.match(BroadcastMsg.class, this::onBroadcast)
				.build();
	}

}
