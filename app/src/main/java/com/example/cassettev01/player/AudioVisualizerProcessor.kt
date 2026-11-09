package com.example.cassettev01.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// The deck reads the latest 48 frequency-band values from here.
object AudioVisualizerState {
    const val BAR_COUNT = 48

    private val _levels = MutableStateFlow(List(BAR_COUNT) { 0f })
    val levels: StateFlow<List<Float>> = _levels

    private val _hasLiveSamples = MutableStateFlow(false)
    val hasLiveSamples: StateFlow<Boolean> = _hasLiveSamples

    internal fun publish(values: FloatArray) {
        _levels.value = values.toList()
        _hasLiveSamples.value = true
    }

    // Clear the old bars when playback pauses or the user seeks.
    fun clearToSilence() {
        _levels.value = List(BAR_COUNT) { 0f }
        _hasLiveSamples.value = false
    }

    internal fun clear() = clearToSilence()
}



// Read the decoded audio for the visualizer, then pass it on unchanged.
@OptIn(UnstableApi::class)
class CassetteVisualizerAudioProcessor : BaseAudioProcessor() {
    private companion object {
        const val FFT_SIZE = 1024
        const val HALF_FFT = FFT_SIZE / 2
        const val MIN_FREQUENCY_HZ = 38.0
        const val MAX_FREQUENCY_HZ = 17_000.0
    }

    private var encoding = C.ENCODING_INVALID
    private var bytesPerSample = 0
    private var channelCount = 0
    private var sampleRate = 0

    private val sampleFrame = FloatArray(FFT_SIZE)
    private var sampleWriteIndex = 0
    private val real = FloatArray(FFT_SIZE)
    private val imaginary = FloatArray(FFT_SIZE)
    private val smoothedLevels = FloatArray(AudioVisualizerState.BAR_COUNT)
    private val publishedLevels = FloatArray(AudioVisualizerState.BAR_COUNT)
    private val bandStartBins = IntArray(AudioVisualizerState.BAR_COUNT)
    private val bandEndBins = IntArray(AudioVisualizerState.BAR_COUNT)
    private var analysisFrameCount = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        encoding = inputAudioFormat.encoding
        bytesPerSample = when (encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> 0
        }
        channelCount = inputAudioFormat.channelCount
        sampleRate = inputAudioFormat.sampleRate
        resetAnalysis()
        configureFrequencyBands()
        AudioVisualizerState.clearToSilence()

        return if (bytesPerSample > 0 && channelCount > 0 && sampleRate > 0) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val byteCount = inputBuffer.remaining()
        if (byteCount <= 0) return

        analyse(inputBuffer)

