package net.citizensnpcs.npc.entity.nonliving;

import net.citizensnpcs.api.event.NPCPushEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.AbstractEntityController;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_7_R1.Block;
import net.minecraft.server.v1_7_R1.Blocks;
import net.minecraft.server.v1_7_R1.EntityFallingBlock;
import net.minecraft.server.v1_7_R1.World;
import net.minecraft.server.v1_7_R1.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R1.CraftServer;
import org.bukkit.craftbukkit.v1_7_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftFallingSand;
import org.bukkit.craftbukkit.v1_7_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

public class FallingBlockController extends AbstractEntityController {
    public FallingBlockController() {
        super(EntityFallingBlockNPC.class);
    }

    @Override
    protected Entity createEntity(Location at, NPC npc) {
        WorldServer ws = ((CraftWorld) at.getWorld()).getHandle();
        Block id = Blocks.STONE;
        int data = npc.data().get("falling-block-data", 0);
        if (npc.data().has("falling-block-id"))
            id = CraftMagicNumbers.getBlock(Material.getMaterial(npc.data().<String> get("falling-block-id")));
        final EntityFallingBlockNPC handle = new EntityFallingBlockNPC(ws, npc, at.getX(), at.getY(), at.getZ(), id,
                data);
        return handle.getBukkitEntity();
    }

    @Override
    public FallingBlock getBukkitEntity() {
        return (FallingBlock) super.getBukkitEntity();
    }

    public static class EntityFallingBlockNPC extends EntityFallingBlock implements NPCHolder {
        private final CitizensNPC npc;

        public EntityFallingBlockNPC(World world) {
            this(world, null);
        }

        public EntityFallingBlockNPC(World world, NPC npc) {
            super(world);
            this.npc = (CitizensNPC) npc;
        }

        public EntityFallingBlockNPC(World world, NPC npc, double d0, double d1, double d2, Block block, int data) {
            super(world, d0, d1, d2, block, data);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public void collide(net.minecraft.server.v1_7_R1.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public void g(double x, double y, double z) {
            if (npc == null) {
                super.g(x, y, z);
                return;
            }
            if (NPCPushEvent.getHandlerList().getRegisteredListeners().length == 0) {
                if (!npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true))
                    super.g(x, y, z);
                return;
            }
            Vector vector = new Vector(x, y, z);
            NPCPushEvent event = Util.callPushEvent(npc, vector);
            if (!event.isCancelled()) {
                vector = event.getCollisionVector();
                super.g(vector.getX(), vector.getY(), vector.getZ());
            }
            // when another entity collides, this method is called to push the
            // NPC so we prevent it from doing anything if the event is
            // cancelled.
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (bukkitEntity == null && npc != null) {
                bukkitEntity = new FallingBlockNPC(this);
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public void h() {
            if (npc != null) {
                npc.update();
                if (Math.abs(motX) > EPSILON || Math.abs(motY) > EPSILON || Math.abs(motZ) > EPSILON) {
                    motX *= 0.98;
                    motY *= 0.98;
                    motZ *= 0.98;
                    move(motX, motY, motZ);
                }
            } else {
                super.h();
            }
        }

        private static final double EPSILON = 0.001;
    }

    public static class FallingBlockNPC extends CraftFallingSand implements NPCHolder {
        private final CitizensNPC npc;

        public FallingBlockNPC(EntityFallingBlockNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        public void setType(Material material, int data) {
            npc.data().setPersistent("falling-block-id", material.name());
            npc.data().setPersistent("falling-block-data", data);
            if (npc.isSpawned()) {
                npc.despawn();
                npc.spawn(npc.getStoredLocation());
            }
        }
    }
}