package fr.neamar.kiss.pojo;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class ContactData {
    private final long id;
    private final String mimeType;

    private String identifier;
    // IM name without special characters
    private StringNormalizer.Result normalizedIdentifier;

    public ContactData(String mimeType, long id) {
        this.mimeType = mimeType;
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        if (identifier != null) {
            // Set the actual user-friendly name
            this.identifier = identifier;
            this.normalizedIdentifier = StringNormalizer.normalizeWithResult(this.identifier, false);
        } else {
            this.identifier = null;
            this.normalizedIdentifier = null;
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getId() {
        return id;
    }

    public StringNormalizer.Result getNormalizedIdentifier() {
        return normalizedIdentifier;
    }
}
