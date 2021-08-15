package com.zevrant.services

class GenericPipelineTriggerVariable {

    final String key
    final String expressionValue
    final TriggerVariableType triggerVariableType
    final String defaultValue
    final String regexpFilter

    GenericPipelineTriggerVariable(Map<String, Object> params) {
        key = params.key
        expressionValue = params.expressionValue
        triggerVariableType = params.triggerVariableType as TriggerVariableType
        regexpFilter = params.regexpFilter
        defaultValue = params.defaultValue ?: ""
    }
}
