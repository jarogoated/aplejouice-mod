package de.joris.aplejuice;

import java.util.Locale;

public enum CoordinateAxes {
    XYZ, XY, XZ;
    public static CoordinateAxes parse(String value) {
        if(value==null) return XYZ;
        try { return valueOf(value.toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException ignored) { return XYZ; }
    }
    /** Block coordinates use floor, including negative positions, rather than truncation toward zero. */
    public String format(double x,double y,double z) {
        int bx=(int)Math.floor(x),by=(int)Math.floor(y),bz=(int)Math.floor(z);
        return switch(this) {
            case XYZ -> bx+" "+by+" "+bz;
            case XY -> bx+" "+by;
            case XZ -> bx+" "+bz;
        };
    }
}
