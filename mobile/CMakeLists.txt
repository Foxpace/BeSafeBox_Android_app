# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.


add_library( # Specifies the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/native-lib.cpp
        src/main/cpp/NativeModel.cpp
        src/main/cpp/calculations/Calculations.cpp
        src/main/cpp/calculations/Classifier.cpp
        src/main/cpp/datatypes/SensorOutput.cpp
        src/main/cpp/datatypes/DataCarrier.cpp
        src/main/cpp/calculations/nn/KerasSequentialModel.cpp
        src/main/cpp/calculations/nn/Parameters.cpp)

include_directories(src/main/cpp/include/)

find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.

        android-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log
        android)

target_link_libraries( # Specifies the target library.
        native-lib
        app-glue
        android
        # Links the log library to the target library.
        ${log-lib}
        ${android-lib})

add_library( app-glue
             STATIC
             ${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c )

# You need to link static libraries against your shared native library.
