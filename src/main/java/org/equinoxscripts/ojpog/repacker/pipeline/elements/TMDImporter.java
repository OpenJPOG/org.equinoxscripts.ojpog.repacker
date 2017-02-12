package org.equinoxscripts.ojpog.repacker.pipeline.elements;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.equinoxscripts.ojpog.io.tkl.TKL_File;
import org.equinoxscripts.ojpog.io.tkl.TKL_Resolver;
import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.repacker.Utils;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElement;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElementDesc;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineException;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;

@PipelineElementDesc(name = "TMD Importer", desc = "Imports a Toshi Model Driver (JPOG Model) file")
public class TMDImporter extends PipelineElement {
	private static final String PROP_SRC = "source";
	private static final String REPO_SRC = "repo";

	public TMDImporter() {
		super(new PipelineFileProperty(PROP_SRC, "Source", "The .tmd file to load").filter(true, false, "tmd"),
				new PipelineFileProperty(REPO_SRC, "Repository Location",
						"The folder to load animation repositories from").filter(true, true));
	}

	@Override
	public TMD_File morph(TMD_File input) throws PipelineException {
		PipelineFileProperty srcProp = super.props.property(PROP_SRC);
		PipelineFileProperty repoProp = super.props.property(REPO_SRC);

		File f = srcProp.file();
		if (f == null)
			throw new PipelineException("No file specified", "You need to specify a file to load");
		if (!f.exists())
			throw new PipelineException("File doesn't exist", "The file you want to load doesn't exist");
		if (f.isDirectory())
			throw new PipelineException("File is directory",
					"The specified path is a directory; you must specify a file");
		ByteBuffer data = Utils.read(f);
		if (data == null)
			throw new PipelineException("Unable to read file", "");

		File repoDefault = f.getParentFile();
		File repoCurr = repoProp.file();

		AtomicReference<PipelineException> resolveFail = new AtomicReference<>();
		TKL_Resolver resolver = new TKL_Resolver() {
			@Override
			public TKL_File resolve(String name) {
				File srcFile = null;

				String fn = name + ".tkl";
				if (repoCurr != null && repoCurr.isDirectory()) {
					File crs = new File(repoCurr, fn);
					if (crs.exists())
						srcFile = crs;
				}
				if (srcFile == null && repoDefault != null && repoDefault.isDirectory()) {
					File crs = new File(repoDefault, fn);
					if (crs.exists())
						srcFile = crs;
				}

				if (srcFile != null)
					try {
						return new TKL_File(Utils.read(srcFile));
					} catch (IOException e) {
						resolveFail.set(new PipelineException("Unable to load animation library",
								"IO Error caused by " + e.getMessage(), e));
					}
				resolveFail.set(new PipelineException("Unable to resolve animation library",
						"Failed to find " + fn + " in " + (repoCurr != null ? "the provided repository"
								: "the model's directory.  Try setting the repository.")));
				return null;
			}
		};

		String source = f.getName().substring(0, f.getName().length() - 4);
		try {
			return new TMD_File(source, data, resolver);
		} catch (Exception e) {
			PipelineException res = resolveFail.get();
			if (res != null)
				throw res;
			throw new PipelineException("Failed to read " + source, "IO Error caused by " + e.getMessage(), e);
		}
	}
}
