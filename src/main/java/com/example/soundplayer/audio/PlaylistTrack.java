package com.example.soundplayer.audio;

public class PlaylistTrack {
    public String displayName;
    public String soundId;
    public float volume;
    public float pitch;
    public int durationSeconds;

    public PlaylistTrack(String displayName, String soundId, float volume, float pitch, int durationSeconds) {
        this.displayName = displayName;
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
        this.durationSeconds = Math.max(1, durationSeconds);
    }

    public String getFormattedDuration() {
        int mins = durationSeconds / 60;
        int secs = durationSeconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}
