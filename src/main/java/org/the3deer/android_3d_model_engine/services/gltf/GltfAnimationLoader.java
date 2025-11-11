package org.the3deer.android_3d_model_engine.services.gltf;// File: GltfAnimationLoader.java

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfAnimationDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfChannelDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// In: GltfAnimationLoader.java

import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.animation.KeyFrame;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSamplerDto;
import org.the3deer.util.math.Quaternion;

import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class GltfAnimationLoader {

    private final GltfDto dto;
    private final List<Node> nodes;

    public GltfAnimationLoader(GltfDto dto, List<Node> nodes) {
        this.dto = dto;
        this.nodes = nodes;
    }

    public List<Animation> load() {
        if (dto.animations == null || dto.animations.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Animation> animations = new ArrayList<>();
        for (GltfAnimationDto animDto : dto.animations) {

            if (animDto.channels == null || animDto.channels.isEmpty()) {
                continue;
            }

            final TreeMap<Float, KeyFrame> keyframes = new TreeMap<>();

            for (GltfChannelDto channel : animDto.channels) {
                final GltfSamplerDto sampler = animDto.samplers.get(channel.sampler);
                final FloatBuffer times = (FloatBuffer) sampler.input;
                final FloatBuffer values = (FloatBuffer) sampler.output;

                // Ensure buffers are ready for reading from the start
                times.rewind();
                values.rewind();

                final Node targetNode = nodes.get(channel.targetNode);
                final String jointId = targetNode.getId();

                for (int i = 0; i < times.capacity(); i++) {
                    final float timeStamp = times.get(i);

                    // Get or create the KeyFrame for this timestamp
                    KeyFrame keyFrame = keyframes.computeIfAbsent(timeStamp, k -> new KeyFrame(k, new HashMap<>()));
                    Map<String, JointTransform> pose = keyFrame.getPose();

                    // Get or create the JointTransform for the target node in this pose
                    JointTransform jointTransform = pose.computeIfAbsent(jointId, k -> new JointTransform());

                    try {
                        if ("translation".equals(channel.targetPath)) {
                            float[] translation = new float[3];
                            values.position(i * 3);
                            values.get(translation);
                            jointTransform.setLocation(translation);
                        } else if ("rotation".equals(channel.targetPath)) {
                            float[] rotation = new float[4];
                            values.position(i * 4);
                            values.get(rotation);
                            // Assuming your JointTransform or Quaternion class can handle this
                            jointTransform.setQuaternion(new Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]));
                        } else if ("scale".equals(channel.targetPath)) {
                            float[] scale = new float[3];
                            values.position(i * 3);
                            values.get(scale);
                            jointTransform.setScale(scale);
                        }
                    } catch (BufferUnderflowException e){
                        // This can happen if animation data is corrupt, ignore this keyframe for this channel
                    }
                }
            }

            if (!keyframes.isEmpty()) {
                final String animationName = animDto.name != null ? animDto.name : "Animation-" + System.identityHashCode(animDto);
                final Animation animation = new Animation(animationName, keyframes.lastKey(), keyframes.values().toArray(new KeyFrame[0]));
                animations.add(animation);
            }
        }
        return animations;
    }
}

