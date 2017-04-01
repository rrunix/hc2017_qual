package com.github.junoft.hc2017.greedy;


import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CacheServer {

    public final int cacheServerId;

    public final int memory;

    private final SortedSet<CachedVideo> cachedVideos;

    public int availableMemory;

    private double hits;

    public CacheServer(int cacheServerId, int memory) {
        this.cacheServerId = cacheServerId;
        this.memory = memory;
        this.cachedVideos = new TreeSet<>();

        this.reset();
    }

    public boolean containVideo(int videoId) {
        return cachedVideos.contains(videoId);
    }

    public void cacheVideo(CachedVideo cachedVideo) {
        this.availableMemory -= cachedVideo.videoSize;
        this.cachedVideos.add(cachedVideo);
    }

    public boolean canCacheVideo(int videoSize) {
        return videoSize <= availableMemory;
    }

    public List<Integer> getCachedVideos() {
        return cachedVideos.stream().map(cv -> cv.videoId).collect(Collectors.toList());
    }

    public void reset() {
        this.availableMemory = this.memory;
        this.cachedVideos.clear();
        this.hits = 0;
    }

    public synchronized void hitCache(double ratio) {
        //Compute relevance of this cache server
        this.hits += ratio;
    }

    public double getCacheHits() {
        return this.hits;
    }

    @Override
    public int hashCode() {
        return this.cacheServerId;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CacheServer) {
            return ((CacheServer) other).cacheServerId == this.cacheServerId;
        }
        return false;
    }
}
