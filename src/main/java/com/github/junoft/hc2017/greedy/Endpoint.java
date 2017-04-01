package com.github.junoft.hc2017.greedy;

import java.util.*;
import java.util.stream.Stream;


public class Endpoint {

    public final int endpointId;

    public final int latencyToDc;

    private EndpointCacheServer[] endpointCacheServers;

    private Map<CacheServer, Double> cacheHitRatios;

    public Endpoint(int endpointId, int latencyToDc) {
        this.endpointId = endpointId;
        this.latencyToDc = latencyToDc;
    }

    public void setCacheServerConnections(List<EndpointCacheServer> endpointCacheServers) {
        EndpointCacheServer[] connections = new EndpointCacheServer[endpointCacheServers.size()];
        endpointCacheServers.toArray(connections);

        this.endpointCacheServers = connections;

        Arrays.sort(this.endpointCacheServers);

        this.cacheHitRatios = new HashMap<CacheServer, Double>();

        DoubleSummaryStatistics statistics = Stream.of(this.endpointCacheServers)
                .mapToDouble(epcs -> epcs.latency).summaryStatistics();

        double maxLatency = statistics.getMax();


        // Compute the relevance of the cache servers
        Stream.of(this.endpointCacheServers)
                .forEach(cs -> {
                    double currLatency = cs.latency;

                    final double ratioInterval = 1.6;

                    double ratio = currLatency / (ratioInterval * maxLatency);

                    //smaller latencies are better
                    double invertedRatio = 1 - ratio;

                    this.cacheHitRatios.put(cs.cacheServer, invertedRatio);
                });
    }

    public final EndpointCacheServer[] getEndpointCacheServers() {
        return this.endpointCacheServers;
    }

    public int getLatencyFor(CacheServer cs) {
        for (EndpointCacheServer ecs : endpointCacheServers) {
            if (cs.cacheServerId == ecs.cacheServer.cacheServerId) {
                return ecs.latency;
            }
        }
        return -1;
    }

    public void hitCaches() {
        Stream.of(endpointCacheServers)
                .forEach(cs -> cs.cacheServer.hitCache(this.cacheHitRatios.get(cs.cacheServer)));
    }
}
