package de.lennard.seasons;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class SeasonScreen extends Screen {
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;

	SeasonScreen() {
		super(Component.literal("Jahreszeit"));
	}

	@Override
	protected void init() {
		int left = (this.width - BUTTON_WIDTH * 2 - GAP) / 2;
		int top = this.height / 2 - 40;
		Season[] seasons = Season.values();
		for (int i = 0; i < seasons.length; i++) {
			Season season = seasons[i];
			int x = left + (i % 2) * (BUTTON_WIDTH + GAP);
			int y = top + (i / 2) * (BUTTON_HEIGHT + GAP);
			String label = season == SeasonsVisual.season() ? "» " + season.label + " «" : season.label;
			addRenderableWidget(Button.builder(Component.literal(label), button -> {
				SeasonsVisual.setSeason(season);
				onClose();
			}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		}

		int optionsTop = top + 2 * (BUTTON_HEIGHT + GAP) + GAP * 2;
		addRenderableWidget(Button.builder(waterLabel(), button -> {
			SeasonsVisual.setIcyWater(!SeasonsVisual.icyWater());
			button.setMessage(waterLabel());
		}).bounds(left, optionsTop, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(particleLabel(), button -> {
			SeasonsVisual.setParticles(SeasonsVisual.particles().next());
			button.setMessage(particleLabel());
		}).bounds(left + BUTTON_WIDTH + GAP, optionsTop, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	private static Component waterLabel() {
		return Component.literal("Eisiges Wasser: " + (SeasonsVisual.icyWater() ? "An" : "Aus"));
	}

	private static Component particleLabel() {
		return Component.literal("Partikel: " + SeasonsVisual.particles().label);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
