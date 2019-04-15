#include <cstdlib>
#include <jni.h>
#include <dlfcn.h>
#include "Hooks.h"
#include "logging.h"
#include "penguin.h"
#include "android_build.h"

void findAndCall(JNIEnv *env, jclass sEntryClass, const char *methodName, const char *methodSig, ...) {
    if (!sEntryClass) {
        LOGE("cannot call method %s, entry class is null", methodName);
        return;
    }
    jmethodID mid = env->GetStaticMethodID(sEntryClass, methodName, methodSig);
    if (env->ExceptionOccurred()) {
        env->ExceptionClear();
        LOGE("method %s not found in entry class", methodName);
        mid = nullptr;
    }
    if (mid) {
        va_list args;
        va_start(args, methodSig);
        env->functions->CallStaticVoidMethodV(env, sEntryClass, mid, args);
        va_end(args);
    } else {
        LOGE("method %s id is null", methodName);
    }
}

/** Adds a path to the beginning of an environment variable. */
bool addPathToEnv(const char* name, const char* path) {
    char *oldPath = getenv(name);
    if (oldPath == nullptr) {
        setenv(name, path, 1);
    } else {
        char newPath[4096];
        int neededLength = snprintf(newPath, sizeof(newPath), "%s:%s", path, oldPath);
        if (neededLength >= (int) sizeof(newPath)) {
            LOGE("ERROR: %s would exceed characters", name);
            return false;
        }
        setenv(name, newPath, 1);
    }
    LOGE("Added Penguin.jar to CLASSPATH");
    return true;
}
