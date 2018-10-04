package com.projects.gillo.rb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import akka.actor.ActorRef;

public class GroupMessage implements Serializable {
	
	private List<ActorRef> group;

	public GroupMessage(List<ActorRef> group) {
		super();
		this.group = Collections.unmodifiableList(group);
	}
	
	public List<ActorRef> getGroup() {
		return new ArrayList<>(this.group);
	}
}
