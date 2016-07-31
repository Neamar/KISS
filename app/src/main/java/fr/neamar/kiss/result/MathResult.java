package fr.neamar.kiss.result;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.MathPojo;

public class MathResult extends Result {

    private final MathPojo mathPojo;

    public MathResult(MathPojo pojo) {
        this.pojo = this.mathPojo = pojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_math);

        TextView txt = (TextView) v.findViewById(R.id.item_math_result);
        txt.setText(mathPojo.expressionValue);

        return v;
    }

    @Override
    protected void doLaunch(Context context, View v) {

    }
}
