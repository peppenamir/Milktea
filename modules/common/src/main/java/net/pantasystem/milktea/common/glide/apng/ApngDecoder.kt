package net.pantasystem.milktea.common.glide.apng

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.github.penfeizhou.animation.apng.decode.APNGDecoder
import com.github.penfeizhou.animation.apng.decode.APNGParser
import com.github.penfeizhou.animation.decode.FrameSeqDecoder
import com.github.penfeizhou.animation.io.ByteBufferReader
import com.github.penfeizhou.animation.loader.ByteBufferLoader
import com.github.penfeizhou.animation.loader.Loader
import java.nio.ByteBuffer


class ByteBufferApngDecoder : ResourceDecoder<ByteBuffer, FrameSeqDecoder<*, *>> {
    override fun decode(
        source: ByteBuffer,
        width: Int,
        height: Int,
        options: Options
    ): Resource<FrameSeqDecoder<*, *>> {
        return ApngDecoderDelegate.decode(source, width, height, options)
    }

    override fun handles(source: ByteBuffer, options: Options): Boolean {
        return ApngDecoderDelegate.handles(source, options)
    }
}

object ApngDecoderDelegate {
    @JvmStatic
    private val PNGHeaderBytes =
        listOf(0x89, 0x50, 0x4E, 0x47)
            .map { it.toByte() }
            .toByteArray()

    @JvmStatic
    private val IENDBytes =
        listOf(0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82)
            .map { it.toByte() }
            .toByteArray()

    @JvmStatic
    fun decode(
        source: ByteBuffer,
        width: Int,
        height: Int,
        options: Options
    ): Resource<FrameSeqDecoder<*, *>> {
        val sourceBytes = source.array()
        val iEndChunkPos = findIEndChunkPosition(sourceBytes)

        source.limit(iEndChunkPos + IENDBytes.size)

        val loader: Loader = object : ByteBufferLoader() {
            override fun getByteBuffer(): ByteBuffer {
                source.position(0)
                return source
            }
        }

        val decoder = APNGDecoder(loader, null)
        return FrameSeqDecoderResource(decoder, source.limit())
    }

    @JvmStatic
    fun handles(source: ByteBuffer, options: Options): Boolean {
        val sourceBytes = source.array()
        if (!checkHeaderBytes(sourceBytes)) {
            return false
        }

        if (findIEndChunkPosition(sourceBytes) < 0) {
            return false
        }

        return APNGParser.isAPNG(ByteBufferReader(source))
    }

    @JvmStatic
    private fun checkHeaderBytes(source: ByteArray): Boolean {
        val headerBytes = source.sliceArray(0..PNGHeaderBytes.size)
        return headerBytes.contentEquals(PNGHeaderBytes)
    }

    @JvmStatic
    private fun findIEndChunkPosition(source: ByteArray): Int {
        if (source.size < IENDBytes.size) {
            return -1
        }

        val startPos = source.size - IENDBytes.size
        for (idx in (0..startPos).reversed()) {
            val curByte = source[idx]
            if (curByte == IENDBytes[0]) {
                val hitTestTargetBytes = source.sliceArray(idx until (idx + IENDBytes.size))
                if (hitTestTargetBytes.contentEquals(IENDBytes)) {
                    return idx
                }
            }
        }

        return -1
    }
}

private class FrameSeqDecoderResource(
    private val decoder: FrameSeqDecoder<*, *>,
    private val size: Int
) : Resource<FrameSeqDecoder<*, *>> {
    override fun getResourceClass(): Class<FrameSeqDecoder<*, *>> {
        return FrameSeqDecoder::class.java
    }

    override fun get(): FrameSeqDecoder<*, *> {
        return decoder
    }

    override fun getSize(): Int {
        return size
    }

    override fun recycle() {
        decoder.stop()
    }
}