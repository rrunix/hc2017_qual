package com.github.junoft.hc2017.greedy;


import com.github.junoft.hc2017.BaseCacheDistributor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GreedyCacheDistributor extends BaseCacheDistributor {


    private final int computeDepth;

    private CacheServer[] cacheServers;

    private Endpoint[] endpoints;

    private RequestPackage[] requestPackages;

    private int[] videos;

    private int videoMinSize = Integer.MAX_VALUE;

    private ScoreCalculator scoreCalculator;

    private double remaingMemory;

    private int randChoice;

    public GreedyCacheDistributor(int computeDepth, ScoreCalculator scoreCalculator, int randChoice) {
        this.computeDepth = computeDepth;
        this.scoreCalculator = scoreCalculator;
        this.randChoice = randChoice;
    }

    @Override
    protected void distributeCache() {

        //Calculate the number of hits per cache server in order to
        //discover the most important cache servers, so we
        //can promote the other servers.
        //Most important = (most connections to and lower latencies)
        Stream.of(this.requestPackages).forEach(rp -> rp.hitCaches());

        //Optimize the cache distribution per mb
        PriorityBlockingQueue<RequestPackage> queue =
                Stream.of(this.requestPackages).parallel()
                        .filter(rp -> rp.numberOfVideoRequests() != 0)
                        .filter(rp -> rp.getVideoSize() < this.x)
                        .filter(rp -> {
                            rp.computePartialScores();
                            return rp.getScore() > 0;
                        })
                        .collect(Collectors.toCollection(PriorityBlockingQueue::new));

        this.remaingMemory = this.x * this.c;

        while (queue.isEmpty() == false) {
            RequestPackage rPackage = queue.poll();

            double consume = rPackage.acquireCache();

            if (rPackage.getScore() > 0) {
                queue.add(rPackage);
            }

            this.remaingMemory -= consume;
        }

        System.out.println("Remaining memory in all cache servers " + this.remaingMemory);

        // TODO: Use a smarter method to distribute the memory and try to minimize the residual memory left in
        // the cache servers.
    }

    @Override
    protected void createRequest(int endpointId, int videoId, int requestCount) {
        this.requestPackages[videoId].addVideoRequest(new VideoRequest(this.endpoints[endpointId], requestCount, this.videos[videoId], this.scoreCalculator));
    }

    @Override
    protected void setUpRequests(int requestCount) {
        this.requestPackages = new RequestPackage[this.v];

        for (int i = 0; i < this.requestPackages.length; i++) {
            this.requestPackages[i] = new RequestPackage(i, this.videos[i], this.computeDepth, this.randChoice);
        }
    }

    @Override
    protected void creteEndpoint(int endpointId, int latencyToDc, int[] connectedCs, int[] latencyCs) {
        Endpoint endpoint = new Endpoint(endpointId, latencyToDc);
        endpoint.setCacheServerConnections(
                IntStream.range(0, connectedCs.length)
                        .mapToObj(i -> new EndpointCacheServer(this.cacheServers[connectedCs[i]], latencyCs[i]))
                        .collect(Collectors.toList()));

        this.endpoints[endpointId] = endpoint;
    }

    @Override
    protected void setUpEndpoints(int endpointCount) {
        this.endpoints = new Endpoint[endpointCount];
    }

    @Override
    protected void createVideo(int videoId, int videoMb) {
        this.videos[videoId] = videoMb;

        if (videoMb < this.videoMinSize) {
            this.videoMinSize = videoMb;
        }
    }

    @Override
    protected void setUpVideos(int videoCount) {
        this.videos = new int[videoCount];
    }

    @Override
    protected void setUpCacheServers(int cacheServerCount, int cacheServerMemory) {
        this.cacheServers = new CacheServer[cacheServerCount];

        for (int i = 0; i < cacheServerCount; i++) {
            this.cacheServers[i] = new CacheServer(i, cacheServerMemory);
        }
    }

    @Override
    protected int getCacheServerCount() {
        return this.cacheServers.length;
    }

    @Override
    protected Integer[] getCachedVideos(int cacheServerId) {
        Collection<Integer> cachedVideosSet = this.cacheServers[cacheServerId].getCachedVideos();
        Integer[] cachedVideos = new Integer[cachedVideosSet.size()];

        cachedVideosSet.toArray(cachedVideos);

        Arrays.sort(cachedVideos);

        return cachedVideos;
    }

    @Override
    protected long evaluateCacheDistributor() {
        Optional<long[]> maybeScores = IntStream.range(0, this.requestPackages.length).parallel()
                .mapToObj(i -> this.requestPackages[i].computeDistributionScore())
                .reduce((score1, score2) ->
                        new long[]{score1[0] + score2[0], score1[1] + score2[1]});

        if (maybeScores.isPresent() && maybeScores.get()[1] > 0) {
            long[] score = maybeScores.get();
            return ((1000 * score[0]) / score[1]);
        } else {
            return 0;
        }
    }

    @Override
    protected void reset() {
        Stream.of(this.cacheServers).forEach(cs -> cs.reset());
        Stream.of(this.requestPackages).forEach(rp -> rp.reset());
        this.remaingMemory = this.x * this.c;
    }
}
