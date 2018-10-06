package com.projects.gillo.epidemic;

import com.projects.gillo.epidemic.messages.EpidemicTimeoutMsg;
import com.projects.gillo.epidemic.messages.InfectedMessage;
import com.projects.gillo.epidemic.messages.PushPullMsg;
import com.projects.gillo.epidemic.messages.ReplyMsg;
import com.projects.gillo.epidemic.messages.StartMessage;

import akka.actor.ActorRef;
import akka.actor.Props;
import scala.util.Random;

public class EpidemicPushPullActor extends EpidemicActor {

	public EpidemicPushPullActor(int id) {
		super(id);
	}
	
	public static Props props(int id) {
		return Props.create(EpidemicPushPullActor.class,
				() -> new EpidemicPushPullActor(id));
	}
	
	@Override
	void onEpidemicTimeout(EpidemicTimeoutMsg msg) {
		int victimIndex = new Random().nextInt(this.actors.size());
		ActorRef victim = (ActorRef)this.actors.toArray()[victimIndex];
		victim.tell(new PushPullMsg(this.timestamp, this.id, this.variable),
				this.getSelf());
		this.sendTimeoutMsg();
	}
	
	@Override
	void onEpidemicReceive(EpidemicMsg msg) {
		if (!(msg instanceof PushPullMsg))
			return;
		PushPullMsg ppm = (PushPullMsg) msg;
		if (this.timestamp < ppm.getTimestamp()) {
			this.timestamp = ppm.getTimestamp();
			this.variable = ppm.getVariable();
			System.out.printf("Node %d has been infected by node %d!\n",
					this.id,
					msg.sid);
		} else if (this.timestamp < ppm.getTimestamp()) {
			this.getSender().tell(new ReplyMsg(this.timestamp,
					this.id,
					this.variable),
					this.getSelf());
			System.out.printf("Node %d has an old value, " +
					"it will be infected by node %d!\n",
					msg.sid,
					this.id);
		}
	}

	void onReplyMsg(ReplyMsg msg) {
		if (this.timestamp < msg.getTimestamp()) {
			this.variable = msg.getVariable();
			this.timestamp = msg.getTimestamp();
			System.out.printf("Node %d has received a new value from %d, value: %s\n",
					this.id,
					msg.sid,
					msg.getVariable());
		}
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartMessage.class, this::onStartMessage)
				.match(EpidemicTimeoutMsg.class, this::onEpidemicTimeout)
				.match(EpidemicMsg.class, this::onEpidemicReceive)
				.match(InfectedMessage.class, this::onInfectedMsg)
				.match(ReplyMsg.class, this::onReplyMsg)
				.build();
	}
	
}
