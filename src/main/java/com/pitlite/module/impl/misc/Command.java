package com.pitlite.module.impl.misc;

import com.pitlite.module.Category;
import com.pitlite.module.Module;

public class Command extends Module {
    public Command() {
        super("Command", "Enables custom client commands starting with '.'", Category.MISC);
        setToggledFromConfig(true);
    }
}
