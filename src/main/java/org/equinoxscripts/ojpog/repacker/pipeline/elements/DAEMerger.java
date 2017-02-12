package org.equinoxscripts.ojpog.repacker.pipeline.elements;

import java.io.File;
import java.io.IOException;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.repacker.model.merge.ModelMerger_DAE;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElement;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElementDesc;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineException;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;

@PipelineElementDesc(name = "DAE Merger", desc = "Merges a DAE file into the pipeline")
public class DAEMerger extends PipelineElement {
	private static final String PROP_SRC = "source";

	public DAEMerger() {
		super(new PipelineFileProperty(PROP_SRC, "Source", "The .dae file to load").filter(true, false, "dae"));
	}

	@Override
	public TMD_File morph(TMD_File input) throws PipelineException {
		PipelineFileProperty srcProp = super.props.property(PROP_SRC);

		File f = srcProp.file();
		if (f == null)
			throw new PipelineException("No file specified", "You need to specify a file to load");

		try {
			ModelMerger_DAE merge = new ModelMerger_DAE(input, ModelMerger_DAE.loadScene(f));
			merge.apply();
			input.updateIntegrity();
		} catch (IOException e) {
			throw new PipelineException("Failed to apply", "The merger failed to operate correctly", e);
		}
		return input;
	}

}
