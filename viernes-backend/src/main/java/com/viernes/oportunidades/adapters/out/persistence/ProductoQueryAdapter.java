package com.viernes.oportunidades.adapters.out.persistence;

import com.viernes.oportunidades.application.port.out.ProductoQueryPort;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.application.port.in.ManageProductoUseCase;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductoQueryAdapter implements ProductoQueryPort {

    private final ManageProductoUseCase productoUseCase;

    @Override
    public Optional<ProductoResponse> buscarPorId(Long idProducto) {
        try {
            return Optional.of(productoUseCase.obtener(idProducto));
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
    }
}
