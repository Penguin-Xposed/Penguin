#ifndef _MISC_H
#define _MISC_H

ssize_t fdgets(char *buf, size_t size, int fd);

int get_proc_name(int pid, char *name, size_t size);

char* getFileData(const char* path);

#endif // _MISC_H
