package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.xerox.socialmedia.communityrecommendation.dbmanager.SqlControler;
import com.xerox.socialmedia.communityrecommendation.sentiment.CMUTagger.TaggedToken;
import com.xerox.socialmedia.communityrecommendation.sentiment.NewSentimentAnalyzer.tweetScore;
import com.xerox.socialmedia.communityrecommendation.sentiment.SentiWordNet.Score;
import com.xerox.socialmedia.communityrecommendation.utils.Constant;

public class SentimentDBRunnable implements Runnable{
	private final int num;
	private final String tweet;
	private final Long tweetId;
	private final SentiWordNet swn;
	private final Emoticon en;
	private final CMUTagger tagger;
	private final Negation ng;
	private final SqlControler controller;
	
	public SentimentDBRunnable(int num, String tweet, Long tweetId, SentiWordNet swn, Emoticon en, CMUTagger tagger, Negation ng, SqlControler controller){
		this.num = num;
		this.tweet = tweet;
		this.tweetId = tweetId;
		this.swn = swn;
		this.en = en;
		this.tagger = tagger;
		this.ng = ng;
		this.controller = controller;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		//POS tagging
        List<TaggedToken> taggedTokens = tagger.tokenizeAndTag(tweet);
        
        //identify negation set
        HashSet<String> negSet = ng.identifyNegation(tweet);
        
        //a list containing the score of each effective word
        ArrayList<Score> scoreList = new ArrayList<Score>();
        
        for (TaggedToken token : taggedTokens){
            if(token.tag.equals("A")){ //adjectives
            	//System.out.println("token: " + token.token);
            	String stem = swn.stemmerWords(token.token);
            	Score wScore = swn.new Score();
            	wScore = swn.extract(stem);
            	//if negation of word exist, its pos and neg need to be exchanged
            	if(negSet.size() > 0 && negSet.contains(token.token)){
            		double temp = wScore.pos;
            		wScore.pos = wScore.neg;
            		wScore.neg = temp;
            	}
            	
            	scoreList.add(wScore);
            }
            else if(token.tag.equals("E")){ //emoticons 
            	int label = en.extractLabel(token.token);
            	//System.out.println("token: " + token.token);
            	Score eScore = swn.new Score();
            	eScore.pos = en.extractPos(label);
            	eScore.neg = en.extractNeg(label);
            	eScore.obj = en.extractObj(label);
            	scoreList.add(eScore);
            }
        }
        
        //average the score of each effective word of the tweet
        tweetScore tScore = new tweetScore();
        
     
        if(scoreList.size()>0){
        	tScore.averageScore(scoreList);	   
        }
        
        //write the result of each tweet into DB
        try {
        	double obj = 1 - tScore.pos - tScore.neg;
        	String update = "update tweets_sentiment_one_month set positivity = " + tScore.pos+ ", negativity = " + 
    				tScore.neg + ", objectivity = " + obj +" where tweet_id = "+ this.tweetId;

    		controller.update(update);
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//if(num % 1000 == 0)
			System.out.println(num + " records have been processed!");
		
	}

}
