package org.equinoxscripts.ojpog.repacker.ui.swt.pipeline;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineBooleanProperty;

public class PipelineBooleanPropertyUI extends PipelinePropertyUI<PipelineBooleanProperty> {

	public PipelineBooleanPropertyUI(PipelineBooleanProperty prop) {
		super(prop);
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	@Override
	protected Composite makeEditorControl(Composite parent, int style) {
		Composite c = new Composite(parent, style);
		c.setLayout(new FormLayout());

		Button check = new Button(c, SWT.CHECK);
		FormData fd = new FormData();
		fd.right = new FormAttachment(100, -GAP);
		fd.top = new FormAttachment(0, GAP);
		fd.bottom = new FormAttachment(100, -GAP);
		check.setLayoutData(fd);
		check.setText("");
		check.setSelection(property.get());
		check.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println(check.getSelection());
				property.set(check.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				property.set(property.defaultValue);
			}
		});
		final Runnable update = () -> {
			if (property.get() != check.getSelection())
				check.setSelection(property.get());
		};
		property.addChangeListener(update);
		parent.addListener(SWT.Dispose, (e) -> {
			property.removeChangeListener(update);
		});
		update.run();
		return c;
	}
}
