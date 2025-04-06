package com.lx862.mtrscripting.data;

import org.mtr.mapping.holder.Identifier;

public class ScriptContent {
    private final Identifier location;
    private final String content;

    /**
     * Create a new script content for parsing
     * @param location The location of the script
     * @param content The plain text content of the script
     */
    public ScriptContent(Identifier location, String content) {
        this.location = location;
        this.content = content;
        if(this.location == null) throw new IllegalArgumentException("Script location must not be null!");
        if(this.content == null) throw new IllegalArgumentException("Script content must not be null!");
    }

    public Identifier getLocation() {
        return this.location;
    }

    public String getContent() {
        // We were given the file location of the script rather than the script content itself.
        return this.content;
    }
}
