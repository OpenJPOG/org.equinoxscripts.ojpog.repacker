package repacker.model.merge;

import java.nio.ByteBuffer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import jassimp.AiNode;
import jassimp.AiWrapperProvider;
import jassimp.Jassimp;

public class JassimpGDXWrapper implements AiWrapperProvider<Vector3, Matrix4, Color, AiNode, Quaternion> {
	@Override
	public Vector3 wrapVector3f(ByteBuffer buffer, int offset, int numComponents) {
		float fx = buffer.getFloat(offset);
		float fy = numComponents <= 1 ? 0 : buffer.getFloat(offset + 4);
		float fz = numComponents <= 2 ? 0 : buffer.getFloat(offset + 8);
		return new Vector3(fx, fy, fz);
	}

	@Override
	public Matrix4 wrapMatrix4f(float[] data) {
		return new Matrix4(data).tra();
	}

	@Override
	public Color wrapColor(ByteBuffer buffer, int offset) {
		float r = buffer.getFloat(offset);
		float g = buffer.getFloat(offset + 4);
		float b = buffer.getFloat(offset + 8);
		float a = buffer.getFloat(offset + 12);
		return new Color(r, g, b, a);
	}

	@Override
	public AiNode wrapSceneNode(Object parent, Object matrix, int[] meshReferences, String name) {
		return (AiNode) Jassimp.BUILTIN.wrapSceneNode(parent, matrix, meshReferences, name);
	}

	@Override
	public Quaternion wrapQuaternion(ByteBuffer buffer, int offset) {
		float w = buffer.getFloat(offset);
		float x = buffer.getFloat(offset + 4);
		float y = buffer.getFloat(offset + 8);
		float z = buffer.getFloat(offset + 12);
		return new Quaternion(x, y, z, w);
	}
}
