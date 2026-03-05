#include <jni.h>
#include <android/log.h>
#include <mutex>
#include <string>

#define TSF_IMPLEMENTATION
#include "tsf.h"

#include <oboe/Oboe.h>

#define LOG_TAG "AirshipPianoSynth"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct SynthEngine : public oboe::AudioStreamDataCallback {
    tsf* soundFont = nullptr;
    oboe::ManagedStream stream;
    std::mutex mutex;
    int sampleRate;
    float gain = 0.8f;

    explicit SynthEngine(int sr) : sampleRate(sr) {}

    ~SynthEngine() {
        stopAudio();
        if (soundFont) {
            tsf_close(soundFont);
            soundFont = nullptr;
        }
    }

    bool loadSoundFont(const char* path) {
        std::lock_guard<std::mutex> lock(mutex);
        if (soundFont) {
            tsf_close(soundFont);
            soundFont = nullptr;
        }
        soundFont = tsf_load_filename(path);
        if (!soundFont) {
            LOGE("Failed to load SoundFont: %s", path);
            return false;
        }
        tsf_set_output(soundFont, TSF_STEREO_INTERLEAVED, sampleRate, gain);
        // Set all channels to piano preset 0
        for (int ch = 0; ch < 16; ch++) {
            if (ch == 9) continue; // skip drum channel
            tsf_channel_set_presetnumber(soundFont, ch, 0, 0);
        }
        LOGI("SoundFont loaded: %s", path);
        return true;
    }

    bool startAudio() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output)
               ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
               ->setSharingMode(oboe::SharingMode::Exclusive)
               ->setFormat(oboe::AudioFormat::Float)
               ->setChannelCount(2)
               ->setSampleRate(sampleRate)
               ->setFramesPerDataCallback(256)
               ->setDataCallback(this);

        oboe::Result result = builder.openManagedStream(stream);
        if (result != oboe::Result::OK) {
            LOGE("Failed to open audio stream: %s", oboe::convertToText(result));
            return false;
        }

        // Update sample rate if stream chose a different one
        if (stream->getSampleRate() != sampleRate) {
            sampleRate = stream->getSampleRate();
            std::lock_guard<std::mutex> lock(mutex);
            if (soundFont) {
                tsf_set_output(soundFont, TSF_STEREO_INTERLEAVED, sampleRate, gain);
            }
        }

        result = stream->requestStart();
        if (result != oboe::Result::OK) {
            LOGE("Failed to start audio stream: %s", oboe::convertToText(result));
            return false;
        }

        LOGI("Audio stream started at %d Hz", sampleRate);
        return true;
    }

    void stopAudio() {
        if (stream) {
            stream->requestStop();
            stream->close();
            stream.reset();
        }
    }

    void noteOn(int channel, int note, int velocity) {
        std::lock_guard<std::mutex> lock(mutex);
        if (soundFont) {
            tsf_channel_note_on(soundFont, channel, note, velocity / 127.0f);
        }
    }

    void noteOff(int channel, int note) {
        std::lock_guard<std::mutex> lock(mutex);
        if (soundFont) {
            tsf_channel_note_off(soundFont, channel, note);
        }
    }

    void controlChange(int channel, int controller, int value) {
        std::lock_guard<std::mutex> lock(mutex);
        if (soundFont) {
            tsf_channel_midi_control(soundFont, channel, controller, value);
        }
    }

    void setGain(float g) {
        std::lock_guard<std::mutex> lock(mutex);
        gain = g;
        if (soundFont) {
            tsf_set_output(soundFont, TSF_STEREO_INTERLEAVED, sampleRate, gain);
        }
    }

    // Oboe audio callback - renders audio from TinySoundFont
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames
    ) override {
        auto* output = static_cast<float*>(audioData);

        std::lock_guard<std::mutex> lock(mutex);
        if (soundFont) {
            tsf_render_float(soundFont, output, numFrames, 0);
        } else {
            // Silence if no soundfont loaded
            memset(output, 0, numFrames * 2 * sizeof(float));
        }
        return oboe::DataCallbackResult::Continue;
    }
};

// JNI exports
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeCreate(
    JNIEnv*, jobject, jint sampleRate
) {
    auto* engine = new SynthEngine(sampleRate);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeDestroy(
    JNIEnv*, jobject, jlong handle
) {
    delete reinterpret_cast<SynthEngine*>(handle);
}

JNIEXPORT jboolean JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeLoadSoundFont(
    JNIEnv* env, jobject, jlong handle, jstring path
) {
    auto* engine = reinterpret_cast<SynthEngine*>(handle);
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    bool result = engine->loadSoundFont(pathStr);
    env->ReleaseStringUTFChars(path, pathStr);
    return result;
}

JNIEXPORT void JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeNoteOn(
    JNIEnv*, jobject, jlong handle, jint channel, jint note, jint velocity
) {
    reinterpret_cast<SynthEngine*>(handle)->noteOn(channel, note, velocity);
}

JNIEXPORT void JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeNoteOff(
    JNIEnv*, jobject, jlong handle, jint channel, jint note
) {
    reinterpret_cast<SynthEngine*>(handle)->noteOff(channel, note);
}

JNIEXPORT void JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeControlChange(
    JNIEnv*, jobject, jlong handle, jint channel, jint controller, jint value
) {
    reinterpret_cast<SynthEngine*>(handle)->controlChange(channel, controller, value);
}

JNIEXPORT void JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeSetGain(
    JNIEnv*, jobject, jlong handle, jfloat gain
) {
    reinterpret_cast<SynthEngine*>(handle)->setGain(gain);
}

JNIEXPORT jboolean JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeStartAudio(
    JNIEnv*, jobject, jlong handle
) {
    return reinterpret_cast<SynthEngine*>(handle)->startAudio();
}

JNIEXPORT void JNICALL
Java_com_windupairships_airshippiano_audio_SynthEngine_nativeStopAudio(
    JNIEnv*, jobject, jlong handle
) {
    reinterpret_cast<SynthEngine*>(handle)->stopAudio();
}

} // extern "C"
