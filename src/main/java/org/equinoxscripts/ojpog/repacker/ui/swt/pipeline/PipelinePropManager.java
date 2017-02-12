package org.equinoxscripts.ojpog.repacker.ui.swt.pipeline;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineBooleanProperty;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineGroupProperty;

public class PipelinePropManager {
	@SuppressWarnings("rawtypes")
	public static final Map<Class, Class> PIPELINE_PROP_UI = new HashMap<>();

	private static <R> void register(Class<R> prop, Class<? extends PipelinePropertyUI<? super R>> ui) {
		PIPELINE_PROP_UI.put(prop, ui);
	}

	static {
		register(PipelineFileProperty.class, PipelineFilePropertyUI.class);
		register(PipelineGroupProperty.class, PipelineGroupPropertyUI.class);
		register(PipelineBooleanProperty.class, PipelineBooleanPropertyUI.class);
	}

	@SuppressWarnings("unchecked")
	public static <R> PipelinePropertyUI<? super R> uiFor(R v) {
		Class<? extends Object> clazz = v.getClass();
		while (clazz != null) {
			@SuppressWarnings("rawtypes")
			Class ui = PIPELINE_PROP_UI.get(clazz);
			if (ui != null) {
				try {
					return (PipelinePropertyUI<? super R>) ui.getConstructor(clazz).newInstance(v);
				} catch (NoSuchMethodException e) {
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | SecurityException e) {
					e.printStackTrace();
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}
}
