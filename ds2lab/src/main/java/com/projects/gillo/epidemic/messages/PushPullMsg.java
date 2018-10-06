package com.projects.gillo.epidemic.messages;

import java.io.Serializable;

import com.projects.gillo.epidemic.EpidemicMsg;

public class PushPullMsg extends EpidemicMsg implements Serializable {
	private Object variable;
	
	public PushPullMsg(long timestamp, int sid, Object variable) {
		super(timestamp, sid);
		this.variable = variable;
	}

	public Object getVariable() {
		return variable;
	}
}
