package com.github.junoft.hc2017.greedy;


public class CachedVideo implements Comparable<CachedVideo> {

    public final int videoId;

    public final double score;

    public final int videoSize;

    public CachedVideo(int videoId, double score, int videoSize) {
        this.videoId = videoId;
        this.score = score;
        this.videoSize = videoSize;
    }

    @Override
    public int compareTo(CachedVideo o) {
        return Double.compare(score, o.score);
    }
}
