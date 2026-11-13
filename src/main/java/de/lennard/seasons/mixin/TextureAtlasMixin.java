package de.lennard.seasons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.lennard.seasons.SeasonSprites;
import net.minecraft.client.renderer.texture.TextureAtlas;

@Mixin(TextureAtlas.class)
abstract class TextureAtlasMixin {
	@Inject(method = "uploadInitialContents", at = @At("TAIL"))
	private void seasons$reapplySeason(CallbackInfo ci) {
		SeasonSprites.onAtlasRebuilt((TextureAtlas) (Object) this);
	}
}
