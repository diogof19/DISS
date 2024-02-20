package com.datamining;

import com.intellij.psi.PsiJavaFile;

public class FileInfo {
    private String methodName;
    private String className;
    private String fullClass;
    private String filePath;
    private PsiJavaFile file;

    public FileInfo(String methodName, String className, String fullClass, String filePath, PsiJavaFile file) {
        this.methodName = methodName;
        this.className = className;
        this.fullClass = fullClass;
        this.filePath = filePath;
        this.file = file;
    }

    public FileInfo(){
        this.methodName = null;
        this.className = null;
        this.fullClass = null;
        this.filePath = null;
        this.file = null;
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

    public PsiJavaFile getFile() {
        return file;
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

    public void setFile(PsiJavaFile file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "FileInfo{" + "methodName=" + methodName + ", className=" + className + ", fullClass=" + fullClass + ", filePath=" + filePath + ", file=" + file + '}';
    }
}
