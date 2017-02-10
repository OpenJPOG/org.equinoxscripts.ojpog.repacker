package org.equinoxscripts.ojpog.repacker.ui.swt.pipeline;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;

import swing2swt.layout.BorderLayout;

public class PipelineFilePropertyUI extends PipelinePropertyUI<PipelineFileProperty> implements DropTargetListener {
	public PipelineFilePropertyUI(PipelineFileProperty prop) {
		super(prop);
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	@Override
	public Composite makeEditorControl(Composite parent, int style) {
		Composite c = new Composite(parent, style);
		c.setLayout(new BorderLayout(GAP, GAP));
		Text tf = new Text(c, SWT.SINGLE | SWT.BORDER);
		tf.setLayoutData(BorderLayout.CENTER);
		tf.setToolTipText(property.tooltip);

		Button brws = new Button(c, SWT.NONE);
		brws.setText("...");
		brws.setToolTipText("Browse");
		brws.setLayoutData(BorderLayout.EAST);

		property.addChangeListener(() -> {
			if (property.file() != null)
				tf.setText(property.file().getAbsolutePath());
			else
				tf.setText("");
		});

		brws.addListener(SWT.Selection, (e) -> {
			FileDialog fd = new FileDialog(parent.getShell());
			if (property.file() != null)
				fd.setFileName(property.file().getParentFile().getAbsolutePath());
			fd.setText("Select value for " + property.title);
			String out = fd.open();
		});

		Transfer[] types = new Transfer[] { TextTransfer.getInstance(), FileTransfer.getInstance() };

		DropTarget dropTarget1 = new DropTarget(tf, DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE);
		dropTarget1.setTransfer(types);
		dropTarget1.addDropListener(this);
		DropTarget dropTarget2 = new DropTarget(brws, DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE);
		dropTarget2.setTransfer(types);
		dropTarget2.addDropListener(this);
		return c;
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
	}

	@Override
	public void dragLeave(DropTargetEvent event) {
	}

	@Override
	public void dragOperationChanged(DropTargetEvent event) {
	}

	@Override
	public void dragOver(DropTargetEvent event) {
	}

	private File checkAndGet(DropTargetEvent event) {
		System.out.println("Query " + event);
		File ft = null;
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
			String[] paths = (String[]) event.data;
			if (paths != null && paths.length == 1)
				ft = new File(paths[0]);
			event.detail = DND.DROP_LINK;
		} else if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
			String path = (String) event.data;
			if (path != null)
				ft = new File(path);
			event.detail = DND.DROP_COPY;
		}
		if (ft == null || !property.accepts(ft)) {
			event.detail = DND.DROP_NONE;
			return null;
		}
		return ft;
	}

	@Override
	public void drop(DropTargetEvent event) {
		File ft = checkAndGet(event);
		if (ft != null)
			property.set(ft);
	}

	@Override
	public void dropAccept(DropTargetEvent event) {
		// checkAndGet(event);
	}

}
