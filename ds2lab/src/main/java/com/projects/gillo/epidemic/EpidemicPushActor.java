package com.projects.gillo.epidemic;

import scala.util.Random;

import akka.actor.ActorRef;
import akka.actor.Props;

public class EpidemicPushActor  extends EpidemicActor {

	public EpidemicPushActor(int id) {
		super(id);
	}
	
	public static Props props(int id) {
		return Props.create(EpidemicPushActor.class,
				() -> new EpidemicPushActor(id));
	}

	@Override
	void onEpidemicTimeout(EpidemicTimeoutMsg msg) {
		int victimIndex = new Random().nextInt(this.actors.size());
		ActorRef victim = (ActorRef)this.actors.toArray()[victimIndex];
		victim.tell(new PushMsg(this.timestamp, this.variable), this.getSelf());
		this.sendTimeoutMsg();
	}

	@Override
	void onEpidemicReceive(EpidemicMsg em) {
		if (!(em instanceof PushMsg))
			return;
		PushMsg msg = (PushMsg) em;
		if (this.timestamp < msg.getTimestamp()) {
			this.variable = msg.getVariable();
			this.timestamp = msg.getTimestamp();
			System.out.printf("Node %d has been infected, value %s\n", this.id, this.variable.toString());
		}
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartMessage.class, this::onStartMessage)
				.match(EpidemicTimeoutMsg.class, this::onEpidemicTimeout)
				.match(EpidemicMsg.class, this::onEpidemicReceive)
				.match(InfectedMessage.class, this::onInfectedMsg)
				.build();
	}
}
