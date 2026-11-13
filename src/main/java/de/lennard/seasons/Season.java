package de.lennard.seasons;

public enum Season {
	SPRING("Frühling"),
	SUMMER("Sommer"),
	AUTUMN("Herbst"),
	WINTER("Winter");

	public final String label;

	Season(String label) {
		this.label = label;
	}
}
