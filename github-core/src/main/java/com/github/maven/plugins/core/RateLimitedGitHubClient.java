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
    private RateLimiter rateLimiter = RateLimiter.create(20.0/60.0);

    public RateLimitedGitHubClient() {
        super();
    }

    public RateLimitedGitHubClient(String hostname) {
        super(hostname);
    }

    public RateLimitedGitHubClient(String hostname, int port, String scheme) {
        super(hostname, port, scheme);
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