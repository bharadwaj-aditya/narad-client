package com.narad.client.applications.collector.queue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileQueue;

public class BasicProfileQueue implements ProfileQueue {
	private static final Logger logger = LoggerFactory.getLogger(BasicProfileQueue.class);
	private Queue<Object> idQueue;

	public BasicProfileQueue(List<Object> ids) {
		super();
		idQueue = new LinkedList<Object>();
		if (ids != null) {
			idQueue.addAll(ids);
		}
	}

	@Override
	public void addIds(List<Object> friendIds) {
		idQueue.addAll(friendIds);
	}

	@Override
	public List<Object> getNextIdsList() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getNextId() {
		return idQueue.poll();
	}
}
