package com.lx862.jcm.mod.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.lx862.jcm.mod.JCM;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BuildMetadata {
    public String version;
    public String fullVersion;
    public String minecraftVersion;

    private BuildMetadata() {
    }

    public static BuildMetadata of() throws IOException {
        Gson gson = new Gson();
        try(InputStream is = JCM.class.getClassLoader().getResourceAsStream("build_metadata.json")) {
            if(is == null) throw new IOException("FATAL: Failed to locate build_metadata.json!");
            return gson.fromJson(new JsonReader(new InputStreamReader(is)), BuildMetadata.class);
        }
    }

    public ReleaseType getReleaseType() {
        return fullVersion.contains(".artifacts_") ? ReleaseType.NIGHTLY : fullVersion.contains("-beta.") ? ReleaseType.BETA : ReleaseType.RELEASE;
    }

    public enum ReleaseType {
        NIGHTLY,
        BETA,
        RELEASE
    }
}