        // We only read the audio here; the actual PCM goes through unchanged.
        val output = replaceOutputBuffer(byteCount)
        output.put(inputBuffer)
        output.flip()
    }

    private fun analyse(input: ByteBuffer) {
        if (bytesPerSample == 0 || channelCount <= 0) return
        val view = input.duplicate().order(ByteOrder.nativeOrder())
        val totalSamples = view.remaining() / bytesPerSample
        val frameCount = totalSamples / channelCount
        val base = view.position()

        for (frame in 0 until frameCount) {
            var mono = 0f
            for (channel in 0 until channelCount) {
                val sampleNumber = frame * channelCount + channel
                val byteIndex = base + sampleNumber * bytesPerSample
                val sample = when (encoding) {
                    C.ENCODING_PCM_8BIT -> ((view.get(byteIndex).toInt() and 0xFF) - 128) / 128f
                    C.ENCODING_PCM_16BIT -> view.getShort(byteIndex).toFloat() / 32768f
                    C.ENCODING_PCM_FLOAT -> view.getFloat(byteIndex).coerceIn(-1f, 1f)
                    else -> 0f
                }
                mono += sample
            }

            sampleFrame[sampleWriteIndex++] = mono / channelCount
            if (sampleWriteIndex == FFT_SIZE) {
                calculateSpectrum()
                sampleWriteIndex = 0
            }
        }
    }

    private fun configureFrequencyBands() {
        if (sampleRate <= 0) return
        val nyquist = sampleRate / 2.0
        val maxFrequency = min(MAX_FREQUENCY_HZ, nyquist * 0.94).coerceAtLeast(MIN_FREQUENCY_HZ * 1.1)
        val ratio = maxFrequency / MIN_FREQUENCY_HZ
        for (band in 0 until AudioVisualizerState.BAR_COUNT) {
            val lowerHz = MIN_FREQUENCY_HZ * ratio.pow(band.toDouble() / AudioVisualizerState.BAR_COUNT)
            val upperHz = MIN_FREQUENCY_HZ * ratio.pow((band + 1.0) / AudioVisualizerState.BAR_COUNT)
            bandStartBins[band] = floor(lowerHz * FFT_SIZE / sampleRate).toInt().coerceIn(1, HALF_FFT - 1)
            bandEndBins[band] = ceil(upperHz * FFT_SIZE / sampleRate).toInt().coerceIn(
                bandStartBins[band] + 1,
                HALF_FFT
            )
        }
    }

    private fun calculateSpectrum() {
        // Clean up the frame a bit before running the FFT.
        var mean = 0f
        for (sample in sampleFrame) mean += sample
        mean /= FFT_SIZE

        for (index in 0 until FFT_SIZE) {
            val hann = 0.5f - 0.5f * cos((2.0 * Math.PI * index / (FFT_SIZE - 1)).toFloat())
            real[index] = (sampleFrame[index] - mean) * hann
            imaginary[index] = 0f
        }

        fftInPlace()

        for (band in 0 until AudioVisualizerState.BAR_COUNT) {
            var peakMagnitude = 0f
            val start = bandStartBins[band]
            val end = bandEndBins[band]
            for (bin in start until end) {
                val magnitude = sqrt(real[bin] * real[bin] + imaginary[bin] * imaginary[bin]) * (4f / FFT_SIZE)
                if (magnitude > peakMagnitude) peakMagnitude = magnitude
            }

            // This keeps quieter parts visible without flattening the louder ones.
            var target = sqrt(peakMagnitude * 1.55f).coerceIn(0f, 1f)
            if (target < 0.035f) target = 0f
            val previous = smoothedLevels[band]
            val smoothing = if (target > previous) 0.68f else 0.22f
            smoothedLevels[band] = (previous + (target - previous) * smoothing).coerceIn(0f, 1f)
        }

        // Skip every fourth frame to keep updates around 30–35 FPS.
        analysisFrameCount++
        if (analysisFrameCount % 4 != 0) {
            smoothedLevels.copyInto(publishedLevels)
            AudioVisualizerState.publish(publishedLevels)
        }
    }

    // Standard in-place Cooley-Tukey FFT.
    private fun fftInPlace() {
        var j = 0
        for (i in 1 until FFT_SIZE) {
            var bit = FFT_SIZE shr 1
            while ((j and bit) != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val realValue = real[i]
                real[i] = real[j]
                real[j] = realValue
                val imaginaryValue = imaginary[i]
                imaginary[i] = imaginary[j]
                imaginary[j] = imaginaryValue
            }
        }

        var length = 2
        while (length <= FFT_SIZE) {
            val angle = -2.0 * Math.PI / length
            val stepReal = cos(angle).toFloat()
            val stepImaginary = sin(angle).toFloat()
            val half = length / 2

            for (start in 0 until FFT_SIZE step length) {
                var twiddleReal = 1f
                var twiddleImaginary = 0f
                for (offset in 0 until half) {
                    val evenIndex = start + offset
                    val oddIndex = evenIndex + half
                    val oddReal = real[oddIndex] * twiddleReal - imaginary[oddIndex] * twiddleImaginary
                    val oddImaginary = real[oddIndex] * twiddleImaginary + imaginary[oddIndex] * twiddleReal
                    val evenReal = real[evenIndex]
                    val evenImaginary = imaginary[evenIndex]

                    real[evenIndex] = evenReal + oddReal
                    imaginary[evenIndex] = evenImaginary + oddImaginary
                    real[oddIndex] = evenReal - oddReal
                    imaginary[oddIndex] = evenImaginary - oddImaginary

                    val nextTwiddleReal = twiddleReal * stepReal - twiddleImaginary * stepImaginary
                    twiddleImaginary = twiddleReal * stepImaginary + twiddleImaginary * stepReal
                    twiddleReal = nextTwiddleReal
                }
            }
            length = length shl 1
        }
    }

    private fun resetAnalysis() {
        sampleFrame.fill(0f)
        sampleWriteIndex = 0
        real.fill(0f)
        imaginary.fill(0f)
        smoothedLevels.fill(0f)
        publishedLevels.fill(0f)
        analysisFrameCount = 0
    }

    override fun onFlush() {
        super.onFlush()
        resetAnalysis()
        AudioVisualizerState.clearToSilence()
    }

    override fun onReset() {
        encoding = C.ENCODING_INVALID
        bytesPerSample = 0
        channelCount = 0
        sampleRate = 0
        resetAnalysis()
        AudioVisualizerState.clearToSilence()
    }
}
