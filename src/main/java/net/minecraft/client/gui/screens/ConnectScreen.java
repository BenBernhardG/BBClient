package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import net.bernhardbmx.Variables;
import net.bernhardbmx.discord.RPC;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectScreen extends Screen {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    static final Logger LOGGER = LogManager.getLogger();
    private static final long NARRATION_DELAY_MS = 2000L;
    public static final Component UNKNOWN_HOST_MESSAGE = new TranslatableComponent("disconnect.genericReason", new TranslatableComponent("disconnect.unknownHost"));
    @Nullable
    volatile Connection connection;
    volatile boolean aborted;
    final Screen parent;
    private Component status = new TranslatableComponent("connect.connecting");
    private long lastNarration = -1L;

    private ConnectScreen(Screen pParent) {
        super(NarratorChatListener.NO_TITLE);
        this.parent = pParent;
    }

    public static void startConnecting(Screen p_169268_, Minecraft p_169269_, ServerAddress p_169270_, @Nullable ServerData p_169271_) {
        ConnectScreen connectscreen = new ConnectScreen(p_169268_);
        p_169269_.clearLevel();
        p_169269_.prepareForMultiplayer();
        p_169269_.setCurrentServer(p_169271_);
        p_169269_.setScreen(connectscreen);
        connectscreen.connect(p_169269_, p_169270_);
        // Discord
        RPC.instance().update("Multiplayer", "Playing on " + p_169270_.getHost() + (p_169270_.getPort() == 25565 ? "" : ":" + + p_169270_.getPort()), "", "");
        Variables.server = true;
    }

    private void connect(final Minecraft p_169265_, final ServerAddress p_169266_) {
        LOGGER.info("Connecting to {}, {}", p_169266_.getHost(), p_169266_.getPort());
        Thread thread = new Thread("Server Connector #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            public void run() {
                InetSocketAddress inetsocketaddress = null;

                try {
                    if (ConnectScreen.this.aborted) {
                        return;
                    }

                    Optional<InetSocketAddress> optional = ServerNameResolver.DEFAULT.resolveAddress(p_169266_).map(ResolvedServerAddress::asInetSocketAddress);

                    if (ConnectScreen.this.aborted) {
                        return;
                    }

                    if (!optional.isPresent()) {
                        p_169265_.execute(() ->
                        {
                            p_169265_.setScreen(new DisconnectedScreen(ConnectScreen.this.parent, CommonComponents.CONNECT_FAILED, ConnectScreen.UNKNOWN_HOST_MESSAGE));
                        });
                        return;
                    }

                    inetsocketaddress = optional.get();
                    ConnectScreen.this.connection = Connection.connectToServer(inetsocketaddress, p_169265_.options.useNativeTransport());
                    ConnectScreen.this.connection.setListener(new ClientHandshakePacketListenerImpl(ConnectScreen.this.connection, p_169265_, ConnectScreen.this.parent, ConnectScreen.this::updateStatus));
                    ConnectScreen.this.connection.send(new ClientIntentionPacket(inetsocketaddress.getHostName(), inetsocketaddress.getPort(), ConnectionProtocol.LOGIN));
                    ConnectScreen.this.connection.send(new ServerboundHelloPacket(p_169265_.getUser().getGameProfile()));
                } catch (Exception exception2) {
                    if (ConnectScreen.this.aborted) {
                        return;
                    }

                    Throwable throwable = exception2.getCause();
                    Exception exception;

                    if (throwable instanceof Exception exception1) {
                        exception = exception1;
                    } else {
                        exception = exception2;
                    }

                    ConnectScreen.LOGGER.error("Couldn't connect to server", exception2);
                    String s = inetsocketaddress == null ? exception.getMessage() : exception.getMessage().replaceAll(inetsocketaddress.getHostName() + ":" + inetsocketaddress.getPort(), "").replaceAll(inetsocketaddress.toString(), "");
                    p_169265_.execute(() ->
                    {
                        p_169265_.setScreen(new DisconnectedScreen(ConnectScreen.this.parent, CommonComponents.CONNECT_FAILED, new TranslatableComponent("disconnect.genericReason", s)));
                    });
                }
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    private void updateStatus(Component pStatus) {
        this.status = pStatus;
    }

    public void tick() {
        if (this.connection != null) {
            if (this.connection.isConnected()) {
                this.connection.tick();
            } else {
                this.connection.handleDisconnection();
            }
        }
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    protected void init() {
        this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20, CommonComponents.GUI_CANCEL, (p_95705_) ->
        {
            this.aborted = true;

            if (this.connection != null) {
                this.connection.disconnect(new TranslatableComponent("connect.aborted"));
            }

            this.minecraft.setScreen(this.parent);
        }));
    }

    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        long i = Util.getMillis();

        if (i - this.lastNarration > 2000L) {
            this.lastNarration = i;
            NarratorChatListener.INSTANCE.sayNow(new TranslatableComponent("narrator.joining"));
        }

        drawCenteredString(pPoseStack, this.font, this.status, this.width / 2, this.height / 2 - 50, 16777215);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }
}
