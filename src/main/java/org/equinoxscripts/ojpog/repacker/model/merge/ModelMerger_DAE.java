package org.equinoxscripts.ojpog.repacker.model.merge;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import javax.vecmath.Vector3f;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.io.tmd.mesh.TMD_DLoD_Level;
import org.equinoxscripts.ojpog.io.tmd.mesh.TMD_Mesh;
import org.equinoxscripts.ojpog.io.tmd.scene.TMD_Node;
import org.equinoxscripts.ojpog.repacker.model.export.FullMesh;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import jassimp.AiNode;
import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import jassimp.Jassimp;

public class ModelMerger_DAE {
	public static final JassimpGDXWrapper WRAP = new JassimpGDXWrapper();
	static {
		Jassimp.setWrapperProvider(WRAP);
	}
	private final AiScene scene;
	private final TMD_File basis;

	public static AiScene loadScene(File dae) throws IOException {
		return Jassimp.importFile(dae.getAbsolutePath(), EnumSet.of(AiPostProcessSteps.TRIANGULATE));
	}

	public ModelMerger_DAE(TMD_File basis, AiScene scene) throws IOException {
		this.scene = scene;
		this.basis = basis;
	}

	private void doMeshLayers() {
		this.basis.dLoD.levels = Arrays.copyOf(this.basis.dLoD.levels, 1);
		TMD_DLoD_Level dest = this.basis.dLoD.levels[0];
		for (int i = 0; i < this.scene.getNumMeshes(); i++) {
			FullMesh fm = new FullMesh(this.scene, this.scene.getMeshes().get(i)).clean();
			TMD_Mesh old = dest.members[i];
//			if (old.pieces.length > 1)
//				throw new UnsupportedOperationException("This basis file, " + basis.source
//						+ ", was divided due to its skeletal complexity.  This program doesn't support exporting to this format yet.");
			System.out.println("Migrating mesh...");
			System.out.println("Material Name: \"" + fm.materialName + "\"");
			dest.members[i] = fm.export(basis);
			System.out.println("Migrated mesh " + i + " Old[v=" + old.verts.length + ", t=" + old.totalTriStripLength
					+ "] New[v=" + dest.members[i].verts.length + ", t=" + dest.members[i].totalTriStripLength + "]");
		}
	}

	private void doNodeLayer(AiNode node, Matrix4 root, String idt) {
		Matrix4 world = new Matrix4().set(root).mul(node.getTransform(WRAP));
		if (node.getName().equals("Scene_Root"))
			world.idt();
		for (AiNode c : node.getChildren())
			doNodeLayer(c, world, idt + "|");
		TMD_Node match = basis.nodes.byName(node.getName());
		if (match != null) {
			Vector3 tra = world.getTranslation(new Vector3());
			match.worldSkinningMatrix.setTranslation(new Vector3f(tra.x, tra.y, tra.z));
			// System.out.println(match.node_name);
			// System.out.println(match.worldPosition_Inv);
			match.worldSkinningMatrix_Inv.invert(match.worldSkinningMatrix);
			// System.out.println(match.worldPosition_Inv);
		}
		// System.out.println(scene);
	}

	public void apply() throws IOException {
		doMeshLayers();
		doNodeLayer(scene.getSceneRoot(WRAP), new Matrix4().idt(), "");
	}
}
