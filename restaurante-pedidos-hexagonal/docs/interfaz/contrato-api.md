# Contrato de la API REST

Contrato entre los servicios y la interfaz web (`gateway/src/main/resources/web/`).
La interfaz solo habla con el gateway (puerto 8080), que reenvía cada ruta al servicio que la atiende;
por eso las rutas no cambiaron al separar el sistema en servicios.
**Cualquier cambio se acuerda entre las dos partes antes de implementarlo.**

## Convenciones
- Mismo origen, prefijo `/api`, JSON en UTF-8.
- **El dinero y las tasas viajan como string** con punto decimal: `"85.50"`, `"0.16"`.
  - Respuestas: el dinero siempre lleva 2 decimales (`"85.00"`); las tasas, sin ceros sobrantes (`"0.16"`, `"0"`).
  - Peticiones: se mandan como string. El backend también acepta números (`85.5`). En descuento y propina,
    un valor vacío o ausente equivale a 0.
- Los enums viajan por su nombre: `"SALA"`, `"EN_COCCION"`, etc.
- Fechas en ISO local sin zona y siempre con segundos: `"2026-10-20T13:45:00"`.
- Errores: el cuerpo siempre es `{ "error": "mensaje para el usuario" }`.
  - `400`: regla de negocio violada, recurso inexistente (p. ej. "No existe la orden X") o petición mal formada.
  - `404`: ruta inexistente.
  - `500`: error interno.
  - `503`: el servicio que atiende la ruta (o uno del que depende, p. ej. Órdenes sin el Menú) no responde.

## Enums
| Enum | Valores |
|---|---|
| TipoOrden | `SALA`, `PARA_LLEVAR`, `DELIVERY` |
| EstadoOrden | `ABIERTA`, `CERRADA`, `CANCELADA` |
| EstadoCocina | `PENDIENTE`, `EN_COCCION`, `LISTO`, `CANCELADA` |
| TipoModificador | `EXTRA`, `EXCLUSION` |
| MetodoPago | `EFECTIVO`, `TARJETA`, `PASARELA_DIGITAL` |
| EstadoPago | `PENDIENTE`, `APROBADO`, `RECHAZADO` |

## Objetos (DTO)
```jsonc
PlatoDto        { "id": "P1", "nombre": "Tacos", "precioBase": "85.00", "tasaImpuesto": "0.16" }
ModificadorDto  { "id": "M1", "nombre": "Queso extra", "tipo": "EXTRA", "costoExtra": "10.00" }
TotalesDto      { "subtotal": "285.00", "descuento": "19.00", "impuestos": "42.56", "propina": "10.00", "total": "318.56" }
PagoDto         { "id": "uuid", "ordenId": "uuid", "fecha": "2026-10-20T13:45:00", "estado": "APROBADO",
                  "metodo": "TARJETA", "monto": "318.56", "referencia": "TAR-1a2b3c4d" }
LineaDto        { "id": "uuid", "platoId": "P1", "platoNombre": "Tacos", "cantidad": 3,
                  "precioUnitario": "85.00", "tasaImpuesto": "0.16",
                  "modificadores": [ ModificadorDto ],   // costoExtra = el que se aplicó al agregar la línea
                  "estadoCocina": "PENDIENTE", "editable": true }
OrdenDto        { "id": "uuid", "tipo": "SALA", "referencia": "Mesa 5", "estado": "ABIERTA",
                  "fechaCreacion": "2026-10-20T13:40:00",
                  "lineas": [ LineaDto ],                // incluye las CANCELADAS
                  "totales": TotalesDto,                 // CERRADA: los cobrados; si no: sin descuento ni propina
                  "pagos": [ PagoDto ] }                 // solo el aprobado; los intentos rechazados los guarda Caja
LineaCocinaDto  { "ordenId": "uuid", "lineaId": "uuid", "referenciaOrden": "Mesa 5", "plato": "Tacos",
                  "cantidad": 3, "modificadores": ["Queso extra"], "estado": "EN_COCCION" }
```

