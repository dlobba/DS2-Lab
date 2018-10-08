package com.projects.gillo.epidemic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.projects.gillo.epidemic.messages.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class EpidemicMain {
	
	private static enum EpiType {
		PUSH,
		PULL,
		PUSHPULL
	}
	
	private static EpiType epiType = EpiType.PUSH;
	
	private static Set<ActorRef> createActors(ActorSystem system,
			EpiType type,
			int num) {
		Class<?> actorType = null;
		Set<ActorRef> actors = new HashSet<ActorRef>();
			switch (type) {
			case PULL:
				actorType = EpidemicPullActor.class;
				break;
			case PUSHPULL:
				actorType = EpidemicPushPullActor.class;
				break;	
			default:
				actorType = EpidemicPushActor.class;
				break;
			}
		Method m = null;
		try {
			m = actorType.getMethod("props", int.class);
		} catch (NoSuchMethodException | SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (int i = 0; i < num; i++) {
			try {
				actors.add(system.actorOf(((Props) m.invoke(null, i)).withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return actors;
	}
	
	
	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("Epidemic");
		Set<ActorRef> actors = createActors(system, epiType, 5);
		
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
				infected.tell(new InfectedMessage(new Random()
						.nextInt(Integer.MAX_VALUE)),
						null);
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.exit(0);
	}

}
