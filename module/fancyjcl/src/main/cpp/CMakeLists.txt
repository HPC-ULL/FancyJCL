cmake_minimum_required(VERSION 3.0.0)

project("fancyjcl")

include (${ANDROID_GRADLE_NATIVE_BUNDLE_PLUGIN_MK})

add_library( fancyjcl SHARED fancyjcl.cpp )
find_library( log-lib log )

target_link_libraries( fancyjcl ${log-lib} ${ANDROID_GRADLE_NATIVE_MODULES} )
