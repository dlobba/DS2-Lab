package com.projects.gillo.adaptive.gossip;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MinBuffList {
	
	private List<MinBuff> minBuffs;
	private final long delta;
	
	public MinBuffList(long delta) {
		if (delta <= 0)
			throw new IllegalArgumentException();
		this.delta = delta;
		this.minBuffs = new LinkedList<>();
	}

	public void add(MinBuff minBuff) {
		int index = 0;
		boolean found = false;
		MinBuff mbTemp = null;
		while (index < this.minBuffs.size() && !found) {
			mbTemp = this.minBuffs.get(index);
			if (mbTemp.period >= minBuff.period)
				found = true;
			index += 1;
		}
		if (!found) {
			this.minBuffs.add(minBuff);
		} else {
			if (mbTemp.period == minBuff.period) {
				mbTemp.bufferSize = mbTemp.bufferSize < minBuff.bufferSize ?
						mbTemp.bufferSize : minBuff.bufferSize;
			}
			this.minBuffs.add(index - 1, minBuff);			
		}
		if (this.minBuffs.size() > this.delta)
			this.minBuffs.remove(0);
	}

	/**
	 * Return the index of the MinBuff having
	 * the minimum bufferSize
	 * @return
	 */
	public int getMinBufferSize() {
		Iterator<MinBuff> mbIter = this.minBuffs.iterator();
		MinBuff mbTemp = mbIter.next();
		long min;
		if (mbTemp == null)
			return -1;
		int minIndex = 0;
		int index = 0;
		min = mbTemp.period;
		while (mbIter.hasNext()) {
			if (mbTemp.period < min) {
				min = mbTemp.period;
				minIndex = index;
			}
			index += 1;
			mbTemp = mbIter.next();
		}
		return minIndex;
	}
	
	public MinBuff getMinBuff(long period) {
		Iterator<MinBuff> mbIter = this.minBuffs.iterator();
		MinBuff mbTemp;
		while (mbIter.hasNext()) {
			mbTemp = mbIter.next();
			if (mbTemp.period == period)
				return mbTemp;
		}
		return null;
	}

	@Override
	public String toString() {
		return "MinBuffList = " + minBuffs;
	}
}
