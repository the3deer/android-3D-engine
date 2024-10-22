/*
package org.the3deer.android_3d_model_engine.services.fbx;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.plugins.AndroidLocator;
import com.jme3.asset.plugins.UrlAssetInfo;
import com.jme3.audio.AudioRenderer;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.scene.plugins.fbx.FbxLoader;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeSystemDelegate;
import com.jme3.texture.plugins.TGALoader;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.animation.KeyFrame;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;
import org.the3deer.android_3d_model_engine.services.collada.entities.JointData;
import org.the3deer.android_3d_model_engine.services.collada.entities.SkeletonData;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

*/
/**
 * + STL loader supported by the org.j3d STL parser
 *
 * @author andresoviedo
 *//*

public final class FBXLoaderTask extends LoaderTask {

    private final static String TAG = FBXLoaderTask.class.getSimpleName();

    private FbxLoader loader = new FbxLoader();

    public FBXLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws IOException {

        // current facet counter
        int counter = 0;

        try {

            JmeSystem.setSystemDelegate(new JmeSystemDelegate() {
                @Override
                public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {

                }

                @Override
                public URL getPlatformAssetConfigURL() {
                    return null;
                }

                @Override
                public JmeContext newContext(AppSettings settings, JmeContext.Type contextType) {
                    return null;
                }

                @Override
                public AudioRenderer newAudioRenderer(AppSettings settings) {
                    return null;
                }

                @Override
                public void initialize(AppSettings settings) {

                }

                @Override
                public void showSoftKeyboard(boolean show) {

                }
            });
            AssetManager assetManager = JmeSystem.newAssetManager();
            assetManager.registerLocator("models", AndroidLocator.class);
            assetManager.registerLoader(TGALoader.class, "tga");
            assetManager.registerLoader(J3MLoader.class, "j3m");
            assetManager.registerLoader(J3MLoader.class, "j3md");
            assetManager.registerLoader(GLSLLoader.class, "vert", "frag","geom","tsctrl","tseval","glsllib","glsl");



            // log event
            Log.i(TAG, "Parsing model...");
            super.publishProgress("Parsing model...");

            List<Object3DData> ret = new ArrayList<>();

            // Parse STL
            try
            {
                AssetInfo assetInfo = UrlAssetInfo.create(assetManager,
                        new ModelKey("test"), uri.toURL());
                Spatial load = loader.load(assetInfo);
                Log.i(TAG, "Object: "+load.getName());

                processSpatial(load, load, ret);
            }
            catch(IOException e) {
                Log.e(TAG, "Face '" + counter + "'" + e.getMessage(), e);
            }
            return ret;
            // super.publishProgress("Loading facets... "+counter+"/"+totalFaces);
            //super.onLoad(data);

        } catch (Exception e) {
            Log.e(TAG, "Face '" + counter + "'" + e.getMessage(), e);
            throw e;
        }
    }

    private void processSpatial(Spatial root, Spatial spatial, List<Object3DData> ret) {
        if (spatial instanceof Geometry){
            Geometry geometry = (Geometry) spatial;
            Mesh mesh = geometry.getMesh();
            if (mesh == null || mesh.getBuffer(VertexBuffer.Type.Index) == null) return;

            final FloatBuffer vertices = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.BindPosePosition).getData();
            final Buffer indices = mesh.getBuffer(VertexBuffer.Type.Index).getData();

            AnimatedModel data = new AnimatedModel(vertices, indices);
            data.setDrawMode(GLES20.GL_TRIANGLES);
            data.setId(spatial.getName());

            final Buffer boneIndex = mesh.getBuffer(VertexBuffer.Type.BoneIndex).getData();
            data.setJointIds(boneIndex);
            data.setJointIdsComponents(mesh.getBuffer(VertexBuffer.Type.BoneIndex).getNumComponents());
//
            final Buffer weightIndex = mesh.getBuffer(VertexBuffer.Type.BoneWeight).getData();
            data.setVertexWeights(weightIndex);
            data.setVertexWeightsComponents(mesh.getBuffer(VertexBuffer.Type.BoneWeight).getNumComponents());

            data.setJointsData(processSkeleton(root));
            data.doAnimation(processAnimation(root));

            ret.add(data);
            super.onLoad(data);
        }
        else if (spatial instanceof SkeletonDebugger){

        }
        else if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (int i = 0; i < node.getChildren().size(); i++) {
                processSpatial(root, node.getChild(i), ret);
            }
        }
    }

    private SkeletonData processSkeleton(Spatial root) {
        if (root.getNumControls() > 0) {
            for (int i = 0; i < root.getNumControls(); i++) {
                final Control control = root.getControl(i);
                if (!(control instanceof SkeletonControl)) continue;
                final SkeletonControl skeletonControl = (SkeletonControl) control;

                final Skeleton skeleton = skeletonControl.getSkeleton();
                if (skeleton == null) continue;
                final Bone[] roots = skeleton.getRoots();
                if (roots == null || roots.length == 0) continue;

                final JointData rootData = buildJointData(skeleton, roots[0]);
                final SkeletonData skeletonData = new SkeletonData(skeleton.getBoneCount(), rootData);
                skeletonData.setBoneCount(skeleton.getBoneCount());

                return skeletonData;
            }
        } else if (root instanceof Node){
            Node node = (Node) root;
            for (int i = 0; i< node.getChildren().size() ; i++) {
                SkeletonData skeletonData = processSkeleton(node.getChild(i));
                if (skeletonData != null) return skeletonData;
            }
        }
        return null;
    }

    private static @NonNull JointData buildJointData(Skeleton skeleton, Bone rootBone) {
        final JointData rootData = new JointData(rootBone.getName(), rootBone.getLocalScale().toArray(),
                rootBone.getLocalRotation().toAngles(), rootBone.getLocalPosition().toArray(),
                rootBone.getBindScale().toArray(), rootBone.getBindRotation().toAngles(), rootBone.getBindPosition().toArray());
        */
