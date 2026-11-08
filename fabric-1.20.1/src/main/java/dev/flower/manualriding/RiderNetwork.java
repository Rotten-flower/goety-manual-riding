package dev.flower.manualriding;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** 输入只发槽位，服务端决定招式、目标与冷却。 / Only the server resolves combat. */
public final class RiderNetwork {
    public static final ResourceLocation INPUT = new ResourceLocation(ManualRiding.ID,"input");
    public static final ResourceLocation STATUS = new ResourceLocation(ManualRiding.ID,"status");
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(INPUT,(server,player,handler,buffer,sender) -> {
            int slot=buffer.readByte();
            if (slot>=-1 && slot<5) server.execute(() -> MountCombat.input(player,slot));
        });
    }
    public static void send(int slot) {
        var buffer=PacketByteBufs.create();buffer.writeByte(slot);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(INPUT,buffer);
    }
    public static void sync(ServerPlayer player,MountCombat.Control control) {
        int[] remaining=new int[Abilities.of(control.mob).length];
        for(int i=0;i<remaining.length;i++)remaining[i]=control.remaining(i);
        var status=new Status(control.mob.getId(),control.isManual(),control.busy(),control.activeSlot,remaining,control.accepted);
        var buffer=PacketByteBufs.create();status.encode(buffer);
        ServerPlayNetworking.send(player,STATUS,buffer);
    }
    public record Status(int mountId,boolean manual,boolean busy,int activeSlot,int[] remaining,int accepted) {
        void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(mountId);buffer.writeBoolean(manual);buffer.writeBoolean(busy);
            buffer.writeByte(activeSlot);buffer.writeVarIntArray(remaining);buffer.writeVarInt(accepted);
        }
        static Status decode(FriendlyByteBuf buffer) {
            return new Status(buffer.readVarInt(),buffer.readBoolean(),buffer.readBoolean(),buffer.readByte(),buffer.readVarIntArray(5),buffer.readVarInt());
        }
    }
}
