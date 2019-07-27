package com.narad.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

	public static File createFile(String filePath, boolean isDir, boolean createParent) {
		File file = new File(filePath);
		return createFile(file, isDir, createParent);
	}

	public static File createFile(File file, boolean isDir, boolean createParent) {
		if (!file.exists()) {
			File parentFile = file.getParentFile();
			if (parentFile != null && parentFile.exists()) {
				try {
					if (isDir) {
						logger.info("Creating directory.", file.getAbsolutePath());
						file.mkdir();
					} else {
						file.createNewFile();
					}
					return file;
				} catch (IOException e) {
					logger.error("Could not create file.", e);
					e.printStackTrace();
					return null;
				}
			} else if (createParent) {
				logger.info("Creating parent for file: {}", file.getAbsolutePath());
				if (parentFile.mkdirs()) {
					// parentFile = createFile(parentFile.getAbsolutePath(), true, true);
					// if (parentFile != null) {
					return createFile(file, isDir, createParent);
				}
			}
		}
		return null;
	}

	/**
	 * Create a new file at the location. Creates parents if not already present.
	 * 
	 * @param filePath
	 * @param isDir
	 * @return
	 */
	public static File createNewFile(String filePath, boolean isDir) {
		return createNewFile(filePath, isDir, false);
	}
	
	public static File createNewFile(String filePath, boolean isDir, boolean preserveExtension) {
		File file = new File(filePath);
		if (file.exists()) {
			String absolutePath = file.getAbsolutePath();
			String absoluteNameWithoutExt = absolutePath;
			String ext = null;
			if (preserveExtension) {
				int lastIndexOfDot = absolutePath.lastIndexOf(".");
				if (lastIndexOfDot > -1) {
					absoluteNameWithoutExt = absolutePath.substring(0, lastIndexOfDot);
					ext = absolutePath.substring(lastIndexOfDot);
				}
			}
			String renamedFilePath = absoluteNameWithoutExt + "_old";
			File renameToFile = new File(renamedFilePath + (ext == null ? "" : ext));
			int i = 0;
			while (renameToFile.exists()) {// To ensure renamed filename does not exist
				renameToFile = new File(renamedFilePath + (ext == null ? "" : ext) + i++);
			}
			file.renameTo(renameToFile);
		}
		File newFile = createFile(filePath, isDir, true);
		return newFile;
	}

	public static boolean writeToFile(File file, String data) {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file);
			fileWriter.write(data);
			return true;
		} catch (IOException e) {
			logger.error("Error while writing to file: {}", e, file.getAbsolutePath());
		} finally {
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					logger.info("Error while closing file after writing. Path: {}, Exception: {}",
							file.getAbsolutePath(), e.getMessage());
				}
			}
		}
		return false;
	}

	public static String readFromFile(String filePath) {

		BufferedReader br = null;
		FileReader fr = null;
		// char[] readChars = new char[4096];
		try {
			fr = new FileReader(filePath);
			br = new BufferedReader(fr);
			StringBuilder sbr = new StringBuilder();
			String readLine = br.readLine();
			/*
			 * int read = fr.read(readChars); while (read != -1) { sbr.append(readChars); read = fr.read(readChars); }
			 */
			while (readLine != null) {
				sbr.append(readLine).append("\n");
				readLine = br.readLine();
			}
			return sbr.toString();
		} catch (IOException e) {
			logger.error("Error while writing to file: {}", e, filePath);
		} finally {
			if (fr != null) {
				try {
					br.close();
					// fr.close();
				} catch (IOException e) {
					logger.info("Error while closing file after reading. Path: {} Exception: {}", filePath,
							e.getMessage());
				}
			}
		}
		return null;
	}
	
	public static JSONObject readJsonFromFile(String filePath) {
		FileReader fileReader;
		try {
			fileReader = new FileReader(filePath);
			JSONObject jsonObj = (JSONObject) new JSONParser().parse(fileReader);
			return jsonObj;
		} catch (FileNotFoundException e) {
			logger.info("Exception while reading json object. ", e);
		} catch (IOException e) {
			logger.info("Exception while reading json object. ", e);
		} catch (ParseException e) {
			logger.info("Exception while reading json object. ", e);
		}
		return null;
	}

	public static String[] getFileList(String dirPath, Date dateModifiedFrom, Date dateModifiedTo, IOFileFilter filter) {
		File file = new File(dirPath);
		if (!file.exists()) {
			return new String[0];
		} else if (dateModifiedFrom == null && dateModifiedTo == null) {
			return new String[0];
		}
		boolean doFilter = false;
		AndFileFilter allFileFilters = new AndFileFilter();
		if (dateModifiedFrom != null) {
			doFilter = true;
			allFileFilters.addFileFilter(new AgeFileFilter(dateModifiedFrom, false));
		}
		if (dateModifiedTo != null) {
			doFilter = true;
			allFileFilters.addFileFilter(new AgeFileFilter(dateModifiedTo, true));
		}
		if (allFileFilters != null) {
			doFilter = true;
			allFileFilters.addFileFilter(filter);
		}

		if (doFilter) {
			return file.list(allFileFilters);
		} else {
			return file.list();
		}
	}

	public static void main(String[] args) {
		// createFile("D:/a/b/c/d/e/f.txt", false, true);
//		String[] fileList = getFileList("D:\\development\\narad\\response_dump\\twitter",
//				new Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000), new Date(System.currentTimeMillis() - 2
//						* 24 * 60 * 60 * 1000), null);
		String[] fileList = getFileList("D:\\development\\narad\\response_dump\\twitter",
				new Date(System.currentTimeMillis() - 4 * 60 * 60 * 1000), new Date(), new IOFileFilter() {
					
					@Override
					public boolean accept(File paramFile, String paramString) {
						return true;
					}
					
					@Override
					public boolean accept(File paramFile) {
						return true;
					}
				});
		
		for (int i = 0; i < fileList.length; i++) {
			System.out.println(fileList[i]);
		}
	}
}
