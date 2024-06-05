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

public class SerializableInventory implements Serializable {
    private final PlayerInventory playerInventory;
    private static final int INVENTORY_SIZE = 9 * 4;
    private static final Gson parser = new Gson();

    public static final String MOD_ID = "empty-sync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SerializableInventory(PlayerInventory playerInventory) {
        this.playerInventory = playerInventory;
    }

    public ArrayList<ItemStack> getItems (){
        ArrayList<ItemStack> items = new ArrayList<>(INVENTORY_SIZE);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            items.add(playerInventory.getStack(i));
        }
        return items;
    }

    private ItemDTO serializeItemStack(ItemStack item) {
        ItemDTO result = new ItemDTO();
        String NBTs = null;
        if (item.hasNbt()) {
            assert item.getNbt() != null;
            Gson serializer = new Gson();
            NBTs = Base64.getEncoder().encodeToString(serializer.toJson(NBTConverter.toJson(item.getNbt())).getBytes());
        }
        result.count = item.getCount();
        result.id = Item.getRawId(item.getItem());
        result.NBT = NBTs;
        return result;
    }

    public String serialize() {
        PlayerDTO result = new PlayerDTO();
        result.inventory = new ArrayList<>(36);
        result.armor = new ArrayList<>(4);
        ArrayList<ItemStack> items = getItems();
        for (ItemStack item : items)
            result.inventory.add(serializeItemStack(item));
        for (ItemStack item : playerInventory.armor)
            result.armor.add(serializeItemStack(item));
        result.offHand = serializeItemStack(playerInventory.offHand.get(0));
        result.experienceLevel = playerInventory.player.experienceLevel;
        result.experienceProgress = playerInventory.player.experienceProgress;
        return parser.toJson(result, PlayerDTO.class);
    }

    static ItemStack deserializeItemStack(ItemDTO item) {
        ItemStack result = new ItemStack(Item.byRawId(item.id), item.count);
        if ((item.NBT != null) && (item.count > 0)) {
            JsonObject je = JsonParser.parseString(new String(Base64.getDecoder().decode(item.NBT))).getAsJsonObject();
            result.setNbt(NBTConverter.toNbtCompound(je));
        }
        return result;
    }

    public static void deserialize(String serialized, PlayerInventory playerInventory) {
        Gson parser = new Gson();
        PlayerDTO player = parser.fromJson(serialized, PlayerDTO.class);
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
