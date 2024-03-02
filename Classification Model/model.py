from sklearn.svm import OneClassSVM
from sklearn2pmml import sklearn2pmml
import pickle

X = [[0], [0.44], [0.45], [0.46], [1]]
#clf = OneClassSVM(gamma='auto').fit(X)

""" clf.predict(X)
clf.score_samples(X) """

from sklearn2pmml.pipeline import PMMLPipeline

pipeline = PMMLPipeline([
	("classifier", OneClassSVM(gamma='auto'))
])
pipeline.fit(X)
    
sklearn2pmml(pipeline, "pipeline.pmml", with_repr = True)