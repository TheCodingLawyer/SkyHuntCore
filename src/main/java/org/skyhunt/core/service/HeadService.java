package org.skyhunt.core.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.skyhunt.SkyHuntCorePlugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Manages head unlocks, drops, and selling.
 */
public class HeadService {

    private final SkyHuntCorePlugin plugin;
    private final NamespacedKey mobKey;
    private final Map<String, Double> headPrices;
    private final List<String> mobTypes;
    private final Map<String, String> textureMap;

    public HeadService(SkyHuntCorePlugin plugin, Map<String, Double> headPrices) {
        this.plugin = plugin;
        this.headPrices = headPrices;
        this.mobKey = new NamespacedKey(plugin, "mob_type");
        this.mobTypes = new ArrayList<>(headPrices.keySet());
        this.textureMap = defaultTextures();
    }

    public ItemStack createHeadItem(String mobType) {
        String normalized = mobType.toUpperCase(Locale.ROOT);
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + normalized + " Head");
            meta.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, normalized);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            applyMobTexture(meta, normalized);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack createDisplayHead(String mobType, boolean unlocked) {
        String normalized = mobType.toUpperCase(Locale.ROOT);
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((unlocked ? "§f" : "§8") + normalized + " Head");
            meta.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, normalized);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            applyMobTexture(meta, normalized);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isHead(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PLAYER_HEAD) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(mobKey, PersistentDataType.STRING);
    }

    public String getMobType(ItemStack stack) {
        if (!isHead(stack)) return null;
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
    }

    public double getPrice(String mobType) {
        return headPrices.getOrDefault(mobType.toUpperCase(Locale.ROOT), 0.0);
    }

    public List<String> getMobTypes() {
        return Collections.unmodifiableList(mobTypes);
    }

    public int sellOne(Player player, ItemStack hand) {
        if (!isHead(hand)) return 0;
        String mob = getMobType(hand);
        double price = getPrice(mob);
        if (price <= 0) {
            return 0;
        }
        hand.setAmount(hand.getAmount() - 1);
        plugin.getEconomy().depositPlayer(player, price);
        String msg = plugin.getConfig().getString("messages.head-sell-single", "&aSold 1x {mob} head for ${price}!")
            .replace("{mob}", mob)
            .replace("{price}", String.format(Locale.US, "%.2f", price));
        player.sendMessage(color(msg));
        return 1;
    }

    public int sellAll(Player player, String mobType) {
        int sold = 0;
        double price = getPrice(mobType);
        if (price <= 0) return 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (!isHead(item)) continue;
            String mob = getMobType(item);
            if (!mobType.equalsIgnoreCase(mob)) continue;
            sold += item.getAmount();
            contents[i] = null;
        }
        player.getInventory().setContents(contents);

        if (sold > 0) {
            double total = sold * price;
            plugin.getEconomy().depositPlayer(player, total);
            String msg = plugin.getConfig().getString("messages.head-sell-bulk", "&aSold {amount}x {mob} heads for ${total}!")
                .replace("{mob}", mobType)
                .replace("{amount}", String.valueOf(sold))
                .replace("{total}", String.format(Locale.US, "%.2f", total));
            player.sendMessage(color(msg));
        }
        return sold;
    }

    private String color(String msg) {
        return msg.replace("&", "§");
    }

    private void applyMobTexture(ItemMeta meta, String normalizedMob) {
        if (!(meta instanceof SkullMeta skullMeta)) return;
        try {
            String textureBase64 = textureMap.get(normalizedMob);
            if (textureBase64 != null && !textureBase64.isEmpty()) {
                // Use Paper's PlayerProfile API (1.18+) for custom textures
                // Decode base64 to get the texture URL
                String textureUrl = decodeTextureUrl(textureBase64);
                if (textureUrl != null) {
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), normalizedMob);
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(new URL(textureUrl));
                    profile.setTextures(textures);
                    skullMeta.setOwnerProfile(profile);
                    return;
                }
            }
        } catch (MalformedURLException e) {
            plugin.getLogger().warning("Invalid texture URL for " + normalizedMob);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply texture for " + normalizedMob + ": " + e.getMessage());
        }
        // Fallback: use Minecraft's head texture based on mob name
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), normalizedMob);
            skullMeta.setOwnerProfile(profile);
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Decode base64 texture data to extract the texture URL
     */
    private String decodeTextureUrl(String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            // JSON format: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/..."}}}
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd = decoded.indexOf("\"", urlStart);
            if (urlStart > 7 && urlEnd > urlStart) {
                return decoded.substring(urlStart, urlEnd);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Map<String, String> defaultTextures() {
        Map<String, String> map = new HashMap<>();
        // Passive mobs
        map.put("PIG", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjE2YmQyMjQ1NjE4NjU5NzQ2NTc3NjI1ZjM0ZjE2MDc1YWQxZGM4YmI4NDRlNTA0ZjljM2Y0ZjllYjBiOGQ0OCJ9fX0=");
        map.put("COW", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM1ZmY4ZTRlNjA2ZWNhZWU5MTFlOTk2ZGZmNDZjNzkzZTU2YzQxNTVjMWY0MjEwYzE4ZTQzZWJlMGRhYzlkNiJ9fX0=");
        map.put("CHICKEN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzBkMWI0ZTZiNmFjZGY1YTkzYWY1ZmNlZjRiYjI5ZDMyZTQyNGM4NGIwYjZmZWNmNWM5ZWU3MTE4MThjMDI3MiJ9fX0=");
        map.put("SHEEP", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTY4OTVmOWU3ODNjODBiMWZjMjA5YWI4Y2NhMmM4MjM5NmVjMjNiNTc5YWIwN2U0Y2FhY2U3OTVmYzNlNTY2YiJ9fX0=");
        map.put("RABBIT", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmMyNTQ1Mjg2MGQ2ZGNiOTAyZjk4NzRjYmE0ZGRhZDVkN2ZjYWQ0YWJkYTFmOTgwZTcwNzA2NGFlMGMzYTBjNCJ9fX0=");
        map.put("HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWZmN2E3ZjE2ZTVkNTljZDUyNmVlZjI4YWMxYmZiMGI4NmMxZDQxNzE4ZjYyY2U3NTg0ZTQzZDM4YjBiNjgxYSJ9fX0=");
        map.put("DONKEY", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNhYmMxNTY1YWU3ODZlMzNkZjM5YzcyZDcwZTM2YTRmZmRmOWQ0MGI0OGI0NzVhN2NmMTI1YzgxN2MzNzlkIn19fQ==");
        map.put("MULE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2RhY2I5YTQ5Y2ViYWE5NTQyNjJkZTY4MmFiNjgyN2YxMmJjYTFiMmVjNjk4ZWMyMzQ4M2ZmMDIyYmJmOGQzYSJ9fX0=");
        map.put("LLAMA", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWJiZjQ2M2M3ZWNjMDRhMjc3Yzc0MTVhNzdiNWRlMTZjNDVhYzcxMzdiNTMwMjFlOWQyOTFlNjMxOTIzMjM3YSJ9fX0=");
        map.put("POLAR_BEAR", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxOWRmYTRjNTc4NjY5NmE2YmE5MjNjNTNlMGU3YmM0OTAzYmZhMGNmNmI4Nzk4ZGM3NGFhYTg0MjcyMzFlMCJ9fX0=");
        map.put("PARROT", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2E5MzE4NmY2ZGFhNjkzMjVjNmQ5MTY1Zjc2ODNhYzM5ZDYyNzNlN2M1NmM2YzY0NDFhYmU0ZDFjYTY5OTZiOSJ9fX0=");
        map.put("OCELOT", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmJhMTc4NmM2YWVjZWRmMjhhZWViYzU2NWU0NDIzZDI3ZjZkNWRhZGEzZmRlMGRkMTUwNTFiNjE2ODk5YTU5YiJ9fX0=");
        map.put("CAT", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ZDljMzNlYzM3OTc1OTIwYWQyMzg4NjJkY2RkN2M5ZjY5NTI1M2VmYjc2NzZmNzZiN2NmMmM5NTFiZjY5MiJ9fX0=");
        map.put("FOX", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjlkODczNmZjMzJlZjc0MzY1NjcwYzExYzVhN2IzNjIwMzRmMDA0ZjdlMjVkNWZkN2M2MDY0NGRhNWY3YjM1YiJ9fX0=");
        map.put("GOAT", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2NjNTYzZGI0ZTRjMTRlN2VmZjMyNTc1ZjA4ODJhMWZhMDZmMjAzZTNmNmI3NmMxZTU5OWQ2YmE3ODQxNDg1YSJ9fX0=");
        map.put("CAMEL", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjc2MWUwM2FjMWMwMjJmZjNmNDQ2Y2M3YzYxZTU5MWYxMzgxOTNhMjkyNjg2MWVkYTI4OWVkMTg5MTg0ODMyNiJ9fX0=");

        // Hostile/neutral mobs
        map.put("ZOMBIE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJiYTU5YjEyOWQ1NzkzZmZmNWVjZDgyNmVlZTgyNzcwNGNiNTJhYzA1ZWFmZDI1OGJjZTMzMjFlMjhlNmM4YSJ9fX0=");
        map.put("SKELETON", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzNhMzMwNzA2ZDA0YTliNmM1ZjU3NzM0NTY0ZDY0MmNiNTU2YTAxYzNjNzljNGQzNzM4Zjg2Mzc5YTUyZDM3In19fQ==");
        map.put("CREEPER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTMyYzcxNjEyOTZhZGEzOWQ0ZTQ5ODJhYjE5NjU2OTM3ZDM3YTRjYTg2M2Q1NzQzZTRjNmNkMmQwMWVlM2ZkMiJ9fX0=");
        map.put("SPIDER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI4ZTM3OGE1ZDBhNmIyZDM0ZTcxNmNkMDI1ODIxN2NiMThlZjFlYzNmNTZiNTAxYTQyYThlZmUyYWVmZjQ5NSJ9fX0=");
        map.put("CAVE_SPIDER", map.get("SPIDER"));
        map.put("ENDERMAN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlYTVhNTY1NmRhN2M3MTRlMjc4ZjM0YTcxZDRhNzliZDViYmUxYmI1ZDc5OTM1ODI4ZDhlNzRmNzhmYWZmZiJ9fX0=");
        map.put("BLAZE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGU3Mjk3MjBjZjg3YWI1YzYzYWM4ODg3M2M5NTA4ZDdkYTgyNTk3NzU3ZjY0NTRlZjNlNjNkN2QxMWE3MSJ9fX0=");
        map.put("WITHER_SKELETON", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDNjOTZhZDljZjMzM2ZmOTVjMTljYzk5NTY2YWY5N2U5NmVjMzFkYzA1ODQyZDkwM2I4NDY0MWZjZTg5MTlhMiJ9fX0=");
        map.put("WITHER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2QyN2RhN2ZhOTQ0ZDI5OWRhZmFhMDM2MmZmMmY0YzQ5MmU5NGYwNTVjMjNiYmQxZjYxZWVhNzliMTc2ZTM0YSJ9fX0=");
        map.put("GHAST", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ5ZTAyNDcwZDE1YmIxOGU2MDhiZGQxNGE0YzFlYTNlOGY5MTM5ZDAxM2FlYjMyNjA4ZmFlNTkwMzVmYjQxMSJ9fX0=");
        map.put("SLIME", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmY0MDI5MTc3YWRhMjAyYTNiNmFkZDlmODhhNzdmYTE5ZTg2ODdlNTBlNTg4MjNlODk3OGY2OTY0MGI0ZjFjMiJ9fX0=");
        map.put("MAGMA_CUBE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjMyNmY4MTlhMGJmMTQ5Y2U0OGE4MjkxYWQ0ZGY1NmE2ZjNmNTRkZjRkZTFjN2U3ODdkMzliZjNhMzQ2MTVhMCJ9fX0=");
        map.put("ZOMBIFIED_PIGLIN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2I3YzE4NjBiZTRlMTcwMDQxZTc1ZmIwMDhiYzY2ZmJkZjBhNmY1ZWY1ZWJmNzEzM2NhZTg1YzYxMjdkNmE1MiJ9fX0=");
        map.put("PIGLIN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2NmYmMzOTMwNzY0ZGQxZjQzNzkyYmJmNmE1YjgyZjBiOTdlZDMzZmYzZmZjZTJkNmFiNmQxYzY0MGI5YWYyNCJ9fX0=");
        map.put("PIGLIN_BRUTE", map.get("PIGLIN"));
        map.put("HOGLIN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjlkMzM3ZjE0NGQ2Yzg0MjQ5NjE5ODc2YmZkMTczMGM1MzA4OGE4YTk0ZTA0YjA2YmMyNWI0MTVkNWM5M2EzOCJ9fX0=");
        map.put("ZOGLIN", map.get("HOGLIN"));
        map.put("STRIDER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDc4NTA3YmVjNjNhYmVkZDI5MjEzZmQzM2Y4MzZlMTRhZTMxYWQxN2I1MzFjM2I4ZmQ5NzMxYTAwNGU1ZjJmYSJ9fX0=");
        map.put("GUARDIAN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ5OWQzNjhlNzM5MjMxOTdiNDhjYzNmYTI3YmU1NDA3NTBkYzM3OTFlZTE0MTQwYmQzYTFjMzBhNWQ1YmU3ZiJ9fX0=");
        map.put("ELDER_GUARDIAN", map.get("GUARDIAN"));
        map.put("SHULKER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTY5YTk1MzcxZjU3ZWQzYzFkM2Q2MjA5NGI1NGYzNDE0NGI0YzA0ZmVmYzFiMTRlNWYxODVlNGM5YjA5YTFmYiJ9fX0=");
        map.put("PHANTOM", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZlN2Y3NzM2ZjNlOWU4OGMzZDMzYzZmM2Q1ZjRjNDM0ZmIzOGM2ZGJhNmZiMWMxNzE5ZDZmYmEzN2M3NjQ4OCJ9fX0=");
        map.put("DROWNED", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjJlNzE2MWYwYTY0OTYyOTRhOWJlMGQ1NzA2YzYwYTI5MjM3NDI3OTdmY2E5ZjFkMmU5N2UxN2U1OTcxY2U3OCJ9fX0=");
        map.put("HUSK", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMDhjZmNiMWJmNTgwZWI2ZWIzZjViZmI0YWI1NzZlY2I2ZDkyNmIyMjgwN2I5ZjA2Y2VkZjc3ZGJhM2FiMDA3NyJ9fX0=");
        map.put("STRAY", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGFlMWUxOWY5NzM3ODRjYWM3N2Q3ZmZmNjdmZDQ5ZGQ4NjMyMjllN2Y5YjI3ZmU2YjQ1ZTcxZTczMzFhZTAxYiJ9fX0=");
        map.put("VEX", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTNmYzA2ZTU5N2EzNmMyNDdiOGE1NDY3YzBkMDk3ZTI3MzUyN2Y5ZTgyMzRkYTQ1OTI1ZDIzMjQ5MWMxMmJiYiJ9fX0=");
        map.put("VINDICATOR", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMGU1NGU3OTcwNWU5ZTg0NmU1ZTBiMjE5NzExOGMxYmQ5MzgwZGM0MWZkMzI5MGE5MzQ2ODExNjY4ZDAxNDY0NyJ9fX0=");
        map.put("EVOKER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzZkZmNmMDRmM2UzNTU2ZmUwMTE1OGI5YTc4ZDg2ZDc2ZTk5OTJkMGM4NTQyNGY1N2I4YWI5NWRhYzk5NDA0YyJ9fX0=");
        map.put("ILLUSIONER", map.get("EVOKER"));
        map.put("PILLAGER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmY4OTc5ZmE2ZjEyODQ1ODQ3NzRkN2E2NDVmMWJiMjE0ZTc3OWI1MjU1ZGIxNTJiNzIxN2U4MTEyMjZiNGM2NCJ9fX0=");
        map.put("RAVAGER", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDg4MmNmZDQ4NzFjZjI3NGYyZjZmZGVhYzI3MWE1YWI5Y2E2YjY3MzE4YmQ5MDMzZWVhNTA1ZmU4NmI1MTBkMSJ9fX0=");
        map.put("WITCH", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjE4ZmE3MjE4MWVjNjcwMzk1YWZhYzU3Nzc3ZjQ4NTE5NDE5YThhNTQ2YTllZjVhY2M4ZDIxNWE2OGE3YmU0MSJ9fX0=");
        map.put("WARDEN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWJlYjU1NjQ2NmM5MjBkYzAzZWE3NThkYmU2NmY3NzhjOTdhNGRmYzgxMzk1Nzc3Y2E0YjExNzgzNjAzZmQ0YiJ9fX0=");
        map.put("ALLAY", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjFmMmZhYWUwMjE5ZjI2NjE5OGQzMjYxNGU1MjM1OTdiODJiOTQyZDBmZmJkZmM4NTFmNTZmMzRiM2I2YjA3YiJ9fX0=");
        map.put("SNOW_GOLEM", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZhZGM1ZjdhNDMxNTZhZGQ0NGYyZGU1YWY1NTg0NGRiYTZhMDcwMjA0NTRmM2NhNTAxNTEyNzEwM2M1NjQ0OSJ9fX0=");
        map.put("IRON_GOLEM", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzY5NTU4ZjA4OTAxMjI1NzJiZDgwY2E4NmU3YTk5MjQ3Mjg1N2I1Zjk0OTFiZjg4YTNkZGJhNmM1NzhmOTMxNyJ9fX0=");
        map.put("SILVERFISH", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZjNzU3ZDhkMGMxNDI4Nzg3ZGIzNDQyYmU1YWUwYmRhYzYyZTg3YzFkYWFhNzQ1YWRkZjRhMDg5NmQzNGMyNiJ9fX0=");
        map.put("ENDERMITE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWFlYjcwZDViZGYwMGMwZTRjNDVmODFmYWZmZDU2ZGQwNjk1Y2Y2ZWI4YzlkNDAxNjMxNjA0ZGQyMWMxY2U5OSJ9fX0=");
        map.put("BAT", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjQ3ZDdjY2YyYjY3MjVlNmY5Mjc5OTMxN2Y0ODg3YmQ1ZjBiNjcyM2M2NjJhOGI3ZTFiYzQ3OTMyNWI2YzY1MiJ9fX0=");
        map.put("BEE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmY4NzZkNjAzZTc0ZjRkMzYyZWQ4NGIyNWM1NGIwZTFhYmVkY2JiZjJlNjg1MzAyNjdkODNhODliNzZmZTg5OCJ9fX0=");
        map.put("TURTLE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmY1NDE5MThjYmFhMzk1Mjk0MTAyYmRkMGRjMTM1ZjBiNjM1NDU4M2FkZWI5MWZiNzM1Zjk5Y2Q4ZmYxM2Y4YyJ9fX0=");
        map.put("DOLPHIN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFlNmRhZTBiMWY5YTgxMzM1N2JhNmY2YmE1OWFmOTcwNWEzZTFiMWIzOWFmODNkNjk1MjcwNTA5NDYyYmUyIn19fQ==");
        map.put("COD", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTFiZGVhNzU4NGU3YzJmZWZmYjEzY2ZjZDQ0M2Q2ZjY3OTQyYjQ4YmNmZDI0YWZmZjkzYWQzZjMxMTE2YmRlZiJ9fX0=");
        map.put("SALMON", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTlhNTQxYTBhZDI2YzQ4NjU1NzZkODQ5ZDljMzYyNzAyYmE2ZDUwNjJlNGY0ZTVjMmVkNTUzNzlhOGNhMmY3NSJ9fX0=");
        map.put("PUFFERFISH", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2NDg2YzUyOGE5NzU3MzM0ZGFjYzc4Mzk1MTZmN2Y5NzhmZjE0YzM1YjBlMzRmNzc2NWFjYTNhNGIwMjk3ZSJ9fX0=");
        map.put("TROPICAL_FISH", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Q3NzljZmJlMWNjNmNmYTY2OGY5NDFiM2Y3NGZmYzY3NTMyZTExOTdiYzJlNmVjYzA3YjU0NDk2NGQ1Mzc4NCJ9fX0=");

        // Boss/minor
        map.put("ENDER_DRAGON", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGMwNTM4MTA5NGQzYzZkZjE5ZTc0OTZiMmM2MDM5NmRjMjM2NmQ2MWUxYTg3YzcwYjY2OWU5ZjFmYjM1M2Y4ZSJ9fX0=");
        map.put("ELDER_GUARDIAN", map.get("GUARDIAN"));
        return map;
    }
}

