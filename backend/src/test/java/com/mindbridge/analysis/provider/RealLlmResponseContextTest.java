package com.mindbridge.analysis.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RealLlmResponseContext}. The context is a
 * {@link ThreadLocal} carrier so the tests focus on the lifecycle
 * (set / read / clear) and on the validation of the {@link
 * RealLlmResponseContext.Snapshot} record.
 */
@DisplayName("RealLlmResponseContext")
class RealLlmResponseContextTest {

    @AfterEach
    void clearAfterEach() {
        RealLlmResponseContext.clear();
    }

    @Test
    @DisplayName("current() is null when nothing has been set")
    void current_isNullByDefault() {
        assertThat(RealLlmResponseContext.current()).isNull();
    }

    @Test
    @DisplayName("set() stores a snapshot retrievable via current()")
    void set_thenCurrent_returnsSameSnapshot() {
        RealLlmResponseContext.Snapshot s =
                new RealLlmResponseContext.Snapshot("openai", "gpt-4o-mini");
        RealLlmResponseContext.set(s);
        assertThat(RealLlmResponseContext.current()).isSameAs(s);
    }

    @Test
    @DisplayName("clear() removes the snapshot")
    void clear_removesSnapshot() {
        RealLlmResponseContext.set(
                new RealLlmResponseContext.Snapshot("openai", "gpt-4o-mini"));
        RealLlmResponseContext.clear();
        assertThat(RealLlmResponseContext.current()).isNull();
    }

    @Test
    @DisplayName("clear() is idempotent")
    void clear_isIdempotent() {
        RealLlmResponseContext.clear();
        RealLlmResponseContext.clear();
        assertThat(RealLlmResponseContext.current()).isNull();
    }

    @Test
    @DisplayName("set(null) throws IllegalArgumentException")
    void set_null_throws() {
        assertThatThrownBy(() -> RealLlmResponseContext.set(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Snapshot rejects null/blank provider")
    void snapshot_rejectsNullProvider() {
        assertThatThrownBy(() ->
                new RealLlmResponseContext.Snapshot(null, "gpt-4o-mini"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Snapshot rejects null/blank model")
    void snapshot_rejectsNullModel() {
        assertThatThrownBy(() ->
                new RealLlmResponseContext.Snapshot("openai", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Snapshot rejects provider > 50 chars")
    void snapshot_rejectsLongProvider() {
        String longName = "a".repeat(51);
        assertThatThrownBy(() ->
                new RealLlmResponseContext.Snapshot(longName, "gpt-4o-mini"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider");
    }

    @Test
    @DisplayName("Snapshot rejects model > 100 chars")
    void snapshot_rejectsLongModel() {
        String longName = "a".repeat(101);
        assertThatThrownBy(() ->
                new RealLlmResponseContext.Snapshot("openai", longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model");
    }

    @Test
    @DisplayName("Snapshot accepts provider exactly 50 chars")
    void snapshot_acceptsFiftyCharProvider() {
        String s = "a".repeat(50);
        RealLlmResponseContext.Snapshot snap =
                new RealLlmResponseContext.Snapshot(s, "gpt-4o-mini");
        assertThat(snap.provider()).isEqualTo(s);
    }

    @Test
    @DisplayName("Snapshot accepts model exactly 100 chars")
    void snapshot_acceptsHundredCharModel() {
        String m = "a".repeat(100);
        RealLlmResponseContext.Snapshot snap =
                new RealLlmResponseContext.Snapshot("openai", m);
        assertThat(snap.model()).isEqualTo(m);
    }
}
