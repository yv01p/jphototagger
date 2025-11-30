package org.jphototagger.program.module.thumbnails.cache;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

class ThumbnailCacheVirtualThreadTest {

    @Test
    void usesVirtualThreadExecutor() throws Exception {
        ThumbnailCache cache = ThumbnailCache.INSTANCE;

        // Use reflection to verify executor type
        Field executorField = ThumbnailCache.class.getDeclaredField("virtualThreadFetcher");
        executorField.setAccessible(true);
        Object fetcher = executorField.get(cache);

        assertThat(fetcher).isNotNull();
        assertThat(fetcher).isInstanceOf(VirtualThreadThumbnailFetcher.class);
    }
}
