package de.lennard.seasons;

import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import de.lennard.seasons.mixin.SpriteContentsAccessor;
import de.lennard.seasons.mixin.TextureAtlasAccessor;
import de.lennard.seasons.mixin.TextureAtlasSpriteAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Eine Tönung kann Texturen nur abdunkeln: Weiß ergäbe graues Laub, Orange ein trübes Braun. Deshalb werden die
 * Laub- und Grasbilder passend zur Jahreszeit direkt im Block-Atlas aufgehellt. Die Originale im RAM bleiben unberührt
 * und werden zum Zurückstellen neu hochgeladen. Das dauert Millisekunden, ein Ressourcen-Reload bräuchte Sekunden.
 */
public final class SeasonSprites {
	private static final Set<String> PLANTS = Set.of(
			"block/grass_block_top", "block/grass_block_side_overlay", "block/short_grass", "block/tall_grass_top",
			"block/tall_grass_bottom", "block/fern", "block/large_fern_top", "block/large_fern_bottom", "block/vine", "block/bush");
	private static final String EVERGREEN = "block/spruce_leaves";

	private static final float SPRING_GAIN = 1.2F;
	private static final float AUTUMN_GAIN = 1.5F;
	// Bis zu diesem Unterschied der Farbkanäle gilt ein Pixel als grau, also als von der Tönung eingefärbt.
	private static final int GREY_TOLERANCE = 12;
	// Helligkeit im Winter: dunkelste Stelle der Textur bis reines Weiß, damit die Struktur sichtbar bleibt.
	private static final float WINTER_DARKEST = 0.74F;

	private static Season applied = Season.SUMMER;

	private SeasonSprites() {
	}

	/** Gleicht den Atlas mit der Jahreszeit ab. Nur auf dem Render-Thread aufrufen. */
	static void refresh() {
		if (SeasonsVisual.season() != applied) {
			upload(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS), SeasonsVisual.season());
		}
	}

	/** Ein Ressourcen-Reload baut den Atlas neu, dann ist er wieder im Originalzustand. */
	public static void onAtlasRebuilt(TextureAtlas atlas) {
		if (atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) {
			applied = Season.SUMMER;
			if (SeasonsVisual.season() != Season.SUMMER) {
				upload(atlas, SeasonsVisual.season());
			}
		}
	}

	private static void upload(TextureAtlas atlas, Season season) {
		GpuTexture texture = atlas.getTexture();
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		for (TextureAtlasSprite sprite : ((TextureAtlasAccessor) atlas).seasons$sprites()) {
			// Auch Bilder, die nur die vorige Jahreszeit verändert hat, müssen zurück. Deshalb entscheidet hier der
			// größte Kreis, und Bilder ohne Änderung gehen im Original hoch.
			if (!sprite.isAnimated() && affected(sprite.contents(), Season.WINTER)) {
				uploadSprite(encoder, texture, sprite, affected(sprite.contents(), season) ? season : Season.SUMMER);
			}
		}
		applied = season;
	}

	private static boolean affected(SpriteContents contents, Season season) {
		String path = contents.name().getPath();
		if (!path.startsWith("block/")) {
			return false;
		}
		return switch (season) {
			case WINTER -> path.endsWith("_leaves") || PLANTS.contains(path);
			case SPRING, AUTUMN -> path.endsWith("_leaves") && !path.equals(EVERGREEN);
			case SUMMER -> false;
		};
	}

	private static void uploadSprite(CommandEncoder encoder, GpuTexture texture, TextureAtlasSprite sprite, Season season) {
		int padding = ((TextureAtlasSpriteAccessor) sprite).seasons$padding();
		NativeImage[] mips = ((SpriteContentsAccessor) sprite.contents()).seasons$byMipLevel();
		for (int level = 0; level < Math.min(mips.length, texture.getMipLevels()); level++) {
			try (NativeImage image = padded(mips[level], padding >> level, season)) {
				encoder.writeToTexture(texture, image, level, 0, sprite.getX() >> level, sprite.getY() >> level);
			}
		}
	}

	/** Der Rand um jedes Bild wiederholt dessen Kante. Bleibt er alt, sieht man ihn aus der Ferne als Linien zwischen Blöcken. */
	private static NativeImage padded(NativeImage source, int padding, Season season) {
		int width = source.getWidth();
		int height = source.getHeight();
		NativeImage result = new NativeImage(width + padding * 2, height + padding * 2, false);
		for (int y = 0; y < result.getHeight(); y++) {
			int sourceY = Mth.clamp(y - padding, 0, height - 1);
			for (int x = 0; x < result.getWidth(); x++) {
				result.setPixel(x, y, transformed(source.getPixel(Mth.clamp(x - padding, 0, width - 1), sourceY), season));
			}
		}
		return result;
	}

	private static int transformed(int color, Season season) {
		return switch (season) {
			case WINTER -> whitened(color);
			case AUTUMN -> brightened(color, AUTUMN_GAIN);
			case SPRING -> brightened(color, SPRING_GAIN);
			case SUMMER -> color;
		};
	}

	private static int whitened(int color) {
		float brightness = (ARGB.red(color) * 0.3F + ARGB.green(color) * 0.59F + ARGB.blue(color) * 0.11F) / 255.0F;
		int value = (int) ((WINTER_DARKEST + (1.0F - WINTER_DARKEST) * brightness) * 255.0F);
		return ARGB.color(ARGB.alpha(color), value, value, value);
	}

	/** Farbige Pixel gehören zu Blättern ohne Tönung, etwa Kirsche oder Azalee, und bleiben, wie sie sind. */
	private static int brightened(int color, float gain) {
		int red = ARGB.red(color);
		int green = ARGB.green(color);
		int blue = ARGB.blue(color);
		if (Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) > GREY_TOLERANCE) {
			return color;
		}
		return ARGB.color(ARGB.alpha(color), Math.min(255, (int) (red * gain)), Math.min(255, (int) (green * gain)), Math.min(255, (int) (blue * gain)));
	}
}
