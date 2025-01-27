package io.github.fabricators_of_create.porting_lib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRenderer;
import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRendererRegistry;
import io.github.fabricators_of_create.porting_lib.item.ArmorTextureItem;

import io.github.fabricators_of_create.porting_lib.util.ArmorTextureRegistry;
import io.github.fabricators_of_create.porting_lib.util.client.ClientHooks;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin extends RenderLayer<LivingEntity, HumanoidModel<LivingEntity>> {

	public HumanoidArmorLayerMixin(RenderLayerParent<LivingEntity, HumanoidModel<LivingEntity>> renderLayerParent) {
		super(renderLayerParent);
	}

	@Inject(method = "getArmorLocation", at = @At("HEAD"), cancellable = true)
	private void port_lib$getArmorLocation(ArmorItem armorItem, boolean bl, String string, CallbackInfoReturnable<ResourceLocation> cir) {
		ResourceLocation id = ArmorTextureRegistry.get(armorItem.getMaterial());
		if (id != null) {
			cir.setReturnValue(id);
		}
	}

	@WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ArmorItem;Lnet/minecraft/client/model/HumanoidModel;ZFFFLjava/lang/String;)V"))
	public<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> void renderModel(HumanoidArmorLayer<T, M, A> instance, PoseStack matrices, MultiBufferSource vertexConsumers, int light, ArmorItem armorItem, A model, boolean withGlint, float red, float green, float blue, @Nullable String armorSuffix, Operation<Void> original, @Local(argsOnly = true) T entity, @Local(argsOnly = true) EquipmentSlot armorSlot, @Local ItemStack itemStack) {
		if(itemStack.getItem() instanceof ArmorTextureItem) {
			ResourceLocation resourceLocation = ClientHooks.getArmorResource(entity, itemStack, armorSlot, null);
			VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
					vertexConsumers, RenderType.armorCutoutNoCull(resourceLocation), false, itemStack.hasFoil()
			);
			model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	@Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
	public<T extends LivingEntity, A extends HumanoidModel<T>> void port_lib$armorRegistry(PoseStack matrices, MultiBufferSource vertexConsumers, T entity, EquipmentSlot armorSlot, int light, HumanoidModel<LivingEntity> armorModel, CallbackInfo ci) {
		ItemStack stack = entity.getItemBySlot(armorSlot);
		ArmorRenderer renderer = ArmorRendererRegistry.get(stack.getItem());

		if (renderer != null) {
			renderer.render(matrices, vertexConsumers, stack, entity, armorSlot, light, getParentModel(), armorModel);
			ci.cancel();
		}
	}
}
