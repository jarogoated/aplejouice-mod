package de.joris.aplejuice;

import java.util.*;

public enum CoordinateAxes {
    XYZ, XY, XZ, YZ, X, Y, Z, NONE;
    public static CoordinateAxes parse(String value) {
        if(value==null) return XYZ;
        if(value.isBlank()) return NONE;
        try { return valueOf(value.toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException ignored) { return XYZ; }
    }
    public boolean includes(char axis) { return this!=NONE && name().indexOf(axis)>=0; }
    public CoordinateAxes toggle(char axis) {
        if("XYZ".indexOf(axis)<0) throw new IllegalArgumentException("Unknown coordinate axis");
        StringBuilder axes=new StringBuilder();
        for(char candidate:"XYZ".toCharArray()) if(includes(candidate) ^ (candidate==axis)) axes.append(candidate);
        return parse(axes.toString());
    }
    /** Block coordinates use floor, including negative positions. */
    public String format(double x,double y,double z) {
        List<String> values=new ArrayList<>();
        if(includes('X')) values.add(Integer.toString((int)Math.floor(x)));
        if(includes('Y')) values.add(Integer.toString((int)Math.floor(y)));
        if(includes('Z')) values.add(Integer.toString((int)Math.floor(z)));
        return String.join(" ",values);
    }
}
