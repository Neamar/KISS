package fr.neamar.kiss.pojo;

public final class PhonePojo extends Pojo {
    public final String phone;
    private final String historyId;

    public PhonePojo(String id, String historyId, String phone) {
        super(id);
        this.historyId = historyId;
        this.phone = phone;
    }

    @Override
    public String getHistoryId() {
        return historyId;
    }
}
