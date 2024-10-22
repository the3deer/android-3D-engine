package org.the3deer.android_3d_model_engine.services.collada.entities;

import android.opengl.Matrix;
import android.util.Log;

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
public class JointData {

	// attributes
	private String id;
	private String name;
	private String sid;
	private String instance_geometry;
	private Map<String,String> materials;

	// local transforms (matrix)
	private float[] bindLocalMatrix;

	// local transforms (detailed)
	private Float[] bindLocalScale;
	private Float[] bindLocalRotation;
	private Quaternion bindLocalQuaternion;
	private Float[] bindLocalTranslation;

	// calculated: sump up of all local transforms - again?
	private float[] bindLocalTransform = Math3DUtils.IDENTITY_MATRIX;

	// calculated: sum up of all matrix up to the "root"
	private float[] bindTransform;
	private Float[] bindScale;
	private Float[] bindRotation;
	private Float[] bindLocation;

	// used in Animation
	// index referenced by skinning data
	// the order may need to be provided by the bone ordered list
	private int index = -1;
	private float[] inverseBindTransform;

	public final List<JointData> children = new ArrayList<>();

	public JointData() {
		refresh();
	}

	public static JointData fromMatrix(float[] matrix){

		JointData ret = new JointData();

		ret.bindLocalMatrix = matrix;
		ret.bindLocalQuaternion = Quaternion.fromMatrix(matrix);
		ret.bindScale = Math3DUtils.scaleFromMatrix(matrix);
		ret.bindLocalRotation = ret.bindLocalQuaternion.toAngles2(null);
		ret.bindLocalTranslation = Math3DUtils.extractTranslation2(matrix, null);

		ret.refresh();

		return ret;
	}

	public static JointData fromTransforms(float[] scale, Quaternion quaternion, float[] translation) {

		JointData ret = new JointData();

		// local transforms
		ret.bindLocalQuaternion = quaternion;
		ret.bindLocalScale = scale != null? new Float[]{scale[0], scale[1], scale[2]} : null;
		if (quaternion != null) {
			ret.bindLocalRotation = quaternion.toAngles2(null);
		}
		ret.bindLocalTranslation = translation != null? new Float[] {translation[0], translation[1], translation[2]} : null;

		ret.refresh();

		return ret;
	}

	public JointData(String id, String name, String sid,
					 float[] bindLocalMatrix, Float[] bindLocalScale, Float[] bindLocalRotation, Float[] bindLocalTranslation,
					 final float[] bindLocalTransform, final float[] bindTransform,
					 String geometryId, Map<String, String> materials) {
		this.id = id;
		this.name = name;
		this.sid = sid;

		// local transforms
		this.bindLocalMatrix = bindLocalMatrix;
		this.bindLocalScale = bindLocalScale;
		this.bindLocalRotation = bindLocalRotation;
		this.bindLocalTranslation = bindLocalTranslation;

		this.instance_geometry = geometryId;
		this.materials = materials;

		this.bindLocalTransform = bindLocalTransform;
		this.bindTransform = bindTransform;
	}

	public JointData setId(String id) {
		this.id = id;
		return this;
	}

	public JointData setName(String name) {
		this.name = name;
		return this;
	}

	public List<String> getMeshes() {
		if (instance_geometry != null) {
			return Collections.singletonList(instance_geometry);
		} else {
			return Collections.emptyList();
		}
	}

	public void setMesh(String instance_geometry) {
		this.instance_geometry = instance_geometry;
	}

	private void refresh() {

		this.bindLocalTransform = new float[16];
		Matrix.setIdentityM(this.bindLocalTransform, 0);

		if (bindLocalTranslation != null) {
			Matrix.translateM(this.bindLocalTransform, 0, this.bindLocalTranslation[0], this.bindLocalTranslation[1], this.bindLocalTranslation[2]);
		}

		if (bindLocalScale != null){
			Matrix.scaleM(this.bindLocalTransform, 0, this.bindLocalScale[0], this.bindLocalScale[1], this.bindLocalScale[2]);
		}

		if (bindLocalRotation != null) {
			Matrix.rotateM(this.bindLocalTransform, 0, this.bindLocalRotation[2], 0, 0, 1);
			Matrix.rotateM(this.bindLocalTransform, 0, this.bindLocalRotation[1], 0, 1, 0);
			Matrix.rotateM(this.bindLocalTransform, 0, this.bindLocalRotation[0], 1, 0, 0);
		}
/*
		if (bindLocalQuaternion != null) {
			*//*bindLocalQuaternion.normalize();
			Matrix.multiplyMM(this.bindLocalTransform, 0, this.bindLocalTransform, 0, bindLocalQuaternion.getMatrix(), 0);*//*
			float[] temp = new float[16];
			Matrix.multiplyMM(this.bindLocalTransform, 0, this.bindLocalTransform, 0, bindLocalQuaternion.getMatrix(), 0);
		} else if (bindLocalRotation != null) {
			Matrix.rotateM(this.bindLocalTransform, 0, this.bindLocalRotation[0], 1, 0, 0);
			Matrix.rotateM(this.bindLocalTransform, 0, this.bindLocalRotation[1], 0, 1, 0);
			Matrix.rotateM(this.bindLocalTransform, 0, this.bindLocalRotation[2], 0, 0, 1);
		}*/
	}

