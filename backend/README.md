# Backend de VulnTrack

Requiere Java 21 o superior, Maven y Docker. Con `DB_PASSWORD` configurada para la base local:

```sh
# Desde la raíz del repositorio
docker compose up -d
# Desde backend, exportando DB_PASSWORD en esta terminal
mvn spring-boot:run
```

Flyway aplica V1 y V2; Hibernate valida el esquema (`ddl-auto=validate`). V1 se conserva sin cambios. V2 incorpora el estado de análisis de cada dependencia y un índice de la clave foránea.

## API

| Método y ruta | Respuesta |
| --- | --- |
| `GET /api/v1/health` | Salud pública |
| `POST /api/v1/dependencies/upload` | Multipart `file=pom.xml`; `scanId` y `results` |
| `GET /api/v1/scans` | Lista con `id`, `filename`, `createdAt`, `dependencyCount`, `vulnerableDependencyCount`, de más reciente a más antigua |
| `GET /api/v1/scans/{id}` | `id`, `filename`, `createdAt` y `dependencies`; 404 si no existe |

Los tres endpoints de análisis requieren una sesión autenticada. Se conserva el formulario de Spring Security y CSRF: la subida requiere un token CSRF válido. El historial es una demostración local compartida entre todos los usuarios autenticados; no existe aislamiento por propietario.

Cada dependencia devuelve `groupId`, `artifactId`, `version`, `vulnerabilities` (array JSON) y `status`. Los estados son `ANALYZED` y `SKIPPED_UNRESOLVED_VERSION`. Las versiones ausentes, vacías o con propiedades Maven sin resolver se conservan, con su versión original y un array vacío; ese array no indica que se hayan analizado sin encontrar vulnerabilidades.

Errores: 400 para archivo vacío, nombre distinto de `pom.xml` o XML/POM inválido; 413 para exceso de tamaño; 502 si OSV falla o devuelve resultados incompletos, inválidos o paginados; 503 para fallos de acceso/persistencia. Los errores no incluyen trazas ni detalles de la base de datos.

## Persistencia

`scans` contiene el archivo y la fecha local del análisis (`TIMESTAMP`, sin zona). `scan_dependencies` contiene las coordenadas Maven, la versión opcional, el estado y las vulnerabilidades JSONB. `Scan` usa `OneToMany` con carga diferida, cascada y eliminación de huérfanos; `ScanDependency` mantiene la clave foránea mediante `ManyToOne` diferido. `addDependency` enlaza ambos lados.

OSV termina antes de abrir la transacción de escritura. `ScanService` valida que haya un resultado coherente por dependencia y guarda todo con `saveAndFlush` en una transacción: un fallo revierte el padre y sus dependencias. La conversión a DTO ocurre dentro de la transacción. El historial calcula los contadores en una sola consulta agregada, sin cargar vulnerabilidades; el detalle utiliza una consulta con `join fetch`.

Las vulnerabilidades se representan mediante `List<Map<String, Object>>` y `@JdbcTypeCode(SqlTypes.JSON)`: Hibernate serializa la lista directamente, sin convertirla previamente en un string JSON. La integración lo comprueba mediante `jsonb_typeof`, acceso SQL al ID y recuperación por JPA.

## Pruebas

```sh
mvn clean verify
```

Requieren Docker disponible. Testcontainers arranca un PostgreSQL 17 aislado, sin usar la base de Compose ni `DB_PASSWORD`. Se prueban Flyway desde cero y la actualización de V1 con datos existentes, validación JPA, JSONB, rollback, orden del historial, ausencia de N+1, endpoints, 404, autenticación, CSRF, XML inválido/XXE y errores OSV. OSV se simula con Mockito en las pruebas HTTP y con `MockRestServiceServer` en las del cliente; ninguna prueba consulta OSV real.

La integración externa con OSV, la resolución de propiedades/versiones heredadas Maven y el aislamiento multiusuario quedan fuera de esta implementación. Si OSV pagina resultados, la petición falla explícitamente sin guardar un análisis parcial.

## Base PostgreSQL de esta reconstrucción

Compose utiliza el volumen `vulntrack_scan_history_data`, creado vacío para esta aplicación. Mantiene la base `vulntrack`, el usuario `vulntrack`, la variable `DB_PASSWORD` y el puerto local 5432.

El volumen anterior `vulntrack_postgres_data` pertenece al proyecto antiguo: contenía `projects` y una V1 denominada `create projects table`. Se conserva intacto junto con los demás volúmenes anteriores. La reconstrucción inicia su propio historial Flyway con `V1__create_scans_tables.sql` y aplica después V2. No se ha ejecutado `repair` ni modificado manualmente `flyway_schema_history`.

Verificación realizada: `mvn clean verify` supera 18 pruebas; Spring Boot arranca contra esta base local y Hibernate valida el esquema. Flyway registra V1 y V2 con éxito. Se comprobaron por HTTP salud pública (200), historial autenticado vacío (200), redirección sin sesión (302), análisis inexistente (404) y rechazo de POST sin CSRF (403). La compilación usa `release 21`; el entorno de Maven disponible ejecuta JDK 27.
