package de.lennard.seasons;

/** Eigene Stufe, damit man Partikel für PvP reduzieren kann, ohne die Minecraft-Einstellung anzufassen. */
public enum ParticleAmount {
	NORMAL("Normal", 1.0F),
	LOW("Wenig", 0.35F),
	OFF("Aus", 0.0F);

	public final String label;
	public final float factor;

	ParticleAmount(String label, float factor) {
		this.label = label;
		this.factor = factor;
	}

	public ParticleAmount next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
