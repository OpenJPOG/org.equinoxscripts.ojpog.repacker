package repacker.model.merge;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import jassimp.Jassimp;
import repacker.model.TMD_File;
import repacker.model.export.FullMesh;
import repacker.model.mesh.TMD_DLoD_Level;

public class ModelMerger_DAE {
	public static final JassimpGDXWrapper WRAP = new JassimpGDXWrapper();
	static {
		Jassimp.setWrapperProvider(WRAP);
	}
	private final AiScene scene;
	private final TMD_File basis;

	public ModelMerger_DAE(TMD_File basis, File dae) throws IOException {
		this.scene = Jassimp.importFile(dae.getAbsolutePath(), EnumSet.of(AiPostProcessSteps.TRIANGULATE));
		this.basis = basis;
	}

	public void apply() throws IOException {
		this.basis.dLoD.levels = Arrays.copyOf(this.basis.dLoD.levels, 1);
		TMD_DLoD_Level dest = this.basis.dLoD.levels[0];
		for (int i = 0; i < this.scene.getNumMeshes(); i++) {
			FullMesh fm = new FullMesh(this.scene, this.scene.getMeshes().get(i)).clean();
			dest.members[i] = fm.export(basis);
		}
	}
}
