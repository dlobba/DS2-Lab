package com.projects.gillo.epidemic;

import java.io.Serializable;

public class PushPullMsg extends EpidemicMsg implements Serializable {
	private Object variable;
	
	public PushPullMsg(long timestamp, Object variable) {
		super(timestamp);
		this.variable = variable;
	}
}
