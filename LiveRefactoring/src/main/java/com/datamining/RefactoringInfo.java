package com.datamining;

import com.intellij.psi.PsiJavaFile;
import org.bson.types.ObjectId;

public class RefactoringInfo {
    private ObjectId _id;
    private String methodName;
    private String className;
    private String fullClass;
    private String filePath;
    private PsiJavaFile beforeFile;
    private PsiJavaFile afterFile;

    public RefactoringInfo(ObjectId _id ,String methodName, String className, String fullClass, String filePath, PsiJavaFile beforeFile, PsiJavaFile afterFile) {
        this._id = _id;
        this.methodName = methodName;
        this.className = className;
        this.fullClass = fullClass;
        this.filePath = filePath;
        this.beforeFile = beforeFile;
        this.afterFile = afterFile;
    }

    public RefactoringInfo(){
        this._id = null;
        this.methodName = null;
        this.className = null;
        this.fullClass = null;
        this.filePath = null;
        this.beforeFile = null;
        this.afterFile = null;
    }

    public ObjectId get_id() {
        return _id;
    }
    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getFullClass() {
        return fullClass;
    }

    public String getFilePath() {
        return filePath;
    }

    public PsiJavaFile getBeforeFile() {
        return beforeFile;
    }

    public PsiJavaFile getAfterFile() {
        return afterFile;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setFullClass(String fullClass) {
        this.fullClass = fullClass;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setBeforeFile(PsiJavaFile file) {
        this.beforeFile = file;
    }

    public void setAfterFile(PsiJavaFile file) {
        this.afterFile = file;
    }

    @Override
    public String toString() {
        return "FileInfo{" + " id=" + _id.toString() + "methodName=" + methodName + ", className=" + className + ", fullClass=" + fullClass + ", filePath=" + filePath + ", beforeFile=" + beforeFile + ", afterFile=" + afterFile +'}';
    }
}
