package com.projects.gillo.epidemic.messages;

import java.io.Serializable;

import com.projects.gillo.epidemic.EpidemicMsg;

public class PushMsg extends EpidemicMsg implements Serializable {
	private Object variable;
	
	public Object getVariable() {
		return variable;
	}

	public PushMsg(long timestamp, int sid, Object variable) {
		super(timestamp, sid);
		this.variable = variable;
	}
}
