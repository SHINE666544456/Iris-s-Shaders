package dev.lightshader.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the vanilla post-processor slot (the one used for spectating creepers etc).
 * Names are the Yarn 1.21.11 names: setPostProcessor(Identifier), postProcessorId, postProcessorEnabled.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("setPostProcessor")
    void lightshader$setPostProcessor(Identifier id);

    @Accessor("postProcessorId")
    Identifier lightshader$getPostProcessorId();

    @Accessor("postProcessorId")
    void lightshader$setPostProcessorId(Identifier id);

    @Accessor("postProcessorEnabled")
    void lightshader$setPostProcessorEnabled(boolean enabled);
}
