package fr.neamar.kiss.pojo;

public final class PhonePojo extends Pojo {
    public final String phone;
    private final String historyId;
    private final String customIconId;

    public PhonePojo(String id, String historyId, String phone, String customIconId) {
        super(id);
        this.historyId = historyId;
        this.phone = phone;
        this.customIconId = customIconId;
    }

    @Override
    public String getHistoryId() {
        return historyId;
    }

    @Override
    public String getCustomIconId() {
        return customIconId;
    }
}
