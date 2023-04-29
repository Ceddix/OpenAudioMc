package com.craftmend.openaudiomc.spigot.modules.predictive;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.database.DatabaseService;
import com.craftmend.openaudiomc.generic.database.internal.Repository;
import com.craftmend.openaudiomc.generic.logging.OpenAudioLogger;
import com.craftmend.openaudiomc.generic.networking.abstracts.AbstractPacket;
import com.craftmend.openaudiomc.generic.client.objects.ClientConnection;
import com.craftmend.openaudiomc.generic.networking.interfaces.Authenticatable;
import com.craftmend.openaudiomc.generic.networking.interfaces.INetworkingEvents;
import com.craftmend.openaudiomc.generic.networking.interfaces.NetworkingService;
import com.craftmend.openaudiomc.generic.networking.payloads.client.interfaces.SourceHolder;
import com.craftmend.openaudiomc.generic.service.Inject;
import com.craftmend.openaudiomc.generic.service.Service;
import com.craftmend.openaudiomc.generic.utils.data.ConcurrentHeatMap;

import com.craftmend.openaudiomc.spigot.modules.predictive.serialization.ChunkMapSerializer;
import com.craftmend.openaudiomc.spigot.modules.predictive.serialization.SerializedAudioChunk;
import com.craftmend.openaudiomc.spigot.modules.predictive.sorage.StoredWorldChunk;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Map;

@NoArgsConstructor
public class PredictiveMediaService extends Service {

    @Inject
    private DatabaseService databaseService;

    private final ChunkMapSerializer chunkMapSerializer = new ChunkMapSerializer();
    private final int chunkAge = 60 * 60 * 10;  // chunk values are kept for 10 hours
    private final int maxChunkData = 70;       // keep up to 70 chunks
    private final int maxChunkCache = 15;      // keep 15 sounds per chunk

    // map "active" audio chunks of the world
    @Getter private ConcurrentHeatMap<String, ConcurrentHeatMap<String, Byte>> chunkTracker = new ConcurrentHeatMap<>(
            chunkAge,
            maxChunkData,
            () -> new ConcurrentHeatMap<String, Byte>(chunkAge, maxChunkCache, ConcurrentHeatMap.BYTE_CONTEXT)
    );

    @Override
    public void onEnable() {
        OpenAudioMc.getService(NetworkingService.class).addEventHandler(getPacketHook());
        try {
            loadFromFile();
        } catch (IOException e) {
            OpenAudioLogger.toConsole("Failed to load chunk-cache from file.");
        }
    }

    public void loadFromFile() throws IOException {
        // load SerializedAudioChunk.ChunkMap.class
        Repository<StoredWorldChunk> scm = databaseService.getRepository(StoredWorldChunk.class);

        SerializedAudioChunk.ChunkMap cm = new SerializedAudioChunk.ChunkMap();

        for (StoredWorldChunk value : scm.values()) {
            cm.getData().put(value.getChunkName(), value.getAudioChunk());
        }

        chunkTracker = chunkMapSerializer.applyFromChunkMap(cm, chunkTracker);
    }

    public void onDisable() {
        // save
        OpenAudioLogger.toConsole("Saving world cache...");
        Repository<StoredWorldChunk> repo = databaseService.getRepository(StoredWorldChunk.class);
        for (Map.Entry<String, SerializedAudioChunk.Chunk> entry : chunkMapSerializer.serialize(chunkTracker).getData().entrySet()) {
            String name = entry.getKey();
            SerializedAudioChunk.Chunk chunk = entry.getValue();
            StoredWorldChunk swc = new StoredWorldChunk(name, chunk);
            repo.save(swc);
        }
    }

    private INetworkingEvents getPacketHook() {
        return new INetworkingEvents() {
            @Override
            public void onPacketSend(Authenticatable target, AbstractPacket packet) {
                if (packet.getData() instanceof SourceHolder) {
                    String source = ((SourceHolder) packet.getData()).getSource();
                    ClientConnection client = (ClientConnection) target;
                    Player player = (Player) client.getUser().getOriginal();

                    // bump the source for the players chunk chunk
                    chunkTracker.get(locationToAudioChunkId(player.getLocation())).getContext().bump(source);
                }
            }
        };
    }

    public String locationToAudioChunkId(Location location) {
        int chunkX = step(location.getBlockX());
        int chunkZ = step(location.getBlockZ());
        return chunkX + "@" + chunkZ;
    }

    private Integer step(Integer i) {
        if (i == 0) {
            return 0;
        }
        return i / 150;
    }
}
