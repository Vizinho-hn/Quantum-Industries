package br.com.quantumindustries.client.codex;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.item.Item;

/** Render state for the clipped 3D machine viewport inside the Quantum Codex tablet. */
public record CodexMachinePreviewState(
        Item item,
        float yawDeg,
        float pitchDeg,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public CodexMachinePreviewState(Item item, float yawDeg, float pitchDeg,
                                    int x0, int y0, int x1, int y1, float scale,
                                    ScreenRectangle scissorArea) {
        this(item, yawDeg, pitchDeg, x0, y0, x1, y1, scale, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
