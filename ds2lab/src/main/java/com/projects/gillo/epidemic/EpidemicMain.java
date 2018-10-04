package com.projects.gillo.epidemic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.projects.gillo.rb.GroupMessage;
import com.projects.gillo.rb.ReliableBroadcast;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class EpidemicMain {
	
	private static enum EpiType {
		PUSH,
		PULL,
		PUSHPULL
	}
	
	private static EpiType epiType = EpiType.PUSH;
	
	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("Epidemic");
		
		Set<ActorRef> actors = new HashSet<ActorRef>();
		
		for (int i = 0; i < 5; i++) {
			actors.add(system.actorOf(EpidemicPushActor.props(i)
					.withDispatcher("akka.actor.my-pinned-dispatcher"),
					"actor" + i));
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
				System.out.printf("Started infection\n");
				infected.tell(new InfectedMessage(new Random().nextInt(Integer.MAX_VALUE)),
						null);
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
