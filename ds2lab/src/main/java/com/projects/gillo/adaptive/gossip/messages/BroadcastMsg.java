package com.projects.gillo.adaptive.gossip.messages;

import java.io.Serializable;

public class InfectedMessage implements Serializable {
	
	private Object variable;
	
	public InfectedMessage(Object variable) {
		this.variable = variable;
	}

	public Object getVariable() {
		return variable;
	}
	
	

}
