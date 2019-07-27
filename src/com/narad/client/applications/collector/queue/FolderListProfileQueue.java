package com.narad.client.applications.collector.queue;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileQueue;

public class FolderListProfileQueue implements ProfileQueue {
	private static final Logger logger = LoggerFactory.getLogger(FolderListProfileQueue.class);
	private String dirPath;
	private Queue<Object> idQueue;

	public FolderListProfileQueue(String dirPath) {
		super();
		this.dirPath = dirPath;
		idQueue = new LinkedList<Object>();
		idQueue.addAll(getNextIdsList());
	}

	@Override
	public void addIds(List<Object> friendIds) {
	}

	@Override
	public List<Object> getNextIdsList() {
		// Populate queue from dir
		File file = new File(dirPath);
		if (!file.exists()) {
			logger.info("Cannot iterate over non existant directory: {} ", dirPath);
			throw new RuntimeException("Cannot iterate over non existant directory:  " + dirPath);
		}
		File[] listFiles = file.listFiles((FileFilter)FileFilterUtils.directoryFileFilter());
		ArrayList<Object> idsList = new ArrayList<Object>();
		for (int i = 0; i < listFiles.length; i++) {
			File file2 = listFiles[i];
			String name = file2.getName();
			idsList.add(name);
		}
		
		return idsList;
	}

	@Override
	public Object getNextId() {
		Object poll = idQueue.poll();
		return poll;
	}
}
