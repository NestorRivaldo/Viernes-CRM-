package com.viernes.oportunidades.application.port.out;

import com.viernes.productos.application.dto.ProductoResponse;
import java.util.Optional;

public interface ProductoQueryPort {

    Optional<ProductoResponse> buscarPorId(Long idProducto);
}
