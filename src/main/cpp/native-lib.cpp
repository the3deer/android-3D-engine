#include <jni.h>
#include <string>
#include <android/log.h>
#include <cinttypes>
#include <vector>
#include "ufbx.h"
#include "fbx_model.h"

#define LOG_TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Manual implementation of vec3 transformation (m * v)
static ufbx_vec3 transform_pos(const ufbx_matrix *m, ufbx_vec3 v) {
    ufbx_vec3 r;
    r.x = m->m00 * v.x + m->m01 * v.y + m->m02 * v.z + m->m03;
    r.y = m->m10 * v.x + m->m11 * v.y + m->m12 * v.z + m->m13;
    r.z = m->m20 * v.x + m->m21 * v.y + m->m22 * v.z + m->m23;
    return r;
}

// Manual implementation of normal transformation (m * v, ignoring translation)
static ufbx_vec3 transform_norm(const ufbx_matrix *m, ufbx_vec3 v) {
    ufbx_vec3 r;
    r.x = m->m00 * v.x + m->m01 * v.y + m->m02 * v.z;
    r.y = m->m10 * v.x + m->m11 * v.y + m->m12 * v.z;
    r.z = m->m20 * v.x + m->m21 * v.y + m->m22 * v.z;
    return r;
}

static ufbx_node* find_mesh_node(fbx_model_t* model, int index) {
    int current = 0;
    for (size_t i = 0; i < model->scene->nodes.count; i++) {
        ufbx_node* node = model->scene->nodes.data[i];
        if (node->mesh) {
            if (current == index) return node;
            current++;
        }
    }
    return nullptr;
}

struct jni_stream_context {
    JNIEnv* env;
    jobject is;
    jmethodID read_method;
    jbyteArray buffer;
    size_t buffer_size;
};

