package com.example.soundplayer.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class SoundPlaybackController {
    public enum RepeatMode {
        OFF("Repeat: Off"),
        ALL("Repeat: All"),
        ONE("Repeat: One");

        public final String label;
        RepeatMode(String label) { this.label = label; }
    }

    private static final SoundPlaybackController INSTANCE = new SoundPlaybackController();

    private SoundInstance currentSoundInstance;
    private int currentTrackIndex = -1;
    private boolean isPlaying = false;
    private boolean shuffle = false;
    private RepeatMode repeatMode = RepeatMode.ALL;
    private float masterVolumeMultiplier = 1.0f;
    private int elapsedTicks = 0;

    public static SoundPlaybackController getInstance() {
        return INSTANCE;
    }

    public void playTrack(int index) {
        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        if (index < 0 || index >= tracks.size()) return;

        stopCurrentSound();

        PlaylistTrack track = tracks.get(index);
        Identifier id = Identifier.tryParse(track.soundId);
        if (id == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().stopSounds(null, SoundCategory.MUSIC);

        SoundEvent event = SoundEvent.of(id);
        float finalVolume = Math.max(0.01f, track.volume * masterVolumeMultiplier);

        currentSoundInstance = new PositionedSoundInstance(
                event.id(),
                SoundCategory.RECORDS,
                finalVolume,
                track.pitch,
                Random.create(),
                false,
                0,
                SoundInstance.AttenuationType.NONE,
                0.0, 0.0, 0.0,
                true
        );

        client.getSoundManager().play(currentSoundInstance);
        currentTrackIndex = index;
        elapsedTicks = 0;
        isPlaying = true;
    }

    public void togglePlayPause() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (isPlaying) {
            client.getSoundManager().pauseAll();
            isPlaying = false;
        } else {
            if (currentSoundInstance != null && client.getSoundManager().isPlaying(currentSoundInstance)) {
                client.getSoundManager().resumeAll();
                isPlaying = true;
            } else if (!PlaylistManager.getInstance().getActiveTracks().isEmpty()) {
                playTrack(currentTrackIndex >= 0 ? currentTrackIndex : 0);
            }
        }
    }

    public void stop() {
        stopCurrentSound();
        isPlaying = false;
        elapsedTicks = 0;
    }

    private void stopCurrentSound() {
        if (currentSoundInstance != null) {
            MinecraftClient.getInstance().getSoundManager().stop(currentSoundInstance);
            currentSoundInstance = null;
        }
    }

    public void nextTrack() {
        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        if (tracks.isEmpty()) return;

        if (repeatMode == RepeatMode.ONE && currentTrackIndex >= 0) {
            playTrack(currentTrackIndex);
            return;
        }

        if (shuffle && tracks.size() > 1) {
            int next;
            do {
                next = (int) (Math.random() * tracks.size());
            } while (next == currentTrackIndex);
            playTrack(next);
            return;
        }

        int nextIndex = currentTrackIndex + 1;
        if (nextIndex >= tracks.size()) {
            if (repeatMode == RepeatMode.ALL) {
                playTrack(0);
            } else {
                stop();
            }
        } else {
            playTrack(nextIndex);
        }
    }

    public void previousTrack() {
        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        if (tracks.isEmpty()) return;

        if (elapsedTicks > 60 && currentTrackIndex >= 0) {
            playTrack(currentTrackIndex);
            return;
        }

        int prevIndex = currentTrackIndex - 1;
        if (prevIndex < 0) {
            prevIndex = tracks.size() - 1;
        }
        playTrack(prevIndex);
    }

    public void tick(MinecraftClient client) {
        if (!isPlaying || currentTrackIndex < 0) return;

        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        if (currentTrackIndex >= tracks.size()) {
            stop();
            return;
        }

        elapsedTicks++;
        PlaylistTrack current = tracks.get(currentTrackIndex);
        int maxTicks = current.durationSeconds * 20;

        boolean soundFinished = currentSoundInstance != null && !client.getSoundManager().isPlaying(currentSoundInstance);
        if (elapsedTicks >= maxTicks || (elapsedTicks > 20 && soundFinished)) {
            nextTrack();
        }
    }

    public void cycleRepeatMode() {
        RepeatMode[] values = RepeatMode.values();
        repeatMode = values[(repeatMode.ordinal() + 1) % values.length];
    }

    public void toggleShuffle() {
        this.shuffle = !this.shuffle;
    }

    public boolean isPlaying() { return isPlaying; }
    public boolean isShuffle() { return shuffle; }
    public RepeatMode getRepeatMode() { return repeatMode; }
    public int getCurrentTrackIndex() { return currentTrackIndex; }
    public float getMasterVolumeMultiplier() { return masterVolumeMultiplier; }
    public void setMasterVolumeMultiplier(float vol) { this.masterVolumeMultiplier = vol; }

    public PlaylistTrack getCurrentTrack() {
        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        if (currentTrackIndex >= 0 && currentTrackIndex < tracks.size()) {
            return tracks.get(currentTrackIndex);
        }
        return null;
    }

    public float getProgress() {
        PlaylistTrack track = getCurrentTrack();
        if (track == null) return 0f;
        return Math.min(1.0f, (float) elapsedTicks / (track.durationSeconds * 20f));
    }

    public String getFormattedElapsed() {
        int secs = elapsedTicks / 20;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }
}
