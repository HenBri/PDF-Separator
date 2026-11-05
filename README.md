# DskV2

DskV2 es una aplicación de escritorio en JavaFX para analizar archivos PDF, detectar qué páginas contienen color y separarlas en documentos independientes: uno en blanco y negro y otro a color. También incluye un calculador de costos de impresión.

## Funciones principales

- Análisis página por página para detectar color en imágenes, recursos, anotaciones y contenido vectorial.
- Separación automática del PDF en dos archivos: `*_BN.pdf` y `*_COLOR.pdf`.
- Carga por arrastrar y soltar o mediante selector de archivos.
- Cálculo de costo total con precios configurables para blanco y negro, color y gastos extra.
- Persistencia de la configuración de precios y del último directorio de exportación.
- Soporte para etiquetas de página del PDF original.

## Requisitos del sistema

*   **Java 21** o superior.
*   **Maven 3.8** o superior (para compilación).
*   Sistema operativo compatible con JavaFX (Windows, macOS, Linux).

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| JavaFX | 21.0.6 |
| Apache PDFBox | 3.0.3 |
| JUnit Jupiter | 5.10.2 |

## Compilación y ejecución

### Desde el código fuente (Generar JAR)

Para compilar el proyecto y generar el archivo ejecutable:

```bash
mvn clean package
```

El archivo JAR resultante se encontrará en `target/sepktop-pdf-2.0.0.jar`.

### Ejecutar con Maven (Desarrollo)

Ideal para probar cambios rápidamente sin generar el JAR:

```bash
mvn javafx:run
```

### Ejecutar el JAR generado

```bash
java -jar target/sepktop-pdf-2.0.0.jar
```

### Ejecutar versión instalada (Windows)

Si has utilizado el instalador, puedes ejecutar directamente:
`DskV2/DskV2.exe`

## Uso

1. Abre un archivo PDF desde la zona de carga o arrástralo a la ventana.
2. La aplicación analiza el documento y detecta las páginas con color.
3. Revisa el resumen: total de páginas, páginas en B/N, páginas a color e índices detectados.
4. Ajusta los precios de impresión desde el diálogo de precios si lo necesitas.
5. Exporta el resultado y selecciona la carpeta de destino.

Se generarán dos archivos:

- `nombre_BN.pdf`
- `nombre_COLOR.pdf`

## Estructura del proyecto

```text
src/
|-- main/
|   |-- java/
|   |   |-- module-info.java
|   |   `-- com/chromasplit/
|   |       |-- Main.java
|   |       |-- ChromasplitApp.java
|   |       |-- model/
|   |       |-- service/
|   |       `-- ui/
|   `-- resources/
|       `-- com/chromasplit/
|           |-- ui/main.fxml
|           `-- css/theme.css
```

## Cómo funciona la detección de color

El analizador revisa tres niveles del documento:

- recursos de página, como imágenes, espacios de color, sombras y patrones;
- anotaciones con color no gris;
- operadores del stream de contenido PDF mediante `PDFGraphicsStreamEngine`.

## Persistencia

La aplicación guarda en `java.util.prefs.Preferences`:

- el precio por página en blanco y negro;
- el precio por página a color;
- el último directorio de exportación usado.

## Licencia

Este proyecto es de codigo abierto.
