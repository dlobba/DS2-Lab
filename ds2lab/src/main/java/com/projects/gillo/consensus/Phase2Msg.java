package com.projects.gillo.consensus;

import java.io.Serializable;

public class Phase2Msg implements Serializable {

	private int round;
	private Object estimate;
	private int senderId;
	public Phase2Msg(int round, Object estimate, int senderId) {
		super();
		this.round = round;
		this.estimate = estimate;
		this.senderId = senderId;
	}
	public int getRound() {
		return round;
	}
	public Object getEstimate() {
		return estimate;
	}
	public int getSenderId() {
		return senderId;
	}
	
	@Override
	public String toString() {
		return "<PHASE2,r" + round + ",e" + estimate + ", p" + senderId + ">";
	}
}
