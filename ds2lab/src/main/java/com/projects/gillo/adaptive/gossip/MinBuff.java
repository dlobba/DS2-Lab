package com.projects.gillo.adaptive.gossip;

public class MinBuff {
	protected long period;
	protected long bufferSize;
	public MinBuff(long period, long bufferSize) {
		super();
		this.period = period;
		this.bufferSize = bufferSize;
	}
	@Override
	public String toString() {
		return "MinBuff [period=" + period + ", bufferSize=" + bufferSize + "]";
	}
}
