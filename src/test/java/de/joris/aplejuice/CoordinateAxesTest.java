package de.joris.aplejuice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateAxesTest {
    @Test void negativeFractionsUseTheContainingBlockRatherThanTruncation() {
        assertEquals("-1 -65 -80",CoordinateAxes.XYZ.format(-.01,-64.01,-79.99));
        assertEquals("0 0 0",CoordinateAxes.XYZ.format(0,0,0));
        assertEquals("113 45 -80",CoordinateAxes.XYZ.format(113.999,45.999,-80));
    }
    @Test void omittedAxesAreNotCopiedAndValuesHaveNoLabelsOrDecimals() {
        assertEquals("1 64",CoordinateAxes.XY.format(1.9,64.5,-999.1));
        assertEquals("1 -1000",CoordinateAxes.XZ.format(1.9,64.5,-999.1));
        assertEquals("29999999 -64 -30000000",CoordinateAxes.XYZ.format(29999999.9,-64,-29999999.9));
    }
    @Test void eachAxisCanBeToggledIndependentlyIncludingAllOff() {
        assertEquals(CoordinateAxes.XZ,CoordinateAxes.XYZ.toggle('Y'));
        assertEquals(CoordinateAxes.Z,CoordinateAxes.XZ.toggle('X'));
        assertEquals(CoordinateAxes.NONE,CoordinateAxes.Z.toggle('Z'));
        assertEquals(CoordinateAxes.Y,CoordinateAxes.NONE.toggle('Y'));
        assertEquals("-2",CoordinateAxes.Y.format(1,-1.2,3));
        assertEquals("-2 3",CoordinateAxes.YZ.format(1,-1.2,3));
        assertEquals("",CoordinateAxes.NONE.format(1,2,3));
        for(var axes:CoordinateAxes.values()) for(char axis:"XYZ".toCharArray()) assertEquals(axes,axes.toggle(axis).toggle(axis));
    }
    @Test void missingOrInvalidPreferencesKeepUsableDefaults() {
        assertEquals(CoordinateAxes.XYZ,CoordinateAxes.parse(null));
        assertEquals(CoordinateAxes.XYZ,CoordinateAxes.parse("unknown"));
        assertEquals(CoordinateAxes.XZ,CoordinateAxes.parse("xz"));
    }
}
