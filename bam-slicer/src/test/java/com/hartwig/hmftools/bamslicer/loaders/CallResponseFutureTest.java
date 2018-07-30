package com.hartwig.hmftools.bamslicer.loaders;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.hartwig.hmftools.bamslicer.SlicerHttpClient;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

public class CallResponseFutureTest {
    @Test(expected = ExecutionException.class)
    public void doesNotRetryWhenRetryCountIs0() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(403));
        server.start();

        final CallResponseFuture future = createCall(server, 0);
        assertEquals(1, future.get().length);                   //MIVO: should throw (403 response with 0 retries)
        server.shutdown();
    }

    @Test
    public void retriesFailedRequest() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(403));
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 42 })));
        server.start();

        final CallResponseFuture future = createCall(server, 1);
        assertEquals(1, future.get().length);
        assertEquals(42, future.get()[0]);
        server.shutdown();
    }

    @Test(expected = CancellationException.class)
    public void cancelsRunningRequest() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 42 })).throttleBody(0, 2, TimeUnit.SECONDS));
        server.start();

        final CallResponseFuture future = createCall(server, 5);
        Thread.sleep(100);
        assertEquals(1, server.getRequestCount());
        future.cancel(true);
        assertEquals(1, future.get().length);                   //MIVO: should throw (accessing results of a cancelled task)
        server.shutdown();
    }

    @NotNull
    private static CallResponseFuture createCall(@NotNull final MockWebServer server, final int retries) {
        final URL url = server.url("/test").url();
        final OkHttpClient client = SlicerHttpClient.create(1);
        final Request request = OkHttpChunkByteLoader.createRangeRequest(url, 0, 100);
        final Call call = client.newCall(request);
        return CallResponseFuture.create(call, retries);
    }

}
