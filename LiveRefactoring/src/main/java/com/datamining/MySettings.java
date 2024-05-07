package com.datamining;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "MySettings", storages = @Storage(StoragePathMacros.CACHE_FILE))
public class MySettings implements PersistentStateComponent<MySettings.State> {
    private State myState = new State();

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public static class State {
        public int counter = 0;
        public String pythonPath = "";
        public int maxExtractMethodsBefUpdate = 10;
        public int biasMultiplier = 10;
    }


}
