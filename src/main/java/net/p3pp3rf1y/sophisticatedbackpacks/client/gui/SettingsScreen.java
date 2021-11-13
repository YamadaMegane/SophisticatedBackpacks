package net.p3pp3rf1y.sophisticatedbackpacks.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.SettingsContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.settings.BackpackSettingsTabControl;

import javax.annotation.Nullable;
import java.util.Optional;

public class SettingsScreen extends AbstractContainerScreen<SettingsContainer> {
	private BackpackSettingsTabControl settingsTabControl;

	public SettingsScreen(SettingsContainer screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
		imageHeight = 114 + getMenu().getNumberOfRows() * 18;
		imageWidth = getMenu().getBackpackBackgroundProperties().getSlotsOnLine() * 18 + 14;
		inventoryLabelY = imageHeight - 94;
		inventoryLabelX = 8 + getMenu().getBackpackBackgroundProperties().getPlayerInventoryXOffset();
	}

	@Override
	protected void init() {
		super.init();

		settingsTabControl = new BackpackSettingsTabControl(this, new Position(leftPos + imageWidth, topPos + 4));
		addWidget(settingsTabControl);
	}

	@Override
	protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
		BackpackBackgroundProperties backpackBackgroundProperties = getMenu().getBackpackBackgroundProperties();
		BackpackGuiHelper.renderBackpackBackground(new Position((width - imageWidth) / 2, (height - imageHeight) / 2), matrixStack, getMenu().getBackpackInventorySlots().size(), getMenu().getSlotsOnLine(), backpackBackgroundProperties.getTextureName(), imageWidth, minecraft, menu.getNumberOfRows());
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		menu.detectSettingsChangeAndReload();
		renderBackground(matrixStack);
		settingsTabControl.render(matrixStack, mouseX, mouseY, partialTicks);
		matrixStack.translate(0, 0, 200);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		settingsTabControl.afterScreenRender(matrixStack, mouseX, mouseY, partialTicks);
		renderTooltip(matrixStack, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
		super.renderLabels(matrixStack, mouseX, mouseY);
		for (int slotId = 0; slotId < menu.ghostSlots.size(); ++slotId) {
			Slot slot = menu.ghostSlots.get(slotId);
			renderSlot(matrixStack, slot);

			settingsTabControl.renderSlotOverlays(matrixStack, slot, this::renderSlotOverlay);

			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				hoveredSlot = slot;
				renderSlotOverlay(matrixStack, slot, getSlotColor(slotId));
			}
		}
	}

	@Override
	protected void renderSlot(PoseStack poseStack, Slot slot) {
		Optional<ItemStack> memorizedStack = getMenu().getMemorizedStackInSlot(slot.getSlotIndex());
		ItemStack itemstack = slot.getItem();
		if (memorizedStack.isPresent()) {
			itemstack = memorizedStack.get();
		}

		setBlitOffset(100);
		itemRenderer.blitOffset = 100.0F;

		RenderSystem.enableDepthTest();
		itemRenderer.renderAndDecorateItem(itemstack, slot.x, slot.y);

		itemRenderer.blitOffset = 0.0F;
		setBlitOffset(0);

		if (memorizedStack.isPresent()) {
			drawMemorizedStackOverlay(poseStack, slot.x, slot.y);
		}
	}

	private void drawMemorizedStackOverlay(PoseStack poseStack, int x, int y) {
		poseStack.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiHelper.GUI_CONTROLS);
		blit(poseStack, x, y, 77, 0, 16, 16);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	@SuppressWarnings("java:S2589") // slot can actually be null despite being marked non null
	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
		//noinspection ConstantConditions
		if (slot != null) {
			settingsTabControl.handleSlotClick(slot, mouseButton);
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		Slot slot = findSlot(mouseX, mouseY);
		if (slot != null) {
			settingsTabControl.handleSlotClick(slot, button);
		}
		return true;
	}

	@Nullable
	@Override
	protected Slot findSlot(double mouseX, double mouseY) {
		for (int i = 0; i < menu.ghostSlots.size(); ++i) {
			Slot slot = menu.ghostSlots.get(i);
			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				return slot;
			}
		}

		return null;
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton) && hasClickedOutsideOfSettings(mouseX, mouseY);
	}

	private boolean hasClickedOutsideOfSettings(double mouseX, double mouseY) {
		return settingsTabControl.getTabRectangles().stream().noneMatch(r -> r.contains((int) mouseX, (int) mouseY));
	}

	private void renderSlotOverlay(PoseStack matrixStack, Slot slot, int slotColor) {
		RenderSystem.disableDepthTest();
		int xPos = slot.x;
		int yPos = slot.y;
		RenderSystem.colorMask(true, true, true, false);
		fillGradient(matrixStack, xPos, yPos, xPos + 16, yPos + 16, slotColor, slotColor);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.enableDepthTest();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			PacketHandler.sendToServer(new BackpackOpenMessage());
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected void renderTooltip(PoseStack matrixStack, int x, int y) {
		super.renderTooltip(matrixStack, x, y);
		GuiHelper.renderTooltip(minecraft, matrixStack, x, y);
	}

	public static SettingsScreen constructScreen(SettingsContainer settingsContainer, Inventory playerInventory, Component title) {
		return new SettingsScreen(settingsContainer, playerInventory, title);
	}

	public BackpackSettingsTabControl getSettingsTabControl() {
		return settingsTabControl;
	}
}
