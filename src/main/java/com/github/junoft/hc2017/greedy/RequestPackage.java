package com.github.junoft.hc2017.greedy;

import java.util.*;
import java.util.Map.Entry;

public class RequestPackage implements Comparable<RequestPackage> {

    private static final Random RAND = new Random();

    public final int videoId;

    public final int videoSize;

    public final int computeDepth;

    public final int randChoice;

    private final List<VideoRequest> videoRequests;

    private final Map<CacheServer, Double> scores;

    private double score;

    private CacheServer targetCache;

    public RequestPackage(int videoId, int videoSize, int computeDepth, int randChoice) {
        this.videoId = videoId;
        this.computeDepth = computeDepth;
        this.videoSize = videoSize;
        this.randChoice = randChoice;
        this.videoRequests = new ArrayList<>();
        this.scores = new LinkedHashMap<>();

        this.reset();
    }

    public void addVideoRequest(VideoRequest videoRequest) {
        VideoRequest other = null;
        if ((other = getRequest(videoRequest.endpoint.endpointId)) != null) {
            other.requestCount += videoRequest.requestCount;
        } else {
            this.videoRequests.add(videoRequest);
        }
    }

    public VideoRequest getRequest(int endpointId) {
        for (VideoRequest vr : this.videoRequests) {
            if (vr.endpoint.endpointId == endpointId) {
                return vr;
            }
        }
        return null;
    }

    public void computePartialScores() {
        this.scores.clear();
        this.videoRequests.stream().forEach((request) -> request.computePartialScores(this.scores, this.computeDepth));

        if (this.scores.size() > 0) {
            int choice = RAND.nextInt(Math.min(this.randChoice, this.scores.size()));

            Optional<Entry<CacheServer, Double>> maybeEntry
                    = this.scores.entrySet().stream().sorted((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()))
                    .skip(choice).findFirst();

            Entry<CacheServer, Double> entry = maybeEntry.get();
            this.score = entry.getValue();
            this.targetCache = entry.getKey();
        } else {
            this.score = -1;
            this.targetCache = null;
        }
    }


    public int acquireCache() {
        if (targetCache.canCacheVideo(this.videoSize)) {
            double scoreGained = this.notifyVideoCached();

            CachedVideo cachedVideo = new CachedVideo(this.videoId, scoreGained, this.videoSize);
            this.targetCache.cacheVideo(cachedVideo);
            this.computePartialScores();
            return this.videoSize;
        }
        this.computePartialScores();
        return 0;
    }


    public double notifyVideoCached() {
        return this.videoRequests.stream().mapToDouble(vr -> vr.notifyVideoCached(this.targetCache)).sum();
    }


    public long[] computeDistributionScore() {
        long[] score = new long[]{0, 0};

        for (VideoRequest vr : this.videoRequests) {
            long[] vScore = vr.computeDistributionScore();

            score[0] += vScore[0];
            score[1] += vScore[1];
        }

        return score;
    }

    @Override
    public int compareTo(RequestPackage o) {
        //Reverse order
        if (score < o.score)
            return 1;
        if (score > o.score)
            return -1;
        return 0;
    }

    public Map<CacheServer, Double> getScores() {
        return this.scores;
    }

    public double getScore() {
        return this.score;
    }

    public CacheServer getTargetCache() {
        return this.targetCache;
    }

    public int numberOfVideoRequests() {
        return this.videoRequests.size();
    }

    public List<VideoRequest> getVideoRequests() {
        return this.videoRequests;
    }

    public int getVideoSize() {
        return this.videoSize;
    }

    public void hitCaches() {
        this.videoRequests.stream().forEach(vr -> vr.hitCaches());
    }


    public void reset() {
        this.scores.clear();
        this.targetCache = null;
        this.score = -1;

        this.videoRequests.stream().forEach(vr -> vr.reset());
    }
}
