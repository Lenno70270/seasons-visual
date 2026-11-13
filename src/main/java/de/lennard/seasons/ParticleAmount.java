package de.lennard.seasons;

/** Eigene Stufe, damit man Partikel für PvP reduzieren kann, ohne die Minecraft-Einstellung anzufassen. */
public enum ParticleAmount {
	NORMAL("normal", 1.0F),
	LOW("low", 0.35F),
	OFF("off", 0.0F);

	/** Übersetzungsschlüssel, die Texte stehen in assets/seasons_visual/lang. */
	public final String key;
	public final float factor;

	ParticleAmount(String id, float factor) {
		this.key = "seasons_visual.particles." + id;
		this.factor = factor;
	}

	public ParticleAmount next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
