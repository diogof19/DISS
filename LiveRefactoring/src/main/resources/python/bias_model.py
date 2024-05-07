import pandas as pd
import numpy as np
import sys
import sqlite3
import warnings

from joblib import dump, load
from sklearn.base import clone

def bias_model(authors, model_path, data_path, scaler_path, bias, bias_multiplier):
    warnings.filterwarnings("ignore", category=UserWarning)
    
    conn = sqlite3.connect(data_path)
    cursor = conn.cursor()

    cursor.execute("SELECT author, numberLinesOfCodeBef, numberCommentsBef, " +
                   "numberBlankLinesBef, totalLinesBef, numParametersBef, " +
                   "numStatementsBef, halsteadLengthBef, halsteadVocabularyBef, " +
                   "halsteadVolumeBef, halsteadDifficultyBef, halsteadEffortBef, " +
                   "halsteadLevelBef, halsteadTimeBef, halsteadBugsDeliveredBef, " +
                   "halsteadMaintainabilityBef, cyclomaticComplexityBef, " +
                   "cognitiveComplexityBef, lackOfCohesionInMethodBef " +
                   "FROM metrics")
    rows = cursor.fetchall()
    conn.close()

    df = pd.DataFrame(rows, columns=[
        'author', 'numberLinesOfCodeBef', 'numberCommentsBef',
        'numberBlankLinesBef', 'totalLinesBef', 'numParametersBef',
        'numStatementsBef', 'halsteadLengthBef', 'halsteadVocabularyBef',
        'halsteadVolumeBef', 'halsteadDifficultyBef', 'halsteadEffortBef',
        'halsteadLevelBef', 'halsteadTimeBef', 'halsteadBugsDeliveredBef',
        'halsteadMaintainabilityBef', 'cyclomaticComplexityBef',
        'cognitiveComplexityBef', 'lackOfCohesionInMethodBef'
    ])

    old_model = load(model_path)
    model = clone(old_model)
    scaler = load(scaler_path)

    if bias:
        df['author'] = df['author'].apply(lambda x: 1 if x in authors else 0)

        sample_weights = np.where(df['author'] == 1, bias_multiplier, 1)
        X = df.drop(columns='author').values
        X = scaler.fit_transform(X)
        model.fit(X, sample_weight=sample_weights)
    else:
        X = df.drop(columns='author').values
        X = scaler.fit_transform(X)
        model.fit(X)

    dump(model, model_path)
    return

if __name__ == "__main__":
    model_path = sys.argv[1]
    scaler_path = sys.argv[2]
    data_path = sys.argv[3]
    bias_multiplier = sys.argv[4]

    bias = True
    if(sys.argv[5] == 'no_bias'):
        bias = False
    authors = sys.argv[5:]

    bias_model(authors, model_path, data_path, scaler_path, bias, bias_multiplier)