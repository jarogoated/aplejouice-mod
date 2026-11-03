package de.joris.aplejuice;

import com.google.gson.JsonParser;

/** Strict metadata validation. The updater only offers the fixed project download page. */
public record UpdateRelease(String version,String installed) {
    public static final String DOWNLOAD_URL="https://aplejouice.com/downloads";
    public static UpdateRelease parse(String json,String minecraft,String installed) {
        try {
            if(json.length()>16384) return null;
            var data=JsonParser.parseString(json).getAsJsonObject();
            if(data.get("schemaVersion").getAsInt()!=1 || !"aplejuice".equals(data.get("modId").getAsString())
                    || !minecraft.equals(data.get("minecraft").getAsString())
                    || !DOWNLOAD_URL.equals(data.get("downloadUrl").getAsString())) return null;
            String version=data.get("version").getAsString();
            return newer(version,installed) ? new UpdateRelease(version,installed) : null;
        } catch(RuntimeException ignored) { return null; }
    }
    public static boolean newer(String candidate,String installed) {
        if(candidate==null || installed==null || !candidate.matches("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")
                || !installed.matches("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")) return false;
        String[] a=candidate.split("\\."),b=installed.split("\\.");
        for(int i=0;i<3;i++) {
            int comparison=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));
            if(comparison!=0) return comparison>0;
        }
        return false;
    }
}
