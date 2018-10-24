package com.projects.gillo.consensus;

import java.io.Serializable;

public class ProposeMsg implements Serializable {

	private Object proposal;
	
	public ProposeMsg(Object proposal) {
		this.proposal = proposal;
	}

	public Object getProposal() {
		return proposal;
	}
}
