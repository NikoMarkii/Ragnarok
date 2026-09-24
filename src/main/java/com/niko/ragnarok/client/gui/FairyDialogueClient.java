package com.niko.ragnarok.client.gui;

import com.niko.ragnarok.entity.geckolib_entity.Costom.Fairy.FairyEntity;
import com.niko.ragnarok.network.DialogueStatePacket;
import com.niko.ragnarok.network.TradeRequestPacket;
import com.niko.ragnarok.network.RagnarokNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public final class FairyDialogueClient {
    private FairyDialogueClient() {
    }

    public static void open(FairyEntity fairy, Player player) {
        RagnarokNetwork.CHANNEL.sendToServer(new DialogueStatePacket(fairy.getId(), true));

        List<NpcDialogueScreen.DialogueLine> lines = buildFairyDialogue(fairy);

        Minecraft.getInstance().setScreen(new NpcDialogueScreen(lines, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM,
                fairy, true, () -> RagnarokNetwork.CHANNEL.sendToServer(
                        new DialogueStatePacket(fairy.getId(), false))));
    }

    private static List<NpcDialogueScreen.DialogueLine> buildFairyDialogue(FairyEntity fairy) {
        return List.of(
                // [0] 挨拶
                NpcDialogueScreen.DialogueLine.translated(
                        "ragnarok.dialogue.fairy.speaker", "ragnarok.dialogue.fairy.greeting", null),

                // [1] PigDialogueTestHandler と同じ選択肢形式
                NpcDialogueScreen.DialogueLine.translated(
                        "ragnarok.dialogue.fairy.speaker", "ragnarok.dialogue.fairy.question", null, List.of(
                                NpcDialogueScreen.DialogueChoice.toTrade(() -> {
                                    RagnarokNetwork.CHANNEL.sendToServer(new TradeRequestPacket(fairy.getId()));
                                }),
                                NpcDialogueScreen.DialogueChoice.translated(
                                        "ragnarok.dialogue.choice.chat", screen -> screen.jumpTo(2)),
                                NpcDialogueScreen.DialogueChoice.translated(
                                        "ragnarok.dialogue.choice.end", NpcDialogueScreen::startClosing)
                        )),

                // [2] 雑談ルート
                NpcDialogueScreen.DialogueLine.translatedRandom(
                        "ragnarok.dialogue.fairy.speaker",
                        List.of(
                                "ragnarok.dialogue.fairy.chat_1",
                                "ragnarok.dialogue.fairy.chat_2",
                                "ragnarok.dialogue.fairy.chat_3",
                                "ragnarok.dialogue.fairy.chat_4"
                        ),
                        null)
        );
    }
}
