package de.lennard.seasons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Die Schneeschicht entsteht beim Bauen des Chunk-Meshes: Unter freiem Himmel wird über einem Block zusätzlich das
 * Modell einer Schneelage ausgegeben. In der Welt ändert sich nichts, und pro Frame kostet das fast nichts.
 */
public final class SnowCover {
	public static final BlockState SNOW = Blocks.SNOW.defaultBlockState();

	private static final int FULL_SKY_LIGHT = 15;
	private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	private static final int SIDE_VARIANTS = 1 << SIDES.length;

	private static final Map<BlockState, Boolean> HOLDS_SNOW = new ConcurrentHashMap<>();

	private static volatile @Nullable BlockStateModel variantsBase;
	private static volatile BlockStateModel[] variants = new BlockStateModel[SIDE_VARIANTS];

	private SnowCover() {
	}

	public static boolean active() {
		return SeasonsVisual.season() == Season.WINTER;
	}

	/** Prüft, ob über dem Block an {@code pos} Schnee liegen soll. */
	public static boolean covers(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return holdsSnow(state) && openToSky(level, pos.above());
	}

	/** Das Modell ohne die Seiten, an denen direkt die nächste Schneelage anschließt. */
	public static BlockStateModel model(BlockAndTintGetter level, BlockPos pos) {
		int visibleSides = 0;
		BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
		for (int i = 0; i < SIDES.length; i++) {
			neighbour.setWithOffset(pos, SIDES[i]);
			if (!covers(level, neighbour, level.getBlockState(neighbour))) {
				visibleSides |= 1 << i;
			}
		}
		return variant(visibleSides);
	}

	private static boolean openToSky(BlockAndTintGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		boolean free = state.isAir() || state.getBlock() instanceof VegetationBlock;
		return free && state.getFluidState().isEmpty() && level.getBrightness(LightLayer.SKY, pos) >= FULL_SKY_LIGHT;
	}

	private static boolean holdsSnow(BlockState state) {
		Boolean known = HOLDS_SNOW.get(state);
		if (known != null) {
			return known;
		}
		boolean result = computeHoldsSnow(state);
		HOLDS_SNOW.put(state, result);
		return result;
	}

	/** Dieselben Regeln wie bei echtem Schnee, nur ohne Zugriff auf die Welt. */
	private static boolean computeHoldsSnow(BlockState state) {
		if (state.isAir() || state.is(BlockTags.CANNOT_SUPPORT_SNOW_LAYER) || !state.getFluidState().isEmpty()) {
			return false;
		}
		return state.is(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER)
				|| Block.isFaceFull(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), Direction.UP);
	}

	private static BlockStateModel variant(int visibleSides) {
		BlockStateModel base = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(SNOW);
		BlockStateModel[] current = variants;
		// Nach einem Ressourcen-Reload ist das Modell ein neues Objekt.
		if (base != variantsBase) {
			current = new BlockStateModel[SIDE_VARIANTS];
			List<BlockStateModelPart> parts = new ArrayList<>();
			base.collectParts(RandomSource.create(0L), parts);
			for (int sides = 0; sides < SIDE_VARIANTS; sides++) {
				current[sides] = new TrimmedModel(base, trim(parts, sides));
			}
			variants = current;
			variantsBase = base;
		}
		return current[visibleSides];
	}

	private static List<BlockStateModelPart> trim(List<BlockStateModelPart> parts, int visibleSides) {
		List<BlockStateModelPart> trimmed = new ArrayList<>(parts.size());
		for (BlockStateModelPart part : parts) {
			trimmed.add(new TrimmedPart(part, visibleSides));
		}
		return List.copyOf(trimmed);
	}

	private record TrimmedModel(BlockStateModel base, List<BlockStateModelPart> parts) implements BlockStateModel {
		@Override
		public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
			output.addAll(parts);
		}

		@Override
		public Material.Baked particleMaterial() {
			return base.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return base.materialFlags();
		}
	}

	private record TrimmedPart(BlockStateModelPart base, int visibleSides) implements BlockStateModelPart {
		@Override
		public List<BakedQuad> getQuads(@Nullable Direction direction) {
			if (direction != null && direction.getAxis().isHorizontal() && !visible(direction)) {
				return List.of();
			}
			return base.getQuads(direction);
		}

		private boolean visible(Direction direction) {
			for (int i = 0; i < SIDES.length; i++) {
				if (SIDES[i] == direction) {
					return (visibleSides & 1 << i) != 0;
				}
			}
			return true;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return base.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return base.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return base.materialFlags();
		}
	}
}
