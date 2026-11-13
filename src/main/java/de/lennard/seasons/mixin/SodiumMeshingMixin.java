package de.lennard.seasons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import de.lennard.seasons.SnowCover;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Der Schnee landet im Mesh des Blocks darunter. So fehlt er nicht, wenn der Abschnitt darüber komplett leer ist. */
@Mixin(ChunkBuilderMeshingTask.class)
abstract class SodiumMeshingMixin {
	@WrapOperation(
			method = "execute",
			require = 0,
			at = @At(
					value = "INVOKE",
					target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"))
	private void seasons$addSnow(
			BlockRenderer renderer, BlockStateModel model, BlockState state, BlockPos pos, BlockPos offset, Operation<Void> original,
			@Local LevelSlice slice) {
		original.call(renderer, model, state, pos, offset);
		if (SnowCover.active() && SnowCover.covers(slice, pos, state)) {
			original.call(renderer, SnowCover.model(slice, pos), SnowCover.SNOW, pos.above(), offset.above());
		}
	}
}
