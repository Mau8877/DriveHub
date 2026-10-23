import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';

import '../../core/theme/app_theme.dart';
import 'aviso_snack.dart';

/// Proveedor de mosaicos del mapa. En la app es `null` (los baja de internet); los tests de las
/// pantallas lo sustituyen para no depender de la red.
final proveedorTeselasProvider = Provider<TileProvider?>((ref) => null);

/// Punto del mapa que devuelve [SelectorUbicacion].
typedef AlCambiarUbicacion = void Function(double latitud, double longitud);

/// Obtiene la ubicación actual del dispositivo, o `null` si no se puede (sin permiso, GPS apagado).
typedef ObtenerUbicacionActual = Future<LatLng?> Function();

/// Mapa de OpenStreetMap para elegir un punto (CU08a): un toque coloca o mueve el marcador y
/// "Mi ubicación" usa el GPS. Lo usan el registro y el perfil (por eso vive en `shared/`).
///
/// * Solo elige el punto: NO calcula la dirección, la calcula el servidor al guardar. Si ya hay una
/// * dirección guardada, se muestra debajo como texto de solo lectura.
/// * Sin permiso de ubicación el mapa sigue funcionando por toque (se avisa con un snackbar).
/// * La atribución de OpenStreetMap siempre está a la vista (lo exige su política de uso).
class SelectorUbicacion extends StatefulWidget {
  final double? latitud;
  final double? longitud;
  final String? direccion;
  final bool habilitado;
  final AlCambiarUbicacion onCambio;

  /// Centro del mapa mientras no hay punto (La Paz por defecto).
  final LatLng centroInicial;

  /// Reemplazable en tests para no pedir permisos ni usar el GPS.
  final ObtenerUbicacionActual? obtenerUbicacionActual;

  /// Reemplazable en tests para no bajar mosaicos de internet.
  final TileProvider? tileProvider;

  const SelectorUbicacion({
    super.key,
    this.latitud,
    this.longitud,
    this.direccion,
    this.habilitado = true,
    required this.onCambio,
    this.centroInicial = const LatLng(-16.4897, -68.1193),
    this.obtenerUbicacionActual,
    this.tileProvider,
  });

  @override
  State<SelectorUbicacion> createState() => _SelectorUbicacionState();
}

class _SelectorUbicacionState extends State<SelectorUbicacion> {
  static const _zoomSinPunto = 12.0;
  static const _zoomConPunto = 16.0;
  static const _urlTeselas = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';

  final _controlador = MapController();

  bool get _hayPunto => widget.latitud != null && widget.longitud != null;

  LatLng? get _punto => _hayPunto ? LatLng(widget.latitud!, widget.longitud!) : null;

  /// Redondea a 6 decimales, lo que guarda la base (unos 10 cm).
  double _seis(double valor) => (valor * 1e6).round() / 1e6;

  void _elegir(LatLng punto) {
    if (!widget.habilitado) return;
    widget.onCambio(_seis(punto.latitude), _seis(punto.longitude));
  }

  @override
  void didUpdateWidget(SelectorUbicacion anterior) {
    super.didUpdateWidget(anterior);
    final punto = _punto;
    if (punto != null && (anterior.latitud != widget.latitud || anterior.longitud != widget.longitud)) {
      _controlador.move(punto, _controlador.camera.zoom < _zoomConPunto ? _zoomConPunto : _controlador.camera.zoom);
    }
  }

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  Future<LatLng?> _ubicacionDelDispositivo() async {
    final inyectada = widget.obtenerUbicacionActual;
    if (inyectada != null) return inyectada();
    if (!await Geolocator.isLocationServiceEnabled()) return null;
    var permiso = await Geolocator.checkPermission();
    if (permiso == LocationPermission.denied) {
      permiso = await Geolocator.requestPermission();
    }
    if (permiso == LocationPermission.denied || permiso == LocationPermission.deniedForever) return null;
    final posicion = await Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high),
    );
    return LatLng(posicion.latitude, posicion.longitude);
  }

  Future<void> _usarMiUbicacion() async {
    LatLng? punto;
    try {
      punto = await _ubicacionDelDispositivo();
    } catch (_) {
      punto = null;
    }
    if (!mounted) return;
    if (punto == null) {
      avisar(context, 'No pudimos obtener tu ubicación. Toca el mapa para elegirla.');
      return;
    }
    _elegir(punto);
    _controlador.move(punto, _zoomConPunto);
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = context.cockpit;
    final punto = _punto;
    final direccion = widget.direccion;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            OutlinedButton.icon(
              key: const Key('botonMiUbicacion'),
              onPressed: widget.habilitado ? _usarMiUbicacion : null,
              // * El tema estira los botones a todo el ancho; acá comparte fila con la ayuda
              style: OutlinedButton.styleFrom(minimumSize: const Size(0, 44)),
              icon: const Icon(Icons.my_location, size: 18),
              label: const Text('Mi ubicación'),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                punto == null ? 'Toca el mapa para marcar tu ubicación.' : 'Toca otro lugar para cambiarla.',
                style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ClipRRect(
          borderRadius: BorderRadius.circular(AppRadios.control),
          child: SizedBox(
            height: 240,
            child: FlutterMap(
              key: const Key('mapaUbicacion'),
              mapController: _controlador,
              options: MapOptions(
                initialCenter: punto ?? widget.centroInicial,
                initialZoom: punto == null ? _zoomSinPunto : _zoomConPunto,
                minZoom: 3,
                maxZoom: 19,
                interactionOptions: const InteractionOptions(
                  flags: InteractiveFlag.all & ~InteractiveFlag.rotate,
                ),
                onTap: (_, punto) => _elegir(punto),
              ),
              children: [
                TileLayer(
                  urlTemplate: _urlTeselas,
                  userAgentPackageName: 'com.example.mobile',
                  tileProvider: widget.tileProvider,
                ),
                if (punto != null)
                  MarkerLayer(
                    markers: [
                      Marker(
                        key: const Key('marcadorUbicacion'),
                        point: punto,
                        width: 40,
                        height: 40,
                        alignment: Alignment.topCenter,
                        child: Icon(Icons.location_on, size: 40, color: colores.acento),
                      ),
                    ],
                  ),
                const SimpleAttributionWidget(source: Text('© OpenStreetMap')),
              ],
            ),
          ),
        ),
        if (direccion != null && direccion.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text('Dirección', style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
          Text(direccion, key: const Key('direccionUbicacion'), style: tema.textTheme.bodyMedium),
        ],
      ],
    );
  }
}
