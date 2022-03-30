cmake_minimum_required(VERSION 3.0.0)

project("fancierfrontend")

include (${ANDROID_GRADLE_NATIVE_BUNDLE_PLUGIN_MK})

add_library( fancierfrontend SHARED fancierfrontend.cpp )
find_library( log-lib log )

target_link_libraries( fancierfrontend ${log-lib} ${ANDROID_GRADLE_NATIVE_MODULES} )
