package com.hartwig.hmftools.bamslicer.loaders;

import static org.junit.Assert.assertEquals;

import com.hartwig.hmftools.bamslicer.SlicerHttpClient;

import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class OkHttpChunkByteLoaderTest {
    @Test
    public void correctlyRetrievesContentLength() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setHeader("Content-Length", 200));
        server.start();

        final OkHttpChunkByteLoader httpLoader = new OkHttpChunkByteLoader(SlicerHttpClient.create(1), server.url("/test").url());
        assertEquals(200, httpLoader.contentLength());
        server.shutdown();
    }
}