static size_t jni_read_fn(void* user, void* data, size_t size) {
    jni_stream_context* ctx = (jni_stream_context*)user;
    size_t to_read = size < ctx->buffer_size ? size : ctx->buffer_size;
    jint bytes_read = ctx->env->CallIntMethod(ctx->is, ctx->read_method, ctx->buffer, 0, (jint)to_read);
    if (bytes_read > 0) {
        ctx->env->GetByteArrayRegion(ctx->buffer, 0, bytes_read, (jbyte*)data);
        return (size_t)bytes_read;
    }
    return (bytes_read == -1) ? 0 : 0;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxParseModel(
        JNIEnv* env, jobject, jstring filePath) {
    const char *nativeFilePath = env->GetStringUTFChars(filePath, 0);

    ufbx_load_opts opts = { 0 };
    opts.target_axes = ufbx_axes_right_handed_y_up;
    opts.target_unit_meters = 1.0f;

    ufbx_error error;
    ufbx_scene *scene = ufbx_load_file(nativeFilePath, &opts, &error);
    env->ReleaseStringUTFChars(filePath, nativeFilePath);

    if (!scene) {
        LOGE("Failed to load file: %s", error.description.data);
        return 0;
    }

    fbx_model_t *model = new fbx_model_t();
    model->scene = scene;
    return (jlong)model;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxParseModelFromStream(
        JNIEnv* env, jobject, jobject is) {

    jclass is_class = env->GetObjectClass(is);
    jmethodID read_method = env->GetMethodID(is_class, "read", "([BII)I");
    
    size_t JNI_BUFFER_SIZE = 32768; 
    jni_stream_context ctx;
    ctx.env = env;
    ctx.is = is;
    ctx.read_method = read_method;
    ctx.buffer = env->NewByteArray(JNI_BUFFER_SIZE);
    ctx.buffer_size = JNI_BUFFER_SIZE;

    ufbx_stream stream = { 0 };
    stream.read_fn = jni_read_fn;
    stream.user = &ctx;

    ufbx_load_opts opts = { 0 };
    opts.target_axes = ufbx_axes_right_handed_y_up;
    opts.target_unit_meters = 1.0f;

    ufbx_error error;
    ufbx_scene *scene = ufbx_load_stream(&stream, &opts, &error);
    env->DeleteLocalRef(ctx.buffer);

    if (!scene) {
        LOGE("Failed to load from stream: %s", error.description.data);
        return 0;
    }

    fbx_model_t *model = new fbx_model_t();
    model->scene = scene;
    return (jlong)model;
}

extern "C" JNIEXPORT void JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_dto_FBXModel_fbxFreeModel(
        JNIEnv* env, jobject, jlong modelPtr) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    delete model; 
}

extern "C" JNIEXPORT jint JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetMeshCount(
        JNIEnv* env, jobject, jlong modelPtr) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    if (!model || !model->scene) return 0;
    int count = 0;
    for (size_t i = 0; i < model->scene->nodes.count; i++) {
        if (model->scene->nodes.data[i]->mesh) count++;
    }
    return (jint)count;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetVertexBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    ufbx_node* node = find_mesh_node(model, meshIndex);
    if (!node || !node->mesh) return NULL;

    ufbx_mesh *mesh = node->mesh;
    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_vertices = (float*)malloc(total_vertices * 3 * sizeof(float));
    size_t v_idx = 0;

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            uint32_t indices[3] = { face.index_begin, face.index_begin + corner + 1, face.index_begin + corner + 2 };
            for (int k = 0; k < 3; k++) {
                ufbx_vec3 pos = ufbx_get_vertex_vec3(&mesh->vertex_position, indices[k]);
                pos = transform_pos(&node->geometry_to_world, pos);
                unrolled_vertices[v_idx++] = (float)pos.x;
                unrolled_vertices[v_idx++] = (float)pos.y;
                unrolled_vertices[v_idx++] = (float)pos.z;
            }
        }
    }

    model->allocated_buffers.push_back(unrolled_vertices);
    return env->NewDirectByteBuffer(unrolled_vertices, (jlong)(total_vertices * 3 * sizeof(float)));
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetNormalsBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    ufbx_node* node = find_mesh_node(model, meshIndex);
    if (!node || !node->mesh || !node->mesh->vertex_normal.exists) return NULL;

    ufbx_mesh *mesh = node->mesh;
    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_normals = (float*)malloc(total_vertices * 3 * sizeof(float));
    size_t v_idx = 0;

    ufbx_matrix normal_matrix = ufbx_matrix_for_normals(&node->geometry_to_world);

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            uint32_t indices[3] = { face.index_begin, face.index_begin + corner + 1, face.index_begin + corner + 2 };
            for (int k = 0; k < 3; k++) {
                ufbx_vec3 norm = ufbx_get_vertex_vec3(&mesh->vertex_normal, indices[k]);
                norm = transform_norm(&normal_matrix, norm);
                unrolled_normals[v_idx++] = (float)norm.x;
                unrolled_normals[v_idx++] = (float)norm.y;
                unrolled_normals[v_idx++] = (float)norm.z;
            }
        }
    }

    model->allocated_buffers.push_back(unrolled_normals);
    return env->NewDirectByteBuffer(unrolled_normals, (jlong)(total_vertices * 3 * sizeof(float)));
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetColorsBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    ufbx_node* node = find_mesh_node(model, meshIndex);
    if (!node || !node->mesh) return NULL;

    ufbx_mesh *mesh = node->mesh;
    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_colors = (float*)malloc(total_vertices * 4 * sizeof(float));
    size_t v_idx = 0;

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            uint32_t indices[3] = { face.index_begin, face.index_begin + corner + 1, face.index_begin + corner + 2 };
            for (int k = 0; k < 3; k++) {
                if (mesh->vertex_color.exists) {
                    ufbx_vec4 col = ufbx_get_vertex_vec4(&mesh->vertex_color, indices[k]);
                    unrolled_colors[v_idx++] = (float)col.x;
                    unrolled_colors[v_idx++] = (float)col.y;
                    unrolled_colors[v_idx++] = (float)col.z;
                    unrolled_colors[v_idx++] = (float)col.w;
                } else {
                    unrolled_colors[v_idx++] = 1.0f; unrolled_colors[v_idx++] = 1.0f;
                    unrolled_colors[v_idx++] = 1.0f; unrolled_colors[v_idx++] = 1.0f;
                }
            }
        }
    }

    model->allocated_buffers.push_back(unrolled_colors);
    return env->NewDirectByteBuffer(unrolled_colors, (jlong)(total_vertices * 4 * sizeof(float)));
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetTexCoordsBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    ufbx_node* node = find_mesh_node(model, meshIndex);
    if (!node || !node->mesh || !node->mesh->vertex_uv.exists) return NULL;

    ufbx_mesh *mesh = node->mesh;
    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_uvs = (float*)malloc(total_vertices * 2 * sizeof(float));
    size_t v_idx = 0;

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            uint32_t indices[3] = { face.index_begin, face.index_begin + corner + 1, face.index_begin + corner + 2 };
            for (int k = 0; k < 3; k++) {
                ufbx_vec2 uv = ufbx_get_vertex_vec2(&mesh->vertex_uv, indices[k]);
                unrolled_uvs[v_idx++] = (float)uv.x;
                unrolled_uvs[v_idx++] = 1-(float)uv.y;
            }
        }
    }

    model->allocated_buffers.push_back(unrolled_uvs);
    return env->NewDirectByteBuffer(unrolled_uvs, (jlong)(total_vertices * 2 * sizeof(float)));
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetTangentsBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    ufbx_node* node = find_mesh_node(model, meshIndex);
    if (!node || !node->mesh || !node->mesh->vertex_tangent.exists) return NULL;

    ufbx_mesh *mesh = node->mesh;
    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_tangents = (float*)malloc(total_vertices * 3 * sizeof(float));
    size_t v_idx = 0;

    ufbx_matrix normal_matrix = ufbx_matrix_for_normals(&node->geometry_to_world);

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            uint32_t indices[3] = { face.index_begin, face.index_begin + corner + 1, face.index_begin + corner + 2 };
            for (int k = 0; k < 3; k++) {
                ufbx_vec3 tang = ufbx_get_vertex_vec3(&mesh->vertex_tangent, indices[k]);
                tang = transform_norm(&normal_matrix, tang);
                unrolled_tangents[v_idx++] = (float)tang.x;
                unrolled_tangents[v_idx++] = (float)tang.y;
                unrolled_tangents[v_idx++] = (float)tang.z;
            }
        }
    }

    model->allocated_buffers.push_back(unrolled_tangents);
    return env->NewDirectByteBuffer(unrolled_tangents, (jlong)(total_vertices * 3 * sizeof(float)));
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetIndexBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    return NULL;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetTexturePath(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    ufbx_node* node = find_mesh_node(model, meshIndex);
    if (!node || node->materials.count == 0) return NULL;

    ufbx_material *mat = node->materials.data[0];
    if (mat->fbx.diffuse_color.texture) {
        return env->NewStringUTF(mat->fbx.diffuse_color.texture->relative_filename.data);
    }
    return NULL;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetModelAttribute(
        JNIEnv *env, jobject thiz, jlong modelPtr, jstring attributeName) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    if (!model || !model->scene) return NULL;
    const char *nativeAttributeName = env->GetStringUTFChars(attributeName, 0);
    jobject result = NULL;
    if(strcmp(nativeAttributeName, "metadata.creator") == 0){
        result = env->NewStringUTF(model->scene->metadata.creator.data);
    } else if(strcmp(nativeAttributeName, "metadata.version") == 0){
        jclass intClass = env->FindClass("java/lang/Integer");
        jmethodID intInit = env->GetMethodID(intClass, "<init>", "(I)V");
        result = env->NewObject(intClass, intInit, (jint)model->scene->metadata.version);
    }
    env->ReleaseStringUTFChars(attributeName, nativeAttributeName);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetVersion(
        JNIEnv* env, jobject, jlong modelPtr) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    if (!model || !model->scene) return 0;
    return (jint)model->scene->metadata.version;
}
