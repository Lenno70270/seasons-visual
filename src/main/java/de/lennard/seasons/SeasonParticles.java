package de.lennard.seasons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Erzeugt pro Tick nur eine feste Handvoll Partikel rund um die Kamera, die Kosten hängen also nicht an der Welt. */
final class SeasonParticles {
	private static final int LEAF_RADIUS = 14;
	private static final int SNOW_RADIUS = 20;
	private static final int MAX_LEAF_DISTANCE_Y = 20;
	private static final int CANOPY_SEARCH_DEPTH = 8;

	// Stichproben je Tick. Nur Treffer unter Bäumen werden zu Partikeln, die echte Rate liegt also deutlich darunter.
	private static final float SPRING_SAMPLES = 1.5F;
	private static final float AUTUMN_SAMPLES = 4.0F;
	private static final float SNOW_PER_TICK = 12.0F;

	private static final int[] BLOSSOMS = {0xF6B3D0, 0xFBD3E4, 0xFFF3F7};
	private static final Identifier SNOW_SPRITE = Identifier.withDefaultNamespace("generic_1");

	private SeasonParticles() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		Season season = SeasonsVisual.season();
		if (level == null || season == Season.SUMMER || minecraft.isPaused()) {
			return;
		}
		if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
			return;
		}
		float amount = SeasonsVisual.particles().factor * gameSetting(minecraft);
		if (amount <= 0.0F) {
			return;
		}

		Vec3 camera = minecraft.gameRenderer.mainCamera().position();
		RandomSource random = level.getRandom();
		switch (season) {
			case SPRING -> spawnLeaves(level, camera, random, roll(SPRING_SAMPLES * amount, random), false);
			case AUTUMN -> spawnLeaves(level, camera, random, roll(AUTUMN_SAMPLES * amount, random), true);
			case WINTER -> spawnSnow(minecraft, level, camera, random, roll(SNOW_PER_TICK * amount, random));
			default -> {
			}
		}
	}

	private static float gameSetting(Minecraft minecraft) {
		ParticleStatus status = minecraft.options.particles().get();
		return switch (status) {
			case ALL -> 1.0F;
			case DECREASED -> 0.5F;
			case MINIMAL -> 0.0F;
		};
	}

	/** Macht aus 1,5 je nach Zufall mal 1 und mal 2, damit auch kleine Raten stimmen. */
	private static int roll(float rate, RandomSource random) {
		int whole = (int) rate;
		return random.nextFloat() < rate - whole ? whole + 1 : whole;
	}

	private static void spawnLeaves(ClientLevel level, Vec3 camera, RandomSource random, int samples, boolean autumn) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < samples; i++) {
			int x = (int) Math.floor(camera.x) + random.nextInt(LEAF_RADIUS * 2 + 1) - LEAF_RADIUS;
			int z = (int) Math.floor(camera.z) + random.nextInt(LEAF_RADIUS * 2 + 1) - LEAF_RADIUS;
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
			if (Math.abs(top - camera.y) > MAX_LEAF_DISTANCE_Y || !level.getBlockState(pos.set(x, top, z)).is(BlockTags.LEAVES)) {
				continue;
			}
			int y = top;
			int lowest = top - CANOPY_SEARCH_DEPTH;
			while (y > lowest && level.getBlockState(pos.set(x, y, z)).is(BlockTags.LEAVES)) {
				y--;
			}
			if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
				continue;
			}
			int color = autumn ? SeasonColors.autumnLeaf(x, y, z) : BLOSSOMS[random.nextInt(BLOSSOMS.length)];
			level.addParticle(ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF000000 | color),
					x + random.nextDouble(), y + 0.9, z + random.nextDouble(), 0.0, 0.0, 0.0);
		}
	}

	private static void spawnSnow(Minecraft minecraft, ClientLevel level, Vec3 camera, RandomSource random, int count) {
		TextureAtlasSprite sprite = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(SNOW_SPRITE);
		for (int i = 0; i < count; i++) {
			double x = camera.x + (random.nextDouble() * 2.0 - 1.0) * SNOW_RADIUS;
			double z = camera.z + (random.nextDouble() * 2.0 - 1.0) * SNOW_RADIUS;
			int blockX = (int) Math.floor(x);
			int blockZ = (int) Math.floor(z);
			if (!level.hasChunk(blockX >> 4, blockZ >> 4)) {
				continue;
			}
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
			// Über dem Boden, nie darunter: In Höhlen und Häusern fällt so kein Schnee.
			double y = Math.max(camera.y - 4.0, ground) + random.nextDouble() * 16.0;
			minecraft.particleEngine.add(new SnowfallParticle(level, x, y, z, ground, sprite));
		}
	}
}
