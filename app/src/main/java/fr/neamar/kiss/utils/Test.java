package fr.neamar.kiss.utils;

/**
 * Created by hydroid7 on 27.06.16.
 */
public class Test {
    private MathEvaluator evaluator;

    public Test() {
        evaluator = new MathEvaluator("3 * sin (2)");
        System.out.println(evaluator.eval());
    }

    public static void main(String[] args) {
        new Test();
    }
}
