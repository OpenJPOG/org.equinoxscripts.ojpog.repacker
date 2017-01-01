package repacker.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import repacker.Utils;
import repacker.texture.TML_File;
import repacker.texture.TML_File.TML_Material;
import repacker.texture.TML_Texture;
import repacker.texture.TML_Texture_Format;
import repacker.texture.dds.DDS_File;

public class MaterialLibraryEditor extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final FileNameExtensionFilter TML_EXTENSION = new FileNameExtensionFilter("Toshi Material Library",
			"tml");

	private final ImageIcon upIcon = new ImageIcon(MaterialLibraryEditor.class.getResource("icon/up.png"));
	private final ImageIcon downIcon = new ImageIcon(MaterialLibraryEditor.class.getResource("icon/down.png"));
	private final ImageIcon addIcon = new ImageIcon(MaterialLibraryEditor.class.getResource("icon/add.png"));
	private final ImageIcon delIcon = new ImageIcon(MaterialLibraryEditor.class.getResource("icon/del.png"));
	private final ImageIcon modIcon = new ImageIcon(MaterialLibraryEditor.class.getResource("icon/mod.png"));

	private File sourceFile;
	private File destFile;
	private TML_File materialLibrary;

	private final JList<TML_Material> materialList;
	private final MaterialListModel materialListModel;

	private final JList<TML_Texture> textureList;
	private final TextureListModel textureListModel;
	private final JButton btnSave;
	private final JLabel lblCurrentFile;

	private final TextureDatabaseModel textureDatabaseModel;

	private final JPanel panelMaterial, panelTexture, panelLibrary;

	private final JComboBox<String> drpTextureFormat;

	private final JLabel lblMaterialCaption, lblTextureCaption, lblTextureData;

	private final JButton btnDelMaterial;
	private final JButton btnUpTexture, btnModTexture, btnDownTexture, btnDelTexture;

	private class TextureDatabaseModel extends AbstractListModel<TML_Texture> {
		private static final long serialVersionUID = 1L;
		private List<TML_Texture> backing = new ArrayList<>();

		private void refresh() {
			if (materialLibrary == null) {
				if (backing.isEmpty())
					return;
				int s = backing.size();
				backing = new ArrayList<>();
				super.fireIntervalRemoved(null, 0, s);
			}
			List<TML_Texture> nn = new ArrayList<>(Arrays.asList(materialLibrary.textures));
			// boolean same = backing.equals(nn);
			nn.sort((a, b) -> Integer.compare(a.textureID, b.textureID));
			this.backing = nn;
			// if (!same)
			super.fireContentsChanged(materialLibrary, 0, backing.size());
		}

		@Override
		public int getSize() {
			return backing.size();
		}

		@Override
		public TML_Texture getElementAt(int index) {
			return backing.get(index);
		}
	}

	private class MaterialListModel extends AbstractListModel<TML_Material> {
		private static final long serialVersionUID = 1L;
		private List<TML_Material> backing = new ArrayList<>();

		private void refresh() {
			if (materialLibrary == null) {
				if (backing.isEmpty())
					return;
				int s = backing.size();
				backing.clear();
				super.fireIntervalRemoved(null, 0, s);
			}
			List<TML_Material> nn = new ArrayList<>(materialLibrary.stringMapping.size());
			for (TML_Material s : materialLibrary.stringMapping.values())
				nn.add(s);
			nn.sort((a, b) -> a.name.compareTo(b.name));
			// boolean same = backing.equals(nn);
			this.backing = nn;
			// if (!same)
			super.fireContentsChanged(materialLibrary, 0, backing.size());
		}

		@Override
		public int getSize() {
			return backing.size();
		}

		@Override
		public TML_Material getElementAt(int index) {
			return backing.get(index);
		}
	}

	private class TextureListModel extends AbstractListModel<TML_Texture> {
		private static final long serialVersionUID = 1L;
		private List<TML_Texture> backing = new ArrayList<>();

		private void refresh(TML_Material ref) {
			if (materialLibrary == null || ref == null) {
				if (backing.isEmpty())
					return;
				int s = backing.size();
				backing = new ArrayList<>();
				super.fireIntervalRemoved(null, 0, s);
			}
			List<TML_Texture> nn = new ArrayList<>(Arrays.asList(ref.textures));
			// boolean same = backing.equals(nn);
			this.backing = nn;
			// if (!same)
			super.fireContentsChanged(ref, 0, backing.size());
		}

		@Override
		public int getSize() {
			return backing.size();
		}

		@Override
		public TML_Texture getElementAt(int index) {
			return backing.get(index);
		}
	}

	private static Icon iconify(BufferedImage img, int maxDim) {
		float aspect = img.getWidth() / (float) img.getHeight();
		if (aspect <= 1)
			return new ImageIcon(img.getScaledInstance((int) (aspect * maxDim), maxDim, Image.SCALE_SMOOTH));
		else
			return new ImageIcon(img.getScaledInstance(maxDim, (int) (maxDim / aspect), Image.SCALE_SMOOTH));
	}

	public MaterialLibraryEditor() {

		setLayout(new BorderLayout(2, 2));

		JPanel panelOpenSave = new JPanel();
		add(panelOpenSave, BorderLayout.NORTH);
		panelOpenSave.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 300, 5, 300),
				BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
						BorderFactory.createEmptyBorder(5, 300, 5, 300))));
		panelOpenSave.setLayout(new BorderLayout(0, 0));

		JButton btnOpen = new JButton("Open");
		panelOpenSave.add(btnOpen, BorderLayout.WEST);
		final JFileChooser openUI = new JFileChooser();
		openUI.setFileSelectionMode(JFileChooser.FILES_ONLY);
		openUI.setFileFilter(TML_EXTENSION);
		openUI.setDialogTitle("Open a material library");
		btnOpen.addActionListener((a) -> {
			if (sourceFile != null && sourceFile.exists())
				openUI.setSelectedFile(sourceFile);
			else if (sourceFile != null && sourceFile.getParentFile() != null && sourceFile.getParentFile().exists())
				openUI.setCurrentDirectory(sourceFile.getParentFile());
			else {
				File defdir = SharedUIOptions.instance().file(SharedUIOptions.SOURCE_DIR);
				if (defdir != null) {
					File matlibs = new File(defdir, "matlibs");
					if (matlibs.exists() && matlibs.isDirectory()) {
						openUI.setCurrentDirectory(matlibs);
					}
				}
			}

			if (openUI.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				open(openUI.getSelectedFile());
			}
		});

		this.lblCurrentFile = new JLabel();
		this.lblCurrentFile.setHorizontalAlignment(JLabel.CENTER);
		panelOpenSave.add(lblCurrentFile, BorderLayout.CENTER);

		this.btnSave = new JButton("Save");
		panelOpenSave.add(btnSave, BorderLayout.EAST);

		final JFileChooser saveUI = new JFileChooser();
		saveUI.setFileSelectionMode(JFileChooser.FILES_ONLY);
		saveUI.setFileFilter(TML_EXTENSION);
		saveUI.setDialogTitle("Save a material library");
		btnSave.addActionListener((a) -> {
			if (destFile != null && destFile.exists())
				saveUI.setSelectedFile(destFile);
			else {
				if (destFile != null && destFile.getParentFile() != null && destFile.getParentFile().exists())
					saveUI.setCurrentDirectory(destFile.getParentFile());
				else {
					File defdir = SharedUIOptions.instance().file(SharedUIOptions.DEST_DIR);
					if (defdir != null && defdir.exists()) {
						File matlibs = new File(defdir, "matlibs");
						if (!matlibs.isFile()) {
							matlibs.mkdirs();
							saveUI.setCurrentDirectory(matlibs);
						}
					}
				}
				if (sourceFile != null && sourceFile.exists()) {
					File cwd = saveUI.getCurrentDirectory();
					if (cwd != null) {
						saveUI.setSelectedFile(new File(cwd, sourceFile.getName()));
					}
				}
			}

			if (saveUI.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				save(saveUI.getSelectedFile());
			}
		});
		btnSave.setEnabled(false);

		JPanel editorPane = new JPanel();
		editorPane.setLayout(new GridBagLayout());
		add(editorPane, BorderLayout.CENTER);

		this.panelLibrary = new JPanel();
		this.panelLibrary.setLayout(new BorderLayout(5, 5));
		this.panelLibrary.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));
		editorPane.add(this.panelLibrary, UIUtil.gbc(0, 0, 1, 1, 0, 1));

		JPanel libraryEdit = new JPanel();
		libraryEdit.setLayout(new GridLayout(1, 0, 5, 0));
		libraryEdit.setPreferredSize(new Dimension(45, 20));
		JButton btnAddMaterial = new JButton("+");
		btnAddMaterial.setMargin(new Insets(0, 0, 0, 0));
		btnAddMaterial.setToolTipText("Add material");
		btnAddMaterial.addActionListener((a) -> {
			addMaterial();
		});
		btnDelMaterial = new JButton("-");
		btnDelMaterial.setToolTipText("Delete material");
		btnDelMaterial.addActionListener((a) -> {
			delMaterial();
		});
		btnDelMaterial.setMargin(new Insets(0, 0, 0, 0));
		libraryEdit.add(btnAddMaterial);
		libraryEdit.add(btnDelMaterial);
		panelLibrary.add(libraryEdit, BorderLayout.SOUTH);

		this.materialList = new JList<TML_Material>(this.materialListModel = new MaterialListModel());
		this.materialList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.materialList.setCellRenderer(new ListCellRenderer<TML_Material>() {
			private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();

			@Override
			public Component getListCellRendererComponent(JList<? extends TML_Material> list, TML_Material value,
					int index, boolean isSelected, boolean cellHasFocus) {
				JLabel c = (JLabel) renderer.getListCellRendererComponent(list, value.name, index, isSelected,
						cellHasFocus);
				c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				return c;
			}
		});

		this.materialList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				refreshMaterial();
			}
		});
		panelLibrary.add(new JScrollPane(materialList), BorderLayout.CENTER);
		panelLibrary.setPreferredSize(new Dimension(250, 600));

		panelMaterial = new JPanel();
		panelMaterial.setLayout(new BorderLayout(5, 5));
		panelMaterial.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));
		editorPane.add(panelMaterial, UIUtil.gbc(1, 0, 1, 1, 0, 1));

		panelTexture = new JPanel();
		panelTexture.setLayout(new BorderLayout(5, 5));
		panelTexture.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));
		editorPane.add(panelTexture, UIUtil.gbc(2, 0, 1, 1, 1, 1));

		this.textureListModel = new TextureListModel();
		this.textureList = new JList<TML_Texture>(this.textureListModel);
		this.textureList.setCellRenderer(new ListCellRenderer<TML_Texture>() {
			private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();

			@Override
			public Component getListCellRendererComponent(JList<? extends TML_Texture> list, TML_Texture value,
					int index, boolean isSelected, boolean cellHasFocus) {
				JLabel c = (JLabel) renderer.getListCellRendererComponent(list,
						value == null ? "" : iconify(value.readImage(), 64), index, isSelected, cellHasFocus);
				c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
						BorderFactory.createEmptyBorder(5, 5, 5, 5)));
				StringBuilder label = new StringBuilder();
				label.append("Layer ").append(index);
				label.append(", Reference ");
				if (value != null) {
					label.append(value.textureID);
					label.append(", Format ").append(value.format().properName);
					if (value.format() == TML_Texture_Format.RAW_DDS) {
						DDS_File dds = value.readDDS();
						if ((dds.pxFmtFlags & DDS_File.DDPF_FOURCC) != 0)
							label.append(" [").append(dds.fourCCStr.trim()).append("]");
					}
				} else {
					label.append("null");
				}
				c.setText(label.toString());
				return c;
			}
		});
		this.textureList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.textureList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				refreshTexture();
			}
		});
		panelMaterial.add(new JScrollPane(this.textureList));
		panelMaterial.setPreferredSize(new Dimension(400, 600));

		lblMaterialCaption = new JLabel();
		panelMaterial.add(lblMaterialCaption, BorderLayout.NORTH);

		JPanel materialEdit = new JPanel();
		panelMaterial.add(materialEdit, BorderLayout.SOUTH);
		materialEdit.setPreferredSize(new Dimension(45, 20));
		materialEdit.setLayout(new GridLayout(1, 0, 5, 0));
		JButton btnAddTexture = new JButton(addIcon);
		btnAddTexture.setMargin(new Insets(0, 0, 0, 0));
		btnAddTexture.setToolTipText("Add texture reference");
		btnAddTexture.addActionListener((a) -> {
			addTextureRef();
		});
		btnDelTexture = new JButton(delIcon);
		btnDelTexture.addActionListener((a) -> {
			delTextureRef();
		});
		btnDelTexture.setToolTipText("Delete texture reference");
		btnDelTexture.setMargin(new Insets(0, 0, 0, 0));

		btnUpTexture = new JButton(upIcon);
		btnUpTexture.addActionListener((a) -> {
			moveTextureRef(-1);
		});
		btnUpTexture.setToolTipText("Move texture reference up");
		btnUpTexture.setMargin(new Insets(0, 0, 0, 0));

		btnModTexture = new JButton(modIcon);
		btnModTexture.addActionListener((a) -> {
			changeTextureRef();
		});
		btnModTexture.setToolTipText("Change texture reference");
		btnModTexture.setMargin(new Insets(0, 0, 0, 0));

		btnDownTexture = new JButton(downIcon);
		btnDownTexture.addActionListener((a) -> {
			moveTextureRef(+1);
		});
		btnDownTexture.setToolTipText("Move texture reference down");
		btnDownTexture.setMargin(new Insets(0, 0, 0, 0));

		materialEdit.add(btnAddTexture);
		materialEdit.add(btnUpTexture);
		materialEdit.add(btnModTexture);
		materialEdit.add(btnDownTexture);
		materialEdit.add(btnDelTexture);

		JPanel textureCaption = new JPanel();
		textureCaption.setLayout(new GridLayout(0, 1));
		lblTextureCaption = new JLabel();
		textureCaption.add(lblTextureCaption);
		JPanel btns = new JPanel();
		textureCaption.add(btns);
		btns.setLayout(new GridLayout(1, 0));
		this.drpTextureFormat = new JComboBox<>(
				new String[] { "RGBA_8888", "ARGB_1555", "ARGB_4444", "DDS_DXT3", "DDS_DXT5", "DDS_AUTO" });
		drpTextureFormat.setEditable(false);
		JButton btnExportTexture = new JButton("Export");
		JButton btnImportTexture = new JButton("Import");
		btns.add(btnImportTexture);
		btns.add(drpTextureFormat);
		btns.add(btnExportTexture);
		btnExportTexture.addActionListener((a) -> {
			exportTexture();
		});
		btnImportTexture.addActionListener((a) -> {
			importTexture();
		});
		panelTexture.add(textureCaption, BorderLayout.NORTH);
		lblTextureData = new JLabel();
		panelTexture.add(lblTextureData);
		this.textureDatabaseModel = new TextureDatabaseModel();

		refreshLibrary();
	}

	private void open(File f) {
		if (!f.exists()) {
			UIUtil.alert("File does not exist\n" + f.getAbsolutePath(), "Failed to load", this);
			return;
		}
		try {
			ByteBuffer data = Utils.read(f);
			materialLibrary = new TML_File(data);
			sourceFile = f;
			lblCurrentFile.setText(f.getName());
			btnSave.setEnabled(true);
			refreshLibrary();
		} catch (Exception e) {
			UIUtil.alert("Failed to load", e, this);
			return;
		}
	}

	private void refreshLibrary() {
		this.materialListModel.refresh();
		this.textureDatabaseModel.refresh();
		refreshMaterial();
		disableRecurse(panelLibrary, materialLibrary == null);
	}

	private void disableRecurse(Container c, boolean disable) {
		for (int i = 0; i < c.getComponentCount(); i++) {
			Component o = c.getComponent(i);
			if (o instanceof Container)
				disableRecurse((Container) o, disable);
			else
				o.setEnabled(!disable);
		}
		c.setEnabled(!disable);
	}

	private void refreshMaterial() {
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		disableRecurse(panelMaterial, mtl == null);
		this.textureListModel.refresh(mtl);
		if (mtl == null) {
			lblMaterialCaption.setText("");
		} else {
			StringBuilder label = new StringBuilder();
			label.append("Material \"").append(mtl.name).append("\", with ").append(mtl.textures.length)
					.append(" textures");
			lblMaterialCaption.setText(label.toString());
		}
		btnDelMaterial.setEnabled(mtlID >= 0 && mtlID < this.materialListModel.getSize());
		refreshTexture();
	}

	private void refreshTexture() {
		int texID = this.textureList.getSelectedIndex();
		TML_Texture tex = texID >= 0 && texID < this.textureListModel.getSize()
				? this.textureListModel.getElementAt(texID) : null;
		disableRecurse(panelTexture, tex == null);

		if (tex == null) {
			lblTextureCaption.setText("");
			lblTextureData.setIcon(null);
		} else {
			StringBuilder label = new StringBuilder();
			label.append("<html>ID ").append(tex.textureID);
			label.append(", Format ").append(tex.format().properName);
			if (tex.format() == TML_Texture_Format.RAW_DDS) {
				DDS_File dds = tex.readDDS();
				if ((dds.pxFmtFlags & DDS_File.DDPF_FOURCC) != 0)
					label.append(" [").append(dds.fourCCStr.trim()).append("]");
			}
			label.append("<br>");
			label.append("Used by: ");
			Set<String> usage = new HashSet<>();
			for (TML_Material m : materialLibrary.stringMapping.values())
				for (TML_Texture t : m.textures)
					if (t == tex)
						usage.add(m.name);
			label.append(usage);
			label.append("</html>");
			lblTextureCaption.setText(label.toString());
			lblTextureData.setIcon(iconify(tex.readImage(), 512));
			if (tex.format() == TML_Texture_Format.RGBA_8888)
				drpTextureFormat.setSelectedItem("RGBA_8888");
			else if (tex.format() == TML_Texture_Format.RGBA_8888)
				drpTextureFormat.setSelectedItem("ARGB_1555");
			else if (tex.format() == TML_Texture_Format.RGBA_8888)
				drpTextureFormat.setSelectedItem("ARGB_4444");
			else if (tex.format() == TML_Texture_Format.RAW_DDS) {
				String cc4 = tex.readDDS().fourCCStr;
				if (cc4.equals("DXT3"))
					drpTextureFormat.setSelectedItem("DDS_DXT3");
				else if (cc4.equals("DXT5"))
					drpTextureFormat.setSelectedItem("DDS_DXT5");
			}
		}
		btnDownTexture.setEnabled(texID + 1 < this.textureListModel.getSize());
		btnUpTexture.setEnabled(texID - 1 >= 0);

		boolean ht = texID >= 0 && texID < this.textureListModel.getSize();
		btnModTexture.setEnabled(ht);
		btnDelTexture.setEnabled(ht);
	}

	private void save(File f) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(materialLibrary.length()).order(ByteOrder.LITTLE_ENDIAN);
			materialLibrary.write(buffer);
			buffer.flip();
			Utils.write(f, buffer);
		} catch (Exception e) {
			UIUtil.alert("Failed to write", e, this);
		}
	}

	private void addTextureRef() {
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		if (mtl != null) {
			int texID = this.textureList.getSelectedIndex();
			if (texID >= 0 && texID < this.textureListModel.getSize()) {
				TML_Texture[] fs = new TML_Texture[mtl.textures.length + 1];
				int j = 0;
				for (int i = 0; i < mtl.textures.length; i++) {
					if (i == texID)
						fs[j++] = null;
					fs[j++] = mtl.textures[i];
				}
				mtl.textures = fs;
				this.textureList.setSelectedIndex(texID + 1);
			} else {
				mtl.textures = Arrays.copyOf(mtl.textures, mtl.textures.length + 1);
				mtl.textures[mtl.textures.length - 1] = null;
			}
			refreshMaterial();
		}
	}

	private void delTextureRef() {
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		if (mtl != null) {
			int texID = this.textureList.getSelectedIndex();
			TML_Texture tex = texID >= 0 && texID < this.textureListModel.getSize()
					? this.textureListModel.getElementAt(texID) : null;
			if (JOptionPane.showConfirmDialog(this,
					"Are you sure you want to remove reference " + texID + " to texture "
							+ (tex == null ? "null" : tex.textureID) + " from material " + mtl.name + "?",
					"Remove Reference", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				TML_Texture[] fs = new TML_Texture[mtl.textures.length - 1];
				int h = 0;
				for (int i = 0; i < mtl.textures.length; i++)
					if (i != texID)
						fs[h++] = mtl.textures[i];
				mtl.textures = fs;
				refreshMaterial();
				this.textureList.clearSelection();
			}
		}
	}

	private void moveTextureRef(int dir) {
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		if (mtl != null) {
			int texID = this.textureList.getSelectedIndex();
			if (texID >= 0 && texID < this.textureListModel.getSize() && texID + dir >= 0
					&& texID + dir < this.textureListModel.getSize()) {
				TML_Texture tmp = mtl.textures[texID];
				mtl.textures[texID] = mtl.textures[texID + dir];
				mtl.textures[texID + dir] = tmp;
				refreshMaterial();
				this.textureList.setSelectedIndex(texID + dir);
			}
		}
	}

	private void changeTextureRef() {
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		if (mtl != null) {
			int texID = this.textureList.getSelectedIndex();
			if (texID >= 0 && texID < this.textureListModel.getSize()) {
				TextureBrowser browse = new TextureBrowser();
				if (browse.show(UIUtil.frameOf(this),
						this.textureListModel.getElementAt(texID)) == JOptionPane.OK_OPTION
						&& browse.selected() != null) {
					mtl.textures[texID] = browse.selected();
					refreshMaterial();
				}
			}
		}
	}

	private class TextureBrowser {
		private JList<TML_Texture> lst;
		private JDialog dialog;

		private final AtomicInteger state = new AtomicInteger(0);

		@SuppressWarnings("deprecation")
		private void makeDialog(Frame owner, TML_Texture sel) {
			dialog = new JDialog(owner, true);
			dialog.setLayout(new BorderLayout());
			dialog.setTitle("Texture Browser");
			dialog.setSize(500, 500);

			lst = new JList<>(MaterialLibraryEditor.this.textureDatabaseModel);
			lst.setVisibleRowCount(-1);
			lst.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			lst.setPreferredSize(new Dimension(400, 400));
			lst.setCellRenderer(new ListCellRenderer<TML_Texture>() {
				private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();

				@Override
				public Component getListCellRendererComponent(JList<? extends TML_Texture> list, TML_Texture value,
						int index, boolean isSelected, boolean cellHasFocus) {
					JLabel c = (JLabel) renderer.getListCellRendererComponent(list,
							value == null ? "" : iconify(value.readImage(), 128), index, isSelected, cellHasFocus);
					c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
							BorderFactory.createEmptyBorder(5, 5, 5, 5)));
					// StringBuilder label = new StringBuilder();
					// label.append("ID ");
					// label.append(value.textureID);
					// label.append(", Format
					// ").append(value.format().properName);
					// if (value.format() == TML_Texture_Format.RAW_DDS) {
					// DDS_File dds = value.readDDS();
					// if ((dds.pxFmtFlags & DDS_File.DDPF_FOURCC) != 0)
					// label.append("
					// [").append(dds.fourCCStr.trim()).append("]");
					// }
					// c.setText(label.toString());
					return c;
				}
			});
			dialog.add(new JScrollPane(lst), BorderLayout.CENTER);
			JPanel caption = new JPanel();
			caption.setLayout(new GridLayout(1, 0));

			final JButton okay = new JButton("OK");
			okay.addActionListener((a) -> {
				state.set(1);
				dialog.hide();
			});
			okay.setEnabled(false);
			lst.addListSelectionListener((e) -> {
				okay.setEnabled(lst.getSelectedIndex() >= 0 && lst.getSelectedIndex() < lst.getModel().getSize());
			});
			JButton add = new JButton(addIcon);
			add.addActionListener((a) -> {
				TML_File mtl = MaterialLibraryEditor.this.materialLibrary;
				int freeID = 0;
				for (TML_Texture t : mtl.textures)
					if (t != null)
						freeID = Math.max(t.textureID + 1, freeID);
				mtl.textures = Arrays.copyOf(mtl.textures, mtl.textures.length + 1);
				mtl.textures[mtl.textures.length - 1] = new TML_Texture(freeID);
				MaterialLibraryEditor.this.textureDatabaseModel.refresh();
				lst.setSelectedValue(mtl.textures[mtl.textures.length - 1], true);
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener((a) -> {
				state.set(2);
				dialog.hide();
			});

			caption.add(okay);
			caption.add(add);
			caption.add(cancel);

			dialog.add(caption, BorderLayout.SOUTH);

			if (sel == null)
				lst.clearSelection();
			else
				lst.setSelectedValue(sel, true);
		}

		public TML_Texture selected() {
			int id = lst.getSelectedIndex();
			if (id >= 0 && id < lst.getModel().getSize())
				return lst.getModel().getElementAt(id);
			return null;
		}

		@SuppressWarnings("deprecation")
		private int show(Frame owner, TML_Texture sel) {
			if (dialog != null)
				throw new IllegalStateException("Already have a dialog");
			state.set(0);
			makeDialog(owner, sel);
			dialog.show();
			dialog = null;
			int state = this.state.get();
			return state == 1 ? JOptionPane.OK_OPTION : JOptionPane.CANCEL_OPTION;
		}
	}

	private void exportTexture() {
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		if (mtl != null) {
			int texID = this.textureList.getSelectedIndex();
			TML_Texture tex = texID >= 0 && texID < this.textureListModel.getSize()
					? this.textureListModel.getElementAt(texID) : null;
			if (tex != null) {
				JFileChooser exp = new JFileChooser();
				exp.setDialogTitle("Select destination");
				exp.setFileSelectionMode(JFileChooser.FILES_ONLY);
				exp.setMultiSelectionEnabled(false);
				if (exp.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					File out = exp.getSelectedFile();
					if (!out.getName().contains("."))
						out = new File(out.getAbsolutePath() + ".png");
					String ext = out.getName().substring(out.getName().lastIndexOf('.') + 1);
					try {
						ImageIO.write(tex.readImage(), ext.toUpperCase(), out);
					} catch (Exception e) {
						UIUtil.alert("Failed to write", e, this);
					}
				}
			}
		}
	}

	private void importTexture() {
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		if (mtl != null) {
			int texID = this.textureList.getSelectedIndex();
			TML_Texture tex = texID >= 0 && texID < this.textureListModel.getSize()
					? this.textureListModel.getElementAt(texID) : null;
			if (tex != null) {
				JFileChooser exp = new JFileChooser();
				exp.setDialogTitle("Select source");
				exp.setFileSelectionMode(JFileChooser.FILES_ONLY);
				exp.setMultiSelectionEnabled(false);
				if (exp.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					BufferedImage image = null;
					try {
						image = ImageIO.read(exp.getSelectedFile());
					} catch (Exception e) {
						UIUtil.alert("Failed to read", e, this);
					}
					if (image != null) {
						TML_Texture_Format format = TML_Texture_Format.ARGB_4444;
						String fourCC = null;
						String fmt = (String) drpTextureFormat.getSelectedItem();
						if (fmt == null)
							fmt = "DDS_AUTO";
						if (fmt.equals("RGBA_8888"))
							format = TML_Texture_Format.RGBA_8888;
						else if (fmt.equals("ARGB_1555"))
							format = TML_Texture_Format.ARGB_1555;
						else if (fmt.equals("ARGB_4444"))
							format = TML_Texture_Format.ARGB_4444;
						else if (fmt.equals("DDS_DXT3")) {
							format = TML_Texture_Format.RAW_DDS;
							fourCC = "DXT3";
						} else if (fmt.equals("DDS_DXT5")) {
							format = TML_Texture_Format.RAW_DDS;
							fourCC = "DXT5";
						} else if (fmt.equals("DDS_DXT35")) {
							format = TML_Texture_Format.RAW_DDS;
							fourCC = "DXT35";
						}
						try {
							if (fourCC != null) {
								tex.writeDDS(image, fourCC);
							} else {
								tex.writeImage(format, image);
							}
							refreshMaterial();
						} catch (Exception e) {
							UIUtil.alert("Failed to write", e, this);
						}
					}
				}
			}
		}
	}

	private void addMaterial() {
		if (this.materialLibrary == null)
			return;
		String nam = JOptionPane.showInputDialog(this, "Material name?");
		if (nam == null || nam.trim().length() == 0)
			return;
		if (this.materialLibrary.stringMapping.keySet().contains(nam)) {
			UIUtil.alert("Failed to create material; one by that name already exists", "", this);
			return;
		}
		boolean hasStr = false;
		for (String s : this.materialLibrary.stringTable)
			if (s.equals(nam)) {
				hasStr = true;
				break;
			}
		if (!hasStr) {
			int op = this.materialLibrary.stringTable.length;
			this.materialLibrary.stringTable = Arrays.copyOf(this.materialLibrary.stringTable, op + 1);
			this.materialLibrary.stringTable[op] = nam;
		}
		TML_Material mat = this.materialLibrary.new TML_Material(nam);
		this.materialLibrary.stringMapping.put(nam, mat);
		refreshLibrary();
	}

	private void delMaterial() {
		if (this.materialLibrary == null)
			return;
		int mtlID = this.materialList.getSelectedIndex();
		TML_Material mtl = mtlID >= 0 && mtlID < this.materialListModel.getSize()
				? this.materialListModel.getElementAt(mtlID) : null;
		if (mtl != null) {
			if (JOptionPane.showConfirmDialog(this, "Are you sure you want to remove material " + mtl.name + "?",
					"Remove Reference", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				this.materialLibrary.stringMapping.remove(mtl.name);
				refreshLibrary();
			}
		}
	}
}
