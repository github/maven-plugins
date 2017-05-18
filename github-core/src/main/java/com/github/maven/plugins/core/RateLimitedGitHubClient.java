package com.github.maven.plugins.core;

import com.github.maven.plugins.core.egit.GitHubClientEgit;
import com.google.common.util.concurrent.RateLimiter;

import java.io.IOException;
import java.net.HttpURLConnection;

public class RateLimitedGitHubClient extends GitHubClientEgit {

    /**
     * AS per https://github.com/octokit/octokit.net/issues/638#issuecomment-67795998,
     * it seems that GitHub only allow 20 API calls per 1-minute period
     */
    private RateLimiter rateLimiter;

    public RateLimitedGitHubClient(double callsPerMinute) {
        super();
        rateLimiter = RateLimiter.create(callsPerMinute/60.0);
    }

    public RateLimitedGitHubClient(String hostname, double callsPerMinute) {
        super(hostname);
        rateLimiter = RateLimiter.create(callsPerMinute/60.0);
    }

    public RateLimitedGitHubClient(String hostname, int port, String scheme, double callsPerMinute) {
        super(hostname, port, scheme);
        rateLimiter = RateLimiter.create(callsPerMinute/60.0);
    }

    @Override
    protected HttpURLConnection createDelete(String uri) throws IOException {
        //rateLimiter.acquire();
        return super.createDelete(uri);
    }

    @Override
    protected HttpURLConnection createGet(String uri) throws IOException {
        //rateLimiter.acquire();
        return super.createGet(uri);
    }

    @Override
    protected HttpURLConnection createPost(String uri) throws IOException {
        rateLimiter.acquire();
        return super.createPost(uri);
    }

    @Override
    protected HttpURLConnection createPut(String uri) throws IOException {
        rateLimiter.acquire();
        return super.createPut(uri);
    }
}