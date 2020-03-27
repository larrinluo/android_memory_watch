//
// Created by larrin luo on 2020/3/7.
//

#include "opengl_hook.h"
#include "xhook.h"

std::string OpenGLWatch::sTargetSo;
std::string OpenGLWatch::sOutputFile;

std::vector<glGenTexturesHookMethod> OpenGLWatch::glGenTextures_hook_list;
GLGentextures OpenGLWatch::origin_glGenTextures = NULL;

std::vector<glDeleteTexturesHookMethod> OpenGLWatch::glDeleteTextures_hook_list;
GLGentextures OpenGLWatch::origin_glDeleteTextures = NULL;

void OpenGLWatch::registerHooks(const char *targetSo, const char *outputFile) {
    sTargetSo = targetSo;
    sOutputFile = outputFile;
}

void OpenGLWatch::glGenTextures_hook_entry(GLsizei n, GLuint *textures) {
    origin_glGenTextures(n, textures);
    for (auto hook: glGenTextures_hook_list) {
        if (hook(n, textures)) {
            return;
        }
    }
}

void OpenGLWatch::glDeleteTextures_hook_entry(GLsizei n, GLuint *textures) {
    origin_glDeleteTextures(n, textures);
    for (auto hook: glDeleteTextures_hook_list) {
        if (hook(n, textures)) {
            return;
        }
    }
}

void OpenGLWatch::installHooks() {
    xhook_register(sTargetSo.c_str(), "glGenTextures", (void *) OpenGLWatch::glGenTextures_hook_entry, (void **) &OpenGLWatch::origin_glGenTextures);
    xhook_register(sTargetSo.c_str(), "glDeleteTextures", (void *) OpenGLWatch::glDeleteTextures_hook_entry, (void **) &OpenGLWatch::origin_glDeleteTextures);
}

bool OpenGLWatch::checkHooks() {

}