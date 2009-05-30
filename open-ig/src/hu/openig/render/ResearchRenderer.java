/*
 * Copyright 2008-2009, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.render;

import hu.openig.ani.Player;
import hu.openig.core.Btn;
import hu.openig.core.LabInfo;
import hu.openig.core.SwappableRenderer;
import hu.openig.model.GameBuildingPrototype;
import hu.openig.model.GameWorld;
import hu.openig.model.ResearchTech;
import hu.openig.res.GameResourceManager;
import hu.openig.res.gfx.CommonGFX;
import hu.openig.res.gfx.ResearchGFX;
import hu.openig.res.gfx.TextGFX;
import hu.openig.sound.SoundFXPlayer;
import hu.openig.utils.JavaUtils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JComponent;

/**
 * Research screen renderer.
 * @author karnokd
 */
public class ResearchRenderer extends JComponent implements SwappableRenderer {
	/** The serial version id. */
	private static final long serialVersionUID = 1724420039211512267L;
	/** The common graphics. */
	private final CommonGFX cgfx;
	/** The research graphics. */
	private final ResearchGFX gfx;
	/** The text graphics. */
	final TextGFX text;
	/** The last rendering position. */
	private final AchievementRenderer achievementRenderer;
	/** The user interface sounds. */
	SoundFXPlayer uiSound;
	/** The information bar renderer. */
	private InfobarRenderer infobarRenderer;
	/** The game world. */
	private GameWorld gameWorld;
	/** The last width. */
	private int lastWidth;
	/** The last height. */
	private int lastHeight;
	/** The main screen rectangle. */
	private final Rectangle screenRect = new Rectangle();
	/** The current selected class. */
	private int clazzIndex;
	/** The current selected type. */
	private int typeIndex;
	/** The areas for the main options labels. */
	private final Rectangle[] rectMainOptions = new Rectangle[] {
		new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()
	};
	/** Areas for the current research selector arrow. */
	private final Rectangle[] rectMainArrow = new Rectangle[] {
		new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()
	};
	/** The areas for the sub options labels. */
	private final Rectangle[] rectSubOptions = {
		new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()	
	};
	/** Areas for the current research selector arrow in sub options. */
	private final Rectangle[] rectSubArrow = {
		new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()	
	};
	/** The actual research rectangles. */
	private final Rectangle[] rectResearch = new Rectangle[] {
		new Rectangle(), new Rectangle(),
		new Rectangle(), new Rectangle(),
		new Rectangle(), new Rectangle()
	};
	/** Animation rectangle. */
	private final Rectangle rectAnimation = new Rectangle();
	/** The animation player. */
	private Player anim = new Player(this);
	/** The animation research tech. */
	private ResearchTech selectedTech;
	/** The lock that protects the swapping and drawing of the frontbuffer. */
	private Lock swapLock = new ReentrantLock();
	/** The backbuffer image. */
	private BufferedImage backbuffer;
	/** The frontbuffer image.*/
	private BufferedImage frontbuffer;
	/** To skip the blackness on start. */
	private volatile boolean justInited;
	/** The areas for required research. */
	private final Rectangle[] rectNeeded = new Rectangle[] {
		new Rectangle(), new Rectangle(), new Rectangle()
	};
	/** Project name rectangle. */
	private final Rectangle rectProjectName = new Rectangle();
	/** Project status rectangle. */
	private final Rectangle rectProjectStatus = new Rectangle();
	/** Completed rectangle. */
	private final Rectangle rectCompleted = new Rectangle();
	/** Time remaining rectangle. */
	private final Rectangle rectTimeRemaining = new Rectangle();
	/** Number of civil enginering rectangle. */
	private final Rectangle rectCiv = new Rectangle();
	/** Number of mechanical research rectangle. */
	private final Rectangle rectMech = new Rectangle();
	/** Number of computer research rectangle. */
	private final Rectangle rectComp = new Rectangle();
	/** Number of AU research rectangle. */
	private final Rectangle rectAI = new Rectangle();
	/** Number of Military research rectangle. */
	private final Rectangle rectMil = new Rectangle();
	/** Start button location. */
	private Btn btnStart;
	/** Equipment rectangle. */
	private Btn btnEquipment;
	/** Production rectangle. */
	private Btn btnProduction;
	/** Bridge rectangle. */
	private Btn btnBridge;
	/** The current project name. */
	private final Rectangle rectProject = new Rectangle();
	/** The current project money. */
	private final Rectangle rectMoney = new Rectangle();
	/** The more money button area. */
	private final Rectangle rectMoreMoney = new Rectangle();
	/** The less money button area. */
	private final Rectangle rectLessMoney = new Rectangle();
	/** The adjust money entire button rectangle. */
	private final Rectangle rectMoneyAdjust = new Rectangle();
	/** The current percent. */
	private final Rectangle rectPercent = new Rectangle();
	/** Current number of civil enginerring rectangle. */
	private final Rectangle rectCurrCiv = new Rectangle();
	/** Current number of mechanical research rectangle. */
	private final Rectangle rectCurrMech = new Rectangle();
	/** Current number of computer research rectangle. */
	private final Rectangle rectCurrComp = new Rectangle();
	/** Current number of AI research rectangle. */
	private final Rectangle rectCurrAI = new Rectangle();
	/** Current number of military research rectangle. */
	private final Rectangle rectCurrMil = new Rectangle();
	/** View current research in the tech tree. */
	private Btn btnView;
	/** Stop research. */
	private Btn btnStop;
	/** The research description area. */
	private final Rectangle rectDescription = new Rectangle();
	/** List of buttons which fire on mouse release. */
	private final List<Btn> releaseButtons = JavaUtils.newArrayList();
	/** Is the currently animating technology available. */
	private boolean selectedTechAvail;
	/**
	 * Returns the back buffer which is safe to draw to at any time.
	 * The get should be initiated by the party who is supplying the images.
	 * @return the backbuffer image.
	 */
	public BufferedImage getBackbuffer() {
		if (backbuffer == null) {
			throw new IllegalStateException("init() not called");
		}
		if (justInited) {
			justInited = false;
			return new BufferedImage(backbuffer.getWidth(), backbuffer.getHeight(), BufferedImage.TYPE_INT_ARGB);
		}
		return backbuffer;
	}
	/**
	 * Swap the front and backbuffers.
	 * The swap must be initiated by the party who is supplying the images.
	 */
	public void swap() {
		swapLock.lock();
		try {
			BufferedImage tmp = frontbuffer;
			frontbuffer = backbuffer;
			backbuffer = tmp;
		} finally {
			swapLock.unlock();
		}
		repaint(rectAnimation);
	}
	/**
	 * Initialize the drawing buffers with the defined size.
	 * Use setScalingMode() to enable resizing of the actual image.
	 * @param width the image width
	 * @param height the image height.
	 */
	public void init(int width, int height) {
		swapLock.lock();
		try {
			// create new buffer only if the animation size changes
			if (backbuffer == null || backbuffer.getWidth() != width || backbuffer.getHeight() != height) {
				backbuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				frontbuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				// disable acceleration on these images?! they change so frequently it
				// is just overhead to move them back and forth between the memory and VRAM on
				// invalidate
				backbuffer.setAccelerationPriority(0);
				frontbuffer.setAccelerationPriority(0);
			}
			justInited = true;
		} finally {
			swapLock.unlock();
		}
	}

