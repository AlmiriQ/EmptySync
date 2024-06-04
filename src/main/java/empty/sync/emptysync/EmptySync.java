package empty.sync.emptysync;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EmptySync implements DedicatedServerModInitializer {
    public static final String MOD_ID = "empty-sync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final static class Configuration {
        String host = "localhost";
        int port = 2222;
    }

    @Override
    public void onInitializeServer() {
        LOGGER.info("Hello Fabric world!");
        Path configPath = Paths.get("empty-sync.json");
        if (!configPath.toFile().exists()) {
            LOGGER.error("Config file (empty-sync.json) not found!");
            return;
        }

        Configuration config;
        {
            Gson parser = new Gson();
            try {
                config = parser.fromJson(new String(Files.readAllBytes(configPath)), Configuration.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        HttpClient client = HttpClient.newHttpClient();
        String defaultURL = "http://" + config.host + ":" + config.port + "/";

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(defaultURL + handler.player.getUuid().toString()))
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String serialized = response.body();
                if (!serialized.equals("NX"))
                    SyncableInventory.deserialize(response.body(), handler.player.getInventory());
            } catch (Exception e) {
                LOGGER.error("Exception while JOIN: {}", e.toString());
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            try {
                SyncableInventory inventory = new SyncableInventory(handler.player.getInventory());
                String serialized = inventory.serialize();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(defaultURL + handler.player.getUuid().toString()))
                        .POST(HttpRequest.BodyPublishers.ofString(serialized))
                        .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                    LOGGER.info("Response: {}", response.body());
                });
            } catch (Exception e) {
                LOGGER.error("Exception while DISCONNECT: {}", e.toString());
            }
        });

    }
}
