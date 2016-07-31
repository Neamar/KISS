package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadMathPojos;
import fr.neamar.kiss.pojo.MathPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.utils.MathEvaluator;

public class MathProvider extends Provider<MathPojo> {

    @Override
    public void reload() {
        this.initialize(new LoadMathPojos(this));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> list = new ArrayList<>();

        try {
            String result = new MathEvaluator(query).eval().toPlainString();
            MathPojo pojo = new MathPojo();
            pojo.expressionValue = result;
            list.add(pojo);
            return list;
        } catch (MathEvaluator.ExpressionException ee) {
            ee.printStackTrace();
            return list;
        }
    }
}

