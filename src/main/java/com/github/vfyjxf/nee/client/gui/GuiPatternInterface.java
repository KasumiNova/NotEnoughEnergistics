package com.github.vfyjxf.nee.client.gui;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotRestrictedInput;
import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import com.github.vfyjxf.nee.client.gui.widgets.GuiImgButtonRemove;
import com.github.vfyjxf.nee.container.ContainerPatternInterface;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketValueConfigServer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GuiPatternInterface extends AEBaseGui {

    private final ContainerPatternInterface container;
    private GuiImgButtonRemove removeButton;

    public GuiPatternInterface(InventoryPlayer playerInventory, TilePatternInterface tile) {
        super(new ContainerPatternInterface(playerInventory, tile));
        this.xSize = 211;
        this.ySize = 197;
        this.container = (ContainerPatternInterface) this.inventorySlots;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.removeButton = new GuiImgButtonRemove(this.guiLeft + 85, this.guiTop + this.ySize - 173);
        addButton(removeButton);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {

    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, 211 - 34, this.ySize);

    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == this.removeButton) {
            if(container.getSelectedSlot() != null) {
                NEENetworkHandler.getInstance().sendToServer(new PacketValueConfigServer("Gui.PatternInterface"));
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        if (btn == 0 && this.getSlotUnderMouse() instanceof SlotRestrictedInput) {
            SlotRestrictedInput slot = (SlotRestrictedInput) this.getSlotUnderMouse();
            NEENetworkHandler.getInstance().sendToServer(new PacketValueConfigServer("Container.selectedSlot", Integer.toString(slot.slotNumber)));
            container.setSelectedSlotIndex(slot.slotNumber);
        }
        super.mouseClicked(mouseX, mouseY, btn);
    }

    private ResourceLocation getBackground() {
        return new ResourceLocation(NotEnoughEnergistics.MODID, "textures/gui/pattern_interface.png");
    }
}
