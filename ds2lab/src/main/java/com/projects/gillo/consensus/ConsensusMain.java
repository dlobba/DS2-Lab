package com.projects.gillo.consensus;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class ConsensusMain {
public static final int nparticipants = 5;
	
	private static Map<Integer, ActorRef> participants;

	private static void broadcast(Object msg) {
		for (ActorRef participant: participants.values()) {
			participant.tell(msg, null);
		}
	}

	public static void main(String[] args) {
		
		ActorSystem system = ActorSystem.create("Consensus");
		participants = new HashMap<Integer, ActorRef>();
		
		GlobalClock clock = GlobalClock.getInstance();
		int i = 0;
		participants.put(i, system.actorOf(ConsensusActor.props(i, clock, ConsensusActor.CrashFailure.WHEN_COORDINATOR)
				.withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
		i += 1;
		participants.put(i, system.actorOf(ConsensusActor.props(i, clock, ConsensusActor.CrashFailure.WHEN_COORDINATOR)
				.withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
		i += 1;
		participants.put(i, system.actorOf(ConsensusActor.props(i, clock, ConsensusActor.CrashFailure.WHEN_COORDINATOR)
				.withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
		i += 1;
		participants.put(i, system.actorOf(ConsensusActor.props(i, clock, ConsensusActor.CrashFailure.NONE)
				.withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
		i += 1;
		participants.put(i, system.actorOf(ConsensusActor.props(i, clock, ConsensusActor.CrashFailure.NONE)
				.withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
		i += 1;

		Map<Integer, Long> failureTimes = new HashMap<>();
		for (i = 0; i < nparticipants; i++) {
			failureTimes.put(i, 0l);
		}
		failureTimes.put(0, 0l);
		failureTimes.put(1, 50l);
		failureTimes.put(2, 500l);
		failureTimes.put(3, 500l);
		failureTimes.put(4, 500l);

		StartMessage startMsg = new StartMessage(participants, failureTimes, failureTimes);
		broadcast(startMsg);
		ProposeMsg proposal = new ProposeMsg(42);
		broadcast(proposal);
	}
}
