package io.github.cputnama11y.screenshot2webhook.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class ScreenShotMixin {
    @Unique
    private static final ScopedValue<Unit> IS_SCREENSHOT = ScopedValue.newInstance();

    @ModifyArg(
            method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V")
    )
    private static <T> Consumer<T> carryIsScreenshotDown(Consumer<T> consumer, @Local(argsOnly = true) String forceName) {
        return it -> ScopedValue.where(IS_SCREENSHOT, forceName == null
                                                      ? Unit.INSTANCE
                                                      : null).run(() -> consumer.accept(it));
    }

    @ModifyArg(
            method = "lambda$grab$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/TracingExecutor;execute(Ljava/lang/Runnable;)V")
    )
    private static Runnable carryIsScreenshotDown(Runnable command) {
        var isScreenshot = IS_SCREENSHOT.isBound();
        return () -> {
            if (isScreenshot) {
                ScopedValue.where(IS_SCREENSHOT, Unit.INSTANCE).run(command);
            } else command.run();
        };
    }

    @Inject(
            method = "lambda$grab$1",
            at = @At(value = "INVOKE:FIRST", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", shift = At.Shift.AFTER)
    )
    private static void onGrab(CallbackInfo ci, @Local(argsOnly = true) File file, @Local(argsOnly = true) Consumer<Component> sender) {
        if (!IS_SCREENSHOT.isBound()) return;
        sender.accept(Component.empty().append(Component.literal("[Click Here]").withStyle(ChatFormatting.UNDERLINE).withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("screenshot2webbhook send2webhook \"" + file.toPath() + "\"")))).append(" to upload this screenshot to discord"));
    }
}
