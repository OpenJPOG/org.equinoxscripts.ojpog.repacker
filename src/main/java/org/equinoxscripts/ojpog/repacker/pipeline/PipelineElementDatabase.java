package org.equinoxscripts.ojpog.repacker.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.equinoxscripts.ojpog.repacker.pipeline.elements.DAEExporter;
import org.equinoxscripts.ojpog.repacker.pipeline.elements.DAEMerger;
import org.equinoxscripts.ojpog.repacker.pipeline.elements.TMDExporter;
import org.equinoxscripts.ojpog.repacker.pipeline.elements.TMDImporter;

public class PipelineElementDatabase {
	private static final List<PipelineElementType> elements = new ArrayList<>();

	public static class PipelineElementType {
		public final Class<? extends PipelineElement> type;
		public final PipelineElementDesc desc;

		public PipelineElementType(Class<? extends PipelineElement> clazz) {
			this.type = clazz;
			this.desc = clazz.getAnnotation(PipelineElementDesc.class);
		}

		public PipelineElement make() {
			try {
				return type.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	public static List<PipelineElementType> elements() {
		return Collections.unmodifiableList(elements);
	}

	private static void add(Class<? extends PipelineElement> clazz) {
		elements.add(new PipelineElementType(clazz));
	}

	static {
		add(TMDImporter.class);
		add(TMDExporter.class);
		add(DAEExporter.class);
		add(DAEMerger.class);
	}
}
