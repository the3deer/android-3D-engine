package org.the3deer.engine.animation;


import org.the3deer.engine.model.AnimatedModel;
import org.the3deer.engine.model.Node;

import java.util.logging.Logger;

/**
 * 
 * Represents an animation that can applied to an {@link AnimatedModel} . It
 * contains the length of the animation in seconds, and a list of
 * {@link KeyFrame}s.
 * 
 * @author andresoviedo
 * 
 *
 */
public class Animation {

	private static final Logger logger = Logger.getLogger(Animation.class.getSimpleName());

	private final String name;//in seconds
	private final float length;//in seconds
	private final KeyFrame[] keyFrames;
	private boolean initialized;
	private Node rootNode;

	/**
	 * @param name
	 * @param lengthInSeconds - the total length of the animation in seconds.
	 * @param frames          - all the keyframes for the animation, ordered by time of
	 *                        appearance in the animation.
	 */
	public Animation(String name, float lengthInSeconds, KeyFrame[] frames) {
		this.name = name;
		this.keyFrames = frames;
		this.length = lengthInSeconds;
	}

	public String getName() {
		return name;
	}

	public Node getRootNode() {
		return rootNode;
	}

	public void setRootNode(Node rootNode) {
		this.rootNode = rootNode;
	}

	public void setInitialized(boolean initialized){
		this.initialized = initialized;
	}

	public boolean isInitialized(){
		return initialized;
	}

	/**
	 * @return The length of the animation in seconds.
	 */
	public float getLength() {
		return length;
	}

	/**
	 * @return An array of the animation's keyframes. The array is ordered based
	 *         on the order of the keyframes in the animation (first keyframe of
	 *         the animation in array position 0).
	 */
	public KeyFrame[] getKeyFrames() {
		return keyFrames;
	}

	public void debugKeyFrames(){
		if (keyFrames == null) return;

		for (int i=0; i<keyFrames.length; i++){
			if (i<10) {
				logger.finest("Keyframe["+i+"] : " + keyFrames[i]);
			}
		}
	}

}
