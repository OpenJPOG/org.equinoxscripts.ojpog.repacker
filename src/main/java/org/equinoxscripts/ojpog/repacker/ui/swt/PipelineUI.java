package org.equinoxscripts.ojpog.repacker.ui.swt;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.repacker.pipeline.ModelPipeline;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElement;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElementDatabase;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineElementDatabase.PipelineElementType;
import org.equinoxscripts.ojpog.repacker.pipeline.PipelineException;
import org.equinoxscripts.ojpog.repacker.ui.swt.pipeline.PipelinePropManager;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;

import swing2swt.layout.BorderLayout;

public class PipelineUI extends Composite {

	private File currentlyOpen = null;
	private ModelPipeline pipeline = new ModelPipeline();
	private PipelineElement element;
	private List pipelineList;
	private ScrolledComposite optionsComposite;
	private StyledText debugText;

	private void loadPipeline(File json) {
		if (!json.isFile()) {
			alertBadChoice("Invalid Choice", json.getAbsolutePath() + " doesn't exist.");
		} else {
			JSONParser jsr = new JSONParser();
			FileReader reader = null;
			try {
				reader = new FileReader(json);
				Object obj = jsr.parse(reader);
				pipeline.unmarshalJSON(obj);
				currentlyOpen = json;
				pipelineChanged();
			} catch (Exception e1) {
				StringWriter sw = new StringWriter();
				e1.printStackTrace(new PrintWriter(sw));
				alertBadChoice("IO Error", "Failed to read pipeline file:\n" + sw.toString());
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e1) {
					}
			}
		}
	}

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public PipelineUI(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new BorderLayout(0, 0));

		ToolBar toolBar = new ToolBar(this, SWT.FLAT | SWT.RIGHT);
		toolBar.setLayoutData(BorderLayout.NORTH);

		ToolItem tltmOpen = new ToolItem(toolBar, SWT.NONE);
		tltmOpen.setText("Open");

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
				return ft;
			}

			@Override
			public void drop(DropTargetEvent event) {
				File ft = checkAndGet(event, true);
				if (ft != null && ft.isFile())
					loadPipeline(ft);
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
			}
		};

		DropTarget dropTarget1 = new DropTarget(tltmOpen.getParent(), DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE);
		dropTarget1.setTransfer(new Transfer[] { TextTransfer.getInstance(), FileTransfer.getInstance() });
		dropTarget1.addDropListener(listener);
		tltmOpen.addListener(SWT.Selection, (e) -> {
			FileDialog fd = new FileDialog(getShell());
			fd.setText("Load Pipeline");
			fd.setFilterExtensions(new String[] { "*.json", "*.*" });
			if (currentlyOpen != null)
				fd.setFileName(currentlyOpen.getAbsolutePath());
			String path = fd.open();
			if (path != null) {
				File json = new File(path);
				loadPipeline(json);
			}
		});

		ToolItem tltmSave = new ToolItem(toolBar, SWT.NONE);
		tltmSave.setText("Save");
		tltmSave.addListener(SWT.Selection, (e) -> {
			FileDialog fd = new FileDialog(getShell());
			fd.setText("Save Pipeline");
			fd.setFilterExtensions(new String[] { "*.json", "*.*" });
			if (currentlyOpen != null)
				fd.setFileName(currentlyOpen.getAbsolutePath());
			String path = fd.open();
			if (path != null) {
				if (!path.toLowerCase().endsWith(".json"))
					path += ".json";
				File json = new File(path);
				FileWriter writer = null;
				try {
					writer = new FileWriter(json);
					((JSONStreamAware) pipeline.marshalJSON()).writeJSONString(writer);
					currentlyOpen = json;
				} catch (Exception e1) {
					StringWriter sw = new StringWriter();
					e1.printStackTrace(new PrintWriter(sw));
					alertBadChoice("IO Error", "Failed to write pipeline file:\n" + sw.toString());
				} finally {
					if (writer != null)
						try {
							writer.close();
						} catch (IOException e1) {
						}
				}
			}
		});

		new ToolItem(toolBar, SWT.SEPARATOR);

		final ToolItem tltmAddType = new ToolItem(toolBar, SWT.DROP_DOWN);
		tltmAddType.setText("Add");
		tltmAddType.setToolTipText("Add an item to the pipeline");
		tltmAddType.addListener(SWT.Selection, new Listener() {
			private Menu menu = new Menu(tltmAddType.getParent().getShell());

			{
				for (PipelineElementType type : PipelineElementDatabase.elements()) {
					final MenuItem item = new MenuItem(menu, SWT.NONE);
					item.setText(type.desc.name());
					item.setData(type);
					item.addListener(SWT.Selection, (e) -> {
						tltmAddType.setText("Add: " + item.getText());
						tltmAddType.setData(type);
						pipeline.pipeline.add(type.make());
						pipelineChanged();
					});
				}
			}

			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.ARROW) {
					ToolItem item = (ToolItem) event.widget;
					Rectangle bs = item.getBounds();
					Point pt = item.getParent().toDisplay(new Point(bs.x, bs.y + bs.height));
					menu.setLocation(pt);
					menu.setVisible(true);
				} else {
					Object ty = tltmAddType.getData();
					if (ty != null && ty instanceof PipelineElementType) {
						PipelineElementType type = (PipelineElementType) ty;
						pipeline.pipeline.add(type.make());
						pipelineChanged();
					}
				}
			}
		});

		new ToolItem(toolBar, SWT.SEPARATOR);

		ToolItem tltmRun = new ToolItem(toolBar, SWT.NONE);
		tltmRun.setText("Run");
		tltmRun.addListener(SWT.Selection, (e) -> runPipeline());

		SashForm root = new SashForm(this, SWT.VERTICAL);
		root.setLayoutData(BorderLayout.CENTER);

		SashForm sashForm = new SashForm(root, SWT.HORIZONTAL);
		sashForm.setLayoutData(BorderLayout.CENTER);

		ScrolledComposite pipelineComposite = new ScrolledComposite(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
		pipelineComposite.setExpandHorizontal(true);
		pipelineComposite.setExpandVertical(true);

		this.pipelineList = new List(pipelineComposite, SWT.BORDER);
		this.pipelineList.addListener(SWT.Selection, (e) -> {
			int idx = this.pipelineList.getSelectionIndex();
			if (idx >= 0) {
				this.element = this.pipeline.pipeline.get(idx);
			} else {
				this.element = null;
			}
			elementChanged();
		});
		final Menu pipelineMenu = new Menu(this.pipelineList);

		final Runnable deleteEvent = () -> {
			int x = this.pipelineList.getSelectionIndex();
			if (x >= 0) {
				this.pipeline.pipeline.remove(x);
				this.pipelineChanged();
			}
		};

		MenuItem delete = new MenuItem(pipelineMenu, SWT.NONE);
		delete.setText("Delete");
		delete.setAccelerator(SWT.DEL);
		delete.addListener(SWT.Selection, (e) -> {
			deleteEvent.run();
		});

		this.pipelineList.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL)
					deleteEvent.run();
			}
		});

		MenuItem moveUp = new MenuItem(pipelineMenu, SWT.NONE);
		moveUp.setText("Move Up");
		moveUp.setAccelerator(SWT.CTRL | 'w');
		moveUp.addListener(SWT.Selection, (e) -> {
			int x = this.pipelineList.getSelectionIndex();
			if (x >= 1) {
				PipelineElement em = this.pipeline.pipeline.remove(x);
				this.pipeline.pipeline.add(x - 1, em);
				this.pipelineChanged();
			}
		});

		MenuItem moveDown = new MenuItem(pipelineMenu, SWT.NONE);
		moveDown.setText("Move Down");
		moveDown.setAccelerator(SWT.CTRL | 's');
		moveDown.addListener(SWT.Selection, (e) -> {
			int x = this.pipelineList.getSelectionIndex();
			if (x < this.pipeline.pipeline.size() - 1) {
				PipelineElement em = this.pipeline.pipeline.remove(x);
				this.pipeline.pipeline.add(x + 1, em);
				this.pipelineChanged();
			}
		});
		this.pipelineList.setMenu(pipelineMenu);
		pipelineMenu.addListener(SWT.Show, (e) -> {
			int selected = this.pipelineList.getSelectionIndex();
			if (selected < 0)
				return;
			PipelineElement pe = this.pipeline.pipeline.get(selected);
			MenuItem[] curr = pipelineMenu.getItems();
			for (int i = 3; i < curr.length; i++) {
				curr[i].dispose();
			}
			moveUp.setEnabled(selected > 0);
			moveDown.setEnabled(selected < this.pipeline.pipeline.size() - 1);
			// Add extras?
		});
		pipelineComposite.setContent(pipelineList);
		pipelineComposite.setMinSize(pipelineList.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		this.optionsComposite = new ScrolledComposite(sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		optionsComposite.setExpandHorizontal(true);
		optionsComposite.setExpandVertical(true);
		sashForm.setWeights(new int[] { 97, 350 });

		this.debugText = new StyledText(root,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
		debugText.setWrapIndent(4);
		debugText.setDoubleClickEnabled(false);
		root.setWeights(new int[] { 221, 53 });
	}

	private void elementChanged() {
		for (Control c : optionsComposite.getChildren())
			c.dispose();
		if (this.element != null)
			this.optionsComposite
					.setContent(PipelinePropManager.uiFor(this.element.props).make(optionsComposite, SWT.NONE));
		else
			this.optionsComposite.setContent(new Composite(this.optionsComposite, SWT.NONE));
	}

	private void pipelineChanged() {
		int ci = this.pipelineList.getSelectionIndex();
		this.pipelineList.removeAll();
		for (int i = 0; i < this.pipeline.pipeline.size(); i++) {
			PipelineElement e = this.pipeline.pipeline.get(i);
			this.pipelineList.add(i + ": " + e.desc().name());
		}
		this.pipelineList.select(Math.min(this.pipeline.pipeline.size() - 1, ci));

		int idx = this.pipelineList.getSelectionIndex();
		if (idx >= 0) {
			this.element = this.pipeline.pipeline.get(idx);
		} else {
			this.element = null;
		}
		elementChanged();
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	private void alertBadChoice(String title, String message) {
		MessageBox box = new MessageBox(getShell(), SWT.OK | SWT.ICON_ERROR | SWT.APPLICATION_MODAL);
		box.setText(title);
		box.setMessage(message);
		box.open();
	}

	private void runPipeline() {
		this.debugText.setText("");

		java.util.List<StyleRange> styleRanges = new ArrayList<>();

		debugText.setText("");

		PrintStream oout = System.out;
		PrintStream oerr = System.err;
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(byte b[], int off, int len) {
				debugText.append(new String(b, off, len));
			}

			@Override
			public void write(int b) throws IOException {
				write(new byte[] { (byte) b }, 0, 1);
			}
		}));
		System.setErr(new PrintStream(new OutputStream() {
			@Override
			public void write(byte b[], int off, int len) {
				String s = new String(b, off, len);
				StyleRange range = new StyleRange();
				range.start = debugText.getCharCount();
				range.length = s.length();
				range.foreground = getDisplay().getSystemColor(SWT.COLOR_RED);
				styleRanges.add(range);
				debugText.append(s);
				debugText.setStyleRanges(styleRanges.toArray(new StyleRange[styleRanges.size()]));
			}

			@Override
			public void write(int b) throws IOException {
				write(new byte[] { (byte) b }, 0, 1);
			}
		}));

		TMD_File stack = null;
		for (int i = 0; i < this.pipeline.pipeline.size(); i++) {
			PipelineElement e = this.pipeline.pipeline.get(i);
			String tag = (i + ": " + e.desc().name());
			this.debugText.append("--Running " + tag + "--\n");

			try {
				stack = e.morph(stack);
			} catch (Throwable err) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				if (err instanceof PipelineException) {
					PipelineException pe = (PipelineException) err;
					pw.println(pe.name);
					pw.println(pe.details);
					err = pe.getCause();
					if (err != null)
						pw.println("Cause: ");
				}
				if (err != null)
					err.printStackTrace(pw);
				StyleRange range = new StyleRange();
				range.start = this.debugText.getCharCount();
				range.length = sw.getBuffer().length();
				range.foreground = this.getDisplay().getSystemColor(SWT.COLOR_RED);
				styleRanges.add(range);
				this.debugText.append(sw.toString() + "\n");
			}
			this.debugText.append("--Finished " + tag + "--\n");
			this.debugText.setStyleRanges(styleRanges.toArray(new StyleRange[styleRanges.size()]));
		}

		System.setOut(oout);
		System.setErr(oerr);
	}
}
