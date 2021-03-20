package fr.neamar.kiss.db;

public class ShortcutRecord {
    public int dbId;

    /**
     * Visible name of shortcut.
     */
    public String name;

    /**
     * The package name of the publisher app.
     */
    public String packageName;

    public String intentUri;

    public byte[] icon_blob;

}
