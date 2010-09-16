/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.editors;

import hu.openig.Setup;
import hu.openig.core.Act;
import hu.openig.core.Configuration;
import hu.openig.core.Labels;
import hu.openig.core.Location;
import hu.openig.core.PlanetType;
import hu.openig.core.ResourceLocator;
import hu.openig.core.RoadType;
import hu.openig.core.Sides;
import hu.openig.core.Tile;
import hu.openig.editors.ImportDialog.OriginalBuilding;
import hu.openig.gfx.ColonyGFX;
import hu.openig.model.Building;
import hu.openig.model.BuildingModel;
import hu.openig.model.BuildingType;
import hu.openig.model.GalaxyModel;
import hu.openig.model.PlanetSurface;
import hu.openig.model.Resource;
import hu.openig.model.SurfaceEntity;
import hu.openig.model.SurfaceEntityType;
import hu.openig.model.SurfaceFeature;
import hu.openig.model.TileSet;
import hu.openig.render.TextRenderer;
import hu.openig.utils.ImageUtils;
import hu.openig.utils.JavaUtils;
import hu.openig.utils.XML;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.w3c.dom.Element;

/**
 * Map editor.
 * @author karnokd, 2010.09.08.
 * @version $Revision 1.0$
 */
public class MapEditor extends JFrame {
	/** */
	private static final long serialVersionUID = -5949479655359917254L;
	/** The minimum memory required to run Open-IG. */
	private static final long MINIMUM_MEMORY = 384L;
	/** The map editor's JAR file version. */
	public static final String MAP_EDITOR_JAR_VERSION = "0.3";
	/** The title text. */
	public static final String TITLE = "Open-IG MapEditor v" + MAP_EDITOR_JAR_VERSION;
	/** The main resource locator. */
	ResourceLocator rl;
	/** The horizontal split between the tool listing and the rendering. */
	JSplitPane split;
	/** The tool splitting. */
	JSplitPane toolSplit;
	/** The map renderer. */
	MapRenderer renderer;
	/** The galaxy model. */
	GalaxyModel galaxyModel;
	/** The surface tiles. */
	JTable surfaceTable;
	/** The surface tile table model. */
	TileList surfaceTableModel;
	/** The building tiles. */
	JTable buildingTable;
	/** The building tile table model. */
	TileList buildingTableModel;
	/** Preview splitter. */
	JSplitPane featuresSplit;
	/** The preview image. */
	ImagePaint preview;
	/** The building model. */
	BuildingModel buildingModel;
	/** Filter surface table. */
	JTextField filterSurface;
	/** Filter buildings table. */
	JTextField filterBuilding;
	/** The row sorter for the surface table. */
	TableRowSorter<TileList> surfaceSorter;
	/** The row sorter for the building sorter. */
	TableRowSorter<TileList> buildingSorter;
	/** The colony graphics. */
	ColonyGFX colonyGFX;
	/** The current alpha level. */
	float alpha = 1.0f;
	/** The import dialog. */
	ImportDialog imp;
	/** The current base tile object. */
	SurfaceFeature currentBaseTile;
	/** The current building type object. */
	BuildingType currentBuildingType;
	/** The current building race. */
	String currentBuildingRace;
	/** The configuration object. */
	final Configuration config;
	/** The building that is currently under edit. */
	Building currentBuilding;
	/** The current save settings. */
	MapSaveSettings saveSettings;
	/** The alpha slider. */
	JSlider alphaSlider;
	/** The labels. */
	Labels labels;
	/** The User Interface elements to rename. */
	class UIElements {
		/** Show/hide buildings. */
		@Rename(to = "mapeditor.show_hide_buildings")
		JCheckBoxMenuItem viewShowBuildings;
		/** Are we in placement mode? */
		@Rename(to = "mapeditor.object_placement_mode")
		JToggleButton buildButton;
		/** Is the placement mode active? */
		@Rename(to = "mapeditor.object_placement_mode")
		JCheckBoxMenuItem editPlaceMode;
		/** View buildings in minimap mode. */
		@Rename(to = "mapeditor.symbolic_buildings")
		JCheckBoxMenuItem viewSymbolicBuildings;
		/** View text naming backgrounds. */
		@Rename(to = "mapeditor.view_text_background")
		JCheckBoxMenuItem viewTextBackgrounds;
		/** The building info panel. */
		BuildingInfoPanel buildingInfoPanel;
		/** The allocation panel. */
		AllocationPanel allocationPanel;
		/** Place roads of a race around the buildings. */
		@Rename(to = "mapeditor.place_roads")
		JMenu editPlaceRoads;
		/** File menu. */
		@Rename(to = "mapeditor.file")
		JMenu fileMenu;
		/** Edit menu. */
		@Rename(to = "mapeditor.edit")
		JMenu editMenu;
		/** View menu. */
		@Rename(to = "mapeditor.view")
		JMenu viewMenu;
		/** Help menu. */
		@Rename(to = "mapeditor.help")
		JMenu helpMenu;
		/** File new. */
		@Rename(to = "mapeditor.file_new")
		JMenuItem fileNew;
		/** File open. */
		@Rename(to = "mapeditor.file_open")
		JMenuItem fileOpen;
		/** File import. */
		@Rename(to = "mapeditor.file_import")
		JMenuItem fileImport;
		/** File save. */
		@Rename(to = "mapeditor.file_save")
		JMenuItem fileSave;
		/** File save as. */
		@Rename(to = "mapeditor.file_save_as")
		JMenuItem fileSaveAs;
		/** File exit. */
		@Rename(to = "mapeditor.file_exit")
		JMenuItem fileExit;
		/** Edit undo. */
		@Rename(to = "mapeditor.edit_undo")
		JMenuItem editUndo;
		/** Edit redo. */
		@Rename(to = "mapeditor.edit_redo")
		JMenuItem editRedo;
		/** Edit cut. */
		@Rename(to = "mapeditor.edit_cut")
		JMenuItem editCut;
		/** Edit copy. */
		@Rename(to = "mapeditor.edit_copy")
		JMenuItem editCopy;
		/** Edit paste. */
		@Rename(to = "mapeditor.edit_paste")
		JMenuItem editPaste;
		/** Edit delete building. */
		@Rename(to = "mapeditor.edit_delete_building")
		JMenuItem editDeleteBuilding;
		/** Edit delete surface. */
		@Rename(to = "mapeditor.edit_delete_surface")
		JMenuItem editDeleteSurface;
		/** Edit delete both. */
		@Rename(to = "mapeditor.edit_delete_both")
		JMenuItem editDeleteBoth;
		/** Edit clear buildings. */
		@Rename(to = "mapeditor.edit_clear_buildings")
		JMenuItem editClearBuildings;
		/** Edit clear surface. */
		@Rename(to = "mapeditor.edit_clear_surface")
		JMenuItem editClearSurface;
		/** View zoom in. */
		@Rename(to = "mapeditor.view_zoom_in")
		JMenuItem viewZoomIn;
		/** View zoom out. */
		@Rename(to = "mapeditor.view_zoom_out")
		JMenuItem viewZoomOut;
		/** View zoom normal. */
		@Rename(to = "mapeditor.view_zoom_normal")
		JMenuItem viewZoomNormal;
		/** View bright. */
		@Rename(to = "mapeditor.view_bright")
		JMenuItem viewBrighter;
		/** View dark. */
		@Rename(to = "mapeditor.view_dark")
		JMenuItem viewDarker;
		/** View more light. */
		@Rename(to = "mapeditor.view_more_light")
		JMenuItem viewMoreLight;
		/** View less light. */
		@Rename(to = "mapeditor.view_less_light")
		JMenuItem viewLessLight;
		/** Help online. */
		@Rename(to = "mapeditor.help_online")
		JMenuItem helpOnline;
		/** Help about. */
		@Rename(to = "mapeditor.help_about")
		JMenuItem helpAbout;
		/** Language EN. */
		@Rename(to = "mapeditor.language_en")
		JRadioButtonMenuItem languageEn;
		/** Language HU. */
		@Rename(to = "mapeditor.language_hu")
		JRadioButtonMenuItem languageHu;
		/** Language. */
		@Rename(to = "mapeditor.language")
		JMenu languageMenu;
		/** File recent. */
		@Rename(to = "mapeditor.recent")
		JMenu fileRecent;
		/** Clear recent. */
		@Rename(to = "mapeditor.clear_recent")
		JMenuItem clearRecent;
	}
	/** The User Interface elements to rename. */
	final UIElements ui = new UIElements();
	/** Tabbed pane. */
	private JTabbedPane propertyTab;
	/** The undo manager. */
	UndoManager undoManager;
	/** The custom surface names. */
	final List<TileEntry> customSurfaceNames = new ArrayList<TileEntry>();
	/** The custom building names. */
	final List<TileEntry> customBuildingNames = new ArrayList<TileEntry>();
	/** The deferred language change. */
	String deferredLanguage = "hu";
	/** The set of recent files. */
	final Set<String> recent = new HashSet<String>();
	/** Load the resource locator. */
	void loadResourceLocator() {
		final BackgroundProgress bgp = new BackgroundProgress();
		bgp.setLocationRelativeTo(this);
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			/** The resource locator. */
			private ResourceLocator rl;
			/** The galaxy model. */
			private GalaxyModel  galaxyMap;
			/** The building model. */
			private BuildingModel buildingMap;
			/** The colony graphics. */
			private ColonyGFX colonyGraphics;
			/** Surface list. */
			private List<TileEntry> surfaces = JavaUtils.newArrayList();
			/** Buildings list. */
			private List<TileEntry> buildings = JavaUtils.newArrayList();
			/** Races. */
			private Set<String> races = JavaUtils.newHashSet();
			/** The loaded text renderer. */
			TextRenderer txt;
			@Override
			protected Void doInBackground() throws Exception {
				try {
					rl = config.newResourceLocator();
					final ResourceLocator rl0 = rl;
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							MapEditor.this.rl = rl0;
							setLabels(deferredLanguage);							
						}
					});
					galaxyMap = new GalaxyModel();
					galaxyMap.processGalaxy(rl, "en", "campaign/main/galaxy");
					buildingMap = new BuildingModel();
					buildingMap.processBuildings(rl, "en", "campaign/main/buildings");
					colonyGraphics = new ColonyGFX(rl);
					colonyGraphics.load("en");
					
					txt = new TextRenderer(rl);
					labels.load(rl, "en", "campaign/main");
					
