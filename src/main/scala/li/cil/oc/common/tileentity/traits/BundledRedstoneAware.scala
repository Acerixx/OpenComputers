package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.integration.Mods
import li.cil.oc.util.ExtendedNBT._
import net.minecraftforge.fml.common.Optional

/* TODO RedLogic
import mods.immibis.redlogic.api.wiring.IBundledEmitter
import mods.immibis.redlogic.api.wiring.IBundledUpdatable
import mods.immibis.redlogic.api.wiring.IInsulatedRedstoneWire
*/
/* TODO Project Red
import mrtjp.projectred.api.IBundledTile
import mrtjp.projectred.api.ProjectRedAPI
*/

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagIntArray
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT

/* TODO MFR
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNetworkContainer
*/

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mrtjp.projectred.api.IBundledTile", modid = Mods.IDs.ProjectRedTransmission)
))
trait BundledRedstoneAware extends RedstoneAware /* with IBundledEmitter with IBundledUpdatable with IBundledTile TODO RedLogic, Project Red, MFR */ {

  protected[tileentity] val _bundledInput = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _rednetInput = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _bundledOutput = Array.fill(6)(Array.fill(16)(0))

  // ----------------------------------------------------------------------- //

  override def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
      if (!value) {
        for (i <- 0 until _bundledOutput.length) {
          for (j <- 0 until _bundledOutput(i).length) {
            _bundledOutput(i)(j) = 0
          }
        }
      }
    }
    super.isOutputEnabled_=(value)
  }

  def bundledInput(side: EnumFacing, color: Int) =
    math.max(_bundledInput(side.ordinal())(color), _rednetInput(side.ordinal())(color))

  def rednetInput(side: EnumFacing, color: Int, value: Int): Unit = {
    val oldValue = _rednetInput(side.ordinal())(color)
    if (oldValue != value) {
      if (oldValue != -1) {
        onRedstoneInputChanged(side, oldValue, value)
      }
      _rednetInput(side.ordinal())(color) = value
    }
  }

  def bundledOutput(side: EnumFacing) = _bundledOutput(toLocal(side).ordinal())

  def bundledOutput(side: EnumFacing, color: Int): Int = bundledOutput(side)(color)

  def bundledOutput(side: EnumFacing, color: Int, value: Int): Unit = if (value != bundledOutput(side, color)) {
    _bundledOutput(toLocal(side).ordinal())(color) = value

    /* TODO MFR
    if (Mods.MineFactoryReloaded.isAvailable) {
      val blockPos = BlockPosition(x, y, z).offset(side)
      world.getBlock(blockPos) match {
        case block: IRedNetNetworkContainer => block.updateNetwork(world, blockPos.x, blockPos.y, blockPos.z, side.getOpposite)
        case _ =>
      }
    }
    */

    onRedstoneOutputChanged(side)
  }

  // ----------------------------------------------------------------------- //

  override protected def updateRedstoneInput(side: EnumFacing) {
    super.updateRedstoneInput(side)
    val ownBundledInput = _bundledInput(side.ordinal())
    val newBundledInput = computeBundledInput(side)
    val oldMaxValue = ownBundledInput.max
    var changed = false
    if (newBundledInput != null) for (color <- 0 until 16) {
      changed = changed || (ownBundledInput(color) >= 0 && ownBundledInput(color) != newBundledInput(color))
      ownBundledInput(color) = newBundledInput(color)
    }
    else for (color <- 0 until 16) {
      changed = changed || ownBundledInput(color) > 0
      ownBundledInput(color) = 0
    }
    if (changed) {
      onRedstoneInputChanged(side, oldMaxValue, ownBundledInput.max)
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)

    nbt.getTagList(Settings.namespace + "rs.bundledInput", NBT.TAG_INT_ARRAY).toArray[NBTTagIntArray].
      map(_.getIntArray).zipWithIndex.foreach {
      case (input, index) if index < _bundledInput.length =>
        val safeLength = input.length min _bundledInput(index).length
        input.copyToArray(_bundledInput(index), 0, safeLength)
      case _ =>
    }
    nbt.getTagList(Settings.namespace + "rs.bundledOutput", NBT.TAG_INT_ARRAY).toArray[NBTTagIntArray].
      map(_.getIntArray).zipWithIndex.foreach {
      case (input, index) if index < _bundledOutput.length =>
        val safeLength = input.length min _bundledOutput(index).length
        input.copyToArray(_bundledOutput(index), 0, safeLength)
      case _ =>
    }

    nbt.getTagList(Settings.namespace + "rs.rednetInput", NBT.TAG_INT_ARRAY).toArray[NBTTagIntArray].
      map(_.getIntArray).zipWithIndex.foreach {
      case (input, index) if index < _rednetInput.length =>
        val safeLength = input.length min _rednetInput(index).length
        input.copyToArray(_rednetInput(index), 0, safeLength)
      case _ =>
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)

    nbt.setNewTagList(Settings.namespace + "rs.bundledInput", _bundledInput.view)
    nbt.setNewTagList(Settings.namespace + "rs.bundledOutput", _bundledOutput.view)

    nbt.setNewTagList(Settings.namespace + "rs.rednetInput", _rednetInput.view)
  }

  // ----------------------------------------------------------------------- //

  protected def computeBundledInput(side: EnumFacing): Array[Int] = {
    /* TODO RedLogic
    val redLogic =
      if (Mods.RedLogic.isAvailable) {
        val (nx, ny, nz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
        if (world.blockExists(nx, ny, nz)) world.getTileEntity(nx, ny, nz) match {
          case wire: IInsulatedRedstoneWire =>
            var strength: Array[Int] = null
            for (face <- -1 to 5 if wire.wireConnectsInDirection(face, side.ordinal()) && strength == null) {
              strength = Array.fill(16)(0)
              strength(wire.getInsulatedWireColour) = wire.getEmittedSignalStrength(face, side.ordinal())
            }
            strength
          case emitter: IBundledEmitter =>
            var strength: Array[Int] = null
            for (i <- -1 to 5 if strength == null) {
              strength = Option(emitter.getBundledCableStrength(i, side.getOpposite.ordinal())).fold(null: Array[Int])(_.map(_ & 0xFF))
            }
            strength
          case _ => null
        }
        else null
      }
      else null
    */
    /* TODO Project Red
    val projectRed =
      if (Mods.ProjectRedTransmission.isAvailable) {
        Option(ProjectRedAPI.transmissionAPI.getBundledInput(world, x, y, z, side.ordinal)).fold(null: Array[Int])(_.map(_ & 0xFF))
      }
      else null
    */
    /* TODO RedLogic or Project Red
    (redLogic, projectRed) match {
      case (a: Array[Int], b: Array[Int]) => (a, b).zipped.map((r1, r2) => math.max(r1, r2))
      case (a: Array[Int], _) => a
      case (_, b: Array[Int]) => b
      case _ => null
    }
    */
    null
  }

  override protected def onRedstoneOutputEnabledChanged() {
    /* TODO MFR
    if (Mods.MineFactoryReloaded.isAvailable) {
      for (side <- EnumFacing.VALID_DIRECTIONS) {
        val blockPos = BlockPosition(x, y, z).offset(side)
        world.getBlock(blockPos) match {
          case block: IRedNetNetworkContainer => block.updateNetwork(world, x, y, z, side.getOpposite)
          case _ =>
        }
      }
    }
    */
    super.onRedstoneOutputEnabledChanged()
  }

  // ----------------------------------------------------------------------- //
  /* TODO RedLogic
    @Optional.Method(modid = Mods.IDs.RedLogic)
    def getBundledCableStrength(blockFace: Int, toDirection: Int): Array[Byte] = bundledOutput(EnumFacing.getOrientation(toDirection)).map(value => math.min(math.max(value, 0), 255).toByte)

    @Optional.Method(modid = Mods.IDs.RedLogic)
    def onBundledInputChanged() = checkRedstoneInputChanged()
  */
  // ----------------------------------------------------------------------- //
  /* TODO Project Red
    @Optional.Method(modid = Mods.IDs.ProjectRedTransmission)
    def canConnectBundled(side: Int) = isOutputEnabled

    @Optional.Method(modid = Mods.IDs.ProjectRedTransmission)
    def getBundledSignal(side: Int) = bundledOutput(EnumFacing.getOrientation(side)).map(value => math.min(math.max(value, 0), 255).toByte)
  */
}
