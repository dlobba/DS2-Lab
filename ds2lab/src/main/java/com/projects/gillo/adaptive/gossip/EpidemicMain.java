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
		for (ActorRef actor : actors) {
			actor.tell(groupMsg, null);
		}
		
		// generate a series of update, giving
		// 5s to the system to get up to date.
		// note: it can happen that within this interval
		// some node could not be able to receive the update
		for (int i = 0; i < 3; i++) {
			try {
				ActorRef infected = (ActorRef) actors
						.toArray()[new Random()
						           .nextInt(actors.size())];
				System.out.printf("Started infection %d\n", i);
				infected.tell(new BroadcastMsg(), null);
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.exit(0);
	}

}
