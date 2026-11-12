# Segmentación de estudiantes (demostración de aprendizaje no supervisado)

**Solo para demostrar en local.** No es parte de la app ni del backend y no se despliega. Muestra cómo se entrena
un modelo **sin etiquetas**, como contraste con los dos modelos supervisados del proyecto (`../entrenar_tablero.py`,
CU40, y `../acciones/entrenar_acciones.py`, CU50).

| | Supervisado (tablero, acciones) | No supervisado (esta carpeta) |
| --- | --- | --- |
| Datos de entrada | Cada ejemplo con su respuesta (la luz, la acción) | Solo las características, sin respuesta |
| Qué aprende | A repetir la etiqueta | Grupos parecidos que nadie definió |
| Cómo se evalúa | Exactitud contra las etiquetas | Silueta / inercia (y aquí, el ARI contra perfiles ocultos) |

## Qué hace

k-means sobre una tabla de estudiantes con 5 columnas: `horas_compradas`, `clases_tomadas`, `cancelaciones`,
`nota_promedio` y `dias_sin_reservar`. Los datos son **sintéticos**: se generan con 4 perfiles ocultos
(constante, intensivo, en riesgo, irregular). El modelo nunca ve el perfil; solo se usa en el último paso para
comprobar que k-means encontró los grupos por su cuenta.

El k-means (algoritmo de Lloyd con inicio k-means++) está escrito a mano en `segmentar_estudiantes.py` (`kmeans()`)
para que se vea el entrenamiento: **asignar** cada estudiante al centroide más cercano y **actualizar** cada
centroide al promedio de su grupo, hasta que no cambie. scikit-learn solo aporta la silueta, el PCA y el ARI.

## Preparar (una vez, antes de la demostración)

Python 3.10 o más nuevo. Desde la raíz del repositorio, en PowerShell:

```powershell
cd projects/mobile/ml/segmentacion
py -3.12 -m venv .venv
.\.venv\Scripts\python -m pip install -r requirements.txt
.\.venv\Scripts\python segmentar_estudiantes.py
```

Si termina con `OK: el modelo encontró los perfiles sin etiquetas.`, está listo. `.venv/` y `salida/` no se suben
al repositorio.

## Durante la demostración

```powershell
.\.venv\Scripts\python segmentar_estudiantes.py --pausas
```

`--pausas` se detiene después de cada paso (Enter para seguir). Qué decir en cada uno:

1. **Datos**: 240 estudiantes y ninguna columna de «respuesta correcta». Se escalan (media 0, desvío 1) para que
   las horas no pesen más que las cancelaciones.
2. **Elegir k**: se entrena con k de 2 a 8. La inercia siempre baja (codo) y la silueta marca el mejor: **k = 4**.
   Lo eligieron los datos, no nosotros. Gráfico: `salida/1_elegir_k.png`.
3. **Entrenar**: la inercia (suma de distancias de cada estudiante a su centroide) baja en cada iteración hasta
   converger, en unas 8 iteraciones. Ese es el aprendizaje. Gráfico: `salida/2_entrenamiento.png`.
4. **Visualizar**: los 5 datos de cada estudiante proyectados a 2D con PCA, coloreados por grupo, con los
   centroides marcados. Gráfico: `salida/3_grupos_pca.png`.
5. **Interpretar**: el centroide de cada grupo en unidades reales y sus rasgos (ALTO / bajo respecto del
   promedio). Aquí se les pone nombre: muchas horas y clases → «intensivo»; compró horas pero lleva semanas sin
   reservar → «en riesgo de abandono»; muchas cancelaciones → «irregular».
6. **Estudiante nuevo**: se lo asigna al centroide más cercano sin volver a entrenar (cae en «en riesgo»).
7. **Comprobación**: recién ahora se revelan los perfiles ocultos. Cada grupo coincide casi entero con un perfil y
   el índice de Rand ajustado da **≈ 0,95** (1 = idéntico, 0 = azar). No da 1 porque los perfiles se superponen a
   propósito, como pasa con datos reales.

Abrir los gráficos: `start salida`.

## Preguntas probables

- **¿Por qué datos sintéticos?** Hoy no hay estudiantes reales suficientes, y con perfiles ocultos se puede
  *comprobar* que el modelo los encontró, cosa imposible con datos reales sin etiquetas. Con datos reales, la tabla
  saldría de Pagos (horas) y Académico (clases, notas), por sus APIs: cada servicio tiene su propia base.
- **¿Para qué le serviría a la escuela?** Para detectar estudiantes en riesgo de abandono y contactarlos antes, o
  para armar planes de horas según el perfil.
- **¿Por qué k-means?** Es el algoritmo no supervisado más simple de explicar y de mostrar iteración por iteración.
  Sus límites: hay que elegir k y supone grupos de forma redonda (por eso se escalan los datos y se usa la silueta).
