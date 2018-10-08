package com.projects.gillo.adaptive.gossip;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import akka.actor.ActorSystem;

public class EpidemicMain {
	
	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("Epidemic");
//		Set<ActorRef> actors = createActors(system, epiType, 5);
		
//		StartMessage groupMsg = new StartMessage(actors);
//		for (ActorRef actor : actors) {
//			actor.tell(groupMsg, null);
//		}
		
		// generate a series of update, giving
		// 5s to the system to get up to date.
		// note: it can happen that within this interval
		// some node could not be able to receive the update
//		for (int i = 0; i < 3; i++) {
//			try {
//				ActorRef infected = (ActorRef) actors
//						.toArray()[new Random()
//						           .nextInt(actors.size())];
//				System.out.printf("Started infection %d\n", i);
//				infected.tell(new InfectedMessage(new Random()
//						.nextInt(Integer.MAX_VALUE)),
//						null);
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		System.exit(0);
	}

}
