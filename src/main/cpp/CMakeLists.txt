# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("the3deer_engine")

# Define UFBX_REAL_IS_FLOAT to use floats instead of doubles for coordinates.
add_definitions(-DUFBX_REAL_IS_FLOAT)

# Creates and names a library.
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-adapter.cpp
        native-lib.cpp
        ufbx.c
        )

# Specifies libraries CMake should link to your target library.
target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)