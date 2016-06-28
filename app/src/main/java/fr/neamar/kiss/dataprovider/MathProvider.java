package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.MathPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.utils.MathEvaluator;

public class MathProvider extends Provider<MathPojo> {

    @Override
    public void reload() {
        //TODO
    }

    public ArrayList<Pojo> getResults(String query) {
        MathPojo pojo = new MathPojo();
        pojo.expressionValue = new MathEvaluator(query).eval().toPlainString();
        ArrayList<Pojo> list = new ArrayList<>();
        list.add(pojo);
        return list;
    }


}

