package com.zevrant.services.pojo

class PipelineParameter<T> {

    private Class<T> type;
    private String name;
    private String description;
    private T defaultValue;

    PipelineParameter(Class<T> type, String name, String description, T defaultValue) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    String getDescription() {
        return description
    }

    void setDescription(String description) {
        this.description = description
    }

    T getDefaultValue() {
        return defaultValue
    }

    void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue
    }
}
