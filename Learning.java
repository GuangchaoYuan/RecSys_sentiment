package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.Arrays;

import com.xerox.socialmedia.communityrecommendation.utils.Constant;

public class Learning {

	public void getPerformance(){
		String trainFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\normalize_graph_feature_3_test.txt";
		String testFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\pred.txt";
		
		int[] metric = new int[4];//performance metric: 0 means TP, 1 means FN, 2 means FP, 3 means TN
		BufferedReader reader1 = null;
		BufferedReader reader2 = null;
		String line1= "";
		String line2= "";
		int value = 0;
		int predict = 0;
		try {
			Arrays.fill(metric, 0);//initialize the array
			reader1 = new BufferedReader(new FileReader(trainFile));
			reader2 = new BufferedReader(new FileReader(testFile));
			while((line1 = reader1.readLine())!=null && (line2 = reader2.readLine())!=null){
				String[] words = line1.split(Constant.SEPARATOR_SPACE);
				if(words[0].equals("#edge"))
					break;
				
				value = Integer.parseInt(words[0].substring(1));//true value
				predict = Integer.parseInt(line2);//predict
				if(value==1){
					if(predict==1)
						metric[0]++; //TP
					else if(predict==0)
						metric[1]++; //FN
				}
				else if(value==0){
					if(predict==1)
						metric[2]++; //FP
					else if(predict==0)
						metric[3]++; //TN
				}
				
			}
			reader1.close();
			reader2.close();
			
			System.out.println("TP: " + metric[0] + " FN: " + metric[1]+ " FP: " + metric[2]+" TN: " + metric[3]);
			double recall =  (double) metric[0]/(double)(metric[0]+metric[1]);
			double precision = (double) metric[0]/(double)(metric[0]+metric[2]);
			System.out.println("Precision: " + precision + " recall: " + recall);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
	}
	
	
	public void readFileStatistics(){
		String trainFile = "D:\\Social_Media_Analytics\\MRF\\OpenCRF\\OpenCRF\\example.txt";
		
		int[] metric = new int[4];//performance metric: 0 means TP, 1 means FN, 2 means FP, 3 means TN
		BufferedReader reader1 = null;
		String line1= "";
		int value = 0;
		String symbol = "";
		try {
			Arrays.fill(metric, 0);//initialize the array
			reader1 = new BufferedReader(new FileReader(trainFile));
			while((line1 = reader1.readLine())!=null){
				String[] words = line1.split(Constant.SEPARATOR_SPACE);
				if(words[0].equals("#edge"))
					break;
				
				symbol = words[0].substring(0, 1);
				value = Integer.parseInt(words[0].substring(1));//true value
				if(symbol.equals("+")){
					if(value==1)
						metric[0]++; //+1
					else if(value==0)
						metric[1]++; //+0
				}
				else if(symbol.equals("?")){
					if(value==1)
						metric[2]++; //?1
					else if(value==0)
						metric[3]++; //?0
				}
				
			}
			reader1.close();
			
			System.out.println("+1: " + metric[0] + " +0: " + metric[1]+ " ?1: " + metric[2]+" ?0: " + metric[3]);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		Learning lr = new Learning();
		lr.getPerformance();
		//lr.readFileStatistics();
	}
}
