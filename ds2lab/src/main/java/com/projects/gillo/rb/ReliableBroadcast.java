package com.projects.gillo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

public class ReliableBroadcast extends AbstractActor {
	
	public static class SendBroadcast implements Serializable {};
	
	public final static int BROADCAST_INTERLEAVING = 5;
	
	private int id;
	private int messageId;
	private Set<Message> delivered;
	private List<ActorRef> actors;
	
	public ReliableBroadcast(int id) {
		super();
		this.id = id;
		this.messageId = 0;
		this.delivered = new HashSet<Message>();
		this.actors = new ArrayList<ActorRef>();
	}
	
	public static Props props(int id) {
		return Props.create(ReliableBroadcast.class,
				() -> new ReliableBroadcast(id));
    }
	
	public void send(Object msg, List<ActorRef> actors) {
		for (ActorRef actor : actors) {
			actor.tell(msg, this.getSelf());
		}
	}
	
	public void Rbroadcast(Message msg) {
		List<ActorRef> others = new ArrayList<ActorRef>(this.actors);
		others.remove(this.getSelf());
		this.send(msg, others);
		System.out.printf("%s has been broadcast\n", msg.toString());
		System.out.printf("%s has been delivered from p%d\n", msg.toString(), this.id);
		this.delivered.add(msg);
	}
	
	public void onReceiveMessage(Message msg) {
		if (this.delivered.contains(msg))
			// already delivered
			System.out.printf("%s has **already** been delivered from p%d\n", msg.toString(), this.id);
		else {
			List<ActorRef> others = new ArrayList<ActorRef>(this.actors);
			others.remove(this.getSelf());
			others.remove(this.getSender());
			this.send(msg, others);
			System.out.printf("%s has been delivered from p%d\n", msg.toString(), this.id);
			this.delivered.add(msg);
		}
	}
	
	public void onReceiveGroupMsg(GroupMessage msg) {
		this.actors.clear();
		for (ActorRef actor : msg.getGroup()) {
			this.actors.add(actor);
		}
		//System.out.printf("length: %d\n", this.actors.size());
		this.getSelf().tell(new SendBroadcast(), null);
	}

	private void spamMessage() {
		Message msg = new Message("ciao"+(new Random().nextInt(Integer.MAX_VALUE)),
				this.messageId, this.id);
		this.messageId += 1;
		
		this.Rbroadcast(msg);
	}
	
	public void onSendBroadcast(SendBroadcast m) {
		this.spamMessage();
		
		int time = new Random().nextInt(BROADCAST_INTERLEAVING) + 1;
		
		this.getContext()
        .getSystem()
        .scheduler()
        .scheduleOnce(Duration.create(time,
                TimeUnit.SECONDS),
                this.getSelf(),
                new SendBroadcast(),
                getContext().system().dispatcher(),
                this.getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, this::onReceiveMessage)
				.match(SendBroadcast.class, this::onSendBroadcast)
				.match(GroupMessage.class, this::onReceiveGroupMsg)
				.build();
	}

}
