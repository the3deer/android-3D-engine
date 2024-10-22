package org.the3deer.android_3d_model_engine.services.gltf;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.animation.KeyFrame;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.collada.entities.JointData;
import org.the3deer.android_3d_model_engine.services.collada.entities.SkeletonData;
import org.the3deer.util.android.AndroidUtils;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.math.Quaternion;

import java.io.InputStream;
import java.net.URI;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.Buffers;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.GltfReferenceResolver;
import de.javagl.jgltf.model.io.IO;
import de.javagl.jgltf.model.v2.MaterialModelV2;

public final class GltfLoader {

    public static final String TAG = GltfLoader.class.getSimpleName();

    @NonNull
    public List<Object3DData> load(URI uri, LoadListener callback) {

        callback.onProgress("Loading file...");

        final List<Object3DData> ret = new ArrayList<>();
        // final List<MeshData> allMeshes = new ArrayList<>();

        try (InputStream is = ContentUtils.getInputStream(uri)) {

            Log.i(TAG, "Loading model file... " + uri);
            callback.onProgress("Loading " + uri);

            // gltf ...
            GltfAssetReader gltfAssetReader = new GltfAssetReader();
            GltfAsset gltfAsset = gltfAssetReader.readWithoutReferences(is);
            URI baseUri = IO.getParent(uri);
            GltfReferenceResolver.resolveAll(gltfAsset.getReferences(), baseUri);
            GltfModel gltfModel = GltfModels.create(gltfAsset);

            // load scene...
            Log.d(TAG, "Loading scene...");
            for (SceneModel sceneModel : gltfModel.getSceneModels()) {

                // load nodes...
                Log.d(TAG, "Loading scene...");
                for (NodeModel nodeModel : sceneModel.getNodeModels()) {

                    Log.d(TAG, "Loading nodes...");
                    callback.onProgress("Loading nodes...");

                    loadNodeModel(gltfModel, callback, ret, null, nodeModel);
                }
            }

            // check
            if (ret.isEmpty()) return ret;

            callback.onProgress("Loading skinning data...");
            SkeletonData skeleton = loadSkeleton(gltfModel);

            // check
            if (skeleton == null) return ret;

            for (int i=0; i<ret.size(); i++) {
                final AnimatedModel model = (AnimatedModel) ret.get(i);
                model.setSkeleton(skeleton);
                model.setBindShapeMatrix(skeleton.getBindShapeMatrix());
            }

            loadAnimation(callback, gltfModel, ret);

        } catch (Exception ex) {
            Log.e(TAG, "Problem loading model", ex);
        }
        return ret;
    }


    private void loadNodeModel(GltfModel gltfModel, LoadListener callback, List<Object3DData> ret,
                               float[] nodeTransform, NodeModel nodeModel) {

        for (MeshModel meshModel : nodeModel.getMeshModels()) {
            loadMeshModel(gltfModel, callback, ret, nodeTransform, nodeModel, meshModel);
        }

        for (NodeModel childNode : nodeModel.getChildren()) {
            loadNodeModel(gltfModel, callback, ret, nodeTransform, childNode);
        }
    }

    private void loadMeshModel(GltfModel gltfModel, LoadListener callback, List<Object3DData> ret,
                               float[] bindTransform, NodeModel nodeModel, MeshModel meshModel) {
        List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();

        Log.d(TAG, "Loading mesh primitives...");
        callback.onProgress("Loading mesh primitives...");


        for (MeshPrimitiveModel meshPrimitiveModel : meshPrimitiveModels) {

            Object3DData model = loadMeshPrimitive(gltfModel, meshModel, meshPrimitiveModel);

            //model.setBindTransform(nodeModel.computeGlobalTransform(null));
            model.setBindTransform(nodeModel.computeGlobalTransform(null));
            //model.setBindTransform(nodeModel.getMatrix());

            callback.onLoad(model);

            ret.add(model);
        }
    }

