package dev.flower.manualriding;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** 输入只发槽位，服务端决定招式、目标与冷却。 / Only the server resolves combat. */
public final class RiderNetwork {
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(ManualRiding.ID, "riding"),
                    () -> "1",
                    "1"::equals,
                    "1"::equals);

    public static void init() {
        CHANNEL.registerMessage(
                0,
                Input.class,
                Input::encode,
                Input::decode,
                Input::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(
                1,
                Status.class,
                Status::encode,
                Status::decode,
                Status::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(int slot) {
        CHANNEL.sendToServer(new Input(slot));
    }

    public static void sync(ServerPlayer p, MountCombat.Control c) {
        int[] remaining = new int[Abilities.of(c.mob).length];
        for (int i = 0; i < remaining.length; i++) remaining[i] = c.remaining(i);
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> p),
                new Status(
                        c.mob.getId(),
                        c.isManual(),
                        c.busy(),
                        c.activeSlot,
                        remaining,
                        c.accepted));
    }

    public record Input(int slot) {
        static void encode(Input p, FriendlyByteBuf b) {
            b.writeByte(p.slot);
        }

        static Input decode(FriendlyByteBuf b) {
            return new Input(b.readByte());
        }

        static void handle(Input p, Supplier<NetworkEvent.Context> context) {
            var c = context.get();
            c.enqueueWork(
                    () -> {
                        if (c.getSender() != null && p.slot >= -1 && p.slot < 5)
                            MountCombat.input(c.getSender(), p.slot);
                    });
            c.setPacketHandled(true);
        }
    }

    public record Status(
            int mountId,
            boolean manual,
            boolean busy,
            int activeSlot,
            int[] remaining,
            int accepted) {
        static void encode(Status p, FriendlyByteBuf b) {
            b.writeVarInt(p.mountId);
            b.writeBoolean(p.manual);
            b.writeBoolean(p.busy);
            b.writeByte(p.activeSlot);
            b.writeVarIntArray(p.remaining);
            b.writeVarInt(p.accepted);
        }

        static Status decode(FriendlyByteBuf b) {
            return new Status(
                    b.readVarInt(),
                    b.readBoolean(),
                    b.readBoolean(),
                    b.readByte(),
                    b.readVarIntArray(5),
                    b.readVarInt());
        }

        static void handle(Status p, Supplier<NetworkEvent.Context> context) {
            var c = context.get();
            c.enqueueWork(
                    () ->
                            DistExecutor.unsafeRunWhenOn(
                                    Dist.CLIENT, () -> () -> ClientControls.receive(p)));
            c.setPacketHandled(true);
        }
    }
}
