package myau.inject;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Replace development Mixin accessor casts with equivalent reflective static adapters. */
final class AccessorTransformer {
    static byte[] transform(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null;) {
                AbstractInsnNode next = insn.getNext();
                if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.CHECKCAST
                        && ((TypeInsnNode) insn).desc.startsWith("myau/mixin/IAccessor")) {
                    method.instructions.remove(insn); changed = true;
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (call.owner.startsWith("myau/mixin/IAccessor")) {
                        String name = call.owner.substring("myau/mixin/IAccessor".length());
                        String owner = owner(name);
                        if (owner == null) throw new IllegalStateException("Unknown accessor " + name);
                        call.owner = "myau/access/Accessor" + name;
                        call.desc = "(L" + owner + ";" + call.desc.substring(1);
                        call.setOpcode(Opcodes.INVOKESTATIC); call.itf = false; changed = true;
                    }
                }
                insn = next;
            }
        }
        if (!changed) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer); return writer.toByteArray();
    }
    private static String owner(String n) {
        switch (n) {
            case "Minecraft": return "net/minecraft/client/Minecraft";
            case "Entity": return "net/minecraft/entity/Entity";
            case "EntityLivingBase": return "net/minecraft/entity/EntityLivingBase";
            case "EntityPlayer": return "net/minecraft/entity/player/EntityPlayer";
            case "EntityRenderer": return "net/minecraft/client/renderer/EntityRenderer";
            case "GuiChat": return "net/minecraft/client/gui/GuiChat";
            case "GuiScreen": return "net/minecraft/client/gui/GuiScreen";
            case "ItemSword": return "net/minecraft/item/ItemSword";
            case "KeyBinding": return "net/minecraft/client/settings/KeyBinding";
            case "PlayerControllerMP": return "net/minecraft/client/multiplayer/PlayerControllerMP";
            case "RenderManager": return "net/minecraft/client/renderer/entity/RenderManager";
            case "C03PacketPlayer": return "net/minecraft/network/play/client/C03PacketPlayer";
            case "C0DPacketCloseWindow": return "net/minecraft/network/play/client/C0DPacketCloseWindow";
            default: return null;
        }
    }
}
