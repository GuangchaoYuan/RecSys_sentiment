package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

public class ANEW {

	private static ANEW instance = null;
	private static final String FILENAME = "D:\\Social_Media_Analytics\\Anew\\ANEW\\anew_healey.csv";
	
	Map<String, Entry> map = null;
	
	private ANEW() {
		load();
	}
	
	public static ANEW getInstance() {
		if (instance == null)
			instance = new ANEW();
		return instance;
	}
	
	private synchronized void load() {
		map = new Hashtable<String, Entry>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(FILENAME));
			String line = "";
			in.readLine(); // skip first line
			while ((line = in.readLine()) != null) {
				StringTokenizer tokens = new StringTokenizer(line, ",");
				Entry entry = new Entry();
				entry.word = tokens.nextToken();
				entry.descripting = tokens.nextToken();
				entry.wordNo = Integer.parseInt(tokens.nextToken());
				entry.valenceMean = Double.parseDouble(tokens.nextToken());
				entry.valenceSD = Double.parseDouble(tokens.nextToken());
				entry.arousalMean = Double.parseDouble(tokens.nextToken());
				entry.arousalSD = Double.parseDouble(tokens.nextToken());
				entry.dominanceMean = Double.parseDouble(tokens.nextToken());
				entry.dominanceSD = Double.parseDouble(tokens.nextToken());
				entry.wordFreq = Integer.parseInt(tokens.nextToken());
				//map.put(entry.word, entry);
				map.put(entry.descripting, entry);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public Entry get(String word) {
		return map.get(word);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ANEW anew = ANEW.getInstance();
		System.out.println(anew.get("future"));
//		System.out.println(anew.get("memory"));
	}

	class Entry {
		String word;
		String descripting;
		int wordNo;
		double valenceMean;
		double valenceSD;
		double arousalMean;
		double arousalSD;
		double dominanceMean;
		double dominanceSD;
		int wordFreq;
		
		public int hashCode() {
			return word.hashCode();
		}
		
		public String toString() {
			return word + ", " + descripting + ", " + wordNo + ", " 
				+ valenceMean + ", " + valenceSD + ", " + arousalMean + ", "
				+ arousalSD + ", " + dominanceMean + ", " + dominanceSD + ", "
				+ wordFreq;
			
		}
	}
}

