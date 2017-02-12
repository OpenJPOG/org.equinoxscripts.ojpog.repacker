package org.equinoxscripts.ojpog.repacker.pipeline.elements;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.repacker.Utils;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElement;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElementDesc;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineException;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;

@PipelineElementDesc(name = "TMD Exporter", desc = "Exports a model to a Toshi Model Driver (JPOG Model) file")
public class TMDExporter extends PipelineElement {
	private static final String PROP_DEST = "dest";

	public TMDExporter() {
		super(new PipelineFileProperty(PROP_DEST, "Destination", "The .tmd file to export to").filter(false, false,
				"tmd"));
	}

	@Override
	public TMD_File morph(TMD_File input) throws PipelineException {
		PipelineFileProperty destProp = props.property(PROP_DEST);
		File dest = destProp.file();
		if (dest == null)
			throw new PipelineException("No destination specified", "You have to specify a destination");
		try {
			ByteBuffer output = ByteBuffer.allocate(input.length()).order(ByteOrder.LITTLE_ENDIAN);
			input.write(output);
			if (output.hasRemaining())
				throw new PipelineException("Failed to write", "Integrity failed on length check");
			output.position(0);
			Utils.write(dest, output);
		} catch (IOException e) {
			throw new PipelineException("Failed to write", "IO Error: " + e.getMessage(), e);
		}
		return input;
	}
}
