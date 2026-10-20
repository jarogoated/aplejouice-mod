package de.joris.aplejuice.client.mixin;

import de.joris.aplejuice.client.BadgeState;
import java.util.List;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.*;
import net.minecraft.world.phys.Vec3;

@Mixin(AvatarRenderState.class)
public abstract class AvatarStateMixin implements BadgeState {
    @Unique private List<Badge> aplejuice$badges = List.of();
    @Unique private Vec3 aplejuice$anchor;
    public List<Badge> aplejuice$getBadges() { return aplejuice$badges; }
    public void aplejuice$setBadges(List<Badge> badges) { aplejuice$badges = badges; }
    public Vec3 aplejuice$getAnchor() { return aplejuice$anchor; }
    public void aplejuice$setAnchor(Vec3 anchor) { aplejuice$anchor = anchor; }
}
