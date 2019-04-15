#ifndef PENGUIN_H
#define PENGUIN_H

#include <jni.h>
#include <xhook.h>

#define NATIVE_METHOD(className, functionName, signature) \
    { #functionName, signature, reinterpret_cast<void*>(className ## _ ## functionName) }
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#define MANAGER_PKG_NAME "in.kmods.penguin"
#define CONFIG_DIR "/data/data/" MANAGER_PKG_NAME "/app_config"
#define PENGUIN_JAR "/system/framework/penguin.jar"

#define PENGUIN_CLASS "in.kmods.penguin.PenguinInit"
#define PENGUIN_CLASS_RUNTIME "in.kmods.penguin.PenguinInit$NoZygote"

#define PENGUIN_HELPER_CLASS "in/kmods/penguin/PenguinHelper"
#define WHALE_CLASS_NAME "com/lody/whale/WhaleRuntime"

#define PROP_KEY_COMPILER_FILTER "dalvik.vm.dex2oat-filter"
#define PROP_KEY_COMPILER_FLAGS "dalvik.vm.dex2oat-flags"
#define PROP_KEY_USEJITPROFILES "dalvik.vm.usejitprofiles"
#define PROP_KEY_PM_BG_DEXOPT "pm.dexopt.bg-dexopt"
#define PROP_VALUE_COMPILER_FILTER "quicken"
#define PROP_VALUE_COMPILER_FLAGS "--inline-max-code-units=0"
#define PROP_VALUE_PM_BG_DEXOPT "speed"

#ifdef __LP64__
#define ZYGOTE_NAME "zygote64"
#define APP_PROCESS_NAME "/system/bin/app_process64"
#define APP_PROCESS_NAME2 "app_process64"
#define ANDROID_RUNTIME_LIBRARY "/system/lib64/libandroid_runtime.so"
#define LibArtPath "/system/lib64/libart.so"
#define PENGUIN_LIBRARY "/system/lib64/libwhale.so"
#else
#define ZYGOTE_NAME "zygote"
#define APP_PROCESS_NAME "/system/bin/app_process"
#define APP_PROCESS_NAME2 "app_process"
#define ANDROID_RUNTIME_LIBRARY "/system/lib/libandroid_runtime.so"
#define LibArtPath "/system/lib/libart.so"
#define PENGUIN_LIBRARY "/system/lib/libwhale.so"
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define EXPORT __attribute__((visibility("default"))) __attribute__((used))

const char* penguin_get_version(void) EXPORT;

#ifdef __cplusplus
}
#endif

#endif // PENGUIN_H
