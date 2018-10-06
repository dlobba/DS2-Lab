package com.projects.gillo.epidemic;

import java.io.Serializable;

public abstract class EpidemicMsg implements Serializable {
	
	private long timestamp;
	protected int sid;
	
	public EpidemicMsg(long timestamp, int sid) {
		this.timestamp = timestamp;
		this.sid = sid;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	
	
}
