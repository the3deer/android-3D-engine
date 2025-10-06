package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import org.the3deer.util.math.Quaternion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Contains the extracted data for a single joint in the model. This stores the
 * joint's index, name, and local bind transform.
 *
 * @author andresoviedo
 *
 */
public class Node {

	// attributes
	private String id;
	private String name;
	private String sid;

	// --- Attachments (What this Node represents) ---
	// A node can have a mesh to be rendered at its position.
	// private Mesh mesh; // This is a REFERENCE to an object in the Scene's mesh library.
	private String meshId;

	private Map<String,String> materials;

	// local transform
	private final Transform bindTransform;

	// This holds the final calculated world-space transform
	protected float[] worldTransform = new float[16];

	// used in Animation
	// index referenced by skinning data
	// the order may need to be provided by the bone ordered list
	private int index = -1;

	private Node parent;
	public final List<Node> children = new ArrayList<>();

	public Node() {
		this(new Transform());
	}

	public Node(Transform bindTransform) {
		this.bindTransform = bindTransform;
	}

	// gltf
	public static Node fromMatrix(float[] matrix){
		return new Node(new Transform(matrix));
	}

	// gltf
	public static Node fromTransforms(float[] scale, Quaternion quaternion, float[] translation) {
		return new Node(new Transform(floatArrayToFloatWrapperArray(scale), quaternion, floatArrayToFloatWrapperArray(translation)));
	}

	// collada
	public Node(String id, String name, String sid,
				float[] bindLocalMatrix, Float[] bindLocalScale, Float[] bindLocalRotation, Float[] bindLocalTranslation,
				final float[] bindTransform, final float[] worldTransform,
				String geometryId, Map<String, String> materials) {
		this.id = id;
		this.name = name;
		this.sid = sid;

		// local transforms
		if (bindLocalMatrix != null){
			this.bindTransform = new Transform(bindLocalMatrix);
		} else {
			this.bindTransform = new Transform(bindLocalScale, bindLocalRotation, bindLocalTranslation);
		}

		this.meshId = geometryId;
		this.materials = materials;

		this.worldTransform = worldTransform;
	}

	public Node setId(String id) {
		this.id = id;
		return this;
	}

	public Node setName(String name) {
		this.name = name;
		return this;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public List<String> getMeshes() {
		if (meshId != null) {
			return Collections.singletonList(meshId);
		} else {
			return Collections.emptyList();
		}
	}

	public void setMesh(String meshId) {
		this.meshId = meshId;
	}

	/**
	 * Root joint constructor only
	 *
	 * @param id id of the visual scene
	 */
	public Node(String id) {
		this.index = -1;
		this.id = id;
		this.name = id;
		this.sid = id;

		this.bindTransform = new Transform();
		this.worldTransform = new float[16];
		Matrix.setIdentityM(this.worldTransform,0);

		// extra data
		this.meshId = id;
		this.materials = null;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}


	public String getSid() {
		return sid;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public Transform getBindTransform() {
		return bindTransform;
	}

	public void updateWorldTransform(float[] parentWorldTransform) {
		// 1. Calculate this node's final world transform
		// worldTransform = parentWorldTransform * this.bindTransform (local matrix)
		Matrix.multiplyMM(this.worldTransform, 0, parentWorldTransform, 0, this.getBindTransform().getTransform(), 0);

		// 2. Recursively update all children
		for (Node child : getChildren()) {
			child.updateWorldTransform(this.worldTransform);
		}
	}

	public String getMeshId() {
		return this.meshId;
	}

	public void setInverseBindTransform(float[] inverseBindTransform) {
		this.bindTransform.setInverseTransform(inverseBindTransform);
	}

	public float[] getInverseBindTransform() {
		return bindTransform.getInverseTransform();
	}

	public List<Node> getChildren() {
		return children;
	}

	public void addChild(Node child) {
		children.add(child);
	}

	/**
	 * use instead {@link Node#findAll(String)}
	 */
	@Deprecated
	public Node find(String id) {
		if (id.equals(this.getId())) {
			return this;
		} else if (id.equals(this.getName())){
			return this;
		} else if (id.equals(this.meshId)){
			return this;
		}

		for (Node childNode : this.children) {
			Node candidate = childNode.find(id);
			if (candidate != null) return candidate;
		}
		return null;
	}

	public List<Node> findAll(String id) {
		return findAllImpl(id, new ArrayList<>());
	}

	private List<Node> findAllImpl(String id, List<Node> ret){
		if (id.equals(this.getId())) {
			ret.add(this);
		} else if (id.equals(this.getName())){
			ret.add(this);
		} else if (id.equals(this.meshId)){
			ret.add(this);
		}
		for (Node childNode : this.children) {
			ret.addAll(childNode.findAll(id));
		}
		return ret;
	}

	public boolean containsMaterial(String materialId){
		return materials.containsKey(materialId);
	}

	public String getMaterial(String materialId){
		return materials.get(materialId);
	}

	public float[] getWorldTransform() {
		return worldTransform;
	}


	private static Float[] floatArrayToFloatWrapperArray(float[] primitiveArray) {
		if (primitiveArray == null) {
			return null; // Handle null case if needed
		}

		// 1. Create the new Float[] array with the same size.
		Float[] wrapperArray = new Float[primitiveArray.length];

		// 2. Loop through the primitive array and auto-box each float to Float.
		for (int i = 0; i < primitiveArray.length; i++) {
			wrapperArray[i] = primitiveArray[i]; // Auto-boxing occurs here
		}

		return wrapperArray;
	}

	@Override
	public String toString() {
		return "JointData{" +
				"index=" + getIndex() +
				", id='" + getId() + '\'' +
				", name='" + getName() + '\'' +
				'}';
	}

	public Float[] getBindLocalTranslation() {
		return bindTransform.getTranslation();
	}

	public Float[] getBindLocalScale() {
		return bindTransform.getScale();
	}

	public Quaternion getBindLocalQuaternion() {
		return bindTransform.getQuaternion();
	}

	public Float[] getBindLocalRotation() {
		return bindTransform.getRotation();
	}
}
