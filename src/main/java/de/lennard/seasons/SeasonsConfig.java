package de.lennard.seasons;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;

/** Liegt als config/seasons_visual.json neben den anderen Mod-Einstellungen. */
final class SeasonsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	Season season = Season.SUMMER;
	boolean icyWater = true;
	ParticleAmount particles = ParticleAmount.NORMAL;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("seasons_visual.json");
	}

	static SeasonsConfig load() {
		Path file = file();
		if (Files.isRegularFile(file)) {
			try {
				SeasonsConfig config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), SeasonsConfig.class);
				if (config != null) {
					// Unbekannte Werte liest Gson als null. Die Mod soll dann trotzdem starten.
					config.season = config.season == null ? Season.SUMMER : config.season;
					config.particles = config.particles == null ? ParticleAmount.NORMAL : config.particles;
					return config;
				}
			} catch (IOException | JsonParseException e) {
				SeasonsVisual.LOGGER.warn("Konfiguration nicht lesbar, nutze Standardwerte", e);
			}
		}
		return new SeasonsConfig();
	}

	void save() {
		try {
			Files.writeString(file(), GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			SeasonsVisual.LOGGER.warn("Konfiguration nicht speicherbar", e);
		}
	}
}
