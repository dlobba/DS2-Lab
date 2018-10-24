package com.projects.gillo.consensus;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import akka.actor.ActorRef;

public class StartMessage implements Serializable {
	
	private Map<Integer, ActorRef> participants;
	private Map<Integer, Long> processFailureList;
	private Map<Integer, Long> globalFailureList;
	
	
	public StartMessage(Map<Integer, ActorRef> participants,
			Map<Integer, Long> processFailureList,
			Map<Integer, Long> globalFailureList) {
		super();
		this.participants = Collections.unmodifiableMap(participants);
		this.processFailureList = Collections.unmodifiableMap(processFailureList);
		this.globalFailureList = Collections.unmodifiableMap(globalFailureList);
	}

	public Map<Integer, ActorRef> getParticipants() {
		return participants;
	}


	public Map<Integer, Long> getProcessFailureList() {
		return processFailureList;
	}


	public Map<Integer, Long> getGlobalFailureList() {
		return globalFailureList;
	}

}
