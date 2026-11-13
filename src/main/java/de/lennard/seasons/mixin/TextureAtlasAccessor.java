package de.lennard.seasons.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(TextureAtlas.class)
public interface TextureAtlasAccessor {
	@Accessor("sprites")
	List<TextureAtlasSprite> seasons$sprites();
}
