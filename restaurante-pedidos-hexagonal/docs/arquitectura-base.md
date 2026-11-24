# Arquitectura (hexagonal, separada en servicios)

El sistema empezó como una sola aplicación hexagonal y se partió en servicios. Cada servicio
conserva la misma organización por capas; lo que cambió es que ya no comparten memoria: se
consultan por REST o se avisan por eventos.

## Servicios

| Módulo Maven | Servicio | Puerto | Actor | RF | Guarda |
|---|---|---|---|---|---|
| `servicio-menu` | Menú | 8081 | Administrador | RF1 | platos y modificadores |
| `monolito` | Órdenes | 8082 | Mesero | RF2, RF3 | las órdenes (la versión oficial) |
| `servicio-cocina` | Cocina | 8083 | Cocinero | RF3 | sus líneas de cocina |
| `servicio-caja` | Caja | 8084 | Cajero | RF4, RF5 | su copia de cada orden y los intentos de pago |
| `bus-eventos` | Bus de eventos | 8090 | — | — | quién está suscrito a qué evento |
| `gateway` | API Gateway | 8080 | — | — | nada: reenvía `/api/...` y sirve la interfaz web |
| `comun` | (librería) | — | — | — | dominio compartido, servidor HTTP y piezas de eventos |

`Orden`, `LineaPedido`, `Plato` y `Modificador` viven en `comun` porque los usan Órdenes y Caja
(Caja rearma la orden con los eventos para calcular los totales con las mismas reglas).

## Capas (`src/main/java/com/restaurante` en cada módulo)

| Paquete | Contenido | Puede depender de |
|---|---|---|
| `domain` | Entidades y reglas de negocio (`Orden`, `LineaPedido`, `Plato`, `Modificador`, `Pago`...) | nada |
| `application.port.in` | Puertos de entrada: un caso de uso por actor y los que reciben eventos | `domain` |
| `application.port.out` | Puertos de salida: repositorios, catálogo del menú y procesador de pago | `domain` |
| `application.service` | Implementaciones de los casos de uso | `domain`, `application.port` |
| `infrastructure` | Adaptadores: REST, consola, cliente del Menú, eventos, persistencia en memoria, pagos; `config` arma cada servicio | `application`, `domain` |

**Regla de dependencias:** todo apunta hacia el dominio. El dominio no importa nada de
`application` ni de `infrastructure`, y `application` no importa nada de `infrastructure`.
Un servicio tampoco depende del código de otro: solo de `comun`.

## Componentes por servicio

| Servicio | Puerto de entrada | Implementación | Puertos de salida y adaptadores |
|---|---|---|---|
| Menú | `AdministrarMenuUseCase` | `AdministrarMenuService` | `PlatoRepository`, `ModificadorRepository` → `*RepositoryMemoria` |
| Órdenes | `GestionarOrdenUseCase`, `RegistrarAvanceCocinaUseCase`, `RegistrarCobroUseCase` | `GestionarOrdenService` | `OrdenRepository` → `OrdenRepositoryMemoria`; `CatalogoMenu` → `MenuRestClient` (REST al Menú); `PublicadorEventos` → `ClienteBusEventos` |
| Cocina | `GestionarCocinaUseCase`, `RecibirPedidosUseCase` | `GestionarCocinaService` | `LineaCocinaRepository` → `LineaCocinaRepositoryMemoria`; `PublicadorEventos` |
| Caja | `CobrarOrdenUseCase`, `RecibirOrdenesUseCase` | `CobrarOrdenService` | `OrdenCajaRepository` → `OrdenCajaRepositoryMemoria`; `ProcesadorPago` → `PagoEfectivoAdapter`, `PagoTarjetaSimuladoAdapter`, `PagoPasarelaSimuladoAdapter`; `PublicadorEventos` |

### Adaptadores de entrada

| Adaptador | Clases | Dónde |
|---|---|---|
| REST | `MenuRestController`, `OrdenRestController`, `CocinaRestController`, `CajaRestController`, servidos por `ServidorApi` (de `comun`) | uno en cada servicio |
| Eventos | `ReceptorEventosController` (`POST /api/eventos/recibir`): pasa cada evento al caso de uso que lo atiende | Órdenes, Cocina y Caja |
| Consola | `infrastructure.in.cli.MenuConsola`: solo las opciones del mesero; las demás avisan en qué servicio están | Órdenes |

- Cada servicio tiene su raíz de composición (`AplicacionMenu`, `Aplicacion`, `AplicacionCocina`,
  `AplicacionCaja`) y su `Main...` con la opción `--puerto=N` (y `--bus=URL` los que usan eventos).
- El servidor HTTP es el del JDK (`com.sun.net.httpserver`) y el JSON se hace con Gson.
  Las piezas compartidas están en `comun`, paquete `infrastructure.in.web.http`: `Enrutador`,
  `Peticion`, `Respuesta`, `ApiHandler` (traduce `ReglaDeNegocioException` a 400 y
  `ServicioNoDisponibleException` a 503), `Json`, `Conversion` y `Formato`.
