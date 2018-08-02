package com.hartwig.hmftools.bamslicer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.bamslicer.loaders.OkHttpChunkByteLoader;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

public class PrefetchingSeekableStreamTest {

    @Test
    public void canReadBytesFromChunk() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 10);
        assertEquals(0, stream.position());
        assertEquals(5, stream.read(new byte[10], 0, 5));
        assertEquals(5, stream.position());
        assertFalse(stream.eof());
        server.shutdown();
    }

    @Test
    public void canReadFullChunk() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 10);
        assertEquals(0, stream.position());
        assertEquals(10, stream.read(new byte[10], 0, 10));
        assertEquals(10, stream.position());
        assertTrue(stream.eof());
        server.shutdown();
    }

    @Test
    public void doesNotReadPastEOF() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 10);
        assertEquals(10, stream.read(new byte[10], 0, 10));
        assertEquals(10, stream.position());
        assertTrue(stream.eof());
        assertEquals(-1, stream.read());
        server.shutdown();
    }

    @Test
    public void canReadBytesAcross2Chunks() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L), Pair.of(10L, 19L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 20);
        assertEquals(0, stream.position());
        assertEquals(10, stream.read(new byte[50], 0, 20));
        assertEquals(10, stream.position());
        assertEquals(10, stream.read(new byte[50], 10, 10));
        assertEquals(20, stream.position());
        assertTrue(stream.eof());
        server.shutdown();
    }

    @Test
    public void canReadFully() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 10);
        assertEquals(0, stream.position());
        stream.readFully(new byte[10]);
        assertEquals(10, stream.position());
        assertTrue(stream.eof());
        server.shutdown();
    }

    @Test
    public void canReadFullyAcross2Chunks() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L), Pair.of(10L, 19L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 20);
        assertEquals(0, stream.position());
        stream.readFully(new byte[20]);
        assertEquals(20, stream.position());
        assertTrue(stream.eof());
        server.shutdown();
    }

    @Test(expected = EOFException.class)
    public void throwsOnAttemptToReadFullyMoreBytesThanChunk() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 10);
        assertEquals(0, stream.position());
        stream.readFully(new byte[20]);
        server.shutdown();
    }

    @Test(expected = IOException.class)
    public void throwsOnAttemptToReadFromClosedStream() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.enqueue(new MockResponse().setBody(new Buffer().write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
        server.start();

        final List<Pair<Long, Long>> ranges = Lists.newArrayList(Pair.of(0L, 9L), Pair.of(10L, 19L));
        final PrefetchingSeekableStream stream = createSeekableStream(server.url("/test").url(), ranges, 20);
        assertEquals(0, stream.position());
        stream.close();
        stream.readFully(new byte[20]);
        server.shutdown();
    }

    @NotNull
    private static PrefetchingSeekableStream createSeekableStream(@NotNull final URL url, @NotNull final List<Pair<Long, Long>> ranges,
            final long contentLength) throws IOException {
        final OkHttpChunkByteLoader httpLoader = new OkHttpChunkByteLoader(SlicerHttpClient.create(1), url, url);
        return new PrefetchingSeekableStream(httpLoader, buildLookaheadMap(ranges), contentLength);
    }

    @NotNull
    private static ConcurrentSkipListMap<Long, Pair<Long, Long>> buildLookaheadMap(@NotNull final List<Pair<Long, Long>> ranges) {
        final ConcurrentSkipListMap<Long, Pair<Long, Long>> chunksPerOffset = new ConcurrentSkipListMap<>();
        ranges.forEach(range -> chunksPerOffset.put(range.getLeft(), range));
        return chunksPerOffset;
    }
}
