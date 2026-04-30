package com.niko.ragnarok.datagen.client;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import java.util.Locale;

public class ENJPLanguageProvider extends LanguageProvider {
    public ENJPLanguageProvider(PackOutput output) {
        super(output, Ragnarok.MOD_ID, Locale.JAPAN.toString().toLowerCase());
    }

    @Override
    protected void addTranslations() {
        addItem(Ragnarok_mainItems.T_LEX_SPAWN_EGG,"ティラノサウルスのスポーンエッグ");
        addItem(Ragnarok_mainItems.RED_CREEPER_SPAWN_EGG,"レッドクリーパーのスポーンエッグ");
        addItem(Ragnarok_mainItems.SCORPION_SPAWN_EGG,"サソリのスポーンエッグ");
        addItem(Ragnarok_mainItems.RAW_NAITOMEA,"ナイトニウムの原石");
        addItem(Ragnarok_mainItems.NAITOMEA_INGOD,"ナイトニウムのインゴッド");
        addItem(Ragnarok_mainItems.DRAGON_SCALE,"ドラゴンの鱗");
        addItem(Ragnarok_mainItems.NAITOMEA_SWORD,"ナイトニウムの剣");
        addItem(Ragnarok_mainItems.NAITOMEA_PICKAXE,"ナイトニウムのツルハシ");
        addItem(Ragnarok_mainItems.NAITOMEA_AXE,"ナイトニウムの斧");
        addItem(Ragnarok_mainItems.NAITOMEA_SHOVEL,"ナイトニウムのシャベル");
        addItem(Ragnarok_mainItems.NAITOMEA_HOE,"ナイトニウムのクワ");
        addItem(Ragnarok_mainItems.SCORPION_CELL,"サソリの殻");
        addItem(Ragnarok_mainItems.SCORPION_NEEDLE,"サソリの針");

        add("creativetabs.ragnarokModItems","Eclipse Awakened");

        add("itemGroup.ragnarok_main","Eclipse Awakened");

        addBlock(RagnarokBlocks.NAITOMEA_BLOCK,"ナイトニウムブロック");
        addBlock(RagnarokBlocks.RAW_NAITOMEA_BLOCK,"ナイトニウムの原石ブロック");
        addBlock(RagnarokBlocks.NAITOMEA_ORE,"ナイトニウム鉱石");

        addEntityType(RagnarokEntities.T_LEX,"ティラノサウルス");
        addEntityType(RagnarokEntities.RED_CREEPER,"レッドクリーパー");
        addEntityType(RagnarokEntities.SCORPION,"サソリ");

        add("curios.identifier.bracelet", "ブレスレット");

    }
}
