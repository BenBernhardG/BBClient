package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bernhardbmx.Variables;
import net.bernhardbmx.discord.RPC;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class DisconnectedScreen extends Screen {
    private final Component reason;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    private final Screen parent;
    private int textHeight;

    public DisconnectedScreen(Screen pParent, Component pTitle, Component pReason) {
        super(pTitle);
        // Discord
        RPC.instance().update("Server Menu", "Looking at Teh servers", "", "");
        Variables.server = false;

        this.parent = pParent;
        this.reason = pReason;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    protected void init() {
        this.message = MultiLineLabel.create(this.font, this.reason, this.width - 50);
        this.textHeight = this.message.getLineCount() * 9;
        this.addRenderableWidget(new Button(this.width / 2 - 100, Math.min(this.height / 2 + this.textHeight / 2 + 9, this.height - 30), 200, 20, new TranslatableComponent("gui.toMenu"), (p_96002_) ->
        {
            this.minecraft.setScreen(this.parent);
        }));
    }

    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, this.height / 2 - this.textHeight / 2 - 9 * 2, 11184810);
        this.message.renderCentered(pPoseStack, this.width / 2, this.height / 2 - this.textHeight / 2);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }
}
