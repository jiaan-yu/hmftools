package com.hartwig.hmftools.bamslicer.loaders;

import java.io.IOException;

import com.google.common.util.concurrent.AbstractFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CallResponseFuture extends AbstractFuture<byte[]> {
    private static final Logger LOGGER = LogManager.getLogger(CallResponseFuture.class);

    @NotNull
    private Call underlyingCall;

    static CallResponseFuture create(@NotNull final Call call, final int retryCount) {
        return new CallResponseFuture(call, retryCount);
    }

    @Override
    protected void interruptTask() {
        underlyingCall.cancel();
    }

    @NotNull
    private Callback retryingCallback(final int retryCount) {
        return new Callback() {
            @Override
            public void onFailure(@NotNull final Call call, @NotNull final IOException e) {
                retryCall(call, e, retryCount);
            }

            @Override
            public void onResponse(@NotNull final Call call, @NotNull final Response response) {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) {
                        set(body.bytes());
                    } else {
                        final String nullBody = body == null ? "body = null" : "";
                        final Exception e = new IOException("Response " + response.code() + ": " + response.message() + "; " + nullBody);
                        retryCall(call, e, retryCount);
                    }
                } catch (final Exception e) {
                    retryCall(call, e, retryCount);
                }
            }
        };
    }

    private void retryCall(@NotNull final Call call, @NotNull final Exception exception, final int remainingRetries) {
        if (isCancelled()) {
            return;
        }
        if (remainingRetries <= 0) {
            final String httpHeadersString = call.request().headers().toString().replace("\n", " ");
            LOGGER.error("Call {} [{}] failed with: {}", call.request().method(), httpHeadersString, exception.getMessage());
            setException(exception);
        } else {
            this.underlyingCall = call.clone();
            underlyingCall.enqueue(retryingCallback(remainingRetries - 1));
        }
    }

    private CallResponseFuture(@NotNull final Call call, final int retryCount) {
        this.underlyingCall = call;
        underlyingCall.enqueue(retryingCallback(retryCount));
    }
}
