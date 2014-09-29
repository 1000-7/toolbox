package eu.amidst.staticModelling.learning;


import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.database.statics.DataStream;
import eu.amidst.staticModelling.models.LearnableModel;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public interface LearningAlgorithm {

    public void setLearnableModel(LearnableModel model);

    public void initLearning();

    public void updateModel(DataInstance data);

    public void learnModelFromStream(DataStream data);

}
