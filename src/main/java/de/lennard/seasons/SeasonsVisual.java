package de.lennard.seasons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class SeasonsVisual implements ClientModInitializer {
	static final Logger LOGGER = LoggerFactory.getLogger("seasons_visual");

	private static SeasonsConfig config = new SeasonsConfig();
	// Die Chunk-Threads lesen beides ständig mit, deshalb volatile statt über die Config.
	private static volatile Season season = Season.SUMMER;
	private static volatile boolean icyWater = true;

	@Override
	public void onInitializeClient() {
		config = SeasonsConfig.load();
		season = config.season;
		icyWater = config.icyWater;

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("seasons_visual", "main"));
		KeyMapping open = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.seasons_visual.open", InputConstants.Type.KEYSYM, InputConstants.KEY_J, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (open.consumeClick()) {
				client.gui.setScreen(new SeasonScreen());
			}
			SeasonParticles.tick(client);
		});
	}

	public static Season season() {
		return season;
	}

	public static boolean icyWater() {
		return icyWater;
	}

	public static ParticleAmount particles() {
		return config.particles;
	}

	public static void setSeason(Season next) {
		if (next == season) {
			return;
		}
		season = next;
		config.season = next;
		config.save();
		SeasonSprites.refresh();
		rebuildChunks();
	}

	static void setIcyWater(boolean value) {
		icyWater = value;
		config.icyWater = value;
		config.save();
		if (season == Season.WINTER) {
			rebuildChunks();
		}
	}

	static void setParticles(ParticleAmount value) {
		config.particles = value;
		config.save();
	}

	/** Derselbe Weg wie F3+A. Sodium und Iris hängen sich hier selbst ein. */
	private static void rebuildChunks() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) {
			minecraft.levelExtractor.allChanged();
		}
	}
}
