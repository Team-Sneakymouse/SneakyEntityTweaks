package com.danidipp.sneakyentitytweaks

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Allay
import org.bukkit.entity.EntityType
import org.bukkit.entity.FishHook
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sqrt

/** One kite rod from config: how to recognise the rod, and the item_model the kite shows. */
private data class KiteRod(val rodModel: NamespacedKey?, val legacyCmd: Int?, val kiteModel: NamespacedKey)

class Kite(config: ConfigurationSection?) : Listener {
    private val byMagicItem = HashMap<String, KiteRod>()
    private val byRodModel = HashMap<NamespacedKey, KiteRod>()
    private val byLegacyCmd = HashMap<Int, KiteRod>()

    init {
        val logger = SneakyEntityTweaks.getInstance().logger
        for (id in config?.getKeys(false) ?: emptySet()) {
            val section = config!!.getConfigurationSection(id) ?: continue
            val kiteModel = section.getString("kite-model")?.let { NamespacedKey.fromString(it) }
            if (kiteModel == null) {
                logger.warning("Kite rod '$id' has no valid kite-model; skipping")
                continue
            }
            val rodModel = section.getString("rod-model")?.let { NamespacedKey.fromString(it) }
            val legacyCmd = if (section.isInt("legacy-cmd")) section.getInt("legacy-cmd") else null
            val rod = KiteRod(rodModel, legacyCmd, kiteModel)
            byMagicItem[id] = rod
            rodModel?.let { byRodModel[it] = rod }
            legacyCmd?.let { byLegacyCmd[it] = rod }
        }
    }

    /** Magic-item id first, then item_model, then legacy integer CMD for copies from before the item_model cutover. */
    private fun kiteRodFor(meta: ItemMeta): KiteRod? {
        meta.persistentDataContainer.get(MAGIC_ITEM_KEY, PersistentDataType.STRING)?.let { id ->
            byMagicItem[id]?.let { return it }
        }
        if (meta.hasItemModel()) {
            byRodModel[meta.itemModel]?.let { return it }
        }
        @Suppress("DEPRECATION")
        if (meta.hasCustomModelData()) {
            byLegacyCmd[meta.customModelData]?.let { return it }
        }
        return null
    }

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
        val kiteRod = kiteRodFor(rodItem.itemMeta) ?: return

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
        kiteMeta.itemModel = kiteRod.kiteModel
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

    companion object {
        private val MAGIC_ITEM_KEY = NamespacedKey("magicspells", "magicitem")
    }
}
