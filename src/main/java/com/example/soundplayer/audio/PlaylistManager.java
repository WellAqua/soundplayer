package com.example.soundplayer.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlaylistManager {
    private static final PlaylistManager INSTANCE = new PlaylistManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("soundplayer_playlists.json");

    private Map<String, List<PlaylistTrack>> playlists = new LinkedHashMap<>();
    private String activePlaylistName = "Vanilla Discs";

    public static PlaylistManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                Type type = new TypeToken<Map<String, List<PlaylistTrack>>>() {}.getType();
                Map<String, List<PlaylistTrack>> loaded = GSON.fromJson(reader, type);
                if (loaded != null && !loaded.isEmpty()) {
                    playlists = loaded;
                    if (!playlists.containsKey(activePlaylistName)) {
                        activePlaylistName = playlists.keySet().iterator().next();
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        createDefaults();
        save();
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(playlists, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDefaults() {
        playlists.clear();
        List<PlaylistTrack> discs = new ArrayList<>();
        discs.add(new PlaylistTrack("C418 - Cat", "minecraft:music_disc.cat", 1.0f, 1.0f, 185));
        discs.add(new PlaylistTrack("Lena Raine - Pigstep", "minecraft:music_disc.pigstep", 1.0f, 1.0f, 148));
        discs.add(new PlaylistTrack("Lena Raine - Otherside", "minecraft:music_disc.otherside", 1.0f, 1.0f, 195));
        discs.add(new PlaylistTrack("Aaron Cherof - Relic", "minecraft:music_disc.relic", 1.0f, 1.0f, 218));
        discs.add(new PlaylistTrack("Lena Raine - Creator", "minecraft:music_disc.creator", 1.0f, 1.0f, 176));
        playlists.put("Vanilla Discs", discs);
        activePlaylistName = "Vanilla Discs";
    }

    public Map<String, List<PlaylistTrack>> getPlaylists() {
        return playlists;
    }

    public String getActivePlaylistName() {
        return activePlaylistName;
    }

    public void setActivePlaylistName(String name) {
        if (playlists.containsKey(name)) {
            this.activePlaylistName = name;
        }
    }

    public List<PlaylistTrack> getActiveTracks() {
        return playlists.computeIfAbsent(activePlaylistName, k -> new ArrayList<>());
    }

    public void createPlaylist(String name) {
        if (name != null && !name.isBlank() && !playlists.containsKey(name)) {
            playlists.put(name.trim(), new ArrayList<>());
            activePlaylistName = name.trim();
            save();
        }
    }

    public void deletePlaylist(String name) {
        if (playlists.size() > 1 && playlists.containsKey(name)) {
            playlists.remove(name);
            activePlaylistName = playlists.keySet().iterator().next();
            save();
        }
    }

    public void addTrack(PlaylistTrack track) {
        getActiveTracks().add(track);
        save();
    }

    public void removeTrack(int index) {
        List<PlaylistTrack> tracks = getActiveTracks();
        if (index >= 0 && index < tracks.size()) {
            tracks.remove(index);
            save();
        }
    }
}
