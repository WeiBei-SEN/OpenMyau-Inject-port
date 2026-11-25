package myau.inject;

import myau.Myau;
import myau.access.AccessorMinecraft;
import myau.event.EventManager;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import org.lwjgl.input.Keyboard;
import myau.events.HitBlockEvent;
import myau.events.LeftClickMouseEvent;
import myau.events.RightClickMouseEvent;
import myau.events.Render2DEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.modules.NoHitDelay;
import myau.module.modules.NoRotate;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.client.Minecraft;
import myau.util.KeyBindUtil;

public final class Callbacks {
    public static final String OWNER = "myau/inject/Callbacks";
    private static float overlayPartialTicks;
    private static float worldPartialTicks;
    private static int lastKey;
    private static boolean lastPressed;
    private static boolean lastSynthetic;
    private Callbacks() {
    }
    public static void tickPre() {
        try {

            Bootstrap.tick();
            NativeBridge.flushTransformLog();
            if (!Bootstrap.isStarted() || !GameState.inGame()) {
                return;
            }
            tickSequence++;
            EventManager.call(new TickEvent(EventType.PRE));
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
        }
    }

    private static long tickSequence;
    private static long lastInteractTick = -1L;

    public static void render2DPre(float partialTicks) {
        overlayPartialTicks = partialTicks;
    }
    public static void render2DPost() {
        try {
            if (!Bootstrap.isStarted() || !GameState.inGame()) {
                return;
            }
            EventManager.call(new Render2DEvent(overlayPartialTicks));
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
        }
    }
    public static boolean rightClickMouse() {
        try {
            if (!Bootstrap.isStarted() || !GameState.inGame()) {
                return false;
            }
            RightClickMouseEvent event = new RightClickMouseEvent();
            EventManager.call(event);
            return event.isCancelled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean sendClickBlockToController() {
        try {
            if (!Bootstrap.isStarted()) {
                return false;
            }
            HitBlockEvent event = new HitBlockEvent();
            EventManager.call(event);
            if (!event.isCancelled()) {
                return false;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.playerController != null) {
                mc.playerController.resetBlockRemoving();
            }
            return true;
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return false;
        }
    }
    public static boolean clickMouse() {
        try {
            if (!Bootstrap.isStarted()) {
                return false;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (Myau.moduleManager != null
                    && Myau.moduleManager.modules.get(NoHitDelay.class).isEnabled()) {
                AccessorMinecraft.setLeftClickCounter(mc, 0);
            }
            LeftClickMouseEvent event = new LeftClickMouseEvent();
            EventManager.call(event);
            return event.isCancelled();
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return false;
        }
    }

    public static boolean keyBindStatePre(int key, boolean pressed) {
        lastKey = key; lastPressed = pressed; return false;
    }
    public static void keyBindStatePost() {
        if (Bootstrap.isStarted() && lastPressed && Minecraft.getMinecraft().currentScreen == null)
            EventManager.call(new KeyEvent(lastKey));
    }
    public static void resized() {
        if (Bootstrap.isStarted()) EventManager.call(new myau.events.ResizeEvent());
    }
    public static void render3DPre(float partialTicks) {
        worldPartialTicks = partialTicks;
    }
    public static void render3DPost() {
        try {
            if (!Bootstrap.isStarted() || !GameState.inGame()) {
                return;
            }
            EventManager.call(new Render3DEvent(worldPartialTicks));
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
        }
    }
    public static void loadWorld() {
        try {
            if (!Bootstrap.isStarted()) {
                return;
            }
            EventManager.call(new LoadWorldEvent());
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
        }
    }
    public static boolean packetReceive(Object manager, Object raw) {
        try {
            if (!Bootstrap.isStarted()) {
                return false;
            }
            Packet<?> packet = (Packet<?>) raw;
            if (Myau.delayManager != null
                    && Myau.delayManager.shouldDelay((Packet<INetHandlerPlayClient>) packet)) {
                return true;
            }
            PacketEvent event = new PacketEvent(EventType.RECEIVE, packet);
            EventManager.call(event);
            if (event.isCancelled()) {
                return true;
            }
            return false;
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return false;
        }
    }
    public static boolean packetSend(Object manager, Object raw) {
        try {
            if (!Bootstrap.isStarted()) {
                return false;
            }
            Packet<?> packet = (Packet<?>) raw;
            PacketEvent event = new PacketEvent(EventType.SEND, packet);
            EventManager.call(event);
            if (event.isCancelled()) {
                return true;
            }
            return handOffToManagers(packet);
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return false;
        }
    }
    public static boolean packetSendWithListeners(Object manager, Object raw) {
        try {
            if (!Bootstrap.isStarted()) {
                return false;
            }
            Packet<?> packet = (Packet<?>) raw;
            return handOffToManagers(packet);
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return false;
        }
    }

    private static boolean handOffToManagers(Packet<?> packet) {
        if (Myau.playerStateManager == null || Myau.blinkManager == null
                || Myau.lagManager == null || Myau.lagManager.isFlushing()) {
            return false;
        }
        Myau.playerStateManager.handlePacket(packet);
        if (Myau.blinkManager.isBlinking() && Myau.blinkManager.offerPacket(packet)) {
            return true;
        }
        return Myau.lagManager.handlePacket(packet);
    }
    public static void tickPost() {
        try {
            if (!Bootstrap.isStarted() || !GameState.inGame()) {
                return;
            }
            EventManager.call(new TickEvent(EventType.POST));
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
        }
    }
}
