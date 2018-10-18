package com.projects.gillo.adaptive.gossip;

import java.io.FileNotFoundException;
import java.io.PrintStream;
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
		PrintStream file= null;
		try {
			file = new PrintStream("log.txt");
			System.setOut(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ActorSystem system = ActorSystem.create("Epidemic");
		List<ActorRef> actors = new ArrayList<>();
		
		for (int i = 0; i < nparticipants; i++) {
			actors.add(system.actorOf(EpidemicActor
					.props(i).withDispatcher("akka.actor.my-pinned-dispatcher")
					, "actor" + i));//.withDispatcher("akka.actor.my-pinned-dispatcher")
		}
		

		system.actorOf(EventGenerator
				.props(actors, 1000).withDispatcher("akka.actor.my-pinned-dispatcher")
				, "PincopAllo");

		StartMessage groupMsg = new StartMessage(actors);
		for (ActorRef actor : actors) {
			actor.tell(groupMsg, null);
		}
		
	}

}
