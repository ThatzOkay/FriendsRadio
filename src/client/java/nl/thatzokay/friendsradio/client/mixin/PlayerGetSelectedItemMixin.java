package nl.thatzokay.friendsradio.client.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface PlayerGetSelectedItemMixin {
    @Accessor("selectedItem")
    ItemStack getSelectedItem();
}