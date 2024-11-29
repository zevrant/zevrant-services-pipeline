package com.zevrant.services.pojo.codeunit

class PackerCodeUnitCollection {

    public static final List<PackerCodeUnit> packerImages = Collections.unmodifiableList([

    ])

    static PackerCodeUnit findCodeUnitByName(String name) {
        Optional<PackerCodeUnit> codeUnitOptional = packerImages.stream().filter { codeUnit -> (codeUnit.name == name) }.findFirst()
        if (codeUnitOptional.isEmpty()) {
            throw new RuntimeException("Packer Code Unit ${name} not found")
        }
        return codeUnitOptional.get()
    }
}
