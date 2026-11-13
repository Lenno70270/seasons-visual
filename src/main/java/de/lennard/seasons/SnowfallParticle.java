package de.lennard.seasons;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Vanillas Schneeflocke lebt nur rund eine Sekunde. Diese fällt langsam und spart sich die Kollisionsprüfung. */
final class SnowfallParticle extends SingleQuadParticle {
	private static final int MAX_AGE = 240;
	private static final double FALL_SPEED = 0.07;

	private final double groundY;
	private final double swayPhase;
	private final double swaySpeed;

	SnowfallParticle(ClientLevel level, double x, double y, double z, double groundY, TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
		this.groundY = groundY;
		this.lifetime = MAX_AGE;
		this.quadSize = 0.07F + this.random.nextFloat() * 0.06F;
		this.swayPhase = this.random.nextDouble() * Math.PI * 2.0;
		this.swaySpeed = 0.04 + this.random.nextDouble() * 0.05;
		this.yd = -FALL_SPEED * (0.8 + this.random.nextDouble() * 0.5);
		setColor(0.95F, 0.97F, 1.0F);
	}

	@Override
	protected Layer getLayer() {
		return Layer.OPAQUE;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime || this.y <= this.groundY || SeasonsVisual.season() != Season.WINTER) {
			remove();
			return;
		}
		double sway = Math.sin(this.swayPhase + this.age * this.swaySpeed) * 0.015;
		setPos(this.x + sway, this.y + this.yd, this.z + sway * 0.6);
	}
}
