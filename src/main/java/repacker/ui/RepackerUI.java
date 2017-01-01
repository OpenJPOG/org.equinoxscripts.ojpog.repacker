package repacker.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class RepackerUI extends JFrame {
	static {
		System.loadLibrary("64".equals(System.getProperty("sun.arch.data.model")) ? "gdx64" : "gdx");
	}

	private static final long serialVersionUID = 1L;

	private JPanel contentPane;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					RepackerUI frame = new RepackerUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public RepackerUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1500, 768);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane);

		ModelExporter modelExporter = new ModelExporter();
		tabbedPane.addTab("Model Exporter", null, modelExporter,
				"Export *.tmd files into the Collada *.dae or LibGDX *.g3dj format");
		ModelMerger modelMerger = new ModelMerger();
		tabbedPane.addTab("Model Merger", null, modelMerger,
				"Merge a *.tmd file and a *.dae file into a new *.tmd file");
		MaterialLibraryEditor textureEditor = new MaterialLibraryEditor();
		tabbedPane.addTab("Material Editor", null, textureEditor, "Read and write to *.tml material libraries");

		OptionsEditor optionsEditor = new OptionsEditor();
		tabbedPane.addTab("Options Editor", null, optionsEditor, "Edit default options");
	}

}
