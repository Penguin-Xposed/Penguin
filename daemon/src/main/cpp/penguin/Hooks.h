#ifndef PENGUIN_HOOKS_H
#define PENGUIN_HOOKS_H

void findAndCall(JNIEnv *env, jclass sEntryClass, const char *methodName, const char *methodSig, ...);

bool addPathToEnv(const char* name, const char* path);

#endif //PENGUIN_HOOKS_H
