#ifndef FBX_MODEL_H
#define FBX_MODEL_H

#include "ufbx.h"
#include <vector>

// Using C++ to manage the list of allocated buffers
struct fbx_model_t {
    ufbx_scene *scene;
    std::vector<void*> allocated_buffers;

    ~fbx_model_t() {
        if (scene) ufbx_free_scene(scene);
        for (void* ptr : allocated_buffers) {
            free(ptr);
        }
    }
};

#endif // FBX_MODEL_H
