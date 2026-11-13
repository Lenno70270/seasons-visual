package de.lennard.seasons;

import java.util.Set;
import java.util.function.IntUnaryOperator;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Fichte und Birke haben feste Farben und laufen deshalb nicht über BiomeColors. Items bleiben unverändert. */
public record LeafTint(BlockTintSource base, IntUnaryOperator seasonal) implements BlockTintSource {
	@Override
	public int color(BlockState state) {
		return base.color(state);
	}

	@Override
	public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
		return seasonal.applyAsInt(base.colorInWorld(state, level, pos));
	}

	@Override
	public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
		return seasonal.applyAsInt(base.colorAsTerrainParticle(state, level, pos));
	}

	@Override
	public Set<Property<?>> relevantProperties() {
		return base.relevantProperties();
	}
}
