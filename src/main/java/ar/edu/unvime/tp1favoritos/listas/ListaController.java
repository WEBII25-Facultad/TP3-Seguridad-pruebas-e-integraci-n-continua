package ar.edu.unvime.tp1favoritos.listas;

import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoResponse;
import ar.edu.unvime.tp1favoritos.listas.dto.ListaRequest;
import ar.edu.unvime.tp1favoritos.listas.dto.ListaResponse;
import ar.edu.unvime.tp1favoritos.listas.dto.MoverFavoritosRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/listas")
@Tag(name = "listas", description = "Gestión de listas para organizar favoritos (Requiere autenticación obligatoria con JWT)")
@SecurityRequirement(name = "bearerAuth")
public class ListaController {

    private final ListaService service;

    public ListaController(ListaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Crear una nueva lista",
            description = "Crea una lista asociada automáticamente al usuario autenticado. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Lista creada exitosamente"),
                    @ApiResponse(responseCode = "400", description = "Error de validación"),
                    @ApiResponse(responseCode = "401", description = "No autenticado")
            }
    )
    public ResponseEntity<ListaResponse> crear(@Valid @RequestBody ListaRequest request) {
        ListaResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/listas/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar todas las listas",
            description = "Devuelve las listas del usuario autenticado (o todas si tiene rol ADMIN). Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Operación exitosa"),
                    @ApiResponse(responseCode = "401", description = "No autenticado")
            }
    )
    public ResponseEntity<List<ListaResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener una lista por su ID",
            description = "Busca una lista por ID. Solo el dueño o un ADMIN pueden acceder. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista encontrada"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado (no es dueño ni ADMIN)"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada")
            }
    )
    public ResponseEntity<ListaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping("/{id}/favoritos")
    @Operation(
            summary = "Obtener los favoritos de una lista",
            description = "Devuelve los favoritos asociados a la lista indicada si pertenece al usuario o es ADMIN. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Favoritos encontrados"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado (no es dueño ni ADMIN)"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada")
            }
    )
    public ResponseEntity<List<FavoritoResponse>> favoritosDeLista(@PathVariable Long id) {
        return ResponseEntity.ok(service.favoritosDeLista(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar una lista por su ID",
            description = "Elimina la lista si pertenece al usuario (o es ADMIN) y está vacía. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Lista eliminada correctamente"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado (no es dueño ni ADMIN)"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada"),
                    @ApiResponse(responseCode = "409", description = "La lista todavía tiene favoritos")
            }
    )
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{origenId}/mover-favoritos")
    @Operation(
            summary = "Mover favoritos de una lista a otra y eliminar la original",
            description = "Mueve favoritos entre listas propias (o con rol ADMIN) y elimina la lista origen. Requiere token JWT Bearer.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Favoritos movidos correctamente"),
                    @ApiResponse(responseCode = "400", description = "La lista origen y destino son la misma"),
                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado sobre alguna de las listas"),
                    @ApiResponse(responseCode = "404", description = "Lista origen o destino no encontrada")
            }
    )
    public ResponseEntity<Void> moverFavoritos(@PathVariable Long origenId, @Valid @RequestBody MoverFavoritosRequest request) {
        service.moverFavoritos(origenId, request);
        return ResponseEntity.noContent().build();
    }
}
