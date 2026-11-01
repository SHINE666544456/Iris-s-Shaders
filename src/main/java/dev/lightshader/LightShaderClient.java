package dev.lightshader;

import dev.lightshader.screen.LightShaderScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightShaderClient implements ClientModInitializer {
    public static final String MOD_ID = "lightshader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding cycleKey;
    private static KeyBinding toggleKey;
    private static KeyBinding screenKey;
    private static KeyBinding reloadKey;

    @Override
    public void onInitializeClient() {
        LightShaderConfig.load();

        // 1.21.9+ requires a KeyBinding.Category object instead of a plain string.
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        cycleKey = reg("key.lightshader.cycle", GLFW.GLFW_KEY_K, category);
        toggleKey = reg("key.lightshader.toggle", GLFW.GLFW_KEY_J, category);
        screenKey = reg("key.lightshader.screen", GLFW.GLFW_KEY_L, category);
        reloadKey = reg("key.lightshader.reload", GLFW.GLFW_KEY_SEMICOLON, category);

        ClientTickEvents.END_CLIENT_TICK.register(LightShaderClient::onTick);
    }

    private static KeyBinding reg(String id, int key, KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, InputUtil.Type.KEYSYM, key, category));
    }

    private static void onTick(MinecraftClient mc) {
        while (cycleKey.wasPressed()) {
            ShaderController.cyclePreset(mc);
        }
        while (toggleKey.wasPressed()) {
            ShaderController.toggle(mc);
        }
        while (screenKey.wasPressed()) {
            mc.setScreen(new LightShaderScreen(mc.currentScreen));
        }
        while (reloadKey.wasPressed()) {
            mc.reloadResources();
            ShaderController.markDirty();
        }
        ShaderController.tick(mc);
    }
}