	/**
	 * Constructor. Sets the graphics objects.
	 * @param grm the resource manager
	 * @param uiSound the sounds
	 * @param infobarRenderer information bar renderer
	 * @param achievementRenderer achievement renderer
	 */
	public ResearchRenderer(GameResourceManager grm, 
			SoundFXPlayer uiSound, InfobarRenderer infobarRenderer,
			AchievementRenderer achievementRenderer) {		
		this.gfx = grm.researchGFX;
		this.cgfx = grm.commonGFX;
		this.text = cgfx.text;
		this.uiSound = uiSound;
		this.infobarRenderer = infobarRenderer;
		this.achievementRenderer = achievementRenderer;
		MouseActions ma = new MouseActions();
		this.addMouseListener(ma);
		this.addMouseMotionListener(ma);
		this.addMouseWheelListener(ma);
		initButtons();
	}
	/** Initialize buttons. */
	private void initButtons() {
		btnStart = new Btn();
		btnEquipment = new Btn();
		btnProduction = new Btn();
		btnBridge = new Btn();
		btnView = new Btn();
		btnStop = new Btn();
		
		releaseButtons.add(btnStart);
		releaseButtons.add(btnEquipment);
		releaseButtons.add(btnProduction);
		releaseButtons.add(btnBridge);
		releaseButtons.add(btnView);
		releaseButtons.add(btnStop);
	}
	/**
	 * @param gameWorld the gameWorld to set
	 */
	public void setGameWorld(GameWorld gameWorld) {
		this.gameWorld = gameWorld;
	}
	/**
	 * @return the gameWorld
	 */
	public GameWorld getGameWorld() {
		return gameWorld;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		int w = getWidth();
		int h = getHeight();

		if (true) {
			alphablendBackground(g2);
		}
		if (w != lastWidth || h != lastHeight) {
			lastWidth = w;
			lastHeight = h;
			// if the render window changes, re-zoom to update scrollbars
			updateRegions();
		}
		// RENDER INFOBARS
		infobarRenderer.renderInfoBars(this, g2);
		
		g2.drawImage(gfx.researchScreen, screenRect.x, screenRect.y, null);
		// draw main options
		for (int i = 0; i < rectMainOptions.length; i++) {
			if (i + 1 == clazzIndex) {
				g2.drawImage(gfx.mainOptionsLight[i], rectMainOptions[i].x, rectMainOptions[i].y, null);
			} else {
				g2.drawImage(gfx.mainOptions[i], rectMainOptions[i].x, rectMainOptions[i].y, null);
			}
		}
		if (clazzIndex > 0) {
			BufferedImage[] images = gfx.subOptions[clazzIndex - 1];
			BufferedImage[] imagesLight = gfx.subOptionsLight[clazzIndex - 1];
			for (int i = 0; i < images.length; i++) {
				if (typeIndex == i + 1) {
					g2.drawImage(imagesLight[i], rectSubOptions[i].x, rectSubOptions[i].y, null);
				} else {
					g2.drawImage(images[i], rectSubOptions[i].x, rectSubOptions[i].y, null);
				}
			}
		}
		if (typeIndex > 0) {
			List<ResearchTech> rtl = gameWorld.player.getResearchFor(clazzIndex, typeIndex);
			for (int i = 0; i < rtl.size(); i++) {
				ResearchTech rt = rtl.get(i);
				g2.drawImage(rt.smallImage, rectResearch[i].x, rectResearch[i].y, null);
				if (gameWorld.isAvailable(rt)) {
					text.paintTo(g2, rectResearch[i].x + 2, rectResearch[i].y + 2, 7, TextGFX.GREEN, Integer.toString(rt.buildCost));
					if (!"Buildings".equals(rt.clazz)) {
						text.paintTo(g2, rectResearch[i].x + 3, rectResearch[i].y + rectResearch[i].height - 18, 7, 
								TextGFX.RED, Integer.toString(gameWorld.getInventoryCount(rt)));
					}
				} else {
					g2.setColor(Color.BLACK);
					for (int j = 1; j < rectResearch[i].height; j += 2) {
						g2.drawLine(rectResearch[i].x, rectResearch[i].y + j, rectResearch[i].x + rectResearch[i].width - 1, rectResearch[i].y + j);
					}
					if (!gameWorld.isResearchable(rt)) {
						g2.drawImage(gfx.researchDisabled, rectResearch[i].x + (rectResearch[i].width - gfx.researchDisabled.getWidth()) / 2, 
								rectResearch[i].y + (rectResearch[i].height - gfx.researchDisabled.getHeight()) / 2, null);
					}
				}
				if (rt == gameWorld.player.selectedTech) {
					g2.setColor(Color.RED);
					g2.drawRect(rectResearch[i].x, rectResearch[i].y, rectResearch[i].width - 1, rectResearch[i].height - 1);
					g2.drawRect(rectResearch[i].x - 1, rectResearch[i].y - 1, rectResearch[i].width + 1, rectResearch[i].height + 1);
				}
				text.paintTo(g2, rectResearch[i].x + 3, rectResearch[i].y + rectResearch[i].height - 9, 7, 
						rt == gameWorld.player.selectedTech ? TextGFX.RED : TextGFX.GREEN, rt.name);
			}
		}
		ResearchTech rt = gameWorld.player.selectedTech;
		if (rt != this.selectedTech || (this.selectedTech != null && gameWorld.isAvailable(this.selectedTech) != selectedTechAvail)
				|| (this.selectedTech == null && !anim.isPlayback())) {
			this.selectedTech = rt;
			this.selectedTechAvail = gameWorld.isAvailable(this.selectedTech);
			if (rt == null || rt.animation == null || !gameWorld.isResearchable(rt)) {
				anim.stopAndWait();
				anim.setFilename(gfx.emptyAnimation.getAbsolutePath());
			} else {
				if (gameWorld.player.availableTechnology.contains(rt) || rt.animationWired == null) {
					anim.setFilename(rt.animation.getAbsolutePath());
				} else {
					anim.setFilename(rt.animationWired.getAbsolutePath());
				}
			}
			anim.setLoop(true);
			anim.setNoAudio(true);
			anim.setMemoryPlayback(true);
			anim.startPlayback();
		}
		renderAnimation(g2);
		LabInfo li = gameWorld.player.getLabInfo();
		if (rt != null) {
			// display prerequisite names
			Shape sp = g2.getClip();
			for (int i = 0; i < Math.min(rt.requires.size(), rectNeeded.length); i++) {
				ResearchTech rrt = rt.requires.get(i);
//				g2.setClip(rectNeeded[i]);
				int color = gameWorld.player.availableTechnology.contains(rrt) 
				? TextGFX.ORANGE : (gameWorld.isResearchable(rrt) ? TextGFX.GREEN : TextGFX.GRAY);
				text.paintTo(g2, rectNeeded[i].x + 2, rectNeeded[i].y + 2, 7, color, rrt.name);
			}
			text.paintTo(g2, rectProjectName.x + 2, rectProjectName.y, 10, TextGFX.GREEN, rt.name);
			boolean details = true;
			boolean researchable = gameWorld.isResearchable(rt);
			if (gameWorld.isAvailable(rt)) {
				text.paintTo(g2, rectProjectStatus.x + 2, rectProjectStatus.y, 10, 
						TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Status.Researched"));
			} else
			if (researchable) {
				text.paintTo(g2, rectProjectStatus.x + 2, rectProjectStatus.y, 10, 
						TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Status.Researchable"));
			} else {
				text.paintTo(g2, rectProjectStatus.x + 2, rectProjectStatus.y, 10, 
						TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Status.NotResearchable"));
				details = false;
			}
			String n = rt.description[0];
			int l = text.getTextWidth(10, n);
			text.paintTo(g2, rectDescription.x + (rectDescription.width - l) / 2, rectDescription.y + 2, 10, TextGFX.RED, n);
			if (details) {
				String desc = rt.description[1] + " " + rt.description[2];
				String[] words = desc.split("\\s+");
				int y = rectDescription.y + 17;
				int x = rectDescription.x + 3;
				int sl = text.getTextWidth(7, " ");
				for (String wd : words) {
					int len = text.getTextWidth(7, wd);
					if (x + len > rectDescription.x + rectDescription.width - 4) {
						y += 12;
						x = rectDescription.x + 3;
					}
					text.paintTo(g2, x, y, 7, TextGFX.GREEN, wd);
					x += len + sl;
				}
			}
			if (researchable) {
				text.paintTo(g2, rectCiv.x, rectCiv.y + 1, 14, li.currentCivil < rt.civil ? TextGFX.RED : TextGFX.GREEN, Integer.toString(rt.civil));
				text.paintTo(g2, rectMech.x, rectMech.y + 1, 14, li.currentMechanic < rt.mechanic ? TextGFX.RED : TextGFX.GREEN, Integer.toString(rt.mechanic));
				text.paintTo(g2, rectComp.x, rectComp.y + 1, 14, li.currentComputer < rt.computer ? TextGFX.RED : TextGFX.GREEN, Integer.toString(rt.computer));
				text.paintTo(g2, rectAI.x, rectAI.y + 1, 14, li.currentAi < rt.ai ? TextGFX.RED : TextGFX.GREEN, Integer.toString(rt.ai));
				text.paintTo(g2, rectMil.x, rectMil.y + 1, 14, li.currentMilitary < rt.military ? TextGFX.RED : TextGFX.GREEN, Integer.toString(rt.military));
			}
			g2.setClip(sp);
		}
		text.paintTo(g2, rectCurrCiv.x, rectCurrCiv.y + 1, 14, li.currentCivil < li.totalCivil ? TextGFX.YELLOW : TextGFX.GREEN, Integer.toString(li.currentCivil));
		text.paintTo(g2, rectCurrMech.x, rectCurrMech.y + 1, 14, li.currentMechanic < li.totalMechanic ? TextGFX.YELLOW : TextGFX.GREEN, Integer.toString(li.currentMechanic));
		text.paintTo(g2, rectCurrComp.x, rectCurrComp.y + 1, 14, li.currentComputer < li.totalComputer ? TextGFX.YELLOW : TextGFX.GREEN, Integer.toString(li.currentComputer));
		text.paintTo(g2, rectCurrAI.x, rectCurrAI.y + 1, 14, li.currentAi < li.totalAi ? TextGFX.YELLOW : TextGFX.GREEN, Integer.toString(li.currentAi));
		text.paintTo(g2, rectCurrMil.x, rectCurrMil.y + 1, 14, li.currentMilitary < li.totalMilitary ? TextGFX.YELLOW : TextGFX.GREEN, Integer.toString(li.currentMilitary));

		btnStart.disabled = rt == null || gameWorld.isAvailable(rt) || !gameWorld.isResearchable(rt);
		if (!btnStart.disabled) {
			if (btnStart.down) {
				g2.drawImage(gfx.btnStartDown, btnStart.rect.x, btnStart.rect.y, null);
			} else {
				g2.drawImage(gfx.btnStart, btnStart.rect.x, btnStart.rect.y, null);
			}
		}
		if (btnEquipment.down) {
			g2.drawImage(gfx.btnEquipmentDown, btnEquipment.rect.x, btnEquipment.rect.y, null);
		}
		if (btnProduction.down) {
			g2.drawImage(gfx.btnProductDown, btnProduction.rect.x, btnProduction.rect.y, null);
		}
		if (btnBridge.down) {
			g2.drawImage(gfx.btnBridgeDown, btnBridge.rect.x, btnBridge.rect.y, null);
		}
		if (btnView.disabled) {
			g2.drawImage(gfx.btnEmpty, btnView.rect.x, btnView.rect.y, null);
		} else
		if (btnView.down) {
			g2.drawImage(gfx.btnViewDown, btnView.rect.x, btnView.rect.y, null);
		}
		if (btnStop.disabled) {
			g2.drawImage(gfx.btnEmpty, btnStop.rect.x, btnStop.rect.y, null);
		} else
		if (btnStop.down) {
			g2.drawImage(gfx.btnStopDown, btnStop.rect.x, btnStop.rect.y, null);
		}
		
		achievementRenderer.renderAchievements(g2, this);
	}
	/**
	 * Alpha blends the background beneath this component.
	 * @param g2 the graphics object
	 */
	private void alphablendBackground(Graphics2D g2) {
		Composite cp = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, getWidth(), getHeight());
		g2.setComposite(cp);
	}
	/**
	 * Renders the animation frame.
	 * @param g2 the graphics object
	 */
	private void renderAnimation(Graphics2D g2) {
		swapLock.lock();
		try {
			g2.drawImage(frontbuffer, rectAnimation.x, rectAnimation.y, null);
		} finally {
			swapLock.unlock();
		}
	}
	/**
	 * Update location of various interresting rectangles of objects.
	 */
	private void updateRegions() {
		infobarRenderer.updateRegions(this);
		int w = getWidth();
		int h = getHeight();
		screenRect.setBounds((w - gfx.researchScreen.getWidth()) / 2, (h - gfx.researchScreen.getHeight()) / 2, 
				gfx.researchScreen.getWidth(), gfx.researchScreen.getHeight());
		
		for (int i = 0; i < rectMainOptions.length; i++) {
			rectMainOptions[i].setBounds(screenRect.x + 341, screenRect.y + 10 + i * 22, 168, 20);
			rectMainArrow[i].setBounds(screenRect.x + 325, screenRect.y + 13 + i * 22, 15, 11);
		}
		for (int i = 0; i < rectSubOptions.length; i++) {
			rectSubOptions[i].setBounds(screenRect.x + 342, screenRect.y + 112 + i * 16, 168, 14);
			rectSubArrow[i].setBounds(screenRect.x + 325, screenRect.y + 112 + i * 16, 15, 11);
		}
		for (int i = 0; i < rectResearch.length; i++) {
			rectResearch[i].setBounds(screenRect.x + 3 + 106 * i, screenRect.y + 201, 104, 78);
		};
		rectAnimation.setBounds(screenRect.x + 2, screenRect.y + 2, 316, 196);
		for (int i = 0; i < rectNeeded.length; i++) {
			rectNeeded[i] = new Rectangle(screenRect.x + 539, screenRect.y + 139 + i * 18, 92, 10);
		}
		rectProjectName.setBounds(screenRect.x + 128, screenRect.y + 292, 156, 10);
		rectProjectStatus.setBounds(screenRect.x + 128, screenRect.y + 311, 156, 10);
		rectProject.setBounds(screenRect.x + 128, screenRect.y + 344, 156, 10);
		rectMoney.setBounds(screenRect.x + 128, screenRect.y + 365, 122, 10);
		rectPercent.setBounds(screenRect.x + 259, screenRect.y + 365, 25, 10);
		
		rectLessMoney.setBounds(screenRect.x + 9, screenRect.y + 361, 28, 19);
		rectMoreMoney.setBounds(screenRect.x + 38, screenRect.y + 361, 28, 19);
		rectMoneyAdjust.setBounds(screenRect.x + 9, screenRect.y + 361, 57, 19);
		
		rectDescription.setBounds(screenRect.x + 9, screenRect.y + 393, 515, 41);
		
		btnStart.setBounds(screenRect.x + 534, screenRect.y + 283, 102, 39);
		btnEquipment.setBounds(screenRect.x + 534, screenRect.y + 322, 102, 39);
		btnProduction.setBounds(screenRect.x + 534, screenRect.y + 361, 102, 39);
		btnBridge.setBounds(screenRect.x + 534, screenRect.y + 400, 102, 39);
		
		btnView.setBounds(screenRect.x + 291, screenRect.y + 360, 115, 23);
		btnStop.setBounds(screenRect.x + 410, screenRect.y + 360, 115, 23);
		
		rectCompleted.setBounds(screenRect.x + 314, screenRect.y + 290, 52, 14);
		rectTimeRemaining.setBounds(screenRect.x + 471, screenRect.y + 290, 51, 14);
		
		rectCiv.setBounds(screenRect.x + 319, screenRect.y + 310, 12, 15);
		rectMech.setBounds(screenRect.x + 373, screenRect.y + 310, 12, 15);
		rectComp.setBounds(screenRect.x + 430, screenRect.y + 310, 12, 15);
		rectAI.setBounds(screenRect.x + 471, screenRect.y + 310, 12, 15);
		rectMil.setBounds(screenRect.x + 512, screenRect.y + 310, 12, 15);

		rectCurrCiv.setBounds(screenRect.x + 319, screenRect.y + 341, 12, 15);
		rectCurrMech.setBounds(screenRect.x + 373, screenRect.y + 341, 12, 15);
		rectCurrComp.setBounds(screenRect.x + 430, screenRect.y + 341, 12, 15);
		rectCurrAI.setBounds(screenRect.x + 471, screenRect.y + 341, 12, 15);
		rectCurrMil.setBounds(screenRect.x + 512, screenRect.y + 341, 12, 15);
	}
	/**
	 * Class to handle mouse actions in the research screen. 
	 * @author karnokd
	 */
	private class MouseActions extends MouseAdapter {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mousePressed(MouseEvent e) {
			Point pt = e.getPoint();
			for (int i = 0; i < rectMainOptions.length; i++) {
				if (rectMainOptions[i].contains(pt)) {
					clazzIndex = i + 1;
					// fix sorter suboptions
					if (typeIndex > gfx.subOptions[i].length) {
						typeIndex = gfx.subOptions[i].length;
					}
					repaint(screenRect);
					return;
				}
			}
			if (clazzIndex > 0) {
				BufferedImage[] opts = gfx.subOptions[clazzIndex - 1];
				for (int i = 0; i < rectSubOptions.length; i++) {
					if (rectSubOptions[i].contains(pt) && i < opts.length) {
						typeIndex = i + 1;
						repaint(screenRect);
						return;
					}
				}
			}
			if (typeIndex > 0 && clazzIndex > 0) {
				List<ResearchTech> rtl = gameWorld.player.getResearchFor(clazzIndex, typeIndex);
				for (int i = 0; i < rtl.size(); i++) {
					if (rectResearch[i].contains(pt)) {
						gameWorld.player.selectedTech = rtl.get(i);
						// if this is a building, select its prototype
						if ("Buildings".equals(gameWorld.player.selectedTech.clazz)) {
							for (GameBuildingPrototype bp : gameWorld.buildingPrototypes) {
								if (bp.researchTech == gameWorld.player.selectedTech) {
									gameWorld.player.selectedBuildingPrototype = bp;
								}
							}
						}
						repaint(screenRect);
						return;
					}
				}
			}
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (gameWorld.player.selectedTech != null) {
					for (int i = 0; i < Math.min(rectNeeded.length, gameWorld.player.selectedTech.requires.size()); i++) {
						if (rectNeeded[i].contains(pt)) {
							gameWorld.player.selectedTech = gameWorld.player.selectedTech.requires.get(i);
							clazzIndex = gameWorld.player.selectedTech.clazzIndex;
							typeIndex = gameWorld.player.selectedTech.typeIndex;
							repaint();
						}
					}
				}
				for (Btn b : releaseButtons) {
					if (b.test(pt)) {
						b.down = true;
						repaint(b.rect);
					}
				}
			}
		}
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseReleased(MouseEvent e) {
			Point pt = e.getPoint();
			if (e.getButton() == MouseEvent.BUTTON1) {
				for (Btn b : releaseButtons) {
					if (b.test(pt) && b.down) {
						b.click();
					}
					b.down = false;
					repaint(b.rect);
				}
			}
		}
	}
	/**
	 * Starts the animations.
	 */
	public void startAnimation() {
		if (anim.getFilename() != null) {
			anim.startPlayback();
		}
	}
	/** Stops the animations. */
	public void stopAnimation() {
		anim.stopAndWait();
	}
	/**
	 * Sets the list indexes to display the currently selected technology if any.
	 */
	public void selectCurrentTech() {
		if (gameWorld.player.selectedTech != null) {
			clazzIndex = gameWorld.player.selectedTech.clazzIndex;
			typeIndex = gameWorld.player.selectedTech.typeIndex;
		} else {
			clazzIndex = 0;
			typeIndex = 0;
		}
	}
}
