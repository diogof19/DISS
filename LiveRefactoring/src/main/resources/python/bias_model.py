import pandas as pd
import numpy as np
import sys
import sqlite3
import warnings

from joblib import dump, load
from sklearn.base import clone

def bias_model(authors, data_path, old_EM_path, new_EM_path, scaler_EM_path, old_EC_path, new_EC_path, scaler_EC_path, bias, bias_multiplier):
    warnings.filterwarnings("ignore", category=UserWarning)
        
    conn = sqlite3.connect(data_path)
    
    query = '''
            SELECT author, numberLinesOfCodeBef, numberCommentsBef, numberBlankLinesBef, totalLinesBef, numParametersBef,
                numStatementsBef, halsteadLengthBef, halsteadVocabularyBef, halsteadVolumeBef, halsteadDifficultyBef,
                halsteadEffortBef, halsteadLevelBef, halsteadTimeBef, halsteadBugsDeliveredBef, halsteadMaintainabilityBef,
                cyclomaticComplexityBef, cognitiveComplexityBef, lackOfCohesionInMethodBef
            FROM methodMetrics;
    '''

    data_EM = pd.read_sql_query(query, conn)
    
    query = '''
        SELECT author, numProperties, numPublicAttributes, numPublicMethods, numProtectedFields, numProtectedMethods,
            numLongMethods, numLinesCode, lackOfCohesion, cyclomaticComplexity, cognitiveComplexity, numMethods,
            numConstructors, halsteadLength, halsteadVocabulary, halsteadVolume, halsteadDifficulty, halsteadEffort,
            halsteadLevel, halsteadTime, halsteadBugsDelivered, halsteadMaintainability
        FROM classMetrics;
    '''
    
    data_EC = pd.read_sql_query(query, conn)
    
    conn.close()

    old_EM_model = load(old_EM_path)
    EM_model = clone(old_EM_model)
    scaler_EM = load(scaler_EM_path)
    
    old_EC_model = load(old_EC_path)
    EC_model = clone(old_EC_model)
    scaler_EC = load(scaler_EC_path)

    if bias:
        data_EM['author'] = data_EM['author'].apply(lambda x: 1 if x in authors else 0)
        sample_weights = np.where(data_EM['author'] == 1, bias_multiplier, 1)
        X = data_EM.drop(columns='author').values
        X = scaler_EM.fit_transform(X)
        EM_model.fit(X, sample_weight=sample_weights)
        
        data_EC['author'] = data_EC['author'].apply(lambda x: 1 if x in authors else 0)
        sample_weights = np.where(data_EC['author'] == 1, bias_multiplier, 1)
        X = data_EC.drop(columns='author').values
        X = scaler_EC.fit_transform(X)
        EC_model.fit(X, sample_weight=sample_weights)
    else:
        X = data_EM.drop(columns='author').values
        X = scaler_EM.fit_transform(X)
        EM_model.fit(X)
        
        X = data_EC.drop(columns='author').values
        X = scaler_EC.fit_transform(X)
        EC_model.fit(X)

    dump(EM_model, new_EM_path)
    dump(EC_model, new_EC_path)
    return

if __name__ == "__main__":
    old_EM_path = sys.argv[1]
    new_EM_path = sys.argv[2]
    scaler_EM_path = sys.argv[3]
    old_EC_path = sys.argv[4]
    new_EC_path = sys.argv[5]
    scaler_EC_path = sys.argv[6]
    data_path = sys.argv[7]
    bias_multiplier = sys.argv[8]

    bias = True
    if(sys.argv[9] == 'no_bias'):
        bias = False
    
    authors = []
    for arg in sys.argv[9:]:
        authors.append(int(arg))

    bias_model(authors, data_path, old_EM_path, new_EM_path, scaler_EM_path, old_EC_path, new_EC_path, scaler_EC_path, bias, bias_multiplier)