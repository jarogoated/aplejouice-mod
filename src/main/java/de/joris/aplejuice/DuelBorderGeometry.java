package de.joris.aplejuice;

/** Approximate arena measured at the four sides, floor and ceiling. No world scans. */
public final class DuelBorderGeometry {
    public record Quad(float ax, float ay, float az, float bx, float by, float bz,
                       float cx, float cy, float cz, float dx, float dy, float dz, int color) {}
    public static java.util.List<Quad> mesh() {
        var result = new java.util.ArrayList<Quad>();
        int longitude = 64, latitude = 32;
        for (int v = 0; v < latitude; v++) for (int u = 0; u < longitude; u++) {
            double p = -Math.PI/2 + Math.PI*v/latitude, q = -Math.PI/2 + Math.PI*(v+1)/latitude;
            double a = 2*Math.PI*u/longitude, b = 2*Math.PI*(u+1)/longitude;
            result.add(quad(p,a,q,a,q,b,p,b,0x16A78AE8));
        }
        // Broad luminous meridians and latitude rings, like the reference. Fixed geometry is cached once.
        for (int line = 0; line < 8; line++) {
            double angle = 2*Math.PI*line/8, width = .012;
            for (int v = 0; v < latitude; v++) {
                double p = -Math.PI/2 + Math.PI*v/latitude, q = -Math.PI/2 + Math.PI*(v+1)/latitude;
                result.add(quad(p,angle-width,q,angle-width,q,angle+width,p,angle+width,0x88BEA2FF));
            }
        }
        for (double p : new double[]{-Math.PI/3, -Math.PI/6, 0, Math.PI/6, Math.PI/3})
            for (int u = 0; u < longitude; u++) {
                double a=2*Math.PI*u/longitude, b=2*Math.PI*(u+1)/longitude;
                result.add(quad(p-.008,a,p+.008,a,p+.008,b,p-.008,b,0x66BEA2FF));
            }
        return java.util.List.copyOf(result);
    }
    private static Quad quad(double p,double a,double q,double b,double r,double c,double s,double d,int color) {
        return new Quad(x(p,a),y(p),z(p,a),x(q,b),y(q),z(q,b),x(r,c),y(r),z(r,c),x(s,d),y(s),z(s,d),color);
    }
    private static float x(double p,double a) { return (float)(Math.cos(p)*Math.cos(a)); }
    private static float y(double p) { return (float)Math.sin(p); }
    private static float z(double p,double a) { return (float)(Math.cos(p)*Math.sin(a)); }
    private DuelBorderGeometry() {}
}
