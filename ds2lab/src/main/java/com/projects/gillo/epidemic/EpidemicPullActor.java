package com.projects.gillo.epidemic;

import com.projects.gillo.epidemic.messages.EpidemicTimeoutMsg;
import com.projects.gillo.epidemic.messages.InfectedMessage;
import com.projects.gillo.epidemic.messages.PullMsg;
import com.projects.gillo.epidemic.messages.ReplyMsg;
import com.projects.gillo.epidemic.messages.StartMessage;

import akka.actor.ActorRef;
import akka.actor.Props;
import scala.util.Random;

public class EpidemicPullActor extends EpidemicActor {

	public EpidemicPullActor(int id) {
		super(id);
	}
	
	public static Props props(int id) {
		return Props.create(EpidemicPullActor.class,
				() -> new EpidemicPullActor(id));
	}
	
	@Override
	void onEpidemicTimeout(EpidemicTimeoutMsg msg) {
		int victimIndex = new Random().nextInt(this.actors.size());
		ActorRef victim = (ActorRef)this.actors.toArray()[victimIndex];
		victim.tell(new PullMsg(this.timestamp, this.id), this.getSelf());
		this.sendTimeoutMsg();
	}

	@Override
	void onEpidemicReceive(EpidemicMsg msg) {
		if (!(msg instanceof PullMsg))
			return;
		PullMsg pm = (PullMsg) msg;
		if (this.timestamp > pm.getTimestamp()) {
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
