package com.github.junoft.hc2017.greedy;

import java.util.Map;

public class VideoRequest {

    public Endpoint endpoint;

    public int requestCount;

    public int referenceLatency;

    public int videoSize;

    private double cost = 1.0d;

    private ScoreCalculator scoreCalculator;

    public VideoRequest(Endpoint endpoint, int requestCount, int videoSize, ScoreCalculator scoreCalculator) {
        this.endpoint = endpoint;
        this.requestCount = requestCount;
        this.referenceLatency = endpoint.latencyToDc;
        this.videoSize = videoSize;
        this.scoreCalculator = scoreCalculator;
    }


    public void computePartialScores(Map<CacheServer, Double> scores, int numberOfScores) {
        int currentScore = 0;
        int currentCache = 0;

        EndpointCacheServer[] cacheConnections = this.endpoint.getEndpointCacheServers();

        int maxNumberScores = numberOfScores == -1 ? cacheConnections.length :
                Integer.min(cacheConnections.length, numberOfScores);

        while (currentScore < maxNumberScores && currentCache < cacheConnections.length) {
            EndpointCacheServer ecs = cacheConnections[currentCache];

            if (ecs.cacheServer.canCacheVideo(this.videoSize)) {

                double score = this.computeDistributionScore(ecs);

                if (score <= 0) {
                    //End because no better score can be achieved.
                    currentScore = numberOfScores;
                } else {
                    scores.put(ecs.cacheServer, score + scores.getOrDefault(ecs.cacheServer, 0.0d));
                    ++currentScore;
                }
            }

            ++currentCache;
        }
    }


    protected double computeDistributionScore(EndpointCacheServer ecs) {
        if (ecs.latency < referenceLatency) {
            return scoreCalculator.calculateScore(this, ecs);
        }
        return -1;
    }

    public double notifyVideoCached(CacheServer cs) {
        int newLatency = this.endpoint.getLatencyFor(cs);

        if (newLatency > 0) {
            if (newLatency < this.referenceLatency) {
                this.referenceLatency = newLatency;

            }
            return (this.endpoint.latencyToDc - newLatency) * this.requestCount;
        }
        this.cost -= 0.001;

        return 0;
    }

    public long[] computeDistributionScore() {
        return new long[]{(endpoint.latencyToDc - referenceLatency) * this.requestCount, this.requestCount};
    }

    public double getCost() {
        return this.cost;
    }

    public void reset() {
        this.cost = 1.0d;
        this.referenceLatency = endpoint.latencyToDc;
    }

    public void hitCaches() {
        this.endpoint.hitCaches();
    }
}