/*final JointData rootData = new JointData(rootBone.getName(), rootBone.getBindScale().toArray(), rootBone.getBindRotation().toAngles(), rootBone.getBindPosition().toArray()
                , rootBone.getLocalScale().toArray(),
                rootBone.getLocalRotation().toAngles(), rootBone.getLocalPosition().toArray()
                );*//*

        rootData.setIndex(skeleton.getBoneIndex(rootBone.getName()));
        rootData.setInverseBindTransform(rootBone.getModelBindInverseTransform().toTransformMatrix().toArray());
        if (rootBone.getChildren() != null) {
            for (int j = 0; j < rootBone.getChildren().size(); j++) {
                final Bone child = rootBone.getChildren().get(j);
                final JointData boneData = buildJointData(skeleton, child);
                rootData.addChild(boneData);
            }
        }
        return rootData;
    }

    private Animation processAnimation(Spatial spatial) {
        if (spatial.getNumControls() > 0) {
            for (int i = 0; i < spatial.getNumControls(); i++) {
                final Control control = spatial.getControl(i);
                if (!(control instanceof AnimControl)) continue;
                final AnimControl animControl = (AnimControl) control;
                if (animControl.getAnimationNames().isEmpty()) continue;
                com.jme3.animation.Animation anim = animControl.getAnim(animControl.getAnimationNames().iterator().next());

                if (anim == null || anim.getLength() == 0) continue;
                if (anim.getTracks() == null || anim.getTracks().length == 0) continue;

                // get all key times
                Set<Float> times = new HashSet<>();
                List<Float> timesList = new ArrayList<>();
                for (int j = 0; j < anim.getTracks().length; j++) {
                    Track track = anim.getTracks()[j];
                    if (!(track instanceof BoneTrack)) continue;
                    float[] keyFrameTimes = track.getKeyFrameTimes();
                    for (int k = 0; k < keyFrameTimes.length; k++) {
                        if (times.add(keyFrameTimes[k]))
                            timesList.add(keyFrameTimes[k]);
                    }
                }

                // init new model
                KeyFrame[] keyFrames = new KeyFrame[times.size()];
                Map<Float, KeyFrame> keyFramesMap = new HashMap<>();
                int j = 0;
                Collections.sort(timesList);
                for (Float time : timesList) {
                    Map<String, JointTransform> map = new HashMap<>();
                    KeyFrame keyFrame = new KeyFrame(time, map);
                    keyFramesMap.put(time, keyFrame);
                    keyFrames[j++] = keyFrame;
                }

                for (int at = 0; at < anim.getTracks().length; at++) {
                    Track track = anim.getTracks()[at];
                    if (!(track instanceof BoneTrack)) continue;
                    BoneTrack boneTrack = (BoneTrack) track;
                    for (int t = 0; t < boneTrack.getKeyFrameTimes().length; t++) {
                        float time = boneTrack.getTimes()[t];
                        KeyFrame keyFrame = keyFramesMap.get(time);

                        Vector3f translation = boneTrack.getTranslations()[t];
                        Vector3f scale = boneTrack.getScales()[t];
                        Quaternion rotation = boneTrack.getRotations()[t];

                        JointTransform jt = new JointTransform(scale.toArray(), rotation.toAngles(), translation.toArray());
                        //JointTransform jt = new JointTransform(new Float[]{1f,1f,1f}, new Float[]{0f,0f,0//f}, translation.toArray());
                        keyFrame.getPose().put(animControl.getSkeleton().getBone(boneTrack.getTargetBoneIndex()).getName(), jt);
                    }
                }

                Animation animData = new Animation(anim.getLength(), keyFrames);
                //KeyFrame keyframe = new KeyFrame(anim.getLength(), jointKeyFramesMap);

                return animData;
            }
        } else if (spatial instanceof Node){
            Node node = (Node) spatial;
            for (int i = 0; i< node.getChildren().size() ; i++) {
                Animation animation = processAnimation(node.getChild(i));
                if (animation != null) return animation;
            }
        }
        return null;
    }


}*/
