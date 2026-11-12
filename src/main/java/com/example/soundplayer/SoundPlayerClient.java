package com.example.soundplayer;

import com.example.soundplayer.audio.PlaylistManager;
import com.example.soundplayer.audio.SoundPlaybackController;
import com.example.soundplayer.gui.MusicPlayerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SoundPlayerClient implements ClientModInitializer {
    public static final String MOD_ID = "soundplayer";

    private static KeyBinding openPlayerKey;
    private static KeyBinding playPauseKey;
    private static KeyBinding nextTrackKey;
    private static KeyBinding prevTrackKey;

    @Override
    public void onInitializeClient() {
        PlaylistManager.getInstance().load();

        openPlayerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soundplayer.open_player",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.soundplayer.keys"
        ));

        playPauseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soundplayer.play_pause",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.soundplayer.keys"
        ));

        nextTrackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soundplayer.next_track",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.soundplayer.keys"
        ));

        prevTrackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soundplayer.prev_track",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.soundplayer.keys"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SoundPlaybackController.getInstance().tick(client);

            while (openPlayerKey.wasPressed()) {
                client.setScreen(new MusicPlayerScreen());
            }
            while (playPauseKey.wasPressed()) {
                SoundPlaybackController.getInstance().togglePlayPause();
            }
            while (nextTrackKey.wasPressed()) {
                SoundPlaybackController.getInstance().nextTrack();
            }
            while (prevTrackKey.wasPressed()) {
                SoundPlaybackController.getInstance().previousTrack();
            }
        });
    }
}
