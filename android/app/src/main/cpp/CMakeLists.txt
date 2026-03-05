cmake_minimum_required(VERSION 3.22)
project(airshippiano_synth)

# Fetch Oboe (Google's low-latency audio library)
include(FetchContent)

FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG        1.9.0
)
FetchContent_MakeAvailable(oboe)

# Our native synth library
add_library(airshippiano_synth SHARED
    synth_engine.cpp
)

target_include_directories(airshippiano_synth PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(airshippiano_synth
    oboe
    android
    log
)

target_compile_options(airshippiano_synth PRIVATE -O2 -ffast-math)

# 16KB page size compatibility (required for Android 15+)
target_link_options(airshippiano_synth PRIVATE -Wl,-z,max-page-size=16384)
