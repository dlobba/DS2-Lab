package com.projects.gillo.consensus;

import java.io.Serializable;

public class DecideMsg implements Serializable {

	private Object estimate;
	private int senderId;
	public DecideMsg(Object estimate, int senderId) {
		super();
		this.estimate = estimate;
		this.senderId = senderId;
	}
	public Object getEstimate() {
		return estimate;
	}
	public int getSenderId() {
		return senderId;
	}
	
}
