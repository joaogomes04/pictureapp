package org.eyeseetea.malariacare.monitor.rows;

import android.content.Context;

import org.eyeseetea.malariacare.R;
import org.eyeseetea.malariacare.monitor.utils.SurveyMonitor;

/**
 * Created by idelcano on 21/07/2016.
 */
public class TreatmentRowBuilder extends CounterRowBuilder {

    public TreatmentRowBuilder(Context context){
        super(context, context.getString(R.string.monitor_row_title_treatment));
    }
    @Override
    protected Integer incrementCount(SurveyMonitor surveyMonitor) {
        return (surveyMonitor.isTreatment())?1:0;
    }
}
