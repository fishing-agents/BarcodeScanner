# Constructed from JNI (wechatqr_jni.cpp: FindClass + "<init>"), so R8 sees no Java call site.
-keep class com.atharok.barcodescanner.wechatqr.WeChatQrNativeResult {
    <init>(java.lang.String[], float[][], float[][]);
}
