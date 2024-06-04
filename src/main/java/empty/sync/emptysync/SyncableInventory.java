package empty.sync.emptysync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SyncableInventory implements Serializable {
    private final PlayerInventory playerInventory;
    private static final int INVENTORY_SIZE = 9 * 4;

    public static final String MOD_ID = "empty-sync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final class Player {
        static final class Item {
            int id;
            int count;
            String NBT;
        }
        int experienceLevel;
        float experienceProgress;
        List<Item> inventory;
        List<Item> armor;
        Item offHand;
    }

    public SyncableInventory(PlayerInventory playerInventory) {
        this.playerInventory = playerInventory;
    }

    public ArrayList<ItemStack> getItems (){
        ArrayList<ItemStack> items = new ArrayList<>(INVENTORY_SIZE);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            items.add(playerInventory.getStack(i));
        }
        return items;
    }

    private String serializeItemStack(ItemStack item) {
        StringBuilder result = new StringBuilder();
        String NBTs = null;
        if (item.hasNbt()) {
            assert item.getNbt() != null;
            Gson serializer = new Gson();
            NBTs = Base64.getEncoder().encodeToString(serializer.toJson(NBTConverter.toJson(item.getNbt())).getBytes());
        }
        result.append("{\"count\":");
        result.append(item.getCount());
        result.append(",\"id\":");
        result.append(Item.getRawId(item.getItem()));
        result.append(",\"NBT\":");
        if (NBTs!= null)
            result.append("\"").append(NBTs).append("\"");
        else result.append("null");
        result.append("}");
        return result.toString();
    }

    public String serialize() {
        ArrayList<ItemStack> items = getItems();
        StringBuilder result = new StringBuilder();
        result.append("{\"inventory\":[");
        {
            String comma = "";
            for (ItemStack item : items) {
                result.append(comma);
                comma = ",";
                result.append(serializeItemStack(item));
            }
        }
        result.append("],\"armor\":[");
        {
            String comma = "";
            for (ItemStack item : playerInventory.armor) {
                result.append(comma);
                comma = ",";
                result.append(serializeItemStack(item));
            }
        }
        result.append("],\"offHand\":");
        result.append(serializeItemStack(playerInventory.offHand.get(0)));
        result.append(",\"experienceLevel\":");
        result.append(playerInventory.player.experienceLevel);
        result.append(",\"experienceProgress\":");
        result.append(playerInventory.player.experienceProgress);
        result.append("}");
        return result.toString();
    }

    static ItemStack deserializeItemStack(Player.Item item) {
        ItemStack result = new ItemStack(Item.byRawId(item.id), item.count);
//        Gson parser = new Gson();
        if ((item.NBT != null) && (item.count > 0)) {
            LOGGER.info("nbt: {}", new String(Base64.getDecoder().decode(item.NBT)));
            JsonObject je = JsonParser.parseString(new String(Base64.getDecoder().decode(item.NBT))).getAsJsonObject();
            result.setNbt(NBTConverter.toNbtCompound(je));
        } else LOGGER.info("no nbt for item {}", result.getItem().toString());
        return result;
    }

    public static void deserialize(String serialized, PlayerInventory playerInventory) {
        Gson parser = new Gson();
        Player player = parser.fromJson(serialized, Player.class);
        LOGGER.info(String.valueOf(player.experienceLevel));
        LOGGER.info(String.valueOf(player.experienceProgress));
        playerInventory.player.experienceLevel = player.experienceLevel;
        playerInventory.player.experienceProgress = player.experienceProgress;
        LOGGER.info(String.valueOf(playerInventory.player.experienceLevel));
        LOGGER.info(String.valueOf(playerInventory.player.experienceProgress));
        for (int i = 0; i < INVENTORY_SIZE; i++)
            playerInventory.setStack(i, deserializeItemStack(player.inventory.get(i)));
        for (int i = 0; i < 4; i++)
            playerInventory.armor.set(i, deserializeItemStack(player.armor.get(i)));
        playerInventory.offHand.set(0, deserializeItemStack(player.offHand));
    }
}
