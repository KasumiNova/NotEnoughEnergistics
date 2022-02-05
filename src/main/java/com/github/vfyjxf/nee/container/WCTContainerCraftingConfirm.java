package com.github.vfyjxf.nee.container;

import appeng.api.storage.ITerminalHost;
import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerCraftConfirm;

public class WCTContainerCraftingConfirm extends ContainerCraftConfirm {

    private TilePatternInterface tile;
    private int patternIndex;
    private boolean hasWorkCommitted = false;

    public WCTContainerCraftingConfirm(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
    }

    @Override
    public void startJob() {
        super.startJob();
        hasWorkCommitted = true;
    }

    @Override
    public void onContainerClosed(EntityPlayer par1EntityPlayer) {
        super.onContainerClosed(par1EntityPlayer);
        if (tile != null && !hasWorkCommitted) {
            this.tile.getPatternInventory().setInventorySlotContents(patternIndex, null);
            this.tile.updateCraftingList();
        }
    }

    public void setTile(TilePatternInterface tile) {
        this.tile = tile;
    }

    public void setPatternIndex(int patternIndex) {
        this.patternIndex = patternIndex;
    }

}
