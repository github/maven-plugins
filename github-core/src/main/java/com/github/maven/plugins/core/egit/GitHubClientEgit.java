/*
 * Copyright (c) 2011 GitHub Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.github.maven.plugins.core.egit;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import org.eclipse.egit.github.core.client.GitHubClient;

/**
 * GitHubClient support proxy
 * 
 * @author Kiyofumi Kondoh
 */
public class GitHubClientEgit extends GitHubClient {

	public GitHubClientEgit() {
		super();
	}

	public GitHubClientEgit(String hostname) {
		super(hostname);
	}

	public GitHubClientEgit(String hostname, int port, String scheme) {
		super(hostname, port, scheme);
	}
	
	protected Proxy proxy;

	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	@Override
	protected HttpURLConnection createConnection(String uri) throws IOException {
		URL url = new URL(createUri(uri));
		if ( null == proxy ) 		{
			return (HttpURLConnection) url.openConnection();
		} else {
			return (HttpURLConnection) url.openConnection( proxy );
		}
	}

}
