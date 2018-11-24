package com.projects.gillo.gnutella;

public class RoutingEntry {
	protected int id;
	protected int hop;
	protected long age;
	
	public RoutingEntry(int id, int hop) {
		super();
		this.id = id;
		this.hop = hop;
		this.age = System.currentTimeMillis();
	}
	public int getId() {
		return id;
	}
	public int getHop() {
		return hop;
	}
	public long getAge() {
		return this.age;
	}
}
