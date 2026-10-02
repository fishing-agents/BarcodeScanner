// JNI bridge for WeChatQrNativeJni. Calls the public WeChatQRCode::detectAndDecode for decoded
// text + points, and the module-internal SSDDetector::forward for every detector candidate
// (decoded or not) — the public API drops undecodable candidates, which auto-zoom needs.
#include <jni.h>
#include <algorithm>
#include <cmath>
#include <string>
#include <vector>
#include <opencv2/core.hpp>
#include <opencv2/wechat_qrcode.hpp>
#include "detector/ssd_detector.hpp"

using cv::wechat_qrcode::SSDDetector;
using cv::wechat_qrcode::WeChatQRCode;

namespace {
cv::Ptr<WeChatQRCode> g_decoder;
cv::Ptr<SSDDetector> g_detector;

/** Converts 4x2 CV_32F point Mats into a Java float[][] of [x0,y0,x1,y1,x2,y2,x3,y3] rows. */
jobjectArray toJavaQuads(JNIEnv* env, const std::vector<cv::Mat>& points) {
    jobjectArray quads = env->NewObjectArray(static_cast<jsize>(points.size()), env->FindClass("[F"), nullptr);
    for (size_t i = 0; i < points.size(); ++i) {
        cv::Mat p;
        points[i].reshape(1, 4).convertTo(p, CV_32F);
        float row[8];
        for (int j = 0; j < 4; ++j) {
            row[j * 2] = p.at<float>(j, 0);
            row[j * 2 + 1] = p.at<float>(j, 1);
        }
        jfloatArray quad = env->NewFloatArray(8);
        env->SetFloatArrayRegion(quad, 0, 8, row);
        env->SetObjectArrayElement(quads, static_cast<jsize>(i), quad);
        env->DeleteLocalRef(quad);
    }
    return quads;
}

std::string modelPath(const char* dir, const char* name) {
    return dir ? std::string(dir) + "/" + name : std::string();
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_atharok_barcodescanner_wechatqr_WeChatQrNativeJni_jniInit(JNIEnv* env, jobject, jstring modelDir) {
    const char* dir = modelDir ? env->GetStringUTFChars(modelDir, nullptr) : nullptr;
    const std::string detectProto = modelPath(dir, "detect.prototxt");
    const std::string detectModel = modelPath(dir, "detect.caffemodel");
    const std::string srProto = modelPath(dir, "sr.prototxt");
    const std::string srModel = modelPath(dir, "sr.caffemodel");
    if (dir) env->ReleaseStringUTFChars(modelDir, dir);

    try {
        g_decoder = cv::makePtr<WeChatQRCode>(detectProto, detectModel, srProto, srModel);
        g_detector.reset();
        if (!detectProto.empty()) {
            auto detector = cv::makePtr<SSDDetector>();
            if (detector->init(detectProto, detectModel) == 0) g_detector = detector;
        }
        return JNI_TRUE;
    } catch (const cv::Exception&) {
        g_decoder.reset();
        g_detector.reset();
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_atharok_barcodescanner_wechatqr_WeChatQrNativeJni_jniDetectAndDecode(
    JNIEnv* env, jobject, jobject yBuffer, jint width, jint height, jint rowStride) {
    auto* data = static_cast<uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    cv::Mat gray(height, width, CV_8UC1, data, static_cast<size_t>(rowStride));

    std::vector<std::string> texts;
    std::vector<cv::Mat> decoded;
    std::vector<cv::Mat> candidates;
    if (g_decoder) texts = g_decoder->detectAndDecode(gray, decoded);
    // detectAndDecode already ran the CNN internally; repeat it only to recover undecodable
    // candidate boxes, which auto-zoom needs solely when nothing decoded.
    if (g_detector && texts.empty()) {
        // Same input sizing as WeChatQRCode::Impl::applyDetector: ~400x400 area, aspect preserved.
        const float scale = std::min(1.f, std::sqrt(400.f * 400.f / (float(width) * float(height))));
        candidates = g_detector->forward(gray, int(width * scale), int(height * scale));
    }

    jobjectArray textArray = env->NewObjectArray(static_cast<jsize>(texts.size()), env->FindClass("java/lang/String"), nullptr);
    for (size_t i = 0; i < texts.size(); ++i) {
        jstring s = env->NewStringUTF(texts[i].c_str());
        env->SetObjectArrayElement(textArray, static_cast<jsize>(i), s);
        env->DeleteLocalRef(s);
    }

    jclass resultClass = env->FindClass("com/atharok/barcodescanner/wechatqr/WeChatQrNativeResult");
    jmethodID ctor = env->GetMethodID(resultClass, "<init>", "([Ljava/lang/String;[[F[[F)V");
    return env->NewObject(resultClass, ctor, textArray, toJavaQuads(env, decoded), toJavaQuads(env, candidates));
}
