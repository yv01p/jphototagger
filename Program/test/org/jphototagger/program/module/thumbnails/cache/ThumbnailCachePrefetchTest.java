package org.jphototagger.program.module.thumbnails.cache;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

class ThumbnailCachePrefetchTest {

    @Test
    void prefetchParallel_methodExists() throws Exception {
        Method method = ThumbnailCache.class.getMethod("prefetchParallel", List.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }
}
