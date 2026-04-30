package com.niko.ragnarok.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ItemDragonScale extends Item {
    public ItemDragonScale() {
        super(new Properties()
                .rarity(Rarity.RARE)
        );
    }
}
