package com.github.junoft.hc2017;


import com.github.junoft.hc2017.greedy.EndpointCacheServer;
import com.github.junoft.hc2017.greedy.GreedyCacheDistributor;
import com.github.junoft.hc2017.greedy.ScoreCalculator;
import com.github.junoft.hc2017.greedy.VideoRequest;

import java.io.*;
import java.util.Scanner;


public class HashcodeQualification {

    private static final ScoreCalculator SPARSE_SCORE_CALCULATOR = new ScoreCalculator() {
        @Override
        public double calculateScore(VideoRequest vr, EndpointCacheServer ecs) {
            double cacheHits = ecs.cacheServer.getCacheHits();
            long latencyImprovement = ((vr.referenceLatency - ecs.latency) * vr.requestCount);
            double sizePenalty = Math.pow(vr.videoSize, 0.77);

            // Promote the 'worst' caches by multiplying the score per mb
            // by the inverse of the 'importance' of the cache server
            // Try to maximize the use of less relevant servers when there are more
            // choices with similar latencies
            double promoteWorstCaches = 1 / Math.pow(cacheHits, vr.getCost() / 4);

            return promoteWorstCaches * (latencyImprovement / sizePenalty);

        }
    };
    private static final ScoreCalculator UNIFORMLY_SCORE_CALCULATOR = new ScoreCalculator() {
        @Override
        public double calculateScore(VideoRequest vr, EndpointCacheServer ecs) {
            double cacheHits = ecs.cacheServer.getCacheHits();
            long latencyImprovement = ((vr.referenceLatency - ecs.latency) * vr.requestCount);
            double sizePenalty = vr.videoSize / 4;
            double promoteWorstCaches = 1 / Math.pow(cacheHits, vr.getCost() / 4);

            return promoteWorstCaches * (latencyImprovement / sizePenalty);
        }
    };
    /**
     * Size doesn't matter because all videos fits in cache and the caches are uniformly
     * distributed.
     */
    private static final ScoreCalculator UNIFORMLY_NON_MEM_SCORE_CALCULATOR = new ScoreCalculator() {
        @Override
        public double calculateScore(VideoRequest vr, EndpointCacheServer ecs) {
            long latencyImprovement = ((vr.referenceLatency - ecs.latency) * vr.requestCount);

            return latencyImprovement;
        }
    };

    public static void main(String[] args) throws Exception {

        long score = 0;


        score += doRun("kittens", "input/kittens.in", SPARSE_SCORE_CALCULATOR, 1);
        //me at the zoo has a random component (in the greedy step, chooses one of the best two elements at random, the best score is achieved when running multiple times
        //Base score (deterministic), with randChoice = 1 is 510288
        score += doRun("me_at_the_zoo", "input/me_at_the_zoo.in", SPARSE_SCORE_CALCULATOR, 2);
        score += doRun("trending_today", "input/trending_today.in", UNIFORMLY_NON_MEM_SCORE_CALCULATOR, 1);
        score += doRun("videos_worth_spreading", "input/videos_worth_spreading.in", UNIFORMLY_SCORE_CALCULATOR, 1);

        System.out.println("Got score " + score);
    }

    private static long doRun(String name, String fileInput, ScoreCalculator scoreCalculator, int randChoice) throws Exception {
        final String outputFile = fileInput.substring(fileInput.indexOf("/") + 1, fileInput.length() - 3) + ".out";

        BaseCacheDistributor cacheDistributor = new GreedyCacheDistributor(-1, scoreCalculator, randChoice);

        cacheDistributor.load(getFileScanner(fileInput));

        long score = cacheDistributor.run();


        cacheDistributor.export(getFileOutput(outputFile));

        System.out.println(String.format("\tFor %s got %d points", name, score));

        return score;
    }

    public static OutputStream getFileOutput(String filename) throws IOException {
        new File("output").mkdirs();
        return new FileOutputStream(new File("output/" + filename));
    }

    public static Scanner getFileScanner(String fileName) throws FileNotFoundException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(fileName);

        return new Scanner(is);
    }
}
