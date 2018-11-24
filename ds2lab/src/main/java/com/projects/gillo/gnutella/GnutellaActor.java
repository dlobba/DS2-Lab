package com.projects.gillo.gnutella;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class GnutellaActor extends AbstractActor {
	
	public static class LogMessage implements Serializable {};
	public static class StartPingMsg implements Serializable {};
	
    public static class GnutellaPingMessage implements Serializable {
		protected int sourceId;
		protected ActorRef sourceActor;
		protected int ttl;
		protected int lastHopId;

		public GnutellaPingMessage(int sourceId, ActorRef sourceActor, int lastHopId, int ttl) {
			super();
			this.sourceId = sourceId;
			this.sourceActor = sourceActor;
			this.ttl = ttl;
			this.lastHopId = lastHopId;
		}
		public int getSourceId() {
			return sourceId;
		}
		public ActorRef getSourceActor() {
			return sourceActor;
		}
		public int getTtl() {
			return this.ttl;
		}
		public int getLastHopId() {
			return this.lastHopId;
		}
    }
    
    public static class GnutellaPongMessage implements Serializable {
    	protected int sourceId;
		protected ActorRef sourceActor;
		
		protected int destId;
		protected ActorRef destActor;
		protected int lastHopId;
		protected int ttl;
		
		public GnutellaPongMessage(int sourceId,
				ActorRef sourceActor,
				int destId,
				ActorRef destActor, 
				int lastHopId,
				int ttl) {
			super();
			this.sourceId = sourceId;
			this.sourceActor = sourceActor;
			this.destId = destId;
			this.destActor = destActor;
			this.lastHopId = lastHopId;
			this.ttl = ttl;
		}
		public int getSourceId() {
			return sourceId;
		}
		public ActorRef getSourceActor() {
			return sourceActor;
		}
		public int getDestId() {
			return destId;
		}
		public ActorRef getDestActor() {
			return destActor;
		}
		public int getTtl() {
			return this.ttl;
		}
		public int getLastHopId() {
			return this.lastHopId;
		}
    }
    
    /* --------------------- attributes -------*/
    
    protected Map<Integer, ActorRef> peerMap;
    protected Map<Integer, RoutingEntry> routingTable;
    
    protected int id;
    public static final int ITTL = 3;
    public static final long MAX_AGE = 10000;

    public GnutellaActor(int id, Map<Integer, ActorRef> peers) {
    	this.id = id;
    	this.peerMap = new HashMap<>();
    	this.peerMap.putAll(peers);
    	this.routingTable = new HashMap<Integer, RoutingEntry>();
	}

    public GnutellaActor(int id) {
    	this(id, new HashMap<>());
	}

    public static Props props(int id) {
    	return Props.create(GnutellaActor.class,
				() -> new GnutellaActor(id));
	}

    protected void insertPeer(int pID, ActorRef peer) {
    	if (!peerMap.containsKey(pID)) {
    		peerMap.put(pID, peer);
    	}
    }
    
    private void onStartMessage(StartMessage msg) {
    	insertPeer(msg.getID(), msg.getInitPeer());
    	updateRoutingTable(msg.getID(), msg.getID(), 1);
    }
    
    private Set<ActorRef> getOneHop() {
    	Set<ActorRef> peers = new HashSet<>();
    	for (RoutingEntry entry : this.routingTable.values()) {
			if (entry.getHop() == 1)
				peers.add(this.peerMap.get(entry.getId()));
		}
    	return peers;
    }
    
    private void updateRoutingTable(int sourceId, int senderId, int ttl) {
    	if (!this.routingTable.containsKey(sourceId)) {
    		this.routingTable.put(sourceId, new RoutingEntry(senderId, ttl));
    	} else {
    		RoutingEntry entry = this.routingTable.get(sourceId);
    		int oldTtl = entry.getHop();
    		if (ttl < oldTtl || System.currentTimeMillis() - entry.getAge() > MAX_AGE) {
    			this.routingTable.put(sourceId, new RoutingEntry(senderId, ttl));
    		}
    	}
    }
    
    private void onPingMsg(GnutellaPingMessage msg) {   
    	this.insertPeer(msg.getLastHopId(), this.getSender());
    	this.insertPeer(msg.getSourceId(), msg.getSourceActor());
    	this.updateRoutingTable(msg.getSourceId(), msg.getLastHopId(), msg.getTtl());
    	if (msg.getTtl() > ITTL) {
    		//System.out.printf("TTL threshold reached: %d. Ping stopped.\n", msg.getTtl());
    		return;
    	}
    	GnutellaPingMessage ping = new GnutellaPingMessage(msg.sourceId,
    			msg.getSourceActor(),
    			this.id,
    			msg.getTtl() + 1);
    	Set<ActorRef> neighbours = this.getOneHop();
    	neighbours.remove(this.getSender());
    	for (ActorRef neighbour : neighbours) {
			neighbour.tell(ping, this.getSelf());
		}
    	// reply with pong
		GnutellaPongMessage pong = new GnutellaPongMessage(this.id,
				this.getSelf(),
				msg.getSourceId(),
				msg.getSourceActor(),
				msg.getLastHopId(),
				1);
		ActorRef nextDestRef = this.peerMap.get(msg.getLastHopId());
		nextDestRef.tell(pong,  this.getSelf());
		/*
		System.out.println(this
				.printRoutingTable(String.format("Peer %d, ping: %d",
						this.id,
						msg.sourceId)));
		*/
    }
    
    private void onPongMsg(GnutellaPongMessage msg) {
    	// update entry
    	this.insertPeer(msg.getLastHopId(), this.getSender());
    	this.insertPeer(msg.getSourceId(), msg.getSourceActor());
    	this.updateRoutingTable(msg.getSourceId(), msg.getLastHopId(), msg.getTtl());
    	if (msg.getDestId() != this.id) {
    		GnutellaPongMessage pong = new GnutellaPongMessage(msg.getSourceId(),
    				msg.getSourceActor(),
    				msg.getDestId(),
    				msg.getDestActor(),
    				this.id,
    				msg.getTtl() + 1);
    		int nextDestId = this.routingTable.get(msg.getDestId()).getId();
    		ActorRef nextDestRef = this.peerMap.get(nextDestId);
    		nextDestRef.tell(pong,  this.getSelf());
    	} else {
    		System.out.println(this
    				.printRoutingTable(String.format("Peer %d, pong: %d",
    						this.id,
    						msg.destId)));
    	}
    }
    
    private String printRoutingTable() {
    	return this.printRoutingTable("Peer: " + this.id);
    }
    
    
    private String printRoutingTable(String title) {
    	StringBuilder strBuilder = new StringBuilder();
    	RoutingEntry entry;
    	strBuilder.append(title + "\n");
    	strBuilder.append("destid,nextpeerid,hops,age\n");
    	for (Integer id : this.routingTable.keySet()) {
			entry = this.routingTable.get(id);
			strBuilder.append(id + "," + entry.getId() + "," + entry.getHop() + "," + entry.getAge() + "\n");
		}
    	return strBuilder.toString();
    }
    
    private void onStartPingMsg(StartPingMsg msg) {
    	System.out.printf("Peer %s starts pinging\n", this.id);
    	this.getSelf().tell(new GnutellaPingMessage(this.id, this.getSelf(), this.id, 0),
    			this.getSelf());
    }

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartMessage.class, this::onStartMessage)
				.match(GnutellaPingMessage.class, this::onPingMsg)
				.match(GnutellaPongMessage.class, this::onPongMsg)
				.match(StartPingMsg.class, this::onStartPingMsg)
				.build();
	}
}
