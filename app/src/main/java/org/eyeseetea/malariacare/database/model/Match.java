/*
 * Copyright (c) 2015.
 *
 * This file is part of QIS Surveillance App.
 *
 *  QIS Surveillance App App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QIS Surveillance App App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QIS Surveillance App.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eyeseetea.malariacare.database.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.eyeseetea.malariacare.database.AppDatabase;
import org.eyeseetea.malariacare.utils.Constants;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;

import java.util.List;

/**
 * Created by Jose on 25/05/2015.
 */
@Table(databaseName = AppDatabase.NAME)
public class Match extends BaseModel {
    @Column
    @PrimaryKey(autoincrement = true)
    long id_match;

    @Column
    Long id_question_relation;
    /**
     * Reference to the associated questionRelation (loaded lazily)
     */
    QuestionRelation questionRelation;

    /**
     * List of questionOptions associated to this match
     */
    List<QuestionOption> questionOptions;

    public Match(){}

    public Match(QuestionRelation questionRelation){
        setQuestionRelation(questionRelation);
    }

    public List<QuestionOption> getQuestionOptions() {
        if(questionOptions==null){
            this.questionOptions = new Select().from(QuestionOption.class)
                    .indexedBy(Constants.QUESTION_OPTION_MATCH_IDX)
                    .where(Condition.column(QuestionOption$Table.ID_MATCH).eq(this.getId_match()))
                    .queryList();
        }
        return this.questionOptions;
    }

    public long getId_match() {
        return id_match;
    }

    public void setId_match(long id_match) {
        this.id_match = id_match;
    }

    public QuestionRelation getQuestionRelation() {
        if(questionRelation==null){
            if(id_question_relation==null) return null;
            questionRelation = new Select()
                    .from(QuestionRelation.class)
                    .where(Condition.column(QuestionRelation$Table.ID_QUESTION_RELATION)
                            .is(id_question_relation)).querySingle();
        }
        return questionRelation;
    }

    public void setQuestionRelation(QuestionRelation questionRelation) {
        this.questionRelation = questionRelation;
        this.id_question_relation = (questionRelation!=null)?questionRelation.getId_question_relation():null;
    }

    public void setQuestionRelation(Long id_question_relation){
        this.id_question_relation = id_question_relation;
        this.questionRelation = null;
    }

    /**
     * Returns the threshold associated with this questionoption
     * @return
     */
    public QuestionThreshold getQuestionThreshold(){
        //Find threshold with this match
        return new Select().from(QuestionThreshold.class)
                .where(Condition.column(Match$Table.ID_MATCH)
                        .is(id_match)).querySingle();
    }

    /**
     * Returns the question from QuestionRelation for this match with the given operationType
     * @param operationType
     * @return
     */
    public Question getQuestionFromRelationWithType(int operationType){
        QuestionRelation questionRelation = this.getQuestionRelation();
        if(questionRelation==null || questionRelation.getOperation()!=operationType){
            return null;
        }

        return questionRelation.getQuestion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Match match = (Match) o;

        if (id_match != match.id_match) return false;
        return !(id_question_relation != null ? !id_question_relation.equals(match.id_question_relation) : match.id_question_relation != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (id_match ^ (id_match >>> 32));
        result = 31 * result + (id_question_relation != null ? id_question_relation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Match{" +
                "id_match=" + id_match +
                ", id_question_relation=" + id_question_relation +
                '}';
    }

    public static List<Match> listAll() {
        return new Select().all().from(Match.class).queryList();
    }
}
