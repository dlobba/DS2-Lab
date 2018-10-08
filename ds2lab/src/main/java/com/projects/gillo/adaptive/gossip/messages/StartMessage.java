package com.projects.gillo.adaptive.gossip.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import akka.actor.ActorRef;

public class StartMessage implements Serializable {
	
	private Set<ActorRef> actors;
	
	public Set<ActorRef> getActors() {
		return actors;
	}

	public StartMessage(Set<ActorRef> actors) {
		this.actors = Collections.unmodifiableSet(actors);
	}
}
