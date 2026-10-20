package de.joris.aplejuice.client;

import java.util.List;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.Vec3;

public interface BadgeState {
    record Badge(ItemStackRenderState icon, String count) {}
    List<Badge> aplejuice$getBadges();
    void aplejuice$setBadges(List<Badge> badges);
    Vec3 aplejuice$getAnchor();
    void aplejuice$setAnchor(Vec3 anchor);
}
