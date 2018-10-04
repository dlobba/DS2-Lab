package com.projects.gillo.epidemic;

import java.io.Serializable;

public class PushMsg extends EpidemicMsg implements Serializable {
	private Object variable;
	
	public Object getVariable() {
		return variable;
	}

	public PushMsg(long timestamp, Object variable) {
		super(timestamp);
		this.variable = variable;
	}
}
