package de.joris.aplejuice;

import java.util.List;

/** Only measured maps are selectable. Add future arena measurements here. */
public record BorderMap(String id, String nameKey, String image, int imageWidth, int imageHeight, double x, double y, double z,
                        double radiusX, double radiusY, double radiusZ) {
    public static final List<BorderMap> ALL = List.of(
            new BorderMap("galaxy","mapGalaxy","galaxy.png",1886,1057,105.5,45.5,-80,8.5,8.5,9),
            new BorderMap("colloseum","mapColloseum","colloseum.png",1856,1020,111.5,51.5,-80,8.5,8.5,9),
            new BorderMap("japan","mapJapan","japan.png",1914,1071,111,47,-78.5,9,9,8.5));
    public static BorderMap byId(String id) {
        String requested="plains_tree".equals(id) ? "galaxy" : id;
        return ALL.stream().filter(map -> map.id.equals(requested)).findFirst().orElse(ALL.getFirst());
    }
    public boolean inRange(double cameraX, double cameraY, double cameraZ) {
        double dx=cameraX-x,dy=cameraY-y,dz=cameraZ-z;
        return dx*dx+dy*dy+dz*dz <= 160*160;
    }
}
