package com.experiencedbutter.villagetransit.mixin;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.TrainManifest;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.passenger.PassengerSeatBlock;
import com.experiencedbutter.villagetransit.passenger.SeatInspectMenu;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create lets players sit on any contraption seat without asking the block, so the only place to
 * enforce seat rules is this method.
 *
 * <ul>
 *   <li>sneak + use on a seat holding a traveller opens the read-only traveller view,</li>
 *   <li>plain use on such a seat is refused (config `seat.blockPlayerSeatWhenOccupied`),</li>
 *   <li>seats occupied by another player are left to Create's own logic.</li>
 * </ul>
 *
 * <p>This is the single mixin of the mod and it only reacts to our own seats.</p>
 */
@Mixin(AbstractContraptionEntity.class)
public abstract class AbstractContraptionEntityMixin {

    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), cancellable = true)
    private void vt$seatRules(Player player, BlockPos localPos, Direction side,
                              InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        if (!(self instanceof CarriageContraptionEntity carriage)) {
            return;
        }
        Contraption contraption = self.getContraption();
        if (contraption == null) {
            return;
        }
        StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof PassengerSeatBlock)) {
            return;
        }

        boolean sneaking = player.isShiftKeyDown();
        if (!sneaking && !VTConfig.get().blockPlayerSeatWhenOccupied) {
            return;
        }
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            // Client side: cancel the "inspect" gesture so the server stays authoritative.
            if (sneaking) {
                cir.setReturnValue(true);
            }
            return;
        }

        ItemStack seatStack = occupant(serverLevel, carriage, localPos);
        if (seatStack == null) {
            return;
        }
        boolean marker = Travellers.isMarker(seatStack);

        if (sneaking) {
            if (marker) {
                player.displayClientMessage(Component.translatable("message.create_village_transit.seat_by_player"), true);
            } else {
                player.openMenu(new SimpleMenuProvider(
                                (id, inventory, p) -> new SeatInspectMenu(id, inventory, seatStack),
                                Component.translatable("screen.create_village_transit.seat")),
                        buffer -> ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer,
                                seatStack.copyWithCount(1)));
            }
            cir.setReturnValue(true);
            return;
        }

        if (!marker) {
            player.displayClientMessage(Component.translatable("message.create_village_transit.seat_occupied"), true);
            cir.setReturnValue(true);
        }
    }

    private static ItemStack occupant(ServerLevel level, CarriageContraptionEntity carriage, BlockPos localPos) {
        TrainManifest manifest = VillageTransitData.get(level.getServer()).peekManifest(carriage.trainId);
        if (manifest == null) {
            return null;
        }
        ItemStack stack = manifest.seats().get(new TrainManifest.SeatKey(carriage.carriageIndex, localPos.immutable()));
        return stack == null || stack.isEmpty() ? null : stack;
    }
}
