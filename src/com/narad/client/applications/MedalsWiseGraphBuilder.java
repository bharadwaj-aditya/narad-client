package com.narad.client.applications;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.narad.client.NaradJsonClient;
import com.narad.command.NaradCommandConstants;
import com.narad.dataaccess.DataAccessConstants;

public class MedalsWiseGraphBuilder {
	private String fileName;
	private List<PlayerStats> playersList;

	public static void main(String[] args) throws IOException {
		MedalsWiseGraphBuilder medalsWiseGraphBuilder = new MedalsWiseGraphBuilder(
				"D:\\workspaces\\liveInboxWorkspace\\NaradClient\\resources\\medal_names");
		medalsWiseGraphBuilder.buildGraph();
	}

	public MedalsWiseGraphBuilder(String fileName) throws IOException {
		super();
		this.fileName = fileName;
		playersList = new ArrayList<MedalsWiseGraphBuilder.PlayerStats>();
		readFile();
	}

	private void readFile() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String readLine = br.readLine();
		int i = 0;
		while (readLine != null) {
			PlayerStats playerStats = new PlayerStats(i++);
			playerStats.load(readLine);
			playersList.add(playerStats);
			readLine = br.readLine();
			if (i > 20) {
				break;
			}
		}
		br.close();
	}

	public List<Map<String, Object>> getPlayerStats() {
		List<Map<String, Object>> playerStatList = new ArrayList<Map<String, Object>>();
		for (PlayerStats player : playersList) {
			playerStatList.add(player.getMap());
		}
		return playerStatList;
	}

	public void buildGraph() {
		// Building graph - making each player a part of country network and total network.
		// If player has more than 1 medal of g/s/b, he is a part of that network
		// Players friends are
		// 1. Ppl from same country on country network
		// 2. Ppl with same no of gold/silver/bronze/total on respective network

		NaradJsonClient client = new NaradJsonClient();
		long startTime = System.currentTimeMillis();
		System.out.println(startTime);

		// Add nodes
		for (PlayerStats player : playersList) {
			String emailId = player.getEmailId();
			List<Map<String, Object>> profileList = new ArrayList<Map<String, Object>>();

			HashMap<String, Object> countryProfile = new HashMap<String, Object>();
			countryProfile.put(DataAccessConstants.NETWORK, "countryNetwork");
			countryProfile.put("countryName", player.getCountryName());
			countryProfile.put("countryAbbr", player.getCountryAbbr());
			countryProfile.put("playerName", player.getName());
			profileList.add(countryProfile);
			addMedalProfile(profileList, "totalNetwork", player.getTotal(), player);
			addMedalProfile(profileList, "goldNetwork", player.getGold(), player);
			addMedalProfile(profileList, "silverNetwork", player.getSilver(), player);
			addMedalProfile(profileList, "bronzeNetwork", player.getBronze(), player);

			HashMap<String, Object> properties = new HashMap<String, Object>();
			properties.put("emailId", emailId);
			properties.put(NaradCommandConstants.COMMAND_PROFILES, profileList);

			client.addNode(emailId, properties);
			System.out.println("Added for player: " + player.getSerialNo() + " emailId: " + emailId);
		}
		long addNodeCompleteTime = System.currentTimeMillis();
		System.out.println("Time to add nodes: " + (addNodeCompleteTime - startTime));

		int addedRelationsCount = 0;
		// Add relations
		for (PlayerStats player : playersList) {
			// Set<PlayerStats> countryList = new HashSet<PlayerStats>();
			// Set<PlayerStats> goldList = new HashSet<PlayerStats>();
			// Set<PlayerStats> silverList = new HashSet<PlayerStats>();
			// Set<PlayerStats> bronzeList = new HashSet<PlayerStats>();
			// Set<PlayerStats> totalList = new HashSet<PlayerStats>();

			// country search
			for (PlayerStats relatedPlayer : playersList) {
				if (relatedPlayer.equals(player)) {
					// Same player, ignore
					continue;
				}
				if (relatedPlayer.getCountryAbbr().equals(player.getCountryAbbr())) {
					// countryList.add(relatedPlayer);
					Map<String, Object> relationShipMap = new HashMap<String, Object>();
					relationShipMap.put(NaradCommandConstants.COMMAND_NETWORK, "countryNetwork");
					// relationShipMap.put(AddRelationCommand.FROM_PROPERTIES, "countryNetwork");
					// relationShipMap.put(AddRelationCommand.TO_PROPERTIES, "countryNetwork");
					client.addRelationship(player.getEmailId(), relatedPlayer.getEmailId(), relationShipMap);
					addedRelationsCount++;
				}

				if (player.getTotal() > 0 && relatedPlayer.getTotal() == player.getTotal()) {
					// totalList.add(relatedPlayer);
					Map<String, Object> relationShipMap = new HashMap<String, Object>();
					relationShipMap.put(NaradCommandConstants.COMMAND_NETWORK, "totalNetwork");
					client.addRelationship(player.getEmailId(), relatedPlayer.getEmailId(), relationShipMap);
					addedRelationsCount++;
				}

				if (player.getGold() > 0 && relatedPlayer.getGold() == player.getGold()) {
					// goldList.add(relatedPlayer);
					Map<String, Object> relationShipMap = new HashMap<String, Object>();
					relationShipMap.put(NaradCommandConstants.COMMAND_NETWORK, "goldNetwork");
					client.addRelationship(player.getEmailId(), relatedPlayer.getEmailId(), relationShipMap);
					addedRelationsCount++;
				}

				if (player.getSilver() > 0 && relatedPlayer.getSilver() == player.getSilver()) {
					// silverList.add(relatedPlayer);
					Map<String, Object> relationShipMap = new HashMap<String, Object>();
					relationShipMap.put(NaradCommandConstants.COMMAND_NETWORK, "silverNetwork");
					client.addRelationship(player.getEmailId(), relatedPlayer.getEmailId(), relationShipMap);
					addedRelationsCount++;
				}

				if (player.getBronze() > 0 && relatedPlayer.getBronze() == player.getBronze()) {
					// bronzeList.add(relatedPlayer);
					Map<String, Object> relationShipMap = new HashMap<String, Object>();
					relationShipMap.put(NaradCommandConstants.COMMAND_NETWORK, "bronzeNetwork");
					client.addRelationship(player.getEmailId(), relatedPlayer.getEmailId(), relationShipMap);
					addedRelationsCount++;
				}
			}
			System.out.println("Relationships added for player: " + player.getSerialNo() + " with email: "
					+ player.getEmailId()+ " Total added relations: " + addedRelationsCount);
		}
		System.out.println("Done adding relations. No of relations: " + addedRelationsCount + " Time: "
				+ (System.currentTimeMillis() - addNodeCompleteTime));
	}

	// Add medal profile if medal count more than 0
	private void addMedalProfile(List<Map<String, Object>> profileList, String networkName, int count,
			PlayerStats player) {
		if (count == 0) {
			return;
		}
		HashMap<String, Object> medalProfile = new HashMap<String, Object>();
		medalProfile.put(DataAccessConstants.NETWORK, networkName);
		medalProfile.put("countryName", player.getCountryName());
		medalProfile.put("playerName", player.getName());
		medalProfile.put("count", count);
		profileList.add(medalProfile);
	}

	private class PlayerStats {
		private int serialNo;
		private String countryName;
		private String countryAbbr;
		private String name;
		private String sport;
		private int gold;
		private int silver;
		private int bronze;
		private int total;
		private String email;

		public PlayerStats(int index) {
			super();
			this.serialNo = index;
		}

		public void load(String statString) {
			List<String> tokenList = new ArrayList<String>();
			StringTokenizer tokenizer = new StringTokenizer(statString, "\t");
			while (tokenizer.hasMoreTokens()) {
				tokenList.add(tokenizer.nextToken());
			}
			int i = 0;
			countryName = tokenList.get(i++);
			countryAbbr = tokenList.get(i++);
			name = tokenList.get(i++);
			sport = tokenList.get(i++);
			gold = Integer.parseInt(tokenList.get(i++));
			silver = Integer.parseInt(tokenList.get(i++));
			bronze = Integer.parseInt(tokenList.get(i++));
			total = Integer.parseInt(tokenList.get(i++));

			String[] nameParts = name.split(" ");
			StringBuilder br = new StringBuilder();
			for (int j = 0; j < nameParts.length; j++) {
				String namePart = nameParts[j];
				br.append(namePart);
			}
			br.append(serialNo).append("@").append(countryAbbr).append(".org");
			email = br.toString().toLowerCase();
		}

		public Map<String, Object> getMap() {
			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			hashMap.put("serialNo", serialNo);
			hashMap.put("countryName", countryName);
			hashMap.put("countryAbbr", countryAbbr);
			hashMap.put("name", name);
			hashMap.put("gold", gold);
			hashMap.put("silver", silver);
			hashMap.put("bronze", bronze);
			hashMap.put("total", total);
			hashMap.put("email", email);
			return hashMap;
		}

		public int getSerialNo() {
			return serialNo;
		}

		public String getCountryName() {
			return countryName;
		}

		public String getCountryAbbr() {
			return countryAbbr;
		}

		public String getName() {
			return name;
		}

		public String getSport() {
			return sport;
		}

		public int getGold() {
			return gold;
		}

		public int getSilver() {
			return silver;
		}

		public int getBronze() {
			return bronze;
		}

		public int getTotal() {
			return total;
		}

		public String getEmailId() {
			return email;
		}
	}
}
