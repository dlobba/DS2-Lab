package com.projects.gillo.gnutella;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class GnutellaMain {
	private static Map<Integer, ActorRef> participants;

	private static int MAX_NODES = 18;
	private static int STARTING_NODES = 10;
	private static int SEED = 42;
	private static double P = 0.35d;
	private static int[][] matrix = new int[MAX_NODES][MAX_NODES];

	public static void main(String[] args) {
		
		ActorSystem system = ActorSystem.create("Gnutella");
		participants = new HashMap<Integer, ActorRef>();
		
		for(int i = 0; i < MAX_NODES; i++) {
			participants.put(i, system.actorOf(GnutellaActor.props(i)
					.withDispatcher("akka.actor.my-pinned-dispatcher"), "actor" + i));
		}
		List<Integer> startingNodes = new ArrayList<>(participants.keySet());
		Collections.shuffle(startingNodes, new Random(SEED));
		Random rnd = new Random(SEED);
		
		startingNodes = startingNodes.subList(0, STARTING_NODES);
		ActorRef source;
		ActorRef dest;
		double outcome = 0d;
		for (Integer id : startingNodes) {
			source = participants.get(id);
			for (Integer destId : startingNodes) {
				if (id != destId) {
					dest = participants.get(destId);
					outcome = rnd.nextDouble();
					if (outcome <= P ) {
						matrix[id][destId] = 1;
						source.tell(new StartMessage(dest, destId), null);
					}
				}
			}
		}
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++)
				System.out.print(matrix[i][j] + " ");
			System.out.print("\n");
		}
		
		int index = rnd.nextInt(STARTING_NODES);
		int triggered = startingNodes.get(index);
		System.out.printf("Starting nodes %s\nInitiator: %d\n",
				startingNodes.toString(),
				triggered);
		long start = System.currentTimeMillis();
		System.out.printf("Start at %d\n", start);
		participants.get(triggered).tell(new GnutellaActor.StartPingMsg(), null);
		try {
			long offset = 1861;
			Thread.sleep(GnutellaActor.MAX_AGE + offset);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.printf("------------------------------------\n");
		long round1 = System.currentTimeMillis();
		System.out.printf("Start at %d, after %d\n", round1, round1 - start);
		participants.get(triggered).tell(new GnutellaActor.StartPingMsg(), null);
	}
}