    private Object3DData loadMeshPrimitive(GltfModel gltfModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel) {
        Log.d(TAG, "Loading mesh primitive...");

        // build model
        final AnimatedModel model = new AnimatedModel();

        AccessorModel position = meshPrimitiveModel.getAttributes().get("POSITION");
        FloatBuffer vertexBuffer = position.getAccessorData().createByteBuffer().asFloatBuffer();

        FloatBuffer normalBuffer = null;
        AccessorModel normal = meshPrimitiveModel.getAttributes().get("NORMAL");
        if (normal != null) {
            normalBuffer = normal.getAccessorData().createByteBuffer().asFloatBuffer();
        }

        FloatBuffer tangentBuffer = null;
        AccessorModel tangent = meshPrimitiveModel.getAttributes().get("TANGENT");
        if (tangent != null) {
            tangentBuffer = tangent.getAccessorData().createByteBuffer().asFloatBuffer();
        }

        Buffer colorBuffer = null;
        AccessorModel color = meshPrimitiveModel.getAttributes().get("COLOR_0");
        if (color != null) {
            if (color.getAccessorData().getComponentType() == short.class) {
                colorBuffer = color.getAccessorData().createByteBuffer().asShortBuffer();
            } else if (color.getAccessorData().getComponentType() == float.class) {
                colorBuffer = color.getAccessorData().createByteBuffer().asFloatBuffer();
            }
        }

        Buffer drawBuffer = null;
        if (meshPrimitiveModel.getIndices() != null) {
            if (meshPrimitiveModel.getIndices().getAccessorData().getComponentType() == short.class) {
                drawBuffer = meshPrimitiveModel.getIndices().getAccessorData().createByteBuffer().asShortBuffer();
            } else if (meshPrimitiveModel.getIndices().getAccessorData().getComponentType() == int.class) {
                drawBuffer = meshPrimitiveModel.getIndices().getAccessorData().createByteBuffer().asIntBuffer();
            } else if (meshPrimitiveModel.getIndices().getAccessorData().getComponentType() == byte.class) {
                drawBuffer = meshPrimitiveModel.getIndices().getAccessorData().createByteBuffer();
            } else {
                Log.e("GltfLoader", "unknown buffer type: " + meshPrimitiveModel.getIndices().getAccessorData().getComponentType());
            }
        }

        // build 3d model
        model.setVertexBuffer(vertexBuffer);
        model.setNormalsBuffer(normalBuffer);
        model.setTangentBuffer(tangentBuffer);
        model.setColorsBuffer(colorBuffer);
        model.setDrawOrder(drawBuffer);
        model.setDrawUsingArrays(drawBuffer == null);
        if (meshModel.getName() != null) {
            model.setId(meshModel.getName());
            model.setName(meshModel.getName());
        } else {
            model.setId(String.valueOf(meshModel.hashCode()));
            model.setName(String.valueOf(meshModel.hashCode()));
        }

        model.setDrawMode(meshPrimitiveModel.getMode());


        // check
        if (drawBuffer != null) {
            int min = Integer.MAX_VALUE;
            int max = -Integer.MAX_VALUE;
            for (int i = 0; i < drawBuffer.capacity(); i++) {
                if (drawBuffer instanceof IntBuffer) {
                    min = Math.min(((IntBuffer) drawBuffer).get(i), min);
                    max = Math.max(((IntBuffer) drawBuffer).get(i), max);
                } if (drawBuffer instanceof ShortBuffer) {
                    min = Math.min(((ShortBuffer) drawBuffer).get(i), min);
                    max = Math.max(((ShortBuffer) drawBuffer).get(i), max);
                }else if (drawBuffer instanceof ByteBuffer) {
                    min = Math.min(((ByteBuffer) drawBuffer).get(i), min);
                    max = Math.max(((ByteBuffer) drawBuffer).get(i), max);
                }
            }

            if (min != 0) {
                Log.e("GltfLoader", "Index not starting in zero: " + min);
            }
        }

        //final Element.Builder elementBuilder = new Element.Builder();

        // parse material
        MaterialModelV2 materialModel = (MaterialModelV2) meshPrimitiveModel.getMaterialModel();
        if (materialModel != null) {
            final Material material = new Material(materialModel.getName());

            // map color
            material.setDiffuse(materialModel.getBaseColorFactor());
            material.setAlphaCutoff(materialModel.getAlphaCutoff());
            try {
                material.setAlhaMode(Material.AlphaMode.valueOf(materialModel.getAlphaMode().name()));
            } catch (Exception e) {
                // ignore
            }

            // map texture
            if (materialModel.getBaseColorTexture() != null) {
                ByteBuffer imageData = materialModel.getBaseColorTexture().getImageModel().getImageData();

                Log.i(TAG, "Decoding diffuse bitmap... " + materialModel.getBaseColorTexture().getName());
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                    material.setColorTexture(new Texture().setBitmap(bitmap).setExtensions(materialModel.getExtensions()));
                } catch (Exception e) {
                    Log.e(TAG, "Issue decoding bitmap... " + materialModel.getBaseColorTexture().getName());
                }
            }

            // map normal map
            if (materialModel.getNormalTexture() != null) {
                ByteBuffer imageData = materialModel.getNormalTexture().getImageModel().getImageData();

                Log.i(TAG, "Decoding normal bitmap... " + materialModel.getNormalTexture().getName());
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                    material.setNormalTexture(new Texture().setBitmap(bitmap));
                } catch (Exception e) {
                    Log.e(TAG, "Issue decoding bitmap... " + materialModel.getBaseColorTexture().getName());
                }
            }

