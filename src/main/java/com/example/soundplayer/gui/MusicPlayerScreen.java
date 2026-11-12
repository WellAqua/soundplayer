package com.example.soundplayer.gui;

import com.example.soundplayer.audio.PlaylistManager;
import com.example.soundplayer.audio.PlaylistTrack;
import com.example.soundplayer.audio.SoundPlaybackController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerScreen extends Screen {
    private TextFieldWidget soundIdField;
    private TextFieldWidget trackNameField;
    private TextFieldWidget durationField;
    private TextFieldWidget pitchField;
    private TextFieldWidget newPlaylistField;

    private int trackScrollOffset = 0;
    private final List<String> autocompleteSuggestions = new ArrayList<>();

    public MusicPlayerScreen() {
        super(Text.literal("Sound Playlist Player"));
    }

    @Override
    protected void init() {
        int sidebarWidth = 140;
        int bottomBarHeight = 64;
        int mainX = sidebarWidth + 10;

        newPlaylistField = new TextFieldWidget(this.textRenderer, 10, 28, 92, 18, Text.literal("New Playlist"));
        newPlaylistField.setPlaceholder(Text.literal("Playlist name..."));
        this.addDrawableChild(newPlaylistField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            PlaylistManager.getInstance().createPlaylist(newPlaylistField.getText());
            newPlaylistField.setText("");
            this.clearAndInit();
        }).dimensions(106, 28, 24, 18).build());

        int py = 54;
        for (String playlistName : PlaylistManager.getInstance().getPlaylists().keySet()) {
            boolean isActive = playlistName.equals(PlaylistManager.getInstance().getActivePlaylistName());
            String label = (isActive ? "> " : "") + playlistName;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                PlaylistManager.getInstance().setActivePlaylistName(playlistName);
                trackScrollOffset = 0;
                this.clearAndInit();
            }).dimensions(10, py, 100, 18).build());

            if (PlaylistManager.getInstance().getPlaylists().size() > 1) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
                    PlaylistManager.getInstance().deletePlaylist(playlistName);
                    this.clearAndInit();
                }).dimensions(112, py, 18, 18).build());
            }
            py += 21;
        }

        int col1 = mainX;
        int col2 = mainX + 165;
        int col3 = mainX + 285;
        int col4 = mainX + 335;

        soundIdField = new TextFieldWidget(this.textRenderer, col1, 24, 160, 18, Text.literal("Sound ID"));
        soundIdField.setMaxLength(128);
        soundIdField.setPlaceholder(Text.literal("minecraft:music_disc.cat"));
        soundIdField.setChangedListener(this::updateAutocomplete);
        this.addDrawableChild(soundIdField);

        trackNameField = new TextFieldWidget(this.textRenderer, col2, 24, 115, 18, Text.literal("Display Name"));
        trackNameField.setPlaceholder(Text.literal("Song Title"));
        this.addDrawableChild(trackNameField);

        durationField = new TextFieldWidget(this.textRenderer, col3, 24, 45, 18, Text.literal("Sec"));
        durationField.setText("180");
        this.addDrawableChild(durationField);

        pitchField = new TextFieldWidget(this.textRenderer, col4, 24, 40, 18, Text.literal("Pitch"));
        pitchField.setText("1.0");
        this.addDrawableChild(pitchField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Track"), btn -> addTrackFromInputs())
                .dimensions(col4 + 45, 24, 65, 18).build());

        SoundPlaybackController controller = SoundPlaybackController.getInstance();
        int centerX = this.width / 2;
        int controlsY = this.height - 34;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(controller.isShuffle() ? "Shuffle: ON" : "Shuffle: OFF"),
                btn -> {
                    controller.toggleShuffle();
                    btn.setMessage(Text.literal(controller.isShuffle() ? "Shuffle: ON" : "Shuffle: OFF"));
                }).dimensions(centerX - 175, controlsY, 72, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("|<"), btn -> controller.previousTrack())
                .dimensions(centerX - 95, controlsY, 32, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Play / Pause"), btn -> controller.togglePlayPause())
                .dimensions(centerX - 58, controlsY, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">|"), btn -> controller.nextTrack())
                .dimensions(centerX + 27, controlsY, 32, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), btn -> controller.stop())
                .dimensions(centerX + 64, controlsY, 42, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(controller.getRepeatMode().label),
                btn -> {
                    controller.cycleRepeatMode();
                    btn.setMessage(Text.literal(controller.getRepeatMode().label));
                }).dimensions(centerX + 112, controlsY, 75, 20).build());

        this.addDrawableChild(new SliderWidget(this.width - 115, controlsY, 105, 20,
                Text.literal("Vol: " + (int) (controller.getMasterVolumeMultiplier() * 100) + "%"),
                controller.getMasterVolumeMultiplier()) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Vol: " + (int) (this.value * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                controller.setMasterVolumeMultiplier((float) this.value);
            }
        });
    }

    private void updateAutocomplete(String query) {
        autocompleteSuggestions.clear();
        if (query == null || query.length() < 2) return;
        String lower = query.toLowerCase();
        for (Identifier id : Registries.SOUND_EVENT.getIds()) {
            String str = id.toString();
            if (str.contains(lower)) {
                autocompleteSuggestions.add(str);
                if (autocompleteSuggestions.size() >= 5) break;
            }
        }
    }

    private void addTrackFromInputs() {
        String rawId = soundIdField.getText().trim();
        if (rawId.isEmpty()) return;
        if (!rawId.contains(":")) rawId = "minecraft:" + rawId;

        String name = trackNameField.getText().trim();
        if (name.isEmpty()) {
            name = rawId.substring(rawId.indexOf(':') + 1);
        }

        int duration = 180;
        float pitch = 1.0f;
        try { duration = Integer.parseInt(durationField.getText().trim()); } catch (NumberFormatException ignored) {}
        try { pitch = Math.max(0.5f, Math.min(2.0f, Float.parseFloat(pitchField.getText().trim()))); } catch (NumberFormatException ignored) {}

        PlaylistManager.getInstance().addTrack(new PlaylistTrack(name, rawId, 1.0f, pitch, duration));
        soundIdField.setText("");
        trackNameField.setText("");
        autocompleteSuggestions.clear();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int sidebarWidth = 140;
        int bottomBarTop = this.height - 64;
        int listTop = 68;

        context.fill(0, 0, sidebarWidth, bottomBarTop, 0xCC121212);
        context.fill(sidebarWidth, 0, this.width, bottomBarTop, 0xAA181818);
        context.fill(0, bottomBarTop, this.width, this.height, 0xEE0D0D0D);

        context.drawTextWithShadow(this.textRenderer, "PLAYLISTS", 10, 12, 0xFF1DB954);
        context.drawTextWithShadow(this.textRenderer, "Sound ID (/playsound)", sidebarWidth + 10, 12, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Title", sidebarWidth + 175, 12, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Sec", sidebarWidth + 295, 12, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Pitch", sidebarWidth + 345, 12, 0xFFAAAAAA);

        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        SoundPlaybackController controller = SoundPlaybackController.getInstance();
        int rowHeight = 24;
        int visibleRows = (bottomBarTop - listTop - 10) / rowHeight;

        for (int i = 0; i < visibleRows && (i + trackScrollOffset) < tracks.size(); i++) {
            int trackIndex = i + trackScrollOffset;
            PlaylistTrack track = tracks.get(trackIndex);
            int rowY = listTop + (i * rowHeight);
            int rowX = sidebarWidth + 10;
            int rowW = this.width - sidebarWidth - 20;

            boolean isCurrent = trackIndex == controller.getCurrentTrackIndex();
            boolean isHovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY <= rowY + rowHeight - 2;

            int bgColor = isCurrent ? 0x551DB954 : (isHovered ? 0x33FFFFFF : 0x22000000);
            context.fill(rowX, rowY, rowX + rowW, rowY + rowHeight - 2, bgColor);

            String indexPrefix = isCurrent && controller.isPlaying() ? "▶ " : (trackIndex + 1) + ". ";
            int titleColor = isCurrent ? 0xFF1DB954 : 0xFFFFFFFF;

            context.drawTextWithShadow(this.textRenderer, indexPrefix + track.displayName, rowX + 6, rowY + 6, titleColor);
            context.drawTextWithShadow(this.textRenderer, track.soundId, rowX + 165, rowY + 6, 0xFF888888);
            context.drawTextWithShadow(this.textRenderer, track.pitch + "x", rowX + rowW - 85, rowY + 6, 0xFFAAAAAA);
            context.drawTextWithShadow(this.textRenderer, track.getFormattedDuration(), rowX + rowW - 48, rowY + 6, 0xFFAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "[X]", rowX + rowW - 20, rowY + 6, 0xFFFF5555);
        }

        PlaylistTrack current = controller.getCurrentTrack();
        String nowPlayingText = current != null
                ? "Now Playing: " + current.displayName + " (" + current.soundId + ")"
                : "No track selected";
        context.drawTextWithShadow(this.textRenderer, nowPlayingText, 12, bottomBarTop + 8, 0xFFFFFFFF);

        int barX = this.width / 2 - 140;
        int barY = bottomBarTop + 12;
        int barW = 280;
        context.drawTextWithShadow(this.textRenderer, controller.getFormattedElapsed(), barX - 32, barY - 2, 0xFFAAAAAA);
        context.fill(barX, barY, barX + barW, barY + 4, 0xFF444444);
        context.fill(barX, barY, barX + (int) (barW * controller.getProgress()), barY + 4, 0xFF1DB954);
        context.drawTextWithShadow(this.textRenderer, current != null ? current.getFormattedDuration() : "0:00", barX + barW + 8, barY - 2, 0xFFAAAAAA);

        super.render(context, mouseX, mouseY, delta);

        if (!autocompleteSuggestions.isEmpty() && soundIdField.isFocused()) {
            int dropY = 44;
            for (String suggestion : autocompleteSuggestions) {
                context.fill(sidebarWidth + 10, dropY, sidebarWidth + 240, dropY + 14, 0xF0222222);
                context.drawTextWithShadow(this.textRenderer, suggestion, sidebarWidth + 14, dropY + 3, 0xFFDDDDDD);
                dropY += 14;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int sidebarWidth = 140;
        int bottomBarTop = this.height - 64;
        int listTop = 68;

        if (!autocompleteSuggestions.isEmpty() && soundIdField.isFocused()) {
            int dropY = 44;
            for (String suggestion : autocompleteSuggestions) {
                if (mouseX >= sidebarWidth + 10 && mouseX <= sidebarWidth + 240 && mouseY >= dropY && mouseY <= dropY + 14) {
                    soundIdField.setText(suggestion);
                    autocompleteSuggestions.clear();
                    return true;
                }
                dropY += 14;
            }
        }

        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        int rowHeight = 24;
        int visibleRows = (bottomBarTop - listTop - 10) / rowHeight;

        for (int i = 0; i < visibleRows && (i + trackScrollOffset) < tracks.size(); i++) {
            int trackIndex = i + trackScrollOffset;
            int rowY = listTop + (i * rowHeight);
            int rowX = sidebarWidth + 10;
            int rowW = this.width - sidebarWidth - 20;

            if (mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY <= rowY + rowHeight - 2) {
                if (mouseX >= rowX + rowW - 24) {
                    PlaylistManager.getInstance().removeTrack(trackIndex);
                } else {
                    SoundPlaybackController.getInstance().playTrack(trackIndex);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<PlaylistTrack> tracks = PlaylistManager.getInstance().getActiveTracks();
        if (verticalAmount < 0 && trackScrollOffset < Math.max(0, tracks.size() - 1)) {
            trackScrollOffset++;
            return true;
        } else if (verticalAmount > 0 && trackScrollOffset > 0) {
            trackScrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
