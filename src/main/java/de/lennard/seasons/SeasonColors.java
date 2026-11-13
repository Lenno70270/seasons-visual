package de.lennard.seasons;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Rechnet Biom-Farben um. Läuft auf den Chunk-Threads, darf also nur rechnen und nichts anfassen. */
public final class SeasonColors {
	private static final int WHITE = 0xFFFFFF;
	private static final int FRESH_GREEN = 0x6FDC4B;
	private static final int DRY_GRASS = 0xA98A3F;
	private static final int ICE_BLUE = 0xA6D9F2;
	private static final int[] AUTUMN_LEAVES = {0xE8801C, 0xCF3A1E, 0xEDBE28};
	private static final int BIRCH_GOLD = 0xE9C22E;

	// Größe der gleichfarbigen Flecken in Blöcken, grob eine Baumkrone.
	private static final int PATCH_WIDTH = 6;
	private static final int PATCH_HEIGHT = 7;

	private SeasonColors() {
	}

	public static int grass(int base) {
		return switch (SeasonsVisual.season()) {
			case SPRING -> mix(base, FRESH_GREEN, 0.4F);
			case AUTUMN -> mix(base, DRY_GRASS, 0.4F);
			case WINTER -> opaque(base, WHITE);
			case SUMMER -> base;
		};
	}

	public static int foliage(int base, BlockPos pos) {
		return switch (SeasonsVisual.season()) {
			case SPRING -> mix(base, FRESH_GREEN, 0.4F);
			case AUTUMN -> opaque(base, autumnLeaf(pos.getX(), pos.getY(), pos.getZ()));
			case WINTER -> opaque(base, WHITE);
			case SUMMER -> base;
		};
	}

	public static int birch(int base) {
		return switch (SeasonsVisual.season()) {
			case SPRING -> mix(base, FRESH_GREEN, 0.3F);
			case AUTUMN -> opaque(base, BIRCH_GOLD);
			case WINTER -> opaque(base, WHITE);
			case SUMMER -> base;
		};
	}

	/** Nadelbäume bleiben im Herbst grün und bekommen erst im Winter Schnee. */
	public static int evergreen(int base) {
		return SeasonsVisual.season() == Season.WINTER ? opaque(base, WHITE) : base;
	}

	public static int water(int base) {
		if (SeasonsVisual.season() == Season.WINTER && SeasonsVisual.icyWater()) {
			return mix(base, ICE_BLUE, 0.55F);
		}
		return base;
	}

	static int autumnLeaf(int x, int y, int z) {
		// Die Verschiebung bricht die Rasterkanten auf, sonst sähe man würfelförmige Farbblöcke.
		int warpedX = x + (hash(z >> 2, 11, 0) & 3);
		int warpedZ = z + (hash(x >> 2, 23, 0) & 3);
		int patch = hash(Math.floorDiv(warpedX, PATCH_WIDTH), Math.floorDiv(y, PATCH_HEIGHT), Math.floorDiv(warpedZ, PATCH_WIDTH));
		int color = AUTUMN_LEAVES[(patch >>> 8) % AUTUMN_LEAVES.length];
		// Leichtes Rauschen je Block, damit eine Krone nicht einfarbig wirkt.
		float shade = 0.9F + (hash(x, y, z) & 15) / 15.0F * 0.1F;
		return scale(color, shade);
	}

	private static int hash(int x, int y, int z) {
		int h = x * 0x27D4EB2D ^ y * 0x165667B1 ^ z * 0x9E3779B1;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		return h ^ h >>> 13;
	}

	/** Übernimmt den Alpha-Wert der Originalfarbe. */
	private static int opaque(int base, int color) {
		return base & 0xFF000000 | color & 0xFFFFFF;
	}

	private static int mix(int from, int to, float amount) {
		int red = (int) Mth.lerp(amount, from >> 16 & 255, to >> 16 & 255);
		int green = (int) Mth.lerp(amount, from >> 8 & 255, to >> 8 & 255);
		int blue = (int) Mth.lerp(amount, from & 255, to & 255);
		return from & 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private static int scale(int color, float factor) {
		int red = (int) ((color >> 16 & 255) * factor);
		int green = (int) ((color >> 8 & 255) * factor);
		int blue = (int) ((color & 255) * factor);
		return red << 16 | green << 8 | blue;
	}
}
