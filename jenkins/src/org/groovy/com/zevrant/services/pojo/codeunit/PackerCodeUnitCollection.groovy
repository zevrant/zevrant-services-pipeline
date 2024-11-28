package com.zevrant.services.pojo.codeunit

class PackerCodeUnitCollection {

    public static final List<PackerCodeUnit> packerImages = Collections.unmodifiableList([

    ])

    static PackerCodeUnit findCodeUnitByName(String name) {
        Optional<PackerCodeUnit> codeUnit = packerImages.stream().filter {codeUnit -> (codeUnit.name == name) }.findFirst()
        return codeUnit.orElseThrow { () -> new RuntimeException("Packer Code Unit ${name} not found")}
    }
}
