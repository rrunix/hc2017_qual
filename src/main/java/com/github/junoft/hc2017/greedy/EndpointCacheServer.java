package com.github.junoft.hc2017.greedy;


public class EndpointCacheServer implements Comparable<EndpointCacheServer> {

    public CacheServer cacheServer;

    public int latency;

    public EndpointCacheServer(CacheServer cacheServer, int latency) {
        this.cacheServer = cacheServer;
        this.latency = latency;
    }

    @Override
    public int compareTo(EndpointCacheServer o) {
        return Integer.compare(this.latency, o.latency);
    }
}
