package com.narad.client.applications.collector.api;

import java.util.List;

public interface ProfileQueue {
	
	public void addIds(List<Object> friendIds);
	
	public List<Object> getNextIdsList();
	
	public Object getNextId();

}
