package com.xerox.socialmedia.communityrecommendation.sentiment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.xerox.socialmedia.communityrecommendation.utils.Constant;

public class Graph {
	HashMap<Integer, ArrayList<Integer>> adj;
	
	public Graph(){
		adj = new HashMap<Integer, ArrayList<Integer>>();
	}
	
	//create the graph for a given input file
	public void createGraph(String inputFile){
		BufferedReader reader = null;
		String line = "";
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line=reader.readLine())!=null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 4){
					System.out.println("There is an error in reading file");
					continue;
				}
				int v1 = Integer.parseInt(words[0]);
				int v2 = Integer.parseInt(words[1]);
				
				//add neighbors
				addNeighbor(v1, v2);
				addNeighbor(v2, v1);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//add an edge into a graph
	private void addNeighbor(int v1, int v2){
		if(adj.containsKey(v1))
			adj.get(v1).add(v2);
		else{
			ArrayList<Integer> list = new ArrayList<Integer>();
			list.add(v2);
			adj.put(v1, list);
		}
	}
	
	//get the degree of a node (undirected graph)
	public int getDegree(int v){
		int degree = adj.get(v).size();
		return degree;
	}
	
	//get the number of nodes in the graph
	public int size(){
		int size = adj.size();
		return size;
	}
	
	public Set<Integer> keySet(){
		return adj.keySet();
	}
	
	public ArrayList<Integer> getNeighbors(int v){
		ArrayList<Integer> list = new ArrayList<Integer>();
		if(adj.containsKey(v))
			list = adj.get(v);
		return list;
	}
	
	//test whether two nodes are connected
	public boolean connected(int v1, int v2){
		boolean result = false;
		ArrayList<Integer> list = new ArrayList<Integer>();
		list = this.getNeighbors(v1);
		for(int i = 0; i<list.size(); i++){
			if(list.get(i)==v2){
				result = true;
				continue;
			}
		}
		return result;
	}
	
	//get the set of 2-hop nodes for a given source node
	public boolean getTwoHopCandidates(int source, BufferedWriter writer, int threshold){
		//System.out.println("Search for the candidates for source node: " + source);
		boolean write = false;
		HashMap<Integer, Integer> candidate = new HashMap<Integer, Integer>();
		ArrayList<Integer> list = new ArrayList<Integer>();
		ArrayList<Integer> sub = new ArrayList<Integer>();
		HashSet<Integer> subOverall = new HashSet<Integer>();
		int temp = 0;
		int posSize = 0;// size of the positive instances
		int subSize = 0;
		int second = 0;
		//one-hop
		list = adj.get(source);
		posSize = list.size();
		for(int k = 0; k < list.size(); k++){
			//prune the pairs whose number of mutual friends is less than the threshold
			//if(this.numMutualFriends(source, list.get(k))<threshold)
				//continue;
			candidate.put(list.get(k), 1);
		}
		
		for(int i = 0; i < list.size(); i++){
			temp = list.get(i);
			//second-hop
			sub = adj.get(temp);
			for(int j = 0; j < sub.size(); j++){
				second = sub.get(j);
				if(this.numMutualFriends(source, second)<threshold)
					continue;
				if(!subOverall.contains(second) && (second!=source)){
					//candidate.put(second, 0);
					subOverall.add(second);
				}
			}
			
		}
		
		//randomly choosing the negative instances that are equal to the number of positive instances
		ArrayList<Integer> subList = new ArrayList<Integer>(subOverall);
		subSize = subList.size();
		
		/*if(posSize < subSize){
			for(int m = 0; m < subSize;m++){
				second = subList.get(m);
				if((!candidate.containsKey(second))){
					candidate.put(second, 0);	
					if(m == (posSize-1))
						break;
				}
				
			}
		}
		else{*/
			for(int m = 0; m < subSize; m++){
				second = subList.get(m);
				if((!candidate.containsKey(second))){
					candidate.put(second, 0);
				}
			}
		//}
			int negSize = candidate.size() - posSize;
			
	
		//System.out.println("Finish searching the candidates for source node: " + source);
		//print the map
			if(negSize>=2){
				this.printMap(source, candidate, writer);
				write = true;
			}
		return write;
	}
	
	//get the number of mutual friends
	private int numMutualFriends(int source, int target){
		int num = 0;
		ArrayList<Integer> sList = new ArrayList<Integer>();
		ArrayList<Integer> tList = new ArrayList<Integer>();
		
		sList = adj.get(source);
		tList = adj.get(target);
		
		num = this.getIntersectionNeighbors(new HashSet<Integer>(sList), new HashSet<Integer>(tList));
		return num;
	}
	
	//get the size of the intersection
	private int getIntersectionNeighbors(Set<Integer> s1, Set<Integer> s2){
		int size = 0;
		HashSet<Integer> clone = new HashSet<Integer>();
		if(s1.size()>0 && s2.size()>0){
			if(s1.size()>=s2.size()){
				clone = new HashSet<Integer>(s1);
				clone.retainAll(s2);
			}
			else{
				clone = new HashSet<Integer>(s2);
				clone.retainAll(s1);
			}
		}
		size = clone.size();
		return size;
	}
		
	//print the 2-hop nodes as well as their source node
	private void printMap(int source, HashMap<Integer, Integer> map, BufferedWriter writer){
		int key = 0;
		int value = 0;
		int num = 0;
		try {
			Iterator it = map.entrySet().iterator();
			while(it.hasNext()){
				num++;
				Map.Entry<Integer, Integer> pairs = (Map.Entry<Integer, Integer>)it.next();
				key = pairs.getKey();
				value = pairs.getValue();
				writer.append(String.valueOf(source) + Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(key) + Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(value));
				writer.append("\n");
				writer.flush();
			}
			System.out.println(num + " candidates for source node: " + source);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
