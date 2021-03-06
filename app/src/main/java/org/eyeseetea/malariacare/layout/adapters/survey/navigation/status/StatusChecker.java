package org.eyeseetea.malariacare.layout.adapters.survey.navigation.status;

import org.eyeseetea.malariacare.database.model.Question;
import org.eyeseetea.malariacare.database.model.QuestionOption;
import org.eyeseetea.malariacare.database.model.QuestionRelation;

import java.util.List;

/**
 * Base class that helps a QuestionNode to decide whether isEnabled (visitable) or
 * can be seen during a previous/review action.
 * Created by arrizabalaga on 13/07/16.
 */
public class StatusChecker {
    /**
     * By default every node is enabled
     * @return
     */
    public boolean isEnabled(){
        return true;
    }

    /**
     * By default every node is visible during review
     * @return
     */
    public boolean isVisibleInReview(){
        return true;
    }

    /**
     * Navigates from questionRelation -> match -> questionOption
     * @param questionRelation
     * @return
     */
    protected QuestionOption findQuestionOption(QuestionRelation questionRelation){
        if(questionRelation==null){
            return null;
        }
        List<QuestionOption> questionOptions = QuestionOption.findByQuestionRelation(questionRelation);
        if(questionOptions.isEmpty()){
            return null;
        }

        return questionOptions.get(0);
    }
}
