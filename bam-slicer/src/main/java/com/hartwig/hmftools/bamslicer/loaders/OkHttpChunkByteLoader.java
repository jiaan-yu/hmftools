package com.hartwig.hmftools.bamslicer.loaders;

import java.io.IOException;
import java.net.URL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpChunkByteLoader implements ChunkByteLoader {
    private static final Logger LOGGER = LogManager.getLogger(OkHttpChunkByteLoader.class);
    @NotNull
    private final URL url;
    @NotNull
    private final URL headUrl;
    @NotNull
    private final OkHttpClient httpClient;

    public OkHttpChunkByteLoader(@NotNull final OkHttpClient httpClient, @NotNull final URL url, @NotNull final URL headUrl) {
        this.httpClient = httpClient;
        this.url = url;
        this.headUrl = headUrl;
    }

    @Override
    @NotNull
    public ListenableFuture<byte[]> getBytes(final long start, final long end) {
        if (start <= end) {
            return readUrlBytes(start, end - start + 1);
        } else {
            return Futures.immediateFailedFuture(new IllegalArgumentException("start offset is greater than end"));
        }
    }

    @NotNull
    private ListenableFuture<byte[]> readUrlBytes(final long offset, final long count) {
        final Request request = createRangeRequest(url, offset, count);
        return CallResponseFuture.create(httpClient.newCall(request), 10);
    }

    @VisibleForTesting
    @NotNull
    static Request createRangeRequest(@NotNull final URL url, final long offset, final long count) {
        final long end = offset + count - 1;
        final Headers headers = new Headers.Builder().add("Range", "bytes=" + offset + "-" + end).build();
        return new Request.Builder().url(url).headers(headers).build();
    }

    @Override
    @NotNull
    public String getSource() {
        return url.toString();
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
    }

    public long contentLength() throws IOException {
        final Request headRequest = new Request.Builder().url(headUrl).head().build();
        final Response response = httpClient.newCall(headRequest).execute();
        if (response.isSuccessful()) {
            try {
                final long contentLength = Long.parseLong(response.header("Content-Length"));
                LOGGER.info("Setting content length to {}", contentLength);
                return contentLength;
            } catch (NumberFormatException ignored) {
            } finally {
                response.close();
            }
        }
        LOGGER.error("Could not retrieve valid content length for {}", headUrl);
        LOGGER.error("Response {}: {}", response.code(), response.message());
        response.close();
        return -1;
    }
}
