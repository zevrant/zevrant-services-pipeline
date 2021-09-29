package com.zevrant.services.pojo

import com.zevrant.services.PipelineTriggerType

class PipelineTrigger {

    final PipelineTriggerType type
    final String value
    final String token
    final List<GenericPipelineTriggerVariable> variables

    PipelineTrigger(Map<String, Object> params) {
        this.type = params.type as PipelineTriggerType
        this.value = params.value
        this.token = params.token
        this.variables = params.variables as ArrayList<GenericPipelineTriggerVariable>
    }
}
