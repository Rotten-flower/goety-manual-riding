package dev.flower.manualriding;
import net.fabricmc.api.ClientModInitializer;
/** 客户端绑定与回包分离于专服入口。 / Keep client code out of dedicated server initialization. */
public final class RidingClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientControls.init();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(RiderNetwork.STATUS,
                (client, handler, buffer, sender) -> {
                    var status = RiderNetwork.Status.decode(buffer);
                    client.execute(() -> ClientControls.receive(status));
                });
    }
}
