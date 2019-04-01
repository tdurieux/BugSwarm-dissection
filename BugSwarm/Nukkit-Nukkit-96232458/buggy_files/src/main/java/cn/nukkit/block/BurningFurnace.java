package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.Tool;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.tile.Furnace;
import cn.nukkit.tile.Tile;

import java.util.Iterator;
import java.util.Map;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class BurningFurnace extends Solid{

    public BurningFurnace() {
        this(0);
    }

    public BurningFurnace(int meta) {
        super(meta);
    }

    @Override
    public int getId() {
        return Block.BURNING_FURNACE;
    }

    @Override
    public String getName() {
        return "Burning Furnace";
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public double getHardness() {
        return 3.5;
    }

    @Override
    public int getToolType() {
        return Tool.TYPE_PICKAXE;
    }

    @Override
    public int getLightLevel() {
        return 13;
    }

    @Override
    public boolean place(Item item, Block block, Block target, int face, double fx, double fy, double fz, Player player) {
        int faces[] = {4, 2, 5, 3};
        this.meta = faces[player instanceof Player ? player.getDirection() : 0];
        this.getLevel().setBlock(block, this, true, true);
        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Items"))
                .putString("id", Tile.FURNACE)
                .putInt("x", (int) this.x)
                .putInt("y", (int) this.y)
                .putInt("z", (int) this.z);

        if(item.hasCustomName()) {
            nbt.putString("CustomName", item.getCustomName());
        }

        if(item.hasCustomBlockData()) {
            Map<String, Tag> customData = item.getCustomBlockData().getTags();
            Iterator iter = customData.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry tag = (Map.Entry) iter.next();
                nbt.put((String) tag.getKey(), (Tag) tag.getValue());
            }
        }
        Tile.createTile("Furnace", this.getLevel().getChunk((int) (this.x) >> 4, (int) (this.z) >> 4), nbt);

        return true;
    }

    @Override
    public boolean onBreak(Item item) {
        this.getLevel().setBlock(this, new Air(), true, true);
        return true;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if(player instanceof Player) {
            Tile t = this.getLevel().getTile(this);
            Furnace furnace = null;
            if(t instanceof Furnace) {
                furnace = (Furnace) t;
            }
            else {
                CompoundTag nbt = new CompoundTag()
                        .putList(new ListTag<>("Items"))
                        .putString("id", Tile.FURNACE)
                        .putInt("x", (int) this.x)
                        .putInt("y", (int) this.y)
                        .putInt("z", (int) this.z);
                furnace = (Furnace) (Tile.createTile("Furnace", this.getLevel().getChunk((int) (this.x) >> 4, (int) (this.z) >> 4), nbt));
            }

            if(furnace.namedTag.contains("Lock") && furnace.namedTag.get("Lock") instanceof StringTag) {
                if(furnace.namedTag.getString("Lock") != item.getCustomName()) {
                    return true;
                }
            }

            if(player.isCreative()) {
                return true;
            }

            player.addWindow(furnace.getInventory());
        }

        return true;
    }

    @Override
    public int[][] getDrops(Item item) {
        if (item.isPickaxe() && item.getTier() >= Tool.TIER_WOODEN) {
            return new int[][]{new int[]{Item.FURNACE, 0, 1}};
        } else {
            return new int[0][];
        }
    }
}
