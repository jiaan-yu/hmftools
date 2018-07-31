package com.hartwig.hmftools.bamslicer.loaders;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import com.hartwig.hmftools.bamslicer.SlicerHttpClient;

import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

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

    @Test(expected = ExecutionException.class)
    public void failsOnIllegalStartEnd() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 42 })));
        server.start();

        final OkHttpChunkByteLoader httpLoader = new OkHttpChunkByteLoader(SlicerHttpClient.create(1), server.url("/test").url());
        assertEquals(1, httpLoader.getBytes(50, 10).get().length);              //MIVO: should throw due to illegal arguments
        server.shutdown();
    }

    @Test
    public void retrievesBytes() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 42 })));
        server.start();

        final OkHttpChunkByteLoader httpLoader = new OkHttpChunkByteLoader(SlicerHttpClient.create(1), server.url("/test").url());
        assertEquals(1, httpLoader.getBytes(0, 10).get().length);
        server.shutdown();
    }
}
