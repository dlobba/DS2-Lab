package com.projects.gillo.adaptive.gossip.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import akka.actor.ActorRef;

public class StartMessage implements Serializable {
	
	private List<ActorRef> actors;
	
	public List<ActorRef> getActors() {
		return actors;
	}

	public StartMessage(List<ActorRef> actors) {
		this.actors = Collections.unmodifiableList(actors);
	}
}
