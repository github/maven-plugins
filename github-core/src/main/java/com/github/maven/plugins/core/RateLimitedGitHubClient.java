package com.github.maven.plugins.core;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.github.maven.plugins.core.egit.GitHubClientEgit;
import com.google.common.util.concurrent.RateLimiter;

public class RateLimitedGitHubClient extends GitHubClientEgit {

    private volatile RateLimiter rateLimiter;

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
        return super.createDelete(uri);
    }

    @Override
    protected HttpURLConnection createGet(String uri) throws IOException {
        return super.createGet(uri);
    }

    @Override
    protected HttpURLConnection createPost(String uri) throws IOException {
        rateLimiter().acquire();
        return super.createPost(uri);
    }

    @Override
    protected HttpURLConnection createPut(String uri) throws IOException {
        rateLimiter().acquire();
        return super.createPut(uri);
    }

    private RateLimiter rateLimiter() {
        final RateLimiter rateLimiter = this.rateLimiter;

        if (rateLimiter != null) {
            return rateLimiter;
        }

        return initializeRateLimiter();
    }

    private synchronized RateLimiter initializeRateLimiter() {

        if (rateLimiter != null) {
            return rateLimiter;
        }

        HttpURLConnection connection = null;

        try {

            //
            // Query rate limit.
            //

            connection = createGet("/rate_limit");

            final int remaining = connection.getHeaderFieldInt("X-RateLimit-Remaining", -1);
            final int reset = connection.getHeaderFieldInt("X-RateLimit-Reset", -1);
            final int now = (int) (currentTimeMillis() / 1000);

            //
            // Calculate the sustained request rate until the limits are reset.
            //

            return rateLimiter = RateLimiter.create((double) remaining / max(reset - now, 1));

        } catch (Exception e) {

            //
            // Fall back to 20 requests per minute.
            //
            // As per https://github.com/octokit/octokit.net/issues/638#issuecomment-67795998,
            // it seems that GitHub only allow 20 API calls per 1-minute period
            //

            return rateLimiter = RateLimiter.create(20. / 60.);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
