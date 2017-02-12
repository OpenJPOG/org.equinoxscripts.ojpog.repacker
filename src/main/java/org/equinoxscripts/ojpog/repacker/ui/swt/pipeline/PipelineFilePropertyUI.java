package org.equinoxscripts.ojpog.repacker.ui.swt.pipeline;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;

import swing2swt.layout.BorderLayout;

public class PipelineFilePropertyUI extends PipelinePropertyUI<PipelineFileProperty> {
	public PipelineFilePropertyUI(PipelineFileProperty prop) {
		super(prop);
	}

	private static final Color RED = new Color(null, 255, 0, 0);
	private static final Color GREEN = new Color(null, 0, 255, 0);

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
		// tf.setEditable(false);
		boolean[] bad = new boolean[1];
		AtomicBoolean verify = new AtomicBoolean();
		tf.addVerifyListener((e) -> {
			verify.set(true);
			try {
				String ntext = "";
				if (e.start > 0)
					ntext += tf.getText().substring(0, e.start);
				ntext += e.text;
				if (e.end < tf.getText().length())
					ntext += tf.getText().substring(e.end);
				File f = new File(ntext);
				bad[0] = !property.accepts(f);
				if (!bad[0])
					property.set(f);
			} catch (Exception err) {
				e.doit = false;
				bad[0] = true;
			}
			verify.set(false);
		});
		tf.addPaintListener((e) -> {
			if (tf.getText().length() > 0) {
				GC gc = e.gc;
				gc.setForeground(bad[0] ? RED : GREEN);
				Rectangle rect = gc.getClipping();
				Rectangle rect1 = new Rectangle(rect.x, rect.y, rect.width - 1, rect.height - 1);
				gc.drawRectangle(rect1);
			}
		});

		Button brws = new Button(c, SWT.NONE);
		brws.setText("...");
		brws.setToolTipText("Browse");
		brws.setLayoutData(BorderLayout.EAST);

		final Runnable update = () -> {
			String want;
			if (property.file() != null)
				want = property.file().getAbsolutePath();
			else
				want = "";
			if (!verify.get())
				if (!tf.getText().equals(want))
					tf.setText(want);
		};
		property.addChangeListener(update);
		update.run();
		parent.addListener(SWT.Dispose, (e) -> {
			property.removeChangeListener(update);
		});

		brws.addListener(SWT.Selection, (e) -> {
			String out;
			if (property.acceptsDirectory) {
				DirectoryDialog fd = new DirectoryDialog(parent.getShell());
				// if (property.file() != null)
				// fd.setDirectoryName(property.file().getParentFile().getAbsolutePath());
				fd.setText("Select value for " + property.title);
				out = fd.open();
			} else {
				FileDialog fd = new FileDialog(parent.getShell());
				if (property.file() != null)
					fd.setFileName(property.file().getAbsolutePath());
				fd.setText("Select value for " + property.title);
				if (property.acceptedExtensions != null && property.acceptedExtensions.length > 0) {
					StringBuilder extf = new StringBuilder();
					for (String s : property.acceptedExtensions) {
						if (extf.length() > 0)
							extf.append(';');
						extf.append("*.").append(s);
					}
					fd.setFilterExtensions(new String[] { extf.toString(), "*.*" });
				}
				out = fd.open();
			}
			if (out != null) {
				File fp = new File(out);
				if (!property.accepts(fp))
					alertBadChoice(c.getShell(), fp);
				else
					property.set(fp);
			}
		});

		final DropTargetListener listener = new DropTargetListener() {
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

			private File checkAndGet(DropTargetEvent event, boolean debug) {
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
				if (ft == null) {
					event.detail = DND.DROP_NONE;
					return null;
				}
				if (!property.accepts(ft)) {
					event.detail = DND.DROP_NONE;
					alertBadChoice(c.getShell(), ft);
					return null;
				}
				return ft;
			}

			@Override
			public void drop(DropTargetEvent event) {
				File ft = checkAndGet(event, true);
				if (ft != null)
					property.set(ft);
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
			}
		};

		for (Control ct : new Control[] { tf, brws }) {
			DropTarget dropTarget1 = new DropTarget(ct, DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE);
			dropTarget1.setTransfer(new Transfer[] { TextTransfer.getInstance(), FileTransfer.getInstance() });
			dropTarget1.addDropListener(listener);
		}
		return c;
	}

	private void alertBadChoice(Shell sc, File f) {
		MessageBox box = new MessageBox(sc, SWT.OK | SWT.ICON_ERROR | SWT.APPLICATION_MODAL);
		String debug = property.acceptDebug(f);
		box.setText("Invalid Choice");
		box.setMessage(f.getName() + " isn't supported:\n" + debug);
		box.open();
	}
}
