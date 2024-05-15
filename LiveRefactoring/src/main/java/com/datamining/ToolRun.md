
1. StartTool.java (82 - 87):\
   Initialize metrics on the file and start the refactoring analysis
    ```java
       System.out.print("\n=========== New Event (After) ===========\n");
       Values.after = new FileMetrics(Values.editor, (PsiJavaFile) psiFile);
       Values.isFirst = false;
       active = 0;
       lastActive = 0;
       utils.startActions((PsiJavaFile) psiFile);
    ```
2. Utilities.java (startActions) -> Candidates.java (getCandidates)\
   Initialize metric files and select which refactorings to find\
   **NEED TO MAKE SURE TO ONLY RUN THE EXTRACT METHOD ON THIS PART**
   1. Candidates.java (getCandidates -> createThreads) -> ExtractMethod.java (run)\
         Find the candidate and save them in **Values.extractMethod**
         ```java
         Values.extractMethod = new ArrayList<>();
         streams.forEachOrdered(Values.extractMethod ::add);
         System.out.println("Extract Method Candidates: " + Values.extractMethod.size());
         ```
3. Candidates.java (getCandidates -> continueAnalysis)\
   Gets the severities for the candidates\
   **I can probably order the severities by _Severity.severity_ to get the most important one, especially since there's only going to be Extract Method refactorings**
   ```java
   Values.candidates = getSeverities();
   ```
   Then saves the candidates on a map using the file name as a key - Map: **Values.openedRefactorings**\
   Lastly, it displays the refactorings on the UI
4. **AFTER DISPLAYED**\
   VisualRepresentation.java (startVisualAnalysis) -> ColoringGutters.java (initGutters)\
   Initializes the gutters
5. ColoringGutters.java (initGutters -> getClickAction)\
   What happens after clicking on the gutter:
   ```java
   if (candidate instanceof ExtractMethodCandidate) {
                            ExtractMethod extractMethod = new ExtractMethod(editor);
                            extractMethod.extractMethod((ExtractMethodCandidate) candidate, severity.severity, severity.indexColorGutter);
   ```
   This means I need, for an Extract Method to happen, the 'editor', 'ExtractMethodCandidate', 'severity'\
6. ColoringGutters.java (getClickAction) -> ExtractMethod.java (extractMethod)\
   Extract Method refactoring happens here, and I'm saving the metrics to the database


Script:
1. DataCollection from start to finish
2. On extractMetrics, before saving to the database, but after the null checks, invoke the plugin on the before file:
   1. SelectedRefactorings.selectedRefactoring = Refactorings.ExtractMethod;\
      SelectedRefactorings.selectedRefactorings.add(Refactorings.ExtractMethod);
   2. Values.editor = ((TextEditor) FileEditorManager.getInstance(e.getProject()).getSelectedEditor()).getEditor();
      Call utils.startActions((PsiJavaFile) psiFile) using the beforeFile
   3. Candidates are stored in Values.candidates
      I can get the candidate with the highest severity
   4. Then call ExtractMethod extractMethod = new ExtractMethod(editor);
      extractMethod.extractMethod((ExtractMethodCandidate) candidate, severity.severity, severity.indexColorGutter);