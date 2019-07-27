package com.narad.client.applications.collector.queue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileQueue;
import com.narad.client.util.FileUtil;

public class FileListProfileQueue implements ProfileQueue {
	private static final long FILE_LIST_TIME_SLOT = 60 * 60 * 1000l;
	private static final Logger logger = LoggerFactory.getLogger(FileListProfileQueue.class);
	private Date dateFrom;
	private Date dateTo;
	private String dirPath;
	private Queue<Object> idQueue;

	public FileListProfileQueue(String dirPath) {
		super();
		this.dirPath = dirPath;
		idQueue = new LinkedList<Object>();
		dateFrom = new Date(System.currentTimeMillis() - 7 * 24 * FILE_LIST_TIME_SLOT/*1 week*/);
		dateTo = dateFrom;
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
		String[] fileNames = null;
		do {
			dateFrom = dateTo;
			dateTo = new Date(dateTo.getTime() + FILE_LIST_TIME_SLOT);
			fileNames = FileUtil.getFileList(dirPath, dateFrom, dateTo, new NoFriendsFileFilter());
		} while (extracted(fileNames));
		List<Object> idsList = new ArrayList<Object>(fileNames.length);
		for (int i = 0; i < fileNames.length; i++) {
			Object idFromFileName = getIdFromFileName(fileNames[i]);
			idsList.add(idFromFileName);
		}
		return idsList;
	}

	private boolean extracted(String[] fileNames) {
		boolean b = fileNames == null || fileNames.length == 0;
		boolean c = dateFrom.getTime() < System.currentTimeMillis();
		return b && c;
	}
	
	@Override
	public Object getNextId() {
		Object poll = idQueue.poll();
		while (poll == null) {
			idQueue.addAll(getNextIdsList());
			poll = idQueue.poll();
			if (poll == null) {
				try {
					Thread.sleep(10 * 60 * 1000);
				} catch (InterruptedException e) {
					logger.error("Error while sleeping thread", e);
				}
			}
		}
		return poll;
	}

	private Object getIdFromFileName(String fileName) {
		String idStr = fileName.substring(0, fileName.length() - 5/* .json length */);
		try {
			long parseLong = Long.parseLong(idStr);
			return parseLong;
		} catch (NumberFormatException ne) {
			return idStr;
		}
	}

	private class NoFriendsFileFilter implements IOFileFilter {

		@Override
		public boolean accept(File paramFile) {
			String fileName = paramFile.getName();
			return accept(null, fileName);
		}

		@Override
		public boolean accept(File paramFile, String paramString) {
//			if (!paramString.endsWith(".json")) {
//				return false;
//			} else if (paramString.endsWith("_friends.json")) {
//				return false;
//			} else if (paramString.contains("_old")) {
//				return false;
//			}
			return true;
		}

	}



}
