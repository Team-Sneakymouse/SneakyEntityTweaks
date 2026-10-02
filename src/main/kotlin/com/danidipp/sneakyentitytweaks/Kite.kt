package com.danidipp.sneakyentitytweaks

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Allay
import org.bukkit.entity.EntityType
import org.bukkit.entity.FishHook
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sqrt

class Kite : Listener {
    @EventHandler
    fun onFishhookSpawn(event: ProjectileLaunchEvent) {
        val hook = event.entity
        if (hook !is FishHook)
            return
        val player = hook.shooter
        if (player !is Player)
            return
        val rodItem = when (player.inventory.itemInMainHand.type) {
            Material.FISHING_ROD -> player.inventory.itemInMainHand
            else -> player.inventory.itemInOffHand
        }
        if (rodItem.type != Material.FISHING_ROD)
            return
        val rodMeta = rodItem.itemMeta
        if (!rodMeta.hasCustomModelData() || rodMeta.customModelData < 1 || rodMeta.customModelData > 100)
            return

        hook.scoreboardTags.add("lom:kite")
        hook.isInvisible = true

        val allay = player.world.spawnEntity(hook.location, EntityType.ALLAY) as Allay
        val velocity = hook.velocity
        val verticalRatio = abs(velocity.y) / velocity.length() // 0 to 1
        val verticalScale = 1.0 - (0.75 * verticalRatio) // 0.25 to 1
        val adjustedVelocity = velocity.clone().apply { y = velocity.y * verticalScale }
        allay.velocity = adjustedVelocity
        allay.scoreboardTags.add("lom:kite")

        val kiteItem = ItemStack(Material.CLAY_BALL)
        val kiteMeta = kiteItem.itemMeta
        kiteMeta.setCustomModelData(rodMeta.customModelData)
        kiteItem.itemMeta = kiteMeta

        allay.equipment.setItemInMainHand(kiteItem)
        allay.isInvulnerable = true
        allay.isSilent = true
        allay.isCollidable = false
        allay.isInvisible = true

        Bukkit.getScheduler().runTaskLater(SneakyEntityTweaks.getInstance(), Runnable {
            hook.hookedEntity = allay
        }, 1)
    }

    @EventHandler
    fun onFishhookRemove(event: EntityRemoveFromWorldEvent) {
        val entity = event.entity
        if (entity !is FishHook || !entity.scoreboardTags.contains("lom:kite"))
            return
        val allay = entity.hookedEntity as Allay
        Bukkit.getScheduler().runTaskLater(SneakyEntityTweaks.getInstance(), Runnable {
            allay.remove()
        }, 10)
    }
}