package io.github.fabricators_of_create.porting_lib.mixin.common;

import net.minecraft.client.gui.components.AbstractButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractButton.class)
public interface AbstractButtonAccessor {
	@Invoker("getTextureY")
	int port_lib$getTextureY();
}
