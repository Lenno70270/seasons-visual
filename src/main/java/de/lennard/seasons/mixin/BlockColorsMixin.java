package de.lennard.seasons.mixin;

import java.util.List;
import java.util.function.IntUnaryOperator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import de.lennard.seasons.LeafTint;
import de.lennard.seasons.SeasonColors;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@Mixin(BlockColors.class)
abstract class BlockColorsMixin {
	@ModifyReturnValue(method = "createDefault", at = @At("RETURN"))
	private static BlockColors seasons$wrapFixedLeaves(BlockColors colors) {
		seasons$wrap(colors, Blocks.BIRCH_LEAVES, SeasonColors::birch);
		seasons$wrap(colors, Blocks.SPRUCE_LEAVES, SeasonColors::evergreen);
		return colors;
	}

	@Unique
	private static void seasons$wrap(BlockColors colors, Block block, IntUnaryOperator seasonal) {
		colors.register(List.of(new LeafTint(colors.getTintSource(block.defaultBlockState(), 0), seasonal)), block);
	}
}
