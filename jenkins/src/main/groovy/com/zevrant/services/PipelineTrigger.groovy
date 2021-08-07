package com.zevrant.services

class PipelineTrigger {

    private final PipelineTriggerType type;
    private final String value;

    PipelineTrigger(PipelineTriggerType type, String value) {
        this.type = type
        this.value = value
    }
}
