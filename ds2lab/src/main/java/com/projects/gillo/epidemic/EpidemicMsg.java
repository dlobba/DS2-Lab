package com.projects.gillo.epidemic;

import java.io.Serializable;

public abstract class EpidemicMsg implements Serializable {
	
	private long timestamp;
	
	public EpidemicMsg(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	
	
}
