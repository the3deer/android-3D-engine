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

extern "C" {
    int get_a_random_number();
}

// -- Custom JNI Stream Context --
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
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxParseModelFromStream(
        JNIEnv* env, jobject, jobject is) {

    LOGD("Reading FBX model from stream with ufbx triangulation...");

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

    // --- CRITICAL OPTIONS ---
    ufbx_load_opts opts = { 0 };
    // Force all faces to be triangles (fixes the "fan" issue)
    //opts.target_face_type = UFBX_FACE_TRIANGLES;
    // Align to Android OpenGL coordinates
    opts.target_axes = ufbx_axes_right_handed_y_up;
    // Ensure scaling is consistent (Blender is often in cm)
    opts.target_unit_meters = 1.0f;
    // ------------------------

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
    LOGD("Freed fbx model 0x%" PRIx64, (uint64_t)modelPtr);
}

extern "C" JNIEXPORT jint JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetMeshCount(
        JNIEnv* env, jobject, jlong modelPtr) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    if (!model || !model->scene) return 0;
    return (jint)model->scene->meshes.count;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetVertexBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
    fbx_model_t *model = (fbx_model_t*)modelPtr;
    if (!model || !model->scene || (size_t)meshIndex >= model->scene->meshes.count) return NULL;

    ufbx_mesh *mesh = model->scene->meshes.data[meshIndex];
    
    // Calculate total vertices after triangulation
    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_vertices = (float*)malloc(total_vertices * 3 * sizeof(float));
    size_t v_idx = 0;

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            // Triangle fan triangulation
            uint32_t indices[3] = {
                face.index_begin,
                face.index_begin + corner + 1,
                face.index_begin + corner + 2
            };

            for (int k = 0; k < 3; k++) {
                ufbx_vec3 pos = ufbx_get_vertex_vec3(&mesh->vertex_position, indices[k]);
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
    if (!model || !model->scene || (size_t)meshIndex >= model->scene->meshes.count) return NULL;

    ufbx_mesh *mesh = model->scene->meshes.data[meshIndex];
    if (!mesh->vertex_normal.exists) return NULL;

    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_normals = (float*)malloc(total_vertices * 3 * sizeof(float));
    size_t v_idx = 0;

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            uint32_t indices[3] = {
                face.index_begin,
                face.index_begin + corner + 1,
                face.index_begin + corner + 2
            };

            for (int k = 0; k < 3; k++) {
                ufbx_vec3 norm = ufbx_get_vertex_vec3(&mesh->vertex_normal, indices[k]);
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
    if (!model || !model->scene || (size_t)meshIndex >= model->scene->meshes.count) return NULL;

    ufbx_mesh *mesh = model->scene->meshes.data[meshIndex];
    
    size_t total_vertices = 0;
    for (size_t i = 0; i < mesh->faces.count; i++) {
        total_vertices += (mesh->faces.data[i].num_indices - 2) * 3;
    }

    float* unrolled_colors = (float*)malloc(total_vertices * 4 * sizeof(float));
    size_t v_idx = 0;

    for (size_t i = 0; i < mesh->faces.count; i++) {
        ufbx_face face = mesh->faces.data[i];
        for (uint32_t corner = 0; corner < face.num_indices - 2; corner++) {
            uint32_t indices[3] = {
                face.index_begin,
                face.index_begin + corner + 1,
                face.index_begin + corner + 2
            };

            for (int k = 0; k < 3; k++) {
                if (mesh->vertex_color.exists) {
                    ufbx_vec4 col = ufbx_get_vertex_vec4(&mesh->vertex_color, indices[k]);
                    unrolled_colors[v_idx++] = (float)col.x;
                    unrolled_colors[v_idx++] = (float)col.y;
                    unrolled_colors[v_idx++] = (float)col.z;
                    unrolled_colors[v_idx++] = (float)col.w;
                } else {
                    unrolled_colors[v_idx++] = 1.0f;
                    unrolled_colors[v_idx++] = 1.0f;
                    unrolled_colors[v_idx++] = 1.0f;
                    unrolled_colors[v_idx++] = 1.0f;
                }
            }
        }
    }

    model->allocated_buffers.push_back(unrolled_colors);
    return env->NewDirectByteBuffer(unrolled_colors, (jlong)(total_vertices * 4 * sizeof(float)));
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_the3deer_android_13d_1model_1engine_services_fbx_FBXParser_fbxGetIndexBuffer(
        JNIEnv* env, jobject, jlong modelPtr, jint meshIndex) {
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