- El gateway (`RutasGateway`, `ReenviadorHandler`) decide a qué servicio va cada ruta por su prefijo
  y sirve la interfaz web desde `gateway/src/main/resources/web/`.
- Contrato completo de la API: [interfaz/contrato-api.md](interfaz/contrato-api.md).

## Eventos

El bus recibe `POST /api/eventos` y lo reenvía a cada servicio suscrito, en orden y con 3 reintentos.
Lo que lleva cada evento está en `comun/.../eventos/DatosEventos.java`.

| Evento | Lo publica | Lo escucha | Para qué |
|---|---|---|---|
| `OrdenCreada` | Órdenes | Caja | Caja crea su copia de la orden |
| `LineaCreada` | Órdenes | Cocina, Caja | la línea aparece en cocina; Caja la suma con el precio con que se pidió |
| `LineaModificada` | Órdenes | Cocina, Caja | cambia cantidad o modificadores |
| `LineaCancelada` | Órdenes | Cocina, Caja | la línea sale de cocina y de los totales |
| `OrdenCancelada` | Órdenes | Cocina, Caja | se retiran todas sus líneas |
| `LineaIniciada` | Cocina | Órdenes | la línea pasa a EN_COCCION y ya no se puede editar |
| `LineaLista` | Cocina | Órdenes | la línea pasa a LISTO |
| `OrdenCobrada` | Caja | Órdenes, Cocina | Órdenes registra el pago y cierra la orden; Cocina deja de mostrarla |

Secuencia completa: [diagramas/5-secuencia-eventos.png](diagramas/5-secuencia-eventos.png)
(fuente en `5-secuencia-eventos.mmd`).

Qué pasa cuando dos servicios no están de acuerdo:
- **Cocina manda sobre el estado de la línea.** Si el mesero modifica una línea justo cuando el
  cocinero la inicia, Cocina ignora el cambio y lo avisa en su consola.
- **Caja cobra con lo que tiene en su copia.** Si llega una línea nueva después de cobrar, Caja la
  ignora; si a Órdenes le llega un `OrdenCobrada` que no cubre lo que tiene la orden, no la cierra y
  lo avisa en su consola.
- Los eventos repetidos no hacen nada la segunda vez (las líneas y órdenes se buscan por su id).

> **Alcance:** la persistencia sigue en memoria y el bus no guarda los eventos. Si Cocina o Caja se
> reinician pierden lo que sabían y no reciben lo que pasó mientras estaban apagados. Por eso el bus
> se arranca primero y Cocina y Caja antes que Órdenes (ver `iniciar-todo.bat`). Para agregar una base
> de datos basta con escribir nuevos adaptadores para los mismos puertos de salida.

## Reglas de negocio principales (ver `diagramas/2-clases-dominio.jpeg`)

- **RF1:** precio mayor a 0 con 2 decimales como máximo; tasa de impuesto como fracción entre 0 y 1;
  ids únicos. Un modificador EXTRA cuesta 0 o más y una EXCLUSION no tiene costo.
- **RF2:** una orden necesita un tipo (SALA, PARA_LLEVAR, DELIVERY) y una referencia (mesa o pedido externo).
- **RF3:** las líneas solo se agregan, modifican o cancelan con la orden ABIERTA y la línea PENDIENTE.
  Una línea cancelada queda en estado CANCELADA y se excluye de la cocina y de los totales.
  El precio unitario, la tasa de impuesto y el costo de los modificadores se copian al crear la línea,
  así la orden conserva sus importes aunque cambie el catálogo.
- **Cocina:** PENDIENTE → EN_COCCION → LISTO.
- **Cancelar orden:** solo si ninguna línea ha entrado a cocina y no hay un pago aprobado.
- **RF4:** subtotal − descuento + impuestos (por línea, sobre la base con descuento) + propina,
  redondeado a 2 decimales (HALF_UP).
- **RF5:** cada intento de cobro se registra en Caja como `Pago` (APROBADO, RECHAZADO o PENDIENTE).
  Se permite como máximo un pago aprobado por orden, y la orden se cierra únicamente con un pago
  aprobado que cubra el total. Al cerrar se guardan los totales cobrados. Si el pago se rechaza, la
  orden sigue abierta y se puede reintentar con otro método.

## Cómo agregar un caso de uso
1. Declara la operación en el puerto de entrada que corresponda en `application/port/in`.
2. Impleméntala en su `XxxService` de `application/service`, que recibe los puertos de salida por constructor.
3. Pon las reglas de negocio dentro del dominio (`Orden`, `LineaPedido`, etc.), no en el servicio.
4. Expónla en el controlador REST del servicio (actualizando `docs/interfaz/contrato-api.md`).
   Si otro servicio necesita enterarse, publica un evento nuevo: agrégalo a `TiposEvento` y a
   `DatosEventos` en `comun` y suscríbelo en la raíz de composición del servicio que lo escucha.
5. Si la ruta tiene un prefijo nuevo, agrégalo en `RutasGateway`.

## Fuera de alcance / mejoras posibles
- Persistencia en base de datos y un bus que guarde los eventos (ver la nota de alcance).
- Integración con una terminal bancaria o pasarela de pago reales (hoy son simulaciones).
