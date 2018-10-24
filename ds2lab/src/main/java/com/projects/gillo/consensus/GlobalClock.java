package com.projects.gillo.consensus;

public class GlobalClock {

	private static GlobalClock instance = null;
	private long startTime;
	private final long delta = 100;
	
	private GlobalClock() {
		this.startTime = System.currentTimeMillis();
	}
	
	public static GlobalClock getInstance() {
		if (instance == null) {
			instance = new GlobalClock();
		}
		return instance;
	}
	
	public long getCurrentTick() {
		return (long) (System.currentTimeMillis() - startTime) / delta;
	}
}
