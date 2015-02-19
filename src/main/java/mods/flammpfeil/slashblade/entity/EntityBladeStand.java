package mods.flammpfeil.slashblade.entity;

import mods.flammpfeil.slashblade.ItemSlashBlade;
import mods.flammpfeil.slashblade.ItemSlashBladeWrapper;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.stats.AchievementList;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Created by Furia on 14/08/15.
 */
public class EntityBladeStand extends Entity {
    public EntityBladeStand(World p_i1582_1_) {
        super(p_i1582_1_);
        this.preventEntitySpawning = true;
        this.setSize(1.0F, 1.0F);
        this.yOffset = this.height / 2.0F;
    }

    public EntityBladeStand(World p_i1582_1_, double x, double y, double z, ItemStack blade) {
        this(p_i1582_1_);
        this.setStandType(-1);
        this.setPositionAndRotation(x,y,z, 180.0f * (this.rand.nextFloat() * 2.0f - 1.0f),this.rotationPitch);
        this.setBlade(blade);
    }

    @Override
    protected void entityInit() {
        //this.dataWatcher.addObject(WatchIndexBlade, SlashBlade.getCustomBlade(SlashBlade.modid,"flammpfeil.slashblade.named.muramasa"));
        this.dataWatcher.addObjectByDataType(WatchIndexBlade, 5); //ItemStack
        this.dataWatcher.addObject(WatchIndexStandType, 0);
        this.dataWatcher.addObject(WatchIndexFlipState, 0);
    }


    static final int WatchIndexFlipState = 10;
    public int getFlip() {
        return this.dataWatcher.getWatchableObjectInt(WatchIndexFlipState);
    }
    public void setFlip(int value) {
        if(hasBlade() && getBlade().getItem() instanceof ItemSlashBladeWrapper && !ItemSlashBladeWrapper.hasWrapedItem(getBlade())){
            if(2 <= value)
                value = 0;
        }
        this.dataWatcher.updateObject(WatchIndexFlipState,value);
    }
    public void doFlip(){
        setFlip(Math.abs((getFlip() + 1) % 4));
    }

    static final int WatchIndexStandType = 9;
    public int getStandType(){
        return this.dataWatcher.getWatchableObjectInt(WatchIndexStandType);
    }
    public void setStandType(int value){
        this.dataWatcher.updateObject(WatchIndexStandType,value);
    }
    public static int getType(EntityBladeStand e){
        switch(e.getStandType()){
            case -1:
                return -1;
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return -1;
            default:
                return 2;
        }
    }

    static final int WatchIndexBlade = 8;
    public ItemStack getBlade(){
        return this.dataWatcher.getWatchableObjectItemStack(WatchIndexBlade);
    }
    public void setBlade(ItemStack blade){
        if(blade != null && blade.getItem() instanceof ItemSlashBladeWrapper && !ItemSlashBladeWrapper.hasWrapedItem(blade)){
            if(2 <= getFlip())
                setFlip(0);
        }

        if(blade != null && blade.getItem() instanceof ItemSlashBlade){
            NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(blade);
            ItemSlashBlade.PrevExp.remove(tag);
        }

        this.dataWatcher.updateObject(WatchIndexBlade,blade);
    }
    public boolean hasBlade(){
        return getBlade() != null;
    }


    static final String SaveKeyBlade = "Blade";
    static final String SaveKeyStandType = "StandType";
    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {

        if(p_70037_1_.hasKey(SaveKeyStandType)){
            int type = p_70037_1_.getInteger(SaveKeyStandType);
            this.setStandType(type);
        }

        if(p_70037_1_.hasKey(SaveKeyBlade)){
            NBTTagCompound tag = p_70037_1_.getCompoundTag(SaveKeyBlade);
            ItemStack blade = ItemStack.loadItemStackFromNBT(tag);

            this.setBlade(blade);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {

        ItemStack blade = getBlade();
        if(blade != null){
            NBTTagCompound tag = new NBTTagCompound();
            blade.writeToNBT(tag);

            p_70014_1_.setTag(SaveKeyBlade,tag);
        }

        {
            int type = this.getStandType();
            p_70014_1_.setInteger(SaveKeyStandType,type);
        }
    }

    @Override
    public void onUpdate() {

        if(SlashBladeHooks.onEntityBladeStandUpdateHooks(this)){
            return;
        }

        super.onUpdate();


        this.motionX = 0;
        this.motionZ = 0;


        if(hasBlade()){
            if (this.posY > 0.0D)
            {
                this.motionY = -0.1D;
            }
            else if(this.posY < -0.5){
                this.motionY = 1.0f;
            }
            else
            {
                this.motionY = 0.0;
            }
        }else{
            this.motionY = -0.1D;
        }

        Block block = this.worldObj.getBlock((int)this.posX,(int)this.posY,(int)this.posZ);
        if(!block.isAir(this.worldObj,(int)this.posX,(int)this.posY,(int)this.posZ)
                && block.getBlockHardness(this.worldObj,(int)this.posX,(int)this.posY,(int)this.posZ) < 0){

            this.setPosition(this.posX,this.posY+1.5,this.posZ);
        }

        this.moveEntity(this.motionX, this.motionY, this.motionZ);

        if(!hasBlade() && posY < -10){
            this.setDead();
        }

        if(getType(this) < 0 && !this.hasBlade() && 200 < this.ticksExisted){
            this.setDead();
        }
    }

    public boolean setStandBlade(Entity e){

        if(e instanceof EntityPlayer){

            EntityPlayer p = (EntityPlayer)e;

            ItemStack stack = p.getHeldItem();
            if(stack == null && this.hasBlade()){

                AchievementList.triggerCraftingAchievement(this.getBlade(), p);

                p.setCurrentItemOrArmor(0, this.getBlade()); 
                this.setBlade(null);

                if(getType(this) == -1)
                    this.setDead();

                return true;

            }else if(stack != null
                    && stack.getItem() instanceof ItemSlashBlade
                    && !this.hasBlade()){

                this.setBlade(stack);

                p.setCurrentItemOrArmor(0,null);

                return true;
            }
        }

        return false;
    }


    @Override
    public boolean hitByEntity(Entity p_85031_1_) {

        if(setStandBlade(p_85031_1_))
            return true;

        if(!this.hasBlade()){
            if(p_85031_1_.isSneaking()){

                if(!p_85031_1_.worldObj.isRemote)
                    this.setDead();

                return true;
            }
        }

        return super.hitByEntity(p_85031_1_);
    }

    @Override
    public boolean interactFirst(EntityPlayer p_130002_1_) {

        if(p_130002_1_.isSneaking()){
            doFlip();
            return true;
        }

        if(setStandBlade(p_130002_1_))
            return true;

        return super.interactFirst(p_130002_1_);
    }

    @Override
    protected boolean canTriggerWalking() {
        return true;
    }
    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    public boolean canBeCollidedWith()
    {
        return true;
    }

    public Random getRand(){
        return this.rand;
    }
}