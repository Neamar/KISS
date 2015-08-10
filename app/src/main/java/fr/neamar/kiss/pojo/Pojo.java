package fr.neamar.kiss.pojo;

public abstract class Pojo {
    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    public String id = "(none)";

    // Name for this pojo, e.g. app name
    public String name = "";

    // Lower-cased name, for faster search
    public String nameLowerCased = "";

    // Name displayed on the screen, may contain HTML (for instance, to put
    // query text in blue)
    public String displayName = "";

    // How relevant is this record ? The higher, the most probable it will be
    // displayed
    public int relevance = 0;
}
