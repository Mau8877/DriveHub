import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/servidor/providers/servidor_provider.dart';
import 'package:mobile/features/servidor/services/servidor_storage.dart';

import '../../helpers/adaptador_en_cola.dart';

class StorageDeServidorEnMemoria implements ServidorStorage {
  String? valor;
  int guardados = 0;

  StorageDeServidorEnMemoria([this.valor]);

  @override
  Future<String?> leer() async => valor;

  @override
  Future<void> guardar(String direccionApi) async {
    valor = direccionApi;
    guardados++;
  }

  @override
  Future<void> borrar() async => valor = null;
}

void main() {
  late StorageDeServidorEnMemoria storage;

  ProviderContainer crear({EstadoServidor? inicial}) {
    final container = ProviderContainer(
      overrides: [
        servidorStorageProvider.overrideWithValue(storage),
        if (inicial != null) servidorInicialProvider.overrideWithValue(inicial),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://10.0.2.2:8080/api/v1');
    ApiClient.configurarBaseUrl(null);
    storage = StorageDeServidorEnMemoria();
  });

  tearDown(() => ApiClient.configurarBaseUrl(null));

  test('por defecto la app se considera configurada (los tests de otras pantallas no pasan por acá)', () {
    expect(crear().read(servidorProvider).configurado, true);
  });

  test('arranca como lo dejó main(): sin servidor elegido, sin configurar', () {
    final container = crear(inicial: const EstadoServidor(configurado: false));

    expect(container.read(servidorProvider).configurado, false);
    expect(container.read(servidorProvider).url, isNull);
  });

  group('guardar', () {
    test('normaliza la dirección, la guarda en el teléfono y deja la app configurada', () async {
      final container = crear(inicial: const EstadoServidor(configurado: false));

      final url = await container.read(servidorProvider.notifier).guardar('192.168.1.10:8080');

      expect(url, 'http://192.168.1.10:8080/api/v1');
      expect(storage.valor, 'http://192.168.1.10:8080/api/v1');
      expect(container.read(servidorProvider).configurado, true);
      expect(container.read(servidorProvider).url, 'http://192.168.1.10:8080/api/v1');
    });

    test('cambia la dirección del ApiClient en el momento', () async {
      final container = crear(inicial: const EstadoServidor(configurado: false));

      await container.read(servidorProvider.notifier).guardar('http://192.168.1.10:8080');

      expect(ApiClient.baseUrlActual, 'http://192.168.1.10:8080/api/v1');
    });

    test('actualiza el MISMO cliente que ya tienen los servicios: ninguno queda con el servidor anterior', () async {
      final container = crear(inicial: const EstadoServidor(configurado: false));
      final antes = ApiClient.instance;
      expect(antes.options.baseUrl, 'http://10.0.2.2:8080/api/v1');

      await container.read(servidorProvider.notifier).guardar('http://192.168.1.10:8080');

      expect(identical(ApiClient.instance, antes), true);
      expect(antes.options.baseUrl, 'http://192.168.1.10:8080/api/v1');
    });

    test('las peticiones salen al servidor nuevo', () async {
      final container = crear(inicial: const EstadoServidor(configurado: false));
      ApiClient.configurarSesion(obtenerToken: () => 't', renovar: () async => null);
      final adaptador = AdaptadorEnCola([RespuestaFalsa(200, exito({'x': 1}))]);
      ApiClient.instance.httpClientAdapter = adaptador;

      await container.read(servidorProvider.notifier).guardar('http://192.168.1.10:8080');
      await ApiClient.instance.get<dynamic>('/estudiantes');

      expect(adaptador.peticiones.single.uri.toString(), 'http://192.168.1.10:8080/api/v1/estudiantes');
    });

    test('una dirección inválida no guarda nada ni cambia nada', () async {
      final container = crear(inicial: const EstadoServidor(configurado: false));

      await expectLater(container.read(servidorProvider.notifier).guardar('ftp://host'), throwsArgumentError);

      expect(storage.guardados, 0);
      expect(container.read(servidorProvider).configurado, false);
      expect(ApiClient.baseUrlActual, 'http://10.0.2.2:8080/api/v1');
    });

    test('cambiar de servidor reemplaza al anterior', () async {
      final container = crear(inicial: const EstadoServidor(configurado: true, url: 'http://viejo:8080/api/v1'));

      await container.read(servidorProvider.notifier).guardar('http://nuevo:8080');

      expect(storage.valor, 'http://nuevo:8080/api/v1');
      expect(container.read(servidorProvider).url, 'http://nuevo:8080/api/v1');
    });
  });

  group('ApiClient.baseUrlActual', () {
    test('sin servidor elegido usa API_BASE_URL del .env', () {
      expect(ApiClient.baseUrlActual, 'http://10.0.2.2:8080/api/v1');
    });

    test('lo elegido manda sobre el .env, y null vuelve al .env', () {
      ApiClient.configurarBaseUrl('http://otro:9000/api/v1');
      expect(ApiClient.baseUrlActual, 'http://otro:9000/api/v1');

      ApiClient.configurarBaseUrl(null);
      expect(ApiClient.baseUrlActual, 'http://10.0.2.2:8080/api/v1');
    });
  });
}
