package org.equinoxscripts.ojpog.repacker.ui.swt.pipeline;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineGroupProperty;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineProperty;

public class PipelineGroupPropertyUI extends PipelinePropertyUI<PipelineGroupProperty> {
	public static final int EDGE_PAD = 4;
	public static final int TOP_PAD = 2;
	public static final int ROW_PAD = 4;

	public PipelineGroupPropertyUI(PipelineGroupProperty prop) {
		super(prop);
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	@Override
	public Composite make(Composite parent, int style) {
		Group c = new Group(parent, style);
		c.setText(property.title);
		c.setToolTipText(property.tooltip);
		groupLayout(c, property.properties());
		return c;
	}

	public static void groupLayout(Composite c, Collection<PipelineProperty> props) {
		if (c.getLayout() == null || !(c.getLayout() instanceof FormLayout))
			c.setLayout(new FormLayout());

		for (Control chi : c.getChildren())
			chi.dispose();

		Control tail = null;
		for (PipelineProperty ps : props) {
			PipelinePropertyUI<?> prop = PipelinePropManager.uiFor(ps);
			Composite child = prop.make(c, SWT.NONE);
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, EDGE_PAD);
			fd.right = new FormAttachment(100, -EDGE_PAD);
			if (tail != null)
				fd.top = new FormAttachment(tail, ROW_PAD, SWT.BOTTOM);
			else
				fd.top = new FormAttachment(TOP_PAD);
			child.setLayoutData(fd);
			tail = child;
		}
	}
}