            // map emissive map
            if (materialModel.getEmissiveTexture() != null) {
                ByteBuffer imageData = materialModel.getEmissiveTexture().getImageModel().getImageData();

                Log.i(TAG, "Decoding emissive bitmap... " + materialModel.getEmissiveTexture().getName());
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                    material.setEmissiveTexture(new Texture().setBitmap(bitmap));
                    material.setEmissiveFactor(materialModel.getEmissiveFactor());
                } catch (Exception e) {
                    Log.e(TAG, "Issue decoding bitmap... " + materialModel.getBaseColorTexture().getName());
                }
            }

            // extensions
            try {
                final Map<String, Object> extensions = materialModel.getExtensions();
                if (extensions != null) {
                    final Map<String, Object> o = (Map<String, Object>) extensions.get("KHR_materials_volume");
                    if (o != null) {
                        final Map<String, Object> o1 = (Map<String, Object>) o.get("thicknessTexture");
                        double o2 = (double)o.get("thicknessFactor");
                        double o3 = (double)o.get("attenuationDistance");
                        List<Double> o4 = (List<Double>)o.get("attenuationColor");
                        final int texIdx = (int) o1.get("index");

                        final TextureModel textureModel = gltfModel.getTextureModels().get(texIdx);
                        final ByteBuffer imageData = textureModel.getImageModel().getImageData();
                        Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                        material.setTransmissionTexture(new Texture().setBitmap(bitmap));
                        material.setThicknessFactor((float) o2);
                        material.setAttenuationDistance((float) o3);
                        material.setAttenuationColor(new float[]{o4.get(0).floatValue(), o4.get(1).floatValue(),
                                o4.get(2).floatValue()});
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Issue decoding extensions... " + e.getMessage(), e);
            }

            model.setMaterial(material);
        }

        FloatBuffer textureBuffer = null;
        if (meshPrimitiveModel.getAttributes().containsKey("TEXCOORD_0")) {
            AccessorData texture_0 = meshPrimitiveModel.getAttributes().get("TEXCOORD_0").getAccessorData();
            textureBuffer = texture_0.createByteBuffer().asFloatBuffer();
            model.setTextureBuffer(textureBuffer);
        }

        // load skinning data
        try {
            AccessorModel joints = meshPrimitiveModel.getAttributes().get("JOINTS");
            if (joints == null){
                joints =  meshPrimitiveModel.getAttributes().get("JOINTS_0");
            }
            AccessorModel weights = meshPrimitiveModel.getAttributes().get("WEIGHTS");
            if (weights == null){
                weights =  meshPrimitiveModel.getAttributes().get("WEIGHTS_0");
            }
            if (joints != null && weights != null){
                final ByteBuffer byteBuffer = joints.getAccessorData().createByteBuffer();
                if (joints.getAccessorData().getComponentType() == int.class){
                    model.setJoints(byteBuffer.asIntBuffer());
                } else if (joints.getAccessorData().getComponentType() == short.class) {
                    model.setJoints(byteBuffer.asShortBuffer());
                } else if (joints.getAccessorData().getComponentType() == byte.class) {
                    model.setJoints(byteBuffer);
                }
                model.setJointIdsComponents(joints.getElementType().getNumComponents());
                final ByteBuffer weightsBuffer = weights.getAccessorData().createByteBuffer();
                if (weights.getAccessorData().getComponentType() == float.class) {
                    model.setWeights(weightsBuffer.asFloatBuffer());
                    model.setWeightsComponents(weights.getElementType().getNumComponents());
                } else {
                    Log.e(TAG, "Unknown weights type: "+weights.getAccessorData().getComponentType());
                }
            }
        }catch(Exception ex){
            Log.e(TAG, "Issue loading skinning data: " + ex.getMessage(), ex);
        }

        return model;
    }

    private SkeletonData loadSkeleton(GltfModel gltfModel){

        Log.d(TAG, "Loading skeleton...");

        // nodes
        final List<NodeModel> nodeList = gltfModel.getNodeModels();
        if (nodeList == null) return null;
        if (nodeList.isEmpty()) return null;

        // joints
        final List<NodeModel> nodeModels = gltfModel.getNodeModels();
        if (nodeModels == null) return null;
        if (nodeModels.isEmpty()) return null;


        // load joints
        final List<JointData> jointDataList = new ArrayList<>();
        for (int i = 0; i< nodeList.size(); i++) {
            final NodeModel node = nodeList.get(i);

            //JointData jointData = JointData.fromMatrix(node.computeLocalTransform(null));
            final JointData jointData;
            if (node.getMatrix() != null){
                jointData = JointData.fromMatrix(node.getMatrix());
            } else if (node.getScale() != null || node.getTranslation() != null || node.getRotation() != null){
                jointData = JointData.fromTransforms(node.getScale(), Quaternion.fromArray(node.getRotation()), node.getTranslation());
            } else {
                jointData = JointData.fromMatrix(node.computeLocalTransform(null));
            }
            jointData.setBindTransform(node.computeGlobalTransform(null));
            String name = node.getName();
            if (name == null){
                name = String.valueOf(i);
            }
            jointData.setName(name);
            if (node.getMeshModels() != null && !node.getMeshModels().isEmpty()) {
                // FIXME: link all meshes
                String meshId = node.getMeshModels().get(0).getName();
                if (meshId == null) meshId = String.valueOf(node.getMeshModels().get(0).hashCode());
                jointData.setMesh(meshId);
            }
            jointDataList.add(jointData);
        }

        // process hierarchy
        for (int i = 0; i< nodeList.size(); i++) {
            final NodeModel node = nodeList.get(i);
            final JointData jointNode = jointDataList.get(i);
            final List<NodeModel> children = node.getChildren();
            if (children == null || children.isEmpty()) continue;

            for (int c = 0; c< children.size(); c++){
                final NodeModel childNode = children.get(c);
                final int indexOfChild = nodeList.indexOf(childNode);
                final JointData child = jointDataList.get(indexOfChild);
                jointNode.addChild(child);
            }
        }

        Log.d(TAG, "Loading skin...");

        // skin
        final List<SkinModel> skinModels = gltfModel.getSkinModels();
        if (skinModels == null || skinModels.isEmpty()) return new SkeletonData(jointDataList, Collections.emptyList(), null) ;

        // joints
        final SkinModel skinModel = skinModels.get(0);
        final List<NodeModel> jointList = skinModel.getJoints();
        final JointData[] boneDataList = new JointData[jointList.size()];
        for (int i=0; i<jointList.size(); i++) {
            int index = nodeModels.indexOf(jointList.get(i));
            if (index != -1) {
                JointData jointData = jointDataList.get(index);
                jointData.setIndex(i);
                jointData.setInverseBindTransform(skinModel.getInverseBindMatrix(i, null));
                boneDataList[i] = jointData;
            }
        }

        // FIXME: handle multiple nodes
        // root node
        List<SceneModel> sceneModels = gltfModel.getSceneModels();
        SceneModel sceneModel = sceneModels.get(0);
        List<NodeModel> nodeModels1 = sceneModel.getNodeModels();
        NodeModel nodeModel = nodeModels1.get(0);
        int rootIdx = nodeList.indexOf(nodeModel);
        JointData headJoint = jointDataList.get(rootIdx);

        Log.d(TAG, "Skeleton loaded... joints: "+jointDataList.size()+", bones: "+boneDataList.length+ ", head: " + headJoint);

        //int rootIndex = nodeList.indexOf(jointList.get(0));
        return new SkeletonData(jointDataList, Arrays.asList(boneDataList), headJoint)
                .setBindShapeMatrix(skinModel.getBindShapeMatrix(null));
        //return new SkeletonData(jointDataList.size(), jointList.size(),  jointDataList.get(0));
    }

    private static void loadAnimation(LoadListener callback, GltfModel gltfModel, List<Object3DData> ret) {
        callback.onProgress("Loading animation data...");
        if (gltfModel.getAnimationModels() == null || gltfModel.getAnimationModels().isEmpty()) return;

        final TreeMap<Float,KeyFrame> times = new TreeMap<>();
        for (AnimationModel temp : gltfModel.getAnimationModels()){


            // load 1st animation
            AnimationModel animationModel = gltfModel.getAnimationModels().get(0);

            // FIXME: load all animations
            /*if (gltfModel.getAnimationModels().size() > 2){
                animationModel = gltfModel.getAnimationModels().get(2);
            }*/

            List<AnimationModel.Channel> channels = animationModel.getChannels();
            if (channels.isEmpty()) break;

            for (int ch=0; ch<channels.size(); ch++) {

                final AnimationModel.Channel animChannel = channels.get(ch);

                final AccessorModel input = animChannel.getSampler().getInput();
                final FloatBuffer bufferData = input.getAccessorData().createByteBuffer().asFloatBuffer();

                final AccessorModel output = animChannel.getSampler().getOutput();
                final FloatBuffer transformData = output.getAccessorData().createByteBuffer().asFloatBuffer();

                for (int idx = 0; idx < input.getCount(); idx++) {
                    float timeStamp = bufferData.get(idx);

                    KeyFrame keyFrame = times.get(timeStamp);
                    Map<String,JointTransform> transformMap;
                    if (keyFrame != null){
                        transformMap = keyFrame.getPose();
                    } else {
                        transformMap = new TreeMap<>();
                        keyFrame = new KeyFrame(timeStamp, transformMap);
                        times.put(timeStamp, keyFrame);
                    }

                    String name = animChannel.getNodeModel().getName();
                    if (name == null){
                        int nodeIdx = gltfModel.getNodeModels().indexOf(animChannel.getNodeModel());
                        name = String.valueOf(nodeIdx);
                    }
                    JointTransform jointTransform = transformMap.get(name);
                    if (jointTransform == null){
                        jointTransform = new JointTransform();
                        transformMap.put(name, jointTransform);
                    }

                    /*if (idx * 3 >= transformData.capacity() - 2){
                        Log.e(TAG, "BufferUnderFlowException: "+idx+", name: "+animChannel.getNodeModel().getName());
                        break;
                    }*/

                    try {
                        if ("translation".equals(animChannel.getPath())){
                            float[] transform = new float[3];
                            transformData.get(transform, 0, 3); // 3 components for translation (float x,float y,float z)
                            jointTransform.setLocation(transform);
                        } else if ("rotation".equals(animChannel.getPath())){
                            float[] transform = new float[4];
                            transformData.get(transform, 0, 4); // 4 components for quaternion
                            jointTransform.setRotation(new Quaternion(transform[0], transform[1], transform[2], transform[3]).normalize().toAngles2(null));
                            jointTransform.setQuaternion(new Quaternion(transform[0], transform[1], transform[2], transform[3]));
                        } else if ("scale".equals(animChannel.getPath())){
                            float[] transform = new float[3];
                            transformData.get(transform, 0, 3); // 3 components to scale (float x,float y,float z)
                            jointTransform.setScale(transform);
                        } else {
                            Log.e(TAG, "Unknown transform: "+animChannel.getPath());
                        }
                    } catch (BufferUnderflowException e) {
                        // ignore
                    }
                }
            }

            Animation animation = new Animation(times.lastKey(), times.values().toArray(new KeyFrame[0]));

            for (int i=0; i<ret.size(); i++) {
                ((AnimatedModel) ret.get(i)).setAnimation(animation);
            }

            return;
        }
    }
}
