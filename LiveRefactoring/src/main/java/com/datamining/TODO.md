### TODO

- Improve the UI:
  - A tooltip to show all the python dependencies needed
  - Improve the size of the buttons and the text inputs
  - Option for local or remote repository in RepositoryMetricsExtraction
- Model Improvements:
  - Check for feature selection
  - Covariance matrix (EllipticEnvelope)


Validation:
  - Case Study: 1 hour thing where people use liveRef Sara and liveRef Diogo and compare it
  - Comparar com liveRef baseline:
    - 2 grupos: 1 com liveRef Sara e outro com meu liveRef
    - Case study com pessoas reais em projetos reais
    
2nd Thesis question (?The threshold way of doing refactorings is not that good for complex things?):
- Comparar abordagem com a Sara com dados que eu tenho de projetos que já usei para criar os meus dados
    - Usar DataCollection
    - Take the before refactoring data and calculate and save the metrics
    - Use Sara's tool (need to figure out script) on that data and make it use the best option it gives and then save the metrics from the results
    - Go to the after refactoring data (the real one) and save those metrics too
    - pattern: visitor pattern 

Future work:
- More data - script that uses high computing resources
- Incremental training (for the thesis future work section as it is very hard)
- Synchronize profiles between different machines (again, thesis future work section)
- Validate the tool with real projects over time (no time now)

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