					prepareLists(galaxyMap, buildingMap, surfaces, buildings, races);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				return null;
			}
			@Override
			protected void done() {
				bgp.dispose();
				MapEditor.this.galaxyModel = galaxyMap;
				MapEditor.this.buildingModel = buildingMap;
				renderer.colonyGFX = colonyGraphics;
				
				renderer.selection = new Tile(1, 1, ImageUtils.recolor(colonyGraphics.tileEdge, 0xFFFFFF00), null);
				renderer.areaAccept = new Tile(1, 1, ImageUtils.recolor(colonyGraphics.tileEdge, 0xFF00FFFF), null);
				renderer.areaEmpty = new Tile(1, 1, ImageUtils.recolor(colonyGraphics.tileEdge, 0xFF808080), null);
				renderer.areaDeny = new Tile(1, 1, ImageUtils.recolor(colonyGraphics.tileCrossed, 0xFFFF0000), null);
				renderer.areaCurrent  = new Tile(1, 1, ImageUtils.recolor(colonyGraphics.tileCrossed, 0xFFFFCC00), null);
				
				renderer.selection.alpha = 1.0f;
				renderer.areaAccept.alpha = 1.0f;
				renderer.areaDeny.alpha = 1.0f;
				renderer.areaCurrent.alpha = 1.0f;
			
				renderer.txt = this.txt;
				
				buildTables(surfaces, buildings, races);
			}
		};
		worker.execute();
		bgp.setVisible(true);
	}
	/**
	 * Program entry point.
	 * @param args no arguments
	 */
	public static void main(String[] args) {
//		System.setProperty("sun.java2d.opengl", "true");
//		System.setProperty("sun.java2d.trace", "log,timestamp,count,out:graphicstrace.log");
//		System.setProperty("sun.java2d.accthreshold", "0");
//		System.setProperty("sun.java2d.d3d", "false");
//		System.setProperty("sun.java2d.noddraw", "true");
//		System.setProperty("sun.java2d.translaccel", "true");
		long maxMem = Runtime.getRuntime().maxMemory();
		if (maxMem < MINIMUM_MEMORY * 1024 * 1024 * 95 / 100) {
			if (!doLowMemory()) {
				doWarnLowMemory(maxMem);
			}
			return;
		}
		final Configuration config = new Configuration("open-ig-config.xml");
		if (!config.load()) {
			doStartConfiguration(config);
		} else {
			doStartProgram(config);
		}
	}
	/**
	 * Start the program.
	 * @param config the configuration.
	 */
	static void doStartProgram(final Configuration config) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				config.watcherWindow = new ConsoleWatcher();
				MapEditor editor = new MapEditor(config);
				editor.setLocationRelativeTo(null);
				editor.setVisible(true);
			}
		});
	}
	/**
	 * Display the configuration window for setup.
	 * @param config the configuration
	 */
	private static void doStartConfiguration(final Configuration config) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Setup setup = new Setup(config);
				setup.setLocationRelativeTo(null);
				setup.setVisible(true);
				setup.pack();
				setup.onRun.add(new Act() {
					@Override
					public void act() {
						doStartProgram(config);
					}
				});
			}
		});
	}
	/**
	 * Put up warning dialog for failed attempt to run the program with appropriate memory.
	 * @param maxMem the detected memory
	 */
	private static void doWarnLowMemory(final long maxMem) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, "<html><p>Unable to auto-start Open Imperium Galactica MapEditor for version " + Configuration.VERSION + ".<br>Please make sure you have at least " 
						+ MINIMUM_MEMORY + "MB defined for running a Java program in either your<br>"
						+ "operating system's configuration for Java programs,<br> or run the program from command line using the <code>-Xmx" + MINIMUM_MEMORY + "M</code> parameter.</p><br>"
						+ "<p>Nem siker�lt automatikusan elind�tani az Open Imperium Galaktika " + Configuration.VERSION + " MapEditor programot.<br>K�rem ellen�rizze, hogy alap�rtelmez�sben a Java programok futtat�s�hoz "
						+ "legal�bb " + MINIMUM_MEMORY + "MB mem�ria<br> van be�ll�tva az Oper�ci�s Rendszerben,<br> vagy ind�tsa a program parancssorb�l a <code>-Xmx" + MINIMUM_MEMORY + "M</code> "
						+ "param�ter megad�s�val.</p>"
				);
			}
		});
	}
	/**
	 * Restart the program using the proper memory settings.
	 * @return true if the re initialization was successful
	 */
	private static boolean doLowMemory() {
		ProcessBuilder pb = new ProcessBuilder();
		if (!new File("open-ig-mapeditor-" + MAP_EDITOR_JAR_VERSION + ".jar").exists()) {
			pb.command(System.getProperty("java.home") + "/bin/java", "-Xmx" + MINIMUM_MEMORY + "M", "-cp", "./bin", "-splash:bin/hu/openig/xold/res/OpenIG_Splash.png", "hu.openig.editors.MapEditor");
		} else {
			pb.command(System.getProperty("java.home") + "/bin/java", "-Xmx" + MINIMUM_MEMORY + "M", "-cp", "open-ig-mapeditor-" + MAP_EDITOR_JAR_VERSION + ".jar", "-splash:hu/openig/xold/res/OpenIG_Splash.png", "hu.openig.editors.MapEditor");
		}
		try {
			/* Process p = */pb.start();
//			createBackgroundReader(p.getInputStream(), System.out).start();
//			createBackgroundReader(p.getErrorStream(), System.err).start();
//			SwingUtilities.invokeLater(new Runnable() {
//				@Override
//				public void run() {
//					JFrame frame = new JFrame("Running MapEditor with correct memory settings...");
//					frame.dispose();
//				}
//			});
//			p.waitFor();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
//		} catch (InterruptedException ex) {
//			ex.printStackTrace();
//			return false;
		}
	}
	/**
	 * Create a background stream copy thread for the given input and output streams.
	 * @param in the input stream
	 * @param out the output stream
	 * @return the thread
	 */
	static Thread createBackgroundReader(final InputStream in, final OutputStream out) {
		return new Thread() {
			@Override
			public void run() {
				int c;
				try {
					while ((c = in.read()) != -1) {
						out.write(c);
						if (c == 10) {
							out.flush();
						}
					}
				} catch (IOException ex) {
					// ignored
				}
			}
		};
	}
	/** 
	 * Build the GUI. 
	 * @param config the configuration
	 */
	public MapEditor(final Configuration config) {
		super(TITLE);
		this.config = config;
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		toolSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		featuresSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		renderer = new MapRenderer();
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				saveConfig();
				dispose();
				doExit();
			}
		});

		labels = new Labels();
		
		renderer.labels = labels;
		
		surfaceTableModel = new TileList();
		buildingTableModel = new TileList();
		buildingTableModel.colNames[3] = "Race";
		
		surfaceTable = new JTable(surfaceTableModel);
		surfaceTable.setRowHeight(32);
		buildingTable = new JTable(buildingTableModel);
		buildingTable.setRowHeight(32);

		surfaceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		surfaceSorter = new TableRowSorter<TileList>(surfaceTableModel);
		surfaceTable.setRowSorter(surfaceSorter);
		
		buildingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		buildingSorter = new TableRowSorter<TileList>(buildingTableModel);
		buildingTable.setRowSorter(buildingSorter);

		surfaceTable.getColumnModel().getColumn(0).setPreferredWidth(45);
		surfaceTable.getColumnModel().getColumn(1).setPreferredWidth(32);
		buildingTable.getColumnModel().getColumn(0).setPreferredWidth(45);
		buildingTable.getColumnModel().getColumn(1).setPreferredWidth(32);

		surfaceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				doSelectSurface();
			}
		});
		surfaceTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					doPlaceSurface();
				}
			}
		});
		buildingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				doSelectBuilding();
			}
		});
		buildingTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					doPlaceBuilding();
				}
			}
		});
		
		JScrollPane sp0 = new JScrollPane(surfaceTable);
		JScrollPane sp1 = new JScrollPane(buildingTable);
		
		JPanel surfacePanel = new JPanel();
		surfacePanel.setLayout(new BoxLayout(surfacePanel, BoxLayout.PAGE_AXIS));
		filterSurface = new JTextField();
		filterSurface.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doFilterSurface();
			}
		});

		GroupLayout gls = new GroupLayout(surfacePanel);
		surfacePanel.setLayout(gls);
		gls.setHorizontalGroup(gls.createParallelGroup().addComponent(filterSurface).addComponent(sp0));
		gls.setVerticalGroup(gls.createSequentialGroup().addComponent(filterSurface, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(sp0));

		JPanel buildingPanel = new JPanel();
		filterBuilding = new JTextField();
		filterBuilding.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doFilterBuilding();
			}
		});
		GroupLayout glb = new GroupLayout(buildingPanel);
		buildingPanel.setLayout(glb);
		glb.setHorizontalGroup(glb.createParallelGroup().addComponent(filterBuilding).addComponent(sp1));
		glb.setVerticalGroup(glb.createSequentialGroup().addComponent(filterBuilding, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(sp1));
		
		JPanel previewPanel = new JPanel();
		
		alphaSlider = new JSlider(0, 100, 100);
		alphaSlider.setMajorTickSpacing(10);
		alphaSlider.setMinorTickSpacing(1);
		alphaSlider.setPaintLabels(true);
		alphaSlider.setPaintTicks(true);
		alphaSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				preview.setAlpha(1.0f * alphaSlider.getValue() / alphaSlider.getMaximum());
			}
		});
		
		preview = new ImagePaint();

		filterSurface.setToolTipText("Filter by size, name and surface. Example: '1x earth' means search for tiles with width=1 and surface=earth");
		filterBuilding.setToolTipText("Filter by size, name and race. Example: '2x human' means search for tiles with width=2 and race=human");
		alphaSlider.setToolTipText("Adjust brightness level. Buildings should turn on lights below 51 by default.");

		ui.buildButton = new JToggleButton("Object placement mode");
		ui.buildButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ui.editPlaceMode.setSelected(ui.buildButton.isSelected());
				renderer.placementMode = ui.buildButton.isSelected();
			}
		});
		GroupLayout gl2 = new GroupLayout(previewPanel);
		previewPanel.setLayout(gl2);
		gl2.setAutoCreateContainerGaps(true);
		
		gl2.setHorizontalGroup(
				gl2.createParallelGroup(Alignment.CENTER)
				.addComponent(alphaSlider)
				.addComponent(preview)
				.addComponent(ui.buildButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
		);
		gl2.setVerticalGroup(
				gl2.createSequentialGroup()
				.addComponent(alphaSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(preview)
				.addComponent(ui.buildButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
		);
		
		
		propertyTab = new JTabbedPane();
		
		split.setLeftComponent(toolSplit);
		split.setRightComponent(renderer);
		split.setDividerLocation(450);
		
		toolSplit.setTopComponent(previewPanel);
		toolSplit.setBottomComponent(propertyTab);
		toolSplit.setDividerLocation(350);
		
		featuresSplit.setTopComponent(surfacePanel);
		featuresSplit.setBottomComponent(buildingPanel);
		featuresSplit.setDividerLocation(150);

		propertyTab.addTab("Surfaces & Buildings", featuresSplit);
		propertyTab.addTab("Building properties", createBuildingPropertiesPanel());
		ui.allocationPanel = new AllocationPanel();
		propertyTab.addTab("Allocation", ui.allocationPanel);
		
		split.setDoubleBuffered(true);
		getContentPane().add(split);
		setSize(1024, 768);
		
		renderer.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.isControlDown()) {
					double pre = renderer.scale;
					double mx = (e.getX() - renderer.offsetX) * pre;
					double my = (e.getY() - renderer.offsetY) * pre;
					if (e.getUnitsToScroll() < 0) {
						doZoomIn();
					} else {
						doZoomOut();
					}
					double mx0 = (e.getX() - renderer.offsetX) * renderer.scale;
					double my0 = (e.getY() - renderer.offsetY) * renderer.scale;
					double dx = (mx - mx0) / pre;
					double dy = (my - my0) / pre;
					renderer.offsetX += (int)(dx);
					renderer.offsetY += (int)(dy);
					renderer.repaint();
				}
			}
		});
		renderer.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && !renderer.placementMode) {
					doSelectBuilding(e.getX(), e.getY());
				}
			}
		});
		
		renderer.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (ui.buildButton.isSelected()) {
						doPlaceObject();
					}
				}
			}
		});
		
		undoManager = new UndoManager();
		
		buildMenu();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				loadConfig();
				loadResourceLocator();
			}
		});
		
	}
	/**
	 * Exit the application.
	 */
	private void doExit() {
		renderer.stopAnimations();
		if (config.watcherWindow != null) {
			try {
				config.watcherWindow.close();
			} catch (IOException ex) {
				
			}
		}
	}
	/**
	 * Construct the menu tree.
	 */
	private void buildMenu() {
		JMenuBar mainmenu = new JMenuBar();
		setJMenuBar(mainmenu);

		ui.fileMenu = new JMenu("File");
		ui.editMenu = new JMenu("Edit");
		ui.viewMenu = new JMenu("View");
		ui.helpMenu = new JMenu("Help");
		
		ui.fileNew = new JMenuItem("New...");
		ui.fileOpen = new JMenuItem("Open...");
		ui.fileImport = new JMenuItem("Import...");
		ui.fileSave = new JMenuItem("Save");
		ui.fileSaveAs = new JMenuItem("Save as...");
		ui.fileExit = new JMenuItem("Exit");

		ui.fileOpen.setAccelerator(KeyStroke.getKeyStroke('O', InputEvent.CTRL_DOWN_MASK));
		ui.fileSave.setAccelerator(KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK));
		
		ui.fileImport.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doImport(); } });
		ui.fileExit.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { dispose(); doExit();  } });
		
		ui.fileOpen.addActionListener(new Act() {
			@Override
			public void act() {
				doLoad();
			}
		});
		ui.fileSave.addActionListener(new Act() {
			@Override
			public void act() {
				doSave();
			}
		});
		ui.fileSaveAs.addActionListener(new Act() {
			@Override
			public void act() {
				doSaveAs();
			}
		});
		
		ui.editUndo = new JMenuItem("Undo");
		ui.editUndo.setEnabled(undoManager.canUndo()); // TODO implement
		
		ui.editRedo = new JMenuItem("Redo");
		ui.editRedo.setEnabled(undoManager.canRedo()); // TODO implement
		
		
		ui.editCut = new JMenuItem("Cut");
		ui.editCut.setEnabled(false); // TODO implement
		ui.editCopy = new JMenuItem("Copy");
		ui.editCopy.setEnabled(false); // TODO implement
		ui.editPaste = new JMenuItem("Paste");
		ui.editPaste.setEnabled(false); // TODO implement
		
		ui.editPlaceMode = new JCheckBoxMenuItem("Placement mode");
		
		ui.editDeleteBuilding = new JMenuItem("Delete building");
		ui.editDeleteSurface = new JMenuItem("Delete surface");
		ui.editDeleteBoth = new JMenuItem("Delete both");
		
		ui.editClearBuildings = new JMenuItem("Clear buildings");
		ui.editClearSurface = new JMenuItem("Clear surface");

		ui.editUndo.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doUndo(); } });
		ui.editRedo.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doRedo(); } });
		ui.editClearBuildings.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doClearBuildings(true); } });
		ui.editClearSurface.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doClearSurfaces(true); } });
		

		ui.editUndo.setAccelerator(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK));
		ui.editRedo.setAccelerator(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK));
		ui.editCut.setAccelerator(KeyStroke.getKeyStroke('X', InputEvent.CTRL_DOWN_MASK));
		ui.editCopy.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.CTRL_DOWN_MASK));
		ui.editPaste.setAccelerator(KeyStroke.getKeyStroke('V', InputEvent.CTRL_DOWN_MASK));
		ui.editPlaceMode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		
		ui.editDeleteBuilding.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		ui.editDeleteSurface.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK));
		ui.editDeleteBoth.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		
		ui.editDeleteBuilding.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doDeleteBuilding(); } });
		ui.editDeleteSurface.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doDeleteSurface(); } });
		ui.editDeleteBoth.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doDeleteBoth(); } });
		ui.editPlaceMode.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doPlaceMode(ui.editPlaceMode.isSelected()); } });
		
		ui.editPlaceRoads = new JMenu("Place roads");
		
		ui.viewZoomIn = new JMenuItem("Zoom in");
		ui.viewZoomOut = new JMenuItem("Zoom out");
		ui.viewZoomNormal = new JMenuItem("Zoom normal");
		ui.viewBrighter = new JMenuItem("Daylight (1.0)");
		ui.viewDarker = new JMenuItem("Night (0.5)");
		ui.viewMoreLight = new JMenuItem("More light (+0.05)");
		ui.viewLessLight = new JMenuItem("Less light (-0.05)");
		
		ui.viewShowBuildings = new JCheckBoxMenuItem("Show/hide buildings", true);
		ui.viewShowBuildings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
		
		ui.viewSymbolicBuildings = new JCheckBoxMenuItem("Minimap rendering mode");
		ui.viewSymbolicBuildings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
		
		ui.viewTextBackgrounds = new JCheckBoxMenuItem("Show/hide text background boxes", true);
		ui.viewTextBackgrounds.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		ui.viewTextBackgrounds.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doToggleTextBackgrounds(); } });
		
		ui.viewZoomIn.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doZoomIn(); } });
		ui.viewZoomOut.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doZoomOut(); } });
		ui.viewZoomNormal.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doZoomNormal(); } });
		ui.viewShowBuildings.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doToggleBuildings(); } });
		ui.viewSymbolicBuildings.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doToggleMinimap(); } });
		
		ui.viewBrighter.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doBright(); } });
		ui.viewDarker.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doDark(); } });
		ui.viewMoreLight.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doMoreLight(); } });
		ui.viewLessLight.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doLessLight(); } });
		
		ui.viewZoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD9, InputEvent.CTRL_DOWN_MASK));
		ui.viewZoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD3, InputEvent.CTRL_DOWN_MASK));
		ui.viewZoomNormal.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK));
		ui.viewMoreLight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD8, InputEvent.CTRL_DOWN_MASK));
		ui.viewLessLight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, InputEvent.CTRL_DOWN_MASK));
		ui.viewBrighter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD7, InputEvent.CTRL_DOWN_MASK));
		ui.viewDarker.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1, InputEvent.CTRL_DOWN_MASK));
		
		ui.helpOnline = new JMenuItem("Online wiki...");
		ui.helpOnline.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doHelp();
			}
		});
		ui.helpAbout = new JMenuItem("About...");
		ui.helpAbout.setEnabled(false); // TODO implement
		
		ui.languageMenu = new JMenu("Language");
		
		ui.languageEn = new JRadioButtonMenuItem("English", true);
		ui.languageEn.addActionListener(new Act() {
			@Override
			public void act() {
				setLabels("en");
			}
		});
		ui.languageHu = new JRadioButtonMenuItem("Hungarian", false);
		ui.languageHu.addActionListener(new Act() {
			@Override
			public void act() {
				setLabels("hu");
			}
		});
		ButtonGroup bg = new ButtonGroup();
		bg.add(ui.languageEn);
		bg.add(ui.languageHu);
		
		ui.fileRecent = new JMenu("Recent");
		ui.clearRecent = new JMenuItem("Clear recent");
		ui.clearRecent.addActionListener(new Act() {
			@Override
			public void act() {
				doClearRecent();
			}
		});
		
		addAll(ui.fileRecent, ui.clearRecent);
		
		addAll(mainmenu, ui.fileMenu, ui.editMenu, ui.viewMenu, ui.languageMenu, ui.helpMenu);
		addAll(ui.fileMenu, ui.fileNew, null, ui.fileOpen, ui.fileRecent, ui.fileImport, null, ui.fileSave, ui.fileSaveAs, null, ui.fileExit);
		addAll(ui.editMenu, ui.editUndo, ui.editRedo, null, ui.editCut, ui.editCopy, ui.editPaste, null, 
				ui.editPlaceMode, null, ui.editDeleteBuilding, ui.editDeleteSurface, ui.editDeleteBoth, null, 
				ui.editClearBuildings, ui.editClearSurface, null, ui.editPlaceRoads);
		addAll(ui.viewMenu, ui.viewZoomIn, ui.viewZoomOut, ui.viewZoomNormal, null, 
				ui.viewBrighter, ui.viewDarker, ui.viewMoreLight, ui.viewLessLight, null, 
				ui.viewShowBuildings, ui.viewSymbolicBuildings, ui.viewTextBackgrounds);
		addAll(ui.helpMenu, ui.helpOnline, null, ui.helpAbout);
		
		addAll(ui.languageMenu, ui.languageEn, ui.languageHu);
		
		ui.fileNew.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doNew();
			}
		});
		
	}
	/** Toggle the rendering of text backgrounds. */
	protected void doToggleTextBackgrounds() {
		renderer.textBackgrounds = ui.viewTextBackgrounds.isSelected();
		renderer.repaint();
	}
	/** Toggle minimap display mode. */
	protected void doToggleMinimap() {
		renderer.minimapMode = ui.viewSymbolicBuildings.isSelected();
		renderer.repaint();
	}
	/**
	 * Toggle between the placement mode and selection mode.
	 * @param selected is placement mode on?
	 */
	protected void doPlaceMode(boolean selected) {
		ui.buildButton.setSelected(selected);
		renderer.placementMode = selected;
	}
	/**
	 * Zoom to 100%.
	 */
	protected void doZoomNormal() {
		renderer.scale = 1.0;
		renderer.repaint();
	}
	/**
	 * 
	 */
	protected void doZoomOut() {
		renderer.scale = Math.max(0.1, renderer.scale - 0.1);
		renderer.repaint();
	}
	/**
	 * 
	 */
	protected void doZoomIn() {
		renderer.scale = Math.min(2.0, renderer.scale + 0.1);
		renderer.repaint();
	}
	/** Set the state of the undo and redo menu items. */
	void setUndoRedoMenu() {
		ui.editUndo.setEnabled(undoManager.canUndo());
		ui.editRedo.setEnabled(undoManager.canRedo());
	}
	/** Redo last operation. */
	protected void doRedo() {
		undoManager.redo();
		setUndoRedoMenu();
	}
	/** Undo last operation. */
	protected void doUndo() {
		undoManager.undo();
		setUndoRedoMenu();
	}
	/** 
	 * Add an undoable map edit and update menus accordingly.
	 * @param undo the undo object
	 */
	void addUndo(UndoableMapEdit undo) {
		undoManager.addEdit(undo);
		setUndoRedoMenu();
	}
	/** 
	 * Clear surfaces. 
	 * @param redoable save a redo point?
	 */
	protected void doClearSurfaces(boolean redoable) {
		if (renderer.surface != null) {
			UndoableMapEdit undo = redoable ? new UndoableMapEdit(renderer.surface) : null;
			renderer.surface.basemap.clear();
			renderer.surface.features.clear();
			if (redoable) {
				undo.setAfter();
				addUndo(undo);
			}
			renderer.repaint();
		}
	}
	/** 
	 * Clear buildings. 
	 * @param redoable save a redo point?
	 */
	protected void doClearBuildings(boolean redoable) {
		if (renderer.surface != null) {
			UndoableMapEdit undo = redoable ? new UndoableMapEdit(renderer.surface) : null;
			renderer.surface.buildingmap.clear();
			renderer.surface.buildings.clear();
			if (redoable) {
				undo.setAfter();
				addUndo(undo);
			}
			renderer.repaint();
		}
	}
	/**
	 * Decrease the light amount on the tiles.
	 */
	protected void doLessLight() {
		alpha = Math.max(0, alpha - 0.05f);
		setAlphaOnTiles();
		repaint();
	}
	/**
	 * 
	 */
	private void setAlphaOnTiles() {
		if (renderer.surface != null) {
			for (SurfaceEntity se : renderer.surface.basemap.values()) {
				se.tile.alpha = alpha; 
			}
			for (SurfaceEntity se : renderer.surface.buildingmap.values()) {
				se.tile.alpha = alpha;
			}
		}
	}
	/**
	 * Increase the light amount on the tiles.
	 */
	protected void doMoreLight() {
		alpha = Math.min(1f, alpha + 0.05f);;
		setAlphaOnTiles();
		repaint();
	}
	/** Set lighting to half. */
	protected void doDark() {
		alpha = 0.5f;
		setAlphaOnTiles();
		repaint();
	}
	/** Set lighting to full. */
	protected void doBright() {
		alpha = 1.0f;
		setAlphaOnTiles();
		repaint();
	}
	/**
	 * Toggle the visibility of the buildings.
	 */
	protected void doToggleBuildings() {
		renderer.showBuildings = ui.viewShowBuildings.getState();
		repaint();
	}
	/** Delete buildings and surface elements of the selection rectangle. */
	protected void doDeleteBoth() {
		if (renderer.surface != null) {
			UndoableMapEdit undo = new UndoableMapEdit(renderer.surface);
			deleteEntitiesOf(renderer.surface.basemap, renderer.selectedRectangle, false);
			deleteEntitiesOf(renderer.surface.buildingmap, renderer.selectedRectangle, true);
			undo.setAfter();
			addUndo(undo);
			repaint();
		}
	}
	/** Delete surface surface. */
	protected void doDeleteSurface() {
		if (renderer.surface != null) {
			UndoableMapEdit undo = new UndoableMapEdit(renderer.surface);
			deleteEntitiesOf(renderer.surface.basemap, renderer.selectedRectangle, false);
			undo.setAfter();
			addUndo(undo);
			repaint();
		}
	}
	/**
	 * Delete buildings falling into the current selection rectangle.
	 */
	protected void doDeleteBuilding() {
		if (renderer.surface != null) {
			UndoableMapEdit undo = new UndoableMapEdit(renderer.surface);
			deleteEntitiesOf(renderer.surface.buildingmap, renderer.selectedRectangle, true);
			undo.setAfter();
			addUndo(undo);
			repaint();
		}
	}
	/**
	 * Delete entries of the given map.
	 * @param map the map
	 * @param rect the target rectangle
	 * @param checkBuildings check for the actual buildings
	 */
	void deleteEntitiesOf(Map<Location, SurfaceEntity> map, Rectangle rect, boolean checkBuildings) {
		// find buildings falling into the selection box
		if (rect != null) {
			Building bld = null;
			for (int a = rect.x; a < rect.x + rect.width; a++) {
				for (int b = rect.y; b > rect.y - rect.height; b--) {
					SurfaceEntity se = map.get(Location.of(a, b));
					if (se != null) {
						int x = a - se.virtualColumn;
						int y = b + se.virtualRow;
						for (int x0 = x; x0 < x + se.tile.width; x0++) {
							for (int y0 = y; y0 > y - se.tile.height; y0--) {
								map.remove(Location.of(x0, y0));
							}
						}
					}
					if (checkBuildings) {
						for (int i = renderer.surface.buildings.size() - 1; i >= 0; i--) {
							bld = renderer.surface.buildings.get(i);
							if (bld.containsLocation(a, b)) {
								renderer.surface.buildings.remove(i);
								if (bld == currentBuilding) {
									renderer.buildingBox = null;
									currentBuilding = null;
								}
							}
						}
					} else {
						for (int i = renderer.surface.features.size() - 1; i >= 0; i--) {
							SurfaceFeature sf = renderer.surface.features.get(i);
							if (sf.containsLocation(a, b)) {
								renderer.surface.features.remove(i);
								if (sf.equals(currentBaseTile)) {
									currentBaseTile = null;
								}
							}
						}
					}
				}
			}
			if (checkBuildings && bld != null) {
				placeRoads(bld.techId);
			}
		}
	}
	/**
	 * Extract the stacktrace as a string.
	 * @param t the throwable
	 * @return the string
	 */
	String stacktraceToString(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		t.printStackTrace(out);
		out.flush();
		return sw.toString();
	}
	/**
	 * Add multiple items to a menu.
	 * @param menu the menu parent
	 * @param items the array of items, null represents a separator
	 */
	void addAll(JMenu menu, JMenuItem... items) {
		for (JMenuItem mi : items) {
			if (mi != null) {
				menu.add(mi);
			} else {
				menu.addSeparator();
			}
		}
	}
	/**
	 * Add multiple items to a menubar.
	 * @param menu the menu parent
	 * @param items the array of items
	 */
	void addAll(JMenuBar menu, JMenuItem... items) {
		for (JMenuItem mi : items) {
			if (mi != null) {
				menu.add(mi);
			}
		}
	}
	/** Filter the building list. */
	protected void doFilterBuilding() {
		if (filterBuilding.getText().length() > 0) {
			final String[] words = filterBuilding.getText().split("\\s+");
			buildingSorter.setRowFilter(new RowFilter<TileList, Integer>() {
				@Override
				public boolean include(
						javax.swing.RowFilter.Entry<? extends TileList, ? extends Integer> entry) {
					TileEntry e = entry.getModel().rows.get(entry.getIdentifier());
					String rowtext = (e.name + " " + e.surface + " " + e.tile.width + "x" + e.tile.height).toLowerCase(); 
					for (String s : words) {
						if (!rowtext.contains(s.toLowerCase())) {
							return false;
						}
					}
					return true;
				}
			});
		} else {
			buildingSorter.setRowFilter(null);
		}
	}
	/** Filter the surface list. */
	protected void doFilterSurface() {
		if (filterSurface.getText().length() > 0) {
			final String[] words = filterSurface.getText().split("\\s+");
			surfaceSorter.setRowFilter(new RowFilter<TileList, Integer>() {
				@Override
				public boolean include(
						javax.swing.RowFilter.Entry<? extends TileList, ? extends Integer> entry) {
					TileEntry e = entry.getModel().rows.get(entry.getIdentifier());
					String rowtext = (e.name + " " + e.surface + " " + e.tile.width + "x" + e.tile.height).toLowerCase(); 
					for (String s : words) {
						if (!rowtext.contains(s.toLowerCase())) {
							return false;
						}
					}
					return true;
				}
			});
			
		} else {
			surfaceSorter.setRowFilter(null);
		}
	}
	/**
	 * A tile list table model.
	 * @author karnokd
	 */
	public static class TileList extends AbstractTableModel {
		/** */
		private static final long serialVersionUID = 1870030483025880490L;
		/** The column names. */
		final String[] colNames = { "Preview", "Size", "Name", "Surface" };
		/** The column classes. */
		final Class<?>[] colClasses = { ImageIcon.class, String.class, String.class, String.class };
		/** The list of rows. */
		public List<TileEntry> rows = new ArrayList<TileEntry>();
		@Override
		public int getColumnCount() {
			return colNames.length;
		}
		@Override
		public int getRowCount() {
			return rows.size();
		}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return colClasses[columnIndex];
		}
		@Override
		public String getColumnName(int column) {
			return colNames[column];
		}
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			TileEntry te = rows.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return te.preview;
			case 1:
				return te.tile.width + " x " + te.tile.height;
			case 2:
				return te.name;
			case 3:
				return te.surface;
			default:
			}
			return null;
		}
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 2;
		}
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 2) {
				TileEntry te = rows.get(rowIndex);
				te.name = String.valueOf(aValue);
			}
		}
	}
	/**
	 * Build table contents.
	 * @param surfaces the list of surface objects
	 * @param buildings the list of building objects
	 * @param races the set of races
	 */
	void buildTables(List<TileEntry> surfaces, List<TileEntry> buildings, Set<String> races) {
		surfaceTableModel.rows = surfaces;
		buildingTableModel.rows = buildings;
		surfaceTableModel.fireTableDataChanged();
		buildingTableModel.fireTableDataChanged();
		List<String> racesList = new ArrayList<String>(races);
		Collections.sort(racesList);
		for (final String s : racesList) {
			JMenuItem mnuPlaceRoad = new JMenuItem(s);
			mnuPlaceRoad.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					placeRoads(s);
					repaint();
				}
			});
			
			
			ui.editPlaceRoads.add(mnuPlaceRoad);
		}
	}
	/**
	 * Create a scaled image.
	 * @param image the base image
	 * @param width the target width
	 * @param height the target height
	 * @return the scaled image
	 */
	BufferedImage scaledImage(BufferedImage image, int width, int height) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = result.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		double sx = (double)width / image.getWidth();
		double sy = (double)height / image.getHeight();
		double scale = Math.min(sx, sy);
		int x = (int)((width - image.getWidth() * scale) / 2);
		int y = (int)((height - image.getHeight() * scale) / 2);
		// center the image
		g2.drawImage(image, x, y, (int)(image.getWidth() * scale), (int)(image.getHeight() * scale), null);
		
		g2.dispose();
		return result;
	}
	/** Select a surface image. */
	void doSelectSurface() {
		int idx = surfaceTable.getSelectedRow();
		if (idx >= 0) {
			idx = surfaceTable.convertRowIndexToModel(idx);
			TileEntry tileEntry = surfaceTableModel.rows.get(idx);
			preview.setImage(tileEntry.previewTile);
			renderer.placementRectangle.width = tileEntry.tile.width;
			renderer.placementRectangle.height = tileEntry.tile.height;
			buildingTable.getSelectionModel().clearSelection();
			
			currentBaseTile = new SurfaceFeature(); 
			currentBaseTile.tile = tileEntry.tile;
			currentBaseTile.id = tileEntry.id;
			currentBaseTile.type = tileEntry.surface;
			
			currentBuildingType = null;
		}
	}
	/** Select a surface image. */
	void doSelectBuilding() {
		int idx = buildingTable.getSelectedRow();
		if (idx >= 0) {
			idx = buildingTable.convertRowIndexToModel(idx);
			TileEntry tileEntry = buildingTableModel.rows.get(idx);
			preview.setImage(tileEntry.previewTile);
			renderer.placementRectangle.width = tileEntry.tile.width + 2;
			renderer.placementRectangle.height = tileEntry.tile.height + 2;
			surfaceTable.getSelectionModel().clearSelection();
			currentBaseTile = null;
			currentBuildingType = tileEntry.buildingType;
			currentBuildingRace = tileEntry.surface;
		}
	}
	/**
	 * Create a new, empty surface map.
	 */
	void doNew() {
		setTitle(TITLE);
		createPlanetSurface(33, 66);
		renderer.repaint();
		saveSettings = null;
		undoManager.discardAllEdits();
		setUndoRedoMenu();
	}
	/**
	 * Create an empty planet surface with the given size.
	 * @param width the horizontal width (and not in coordinate amounts!)
	 * @param height the vertical height (and not in coordinate amounts!) 
	 */
	void createPlanetSurface(int width, int height) {
		renderer.surface = new PlanetSurface();
		renderer.surface.width = width;
		renderer.surface.height = height;
		renderer.surface.computeRenderingLocations();
		ui.allocationPanel.buildings = renderer.surface.buildings;
	}
	/**
	 * Place a tile onto the current surface map.
	 * @param tile the tile
	 * @param x the tile's leftmost coordinate
	 * @param y the tile's leftmost coordinate
	 * @param type the tile type
	 * @param building the building object to assign
	 */
	void placeTile(Tile tile, int x, int y, SurfaceEntityType type, Building building) {
		for (int a = x; a < x + tile.width; a++) {
			for (int b = y; b > y - tile.height; b--) {
				SurfaceEntity se = new SurfaceEntity();
				se.type = type;
				se.virtualRow = y - b;
				se.virtualColumn = a - x;
				se.tile = tile;
				se.tile.alpha = alpha;
				se.building = building;
				if (type != SurfaceEntityType.BASE) {
					renderer.surface.buildingmap.put(Location.of(a, b), se);
				} else {
					renderer.surface.basemap.put(Location.of(a, b), se);
				}
			}
		}
	}
	/**
	 * Place the selected surface tile into the selection rectangle.
	 */
	void doPlaceSurface() {
		int idx = surfaceTable.getSelectedRow();
		if (idx >= 0) {
			idx = surfaceTable.convertRowIndexToModel(idx);
			TileEntry te = surfaceTableModel.rows.get(idx);
			if (renderer.selectedRectangle != null && renderer.selectedRectangle.width > 0) {
				UndoableMapEdit undo = new UndoableMapEdit(renderer.surface);
				
				Rectangle clearRect = new Rectangle(renderer.selectedRectangle);
				clearRect.width = ((clearRect.width + te.tile.width - 1) / te.tile.width) * te.tile.width;
				clearRect.height = ((clearRect.height + te.tile.height - 1) / te.tile.height) * te.tile.height;
				deleteEntitiesOf(renderer.surface.basemap, clearRect, false);
				
				for (int x = renderer.selectedRectangle.x; x < renderer.selectedRectangle.x + renderer.selectedRectangle.width; x += te.tile.width) {
					for (int y = renderer.selectedRectangle.y; y > renderer.selectedRectangle.y - renderer.selectedRectangle.height; y -= te.tile.height) {
						SurfaceFeature sf = new SurfaceFeature();
						sf.id = te.id;
						sf.type = te.surface;
						sf.location = Location.of(x, y);
						sf.tile = te.tile;
						renderer.surface.features.add(sf);
						placeTile(te.tile, x, y, SurfaceEntityType.BASE, null);
					}
				}
				undo.setAfter();
				addUndo(undo);
			}
			renderer.repaint();
		}
	}
	/**
	 * Place the selected building tile into the selection rectangle.
	 */
	void doPlaceBuilding() {
		int idx = buildingTable.getSelectedRow();
		if (idx >= 0) {
			idx = buildingTable.convertRowIndexToModel(idx);
			TileEntry te = buildingTableModel.rows.get(idx);
			if (renderer.selectedRectangle != null && renderer.selectedRectangle.width > 0) {
				UndoableMapEdit undo = new UndoableMapEdit(renderer.surface);
				
				Rectangle clearRect = new Rectangle(renderer.selectedRectangle);
				clearRect.width = ((clearRect.width + te.tile.width) / (te.tile.width + 1)) * (te.tile.width + 1) + 1;
				clearRect.height = ((clearRect.height + te.tile.height) / (te.tile.height + 1)) * (te.tile.height + 1) + 1;
				deleteEntitiesOf(renderer.surface.buildingmap, clearRect, true);
				
				for (int x = renderer.selectedRectangle.x; x < renderer.selectedRectangle.x + renderer.selectedRectangle.width; x += te.tile.width + 1) {
					for (int y = renderer.selectedRectangle.y; y > renderer.selectedRectangle.y - renderer.selectedRectangle.height; y -= te.tile.height + 1) {
						Building bld = new Building(te.buildingType, te.surface);
						bld.makeFullyBuilt();
						bld.location = Location.of(x + 1, y - 1);
						renderer.surface.buildings.add(bld);
						placeTile(te.tile, bld.location.x, bld.location.y, SurfaceEntityType.BUILDING, bld);
					}
				}
				placeRoads(te.surface);
				undo.setAfter();
				addUndo(undo);
				renderer.repaint();
			}
		}
	}
	/**
	 * 
	 */
	void doHelp() {
		Desktop desktop = Desktop.getDesktop();
		if (desktop != null) {
			try {
				desktop.browse(new URI("http://code.google.com/p/open-ig/wiki/MapEditor"));
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(MapEditor.this, "Exception", stacktraceToString(ex), JOptionPane.ERROR_MESSAGE);
			} catch (URISyntaxException ex) {
				JOptionPane.showMessageDialog(MapEditor.this, "Exception", stacktraceToString(ex), JOptionPane.ERROR_MESSAGE);
			}
		} else {
			JOptionPane.showMessageDialog(MapEditor.this, "Desktop not supported. Please navigate to http://code.google.com/p/open-ig/wiki/MapEditor manually.");
		}
	}
	/**
	 * Show the import dialog.
	 */
	void doImport() {
		if (imp == null) {
			imp = new ImportDialog(rl);
		}
		imp.setLabels(labels);
		imp.setLocationRelativeTo(this);
		imp.setVisible(true);
		if (imp.success) {
			if (renderer.surface == null) {
				createPlanetSurface(33, 66);
			}
			UndoableMapEdit undo = new UndoableMapEdit(renderer.surface);
			if (imp.selected != null) {
				if (imp.replaceSurface) {
					doClearSurfaces(false);
				}
				placeTilesFromOriginalMap(imp.selected.fullPath, imp.selected.surfaceType, imp.shiftXValue, imp.shiftYValue);
			}
			if (imp.planet != null) {
				if (imp.replaceBuildings) {
					doClearBuildings(false);
				}
				if (imp.withSurface) {
					if (imp.replaceSurface) {
						doClearSurfaces(false);
					}
					placeTilesFromOriginalMap("colony/" + imp.planet.getMapName(), imp.planet.surfaceType.toLowerCase(), imp.shiftXValue, imp.shiftYValue);
				}

				for (OriginalBuilding ob : imp.planet.buildings) {
					BuildingType bt = buildingModel.buildings.get(ob.getName()); 
					String r = imp.planet.getRaceTechId();
					TileSet t = bt.tileset.get(r);
					Building bld = new Building(bt, r);
					bld.makeFullyBuilt();
					bld.location = Location.of(ob.location.x + imp.shiftXValue, ob.location.y + imp.shiftYValue);
					renderer.surface.buildings.add(bld);
					placeTile(t.normal, ob.location.x + imp.shiftXValue, ob.location.y + imp.shiftYValue, SurfaceEntityType.BUILDING, bld);
				}
				renderer.buildingBox = null;
				currentBuilding = null;
				placeRoads(imp.planet.getRaceTechId());
			} else {
				doClearBuildings(false);
			}
			undo.setAfter();
			addUndo(undo);
			repaint();
		}
	}
	/**
	 * Place an original map tiles onto the current surface.
	 * @param path the path to the original map file
	 * @param surfaceType the surface type
	 * @param shiftX the shift in X coordinates to place the map elements
	 * @param shiftY the shift in Y coordinates to place the map elements
	 */
	private void placeTilesFromOriginalMap(String path, String surfaceType, int shiftX, int shiftY) {
		byte[] map = rl.getData("en", path);
		PlanetType pt = galaxyModel.planetTypes.get(surfaceType);
		int bias = 41; 
		if ("neptoplasm".equals(surfaceType)) {
			bias = 84;
		}
		for (int i = 0; i < 65 * 65; i++) {
			int tile = (map[4 + i * 2] & 0xFF) - bias;
			int strip = map[5 + i * 2] & 0xFF;
			if (strip == 0 && tile != 255) {
				Location loc = toOriginalLocation(i);
				Tile t = pt.tiles.get(tile);
				if (t != null) {
					SurfaceFeature sf = new SurfaceFeature();
					sf.location = Location.of(loc.x + shiftX, loc.y + shiftY);
					sf.id = tile;
					sf.type = surfaceType;
					sf.tile = t;
					renderer.surface.features.add(sf);
					placeTile(t, loc.x + shiftX, loc.y + shiftY, SurfaceEntityType.BASE, null);
				}
			}
		}
	}
	/**
	 * Converts the tile x and y coordinates to map offset.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return the map offset
	 */
	public int toOriginalMapOffset(int x, int y) {
		return (x - y) * 65 + (x - y + 1) / 2 - x;
	}
	/**
	 * Convert the original index location of the map to actual (x, y) location.
	 * @param index the index into the map block, starting at 0
	 * @return the location
	 */
	public Location toOriginalLocation(int index) {
		int row = index % 65;
		int col = index / 65;
		
		int x0 = (col + 1) / 2;
		int y0 = - col / 2;
		
		int x = x0 - row;
		int y = y0 - row;
		return Location.of(x, y);
	}
	/**
	 * Place roads around buildings for the given race.
	 * @param raceId the race who builds the roads
	 */
	void placeRoads(String raceId) {
		Map<RoadType, Tile> rts = buildingModel.roadTiles.get(raceId);
		Map<Tile, RoadType> trs = buildingModel.tileRoads.get(raceId);
		// remove all roads
		Iterator<SurfaceEntity> it = renderer.surface.buildingmap.values().iterator();
		while (it.hasNext()) {
			SurfaceEntity se = it.next();
			if (se.type == SurfaceEntityType.ROAD) {
				it.remove();
			}
		}
		Set<Location> corners = new HashSet<Location>();
		for (Building bld : renderer.surface.buildings) {
			Rectangle rect = new Rectangle(bld.location.x - 1, bld.location.y + 1, bld.tileset.normal.width + 2, bld.tileset.normal.height + 2);
			addRoadAround(rts, rect, corners);
		}
		SurfaceEntity[] neighbors = new SurfaceEntity[9];
		for (Location l : corners) {
			SurfaceEntity se = renderer.surface.buildingmap.get(l);
			if (se == null || se.type != SurfaceEntityType.ROAD) {
				continue;
			}
			setNeighbors(l.x, l.y, renderer.surface.buildingmap, neighbors);
			int pattern = 0;
			
			RoadType rt1 = null;
			if (neighbors[1] != null && neighbors[1].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.TOP;
				rt1 = trs.get(neighbors[1].tile);
			}
			RoadType rt3 = null;
			if (neighbors[3] != null && neighbors[3].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.LEFT;
				rt3 = trs.get(neighbors[3].tile);
			}
			RoadType rt5 = null;
			if (neighbors[5] != null && neighbors[5].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.RIGHT;
				rt5 = trs.get(neighbors[5].tile);
			}
			RoadType rt7 = null;
			if (neighbors[7] != null && neighbors[7].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.BOTTOM;
				rt7 = trs.get(neighbors[7].tile);
			}
			RoadType rt = RoadType.get(pattern);
			// place the new tile fragment onto the map
			// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
			se = createRoadEntity(rts.get(rt));
			renderer.surface.buildingmap.put(l, se);
			// alter the four neighboring tiles to contain road back to this
			if (rt1 != null) {
				rt1 = RoadType.get(rt1.pattern | Sides.BOTTOM);
				renderer.surface.buildingmap.put(l.delta(0, 1), createRoadEntity(rts.get(rt1)));
			}
			if (rt3 != null) {
				rt3 = RoadType.get(rt3.pattern | Sides.RIGHT);
				renderer.surface.buildingmap.put(l.delta(-1, 0), createRoadEntity(rts.get(rt3)));
			}
			if (rt5 != null) {
				rt5 = RoadType.get(rt5.pattern | Sides.LEFT);
				renderer.surface.buildingmap.put(l.delta(1, 0), createRoadEntity(rts.get(rt5)));
			}
			if (rt7 != null) {
				rt7 = RoadType.get(rt7.pattern | Sides.TOP);
				renderer.surface.buildingmap.put(l.delta(0, -1), createRoadEntity(rts.get(rt7)));
			}
			
		}
	}
	/**
	 * Fills the fragment array of the 3x3 rectangle centered around x and y.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param map the map
	 * @param fragments the fragments
	 */
	void setNeighbors(int x, int y, Map<Location, SurfaceEntity> map, SurfaceEntity[] fragments) {
		fragments[0] = map.get(Location.of(x - 1, y + 1));
		fragments[1] = map.get(Location.of(x, y + 1));
		fragments[2] = map.get(Location.of(x + 1, y + 1));
		
		fragments[3] = map.get(Location.of(x - 1, y));
		fragments[4] = map.get(Location.of(x, y));
		fragments[5] = map.get(Location.of(x + 1, y));
		
		fragments[6] = map.get(Location.of(x - 1, y - 1));
		fragments[7] = map.get(Location.of(x, y - 1));
		fragments[8] = map.get(Location.of(x + 1, y - 1));
	}
	/**
	 * Places a road frame around the tilesToHighlight rectangle.
	 * @param rts the road to tile map for a concrete race
	 * @param rect the rectangle to use
	 * @param corners where to place the created corners
	 */
	void addRoadAround(Map<RoadType, Tile> rts, Rectangle rect, Collection<Location> corners) {
		Location la = Location.of(rect.x, rect.y);
		Location lb = Location.of(rect.x + rect.width - 1, rect.y);
		Location lc = Location.of(rect.x, rect.y - rect.height + 1);
		Location ld = Location.of(rect.x + rect.width - 1, rect.y - rect.height + 1);
		
		corners.add(la);
		corners.add(lb);
		corners.add(lc);
		corners.add(ld);
		
		renderer.surface.buildingmap.put(la, createRoadEntity(rts.get(RoadType.RIGHT_TO_BOTTOM)));
		renderer.surface.buildingmap.put(lb, createRoadEntity(rts.get(RoadType.LEFT_TO_BOTTOM)));
		renderer.surface.buildingmap.put(lc, createRoadEntity(rts.get(RoadType.TOP_TO_RIGHT)));
		renderer.surface.buildingmap.put(ld, createRoadEntity(rts.get(RoadType.TOP_TO_LEFT)));
		// add linear segments
		
		Tile ht = rts.get(RoadType.HORIZONTAL);
		for (int i = rect.x + 1; i < rect.x + rect.width - 1; i++) {
			renderer.surface.buildingmap.put(Location.of(i, rect.y), createRoadEntity(ht));
			renderer.surface.buildingmap.put(Location.of(i, rect.y - rect.height + 1), createRoadEntity(ht));
		}
		Tile vt = rts.get(RoadType.VERTICAL);
		for (int i = rect.y - 1; i > rect.y - rect.height + 1; i--) {
			renderer.surface.buildingmap.put(Location.of(rect.x, i), createRoadEntity(vt));
			renderer.surface.buildingmap.put(Location.of(rect.x + rect.width - 1, i), createRoadEntity(vt));
		}
	}
	/**
	 * Create a road entity for the tile.
	 * @param tile the tile
	 * @return the entity
	 */
	SurfaceEntity createRoadEntity(Tile tile) {
		SurfaceEntity result = new SurfaceEntity();
		result.tile = tile;
		result.tile.alpha = alpha;
		result.type = SurfaceEntityType.ROAD;
		return result;
	}
	/**
	 * Place the currently selected object onto the surface.
	 */
	void doPlaceObject() {
		if (renderer.surface == null) {
			return;
		}
		UndoableMapEdit undo = new UndoableMapEdit(renderer.surface);
		if (currentBaseTile != null) {
			deleteEntitiesOf(renderer.surface.basemap, renderer.placementRectangle, false);
			SurfaceFeature sf = new SurfaceFeature();
			sf.id = currentBaseTile.id;
			sf.type = currentBaseTile.type;
			sf.tile = currentBaseTile.tile;
			sf.location = Location.of(renderer.placementRectangle.x, renderer.placementRectangle.y);
			renderer.surface.features.add(sf);
			
			placeTile(currentBaseTile.tile, renderer.placementRectangle.x, renderer.placementRectangle.y, SurfaceEntityType.BASE, null);
		} else
		if (currentBuildingType != null && renderer.canPlaceBuilding(renderer.placementRectangle) && renderer.placementRectangle.width > 0) {
			Building bld = new Building(currentBuildingType, currentBuildingRace);
			bld.makeFullyBuilt();
			bld.location = Location.of(renderer.placementRectangle.x + 1, renderer.placementRectangle.y - 1); // leave room for the roads!
			renderer.surface.buildings.add(bld);
			
			placeTile(bld.tileset.normal, bld.location.x, bld.location.y, SurfaceEntityType.BUILDING, bld);
		}
		placeRoads(currentBuildingRace);
		undo.setAfter();
		addUndo(undo);
		renderer.repaint();
	}
	/**
	 * Select a building.
	 * @param mx the mouse coordinate
	 * @param my the mouse coordinate
	 */
	void doSelectBuilding(int mx, int my) {
		if (renderer.surface == null) {
			return;
		}
		Location loc = renderer.getLocationAt(mx, my);
		SurfaceEntity se = renderer.surface.buildingmap.get(loc);
		currentBuilding = null;
		if (se != null && se.type == SurfaceEntityType.BUILDING) {
			currentBuilding = se.building;
		} else { 
			se = renderer.surface.basemap.get(loc);
			if (se != null) {
				selectSurfaceEntity(se.tile);
			}
		}
		loadBuildingProperties();
		renderer.buildingBox = renderer.getBoundingRect(loc);
		renderer.repaint();
	}
	/**
	 * Select the surface tile in the lists and preview.
	 * @param tile the tile
	 */
	private void selectSurfaceEntity(Tile tile) {
		int i = 0;
		for (TileEntry te : surfaceTableModel.rows) {
			if (te.tile == tile) {
				int idx = surfaceTable.convertRowIndexToView(i);
				surfaceTable.getSelectionModel().addSelectionInterval(idx, idx);
				surfaceTable.scrollRectToVisible(surfaceTable.getCellRect(idx, 0, true));
			}
			i++;
		}		
	}
	/**
	 * Prepare the lists.
	 * @param galaxyModel the galaxy model
	 * @param buildingModel the building model
	 * @param surfaces the surfaces list
	 * @param buildings the buildings list
	 * @param races the races set
	 */
	void prepareLists(GalaxyModel galaxyModel, BuildingModel buildingModel, 
			List<TileEntry> surfaces, List<TileEntry> buildings, Set<String> races) {
		for (Map.Entry<String, PlanetType> pt : galaxyModel.planetTypes.entrySet()) {
			for (Map.Entry<Integer, Tile> te : pt.getValue().tiles.entrySet()) {
				TileEntry e = new TileEntry();
				e.id = te.getKey();
				e.surface = pt.getKey();
				e.name = findCustomName(customSurfaceNames, e.id, e.surface, "" + e.id);
				e.tile = te.getValue();
				e.tile.alpha = 1.0f;
				e.previewTile = e.tile.copy();
				e.preview = new ImageIcon(scaledImage(e.tile.getFullImage(), 32, 32));
				surfaces.add(e);
			}
		}
		Collections.sort(surfaces, new Comparator<TileEntry>() {
			@Override
			public int compare(TileEntry o1, TileEntry o2) {
				int c = o1.surface.compareTo(o2.surface);
				return c != 0 ? c : (o1.id - o2.id);
			}
		});
		
		int idx = 0;
		for (Map.Entry<String, BuildingType> bt : buildingModel.buildings.entrySet()) {
			for (Map.Entry<String, TileSet> tss : bt.getValue().tileset.entrySet()) {
				TileEntry e = new TileEntry();
				e.id = idx;
				e.surface = tss.getKey();
				e.name = findCustomName(customBuildingNames, e.id, e.surface, bt.getKey());
				e.tile = tss.getValue().normal;
				e.tile.alpha = 1.0f;
				e.previewTile = e.tile.copy();
				e.preview = new ImageIcon(scaledImage(e.tile.getFullImage(), 32, 32));
				e.buildingType = bt.getValue();
				buildings.add(e);
				
//				e = new TileEntry();
//				e.id = idx;
//				e.surface = tss.getKey();
//				e.name = bt.getKey() + " damaged";
//				e.tile = tss.getValue().damaged;
//				e.tile.alpha = 1.0f;
//				e.previewTile = e.tile.copy();
//				e.preview = new ImageIcon(scaledImage(e.tile.getFullImage(), 32, 32));
//				e.buildingType = bt.getValue();
//				buildings.add(e);
				races.add(tss.getKey());
			}
			idx++;
		}
		Collections.sort(buildings, new Comparator<TileEntry>() {
			@Override
			public int compare(TileEntry o1, TileEntry o2) {
				int c = o1.surface.compareTo(o2.surface);
				return c != 0 ? c : (o1.id - o2.id);
			}
		});
	}
	/**
	 * Find a custom name in the given entry list.
	 * @param list the entry list
	 * @param id the tile identifier
	 * @param type the tile type (e.g., suface or race)
	 * @param defaultName the default name to use if not found
	 * @return the custom name
	 */
	String findCustomName(List<TileEntry> list, int id, String type, String defaultName) {
		for (TileEntry te : list) {
			if (te.id == id && te.surface.equals(type)) {
				return te.name;
			}
		}
		return defaultName;
	}
	/**
	 * @return the building properties panel
	 */
	JPanel createBuildingPropertiesPanel() {
		ui.buildingInfoPanel = new BuildingInfoPanel();
		
		ui.buildingInfoPanel.apply.addActionListener(new Act() {
			@Override
			public void act() {
				doApplyBuildingSettings();
			}
		});
		ui.buildingInfoPanel.refresh.addActionListener(new Act() {
			@Override
			public void act() {
				displayBuildingInfo();
			}
		});

		
		return ui.buildingInfoPanel;
	}
	/**
	 * Set the GUI values from the current selected building object.
	 */
	void loadBuildingProperties() {
		if (currentBuilding != null) {
			// locate the building in the buildings list
			int i = 0;
			for (TileEntry te : buildingTableModel.rows) {
				if (te.buildingType.tileset.get(te.surface) == currentBuilding.tileset) {
					int idx = buildingTable.convertRowIndexToView(i);
					buildingTable.getSelectionModel().addSelectionInterval(idx, idx);
					buildingTable.scrollRectToVisible(buildingTable.getCellRect(idx, 0, true));
					break;
				}
				i++;
			}
			
			displayBuildingInfo();
		} else {
			ui.buildingInfoPanel.apply.setEnabled(false);
		}
	}
	/**
	 * Display the building info.
	 */
	private void displayBuildingInfo() {
		ui.buildingInfoPanel.buildingName.setText(currentBuilding.type.label);
		ui.buildingInfoPanel.completed.setText("" + currentBuilding.buildProgress);
		ui.buildingInfoPanel.completedTotal.setText("" + currentBuilding.type.hitpoints);
		ui.buildingInfoPanel.hitpoints.setText("" + currentBuilding.hitpoints);
		ui.buildingInfoPanel.hitpointsTotal.setText("" + currentBuilding.buildProgress);
		ui.buildingInfoPanel.assignedWorkers.setText("" + currentBuilding.assignedWorker);
		ui.buildingInfoPanel.workerTotal.setText("" + currentBuilding.getWorkers());
		ui.buildingInfoPanel.assignedEnergy.setText("" + currentBuilding.assignedEnergy);
		ui.buildingInfoPanel.energyTotal.setText("" + currentBuilding.getEnergy());
		ui.buildingInfoPanel.efficiency.setText(String.format("%.3f%%", currentBuilding.getEfficiency() * 100));
		ui.buildingInfoPanel.tech.setText(currentBuilding.techId);
		ui.buildingInfoPanel.cost.setText("" + currentBuilding.type.cost);
		ui.buildingInfoPanel.locationX.setText("" + currentBuilding.location.x);
		ui.buildingInfoPanel.locationY.setText("" + currentBuilding.location.y);
		ui.buildingInfoPanel.apply.setEnabled(true);
		ui.buildingInfoPanel.buildingEnabled.setSelected(currentBuilding.enabled);
		ui.buildingInfoPanel.buildingRepairing.setSelected(currentBuilding.repairing);
		
		ui.buildingInfoPanel.completionPercent.setText(String.format("  %.3f%%", currentBuilding.buildProgress * 100.0 / currentBuilding.type.hitpoints));
		ui.buildingInfoPanel.hitpointPercent.setText(String.format("  %.3f%%", currentBuilding.hitpoints * 100.0 / currentBuilding.buildProgress));
		ui.buildingInfoPanel.workerPercent.setText(String.format("  %.3f%%", currentBuilding.assignedWorker * 100.0 / currentBuilding.getWorkers()));
		ui.buildingInfoPanel.energyPercent.setText(String.format("  %.3f%%", currentBuilding.assignedEnergy * 100.0 / currentBuilding.getEnergy()));
		
		ui.buildingInfoPanel.upgradeList.removeAllItems();
		ui.buildingInfoPanel.upgradeList.addItem("None");
		for (int j = 0; j < currentBuilding.type.upgrades.size(); j++) {
			ui.buildingInfoPanel.upgradeList.addItem(currentBuilding.type.upgrades.get(j).description);
		}
		ui.buildingInfoPanel.upgradeList.setSelectedIndex(currentBuilding.upgradeLevel);
		
		ui.buildingInfoPanel.resourceTableModel.rows.clear();
		for (String r : currentBuilding.type.resources.keySet()) {
			if ("worker".equals(r) || "energy".equals(r)) {
				continue;
			}
			Resource res = new Resource();
			res.type = r;
			res.amount = currentBuilding.getResource(r);
			ui.buildingInfoPanel.resourceTableModel.rows.add(res);
		}
		ui.buildingInfoPanel.resourceTableModel.fireTableDataChanged();
	}
	/**
	 * Apply the building settings.
	 */
	void doApplyBuildingSettings() {
		if (currentBuilding == null) {
			return;
		}
		
		currentBuilding.enabled = ui.buildingInfoPanel.buildingEnabled.isSelected();
		currentBuilding.repairing = ui.buildingInfoPanel.buildingRepairing.isSelected();
		
		currentBuilding.upgradeLevel = ui.buildingInfoPanel.upgradeList.getSelectedIndex();
		if (currentBuilding.upgradeLevel > 0) {
			currentBuilding.currentUpgrade = currentBuilding.type.upgrades.get(currentBuilding.upgradeLevel - 1);
		} else {
			currentBuilding.currentUpgrade = null;
		}
		
		currentBuilding.buildProgress = Math.min(Integer.parseInt(ui.buildingInfoPanel.completed.getText()), currentBuilding.type.hitpoints);
		currentBuilding.hitpoints = Math.min(currentBuilding.buildProgress, Integer.parseInt(ui.buildingInfoPanel.hitpoints.getText()));
		currentBuilding.assignedWorker = Math.min(0, Math.max(Integer.parseInt(ui.buildingInfoPanel.assignedWorkers.getText()), currentBuilding.getWorkers()));
		currentBuilding.assignedEnergy = Math.min(0, Math.max(Integer.parseInt(ui.buildingInfoPanel.assignedEnergy.getText()), currentBuilding.getEnergy()));

		displayBuildingInfo();
		renderer.repaint();
	}
	/**
	 * Save the current planet definition to the output.
	 * @param settings the save settings
	 */
	void savePlanet(MapSaveSettings settings) {
		try {
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(settings.fileName), 1024 * 1024), "UTF-8"));
			try {
				out.printf("<?xml version='1.0' encoding='UTF-8'?>%n");
				out.printf("<planets>%n");
				out.printf("  <planet id='%s' x='%d' y='%d' radius='%d' type='%s' rotation='%s'>%n", "", 0, 0, 30, "rocky", "left-to-right");
				if (settings.surface) {
					out.printf("    <surface width='%d' height='%d'>%n", renderer.surface.width, renderer.surface.height);
					for (SurfaceFeature sf : renderer.surface.features) {
						out.printf("      <tile type='%s' id='%s' x='%d' y='%d'/>%n", sf.type, sf.id, sf.location.x, sf.location.y);
					}
					out.printf("    </surface>%n");
				}
				if (settings.buildings) {
					out.printf("    <buildings>%n");
					for (Building b : renderer.surface.buildings) {
						out.printf("      <building id='%s' tech='%s' x='%d' y='%d' build='%d' hp='%d' level='%d' worker='%d' energy='%d' enabled='%s' repairing='%s' />%n",
								b.type.id, b.techId, b.location.x, b.location.y, b.buildProgress, b.hitpoints, b.upgradeLevel, b.assignedWorker, b.assignedEnergy, b.enabled, b.repairing);
					}
					out.printf("    </buildings>%n");
				}
				out.printf("  </planet>");
				out.printf("</planets>");
			} finally {
				out.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	/** Perform the save. */
	void doSave() {
		if (saveSettings == null) {
			doSaveAs();
		} else {
			savePlanet(saveSettings);
			setTitle(TITLE + ": " + saveSettings.fileName.getAbsolutePath());
		}
	}
	/** Perform the save as. */
	void doSaveAs() {
		MapOpenSaveDialog dlg = new MapOpenSaveDialog(true, labels, saveSettings);
		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
		if (dlg.saveSettings != null) {
			saveSettings = dlg.saveSettings;
			final String fn = dlg.saveSettings.fileName.getPath();
			addRecentEntry(fn);
			doSave();
		}
	}
	/** Perform the load. */
	void doLoad() {
		MapOpenSaveDialog dlg = new MapOpenSaveDialog(false, labels, saveSettings);
		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
		if (dlg.saveSettings != null) {
			
			final String fn = dlg.saveSettings.fileName.getPath();
			addRecentEntry(fn);
			
			saveSettings = dlg.saveSettings;
			loadPlanet(saveSettings);
			setTitle(TITLE + ": " + saveSettings.fileName.getAbsolutePath());
			undoManager.discardAllEdits();
			setUndoRedoMenu();
		}		
	}
	/**
	 * Load a planet definition.
	 * @param settings the settings
	 */
	void loadPlanet(MapSaveSettings settings) {
		try {
			if (renderer.surface == null) {
				createPlanetSurface(33, 66);
			}
			Element root = XML.openXML(settings.fileName);
			Element planet = XML.childElement(root, "planet");
			if (settings.surface) {
				doClearSurfaces(false);
				Element surface = XML.childElement(planet, "surface");
				if (surface != null) {
					int width = Integer.parseInt(surface.getAttribute("width"));
					int height = Integer.parseInt(surface.getAttribute("height"));
					createPlanetSurface(width, height);
					for (Element tile : XML.childrenWithName(surface, "tile")) {
						String type = tile.getAttribute("type");
						int id = Integer.parseInt(tile.getAttribute("id"));
						int x = Integer.parseInt(tile.getAttribute("x"));
						int y = Integer.parseInt(tile.getAttribute("y"));
						
						Tile t = galaxyModel.planetTypes.get(type).tiles.get(id);
						SurfaceFeature sf = new SurfaceFeature();
						sf.id = id;
						sf.type = type;
						sf.location = Location.of(x, y);
						sf.tile = t;
						renderer.surface.features.add(sf);
						
						placeTile(t, x, y, SurfaceEntityType.BASE, null);
					}
				}
			}
			if (settings.buildings) {
				doClearBuildings(false);
				Element buildings = XML.childElement(planet, "buildings");
				if (buildings != null) {
					String tech = null;
					for (Element tile : XML.childrenWithName(buildings, "building")) {
						String id = tile.getAttribute("id");
						tech = tile.getAttribute("tech");
						
						Building b = new Building(buildingModel.buildings.get(id), tech);
						int x = Integer.parseInt(tile.getAttribute("x"));
						int y = Integer.parseInt(tile.getAttribute("y"));
					
						b.location = Location.of(x, y);
						
						b.buildProgress = Integer.parseInt(tile.getAttribute("build"));
						b.hitpoints = Integer.parseInt(tile.getAttribute("hp"));
						b.setLevel(Integer.parseInt(tile.getAttribute("level")));
						b.assignedEnergy = Integer.parseInt(tile.getAttribute("energy"));
						b.assignedWorker = Integer.parseInt(tile.getAttribute("worker"));
						b.enabled = "true".equals(tile.getAttribute("enabled"));
						b.repairing = "true".equals(tile.getAttribute("repairing"));
						renderer.surface.buildings.add(b);
						
						placeTile(b.tileset.normal, x, y, SurfaceEntityType.BUILDING, b);
					}
					if (tech != null) {
						placeRoads(tech);
					}
				}
			}
			renderer.repaint();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	/**
	 * Set the UI labels for the given language.
	 * @param language the new language
	 */
	void setLabels(String language) {
		if (rl == null) {
			deferredLanguage = language;
			return;
		}
		labels.load(rl, language, "campaign/main");
		renameFieldsOf(ui);
		renameFieldsOf(ui.allocationPanel);
		renameFieldsOf(ui.buildingInfoPanel);
		propertyTab.setTitleAt(0, labels.get("mapeditor.surface_and_buildings"));
		propertyTab.setTitleAt(1, labels.get("mapeditor.building_properties"));
		propertyTab.setTitleAt(2, labels.get("mapeditor.allocation"));
		surfaceTableModel.colNames[0] = labels.get("mapeditor.tile_preview");
		surfaceTableModel.colNames[1] = labels.get("mapeditor.tile_size");
		surfaceTableModel.colNames[2] = labels.get("mapeditor.tile_name");
		surfaceTableModel.colNames[3] = labels.get("mapeditor.tile_surface");

		buildingTableModel.colNames[0] = labels.get("mapeditor.tile_preview");
		buildingTableModel.colNames[1] = labels.get("mapeditor.tile_size");
		buildingTableModel.colNames[2] = labels.get("mapeditor.tile_name");
		buildingTableModel.colNames[3] = labels.get("mapeditor.tile_race");
		
		surfaceTableModel.fireTableStructureChanged();
		buildingTableModel.fireTableStructureChanged();
		
		filterSurface.setToolTipText(labels.get("mapeditor.filter_surface"));
		filterBuilding.setToolTipText(labels.get("mapeditor.filter_building"));
		alphaSlider.setToolTipText(labels.get("mapeditor.preview_brightness"));
		
		surfaceTable.getColumnModel().getColumn(0).setPreferredWidth(45);
		surfaceTable.getColumnModel().getColumn(1).setPreferredWidth(32);
		buildingTable.getColumnModel().getColumn(0).setPreferredWidth(45);
		buildingTable.getColumnModel().getColumn(1).setPreferredWidth(32);
	}
	/**
	 * Rename the annotated fields of the object.
	 * @param o the target object
	 */
	void renameFieldsOf(Object o) {
		for (Field f : o.getClass().getDeclaredFields()) {
			Rename fr = f.getAnnotation(Rename.class);
			if (fr != null) {
				try {
					if (AbstractButton.class.isAssignableFrom(f.getType())) {
						AbstractButton.class.cast(f.get(o)).setText(labels.get(fr.to()));
					}
					if (JLabel.class.isAssignableFrom(f.getType())) {
						JLabel.class.cast(f.get(o)).setText(labels.get(fr.to()));
					}
					if (String.class.isAssignableFrom(f.getType())) {
						f.set(o, labels.get(fr.to()));
					}
				} catch (IllegalAccessException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	/**
	 * The state capture for the surface editing.
	 * @author karnokd
	 */
	class UndoableMapEdit implements UndoableEdit {
		/** The reference surface. */
		final PlanetSurface surface;
		/** The before state of the basemap. */
		Map<Location, SurfaceEntity> basemapBefore;
		/** The after state of the basemap. */
		Map<Location, SurfaceEntity> basemapAfter;
		/** The building map before. */
		Map<Location, SurfaceEntity> buildingmapBefore;
		/** The building map after. */
		Map<Location, SurfaceEntity> buildingmapAfter;
		/** The surface features before. */
		List<SurfaceFeature> surfaceBefore;
		/** The surface features after. */
		List<SurfaceFeature> surfaceAfter;
		/** The buildings before. */
		List<Building> buildingsBefore;
		/** The buildings after. */
		List<Building> buildingsAfter;
		/**
		 * Constructor.
		 * @param surface sets the surface object
		 */
		public UndoableMapEdit(PlanetSurface surface) {
			this.surface = surface;
			basemapBefore = new HashMap<Location, SurfaceEntity>(surface.basemap);
			surfaceBefore = new ArrayList<SurfaceFeature>(surface.features);
			
			buildingmapBefore = new HashMap<Location, SurfaceEntity>(surface.buildingmap);
			buildingsBefore = new ArrayList<Building>(surface.buildings);
		}
		/**
		 * Set the after status of the surface.
		 */
		public void setAfter() {
			basemapAfter = new HashMap<Location, SurfaceEntity>(surface.basemap);
			surfaceAfter = new ArrayList<SurfaceFeature>(surface.features);
			
			buildingmapAfter = new HashMap<Location, SurfaceEntity>(surface.buildingmap);
			buildingsAfter = new ArrayList<Building>(surface.buildings);
		}
		/**
		 * Restore the state to before.
		 */
		void restoreBefore() {
			surface.basemap.clear();
			surface.basemap.putAll(basemapBefore);
			surface.buildingmap.clear();
			surface.buildingmap.putAll(buildingmapBefore);
			surface.features.clear();
			surface.features.addAll(surfaceBefore);
			surface.buildings.clear();
			surface.buildings.addAll(buildingsBefore);
		}
		/**
		 * Restore the state after.
		 */
		void restoreAfter() {
			surface.basemap.clear();
			surface.basemap.putAll(basemapAfter);
			surface.buildingmap.clear();
			surface.buildingmap.putAll(buildingmapAfter);
			surface.features.clear();
			surface.features.addAll(surfaceAfter);
			surface.buildings.clear();
			surface.buildings.addAll(buildingsAfter);
		}
		@Override
		public boolean addEdit(UndoableEdit anEdit) {
			// TODO Auto-generated method stub
			return false;
		}
		@Override
		public boolean canRedo() {
			return true;
		}
		@Override
		public boolean canUndo() {
			return true;
		}
		@Override
		public void die() {
			// TODO Auto-generated method stub
			
		}
		@Override
		public String getPresentationName() {
			return "Map editing";
		}
		@Override
		public String getRedoPresentationName() {
			return "Redo map editing";
		}
		@Override
		public String getUndoPresentationName() {
			return "Undo map editing";
		}
		@Override
		public boolean isSignificant() {
			return true;
		}
		@Override
		public void redo() {
			restoreAfter();
		}
		@Override
		public boolean replaceEdit(UndoableEdit anEdit) {
			return false;
		}
		@Override
		public void undo() {
			restoreBefore();
		}
	}
	/** Save the current editor state. */
	void saveConfig() {
		try {
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("open-ig-mapeditor-config.xml"), 64 * 1024), "UTF-8"));
			try {
				out.printf("<?xml version='1.0' encoding='UTF-8'?>%n");
				out.printf("<mapeditor-config>%n");
				out.printf("  <window x='%d' y='%d' width='%d' height='%d' state='%d'/>%n", getX(), getY(), getWidth(), getHeight(), getExtendedState());
				out.printf("  <language id='%s'/>%n", ui.languageEn.isSelected() ? "en" : "hu");
				out.printf("  <splitters main='%d' preview='%d' surfaces='%d'/>%n", split.getDividerLocation(), toolSplit.getDividerLocation(), featuresSplit.getDividerLocation());
				out.printf("  <editmode type='%s'/>%n", ui.buildButton.isSelected());
				out.printf("  <tabs selected='%d'/>%n", propertyTab.getSelectedIndex());
				out.printf("  <lights preview='%d' map='%s'/>%n", alphaSlider.getValue(), Float.toString(alpha));
				out.printf("  <filter surface='%s' building='%s'/>%n", XML.toHTML(filterSurface.getText()), XML.toHTML(filterBuilding.getText()));
				out.printf("  <allocation worker='%s' strategy='%d'/>%n", ui.allocationPanel.availableWorkers.getText(), ui.allocationPanel.strategies.getSelectedIndex());
				out.printf("  <view buildings='%s' minimap='%s' textboxes='%s' zoom='%s'/>%n", ui.viewShowBuildings.isSelected(), 
						ui.viewSymbolicBuildings.isSelected(), ui.viewTextBackgrounds.isSelected(), Double.toString(renderer.scale));
				out.printf("  <custom-surface-names>%n");
				for (TileEntry te : surfaceTableModel.rows) {
					out.printf("    <tile id='%s' type='%s' name='%s'/>%n", te.id, XML.toHTML(te.surface), XML.toHTML(te.name));
				}
				out.printf("  </custom-surface-names>%n");
				out.printf("  <custom-building-names>%n");
				for (TileEntry te : buildingTableModel.rows) {
					out.printf("    <tile id='%s' type='%s' name='%s'/>%n", te.id, XML.toHTML(te.surface), XML.toHTML(te.name));
				}
				out.printf("  </custom-building-names>%n");
				out.printf("  <recent>%n");
				for (int i = ui.fileRecent.getItemCount() - 1; i >= 2 ; i--) {
					out.printf("    <entry file='%s'/>%n", XML.toHTML(ui.fileRecent.getItem(i).getText()));
				}
				out.printf("  </recent>%n");
				out.printf("</mapeditor-config>%n");
			} finally {
				out.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	/** Load the editor configuration. */
	void loadConfig() {
		File file = new File("open-ig-mapeditor-config.xml");
		if (file.canRead()) {
			try {
				Element root = XML.openXML(file);
				
				// reposition the window
				Element eWindow = XML.childElement(root, "window");
				if (eWindow != null) {
					setBounds(
						Integer.parseInt(eWindow.getAttribute("x")),
						Integer.parseInt(eWindow.getAttribute("y")),
						Integer.parseInt(eWindow.getAttribute("width")),
						Integer.parseInt(eWindow.getAttribute("height"))
					);
					setExtendedState(Integer.parseInt(eWindow.getAttribute("state")));
				}
				Element eLanguage = XML.childElement(root, "language");
				if (eLanguage != null) {
					String langId = eLanguage.getAttribute("id");
					if ("hu".equals(langId)) {
						ui.languageHu.setSelected(true);
						ui.languageHu.doClick();
					} else {
						ui.languageEn.setSelected(true);
						ui.languageEn.doClick();
					}
				}
				
				Element eSplitters = XML.childElement(root, "splitters");
				if (eSplitters != null) {
					split.setDividerLocation(Integer.parseInt(eSplitters.getAttribute("main")));
					toolSplit.setDividerLocation(Integer.parseInt(eSplitters.getAttribute("preview")));
					featuresSplit.setDividerLocation(Integer.parseInt(eSplitters.getAttribute("surfaces")));
				}

				Element eTabs = XML.childElement(root, "tabs");
				if (eTabs != null) {
					propertyTab.setSelectedIndex(Integer.parseInt(eTabs.getAttribute("selected")));
				}
				
				Element eLights = XML.childElement(root, "lights");
				if (eLights != null) {
					alphaSlider.setValue(Integer.parseInt(eLights.getAttribute("preview")));
					alpha = Float.parseFloat(eLights.getAttribute("map"));
				}
				
				Element eMode = XML.childElement(root, "editmode");
				if (eMode != null) {
					if ("true".equals(eMode.getAttribute("type"))) {
						ui.buildButton.doClick();
					}
				}
				
				Element eView = XML.childElement(root, "view");
				if (eView != null) {
					ui.viewShowBuildings.setSelected(false);
					if ("true".equals(eView.getAttribute("buildings"))) {
						ui.viewShowBuildings.doClick();
					}
					ui.viewSymbolicBuildings.setSelected(false);
					if ("true".equals(eView.getAttribute("minimap"))) {
						ui.viewSymbolicBuildings.doClick();
					}
					
					ui.viewTextBackgrounds.setSelected(false);
					if ("true".equals(eView.getAttribute("textboxes"))) {
						ui.viewTextBackgrounds.doClick();
					}
					renderer.scale = Double.parseDouble(eView.getAttribute("zoom"));
				}
				
				Element eSurfaces = XML.childElement(root, "custom-surface-names");
				if (eSurfaces != null) {
					for (Element tile : XML.childrenWithName(eSurfaces, "tile")) {
						TileEntry te = new TileEntry();
						te.id = Integer.parseInt(tile.getAttribute("id"));
						te.surface = tile.getAttribute("type");
						te.name = tile.getAttribute("name");
						customSurfaceNames.add(te);
					}
				}
				
				Element eBuildigns = XML.childElement(root, "custom-building-names");
				if (eBuildigns != null) {
					for (Element tile : XML.childrenWithName(eBuildigns, "tile")) {
						TileEntry te = new TileEntry();
						te.id = Integer.parseInt(tile.getAttribute("id"));
						te.surface = tile.getAttribute("type");
						te.name = tile.getAttribute("name");
						customBuildingNames.add(te);
					}
				}
				Element eFilter = XML.childElement(root, "filter");
				if (eFilter != null) {
					filterSurface.setText(eFilter.getAttribute("surface"));
					filterBuilding.setText(eFilter.getAttribute("building"));
				}
				Element eAlloc = XML.childElement(root, "allocation");
				if (eAlloc != null) {
					ui.allocationPanel.availableWorkers.setText(eAlloc.getAttribute("worker"));
					ui.allocationPanel.strategies.setSelectedIndex(Integer.parseInt(eAlloc.getAttribute("strategy")));
				}
				Element eRecent = XML.childElement(root, "recent");
				if (eRecent != null) {
					for (Element r : XML.childrenWithName(eRecent, "entry")) {
						addRecentEntry(r.getAttribute("file")); 
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	/** Clear all recent entries. */
	void doClearRecent() {
		ui.fileRecent.removeAll();
		ui.fileRecent.add(ui.clearRecent);
	}
	/**
	 * Open a recent file.
	 * @param fileName the filename
	 */
	void doOpenRecent(String fileName) {
		saveSettings.fileName = new File(fileName);
		doLoad();
	}
	/**
	 * Add a recent file entry.
	 * @param fileName the filename
	 */
	void addRecentEntry(final String fileName) {
		if (recent.add(fileName)) {
			if (ui.fileRecent.getItemCount() < 2) {
				ui.fileRecent.addSeparator();
			}
			JMenuItem item = new JMenuItem(fileName);
			item.addActionListener(new Act() {
				@Override
				public void act() {
					doOpenRecent(fileName);
				}
			});
			ui.fileRecent.insert(item, 2);
		}
	}
}
