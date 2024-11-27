package io.github.fabricators_of_create.porting_lib.entity.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerInteractionEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.server.network.ServerGamePacketListenerImpl$1")
public class ServerGamePacketListenerImplInteractHandlerMixin {
	@WrapOperation(
			method = "method_33898",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Entity;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
			)
	)
	private static InteractionResult handleInteract(Entity entity, Player player, Vec3 pos, InteractionHand hand, Operation<InteractionResult> original) {
		InteractionResult result = PlayerInteractionEvents.INTERACT_ENTITY_POSITIONED.invoker().onInteract(player, entity, pos, hand);
		if (result != null)
			return result;

		return original.call(entity, player, pos, hand);
	}
}
