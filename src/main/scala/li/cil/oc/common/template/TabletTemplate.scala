package li.cil.oc.common.template

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.fml.common.event.FMLInterModComms

object TabletTemplate extends Template {
  override protected val suggestedComponents = Array(
    "BIOS" -> hasComponent("eeprom") _,
    "Keyboard" -> hasComponent("keyboard") _,
    "GraphicsCard" -> ((inventory: IInventory) => Array("graphicsCard1", "graphicsCard2", "graphicsCard3").exists(name => hasComponent(name)(inventory))),
    "OS" -> hasFileSystem _)

  override protected def hostClass = classOf[internal.Tablet]

  def selectTier1(stack: ItemStack) = api.Items.get(stack) == api.Items.get("tabletCase1")

  def selectTier2(stack: ItemStack) = api.Items.get(stack) == api.Items.get("tabletCase2")

  def selectCreative(stack: ItemStack) = api.Items.get(stack) == api.Items.get("tabletCaseCreative")

  def validate(inventory: IInventory): Array[AnyRef] = validateComputer(inventory)

  def assemble(inventory: IInventory): Array[AnyRef] = {
    val items = (1 until inventory.getSizeInventory).map(slot => Option(inventory.getStackInSlot(slot)))
    val data = new TabletData()
    data.tier = ItemUtils.caseTier(inventory.getStackInSlot(0))
    data.container = items.headOption.getOrElse(None)
    data.items = Array(Option(api.Items.get("screen1").createItemStack(1))) ++ items.drop(if (data.tier == Tier.One) 0 else 1).filter(_.isDefined)
    data.energy = Settings.get.bufferTablet
    data.maxEnergy = data.energy
    val stack = api.Items.get("tablet").createItemStack(1)
    data.save(stack)
    val energy = Settings.get.tabletBaseCost + complexity(inventory) * Settings.get.tabletComplexityCost

    Array(stack, double2Double(energy))
  }

  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get("tablet")

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new TabletData(stack)
    val itemName = ItemUtils.caseNameWithTierSuffix("tabletCase", info.tier)
    Array(api.Items.get(itemName).createItemStack(1)) ++ info.items.collect {
      case Some(item) => item
    }.drop(1) // Screen.
  }

  def register() {
    // Tier 1
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Tablet (Tier 1)")
      nbt.setString("select", "li.cil.oc.common.template.TabletTemplate.selectTier1")
      nbt.setString("validate", "li.cil.oc.common.template.TabletTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.TabletTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Tablet")

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Two))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Tier 2
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Tablet (Tier 2)")
      nbt.setString("select", "li.cil.oc.common.template.TabletTemplate.selectTier2")
      nbt.setString("validate", "li.cil.oc.common.template.TabletTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.TabletTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Tablet")

      val containerSlots = new NBTTagList()
      containerSlots.appendTag(Map("tier" -> Tier.Two))
      nbt.setTag("containerSlots", containerSlots)

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Two))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Creative
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Tablet (Creative)")
      nbt.setString("select", "li.cil.oc.common.template.TabletTemplate.selectCreative")
      nbt.setString("validate", "li.cil.oc.common.template.TabletTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.TabletTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Tablet")

      val containerSlots = new NBTTagList()
      containerSlots.appendTag(Map("tier" -> Tier.Three))
      nbt.setTag("containerSlots", containerSlots)

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Three))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Disassembler
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Tablet")
      nbt.setString("select", "li.cil.oc.common.template.TabletTemplate.selectDisassembler")
      nbt.setString("disassemble", "li.cil.oc.common.template.TabletTemplate.disassemble")

      FMLInterModComms.sendMessage("OpenComputers", "registerDisassemblerTemplate", nbt)
    }
  }

  override protected def maxComplexity(inventory: IInventory) = super.maxComplexity(inventory) / 2 + 5

  override protected def caseTier(inventory: IInventory) = ItemUtils.caseTier(inventory.getStackInSlot(0))
}
