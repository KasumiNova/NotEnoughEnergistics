package com.github.vfyjxf.nee.network.packet;

import appeng.container.AEBaseContainer;
import com.github.vfyjxf.nee.jei.CraftingHelperTransferHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketValueConfigClient implements IMessage, IMessageHandler<PacketValueConfigClient, IMessage> {

    private String name;
    private String value;

    public PacketValueConfigClient() {

    }

    public PacketValueConfigClient(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public PacketValueConfigClient(String name) {
        this.name = name;
        this.value = "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        this.value = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name);
        ByteBufUtils.writeUTF8String(buf, this.value);
    }

    @Override
    public IMessage onMessage(PacketValueConfigClient message, MessageContext ctx) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        Container container = player.openContainer;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if ("PatternInterface.check".equals(message.name) && Boolean.parseBoolean(message.value)) {
                if (container instanceof AEBaseContainer) {
                    CraftingHelperTransferHandler.setIsPatternInterfaceExists(true);
                }
            }
        });
        return null;
    }
}