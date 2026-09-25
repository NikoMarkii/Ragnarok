package com.niko.ragnarok.event;

import com.niko.ragnarok.client.ScreenShakeHandler;
import com.niko.ragnarok.client.gui.NpcDialogueScreen;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.Projectile.DinocampusBubbleEntity;
import com.niko.ragnarok.entity.costom.Groot;
import com.niko.ragnarok.entity.costom.Magic_Golem;
import com.niko.ragnarok.item.Armor.GradiusArmorItem;
import com.niko.ragnarok.item.ItemScorpionNecklace;
import com.niko.ragnarok.item.VoidScythe;
import com.niko.ragnarok.network.RagnarokNetwork;
import com.niko.ragnarok.network.ScreenShakePacket;
import com.niko.ragnarok.world.WorldModeData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosCapability;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "ragnarok")
public class RagnarokEvent {

    // ドラゴンの消滅検知からハードモード発火までのカウントダウンマップ
    private static final Map<UUID, Integer> PENDING_HARDMODE_TICKS = new HashMap<>();

    /**
     * 1. エンダードラゴンの消滅演出が完了し、ワールドから除外（remove）された瞬間をフック
     */
    @SubscribeEvent
    public static void onDragonLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        // ワールドから離脱したのがエンダードラゴンであり、かつ「死亡・消滅（KILLED/DISCARDED）」による離脱かをチェック
        if (event.getEntity() instanceof EnderDragon dragon && dragon.isRemoved()) {
            // 消滅完了（ポータル出現＆経験値放出）の瞬間から正確に 20 ticks（1秒後）のタイマーをセット
            PENDING_HARDMODE_TICKS.put(dragon.getUUID(), 20);
        }
    }

    /**
     * 2. サーバーTickで20 ticks（1秒）のカウントダウンを進行
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!PENDING_HARDMODE_TICKS.isEmpty()) {
            ServerLevel overworld = event.getServer().overworld();

            PENDING_HARDMODE_TICKS.entrySet().removeIf(entry -> {
                int left = entry.getValue() - 1;
                if (left <= 0) {
                    // 20 ticks経過：ハードモード突入処理を発火
                    triggerHardmode(overworld);
                    return true; // リストから削除
                } else {
                    entry.setValue(left);
                    return false;
                }
            });
        }
    }
    /**
     * ハードモード突入演出およびデータ更新
     */
    public static void triggerHardmode(ServerLevel level) {
        WorldModeData modeData = WorldModeData.get(level);

        // すでにハードモード以上の場合は処理しない
        if (modeData.getCurrentState() != WorldModeData.GameModeState.NORMAL) {
            return;
        }

        // 状態をハードモードに変更
        modeData.setCurrentState(WorldModeData.GameModeState.HARD);

        // 演出：テラリア風メッセージ（langファイルを参照）
        Component message = Component.translatable("message.ragnarok.hardmode_start")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);

        level.getServer().getPlayerList().getPlayers().forEach(player -> {
            grantHardmodeAdvancement(player);
            player.displayClientMessage(message, true);

            player.playNotifySound(
                    SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.AMBIENT,
                    1.0F,
                    0.5F
            );

            RagnarokNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ScreenShakePacket(5.0F, 40)
            );
        });
    }

    private static void grantHardmodeAdvancement(ServerPlayer player) {
        net.minecraft.advancements.Advancement advancement = player.getServer().getAdvancements()
                .getAdvancement(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ragnarok", "hardmode"));
        if (advancement == null) return;

        net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        advancement.getCriteria().keySet().forEach(criterion -> {
            if (!progress.isDone()) {
                player.getAdvancements().award(advancement, criterion);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        DinocampusBubbleEntity.tickSuffocation(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {

            player.getCapability(CuriosCapability.INVENTORY).ifPresent(handler -> {

                handler.findFirstCurio(stack -> stack.getItem() instanceof ItemScorpionNecklace)
                        .ifPresent(slotResult -> {
                            LivingEntity target = event.getEntity();
                            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                        });
            });
        }
    }
    @SubscribeEvent
    public static void onAnimalKilled(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();

        // 殺されたのが動物で、殺したのがプレイヤーの場合
        if (killed instanceof Animal && event.getSource().getEntity() instanceof Player) {
            Player killer = (Player) event.getSource().getEntity();

            // 周囲16ブロック以内のGrootを探す
            AABB searchBox = new AABB(
                    killed.getX() - 16.0D, killed.getY() - 8.0D, killed.getZ() - 16.0D,
                    killed.getX() + 16.0D, killed.getY() + 8.0D, killed.getZ() + 16.0D
            );

            List<Groot> nearbyGroots = killed.level().getEntitiesOfClass(Groot.class, searchBox);

            // 近くのGrootを全て敵対化
            for (Groot groot : nearbyGroots) {
                if (!groot.isAngry()) {
                    groot.onNearbyAnimalKilled(killer);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();

        if (!level.isClientSide && entity instanceof Raider raider) {

            if (raider.getCurrentRaid() != null && raider instanceof Evoker) {

                if (level.random.nextFloat() < 1F) {
                    Magic_Golem golem = new Magic_Golem(RagnarokEntities.MAGIC_GOLEM.get(), level);
                    golem.moveTo(raider.getX(), raider.getY(), raider.getZ(), raider.getYRot(), 0.0F);

                    level.addFreshEntity(golem);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onLeftClick(InputEvent.InteractionKeyMappingTriggered event) {
        // 左クリック（攻撃キー）がトリガーされた時
        if (event.isAttack()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player != null && player.getMainHandItem().getItem() instanceof VoidScythe) {
                // 視線の先が「空気（MISS）」であるか確認
                if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.MISS) {

                    // クライアントの接続ハンドラを通じてパケットを送信
                    // player.connection (LocalPlayer内のフィールド) を使用
                    if (player.connection != null) {
                        player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    }
                }
            }
        }
    }
        /**
         * 会話画面が開いている時、右クリックによる「アイテムの使用（食べる、エッグ使用など）」をブロックする
         */
        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (Minecraft.getInstance().screen instanceof NpcDialogueScreen) {
                event.setCanceled(true);
            }
        }

        /**
         * 会話画面が開いている時、右クリックによる「ブロックへの使用（設置など）」をブロックする
         */
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (Minecraft.getInstance().screen instanceof NpcDialogueScreen) {
                event.setCanceled(true);
            }
        }

        /**
         * 会話画面が開いている時、右クリックによる「エンティティへの使用（NPCへの連続右クリックなど）」をブロックする
         */
        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            if (Minecraft.getInstance().screen instanceof NpcDialogueScreen) {
                event.setCanceled(true);
            }
        }

        /**
         * 会話画面が開いている間、バニラの「使用キー（デフォルトで右クリック）」の入力自体を無効化する
         */
        @SubscribeEvent
        public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (Minecraft.getInstance().screen instanceof NpcDialogueScreen) {
                if (event.isUseItem()) {
                    event.setCanceled(true);
                    event.setSwingHand(false); // 手を振るアニメーションも止める
                }
            }
        }
    @SubscribeEvent
    public static void onCameraSetup(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        float offsetX = ScreenShakeHandler.getCurrentOffsetX();
        float offsetY = ScreenShakeHandler.getCurrentOffsetY();
        event.setYaw(event.getYaw() + offsetX);
        event.setPitch(event.getPitch() + offsetY);
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            ScreenShakeHandler.tick();
        }
    }
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {

            // フルセット判定
            if (GradiusArmorItem.hasFullSet(player)) {
                Item helmet = player.getArmorSlots().iterator().next().getItem(); // クールタイム参照用

                // クールタイム中ではないか確認
                if (!player.getCooldowns().isOnCooldown(helmet)) {

                    // 確率判定（例: 20%の確率で発動）
                    if (player.getRandom().nextFloat() < 0.20F) {

                        // ダメージ無効化
                        event.setCanceled(true);

                        // クールタイム設定（30秒 = 600 ticks）
                        player.getCooldowns().addCooldown(helmet, 1000);

                        // 攻撃してきた敵を吹き飛ばす
                        Entity attacker = event.getSource().getEntity();
                        if (attacker instanceof LivingEntity livingAttacker) {
                            // 強さ（第1引数: 強さ, 第2引数: X方向の差, 第3引数: Z方向の差）
                            double dx = livingAttacker.getX() - player.getX();
                            double dz = livingAttacker.getZ() - player.getZ();

                            // 敵にノックバックを与える (強さ1.5F、少し上に打ち上げる)
                            livingAttacker.knockback(1.5F, -dx, -dz);
                            livingAttacker.setDeltaMovement(
                                    livingAttacker.getDeltaMovement().add(0, 0.3D, 0)
                            );
                            livingAttacker.hurtMarked = true; // クライアントへ移動同期を強制
                        }

                        // 効果音とメッセージ演出
                        player.level().playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F
                        );
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Phase.END（チックの終わり）かつサーバー側でのみ処理を実施
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            Player player = event.player;
            AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);

            if (attr != null) {
                boolean hasBonus = attr.getModifier(GradiusArmorItem.ATTACK_DAMAGE_BONUS_ID) != null;
                boolean isFullSet = GradiusArmorItem.hasFullSet(player);

                if (isFullSet && !hasBonus) {
                    // フルセットかつボーナス未付与なら付与
                    AttributeModifier modifier = new AttributeModifier(
                            GradiusArmorItem.ATTACK_DAMAGE_BONUS_ID,
                            "Gradius Fullset Attack Bonus",
                            3.0D,
                            AttributeModifier.Operation.ADDITION
                    );
                    attr.addTransientModifier(modifier);
                } else if (!isFullSet && hasBonus) {
                    // フルセット崩壊かつボーナス付与済みなら除去
                    attr.removeModifier(GradiusArmorItem.ATTACK_DAMAGE_BONUS_ID);
                }
            }
        }
    }

}
