package com.github.junoft.hc2017;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Scanner;

public abstract class BaseCacheDistributor {

    protected int v;

    protected int e;

    protected int r;

    protected int c;

    protected int x;

    protected String name;

    public void load(Scanner input) {
        this.readData(input);
    }

    public long run() {
        this.reset();
        this.distributeCache();

        long score = this.evaluateCacheDistributor();
        return score;
    }

    public void export(OutputStream os) throws IOException {
        this.writeOutput(os);
    }

    private void writeOutput(OutputStream os) throws IOException {
        os.write((Integer.toString(this.getCacheServerCount()) + " \n").getBytes());

        for (int cacheServerId = 0; cacheServerId < this.getCacheServerCount(); cacheServerId++) {
            os.write(Integer.toString(cacheServerId).getBytes());

            Integer[] cachedVideo = this.getCachedVideos(cacheServerId);
            Arrays.sort(cachedVideo);

            for (Integer video : cachedVideo) {
                os.write((" " + Integer.toString(video)).getBytes());
            }
            os.write("\n".getBytes());
        }
        os.flush();
    }

    private void readData(Scanner input) {
        // Read args
        this.v = input.nextInt();
        this.e = input.nextInt();
        this.r = input.nextInt();
        this.c = input.nextInt();
        this.x = input.nextInt();

        input.nextLine();

        // Set up cache servers
        this.setUpCacheServers(this.c, this.x);

        // Read videos
        this.readVideos(input);
        this.readEndpoints(input);
        this.readRequests(input);
    }

    private void readRequests(Scanner input) {
        this.setUpRequests(this.r);

        for (int requestNum = 0; requestNum < this.r; requestNum++) {
            int rv = input.nextInt();
            int re = input.nextInt();
            int rn = input.nextInt();

            // A trail line in the file need to be added (avoid checking whether is the last line or not)
            input.nextLine();

            this.createRequest(re, rv, rn);
        }

    }

    private void readEndpoints(Scanner input) {
        //Set up endpoints
        this.setUpEndpoints(this.e);

        for (int endpointId = 0; endpointId < this.e; endpointId++) {
            int latencyToDC = input.nextInt();
            int k = input.nextInt();

            input.nextLine();

            int[] connectedCs = new int[k];
            int[] latencyCs = new int[k];

            for (int item = 0; item < k; item++) {
                connectedCs[item] = input.nextInt();
                latencyCs[item] = input.nextInt();

                input.nextLine();
            }

            this.creteEndpoint(endpointId, latencyToDC, connectedCs, latencyCs);
        }
    }

    private void readVideos(Scanner input) {
        // Set up videos
        this.setUpVideos(this.v);

        // Create videos
        String[] videosSize = input.nextLine().split(" ");

        int videoId = 0;

        for (String videoSize : videosSize) {
            this.createVideo(videoId, Integer.parseInt(videoSize));
            videoId++;
        }
    }

    protected abstract void distributeCache();

    protected abstract void createRequest(int endpointId, int videoId, int requestCount);

    protected abstract void setUpRequests(int requestCount);

    protected abstract void creteEndpoint(int endpointId, int latencyToDc, int[] connectedCs, int[] latencyCs);

    protected abstract void setUpEndpoints(int endpointCount);

    protected abstract void createVideo(int videoId, int videoMb);

    protected abstract void setUpVideos(int videoCount);

    protected abstract void setUpCacheServers(int cacheServerCount, int cacheServerMemory);

    protected abstract int getCacheServerCount();

    protected abstract Integer[] getCachedVideos(int cacheServerId);

    protected abstract long evaluateCacheDistributor();

    protected abstract void reset();
}

