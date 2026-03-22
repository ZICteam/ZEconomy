package net.sixik.zeconomy.content;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import net.sixik.zeconomy.ZEconomy;
import net.sixik.zeconomy.block.AtmBlock;
import net.sixik.zeconomy.block.ExchangeBlock;
import net.sixik.zeconomy.block.MailboxBlock;
import net.sixik.zeconomy.block.entity.ExchangeBlockEntity;
import net.sixik.zeconomy.block.entity.MailboxBlockEntity;
import net.sixik.zeconomy.item.BankNpcSpawnerItem;
import net.sixik.zeconomy.menu.AtmMenu;
import net.sixik.zeconomy.menu.BankMenu;
import net.sixik.zeconomy.menu.ExchangeMenu;
import net.sixik.zeconomy.menu.MailboxMenu;

public final class EconomyContent {
    private static final ResourceLocation CREATIVE_TAB_REGISTRY = new ResourceLocation("minecraft", "creative_mode_tab");
    private static final ResourceLocation MAIN_TAB_ID = new ResourceLocation(ZEconomy.MOD_ID, "main");
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ZEconomy.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ZEconomy.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ZEconomy.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ZEconomy.MOD_ID);

    public static final RegistryObject<Block> ATM_BLOCK = BLOCKS.register("atm_block", () ->
        new AtmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL))
    );
    public static final RegistryObject<Block> EXCHANGE_BLOCK = BLOCKS.register("exchange_block", () ->
        new ExchangeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 6.0F).sound(SoundType.STONE))
    );
    public static final RegistryObject<Block> MAILBOX_BLOCK = BLOCKS.register("mailbox_block", () ->
        new MailboxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 4.0F).sound(SoundType.WOOD))
    );

    public static final RegistryObject<Item> ATM_BLOCK_ITEM = ITEMS.register("atm_block", () -> new BlockItem(ATM_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXCHANGE_BLOCK_ITEM = ITEMS.register("exchange_block", () -> new BlockItem(EXCHANGE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAILBOX_BLOCK_ITEM = ITEMS.register("mailbox_block", () -> new BlockItem(MAILBOX_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> Z_COIN_ITEM = ITEMS.register("z_coin_item", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> B_COIN_ITEM = ITEMS.register("b_coin_item", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BANK_NPC_SPAWNER = ITEMS.register("bank_npc_spawner", () -> new BankNpcSpawnerItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<BlockEntityType<ExchangeBlockEntity>> EXCHANGE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
        "exchange_block_entity",
        () -> BlockEntityType.Builder.of(ExchangeBlockEntity::new, EXCHANGE_BLOCK.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<MailboxBlockEntity>> MAILBOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
        "mailbox_block_entity",
        () -> BlockEntityType.Builder.of(MailboxBlockEntity::new, MAILBOX_BLOCK.get()).build(null)
    );
    public static final RegistryObject<MenuType<AtmMenu>> ATM_MENU = MENU_TYPES.register("atm_menu", () -> new MenuType<>(AtmMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<BankMenu>> BANK_MENU = MENU_TYPES.register("bank_menu", () -> new MenuType<>(BankMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<MailboxMenu>> MAILBOX_MENU = MENU_TYPES.register("mailbox_menu", () -> new MenuType<>(MailboxMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ExchangeMenu>> EXCHANGE_MENU = MENU_TYPES.register("exchange_menu", () -> IForgeMenuType.create(ExchangeMenu::new));
    private static CreativeModeTab createMainTab() {
        return CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.zeconomy.main"))
            .icon(() -> ATM_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ATM_BLOCK_ITEM.get());
                output.accept(EXCHANGE_BLOCK_ITEM.get());
                output.accept(MAILBOX_BLOCK_ITEM.get());
                output.accept(BANK_NPC_SPAWNER.get());
                output.accept(Z_COIN_ITEM.get());
                output.accept(B_COIN_ITEM.get());
            })
            .build();
    }

    private EconomyContent() {
    }

    private static boolean isCreativeTabRegistry(Object key) {
        return String.valueOf(key).contains("minecraft:creative_mode_tab");
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!isCreativeTabRegistry(event.getRegistryKey())) {
            return;
        }
        event.register((net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<CreativeModeTab>>) event.getRegistryKey(),
            helper -> helper.register(MAIN_TAB_ID, createMainTab()));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        modBus.register(EconomyContent.class);
    }
}
