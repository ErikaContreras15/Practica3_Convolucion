# Práctica 3 – Procesamiento de imágenes con Threads

Filtro de convolución **Sobel 3×3** aplicado a una imagen de **7289 × 4862 px**, comparando la versión **secuencial** con la **paralela** usando la clase `Thread` de Java. La máscara está programada a mano, **sin OpenCV**.

**Integrantes:** Erika Contreras · Jorge Pizarro


---

## Resultado principal

| Hilos | Tiempo promedio (ms) | Aceleración | Eficiencia | RAM pico (MB) |
|---|---|---|---|---|
| 1 (secuencial) | 1440 | 1,00 | 100 % | 513,6 |
| 2 | 838 | 1,72 | 86 % | 513,6 |
| 4 | 601 | 2,40 | 60 % | 513,6 |
| 8 | 414 | **3,48** | 43 % | 513,6 |

Con 8 hilos el filtro es **3,48 veces más rápido** que el secuencial. La RAM es la misma con cualquier número de hilos, porque todos comparten la imagen.

*Equipo: 8 núcleos lógicos, 16 GB de RAM, Windows. Los valores son el promedio de 7 ejecuciones, cada una con 5 repeticiones.*

---

## Estructura

```
src/
  Main.java             Carga la imagen, mide tiempos y RAM, imprime resultados
  Convolucion.java      Máscaras de Sobel, versión secuencial y paralela
  HiloConvolucion.java  Hilo (extends Thread) que filtra una franja de filas
  Telemetria.java       Medición de RAM y hilo monitor del pico de memoria
data/
  imagen.jpg            Imagen de entrada (6000 px o más)
salida/
  resultado.jpg         Imagen filtrada (se genera al ejecutar)
```

## Requisitos

- **JDK 17 o superior**. Para comprobarlo, ejecuta `java -version` y `javac -version`.
- Opcional: Visual Studio Code con *Extension Pack for Java*.

## Cómo ejecutar

1. Coloca la imagen en `data/imagen.jpg`.
2. Abre una terminal dentro de la carpeta del proyecto y ejecuta:

```bash
javac src/*.java -d bin
java -Xmx4g -cp bin Main
```

`-Xmx4g` le da a Java hasta 4 GB de memoria, suficiente para imágenes grandes.

### Parámetros opcionales

```bash
java -Xmx4g -cp bin Main [imagen] [repeticiones] [hilos]
```

| Parámetro | Por defecto | Ejemplo |
|---|---|---|
| imagen | `data/imagen.jpg` | `data/foto.jpg` |
| repeticiones | `5` | `10` |
| hilos | `1,2,4,8` | `1,2,4,8,12` |

Ejemplo: `java -Xmx4g -cp bin Main data/imagen.jpg 5 1,2,4,8`

## Ejemplo de salida

```
===== CONFIGURACION =====
Imagen: imagen.jpg
Tamano: 7289 x 4862 (35.4 megapixeles)
Filtro: Sobel 3x3 (halo de 1 px)
Repeticiones: 5 | Hilos: [1, 2, 4, 8]
Nucleos disponibles: 8

===== TELEMETRIA DE RAM =====
RAM fisica del equipo:        16299 MB (libre: 7572 MB)
Maximo para la JVM (-Xmx):    4096 MB
RAM usada al iniciar:         2.2 MB
RAM con la imagen cargada:    512.4 MB
Una copia de la imagen:       135.2 MB (7289 x 4862 x 4 bytes)
Estimado teorico (4 copias):  540.8 MB

===== RESULTADOS =====
Hilos            Tiempo promedio (ms)   Aceleracion    Eficiencia   RAM pico (MB)
1 (secuencial)   1416.81                1.00           100%         513.6
2                745.99                 1.90           95%          513.6
4                578.31                 2.45           61%          513.6
8                406.05                 3.49           44%          513.6
```

---

## Cómo funciona

### Filtro Sobel

Cada píxel nuevo se calcula con sus 9 vecinos (3 × 3) convertidos a gris. Se aplican dos máscaras, una para los cambios horizontales (Gx) y otra para los verticales (Gy):

```
Gx = | -1  0  +1 |      Gy = | -1  -2  -1 |
     | -2  0  +2 |           |  0   0   0 |
     | -1  0  +1 |           | +1  +2  +1 |

m = min(255, √(sx² + sy²))
```

En las zonas lisas el resultado es negro y en los bordes es blanco.

### Versión secuencial

Un solo hilo (`main`) recorre las 4862 filas, una tras otra. Solo trabaja un núcleo del procesador.

### Versión paralela

La imagen se divide en **N franjas horizontales de filas**, una por hilo:

1. **Reparto:** cada hilo recibe `alto / N` filas. Si sobran filas, se dan de una en una a los primeros hilos.
2. **`start()`:** todos los hilos filtran su franja al mismo tiempo, en núcleos distintos.
3. **`join()`:** el hilo `main` espera a que todos terminen y luego guarda `resultado.jpg`.

Todos los hilos **leen** la misma imagen original, incluido el **halo**: la fila vecina que necesita la máscara. Cada hilo **escribe solo sus filas** del resultado, así que no hay condiciones de carrera y no se necesita `synchronized`.

### Diseño por fases

| Fase | En esta práctica |
|---|---|
| Descomposición | Por dominio: la imagen se divide en filas |
| Comunicación | Solo el halo (1 fila arriba y 1 abajo), leído de la memoria compartida |
| Aglomeración | N franjas grandes, una por hilo, en vez de una tarea por píxel |
| Mapeo | Estático: `alto / N` filas por hilo; el sistema operativo asigna los núcleos |

### Medición

- La imagen se carga **antes** de medir el tiempo.
- Se hacen **2 rondas de calentamiento** de la JVM, que no se miden.
- Cada configuración se repite **5 veces** con `System.nanoTime()` y se informa el promedio.
- Cada resultado paralelo se **compara píxel por píxel** con el secuencial. Si no coincide, se imprime un ERROR.
- Un **hilo monitor** mide la RAM cada 5 ms y registra el pico.

Fórmulas usadas:

- **Aceleración:** S = Ts / Tp
- **Eficiencia:** E = S / N × 100 %

## Conclusiones

- El paralelo **sí mejora**: con 8 hilos el tiempo bajó de 1440 ms a 414 ms (aceleración de 3,48), con un resultado idéntico al secuencial.
- La mejora **no es lineal**: la eficiencia baja de 86 % con 2 hilos a 43 % con 8.
- La **memoria compartida ahorra RAM**: el pico fue de 513,6 MB con cualquier número de hilos.
