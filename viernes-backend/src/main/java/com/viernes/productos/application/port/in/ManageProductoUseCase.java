package com.viernes.productos.application.port.in;

import com.viernes.productos.application.dto.ProductoRequest;
import com.viernes.productos.application.dto.ProductoResponse;
import com.viernes.productos.domain.EstadoProducto;
import java.util.List;

public interface ManageProductoUseCase {
    List<ProductoResponse> listar();
    List<ProductoResponse> buscar(String q);
    List<ProductoResponse> listarPorEstado(EstadoProducto estado);
    ProductoResponse obtener(Long id);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
}
