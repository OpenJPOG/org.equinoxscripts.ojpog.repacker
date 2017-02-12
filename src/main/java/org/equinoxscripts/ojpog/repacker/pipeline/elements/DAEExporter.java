package org.equinoxscripts.ojpog.repacker.pipeline.elements;

import java.io.File;
import java.io.IOException;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.repacker.model.export.ModelBuilder_DAE;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElement;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElementDesc;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineException;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineBooleanProperty;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;

@PipelineElementDesc(name = "DAE Exporter", desc = "Exports a model to a DAE (Collada) file")
public class DAEExporter extends PipelineElement {
	private static final String PROP_DEST = "dest";
	private static final String PROP_CLEAN = "clean";

	public DAEExporter() {
		super(new PipelineFileProperty(PROP_DEST, "Destination", "The .dae file to export to").filter(false, false,
				"dae"),
				new PipelineBooleanProperty(PROP_CLEAN, "Clean Model", "Merges vertices to ease editing", true));
	}

	@Override
	public TMD_File morph(TMD_File input) throws PipelineException {
		PipelineFileProperty destProp = props.property(PROP_DEST);
		File dest = destProp.file();
		if (dest == null)
			throw new PipelineException("No destination specified", "You have to specify a destination");
		PipelineBooleanProperty cleanProp = props.property(PROP_CLEAN);
		try {
			new ModelBuilder_DAE(dest, input, cleanProp.get());
		} catch (IOException e) {
			throw new PipelineException("Failed to write", "IO Error: " + e.getMessage(), e);
		}
		return input;
	}
}
