package fr.neamar.kiss.pojo;

public class SearchPojo extends Pojo {
    public static int SEARCH_QUERY = 0;
    public static int URL_QUERY = 1;
    public static int CALCULATOR_QUERY = 2;

    public String query = "";
    public String url = "";
    public int type = SEARCH_QUERY;
}
