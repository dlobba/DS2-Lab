package com.projects.gillo.adaptive.gossip;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.projects.gillo.adaptive.gossip.messages.BroadcastMsg;
import com.projects.gillo.adaptive.gossip.messages.StartMessage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class EpidemicMain {
	
	public static final int nparticipants = 5;
	
	
	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("Epidemic");
		List<ActorRef> actors = new ArrayList<>();
		
		for (int i = 0; i < nparticipants; i++) {
			actors.add(system.actorOf(EpidemicActor
					.props(i)
					.withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
		}
		
		StartMessage groupMsg = new StartMessage(actors);

		system.actorOf(EventGenerator.props(actors, 1000).withDispatcher("akka.actor.my-pinned-dispatcher"),"PincopAllo");
		
		for (ActorRef actor : actors) {
			actor.tell(groupMsg, null);
		}
		
	}

}
