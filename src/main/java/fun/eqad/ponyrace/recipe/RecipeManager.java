package fun.eqad.ponyrace.recipe;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;

public class RecipeManager {
    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        registerRebirthPotionRecipe();
        registerManaPotionRecipe();
        registerStaminaPotionRecipe();
    }

    private void registerRebirthPotionRecipe() {
        ItemStack potion = createRebirthPotion();

        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "rebirth_potion"),
                potion
        );

        recipe.shape(
                "GGG",
                "EAS",
                "GGG"
        );

        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('E', Material.END_CRYSTAL);
        recipe.setIngredient('A', Material.ENCHANTED_GOLDEN_APPLE);
        recipe.setIngredient('S', Material.NETHER_STAR);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerManaPotionRecipe() {
        ItemStack potion = createManaPotion();

        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "mana_potion"),
                potion
        );

        recipe.shape(
                "GGG",
                "BWB",
                "GGG"
        );

        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('B', Material.BLAZE_POWDER);
        recipe.setIngredient('W', Material.NETHER_WART);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerStaminaPotionRecipe() {
        ItemStack potion = createStaminaPotion();

        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "stamina_potion"),
                potion
        );

        recipe.shape(
                "GGG",
                "ACA",
                "GGG"
        );

        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('A', Material.APPLE);
        recipe.setIngredient('C', Material.GOLDEN_CARROT);

        plugin.getServer().addRecipe(recipe);
    }

    public ItemStack createRebirthPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.setDisplayName("§b转生药水");
        meta.setLore(Arrays.asList(
                "§8--------",
                "§7可以让你重新选择种族",
                "§8--------",
                "§a右键饮用",
                "§8--------"
        ));

        meta.setColor(Color.fromRGB(0, 247, 255));

        meta.setCustomModelData(389000);

        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.LUCK, 1, true);

        potion.setItemMeta(meta);
        return potion;
    }

    public ItemStack createManaPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.setDisplayName("§d魔力药水");
        meta.setLore(Arrays.asList(
                "§8--------",
                "§7饮用后瞬间回满魔力值",
                "§7适用种族: 独角兽",
                "§8--------",
                "§a右键饮用",
                "§8--------"
        ));

        meta.setColor(Color.fromRGB(148, 0, 211));

        meta.setCustomModelData(389001);

        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.LUCK, 1, true);

        potion.setItemMeta(meta);
        return potion;
    }

    public ItemStack createStaminaPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.setDisplayName("§6体力药水");
        meta.setLore(Arrays.asList(
                "§8--------",
                "§7饮用后瞬间回满体力值",
                "§7适用种族: 飞马, 陆马, 夜琪, 海马, 龙族",
                "§8--------",
                "§a右键饮用",
                "§8--------"
        ));

        meta.setColor(Color.fromRGB(255, 165, 0));

        meta.setCustomModelData(389002);

        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.LUCK, 1, true);

        potion.setItemMeta(meta);
        return potion;
    }
}