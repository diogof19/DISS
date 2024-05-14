### TODO

- Improve the UI:
  - A tooltip to show all the python dependencies needed
  - Improve the size of the buttons and the text inputs
  - Option for local or remote repository in RepositoryMetricsExtraction
- Model Improvements:
  - Check for feature selection
  - Covariance matrix (EllipticEnvelope)


Validation:
  - Compare with intellij Extract Method Refactoring
  - NEED TO FIND MORE STUFF -> maybe a collection of refactorings that already exist for testing purposes

Future work:
- More data - script that uses high computing resources
- Incremental training (for the thesis future work section as it is very hard)
- Synchronize profiles between different machines (again, thesis future work section)

Meeting:
  - Extract Method and Extract Class are done
  - UI still needs improvement, but everything is working
  - Where to use the data I have for Extract Method Before and After that's used in the model in the thesis?
    - Maybe in the evaluation/validation section
  - Does it make sense to do the script that uses high computing resources to collect data?
    - Which repositories should I use for this?
  - How to do validation?
    - Compare with intellij Extract Method Refactoring (I don't think it has Extract Class)
    - Perform refactorings manually and extract metrics from the before and after (may take a lot of time)
    




















