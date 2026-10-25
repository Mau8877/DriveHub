import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/fila_accion.dart';

/// La sección «Pagos» del estudiante (pestaña del `EstudianteShell`).
///
/// * Por ahora tiene la acción «Comprar horas» (CU26) y un estado vacío; el historial de compras llega con
/// * CU28 y reemplaza al estado vacío, dejando «Comprar horas» como acción.
class PagosScreen extends StatelessWidget {
  const PagosScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
      children: [
        Card(
          child: FilaAccion(
            icono: Icons.add_card_outlined,
            titulo: 'Comprar horas',
            detalle: 'Elige un plan y paga con tarjeta',
            onTap: () => context.push('/payments/plans'),
          ),
        ),
        const EstadoVacio(
          icono: Icons.account_balance_wallet_outlined,
          titulo: 'Todavía no tienes compras',
          texto: 'Cuando compres un plan de horas, aparecerá aquí.',
        ),
      ],
    );
  }
}
