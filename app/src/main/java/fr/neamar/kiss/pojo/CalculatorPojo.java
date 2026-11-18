package fr.neamar.kiss.pojo;

/**
 * Result of a computation typed in the search bar, e.g. "2+2 = 4"
 */
public final class CalculatorPojo extends Pojo {
    public final String query;

    public CalculatorPojo(String id, String query) {
        super(id);
        this.query = query;
    }

    @Override
    public String getHistoryId() {
        // Calculator results should not appear in history
        return "";
    }
}
