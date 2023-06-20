package com.zevrant.services

import com.zevrant.services.services.GitService
import com.zevrant.services.services.KubernetesService
import com.zevrant.services.services.PostgresYamlConfigurer
import com.zevrant.services.services.VersionService
//import com.zevrant.services.services.GooglePlayService
class ServiceLoader {
    static List<Class> taskClassList() {
        [
                // NOTE: New tasks must be added here
                VersionService,
                GitService,
                KubernetesService,
                PostgresYamlConfigurer
//                GooglePlayService
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
}
