package com.projects.gillo.epidemic.messages;

import java.io.Serializable;

public class ReplyMsg implements Serializable{
	private Object variable;
	private long timestamp;
	public int sid;
	
	public ReplyMsg(long timestamp, int sid, Object variable) {
		this.timestamp = timestamp;
		this.variable = variable;
		this.sid = sid;
	}

	public Object getVariable() {
		return variable;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
