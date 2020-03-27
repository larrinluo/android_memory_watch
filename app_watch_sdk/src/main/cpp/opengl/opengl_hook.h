//
// Created by larrin luo on 2020/3/7.
//

#ifndef ANDROID_MEMORY_WATCH_OPENGL_HOOK_H
#define ANDROID_MEMORY_WATCH_OPENGL_HOOK_H

#include <GLES2/gl2.h>
#include <vector>
#include <string>

typedef void (*GLGentextures)(GLsizei n, GLuint *textures);
typedef void (*GLDeleteTextures) (GLsizei n, const GLuint *textures);

typedef int (*glGenTexturesHookMethod)(int n, GLuint *textures);
typedef int (*glDeleteTexturesHookMethod)(int n, GLuint *textures);

class OpenGLWatch {

    static std::vector<glGenTexturesHookMethod> glGenTextures_hook_list;
    static void glGenTextures_hook_entry(GLsizei n, GLuint *textures);
    static GLGentextures origin_glGenTextures;

    static std::vector<glDeleteTexturesHookMethod> glDeleteTextures_hook_list;
    static void glDeleteTextures_hook_entry(GLsizei n, GLuint *textures);
    static GLGentextures origin_glDeleteTextures;

    static std::string sTargetSo;
    static std::string sOutputFile;

public:

    static void installHooks();
    static bool checkHooks();

    static void registerHooks(const char *targetSo, const char *outputFile);
};

#endif //ANDROID_MEMORY_WATCH_OPENGL_HOOK_H
