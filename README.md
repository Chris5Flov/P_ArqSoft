# arqds
ProyectosADS

## Sistema de Pedidos de Restaurante (`restaurante-pedidos-hexagonal/`)

Aplicación en Java 17 con arquitectura hexagonal (puertos y adaptadores) para gestionar las
comandas de un restaurante. Se usa desde una interfaz web (con una API REST) o desde la consola:

| Actor | Funcionalidad |
|---|---|
| Administrador | Registrar y consultar platos y modificadores (extras y exclusiones) — RF1 |
| Mesero | Abrir órdenes y agregar, modificar o cancelar líneas; cancelar una orden — RF2, RF3 |
| Cocinero | Consultar los pedidos pendientes y avanzar su estado (PENDIENTE → EN_COCCION → LISTO) — RF3 |
| Cajero | Calcular totales con descuento y propina, y cobrar en efectivo, tarjeta o pasarela digital — RF4, RF5 |

El sistema está separado en servicios que se comunican por REST y por un bus de eventos.
La interfaz web entra siempre por el gateway (http://localhost:8080):

| Proceso | Módulo | Puerto | Qué hace |
|---|---|---|---|
| Bus de eventos | `bus-eventos` | 8090 | recibe los eventos y los reenvía a los servicios suscritos |
| Menú | `servicio-menu` | 8081 | platos y modificadores (RF1) |
| Cocina | `servicio-cocina` | 8083 | pedidos pendientes y su avance (RF3) |
| Caja | `servicio-caja` | 8084 | totales y cobro (RF4, RF5) |
| Órdenes | `monolito` | 8082 | órdenes y sus líneas (RF2, RF3) |
| API Gateway | `gateway` | 8080 | interfaz web y reenvío de `/api/...` a cada servicio |

`comun` no es un proceso: es la librería con el dominio y las piezas compartidas.

### Requisitos
- JDK 17 o superior
- Maven 3.8 o superior

### Compilar y probar
```bash
cd restaurante-pedidos-hexagonal
mvn test                       # pruebas JUnit 5 de todos los módulos
mvn -DskipTests package        # un .jar ejecutable por servicio en <módulo>/target/
```

### Levantar todo
En Windows, desde `restaurante-pedidos-hexagonal`:
```bat
iniciar-todo.bat            :: compila si hace falta y abre una ventana por proceso
iniciar-todo.bat compilar   :: fuerza la compilación antes de arrancar
```
Abre http://localhost:8080 con un menú y una orden de ejemplo. Para detener todo, cierra las ventanas.

A mano (o en macOS/Linux), una terminal por proceso y **en este orden**, después de `mvn -DskipTests package`:
```bash
java -jar bus-eventos/target/bus-eventos-0.1.0.jar                # 1. el bus primero
java -jar servicio-menu/target/servicio-menu-0.1.0.jar --demo     # 2.
java -jar servicio-cocina/target/servicio-cocina-0.1.0.jar        # 3.
java -jar servicio-caja/target/servicio-caja-0.1.0.jar            # 4.
java -jar monolito/target/monolito-0.1.0.jar --demo               # 5. Órdenes, cuando Cocina y Caja ya estén arriba
java -jar gateway/target/gateway-0.1.0.jar                        # 6.
```
El orden importa porque el bus no guarda eventos: si Cocina o Caja arrancan después que Órdenes,
no se enteran de las órdenes que ya existían. Lo mismo pasa si se reinicia alguno de los dos.

Cada servicio acepta `--puerto=N`. Órdenes, Cocina y Caja aceptan `--bus=URL`; Órdenes, `--menu=URL`;
el gateway, `--menu`, `--ordenes`, `--cocina` y `--caja` (o las variables `MENU_URL`, `BUS_URL`, etc.).

Consola del mesero (necesita al Menú arriba; usa un bus en memoria, así que no avisa a Cocina ni a Caja):
```bash
java -cp monolito/target/monolito-0.1.0.jar com.restaurante.infrastructure.in.cli.Main --demo
```

### Documentación
- [Arquitectura, servicios y eventos](restaurante-pedidos-hexagonal/docs/arquitectura-base.md)
- [Contrato de la API REST](restaurante-pedidos-hexagonal/docs/interfaz/contrato-api.md)
- [Secuencia con los eventos](restaurante-pedidos-hexagonal/docs/diagramas/5-secuencia-eventos.png)
- [Diagramas](restaurante-pedidos-hexagonal/docs/diagramas/)
