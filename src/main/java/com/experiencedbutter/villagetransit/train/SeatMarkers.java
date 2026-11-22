package com.experiencedbutter.villagetransit.train;

import com.experiencedbutter.villagetransit.data.TrainManifest;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A seat a player sits on holds a special "player" item, so the seat counts as occupied for every
 * other rule (boarding, settlement, inspection). Markers are removed when the player gets up.
 */
public final class SeatMarkers {

    private SeatMarkers() {
    }

    public static void sync(TrainManifest manifest, Map<TrainManifest.SeatKey, UUID> occupied,
                            MinecraftServer server) {
        Set<TrainManifest.SeatKey> seats = occupied.keySet();
        manifest.seats().entrySet().removeIf(entry ->
                Travellers.isMarker(entry.getValue()) && !seats.contains(entry.getKey()));
        occupied.forEach((seat, playerId) -> {
            if (manifest.seats().containsKey(seat)) {
                return;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            String name = player == null ? "player" : player.getGameProfile().getName();
            manifest.seats().put(seat, Travellers.marker(name));
        });
    }

    public static boolean hasMarkers(TrainManifest manifest) {
        for (ItemStack stack : manifest.seats().values()) {
            if (Travellers.isMarker(stack)) {
                return true;
            }
        }
        return false;
    }
}
