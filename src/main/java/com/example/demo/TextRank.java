package com.example.demo;


/**
 * Created by WuLC on 2017/6/8.
 *
 */

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import java.io.*;
import java.util.*;

public class TextRank {
    private static final float d = 0.85f;           //damping factor, default 0.85
    private static final int max_iter = 200;        //max iteration times
    private static final float min_diff = 0.0001f;  //condition to judge whether recurse or not
    private static int nKeyword = 5;         //number of keywords to extract,default 5
    private static int coOccuranceWindow = 3; //size of the co-occurance window, default 3
    private static Set<String> stopWords = new HashSet<>();


    // load stop words when instance is created
    static {
        BufferedReader br = null;
        InputStream ir = null;
        try {
            //Get file from resources folder
            String stopWordFile = "/stopwords.txt";
            ir = TextRank.class.getResourceAsStream(stopWordFile);
            br = new BufferedReader(new InputStreamReader(ir));
            String curr;
            while ((curr = br.readLine()) != null) {
                stopWords.add(curr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();

                if (ir != null)
                    ir.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // change default parameters
    public static void setKeywordNumber(int sysKeywordNum) {
        nKeyword = sysKeywordNum;
    }


    public static void setWindowSize(int window) {
        coOccuranceWindow = window;
    }


    /**
     * extract keywords in terms of title and content of document
     *
     * @param title(String):   title of document
     * @param content(String): content of document
     * @return (List < String >): list of keywords
     */
    public static List<String> getKeyword(String title, String content) {

        Map<String, Float> score = TextRank.getWordScore(title, content);

        //rank keywords in terms of their score
        List<Map.Entry<String, Float>> entryList = new ArrayList<>(score.entrySet());
        entryList.sort((o1, o2) -> (int) (o2.getValue() - o1.getValue()));

        //System.out.println("After sorting: "+entryList);

        List<String> sysKeywordList = new ArrayList<>();

        //List<String>  unmergedList=new ArrayList<String>();
        for (int i = 0; i < nKeyword; ++i) {
            try {
                //unmergedList.add(entryList.get(i).getKey());
                sysKeywordList.add(entryList.get(i).getKey());
            } catch (IndexOutOfBoundsException ignored) {
            }
        }

        System.out.print("window:" + coOccuranceWindow + "\nkeywordNum: " + nKeyword + "\n");
        return sysKeywordList;
    }


    /**
     * return score of each word after TextRank algorithm
     *
     * @param title(String):   title of document
     * @param content(String): content of document
     * @return (Map < String, Float >):  score of each word
     */
    public static Map<String, Float> getWordScore(String title, String content) {

        //segment text into words

        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<SegToken> tokens = segmenter.process(title + content, JiebaSegmenter.SegMode.INDEX);

        int count = 1;  //position of each word
        Map<String, Integer> wordPosition = new HashMap<>();

        List<String> wordList = new ArrayList<>();

        //filter stop words
        for (SegToken t : tokens) {
            if (!stopWords.contains(t.word)) {
                wordList.add(t.word);
                if (!wordPosition.containsKey(t.word)) {
                    wordPosition.put(t.word, count);
                    count++;
                }
            }
        }

        //System.out.println("Keyword candidates:"+wordList);

        //generate word-graph in terms of size of co-occur window
        Map<String, Set<String>> words = new HashMap<>();
        Queue<String> que = new LinkedList<>();
        for (String w : wordList) {
            if (!words.containsKey(w)) {
                words.put(w, new HashSet<>());
            }
            que.offer(w);    // insert into the end of the queue
            if (que.size() > coOccuranceWindow) {
                que.poll();  // pop from the queue
            }

            for (String w1 : que) {
                for (String w2 : que) {
                    if (w1.equals(w2)) {
                        continue;
                    }

                    words.get(w1).add(w2);
                    words.get(w2).add(w1);
                }
            }
        }
        //System.out.println("word-graph:"+words); //each k,v represents all the words in v point to k

        // iterate till recurse
        Map<String, Float> score = new HashMap<>();
        for (int i = 0; i < max_iter; ++i) {
            Map<String, Float> m = new HashMap<>();
            float max_diff = 0;
            for (Map.Entry<String, Set<String>> entry : words.entrySet()) {
                String key = entry.getKey();
                Set<String> value = entry.getValue();
                m.put(key, 1 - d);
                for (String other : value) {
                    int size = words.get(other).size();
                    if (key.equals(other) || size == 0) continue;
                    m.put(key, m.get(key) + d / size * (score.get(other) == null ? 0 : score.get(other)));
                }

                max_diff = Math.max(max_diff, Math.abs(m.get(key) - (score.get(key) == null ? 1 : score.get(key))));
            }
            score = m;

            //exit once recurse
            if (max_diff <= min_diff)
                break;
        }
        return score;
    }
}
