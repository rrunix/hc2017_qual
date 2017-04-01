package com.github.junoft.hc2017.greedy;


public interface ScoreCalculator {

    double calculateScore(VideoRequest vr, EndpointCacheServer ecs);
}
