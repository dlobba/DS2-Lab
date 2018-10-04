package com.projects.gillo.rb;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class RBMain {

	public static void main(String[] args) {
		final ActorSystem system = ActorSystem.create("Reliable_Broadcast");
		
		List<ActorRef> actors = new ArrayList<>();
		
		for (int i = 0; i < 5; i++) {
			actors.add(system.actorOf(ReliableBroadcast.props(i)
					.withDispatcher("akka.actor.my-pinned-dispatcher"),
					"actor" + i));
		}
		
		GroupMessage groupMsg = new GroupMessage(actors);
		for (ActorRef actor : actors) {
			actor.tell(groupMsg, null);
		}
		
	}
	
}
