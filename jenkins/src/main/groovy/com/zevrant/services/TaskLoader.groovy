package com.zevrant.services


import com.zevrant.services.services.VersionTasks

class TaskLoader {
    static List<Class> taskClassList() {
        [
                // NOTE: New tasks must be added here
                VersionTasks
        ]
    }

    static String classVariableName(Class clazz) {
        String variableName = clazz.name.split(/\./)[-1]
        return variableName[0].toLowerCase() + variableName[1..-1]
    }

    static Object load(Binding binding, Class clazz) {
        String preloadVariable = "preload_${classVariableName(clazz)}" // These are loaded by baseFcsPipelineTest

        if (binding.hasVariable(preloadVariable)) {
            return binding.getVariable(preloadVariable)
        } else {
            return clazz.classLoader.loadClass(clazz.name).getConstructor().newInstance()
        }
    }

    /**
     * Given a binding, loads all of the classes instantiated in taskClassList with auto-generated names into the
     * binding provided
     * @param binding
     */
    static void loadAll(Binding binding) {
        taskClassList().each { Class clazz ->
            binding.setVariable(classVariableName(clazz), load(binding, clazz))
        }
    }
}
