#include <dlfcn.h>
#include <unistd.h>
#include <xhook.h>
#include <cstring>
#include <string>
#include "penguin.h"
#include "misc.h"
#include "logging.h"
#include "Hooks.h"
#include "android_build.h"
#include "fd_utils-inl.h"
#include <android/art/native_on_load.h>

static int api_level = 0;
static bool isLoaded = false;

#if PLATFORM_SDK_VERSION >= 21
static FileDescriptorTable* gClosedFdTable = NULL;
#endif

void XposedBridge_closeFilesBeforeForkNative(JNIEnv*, jclass) {
#if PLATFORM_SDK_VERSION >= 21
    gClosedFdTable = FileDescriptorTable::Create();
#endif
}

void XposedBridge_reopenFilesAfterForkNative(JNIEnv*, jclass) {
#if PLATFORM_SDK_VERSION >= 21
    gClosedFdTable->Reopen();
    delete gClosedFdTable;
    gClosedFdTable = NULL;
#endif
}

jstring Penguin_getFileData(JNIEnv* env, jclass, jstring path) {
    const char* _path = env->GetStringUTFChars(path, nullptr);
    char * data = getFileData(_path);
    env->ReleaseStringUTFChars(path, _path);
    return env->NewStringUTF(data);
}

jstring Penguin_getVersion(JNIEnv* env, jclass){
    return env->NewStringUTF(penguin_get_version());
}

static JNINativeMethod jniMethods[] = {
        NATIVE_METHOD(XposedBridge, closeFilesBeforeForkNative, "()V"),
        NATIVE_METHOD(XposedBridge, reopenFilesAfterForkNative, "()V"),
        NATIVE_METHOD(Penguin, getFileData, "(Ljava/lang/String;)Ljava/lang/String;"),
        NATIVE_METHOD(Penguin, getVersion, "()Ljava/lang/String;"),
};

static void (*orig_AndroidRuntime_start)(void* object, const char* classname, void* options, bool zygote);
static void AndroidRuntime_start(void* object, const char* classname, void* options, bool zygote){
    LOGI("AndroidRuntime::start called");
    isLoaded = addPathToEnv("CLASSPATH", PENGUIN_JAR);
    LOGI("CLASSPATH: %s", getenv("CLASSPATH"));
    orig_AndroidRuntime_start(object, (isLoaded ? (zygote ? PENGUIN_CLASS : PENGUIN_CLASS_RUNTIME) : classname), options, zygote);
}

static int (*orig_AndroidRuntime_startVm)(void* object, JavaVM** pJavaVM, JNIEnv** pEnv, bool zygote);
static int AndroidRuntime_startVm(void* object, JavaVM** pJavaVM, JNIEnv** pEnv, bool zygote){
    LOGI("AndroidRuntime::startVm called!");
    int i = orig_AndroidRuntime_startVm(object, pJavaVM, pEnv, zygote);
    JNIEnv *env = *pEnv;
    if(env != nullptr){
        jclass helperClass = env->FindClass(PENGUIN_HELPER_CLASS);
        if(helperClass != nullptr){
            env->RegisterNatives(helperClass, jniMethods, NELEM(jniMethods));
        }
        jclass whaleClass = env->FindClass(WHALE_CLASS_NAME);
        if(whaleClass != nullptr){
            Whale_OnLoad(env, whaleClass);
        }
    }
    return i;
}

static int (*orig_jniRegisterNativeMethods)(JNIEnv *env, const char *className, const JNINativeMethod *methods, int numMethods);
static int jniRegisterNativeMethods(JNIEnv *env, const char *className, const JNINativeMethod *methods, int numMethods) {
    //LOGI("jniRegisterNativeMethods %s", className);
    return orig_jniRegisterNativeMethods(env, className, methods, numMethods);
}

