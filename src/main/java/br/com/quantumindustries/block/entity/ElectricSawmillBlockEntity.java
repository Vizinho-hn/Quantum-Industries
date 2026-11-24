package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.logistics.MachineItemSlot;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.recipe.MechanicalProcesses;
import br.com.quantumindustries.research.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Single chamber: one input is committed exactly once after forty paid ticks. */
public final class ElectricSawmillBlockEntity extends BlockEntity
    implements EnergyInspectable, MachineStatusInspectable {
  public static final int CAPACITY = 1200, WORK_TICKS = 40, FE_PER_TICK = 4;
  private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, 64, 4);
  private final SimpleContainer inventory =
      new SimpleContainer(8) {
        @Override
        public ItemStack removeItem(int slot, int amount) {
          if (!loading && slot == active && amount > 0) {
            active = -1;
            progress = 0;
            reserved = ItemStack.EMPTY;
          }
          return super.removeItem(slot, amount);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
          if (!loading
              && slot == active
              && !ItemStack.isSameItemSameComponents(getItem(slot), stack)) {
            active = -1;
            progress = 0;
            reserved = ItemStack.EMPTY;
          }
          super.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
          super.setChanged();
          if (loading) ElectricSawmillBlockEntity.this.setChanged();
          else ElectricSawmillBlockEntity.this.changed();
        }
      };
  private int progress, active = -1, next, clientState = 1;
  private boolean loading;
  private final br.com.quantumindustries.energy.TransferBudget transferBudget =
      new br.com.quantumindustries.energy.TransferBudget(64);
  private long progressSyncTime;
  private ItemStack reserved = ItemStack.EMPTY;
  private String owner = "", recipeId = "";

  private final br.com.quantumindustries.energy.MachineEnergyPort energyPort =
      new br.com.quantumindustries.energy.MachineEnergyPort(this, battery, () -> true, () -> false, transferBudget, this::changed);

  public net.neoforged.neoforge.transfer.energy.EnergyHandler energyPort() { return energyPort; }

  private final ResourceHandler<ItemResource> itemPort = new CombinedResourceHandler<>(
      java.util.stream.IntStream.range(0, 8).mapToObj(this::automationSlot).toList());

  private MachineItemSlot automationSlot(int slot) {
    return new MachineItemSlot(this, () -> inventory.getItem(slot),
        stack -> inventory.setItem(slot, stack, true),
        slot < 4 ? this::canInsert : null, 64, before -> changed());
  }

  public ResourceHandler<ItemResource> itemPort() { return itemPort; }

  public ElectricSawmillBlockEntity(BlockPos p, BlockState s) {
    super(ModBlocks.ELECTRIC_SAWMILL_ENTITY.get(), p, s);
  }

  public int energy() {
    return (int) battery.getAmountAsLong();
  }

  public int capacity() {
    return CAPACITY;
  }

  public void invalidateActive(int slot) {
    if (level instanceof ServerLevel && slot == active) {
      progress = 0;
      active = -1;
      reserved = ItemStack.EMPTY;
      changed();
    }
  }

  public int progress() {
    return progress;
  }

  public float visualProgress(float partialTick) {
    return progress
        + (working() && level != null
            ? Math.clamp(level.getGameTime() - progressSyncTime + partialTick, 0, 5)
            : 0);
  }

  public int activeSlot() {
    return active;
  }

  public Container inventory() {
    return inventory;
  }

  public boolean bind(Player p) {
    if (!(level instanceof ServerLevel s)
        || p.level() != level
        || p.distanceToSqr(worldPosition.getCenter()) > 64
        || !WorldResearchData.get(s).knows(p, "electric_wood_processing")) return false;
    if (owner.isEmpty() || !WorldResearchData.get(s).knows(owner, ResearchIds.of("electric_wood_processing"))) {
      owner = p.getUUID().toString();
      changed();
    }
    return true;
  }

  public boolean canInsert(ItemStack stack) {
    if (!(level instanceof ServerLevel s) || owner.isEmpty()) return false;
    var r = MechanicalProcesses.find(s, stack);
    if (r == null
        || !WorldResearchData.get(s).knows(owner, ResearchIds.of("electric_wood_processing")))
      return false;
    for (int i = 0; i < 4; i++) {
      var v = inventory.getItem(i);
      if (v.isEmpty()
          || ItemStack.isSameItemSameComponents(v, stack) && v.getCount() < v.getMaxStackSize())
        return true;
    }
    return false;
  }

  public boolean insert(ItemStack stack) {
    if (!canInsert(stack)) return false;
    for (int pass = 0; pass < 2; pass++)
      for (int i = 0; i < 4; i++) {
        var v = inventory.getItem(i);
        if (pass == 0 && v.isEmpty() || pass == 1 && !v.isEmpty()) continue;
        if (v.isEmpty()) {
          inventory.setItem(i, stack.copyWithCount(1));
          stack.shrink(1);
          changed();
          return true;
        }
        if (ItemStack.isSameItemSameComponents(v, stack) && v.getCount() < v.getMaxStackSize()) {
          v.grow(1);
          stack.shrink(1);
          changed();
          return true;
        }
      }
    return false;
  }

  public ItemStack previewOutput() {
    for (int i = 4; i < 8; i++)
      if (!inventory.getItem(i).isEmpty()) return inventory.getItem(i).copy();
    return ItemStack.EMPTY;
  }

  public ItemStack extractOutput(int max) {
    if (!(level instanceof ServerLevel) || max <= 0) return ItemStack.EMPTY;
    for (int i = 4; i < 8; i++)
      if (!inventory.getItem(i).isEmpty()) {
        var result = inventory.removeItem(i, max);
        changed();
        return result;
      }
    return ItemStack.EMPTY;
  }

  public ItemStack result(int slot) {
    if (!(level instanceof ServerLevel s) || slot < 0) return ItemStack.EMPTY;
    var r = MechanicalProcesses.find(s, inventory.getItem(slot));
    return r == null ? ItemStack.EMPTY : r.value().result().copyWithCount(8);
  }

  private boolean room(ItemStack r) {
    if (r.isEmpty()) return false;
    int free = 0;
    for (int i = 4; i < 8; i++) {
      var v = inventory.getItem(i);
      if (v.isEmpty()) free += r.getMaxStackSize();
      else if (ItemStack.isSameItemSameComponents(v, r)) free += v.getMaxStackSize() - v.getCount();
    }
    return free >= r.getCount();
  }

  public int stateCode() {
    if (working()) return 0;
    if (!(level instanceof ServerLevel server)) return clientState;
    if (!WorldResearchData.get(server).knows(owner, ResearchIds.of("electric_wood_processing")))
      return 5;
    boolean any = false, valid = false;
    for (int i = 0; i < 4; i++) {
      any |= !inventory.getItem(i).isEmpty();
      var r = result(i);
      if (!r.isEmpty()) {
        valid = true;
        if (room(r)) return energy() < 4 ? 2 : 1;
      }
    }
    return valid ? 3 : any ? 4 : 1;
  }

  public int transportFlags() {
    if (level == null) return 0;
    int flags = 0;
    for (var side : Direction.values()) {
      var pos = worldPosition.relative(side);
      if (!level.hasChunkAt(pos)
          || !(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) continue;
      var m = tube.connectionMode(side.getOpposite());
      if (m.allowsOutput()) flags |= 1;
      if (m == br.com.quantumindustries.logistics.ConnectionMode.PULL) flags |= 2;
    }
    return flags;
  }

  public boolean working() {
    return getBlockState().getValueOrElse(BlockStateProperties.LIT, false);
  }

  public void tick() {
    if (!(level instanceof ServerLevel s)) return;
    if (active >= 0
        && (!ItemStack.isSameItemSameComponents(reserved, inventory.getItem(active))
            || inventory.getItem(active).isEmpty())) {
      active = -1;
      progress = 0;
      reserved = ItemStack.EMPTY;
    }
    if (active >= 0) {
      var current = MechanicalProcesses.find(s, reserved);
      if (current == null || !current.id().identifier().toString().equals(recipeId)) {
        active = -1;
        progress = 0;
        reserved = ItemStack.EMPTY;
      }
    }
    if (active < 0)
      for (int k = 0; k < 4; k++) {
        int i = (next + k) % 4;
        var r = result(i);
        if (room(r)) {
          active = i;
          reserved = inventory.getItem(i).copyWithCount(1);
          recipeId = MechanicalProcesses.find(s, reserved).id().identifier().toString();
          changed();
          break;
        }
      }
    boolean run =
        active >= 0
            && room(result(active))
            && WorldResearchData.get(s).knows(owner, ResearchIds.of("electric_wood_processing"))
            && energy() >= 4;
    if (run && s.getGameTime() % 20 == 0)
      s.playSound(
          null,
          worldPosition,
          net.minecraft.sounds.SoundEvents.GRINDSTONE_USE,
          net.minecraft.sounds.SoundSource.BLOCKS,
          .25f,
          1.2f);
    if (run) {
      try (var tx = Transaction.openRoot()) {
        if (battery.extract(4, tx) != 4) return;
        tx.commit();
      }
      if (++progress == 40) {
        var r = result(active);
        for (int pass = 0; pass < 2; pass++)
          for (int i = 4; i < 8 && !r.isEmpty(); i++) {
            var v = inventory.getItem(i);
            if (pass == 0 && v.isEmpty() || pass == 1 && !v.isEmpty()) continue;
            if (v.isEmpty()) {
              inventory.setItem(i, r.copy());
              r.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(v, r)) {
              int n = Math.min(r.getCount(), v.getMaxStackSize() - v.getCount());
              v.grow(n);
              r.shrink(n);
            }
          }
        int completed = active;
        active = -1;
        inventory.removeItem(completed, 1);
        next = (completed + 1) % 4;
        progress = 0;
        reserved = ItemStack.EMPTY;
        recipeId = "";
      }
      setChanged();
    }
    if (working() != run) {
      s.setBlock(
          worldPosition, getBlockState().setValue(BlockStateProperties.LIT, run), Block.UPDATE_ALL);
      changed();
    }
    if (run && s.getGameTime() % 5 == 0) changed();
  }

  public int receiveEnergy(int max) {
    if (!(level instanceof ServerLevel) || max <= 0) return 0;
    try (var tx = Transaction.openRoot()) {
      int accepted = energyPort.insert(max, tx);
      if (accepted > 0) tx.commit();
      return accepted;
    }
  }

  private void changed() {
    setChanged();
    if (level != null)
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
  }

  public Component inspectTitle() {
    return Component.translatable("block.quantumindustries.electric_sawmill");
  }

  public Component inspectDetail() {
    return Component.translatable(
        "gui.quantumindustries.electric_sawmill.inspect",
        active >= 0 ? inventory.getItem(active).getHoverName() : Component.literal("-"),
        Component.translatable("gui.quantumindustries.electric_sawmill.state." + stateCode()),
        progress * 100 / 40);
  }

  public Component statusTitle() {
    return inspectTitle();
  }

  public Component statusDetail() {
    return inspectDetail();
  }

  public State statusState() {
    return working() ? State.WORKING : active >= 0 ? State.BLOCKED : State.IDLE;
  }

  public float statusProgress() {
    return progress / 40f;
  }

  @Override
  protected void saveAdditional(ValueOutput out) {
    super.saveAdditional(out);
    out.putInt("stored_fe", energy());
    out.putInt("progress", progress);
    out.putInt("active", active);
    out.putInt("next", next);
    out.putString("owner", owner);
    out.putString("recipe", recipeId);
    if (!reserved.isEmpty()) out.store("reserved", ItemStack.CODEC, reserved);
    for (int i = 0; i < 8; i++)
      if (!inventory.getItem(i).isEmpty())
        out.store("slot_" + i, ItemStack.CODEC, inventory.getItem(i));
  }

  @Override
  protected void loadAdditional(ValueInput in) {
    super.loadAdditional(in);
    loading = true;
    progressSyncTime = in.getLongOr("progress_sync_time", level == null ? 0 : level.getGameTime());
    clientState = Math.clamp(in.getIntOr("inspection_state", 1), 0, 5);
    battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, 1200));
    progress = Math.clamp(in.getIntOr("progress", 0), 0, 39);
    active = Math.clamp(in.getIntOr("active", -1), -1, 3);
    next = Math.clamp(in.getIntOr("next", 0), 0, 3);
    owner = in.getStringOr("owner", "");
    recipeId = in.getStringOr("recipe", "");
    reserved = in.read("reserved", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    for (int i = 0; i < 8; i++)
      inventory.setItem(i, in.read("slot_" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
    loading = false;
  }

  @Override
  public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider r) {
    var tag = saveCustomOnly(r);
    tag.putInt("inspection_state", stateCode());
    tag.putLong("progress_sync_time", level == null ? 0 : level.getGameTime());
    return tag;
  }

  @Override
  public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
    return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public void preRemoveSideEffects(BlockPos p, BlockState s) {
    super.preRemoveSideEffects(p, s);
    if (level instanceof ServerLevel l)
      for (int i = 0; i < 8; i++) {
        Block.popResource(l, p, inventory.removeItemNoUpdate(i));
      }
  }
}