	/**
	 * Root joint constructor only
	 *
	 * @param id id of the visual scene
	 */
	public JointData(String id) {
		this.index = -1;
		this.id = id;
		this.name = id;
		this.sid = id;

		// local transforms
		this.bindLocalMatrix = new float[16];
		Matrix.setIdentityM(this.bindLocalMatrix,0);
		this.bindLocalScale = new Float[]{1f,1f,1f};
		this.bindLocalRotation = new Float[3];
		this.bindLocalTranslation = new Float[3];
		this.inverseBindTransform = new float[16];
		Matrix.setIdentityM(this.inverseBindTransform, 0);

		// calculated transforms
		this.bindLocalTransform = new float[16];
		Matrix.setIdentityM(this.bindLocalTransform,0);
		this.bindTransform = new float[16];
		Matrix.setIdentityM(this.bindTransform,0);

		// extra data
		this.instance_geometry = id;
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

	public Float[] getBindLocalScale() {
		return bindLocalScale;
	}

	public Float[] getBindLocalRotation() {
		return bindLocalRotation;
	}

	public Quaternion getBindLocalQuaternion() {
		return bindLocalQuaternion;
	}

	public Float[] getBindLocalTranslation() {
		return bindLocalTranslation;
	}

	public String getGeometryId() {
		return this.instance_geometry;
	}

	public void setInverseBindTransform(float[] inverseBindTransform) {
		this.inverseBindTransform = inverseBindTransform;
	}

	public float[] getInverseBindTransform() {
		if (this.inverseBindTransform == null){
			this.inverseBindTransform = new float[16];
			Matrix.setIdentityM(this.inverseBindTransform,0);
			if (this.bindLocalTransform != null){
				if(!Matrix.invertM(this.inverseBindTransform, 0, this.bindLocalTransform, 0)){
					Log.e("JointData", "Couldn't invert matrix for inverseBindTransform");
				}
			}
		}
		return inverseBindTransform;
	}

	public List<JointData> getChildren() {
		return children;
	}

	public void addChild(JointData child) {
		children.add(child);
	}

	/**
	 * use instead {@link JointData#findAll(String)}
	 */
	@Deprecated
	public JointData find(String id) {
		if (id.equals(this.getId())) {
			return this;
		} else if (id.equals(this.getName())){
			return this;
		} else if (id.equals(this.instance_geometry)){
			return this;
		}

		for (JointData childJointData : this.children) {
			JointData candidate = childJointData.find(id);
			if (candidate != null) return candidate;
		}
		return null;
	}

	public List<JointData> findAll(String id) {
		return findAllImpl(id, new ArrayList<>());
	}

	private List<JointData> findAllImpl(String id, List<JointData> ret){
		if (id.equals(this.getId())) {
			ret.add(this);
		} else if (id.equals(this.getName())){
			ret.add(this);
		} else if (id.equals(this.instance_geometry)){
			ret.add(this);
		}
		for (JointData childJointData : this.children) {
			ret.addAll(childJointData.findAll(id));
		}
		return ret;
	}

	public boolean containsMaterial(String materialId){
		return materials.containsKey(materialId);
	}

	public String getMaterial(String materialId){
		return materials.get(materialId);
	}

	public float[] getBindTransform() {
		return bindTransform;
	}

	public float[] getBindLocalTransform() {
		if (bindLocalMatrix != null){
			return bindLocalMatrix;
		} else {
			return bindLocalTransform;
		}
	}


	@Override
	public String toString() {
		return "JointData{" +
				"index=" + getIndex() +
				", id='" + getId() + '\'' +
				", name='" + getName() + '\'' +
				'}';
	}

	public void setBindTransform(float[] matrix) {
		this.bindTransform = matrix;
	}
}
