package com.niko.ragnarok.datagen.client;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import java.util.Locale;

public class ENUSLanguageProvider extends LanguageProvider {
    public ENUSLanguageProvider(PackOutput output) {
        super(output, Ragnarok.MOD_ID, Locale.US.toString().toLowerCase());
    }

    @Override
    protected void addTranslations() {
        addItem(Ragnarok_mainItems.T_LEX_SPAWN_EGG,"t lex spawn egg");
        addItem(Ragnarok_mainItems.RED_CREEPER_SPAWN_EGG,"red creeper spawn egg");
        addItem(Ragnarok_mainItems.SCORPION_SPAWN_EGG,"scorpion spawn egg");
        addItem(Ragnarok_mainItems.RAW_NAITOMEA,"Raw Naitonium");
        addItem(Ragnarok_mainItems.NAITOMEA_INGOD,"naitonium ingot");
        addItem(Ragnarok_mainItems.DRAGON_SCALE,"dragon scale");
        addItem(Ragnarok_mainItems.NAITOMEA_SWORD,"naitonium sword");
        addItem(Ragnarok_mainItems.NAITOMEA_PICKAXE,"naitonium pickaxe");
        addItem(Ragnarok_mainItems.NAITOMEA_AXE,"naitonium axe");
        addItem(Ragnarok_mainItems.NAITOMEA_SHOVEL,"naitonium shovel");
        addItem(Ragnarok_mainItems.NAITOMEA_HOE,"naitonium hoe");
        addItem(Ragnarok_mainItems.SCORPION_NECKLACE,"scorpion necklase");
        addItem(Ragnarok_mainItems.SCORPION_CELL,"scorpion cell");
        addItem(Ragnarok_mainItems.SCORPION_NEEDLE,"scorpion needle");

        add("creativetabs.ragnarokModItems","Eclipse Awakened");

        add("itemGroup.ragnarok_main","Eclipse Awakened");

        addBlock(RagnarokBlocks.NAITOMEA_BLOCK,"naitonium block");
        addBlock(RagnarokBlocks.RAW_NAITOMEA_BLOCK,"raw naitonium block");
        addBlock(RagnarokBlocks.NAITOMEA_ORE,"naitonium ore");

        addEntityType(RagnarokEntities.T_LEX,"t lex");
        addEntityType(RagnarokEntities.RED_CREEPER,"red creeper");
        addEntityType(RagnarokEntities.SCORPION,"scorpion");

        add("curios.identifier.bracelet", "bracelet");

    }
}
