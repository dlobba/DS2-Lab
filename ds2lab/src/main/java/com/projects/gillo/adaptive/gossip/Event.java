package com.projects.gillo.adaptive.gossip;

import java.io.Serializable;

public class Event implements Serializable {
	
	protected int senderId;
	private long age;
	private int id;
	private long bufferSize;
	private String message;
	
	public Event(int id, long bufferSize, String message, int senderId) {
		this.id = id;
		this.age = 0;
		this.bufferSize = bufferSize;
		this.message = new String(message);
		this.senderId = senderId;
	}

	public long getBufferSize() {
		return bufferSize;
	}

	public int getSenderId() {
		return senderId;
	}

	public long getAge() {
		return age;
	}
	
	public void setAge(long newAge) {
		this.age = newAge;
	}
	
	public String getMessage() {
		return message;
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + senderId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		if (id != other.id)
			return false;
		if (senderId != other.senderId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Event [senderId=" + senderId + ", age=" + age + ", id=" + id + ", message=" + message + "]";
	}
}
