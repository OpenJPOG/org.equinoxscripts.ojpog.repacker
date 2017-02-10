package org.equinoxscripts.ojpog.repacker.ui.swt.pipeline;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineProperty;

import swing2swt.layout.BorderLayout;

public abstract class PipelinePropertyUI<T extends PipelineProperty> {
	public static final int GAP = 4;

	public final T property;

	public PipelinePropertyUI(T prop) {
		this.property = prop;
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	public Composite make(Composite parent, int style) {
		Composite c = new Composite(parent, style);
		c.setLayout(new BorderLayout(GAP, GAP));

		Composite dummy = new Composite(c, SWT.NULL);
		dummy.setLayoutData(BorderLayout.WEST);
		dummy.setLayout(new FormLayout());

		Label lbl = new Label(dummy, SWT.NULL);
		FormData fd = new FormData();
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.top = new FormAttachment(0,2);
		lbl.setLayoutData(fd);
		lbl.setText(property.title);
		lbl.setToolTipText(property.tooltip);

		Composite cts = makeEditorControl(c, SWT.NONE);
		if (cts != null)
			cts.setLayoutData(BorderLayout.CENTER);
		return c;
	}

	protected Composite makeEditorControl(Composite parent, int style) {
		return null;
	}
}
