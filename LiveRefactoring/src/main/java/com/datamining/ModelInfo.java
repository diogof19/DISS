package com.datamining;

public class ModelInfo {
    private String name;
    private String pathEM;
    private String pathEC;
    private boolean selected;

    public ModelInfo(String name, String pathEM, String pathEC, boolean selected){
        this.name = name;
        this.pathEM = pathEM;
        this.pathEC = pathEC;
        this.selected = selected;
    }

    public String getName(){
        return name;
    }

    public String getPathEM(){
        return pathEM;
    }

    public String getPathEC(){
        return pathEC;
    }

    public boolean isSelected(){
        return selected;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setPathEM(String pathEM){
        this.pathEM = pathEM;
    }

    public void setPathEC(String pathEC){
        this.pathEC = pathEC;
    }

    public void setSelected(boolean selected){
        this.selected = selected;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModelInfo modelInfo = (ModelInfo) obj;
        return name.equals(modelInfo.name);
    }

}
