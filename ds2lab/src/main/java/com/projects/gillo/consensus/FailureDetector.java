package com.projects.gillo.consensus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;

public class FailureDetector {
	// map each process to the tick it is going
	// to crash in
	private final Map<Integer, Long> failureList;
	
	public FailureDetector(Map<Integer, Long> failureList) {
		this.failureList = new HashMap<Integer, Long>();
		this.failureList.putAll(failureList);
	}
	
	public long getFailureTimeOf(int processId) {
		Long tmp = this.failureList.get(processId);
		return tmp == null ? 0 : tmp.longValue();
	}
	
	public Set<Integer> getSuspectsAt(long tick) {
		Set<Integer> suspects = new HashSet<>();
		for (Map.Entry<Integer, Long> entry : failureList.entrySet()) {
			if (entry.getValue() <= tick)
				suspects.add(entry.getKey());
		}
		return suspects;
	}
}
