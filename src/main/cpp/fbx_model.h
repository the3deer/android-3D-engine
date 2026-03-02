#ifndef FBX_MODEL_H
#define FBX_MODEL_H

#include "ufbx.h"
#include <vector>

struct fbx_primitive_t {
    ufbx_node* node;
    int material_index;
};

struct fbx_model_t {
    ufbx_scene *scene;
    std::vector<fbx_primitive_t> primitives;
    std::vector<void*> allocated_buffers;

    fbx_model_t() : scene(nullptr) {}

    ~fbx_model_t() {
        if (scene) ufbx_free_scene(scene);
        for (void* ptr : allocated_buffers) {
            free(ptr);
        }
    }
};

#endif // FBX_MODEL_H
