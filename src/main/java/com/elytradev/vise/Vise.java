package com.elytradev.vise;

import java.util.List;
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.elytradev.concrete.reflect.accessor.Accessor;
import com.elytradev.concrete.reflect.accessor.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;

@Mod(modid="vise", name="Vise", version="@VERSION@", clientSideOnly=true)
public class Vise {
	public static final Logger log = LogManager.getLogger("Vise");
	
	@Instance
	public static Vise inst;
	
	private final Accessor<GuiOptionsRowList> optionsRowList = Accessors.findField(GuiVideoSettings.class, "field_146501_h", "optionsRowList");
	private final Accessor<List<GuiOptionsRowList.Row>> options = Accessors.findField(GuiOptionsRowList.class, "field_148184_k", "options");
	private final Accessor<GuiButton> buttonA = Accessors.findField(GuiOptionsRowList.Row.class, "field_148323_b", "buttonA");
	private final Accessor<GuiButton> buttonB = Accessors.findField(GuiOptionsRowList.Row.class, "field_148324_c", "buttonB");
	
	private boolean ingameRenderTakenOver = false;
	private boolean chatRenderTakenOver = false;
	
	public static int hudScale;
	public static int tooltipScale;
	
	private Configuration cfg;
	
	@EventHandler
	public void onPreInit(FMLPreInitializationEvent e) {
		cfg = new Configuration(e.getSuggestedConfigurationFile());
		if (cfg.hasKey("scale", "hud")) {
			hudScale = cfg.getInt("hud", "scale", 0, 0, 16, "");
			tooltipScale = cfg.getInt("tooltip", "scale", 0, 0, 16, "");
		} else {
			hudScale = Minecraft.getMinecraft().gameSettings.guiScale;
			tooltipScale = Minecraft.getMinecraft().gameSettings.guiScale;
		}
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public static void saveConfig() {
		inst.cfg.get("scale", "hud", 0).set(hudScale);
		inst.cfg.get("scale", "tooltip", 0).set(tooltipScale);
		inst.cfg.save();
	}
	
	private int clampScale(int scale) {
		int max = getMaxScale();
		if (scale == 0 || scale > max) {
			return max;
		}
		return scale;
	}
	
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void onPreInitGui(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (e.getGui() instanceof GuiChat) {
			if (chatRenderTakenOver) return;
			chatRenderTakenOver = true;
			e.setCanceled(true);
			int scale = clampScale(hudScale);
			ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
			float f = scale/(float)res.getScaleFactor();
			e.getGui().setWorldAndResolution(Minecraft.getMinecraft(), (int)(e.getGui().width/f), (int)(e.getGui().height/f));
			chatRenderTakenOver = false;
		}
	}
	
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void onPreDrawGui(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (e.getGui() instanceof GuiChat) {
			int scale = clampScale(hudScale);
			ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
			float f = scale/(float)res.getScaleFactor();
			GlStateManager.pushMatrix();
			GlStateManager.scale(f, f, f);
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onPostDrawGui(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (e.getGui() instanceof GuiChat) {
			GlStateManager.popMatrix();
		}
	}
	
	@SubscribeEvent
	public void onPostInitGui(GuiScreenEvent.InitGuiEvent.Post e) {
		if (e.getGui() instanceof GuiVideoSettings) {
			GuiOptionsRowList gorl = optionsRowList.get(e.getGui());
			List<GuiOptionsRowList.Row> li = options.get(gorl);
			ListIterator<GuiOptionsRowList.Row> iter = li.listIterator();
			boolean found = false;
			while (iter.hasNext()) {
				GuiOptionsRowList.Row row = iter.next();
				GuiButton a = buttonA.get(row);
				GuiButton b = buttonB.get(row);
				boolean subst = false;
				if (a instanceof GuiOptionButton) {
					GuiOptionButton gob = (GuiOptionButton)a;
					if (gob.getOption() == Options.GUI_SCALE) {
						a = new GuiViseButton(99999, a.x, a.y, a.width, a.height, I18n.format("options.vise.scale"));
						subst = true;
					}
				}
				if (b instanceof GuiOptionButton) {
					GuiOptionButton gob = (GuiOptionButton)b;
					if (gob.getOption() == Options.GUI_SCALE) {
						b = new GuiViseButton(99999, b.x, b.y, b.width, b.height, I18n.format("options.vise.scale"));
						subst = true;
					}
				}
				if (subst) {
					iter.set(new GuiOptionsRowList.Row(a, b));
					found = true;
					break;
				}
			}
			if (!found) {
				log.warn("Couldn't find a GUI scale button to substitute!");
			}
		}
	}
	
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void onPreRenderTooltip(RenderTooltipEvent.Pre e) {
		int scale = clampScale(tooltipScale);
		GlStateManager.pushMatrix();
		ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
		float f = scale/(float)res.getScaleFactor();
		GlStateManager.scale(f, f, f);
		e.setX((int)(e.getX()/f));
		e.setY((int)(e.getY()/f));
		e.setScreenWidth((int)(e.getScreenWidth()/f));
		e.setScreenHeight((int)(e.getScreenHeight()/f));
		e.setMaxWidth((int)(e.getMaxWidth()/f));
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onPostRenderTooltip(RenderTooltipEvent.PostText e) {
		GlStateManager.popMatrix();
	}
	
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void onPreRenderGameOverlay(RenderGameOverlayEvent.Pre e) {
		if (e.getType() == ElementType.ALL) {
			if (ingameRenderTakenOver) return;
			int scale = clampScale(hudScale);
			int oldScale = Minecraft.getMinecraft().gameSettings.guiScale;
			try {
				Minecraft.getMinecraft().gameSettings.guiScale = scale;
				ingameRenderTakenOver = true;
				e.setCanceled(true);
				Minecraft.getMinecraft().ingameGUI.renderGameOverlay(e.getPartialTicks());
				ingameRenderTakenOver = false;
			} finally {
				Minecraft.getMinecraft().gameSettings.guiScale = oldScale;
				Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
			}
		}
	}
	
	public static int getMaxScale() {
		Minecraft mc = Minecraft.getMinecraft();
		int maxScaleW = (mc.displayWidth/320);
		int maxScaleH = (mc.displayHeight/240);
		int maxScale = Math.min(maxScaleW, maxScaleH);
		return maxScale;
	}

	private static final class GuiViseButton extends GuiButton {
		public GuiViseButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
			super(buttonId, x, y, widthIn, heightIn, buttonText);
		}
		
		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			boolean sup = super.mousePressed(mc, mouseX, mouseY);
			if (sup) {
				mc.displayGuiScreen(new GuiScaleSettings(mc.currentScreen));
			}
			return sup;
		}
	}

}
