package de.joris.aplejuice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DuelBorderGeometryTest {
    @Test void galaxyMatchesFloorCeilingAndApproximateMeasuredSides() {
        var map=BorderMap.byId("galaxy");
        assertEquals(37,map.y()-map.radiusY());
        assertEquals(54,map.y()+map.radiusY());
        for (double[] p : new double[][]{{114,45,-80},{105,45,-71},{97,45,-80},{106,45,-89},{106,54,-80},{106,37,-80}}) {
            double dx=(p[0]-map.x())/map.radiusX(),dy=(p[1]-map.y())/map.radiusY(),dz=(p[2]-map.z())/map.radiusZ();
            assertEquals(1,Math.sqrt(dx*dx+dy*dy+dz*dz),.01);
        }
        double bottom=Double.POSITIVE_INFINITY, top=Double.NEGATIVE_INFINITY;
        var mesh=DuelBorderGeometry.mesh();
        assertTrue(mesh.size() < 3000);
        for (var q : mesh) {
            for (float[] p : new float[][]{{q.ax(),q.ay(),q.az()},{q.bx(),q.by(),q.bz()},
                    {q.cx(),q.cy(),q.cz()},{q.dx(),q.dy(),q.dz()}}) {
                assertEquals(1,Math.sqrt(p[0]*p[0]+p[1]*p[1]+p[2]*p[2]),.00001);
                bottom=Math.min(bottom,p[1]*map.radiusY()+map.y()); top=Math.max(top,p[1]*map.radiusY()+map.y());
            }
            assertTrue((q.color() >>> 24) > 0 && (q.color() >>> 24) < 255);
        }
        assertEquals(37,bottom); assertEquals(54,top);
    }
    @Test void onlyNearbyCameraPositionsQualify() {
        var map=BorderMap.byId("galaxy");
        assertTrue(map.inRange(map.x(),map.y(),map.z()));
        assertTrue(map.inRange(map.x()+160,map.y(),map.z()));
        assertFalse(map.inRange(map.x()+161,map.y(),map.z()));
        assertFalse(map.inRange(map.x(),map.y()+161,map.z()));
    }
    @Test void unknownOrRemovedProfilesCannotRestoreTheOriginalBorder() {
        assertEquals(java.util.List.of("galaxy","colloseum","japan"),BorderMap.ALL.stream().map(BorderMap::id).toList());
        assertEquals("galaxy",BorderMap.byId(null).id());
        assertEquals("galaxy",BorderMap.byId("original").id());
        assertEquals("galaxy",BorderMap.byId("plains_tree").id());
    }
    @Test void newMapsUseCorrectedMeasurements() {
        assertMeasured("colloseum",43,60,new double[][]{
                {112,43,-80},{112,60,-80},{112,51,-89},{120,51,-80},{111,51,-71},{103,51,-80}});
        assertMeasured("japan",38,56,new double[][]{
                {111,56,-79},{111,38,-79},{111,46,-70},{102,46,-79},{111,46,-87},{120,46,-79}});
    }
    private void assertMeasured(String id,int floor,int ceiling,double[][] points) {
        var map=BorderMap.byId(id);
        assertEquals(floor,map.y()-map.radiusY()); assertEquals(ceiling,map.y()+map.radiusY());
        for(var p:points) {
            double dx=(p[0]-map.x())/map.radiusX(),dy=(p[1]-map.y())/map.radiusY(),dz=(p[2]-map.z())/map.radiusZ();
            assertEquals(1,Math.sqrt(dx*dx+dy*dy+dz*dz),.015,id);
        }
    }
}