## Endpoints
| Método | Ruta | Cuerpo | Respuesta | Servicio |
|---|---|---|---|---|
| GET | `/api/platos` | | 200 `PlatoDto[]` ordenados por nombre | Menú (8081) |
| POST | `/api/platos` | `{id, nombre, precioBase, tasaImpuesto}` | 201 `PlatoDto` | Menú (8081) |
| PUT | `/api/platos/{platoId}` | `{nombre, precioBase, tasaImpuesto}` | 200 `PlatoDto` | Menú (8081) |
| DELETE | `/api/platos/{platoId}` | | 204 | Menú (8081) |
| GET | `/api/modificadores` | | 200 `ModificadorDto[]` ordenados por nombre | Menú (8081) |
| POST | `/api/modificadores` | `{id, nombre, tipo, costoExtra}` | 201 `ModificadorDto` | Menú (8081) |
| PUT | `/api/modificadores/{modificadorId}` | `{nombre, tipo, costoExtra}` | 200 `ModificadorDto` | Menú (8081) |
| DELETE | `/api/modificadores/{modificadorId}` | | 204 | Menú (8081) |
| GET | `/api/ordenes?estado=ABIERTA` (`estado` es opcional) | | 200 `OrdenDto[]`, de la más antigua a la más reciente | Órdenes (8082) |
| GET | `/api/ordenes/{ordenId}` | | 200 `OrdenDto` | Órdenes (8082) |
| POST | `/api/ordenes` | `{tipo, referencia}` | 201 `OrdenDto` | Órdenes (8082) |
| POST | `/api/ordenes/{ordenId}/lineas` | `{platoId, cantidad, modificadorIds: []}` | 200 `OrdenDto` | Órdenes (8082) |
| PUT | `/api/ordenes/{ordenId}/lineas/{lineaId}` | `{cantidad, modificadorIds: []}` | 200 `OrdenDto` | Órdenes (8082) |
| DELETE | `/api/ordenes/{ordenId}/lineas/{lineaId}` | | 200 `OrdenDto` (la línea queda CANCELADA) | Órdenes (8082) |
| POST | `/api/ordenes/{ordenId}/cancelar` | | 200 `OrdenDto` | Órdenes (8082) |
| GET | `/api/cocina/pendientes` | | 200 `LineaCocinaDto[]` (PENDIENTE y EN_COCCION de órdenes sin cobrar) | Cocina (8083) |
| POST | `/api/cocina/ordenes/{ordenId}/lineas/{lineaId}/iniciar` | | 204 | Cocina (8083) |
| POST | `/api/cocina/ordenes/{ordenId}/lineas/{lineaId}/lista` | | 204 | Cocina (8083) |
| GET | `/api/caja/ordenes/{ordenId}/totales?descuento=19&propina=10` | | 200 `TotalesDto` | Caja (8084) |
| GET | `/api/caja/ordenes/{ordenId}/pagos` | | 200 `PagoDto[]` (todos los intentos, también los rechazados) | Caja (8084) |
| POST | `/api/caja/ordenes/{ordenId}/cobrar` | `{metodo, descuento, propina}` | 200 `PagoDto` (APROBADO **o** RECHAZADO) | Caja (8084) |

Los cambios entre servicios no pasan por estas rutas sino por el bus de eventos (8090). Cuando un
servicio se entera por un evento, el cambio tarda unos milisegundos en verse en el otro: por ejemplo,
justo después de cobrar, `GET /api/ordenes/{id}` puede responder todavía `ABIERTA`.

`cantidad` es un entero. `modificadorIds` es un arreglo de ids (puede ir vacío u omitirse).

## Ejemplos rápidos (curl)
```bash
curl localhost:8080/api/platos
curl -X POST localhost:8080/api/ordenes -d '{"tipo":"SALA","referencia":"Mesa 5"}'
curl -X POST localhost:8080/api/ordenes/<ordenId>/lineas -d '{"platoId":"P-001","cantidad":2,"modificadorIds":["M-001"]}'
curl "localhost:8080/api/caja/ordenes/<ordenId>/totales?descuento=10&propina=5"
curl -X POST localhost:8080/api/caja/ordenes/<ordenId>/cobrar -d '{"metodo":"TARJETA","descuento":"10","propina":"5"}'
```
