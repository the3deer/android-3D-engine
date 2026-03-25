#include "ufbx.h"
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include "fbx_model.h"

#define LOG_TAG "ExampleLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

fbx_model_t* loadfbx_scene(const char *nativeFilePath){
    ufbx_load_opts opts = { 0 };
    ufbx_error error;

    LOGI("Loading FBX (C++): %s", nativeFilePath);

    ufbx_scene *scene = ufbx_load_file(nativeFilePath, &opts, &error);
    if (!scene) {
        LOGE("Failed to load: %s", error.description.data);
        return NULL;
    }

    // Use 'new' to ensure the fbx_model_t constructor (and std::vector) is called
    fbx_model_t *model = new fbx_model_t();
    model->scene = scene;

    LOGI("Successfully loaded scene with %zu nodes", scene->nodes.count);
    return model;
}

void freefbx_scene(fbx_model_t *model) {
    if (model) {
        // 'delete' will call the destructor, freeing the scene and all tracked buffers
        delete model;
    }
}

int get_a_random_number() {
    return rand();
}

}
