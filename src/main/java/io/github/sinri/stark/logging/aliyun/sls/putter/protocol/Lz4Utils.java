package io.github.sinri.stark.logging.aliyun.sls.putter.protocol;

import io.vertx.core.buffer.Buffer;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * LZ4压缩工具类
 * <p>
 * 使用ThreadLocal缓冲区池优化内存分配，减少GC压力。每个线程维护自己的缓冲区，避免线程竞争的同时提供内存复用。
 *
 * @since 5.0.0
 */
public final class Lz4Utils {
    static final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
    static final LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();

    /**
     * ThreadLocal缓冲区池，每个线程维护自己的缓冲区
     *
     * @since 3.0.0.1
     */
    private static final ThreadLocal<BufferPool> bufferPool = ThreadLocal.withInitial(BufferPool::new);

    /**
     * 使用LZ4算法压缩一个{@link Buffer}对象内的字节数组，并返回压缩后的字节数组组成的{@link Buffer}对象。
     *
     * @param buffer 待压缩的Buffer
     * @return 压缩后的Buffer
     */
    public static Buffer compress(@Nullable Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            return Buffer.buffer();
        }
        return Buffer.buffer(compress(buffer.getBytes()));
    }

    /**
     * 使用LZ4算法压缩字节数组，之前的实现
     *
     * @param srcBytes 待压缩的字节数组
     * @return 压缩后的字节数组
     * @since 2.1.0
     * @deprecated as of 3.0.0.1, renamed from original {@link #compress(byte[])} and will be removed in a future release
     */
    @Deprecated(forRemoval = true)
    static byte[] compressLegacy(byte @Nullable [] srcBytes) {
        if (srcBytes == null || srcBytes.length == 0) {
            return new byte[0];
        }

        int maxCompressedLength = compressor.maxCompressedLength(srcBytes.length);
        byte[] compressedBytes = new byte[maxCompressedLength];
        int compressedLength = compressor.compress(srcBytes, 0, srcBytes.length, compressedBytes, 0);
        byte[] resultBytes = new byte[compressedLength];
        System.arraycopy(compressedBytes, 0, resultBytes, 0, compressedLength);
        return resultBytes;
    }

    /**
     * 使用LZ4算法压缩字节数组
     * <p>
     * 使用ThreadLocal缓冲区池优化内存分配，减少GC压力。
     *
     * @param srcBytes 待压缩的字节数组
     * @return 压缩后的字节数组
     * @since 3.0.0.1
     */
    public static byte[] compress(byte @Nullable [] srcBytes) {
        if (srcBytes == null || srcBytes.length == 0) {
            return new byte[0];
        }

        BufferPool pool = bufferPool.get();
        try {
            int maxCompressedLength = compressor.maxCompressedLength(srcBytes.length);
            byte[] compressedBytes = pool.getBuffer(maxCompressedLength);

            int compressedLength = compressor.compress(srcBytes, 0, srcBytes.length, compressedBytes, 0);

            // 总是返回精确大小的数组，避免返回过大的缓冲区
            return java.util.Arrays.copyOf(compressedBytes, compressedLength);
        } finally {
            // 归还缓冲区并进行内存管理
            pool.afterUsingBuffer();
        }
    }

    /**
     * 使用LZ4算法解压缩一个{@link Buffer}对象内的字节数组，并返回解压后的字节数组组成的{@link Buffer}对象。
     *
     * @param compressedBuffer 待解压的Buffer
     * @param originalLength   原始数据的长度（解压后的长度）
     * @return 解压后的Buffer
     * @since 5.0.0
     */
    public static Buffer decompress(@Nullable Buffer compressedBuffer, int originalLength) {
        if (compressedBuffer == null || compressedBuffer.length() == 0) {
            return Buffer.buffer();
        }
        return Buffer.buffer(decompress(compressedBuffer.getBytes(), originalLength));
    }

    /**
     * 使用LZ4算法解压缩字节数组。
     *
     * @param compressedBytes 待解压的字节数组
     * @param originalLength  原始数据的长度（解压后的长度）
     * @return 解压后的字节数组
     * @since 5.0.0
     */
    public static byte[] decompress(byte @Nullable [] compressedBytes, int originalLength) {
        if (compressedBytes == null || compressedBytes.length == 0) {
            return new byte[0];
        }

        byte[] decompressedBytes = new byte[originalLength];
        decompressor.decompress(compressedBytes, 0, decompressedBytes, 0, originalLength);
        return decompressedBytes;
    }

    /**
     * 缓冲区池实现类
     *
     * @since 3.0.0.1
     */
    @NullMarked
    private static class BufferPool {
        private static final int BASE_BUFFER_SIZE = 10 * 1024 * 1024;
        private final byte[] fixedBuffer;
        private final byte[] emptyBuffer;
        private byte[] tempBuffer;
        private long tempBirth;

        BufferPool() {
            fixedBuffer = new byte[BASE_BUFFER_SIZE];
            emptyBuffer = new byte[0];
            tempBuffer = emptyBuffer;
        }

        private void resetTempBuffer(int bufferSize) {
            if (bufferSize <= 0) {
                tempBuffer = emptyBuffer;
                tempBirth = 0;
            } else {
                tempBuffer = new byte[bufferSize];
                tempBirth = System.currentTimeMillis();
            }
        }

        byte[] getBuffer(int requiredSize) {
            if (requiredSize <= BASE_BUFFER_SIZE) {
                return fixedBuffer;
            }

            if (requiredSize <= tempBuffer.length) {
                return tempBuffer;
            }

            resetTempBuffer(requiredSize);
            return tempBuffer;
        }

        void afterUsingBuffer() {
            if (tempBuffer.length > 0) {
                // 如果临时缓冲区5分钟未使用，释放它以避免内存泄漏
                if ((System.currentTimeMillis() - tempBirth) > 300_000) {
                    resetTempBuffer(0);
                }
            }
        }
    }
}
