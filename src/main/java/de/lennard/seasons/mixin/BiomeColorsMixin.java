package de.lennard.seasons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import de.lennard.seasons.SeasonColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;

/** Vanilla und Sodium holen ihre Farben beide hier. Hinter dem Biom-Blending, die Herbstflecken bleiben also scharf. */
@Mixin(BiomeColors.class)
abstract class BiomeColorsMixin {
	@ModifyReturnValue(method = "getAverageGrassColor", at = @At("RETURN"))
	private static int seasons$grass(int color) {
		return SeasonColors.grass(color);
	}

	@ModifyReturnValue(method = "getAverageFoliageColor", at = @At("RETURN"))
	private static int seasons$foliage(int color, BlockAndTintGetter level, BlockPos pos) {
		return SeasonColors.foliage(color, pos);
	}

	@ModifyReturnValue(method = "getAverageWaterColor", at = @At("RETURN"))
	private static int seasons$water(int color) {
		return SeasonColors.water(color);
	}
}
