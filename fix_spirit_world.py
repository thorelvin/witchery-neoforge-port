path = 'src/main/java/com/emoniph/witchery/spiritworld/SpiritWorldHelper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

import re

# Fix saveAndStripInventory
OLD_SAVE = "ListTag saved = new ListTag();\n        inv.save(saved);\n        data.setSavedInventorySnbt(saved.toString());"
NEW_SAVE = """// Serialize inventory using ItemStack.CODEC
        net.minecraft.nbt.ListTag invList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.nbt.CompoundTag itemTag = new net.minecraft.nbt.CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                net.minecraft.nbt.Tag saved2 = ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack).result().orElse(null);
                if (saved2 != null) itemTag.put("tag", saved2);
                invList.add(itemTag);
            }
        }
        net.minecraft.nbt.CompoundTag wrapper = new net.minecraft.nbt.CompoundTag();
        wrapper.put("inv", invList);
        data.setSavedInventorySnbt(wrapper.toString());"""
content = content.replace(OLD_SAVE, NEW_SAVE)

# Fix restoreInventory
OLD_LOAD = "Tag parsed = TagParser.parseTag(snbt);\n            if (!(parsed instanceof ListTag listTag)) return;\n            player.getInventory().clearContent();\n            player.getInventory().load(listTag);"
NEW_LOAD = """net.minecraft.nbt.Tag parsed = net.minecraft.nbt.TagParser.parseTag(snbt);
            if (!(parsed instanceof net.minecraft.nbt.CompoundTag wTag)) return;
            net.minecraft.nbt.ListTag invList = wTag.getList("inv", net.minecraft.nbt.Tag.TAG_COMPOUND);
            player.getInventory().clearContent();
            for (int i = 0; i < invList.size(); i++) {
                net.minecraft.nbt.CompoundTag itemTag = invList.getCompound(i);
                int slot = itemTag.getByte("Slot") & 0xFF;
                if (itemTag.contains("tag")) {
                    ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, itemTag.get("tag"))
                        .result().ifPresent(s -> player.getInventory().setItem(slot, s));
                }
            }"""
content = content.replace(OLD_LOAD, NEW_LOAD)

# Fix spawnNightmare
content = content.replace(
    "NightmareEntity nightmare = WitcheryEntities.NIGHTMARE.get().create(sw);",
    "NightmareEntity nightmare = WitcheryEntities.NIGHTMARE.get().create(sw, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);"
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed SpiritWorldHelper")
