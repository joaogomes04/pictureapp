package org.eyeseetea.malariacare.monitor.rows;

import android.content.Context;

import org.eyeseetea.malariacare.R;
import org.eyeseetea.malariacare.monitor.utils.SurveyMonitor;

/**
 * Created by idelcano on 21/07/2016.
 */
public class ACTStockoutRowBuilder extends CounterRowBuilder {

    public ACTStockoutRowBuilder(Context context){
        super(context, context.getString(R.string.monitor_row_title_act_stockout));
    }

    @Override
    protected Integer incrementCount(SurveyMonitor surveyMonitor) {
        return (surveyMonitor.isACTStockout())?1:0;
    }
}
