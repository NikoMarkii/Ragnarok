package com.niko.ragnarok.entity.geckolib_entity.Costom;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * フェアリーの取引リスト
 * 花を通貨として様々なアイテムと交換できる
 */
public class Fairytrades {

    /**
     * フェアリーとの取引を提供する基本クラス
     */
    public static class FlowerTrade implements VillagerTrades.ItemListing {
        private final ItemStack inputFlower;
        private final int flowerCount;
        private final ItemStack output;
        private final int maxUses;
        private final int xpValue;

        public FlowerTrade(ItemStack inputFlower, int flowerCount, ItemStack output, int maxUses, int xpValue) {
            this.inputFlower = inputFlower;
            this.flowerCount = flowerCount;
            this.output = output;
            this.maxUses = maxUses;
            this.xpValue = xpValue;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            ItemStack input = this.inputFlower.copy();
            input.setCount(this.flowerCount);

            return new MerchantOffer(
                    input,
                    this.output.copy(),
                    this.maxUses,
                    this.xpValue,
                    0.05F
            );
        }
    }

    /**
     * 基本的な取引リスト
     */
    public static VillagerTrades.ItemListing[] getBasicTrades() {
        return new VillagerTrades.ItemListing[] {
                // タンポポ2個 → グロウストーンダスト1個
                new FlowerTrade(new ItemStack(Items.DANDELION), 2, new ItemStack(Items.GLOWSTONE_DUST), 12, 2),

                // ポピー3個 → レッドストーン4個
                new FlowerTrade(new ItemStack(Items.POPPY), 3, new ItemStack(Items.REDSTONE, 4), 12, 2),

                // 青いラン2個 → ラピスラズリ3個
                new FlowerTrade(new ItemStack(Items.BLUE_ORCHID), 2, new ItemStack(Items.LAPIS_LAZULI, 3), 12, 2),

                // ヒスイラン4個 → エンダーパール1個
                new FlowerTrade(new ItemStack(Items.ALLIUM), 4, new ItemStack(Items.ENDER_PEARL), 8, 5),

                // ヤグルマギク3個 → 骨粉6個
                new FlowerTrade(new ItemStack(Items.CORNFLOWER), 3, new ItemStack(Items.BONE_MEAL, 6), 16, 1),
        };
    }

    /**
     * レアな取引リスト
     */
    public static VillagerTrades.ItemListing[] getRareTrades() {
        return new VillagerTrades.ItemListing[] {
                // ウィザーローズ1個 → ネザースター1個（超レア）
                new FlowerTrade(new ItemStack(Items.WITHER_ROSE), 1, new ItemStack(Items.NETHER_STAR), 1, 30),

                // ヒマワリ8個 → エンチャントされた金のリンゴ1個
                new FlowerTrade(new ItemStack(Items.SUNFLOWER), 8, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 3, 20),

                // ライラック5個 → ダイヤモンド2個
                new FlowerTrade(new ItemStack(Items.LILAC), 5, new ItemStack(Items.DIAMOND, 2), 6, 10),

                // バラの低木6個 → エメラルド4個
                new FlowerTrade(new ItemStack(Items.ROSE_BUSH), 6, new ItemStack(Items.EMERALD, 4), 8, 8),

                // ボタン10個 → トーテム・オブ・アンデイング1個（超レア）
                new FlowerTrade(new ItemStack(Items.PEONY), 10, new ItemStack(Items.TOTEM_OF_UNDYING), 1, 50),
        };
    }

    /**
     * 装飾品の取引リスト
     */
    public static VillagerTrades.ItemListing[] getDecorativeTrades() {
        return new VillagerTrades.ItemListing[] {
                // 白いチューリップ4個 → グロウベリー8個
                new FlowerTrade(new ItemStack(Items.WHITE_TULIP), 4, new ItemStack(Items.GLOW_BERRIES, 8), 12, 3),

                // ピンクのチューリップ3個 → 蜂蜜入りの瓶2個
                new FlowerTrade(new ItemStack(Items.PINK_TULIP), 3, new ItemStack(Items.HONEY_BOTTLE, 2), 12, 3),

                // フランスギク5個 → 音符ブロック1個
                new FlowerTrade(new ItemStack(Items.OXEYE_DAISY), 5, new ItemStack(Items.NOTE_BLOCK), 8, 4),

                // スズラン6個 → 輝くツツジの葉8個
                new FlowerTrade(new ItemStack(Items.LILY_OF_THE_VALLEY), 6, new ItemStack(Items.FLOWERING_AZALEA_LEAVES, 8), 10, 4),
        };
    }
}
