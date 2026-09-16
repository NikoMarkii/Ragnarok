package com.niko.ragnarok.client.gui;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * BossDialogueOverlay の登録（MODバス）と、毎tickの更新呼び出し（FORGEバス）。
 * 発火するバスが違う2つのイベントなので、クラスを分けている。
 */
public class BossDialogueClientEvents {

    // ── オーバーレイの登録（MODバス）──
    @Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegisterEvents {
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            // ホットバーより上に重ねて表示する
            event.registerAboveAll("boss_dialogue", BossDialogueOverlay.INSTANCE);
        }
    }

    // ── 毎tickの更新（FORGEバス）──
    @Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID, value = Dist.CLIENT)
    public static class TickEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (Minecraft.getInstance().level == null) {
                return; // タイトル画面等では動かさない
            }
            BossDialogueOverlay.INSTANCE.tick();
        }
    }
}