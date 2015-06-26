package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cmu.arktweetnlp.Twokenize;
import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.impl.features.FeatureExtractor;

public class CMUTagger {
	public Model model;
    public FeatureExtractor featureExtractor;
    private String modelFilename = "model.20120919.txt";

    /**
     * Loads a model from a file.  The tagger should be ready to tag after calling this.
     * 
     * @param modelFilename
     * @throws IOException
     */
    public void loadModel() throws IOException {
            model = Model.loadModelFromText(modelFilename);
            featureExtractor = new FeatureExtractor(model, false);
    }

    /**
     * One token and its tag.
     **/
    public static class TaggedToken {
            public String token;
            public String tag;
    }


    /**
     * Run the tokenizer and tagger on one tweet's text.
     **/
    public List<TaggedToken> tokenizeAndTag(String text) {
            if (model == null) throw new RuntimeException("Must loadModel() first before tagging anything");
            List<String> tokens = Twokenize.tokenizeRawTweetText(text);

            Sentence sentence = new Sentence();
            sentence.tokens = tokens;
            ModelSentence ms = new ModelSentence(sentence.T());
            featureExtractor.computeFeatures(sentence, ms);
            model.greedyDecode(ms, false);

            ArrayList<TaggedToken> taggedTokens = new ArrayList<TaggedToken>();

            for (int t=0; t < sentence.T(); t++) {
                    TaggedToken tt = new TaggedToken();
                    tt.token = tokens.get(t);
                    tt.tag = model.labelVocab.name( ms.labels[t] );
                    taggedTokens.add(tt);
            }

            return taggedTokens;
    }

    /**
     * Illustrate how to load and call the POS tagger.
     * This main() is not intended for serious use; see RunTagger.java for that.
     **/
    public static void main(String[] args) throws IOException {
            /*if (args.length < 1) {
                    System.out.println("Supply the model filename as first argument.");
            }*/

            CMUTagger tagger = new CMUTagger();
            tagger.loadModel();

            String text = "Supporters @ DJ's for #Obamahttp://t.co/3dGtEVzt";
            List<TaggedToken> taggedTokens = tagger.tokenizeAndTag(text);

            for (TaggedToken token : taggedTokens) {
            	/*if(token.tag.equals("A")){*/
            		System.out.println(token.tag + token.token);
                //}
            	/*else if(token.tag.equals("E"))
        			System.out.println("E: " + token.token);*/
            }
            
            
    }

}
