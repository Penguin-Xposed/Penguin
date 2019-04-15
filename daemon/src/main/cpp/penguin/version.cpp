#include "penguin.h"

#define VERSION "Penguin-Xposed v1.0"

extern "C" {
	const char* penguin_get_version(void) {
		return VERSION;
	}
}