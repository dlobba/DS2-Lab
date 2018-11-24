package com.projects.gillo.gnutella;

import java.io.Serializable;

import akka.actor.ActorRef;

public class StartMessage implements Serializable {
	protected final ActorRef initPeer;
	protected final int id;
	
    public StartMessage(ActorRef ip, int id) {
        this.initPeer = ip;
        this.id = id;
    }
    
    public ActorRef getInitPeer() {
    	return initPeer;
    }
    
    public int getID() {
    	return id;
    }
}
