package com.niko.ragnarok.client.gui;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Pig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID, value = Dist.CLIENT)
public class PigDialogueTestHandler {

    // ★ 表情ごとの顔グラテクスチャを用意
    private static final ResourceLocation FACE_NORMAL =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/faces/pig_normal.png");
    private static final ResourceLocation FACE_HAPPY =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/faces/pig_happy.png");
    private static final ResourceLocation FACE_SURPRISED =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/gui/faces/pig_surprised.png");

    @SubscribeEvent
    public static void onInteractPig(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() || !(event.getTarget() instanceof Pig) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        event.setCanceled(true);

        Minecraft.getInstance().setScreen(
                new NpcDialogueScreen(buildTestDialogue(), net.minecraft.sounds.SoundEvents.PIG_AMBIENT));
    }

    private static List<NpcDialogueScreen.DialogueLine> buildTestDialogue() {
        return List.of(
                // 通常顔
                new NpcDialogueScreen.DialogueLine("謎の豚", "ブヒッ……ここは一体……？", FACE_NORMAL),
                // 驚き顔
                new NpcDialogueScreen.DialogueLine("謎の豚", "おお、お前は勇者か！", FACE_SURPRISED),
                // 通常顔 + 選択肢
                new NpcDialogueScreen.DialogueLine("謎の豚", "力を貸してくれないか？", FACE_NORMAL, List.of(
                        new NpcDialogueScreen.DialogueChoice("もちろんだ！", screen -> screen.advance()),
                        new NpcDialogueScreen.DialogueChoice("……豚が喋った？", screen -> screen.jumpTo(4))
                )),
                // 笑顔
                new NpcDialogueScreen.DialogueLine("謎の豚", "頼りにしているぞ、勇者よ！", FACE_HAPPY),
                // 驚き顔
                new NpcDialogueScreen.DialogueLine("謎の豚", "……驚くところはそこか。", FACE_SURPRISED)
        );
    }
}