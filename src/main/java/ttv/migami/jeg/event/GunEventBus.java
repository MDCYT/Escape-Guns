package ttv.migami.jeg.event;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.UnderwaterFirearmItem;
import ttv.migami.jeg.item.attachment.IAttachment;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunEventBus
{
    @SubscribeEvent
    public static void preShoot(GunFireEvent.Pre event)
    {

        Player player = event.getPlayer();
        Level level = event.getPlayer().level;
        ItemStack heldItem = player.getMainHandItem();
        CompoundTag tag = heldItem.getTag();

        if(heldItem.getItem() instanceof GunItem gunItem)
        {
            Gun gun = gunItem.getModifiedGun(heldItem);
            if (!(heldItem.getItem() instanceof UnderwaterFirearmItem) && player.isUnderWater())
            {
                event.setCanceled(true);
            }

            if (heldItem.isDamageableItem() && tag != null) {
                if (heldItem.getDamageValue() == (heldItem.getMaxDamage() - 1)) {
                    level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    event.getPlayer().getCooldowns().addCooldown(event.getStack().getItem(), gun.getGeneral().getRate());
                    event.setCanceled(true);
                }
                //This is the Jam function
                int maxDamage = heldItem.getMaxDamage();
                int currentDamage = heldItem.getDamageValue();
                if (currentDamage >= maxDamage / 1.5) {
                    if (Math.random() >= 0.975) {
                        event.getEntity().playSound(ModSounds.ITEM_PISTOL_COCK.get(), 1.0F, 1.0F);
                        int coolDown = gun.getGeneral().getRate() * 10;
                        if (coolDown > 60) {
                            coolDown = 60;
                        }
                        event.getPlayer().getCooldowns().addCooldown(event.getStack().getItem(), (coolDown));
                        event.setCanceled(true);
                    }
                } else if (tag.getInt("AmmoCount") >= 1) {
                    broken(heldItem, level, player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void postShoot(GunFireEvent.Post event)
    {
        Player player = event.getPlayer();
        Level level = event.getPlayer().level;
        ItemStack heldItem = player.getMainHandItem();
        CompoundTag tag = heldItem.getTag();
        if(heldItem.getItem() instanceof GunItem gunItem)
        {
            Gun gun = gunItem.getModifiedGun(heldItem);
            if (gun.getProjectile().ejectsCasing() && tag != null)
            {
                if (tag.getInt("AmmoCount") >= 1) {
                    //event.getEntity().level.playSound(player, player.blockPosition(), SoundInit.GARAND_PING.get(), SoundSource.MASTER, 3.0F, 1.0F);
                    ejectCasing(level, player);
                }
            }

            if (heldItem.isDamageableItem() && tag != null) {
                if (tag.getInt("AmmoCount") >= 1) {
                    damageGun(heldItem, level, player);
                    damageAttachments(heldItem, level, player);
                }
                if (heldItem.getDamageValue() >= (heldItem.getMaxDamage() / 1.5)) {
                    level.playSound(player, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.75F);
                }
            }
        }
    }

    public static void broken(ItemStack stack, Level level, Player player) {
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        if (currentDamage >= (maxDamage - 2)) {
            level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void damageGun(ItemStack stack, Level level, Player player) {
        if (!player.getAbilities().instabuild) {
            if (stack.isDamageableItem()) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamageValue();
                if (currentDamage >= (maxDamage - 1)) {
                    if (currentDamage >= (maxDamage - 2)) {
                        level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                        //stack.shrink(1);
                    }
                } else {
                    stack.hurtAndBreak(1, player, null);
                }
            }
        }
    }

    public static void damageAttachments(ItemStack stack, Level level, Player player) {
        if (!player.getAbilities().instabuild) {
            if (stack.getItem() instanceof GunItem) {

                //Scope
                ItemStack scopeStack = Gun.getAttachment(IAttachment.Type.SCOPE, stack);
                if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.SCOPE) && scopeStack.isDamageableItem()) {
                    int maxDamage = scopeStack.getMaxDamage();
                    int currentDamage = scopeStack.getDamageValue();
                    if (currentDamage == (maxDamage - 1)) {
                        level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                        Gun.removeAttachment(stack, "Scope");
                    } else {
                        scopeStack.hurtAndBreak(1, player, null);
                    }
                }

                //Barrel
                ItemStack barrelStack = Gun.getAttachment(IAttachment.Type.BARREL, stack);
                if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL) && barrelStack.isDamageableItem()) {
                    int maxDamage = barrelStack.getMaxDamage();
                    int currentDamage = barrelStack.getDamageValue();
                    if (currentDamage == (maxDamage - 1)) {
                        level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                        Gun.removeAttachment(stack, "Barrel");
                    } else {
                        barrelStack.hurtAndBreak(1, player, null);
                    }
                }

                //Stock
                ItemStack stockStack = Gun.getAttachment(IAttachment.Type.STOCK, stack);
                if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK) && stockStack.isDamageableItem()) {
                    int maxDamage = stockStack.getMaxDamage();
                    int currentDamage = stockStack.getDamageValue();
                    if (currentDamage == (maxDamage - 1)) {
                        level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                        Gun.removeAttachment(stack, "Stock");
                    } else {
                        stockStack.hurtAndBreak(1, player, null);
                    }
                }

                //Under Barrel
                ItemStack underBarrelStack = Gun.getAttachment(IAttachment.Type.UNDER_BARREL, stack);
                if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.UNDER_BARREL) && underBarrelStack.isDamageableItem()) {
                    int maxDamage = underBarrelStack.getMaxDamage();
                    int currentDamage = underBarrelStack.getDamageValue();
                    if (currentDamage == (maxDamage - 1)) {
                        level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                        Gun.removeAttachment(stack, "Under_Barrel");
                    } else {
                        underBarrelStack.hurtAndBreak(1, player, null);
                    }
                }
            }
        }
    }

    public static void ejectCasing(Level level, LivingEntity livingEntity)
    {
        Player playerEntity = (Player) livingEntity;
        ItemStack heldItem = playerEntity.getMainHandItem();
        Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);

        Vec3 lookVec = playerEntity.getLookAngle(); //Get the player's look vector
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 forwardVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        double offsetX = rightVec.x * 0.5 + forwardVec.x * 0.5; //Move the particle 0.5 blocks to the right and 0.5 blocks forward
        double offsetY = playerEntity.getEyeHeight() - 0.4; //Move the particle slightly below the player's head
        double offsetZ = rightVec.z * 0.5 + forwardVec.z * 0.5; //Move the particle 0.5 blocks to the right and 0.5 blocks forward

        Vec3 particlePos = playerEntity.getPosition(1).add(offsetX, offsetY, offsetZ); //Add the offsets to the player's position

        ResourceLocation pistolAmmoLocation = ModItems.PISTOL_AMMO.getId();
        ResourceLocation rifleAmmoLocation = ModItems.RIFLE_AMMO.getId();
        ResourceLocation shotgunShellLocation = ModItems.SHOTGUN_SHELL.getId();
        ResourceLocation spectreAmmoLocation = ModItems.SPECTRE_AMMO.getId();
        ResourceLocation projectileLocation = gun.getProjectile().getItem();

        SimpleParticleType casingType = ModParticleTypes.CASING_PARTICLE.get();

        if (projectileLocation != null) {
            if (projectileLocation.equals(pistolAmmoLocation) || projectileLocation.equals(rifleAmmoLocation)) {
                casingType = ModParticleTypes.CASING_PARTICLE.get();
            } else if (projectileLocation.equals(shotgunShellLocation)) {
                casingType = ModParticleTypes.SHELL_PARTICLE.get();
            }
            else if (projectileLocation.equals(spectreAmmoLocation)) {
                casingType = ModParticleTypes.SPECTRE_CASING_PARTICLE.get();
            }
        }

        if (level instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(casingType,
                    particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
        }
    }

}