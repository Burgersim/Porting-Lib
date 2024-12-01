package io.github.fabricators_of_create.porting_lib.mixin.common;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.StructureTemplateExtensions;
import io.github.fabricators_of_create.porting_lib.util.StructureTemplateUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.phys.Vec3;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin implements StructureTemplateExtensions {
	@Unique
	private static final ThreadLocal<StructurePlaceSettings> currentSettings = new ThreadLocal<>();

	@Shadow
	@Final
	private List<StructureEntityInfo> entityInfoList;

	@Shadow
	protected abstract void placeEntities(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, Mirror mirror, Rotation rotation, BlockPos blockPos2, @Nullable BoundingBox boundingBox, boolean bl);

	@WrapOperation(
			method = "placeInWorld",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;placeEntities(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Mirror;Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Z)V"
			)
	)
	private void grabSettings(StructureTemplate self, ServerLevelAccessor level, BlockPos pos, Mirror mirror, Rotation rotation,
							  BlockPos pivot, BoundingBox bounds, boolean finalize, Operation<Void> original,
							  @Local(argsOnly = true) StructurePlaceSettings settings) {
		currentSettings.set(settings);
		original.call(self, level, pos, mirror, rotation, pivot, bounds, finalize);
		currentSettings.remove();
	}

	@ModifyExpressionValue(
			method = "placeEntities",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;entityInfoList:Ljava/util/List;"
			)
	)
	private List<StructureEntityInfo> processEntityInfos(List<StructureEntityInfo> original,
														 ServerLevelAccessor level, BlockPos pos, Mirror mirror, Rotation rotation,
														 BlockPos pivot, @Nullable BoundingBox bounds, boolean finalizeMobs) {
		StructurePlaceSettings settings = currentSettings.get();

		if (PortingLib.DEBUG)
			Objects.requireNonNull(settings);

		if (settings == null)
			return original;

		return StructureTemplateUtils.processEntityInfos(
				(StructureTemplate) (Object) this, level, pos, settings, original
		);
	}

	@ModifyExpressionValue(
			method = "placeEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/core/BlockPos;offset(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"
			)
	)
	private BlockPos dontProcessBlockPosTwice(BlockPos original, @Local StructureEntityInfo info) {
		StructurePlaceSettings settings = currentSettings.get();
		if (settings != null) { // pos was already processed.
			return info.blockPos;
		}
		return original;
	}

	@ModifyExpressionValue(
			method = "placeEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"
			)
	)
	private Vec3 dontProcessVecTwice(Vec3 original, @Local StructureEntityInfo info) {
		StructurePlaceSettings settings = currentSettings.get();
		if (settings != null) { // pos was already processed.
			return info.pos;
		}
		return original;
	}

	// --- deprecated API ---

	@Override
	public List<StructureEntityInfo> getEntities() {
		return entityInfoList;
	}

	@Override
	public Vec3 transformedVec3d(StructurePlaceSettings settings, Vec3 pos) {
		return StructureTemplateUtils.transformedVec3d(settings, pos);
	}

	@Override
	public List<StructureEntityInfo> processEntityInfos(@Nullable StructureTemplate template, LevelAccessor world, BlockPos blockPos, StructurePlaceSettings settings, List<StructureEntityInfo> infos) {
		return StructureTemplateUtils.processEntityInfos(template, world, blockPos, settings, infos);
	}

	@Override
	public void addEntitiesToWorld(ServerLevelAccessor level, BlockPos pos, StructurePlaceSettings settings) {
		this.placeEntities(
				level, pos, settings.getMirror(), settings.getRotation(),
				settings.getRotationPivot(), settings.getBoundingBox(),
				settings.shouldFinalizeEntities()
		);
	}
}
