@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    allowedDependencies = {
        "clientes",
        "oportunidades",
        "actividades",
        "propuestas",
        "productos",
        "usuarios",
        "shared",
        "shared.exception"
    }
)
package com.viernes.relationshipmap;
