package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import org.the3deer.util.math.Math3DUtils;
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

	private int skinIndex = -1;

	private Map<String,String> materials;

	// local transform
	private Transform localTransform;

	// this holds the animated local space transform
	private float[] animatedLocalTransform;

	// This holds the final calculated world-space transform
	protected final float[] bindWorldTransform;

	// this holds the final animated world space transform
	private float[] animatedWorldTransform;

	// used in Animation
	// index referenced by skinning data
	// the order may need to be provided by the bone ordered list
	private int jointIndex = -1;

	private Node parent;
	public final List<Node> children = new ArrayList<>();
	public List<Object3DData> meshes;
	private Skin skin;

	// scene
	private Scene scene;
	// camera
	private Camera camera;

	// scene
	public Node() {
		this(new Transform());
	}

	// gltf - new
	public Node(int index) {
		this.id = String.valueOf(index); // The ID is the index.
		this.localTransform = new Transform();
		this.bindWorldTransform = new float[16];
		Matrix.setIdentityM(this.bindWorldTransform,0);
	}

	public Node(Transform localTransform) {
		this.id = "-2";
		this.localTransform = localTransform;
		this.bindWorldTransform = new float[16];
		Matrix.setIdentityM(this.bindWorldTransform,0);
	}

	// gltf - legacy
	public static Node fromMatrix(float[] matrix){
		return new Node(new Transform(matrix));
	}

	// gltf - legacy
	public static Node fromTransforms(float[] scale, Quaternion quaternion, float[] translation) {
		return new Node(new Transform(floatArrayToFloatWrapperArray(scale), quaternion, floatArrayToFloatWrapperArray(translation)));
	}

	// collada legacy
	public Node(String id, String name, String sid,
				float[] bindLocalMatrix, Float[] bindLocalScale, Float[] bindLocalRotation, Float[] bindLocalTranslation,
				final float[] localTransform, final float[] bindWorldTransform,
				String geometryId, Map<String, String> materials) {
		this.id = id;
		this.name = name;
		this.sid = sid;

		// local transforms
		if (bindLocalMatrix != null){
			this.localTransform = new Transform(bindLocalMatrix);
		} else {
			this.localTransform = new Transform(bindLocalScale, bindLocalRotation, bindLocalTranslation);
		}

		this.meshId = geometryId;
		this.materials = materials;

		this.bindWorldTransform = bindWorldTransform;
	}



	public Node setName(String name) {
		this.name = name;
		return this;
	}



	public void setMatrix(float[] matrix) {
		this.localTransform = new Transform(matrix);
	}

	public Node getParent() {
		return parent;
	}

	/**
	 * Returns the topmost parent node.
	 *
	 * @return the topmost parent node
	 */
	public Node getRoot() {
		Node ret = this;
		while (ret != null && ret.getParent() != null) {
			ret = ret.getParent();
		}
		return ret;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public Scene getScene() {
		return scene;
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		this.skin = skin;
	}

	public List<String> getMeshesId() {
		if (meshId != null) {
			return Collections.singletonList(meshId);
		} else {
			return Collections.emptyList();
		}
	}

	public void setMeshId(String meshId) {
		this.meshId = meshId;
	}

	public void setMesh(Object3DData mesh) {
		this.meshes = new ArrayList<>(Collections.singletonList(mesh));
	}

	public Object3DData getMesh(){
		if (this.meshes == null || this.meshes.isEmpty()) return null;
		return this.meshes.get(0);
	}

	/**
	 * Root joint constructor only
	 *
	 * @param id id of the visual scene
	 */
	// collada - legacy
	public Node(String id) {
		this.jointIndex = -1;
		this.id = id;
		this.name = id;

		this.localTransform = new Transform();
		this.bindWorldTransform = new float[16];
		Matrix.setIdentityM(this.bindWorldTransform,0);

		// extra data
		this.meshId = id;
		this.materials = null;
    }

	public String getId() {
		return id;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getName() {
		return name;
	}


	public String getSid() {
		return sid;
	}

	public void setJointIndex(int jointIndex) {
		this.jointIndex = jointIndex;
	}

	public int getJointIndex() {
		return jointIndex;
	}

	public int getSkinIndex() {
		return skinIndex;
	}

	public void setSkinIndex(int skinIndex) {
		this.skinIndex = skinIndex;
	}

	public Transform getLocalTransform() {
		return localTransform;
	}

	public boolean isStatic() {
		// A node is static if its local animated transform is the identity matrix.
		// You may need to add a utility function for this check.
		return Math3DUtils.isIdentity(this.animatedLocalTransform);
	}



	public void updateBindWorldTransform(float[] parentWorldTransform) {

		// 0. get inverse bind matrix
		float[] matrix = Math3DUtils.IDENTITY_MATRIX;
		if (skinIndex != -1 && skin != null && skin.getInverseBindMatrices() != null){
			matrix = new float[16];
			Matrix.setIdentityM(matrix,0);
			Matrix.multiplyMM(matrix, 0, this.getLocalTransform().getTransform(), 0, skin.getInverseBindMatrices(), skinIndex * 16);
		}

		// 1. Calculate this node's final world transform
		// worldTransform = parentWorldTransform * this.bindTransform (local matrix)
		Matrix.multiplyMM(this.bindWorldTransform, 0, parentWorldTransform, 0, matrix, 0);

		// 2. Recursively update all children
		for (Node child : getChildren()) {
			child.updateBindWorldTransform(this.bindWorldTransform);
		}
	}

	public float[] getAnimatedLocalTransform() {
		return animatedLocalTransform;
	}

	public void setAnimatedLocalTransform(float[] animatedLocalTransform) {
		this.animatedLocalTransform = animatedLocalTransform;
	}

	public void setAnimatedWorldTransform(float[] animatedWorldTransform) {
		this.animatedWorldTransform = animatedWorldTransform;
	}

	/**
	 * Calculates the final world transform for the current frame, taking animation into account.
	 * It recursively multiplies the parent's final transform with this node's (potentially animated) local transform.
	 * @param parentAnimatedWorldTransform The final transform of the parent node.
	 */
	//private final float[] temp = new float[16];

	public void updateAnimatedWorldTransform(float[] parentAnimatedWorldTransform) {
		// 1. Calculate this node's final world transform for this frame.
		// animatedWorldTransform = parentAnimatedWorldTransform * this.localTransform
		if (this.animatedWorldTransform == null) {
			this.animatedWorldTransform = new float[16];
			Matrix.setIdentityM(this.animatedWorldTransform,0);
		}

		if (this.getAnimatedLocalTransform() != null) {
			Matrix.multiplyMM(this.animatedWorldTransform, 0, parentAnimatedWorldTransform, 0, this.getAnimatedLocalTransform(), 0);
		} else {
			Matrix.multiplyMM(this.animatedWorldTransform, 0, parentAnimatedWorldTransform, 0, this.getLocalTransform().getTransform(), 0);
		}
		//Matrix.multiplyMM(this.animatedWorldTransform, 0, temp, 0, this.getInverseBindMatrix(), 0);

		// 2. Recursively update all children using THIS node's final transform as the new parent.
		for (Node child : getChildren()) {
			child.updateAnimatedWorldTransform(this.animatedWorldTransform);
		}
	}

	/**
	 * The animated transform is the transform that gets loaded up to the shader
	 * and is used to deform the vertices of the "skin". It represents the
	 * transformation from the joint's bind position (original position in
	 * model-space) to the joint's desired animation pose (also in model-space).
	 * This matrix is calculated by taking the desired model-space transform of
	 * the joint and multiplying it by the inverse of the starting model-space
	 * transform of the joint.
	 *
	 * @return The transformation matrix of the joint which is used to deform
	 * associated vertices of the skin in the shaders.
	 */
	public float[] getAnimatedWorldTransform() {
		return animatedWorldTransform;
	}

	public String getMeshId() {
		return this.meshId;
	}

	public void setInverseBindMatrix(float[] inverseBindTransform) {
		this.localTransform.setInverseTransform(inverseBindTransform);
	}

	public float[] getInverseBindMatrix() {
		return localTransform.getInverseTransform();
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

	public float[] getBindWorldTransform() {
		return bindWorldTransform;
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


	public Float[] getBindLocalTranslation() {
		return localTransform.getTranslation();
	}

	public float[] getTransform() {
		return localTransform.getTransform();
	}

	public Float[] getBindLocalScale() {
		return localTransform.getScale();
	}

	public Quaternion getBindLocalQuaternion() {
		return localTransform.getQuaternion();
	}

	public Float[] getBindLocalRotation() {
		return localTransform.getRotation();
	}

	// camera
	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	public Camera getCamera() {
		return camera;
	}

	public void setMeshes(List<Object3DData> meshes) {
		this.meshes = meshes;
	}

	public List<Object3DData> getMeshes() {
		return meshes;
	}

	public void setLocalTransform(Transform transform) {
		this.localTransform = transform;
	}

	@Deprecated
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Node{" +
				"id='" + getId() + '\'' +
				", name='" + getName() + '\'' +
				", index=" + getJointIndex() +
				'}';
	}
}
