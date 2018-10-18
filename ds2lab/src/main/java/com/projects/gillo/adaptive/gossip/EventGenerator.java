package com.projects.gillo.adaptive.gossip;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.projects.gillo.adaptive.gossip.messages.BroadcastMsg;
import com.projects.gillo.adaptive.gossip.messages.Gossip;
import com.projects.gillo.adaptive.gossip.messages.GossipTimeoutMsg;
import com.projects.gillo.adaptive.gossip.messages.MinBuffTimeoutMsg;
import com.projects.gillo.adaptive.gossip.messages.StartMessage;
import com.projects.gillo.adaptive.gossip.messages.TokenTimeoutMsg;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

public class EventGenerator extends AbstractActor{
	public static class GenerateMsg implements Serializable{};
	private List<ActorRef> actors;
	private long rate;

	public EventGenerator(List<ActorRef> actors, long rate) {
		this.actors = actors;
		this.rate = rate;
		sendGenerateMsg();
	}
	
	static Props props(List<ActorRef> actors, long rate) {
		//System.out.printf("%l P%d Actor created", System.currentTimeMillis(),id);
		return Props.create(EventGenerator.class,
				() -> new EventGenerator(actors,rate));
	}
	
	public void onGenerateMsg(GenerateMsg msg) {
		int chosen = new Random().nextInt(actors.size());
		ActorRef infected = (ActorRef) actors.toArray()[chosen];
		System.out.printf("%d p%d PA Started infection\n", System.currentTimeMillis(), chosen);
		infected.tell(new BroadcastMsg(), null);
		if(new Random().nextInt() == 42) {
			System.exit(0);
		}
		sendGenerateMsg();
	}
	
	void sendGenerateMsg() {
		this.getContext()
		.getSystem()
		.scheduler()
		.scheduleOnce(Duration.create(this.rate,
				TimeUnit.MILLISECONDS),
				this.getSelf(),
				new GenerateMsg(),
				getContext().system().dispatcher(),
				this.getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(GenerateMsg.class, this::onGenerateMsg)
				.build();
	}

	
}
