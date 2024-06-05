package empty.sync.emptysync;

import com.google.gson.Gson;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Base64;

public class ItemDTO {
    int id;
    int count;
    String NBT;

    ItemDTO(ItemStack item) {
        String NBTs = null;
        if (item.hasNbt()) {
            assert item.getNbt() != null;
            Gson serializer = new Gson();
            NBTs = Base64.getEncoder().encodeToString(serializer.toJson(NBTConverter.toJson(item.getNbt())).getBytes());
        }

        this.count = item.getCount();
        this.id = Item.getRawId(item.getItem());
        this.NBT = NBTs;
    }
}
