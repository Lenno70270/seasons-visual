package de.lennard.seasons;

import net.minecraft.network.chat.Component;

public enum Season {
	SPRING("spring"),
	SUMMER("summer"),
	AUTUMN("autumn"),
	WINTER("winter");

	/** Übersetzungsschlüssel, die Texte stehen in assets/seasons_visual/lang. */
	public final String key;

	Season(String id) {
		this.key = "seasons_visual.season." + id;
	}

	public Component label() {
		return Component.translatable(key);
	}
}
