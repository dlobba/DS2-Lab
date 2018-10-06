package com.projects.gillo.epidemic;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.projects.gillo.epidemic.messages.EpidemicTimeoutMsg;
import com.projects.gillo.epidemic.messages.InfectedMessage;
import com.projects.gillo.epidemic.messages.StartMessage;

import akka.actor.AbstractActor;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;

public abstract class EpidemicActor extends AbstractActor {
	
	int id;
	Object variable;
	final long delta = 100;
	Set<ActorRef> actors;
	long timestamp;
	
	public EpidemicActor(int id) {
		this.id = id;
		this.timestamp = 0;
		this.variable = 0;
		actors = new HashSet<>();
	}
	
	void sendTimeoutMsg() {
		this.getContext()
        .getSystem()
        .scheduler()
        .scheduleOnce(Duration.create(this.delta,
                TimeUnit.MILLISECONDS),
                this.getSelf(),
                new EpidemicTimeoutMsg(),
                getContext().system().dispatcher(),
                this.getSelf());
	}
	
	void onStartMessage(StartMessage msg) {
		this.actors = new HashSet<ActorRef>(msg.getActors());
		sendTimeoutMsg();
	}
	
	void onInfectedMsg(InfectedMessage msg) {
		this.variable = msg.getVariable();
		this.timestamp = System.currentTimeMillis();
				
	}

	abstract void onEpidemicTimeout(EpidemicTimeoutMsg msg);
	abstract void onEpidemicReceive(EpidemicMsg msg);

	
}
