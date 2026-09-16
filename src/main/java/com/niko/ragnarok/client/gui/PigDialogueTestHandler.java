package com.niko.ragnarok.client.gui;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Pig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * テスト用：豚を右クリックすると NpcDialogueScreen を開く。
 * GUIはクライアント側でしか開けないので、クライアント側イベントとして登録している。
 */
@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID, value = Dist.CLIENT)
public class PigDialogueTestHandler {

    @SubscribeEvent
    public static void onInteractPig(PlayerInteractEvent.EntityInteract event) {
        // クライアント側でのみ反応する（このイベント自体は両側で発火するため）
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof Pig)) {
            return;
        }
        // メインハンド／オフハンドの両方で2重に開かないようにする
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        // バニラの豚インタラクション（餌やり・騎乗など）を抑制する
        event.setCanceled(true);

        Minecraft.getInstance().setScreen(
                new NpcDialogueScreen(buildTestDialogue(), net.minecraft.sounds.SoundEvents.PIG_AMBIENT));
    }

    private static List<NpcDialogueScreen.DialogueLine> buildTestDialogue() {
        return List.of(
                new NpcDialogueScreen.DialogueLine("謎の豚", "ブヒッ……ここは一体……？"),
                new NpcDialogueScreen.DialogueLine("謎の豚", "おお、お前は勇者か！"),
                new NpcDialogueScreen.DialogueLine("謎の豚", "力を貸してくれないか？", List.of(
                        new NpcDialogueScreen.DialogueChoice("もちろんだ！",
                                screen -> screen.advance()),
                        new NpcDialogueScreen.DialogueChoice("……豚が喋った？",
                                screen -> screen.jumpTo(4))
                )),
                new NpcDialogueScreen.DialogueLine("謎の豚", "頼りにしているぞ、勇者よ！"),
                new NpcDialogueScreen.DialogueLine("謎の豚", "……驚くところはそこか。")
        );
    }
}