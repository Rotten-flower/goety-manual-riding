package dev.flower.manualriding;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.function.Consumer;

/** 输入只发槽位，服务端决定招式、目标与冷却。 / Only the server resolves combat. */
public final class RiderNetwork {
    public static Consumer<Status> clientReceiver = value -> {};
    public static void init(IEventBus bus) { bus.addListener(RiderNetwork::register); }
    private static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Input.TYPE, Input.CODEC, (input, context) -> {
            if (context.player() instanceof ServerPlayer player && input.slot >= -1 && input.slot < 5)
                MountCombat.input(player, input.slot);
        });
        registrar.playToClient(Status.TYPE, Status.CODEC, (status, context) -> clientReceiver.accept(status));
    }
    public static void send(int slot) { PacketDistributor.sendToServer(new Input(slot)); }
    public static void sync(ServerPlayer player, MountCombat.Control control) {
        int[] remaining = new int[Abilities.of(control.mob).length];
        for (int i = 0; i < remaining.length; i++) remaining[i] = control.remaining(i);
        PacketDistributor.sendToPlayer(player, new Status(control.mob.getId(), control.isManual(),
                control.busy(), control.activeSlot, remaining, control.accepted));
    }
    public record Input(int slot) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ManualRiding.ID, "input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeByte(value.slot), buffer -> new Input(buffer.readByte()));
        @Override public Type<Input> type() { return TYPE; }
    }
    public record Status(int mountId, boolean manual, boolean busy, int activeSlot, int[] remaining,
                         int accepted) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ManualRiding.ID, "status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeVarInt(value.mountId);
                    buffer.writeBoolean(value.manual);
                    buffer.writeBoolean(value.busy);
                    buffer.writeByte(value.activeSlot);
                    buffer.writeVarIntArray(value.remaining);
                    buffer.writeVarInt(value.accepted);
                }, buffer -> new Status(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
                        buffer.readByte(), buffer.readVarIntArray(5), buffer.readVarInt()));
        @Override public Type<Status> type() { return TYPE; }
    }
}