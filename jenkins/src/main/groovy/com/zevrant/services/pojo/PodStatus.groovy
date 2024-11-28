package com.zevrant.services.pojo

enum PodStatus {
    NONE("''"),//Will return literal '' so grep has a paramter
    PENDING('Pending'),
    CONTAINER_CREATING('ContainerCreating'),
    RUNNING('Running'),
    TERMINATING('Terminating');

    final String statusName

    PodStatus(String statusName) {
        this.statusName = statusName
    }
}
