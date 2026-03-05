# Keep JNI native methods
-keepclasseswithmembernames class com.windupairships.airshippiano.audio.SynthEngine {
    native <methods>;
}

# Keep data classes used by the synth engine
-keep class com.windupairships.airshippiano.audio.SynthEngine { *; }
