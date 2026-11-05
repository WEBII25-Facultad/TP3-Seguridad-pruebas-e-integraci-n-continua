package ar.edu.unvime.tp1favoritos.favoritos;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoRequest;
import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoResponse;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "favoritos", description = "CRUD de favoritos persistidos en PostgreSQL (Requiere autenticación obligatoria con JWT)")
@SecurityRequirement(name = "bearerAuth")
public class FavoritoController {

    private final FavoritoService service;

    public FavoritoController(FavoritoService service) {
        this.service = service;
    }

    @Operation(
            summary = "Crear un favorito",
            description = "Registra un nuevo producto como favorito dentro de una lista. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Favorito creado exitosamente"),
                    @ApiResponse(responseCode = "400", description = "Error de validación"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "404", description = "La lista indicada no existe")
            }
    )
    @PostMapping
    public ResponseEntity<FavoritoResponse> crear(@Valid @RequestBody FavoritoRequest request) {
        FavoritoResponse creado = service.crear(request);
        return ResponseEntity
                .created(URI.create("/api/favoritos/" + creado.id()))
                .body(creado);
    }

    @Operation(
            summary = "Listar todos los favoritos",
            description = "Obtiene la colección de todos los favoritos registrados. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Operación exitosa"),
                    @ApiResponse(responseCode = "401", description = "No autenticado")
            }
    )
    @GetMapping
    public List<FavoritoResponse> listar() {
        return service.listar();
    }

    @Operation(
            summary = "Obtener un favorito por id",
            description = "Busca un favorito específico por su identificador. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Favorito encontrado"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "404", description = "Favorito no encontrado")
            }
    )
    @GetMapping("/{id}")
    public FavoritoResponse obtenerPorId(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @Operation(
            summary = "Actualizar un favorito",
            description = "Modifica un favorito existente. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Favorito actualizado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Error de validación"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "404", description = "Favorito o lista no encontrados")
            }
    )
    @PutMapping("/{id}")
    public FavoritoResponse actualizar(@PathVariable Long id, @Valid @RequestBody FavoritoRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(
            summary = "Eliminar un favorito",
            description = "Elimina un favorito existente. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Favorito eliminado correctamente"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "404", description = "Favorito no encontrado")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
