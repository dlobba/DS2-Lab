package com.projects.gillo.rb;

import java.io.Serializable;

public class Message implements Serializable {

	private String content;
	private int id;
	private int senderId;
	
	public Message(String content, int id, int senderId) {
		super();
		this.content = content;
		this.id = id;
		this.senderId = senderId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
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
		Message other = (Message) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (id != other.id)
			return false;
		if (senderId != other.senderId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "p" + senderId + "m" + id + "content:" + content;
	}
	
	
	
}
