cmake_minimum_required(VERSION 3.22)
# C is needed for OpenCV's bundled 3rdparty sources (zlib, cpufeatures).
project(wechatqr C CXX)

set(THIRD_PARTY ${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party)
set(OPENCV_SRC ${THIRD_PARTY}/opencv)
set(CONTRIB_MODULES ${THIRD_PARTY}/opencv_contrib/modules)
set(OPENCV_BIN ${CMAKE_CURRENT_BINARY_DIR}/opencv_build)

# wechat_qrcode pulls in its deps (core, imgproc, dnn); everything else stays off.
# All options must be cached before add_subdirectory. BUILD_PROTOBUF stays ON (dnn needs it).
set(BUILD_LIST "wechat_qrcode" CACHE STRING "" FORCE)
set(OPENCV_EXTRA_MODULES_PATH ${CONTRIB_MODULES} CACHE PATH "" FORCE)
foreach(opt BUILD_SHARED_LIBS BUILD_JAVA BUILD_ANDROID_PROJECTS BUILD_ANDROID_EXAMPLES
            BUILD_opencv_java_bindings_generator BUILD_opencv_python_bindings_generator
            BUILD_opencv_python3 BUILD_opencv_apps BUILD_TESTS BUILD_PERF_TESTS BUILD_EXAMPLES BUILD_DOCS
            WITH_OPENCL WITH_IPP WITH_ITT WITH_CAROTENE WITH_KLEIDICV WITH_ANDROID_MEDIANDK
            WITH_JPEG WITH_PNG WITH_TIFF WITH_WEBP WITH_OPENJPEG WITH_JASPER WITH_OPENEXR
            WITH_IMGCODEC_HDR WITH_IMGCODEC_SUNRASTER WITH_IMGCODEC_PXM WITH_IMGCODEC_PFM)
    set(${opt} OFF CACHE BOOL "" FORCE)
endforeach()

# Object files with debug info would multiply the build tree per variant x ABI; this code
# is never stepped through, so the debug variant builds it optimized without -g.
set(CMAKE_CXX_FLAGS_DEBUG "-O2" CACHE STRING "" FORCE)
set(CMAKE_C_FLAGS_DEBUG "-O2" CACHE STRING "" FORCE)

add_subdirectory(${OPENCV_SRC} ${OPENCV_BIN} EXCLUDE_FROM_ALL)

add_library(wechatqr_android SHARED wechatqr_jni.cpp)
# OpenCV's in-tree targets export no include dirs (all PRIVATE), so list them here. OpenCV
# writes its generated headers (cvconfig.h, opencv2/opencv_modules.hpp) to the *top-level*
# CMAKE_BINARY_DIR, not its own subdirectory. The wechat_qrcode src/ path is for the
# module-private SSDDetector header and is tied to the pinned opencv_contrib commit.
target_include_directories(wechatqr_android PRIVATE
    ${CMAKE_BINARY_DIR}
    ${OPENCV_SRC}/include
    ${OPENCV_SRC}/modules/core/include
    ${OPENCV_SRC}/modules/imgproc/include
    ${OPENCV_SRC}/modules/dnn/include
    ${CONTRIB_MODULES}/wechat_qrcode/include
    ${CONTRIB_MODULES}/wechat_qrcode/src)
target_link_libraries(wechatqr_android PRIVATE opencv_wechat_qrcode opencv_dnn opencv_imgproc opencv_core log)
# Hide everything coming from the static OpenCV archives so only the JNI entry points are
# exported; that lets --gc-sections drop the unused parts of dnn/imgproc/core.
target_link_options(wechatqr_android PRIVATE -Wl,--exclude-libs,ALL -Wl,--gc-sections)
