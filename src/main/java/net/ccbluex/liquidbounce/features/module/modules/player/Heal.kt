/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.util.MathHelper
import net.ccbluex.liquidbounce.event.PacketEvent
import net.minecraft.network.play.server.S02PacketChat
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.init.Items
import net.minecraft.potion.Potion
import org.apache.commons.lang3.tuple.Pair
import java.util.*

@ModuleInfo(name = "Heal", category = ModuleCategory.PLAYER)
class Heal : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Auto", "LegitAuto", "Head"), "Auto")
    private val percent = FloatValue("HealthPercent", 75.0f, 1.0f, 100.0f)
    private val min = IntegerValue("MinDelay", 75, 1, 5000)
    private val max = IntegerValue("MaxDelay", 125, 1, 5000)
    private val regenSec = FloatValue("RegenSec", 4.6f, 0.0f, 10.0f)
    private val groundCheck = BoolValue("GroundCheck", false)
    private val voidCheck = BoolValue("VoidCheck", true)
    private val waitRegen = BoolValue("WaitRegen", true)
    private val invCheck = BoolValue("InvCheck", false)
    private val absorpCheck = BoolValue("AbsorpCheck", true)
    val timer = MSTimer()
    private var eating = -1
    var delay = 0
    var isDisable = false
    var tryHeal = false
    override fun onEnable() {
        eating = -1
        timer.reset()
        isDisable = false
        tryHeal = false
        delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        isDisable = true
        tryHeal = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (tryHeal) {
            when (modeValue.get().lowercase()) {
            "auto" -> {
                val gappleInHotbar = InventoryUtils.findItem(36, 45, Items.golden_apple)
                if (gappleInHotbar != -1) {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(gappleInHotbar - 36))
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                    repeat(35) {
                        mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
                    }
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    alert("Gapple eaten")
                    tryHeal = false
                    timer.reset()
                    delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
                }else {
                    tryHeal = false
                }
            }
            "legitauto" -> {
                if (eating == -1) {
                    val gappleInHotbar = InventoryUtils.findItem(36, 45, Items.golden_apple)
                    if(gappleInHotbar == -1) {
                        tryHeal = false
                        return
                    }
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(gappleInHotbar - 36))
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                    eating = 0
                } else if (eating > 35) {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    timer.reset()
                    tryHeal = false
                    delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
                }
            }
            "head" -> {
                val headInHotbar = InventoryUtils.findItem(36, 45, Items.skull)
                if (headInHotbar != -1) {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(headInHotbar - 36))
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    timer.reset()
                    tryHeal = false
                    delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
                }else {
                    tryHeal = false
                }
            }
        }
        if (mc.thePlayer.ticksExisted <= 10 && isDisable) {
            isDisable = false
        }
        val absorp = MathHelper.ceiling_double_int(mc.thePlayer.absorptionAmount.toDouble())
        if ((groundCheck.get() && !mc.thePlayer.onGround) || (voidCheck.get() && !MovementUtils.isBlockUnder()) || (invCheck.get() && mc.currentScreen is GuiContainer) || (absorp != 0 && absorpCheck.get()))
            return
        if (waitRegen.get() && mc.thePlayer.isPotionActive(Potion.regeneration) && mc.thePlayer.getActivePotionEffect(Potion.regeneration).duration > regenSec.get() * 20.0f)
            return
        if (!isDisable && (mc.thePlayer.health <= (percent.get() / 100.0f) * mc.thePlayer.maxHealth) && timer.hasTimePassed(delay.toLong())) {
            if (tryHeal)
                return
            tryHeal = true
        }
    }

    override val tag: String?
        get() = if (mc.thePlayer == null || mc.thePlayer.health == Float.NaN) modeValue.get() else modeValue.get()+" "+String.format(
            "%.2f HP",
            percent.get() / 100.0f * mc.thePlayer.maxHealth
        )
} 