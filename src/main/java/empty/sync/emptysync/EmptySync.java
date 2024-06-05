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
        Configuration config;
        Path configPath = Paths.get("empty-sync.json");
        if (!configPath.toFile().exists()) {
            LOGGER.error("Config file (empty-sync.json) not found!");
            return;
        }
        { // here we grab the configuration file and get sync core ip and port
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

                if (response.statusCode() != 200)
                    throw new Exception("Server returned error: " + response.body());

                String serialized = response.body();

                if (serialized.equals("NX")) { // user inventory not found on sync core
                    LOGGER.info("No data for player {}", handler.player.getName().toString());
                    return;
                }
                // writing data from sync core to user's inventory
                SerializableInventory.deserialize(response.body(), handler.player.getInventory());
            } catch (Exception e) {
                LOGGER.error("Exception while JOIN: {}", e.toString()); // something wrong IDK
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            try {
                // getting data from user's inventory and serializing it
                SerializableInventory inventory = new SerializableInventory(handler.player.getInventory());
                String serialized = inventory.serialize(); // this is json containing user's inventory

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(defaultURL + handler.player.getUuid().toString()))
                        .POST(HttpRequest.BodyPublishers.ofString(serialized))
                        .build();

                // sending data to sync core
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> LOGGER.info("Response: {}", response.body()));
            } catch (Exception e) {
                LOGGER.error("Exception while DISCONNECT: {}", e.toString()); // also something wrong IDK
            }
        });
    }
}