static int (*orig_system_property_get)(const char *key, char *value);
static int system_property_get(const char *key, char *value){
    int res = orig_system_property_get(key, value);
    if (key) {
        /*if (strcmp(PROP_KEY_COMPILER_FILTER, key) == 0) {
            strcpy(value, PROP_VALUE_COMPILER_FILTER);
            LOGI("system_property_get: %s -> %s", key, value);
        } */
        if (strcmp(PROP_KEY_COMPILER_FLAGS, key) == 0) {
            strcpy(value, PROP_VALUE_COMPILER_FLAGS);
            LOGI("system_property_get: %s -> %s", key, value);
        }
        if (api_level == ANDROID_O_MR1) {
            // https://android.googlesource.com/platform/art/+/f5516d38736fb97bfd0435ad03bbab17ddabbe4e
            // Android 8.1 add a fatal check for debugging (removed in Android 9.0),
            // which will be triggered by EdXposed in cases where target method is hooked
            // (native flag set) after it has been called several times(getCounter() return positive number)
            if (strcmp(PROP_KEY_USEJITPROFILES, key) == 0) {
                strcpy(value, "false");
            } else if (strcmp(PROP_KEY_PM_BG_DEXOPT, key) == 0) {
                // use speed as bg-dexopt filter since that speed-profile won't work after
                // jit profiles is disabled
                strcpy(value, PROP_VALUE_PM_BG_DEXOPT);
            }
            LOGD("system_property_get: %s -> %s", key, value);
        }
    }
    return res;
}

static std::string (*orig_abase_GetProperty)(const std::string &key, const std::string &default_value);
static std::string abase_GetProperty(const std::string &key, const std::string &default_value){
    std::string res = orig_abase_GetProperty(key, default_value);
    /*if (strcmp(PROP_KEY_COMPILER_FILTER, key.c_str()) == 0) {
        res = PROP_VALUE_COMPILER_FILTER;
        LOGI("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
    } */
    if (strcmp(PROP_KEY_COMPILER_FLAGS, key.c_str()) == 0) {
        res = PROP_VALUE_COMPILER_FLAGS;
        LOGI("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
    }
    if (api_level == ANDROID_O_MR1) {
        // see __system_property_get hook above for explanations
        if (strcmp(PROP_KEY_USEJITPROFILES, key.c_str()) == 0) {
            res = "false";
        } else if (strcmp(PROP_KEY_PM_BG_DEXOPT, key.c_str()) == 0) {
            res = PROP_VALUE_PM_BG_DEXOPT;
        }
        LOGD("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
    }
    return res;
}

__attribute__((constructor))
void penguin_daemon_constructor() {
    char process[64];
    get_proc_name(getpid(), process, 63);
    if (strncmp(ZYGOTE_NAME, process, strlen(ZYGOTE_NAME)) != 0 &&
        strncmp(APP_PROCESS_NAME, process, strlen(APP_PROCESS_NAME)) != 0) {
        return;
    }

    api_level = GetAndroidApiLevel();

    LOGI("Penguin Daemon loaded on PID(%d), UID(%d), Process(%s)", getpid(), getuid(), process);

    if (xhook_register(".*", "_ZN7android14AndroidRuntime5startEPKcRKNS_6VectorINS_7String8EEEb",
            (void*) AndroidRuntime_start, (void **) &orig_AndroidRuntime_start) != 0){
        LOGE("failed to register hook for AndroidRuntime::start.");
    }

    if (xhook_register(".*", "_ZN7android14AndroidRuntime7startVmEPP7_JavaVMPP7_JNIEnvb",
                       (void*) AndroidRuntime_startVm, (void **) &orig_AndroidRuntime_startVm) != 0){
        LOGE("failed to register hook for AndroidRuntime::startVm.");
    }

    if (xhook_register(".*", "jniRegisterNativeMethods",
            (void*) jniRegisterNativeMethods, (void **) &orig_jniRegisterNativeMethods) != 0){
        LOGE("failed to register hook for jniRegisterNativeMethods.");
    }

    if (xhook_register(".*", "__system_property_get",
                       (void*) system_property_get, (void **) &orig_system_property_get) != 0){
        LOGE("failed to register hook for system_property_get.");
    }

    if (api_level >= ANDROID_P) {
        if (xhook_register(".*", "_ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_",
                           (void*) abase_GetProperty, (void **) &orig_abase_GetProperty) != 0){
            LOGE("failed to register hook for abase_GetProperty.");
        }
    }

    if (xhook_refresh(0) == 0) {
        xhook_clear();
        LOGI("Penguin hook installed");
    } else {
        LOGE("Failed to refresh hook");
    }
}