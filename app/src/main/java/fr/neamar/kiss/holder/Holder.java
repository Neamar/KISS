package fr.neamar.kiss.holder;

public abstract class Holder extends Object {
	// Globally unique ID.
	// Usually start with provider scheme, e.g. "app://" or "contact://" to
	// ensure unicity
	public String id = "(none)";

	// Name for this holder, e.g. app name
	public String name = "";

	// Lowercased name, for faster search
	public String nameLowerCased = "";

	// Name displayed on the screen, may contain HTML (for instance, to put
	// query text in blue)
	public String displayName = "";

	// How relevant is this record ? The higher, the most probable it will be
	// displayed
	public int relevance = 0;
}
